// Copyright (C) 2008 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.caja.config;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Pair;
import com.google.caja.util.Strings;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Utilities for resolving and parsing configuration files.
 *
 * @author mikesamuel@gmail.com
 */
public class ConfigUtil {
  /**
   * Resolves a URI from a configuration file, allowing access to resources on
   * the classpath, or content inside {@code content:} URIs.
   */
  public static final ImportResolver RESOURCE_RESOLVER = new ImportResolver() {
    /**
     * {@inheritDoc}
     * @param uri a URI relative to base, or an absolute URI with scheme in
     *  {@code ("content", "resource")}.
     * @param base null or a URI with scheme in {@code "resource"}.
     */
    public Pair<Reader, FilePosition> resolve(
        URI uri, URI base, FilePosition uriPos)
        throws IOException {
      if (uri == null) { throw new NullPointerException(); }

      if (!uri.isAbsolute()) {
        if (base == null) {
          throw new IllegalArgumentException("Missing base URI");
        }
        String scheme = base.getScheme();
        if (!(Strings.equalsIgnoreCase("resource", scheme)
              && base.isAbsolute())) {
          throw new IllegalArgumentException("base URI: " + base);
        }

        uri = base.resolve(uri);
      }

      if (!uri.isAbsolute()) {
        throw new IllegalArgumentException("URI not absolute: " + uri);
      }

      InputStream in;

      String scheme = Strings.toLowerCase(uri.getScheme());
      if ("content".equals(scheme)) {
        String content = uri.getSchemeSpecificPart();
        if (content == null) {
          throw new IllegalArgumentException("URI missing content: " + uri);
        }
        return Pair.pair(
            (Reader) new StringReader(content),
            FilePosition.startOfFile(new InputSource(uri)));
      } else if ("resource".equals(scheme)) {
        String path = uri.getPath();
        if (path == null) {
          throw new IllegalArgumentException("URI missing path: " + uri);
        }
        in = ConfigUtil.class.getResourceAsStream(path);
        if (in == null) {
          throw new FileNotFoundException(uri.toString());
        }
      } else {
        throw new IllegalArgumentException("URI: " + uri);
      }

      return Pair.pair(
          (Reader) new InputStreamReader(in, "UTF-8"),
          FilePosition.startOfFile(new InputSource(uri)));
    }
  };

  /**
   * Produce a whitelist from the given JSONObject.
   *
   * This implementation uses a third party JSON parser, so does not accurately
   * track file positions of sub objects.  Error messages will correctly
   * identify the file, but not the line number.  We do not use the javascript
   * parser, since JSON is not a subset of javascript.  We could use the
   * javascript parser with a JSON lexer if error message positions in config
   * files become a problem.
   *
   * @param in a {@code application/JSON} file of the form described at
   *     http://code.google.com/p/google-caja/wiki/CajaWhitelists
   * @param src the source of the JS.  Used to resolve relative URIs.
   * @param mq receives warnings and errors that don't prevent us from
   *     producing a whitelist.
   *
   * @return a WhiteList that may be invalid.  If mq contains no new
   *     {@link MessageLevel#ERROR}s or more serious messages, then the return
   *     value is valid.
   *
   * @throws IOException if we can't load an inherited whitelist.
   * @throws ParseException if we can't produce a whitelist.
   */
  public static WhiteList loadWhiteListFromJson(
      Reader in, FilePosition src, ImportResolver resolver, MessageQueue mq)
      throws IOException, ParseException {
    return (new JSONWhiteListLoader(src, resolver, mq)).loadFrom(in);
  }

  /**
   * Produce a whitelist from the given JSONObject.
   *
   * This implementation uses a third party JSON parser, so does not accurately
   * track file positions of sub objects.  Error messages will correctly
   * identify the file, but not the line number.  We do not use the javascript
   * parser, since JSON is not a subset of javascript.  We could use the
   * javascript parser with a JSON lexer if error message positions in config
   * files become a problem.
   *
   * @param whitelistUri an absolute URI loadable by resolver.
   * @param resolver used to load the given URI and any URIs referenced in that
   *     or included whitelists.
   * @param mq receives warnings and errors that don't prevent us from
   *     producing a whitelist.
   *
   * @return a WhiteList that may be invalid.  If mq contains no new
   *     {@link MessageLevel#ERROR}s or more serious messages, then the return
   *     value is valid.
   *
   * @throws IOException if we can't load an inherited whitelist.
   * @throws ParseException if we can't produce a whitelist.
   */
  public static WhiteList loadWhiteListFromJson(
      URI whitelistUri, ImportResolver resolver, MessageQueue mq)
      throws IOException, ParseException {
    Pair<Reader, FilePosition> wl = resolver.resolve(whitelistUri, null, null);
    try {
      return (new JSONWhiteListLoader(wl.b, resolver, mq)).loadFrom(wl.a);
    } finally {
      wl.a.close();
    }
  }

  /**
   * Produce a whitelist from the given JSONObject.
   *
   * @param value as described at
   *     http://code.google.com/p/google-caja/wiki/CajaWhitelists
   * @param src the source of the JS.  Used to resolve relative URIs.
   * @param mq receives warnings and errors that don't prevent us from
   *     producing a whitelist.
   *
   * @return a WhiteList that may be invalid.  If mq contains no new
   *     {@link MessageLevel#ERROR}s or more serious messages, then the return
   *     value is valid.
   *
   * @throws IOException if we can't load an inherited whitelist.
   * @throws ParseException if we can't produce a whitelist.
   */
  public static WhiteList loadWhiteListFromJson(
      JSONObject value, FilePosition src, ImportResolver resolver,
      MessageQueue mq)
      throws IOException, ParseException {
    return new JSONWhiteListLoader(src, resolver, mq).loadFrom(value);
  }

  private ConfigUtil() {}
}

final class UriReader extends BufferedReader {
  private final URI uri;
  UriReader(URI uri, Reader underlying) {
    super(underlying);
    this.uri = uri;
  }
  public URI getUri() { return uri; }
}

class JSONWhiteListLoader {
  final FilePosition src;
  final ImportResolver resolver;
  MessageQueue mq;

  JSONWhiteListLoader(
      FilePosition src, ImportResolver resolver, MessageQueue mq) {
    this.src = src;
    this.resolver = resolver;
    this.mq = mq;
  }

  WhiteList loadFrom(Reader in) throws IOException, ParseException {
    return fromSkeleton(loadSkeleton(in));
  }

  WhiteList loadFrom(JSONObject value) throws IOException, ParseException {
    return fromSkeleton(loadSkeleton(value));
  }

  // Prevent unnecessary reparsing of schemas.
  private static final Map<URI, Pair<WhiteListSkeleton, List<Message>>> cache
      = Collections.synchronizedMap(
            new WeakHashMap<URI, Pair<WhiteListSkeleton, List<Message>>>());

  private WhiteListSkeleton loadSkeleton(Reader in)
      throws IOException, ParseException {
    if (in instanceof UriReader) {
      URI uri = ((UriReader) in).getUri();
      Pair<WhiteListSkeleton, List<Message>> p = cache.get(uri);
      if (p != null) {
        mq.getMessages().addAll(p.b);
        return p.a;
      }
    }
    MessageQueue origMq = mq;
    SimpleMessageQueue cacheMq = new SimpleMessageQueue();
    this.mq = cacheMq;
    try {
      WhiteListSkeleton skel = loadSkeleton(
          expectJSONObject(JSONValue.parse(in), "whitelist"));
      if (in instanceof UriReader) {
        cache.put(
            ((UriReader) in).getUri(),
            Pair.pair(skel, cacheMq.getMessages()));
      }
      return skel;
    } finally {
      this.mq = origMq;
      origMq.getMessages().addAll(cacheMq.getMessages());
    }
  }

  /**
   * Split a JSON object into bits that can be turned into a skeleton,
   * and issue warnings for bits we don't understand.
   *
   * We produce a skeleton before building a full whitelist since the skeleton
   * makes it easier to deal recursively with inherited whitelists.
   *
   * @throws IOException if we can't load an inherited whitelist.
   * @throws ParseException if we can't produce a whitelist.
   */
  private WhiteListSkeleton loadSkeleton(JSONObject whitelistJson)
      throws IOException, ParseException {
    JSONArray inherits = optionalJSONArray(
        whitelistJson.get("inherits"), "inherits");
    JSONArray allows = optionalJSONArray(
        whitelistJson.get("allowed"), "allowed");
    JSONArray denies = optionalJSONArray(
        whitelistJson.get("denied"), "denied");
    JSONArray types = optionalJSONArray(
        whitelistJson.get("types"), "types");

    // Issue warnings for unrecognized keys.
    for (Object key : whitelistJson.keySet()) {
      if (!("inherits".equals(key)
            || "allowed".equals(key)
            || "denied".equals(key)
            || "types".equals(key)
            || "description".equals(key))) {
        mq.addMessage(ConfigMessageType.UNRECOGNIZED_KEY, src,
                      MessagePart.Factory.valueOf((String) key));
      }
    }

    // Look for well known properties, and pull out bits of the JSON.
    List<WhiteListSkeleton> inherited = new ArrayList<WhiteListSkeleton>();
    if (inherits != null) {
      for (Object obj : inherits) {
        // Match "resource://..." or { "src": "resource://..." }.
        String srcStr = obj instanceof String
            ? (String) obj
            : expectString(expectJSONObject(obj, "inherits").get("src"),
                           "inherits src");
        try {
          URI uri = src.source().getUri().resolve(new URI(srcStr));
          Pair<Reader, FilePosition> loaded = resolver.resolve(
              uri, src.source().getUri(), src);
          try {
            inherited.add(
                new JSONWhiteListLoader(loaded.b, resolver, mq)
                .loadSkeleton(loaded.a));
          } finally {
            loaded.a.close();
          }
        } catch (URISyntaxException ex) {
          mq.addMessage(ConfigMessageType.BAD_URL, src,
                        MessagePart.Factory.valueOf(srcStr));
        }
      }
    }

    Set<String> allowedItemSet = new HashSet<String>();
    if (allows != null) {
      for (Object obj : allows) {
        // Match either "foo" or { "key": "foo" }.
        String key = obj instanceof String
            ? (String) obj
            : expectString(expectJSONObject(obj, "allowed").get("key"),
                           "allowed key");
        allowedItemSet.add(key);
      }
    }

    Set<String> deniedItemSet = new HashSet<String>();
    if (denies != null) {
      for (Object obj : denies) {
        String key = obj instanceof String
            ? (String) obj
            : expectString(expectJSONObject(obj, "denied").get("key"),
                           "denied key");
        deniedItemSet.add(key);
      }
    }

    // Match { "key": "xyz", ... }
    List<JSONObject> typeDefinitions = new ArrayList<JSONObject>();
    if (types != null) {
      for (Object obj : types) {
        JSONObject def = expectJSONObject(obj, "type");
        expectString(def.get("key"), "type key");
        typeDefinitions.add(def);
      }
    }

    return makeSkeleton(
        inherited, allowedItemSet, deniedItemSet, typeDefinitions);
  }

  /**
   * Gather information into a skeleton, so we can deal recursively with
   * inherited whitelists.
   */
  WhiteListSkeleton makeSkeleton(
      List<WhiteListSkeleton> loaded, Set<String> allowed, Set<String> denied,
      List<JSONObject> definitions) {

    // Apply the algo described at
    // http://code.google.com/p/google-caja/wiki/CajaWhitelists .
    // See that URL for the list of invariants this maintains.

    // * Create an empty whitelist W
    WhiteListSkeleton w = new WhiteListSkeleton();
    w.denied.addAll(denied);  // Propagated to inheriting for the 2nd step below
    // * For each inherits
    //       o Fetch its URL -- Abort on failure
    //       o Load it using this algorithm
    //       o Add it to the list of loaded whitelists
    // Done already.

    // * For each loaded whitelist LW
    //       o Add LW's allows to W
    //       o Add LW's types to W
    for (WhiteListSkeleton lw : loaded) {
      w.allowed.addAll(lw.allowed);
      for (Map.Entry<String, List<JSONObject>> e
             : lw.definitions.entrySet()) {
        multimapAdd(w.definitions, e.getKey(), e.getValue());
      }
    }

    // * For each loaded whitelist LW
    //       o Remove any items in W matching LW's denies
    for (WhiteListSkeleton lw : loaded) {
      w.allowed.removeAll(lw.denied);
    }

    // * For each allows
    //       o Add an item to W.
    w.allowed.addAll(allowed);

    // * For each denies
    //       o Remove any item in W with the same key.
    w.allowed.removeAll(denied);

    // * For each types
    //       o Remove any type definition from W with the same key.
    for (JSONObject type : definitions) {
      w.definitions.remove(type.get("key"));
    }

    // * For each types
    //       o Add a type definition to W
    for (JSONObject type : definitions) {
      multimapAdd(w.definitions, (String) type.get("key"),
                  Collections.singletonList(type));
    }

    // * If there are type definitions in W with the same key, and the same
    //   value, remove all but 1.
    for (List<JSONObject> defs : w.definitions.values()) {
      if (defs.size() == 1) { continue; }
      JSONObject definition = defs.get(0);
      List<JSONObject> rest = defs.subList(1, defs.size());
      for (Iterator<JSONObject> otherIt = rest.iterator(); otherIt.hasNext();) {
        JSONObject other = otherIt.next();
        if (other.equals(definition)) {
          otherIt.remove();
        }
      }
      // The size should now be 1 if all the definitions were the same.
    }

    // * If there exist any two distinct type definitions in W with the
    //   same key, mark W invalid.
    // Done in checkValidity below.

    // * Return W.
    return w;
  }

  /**
   * Appends one or more {@link MessageLevel#FATAL_ERROR} to the given message
   * queue if the skeleton is invalid.
   */
  void checkValidity(WhiteListSkeleton s) {
    // Check for ambiguous type definitions.
    for (Map.Entry<String, List<JSONObject>> def : s.definitions.entrySet()) {
      if (def.getValue().size() > 1) {
        JSONObject first = def.getValue().get(0);
        JSONObject second = def.getValue().get(1);
        mq.addMessage(ConfigMessageType.AMBIGUOUS_DEFINITION, src,
                      // "" + first converts to a string of JSON.
                      MessagePart.Factory.valueOf("" + first),
                      MessagePart.Factory.valueOf("" + second));
      }
    }
  }


  WhiteList fromSkeleton(WhiteListSkeleton s) {
    // We check validity here instead of when recursing so that an inheriting
    // WhiteList can resolve ambiguities among inherited WhiteLists.
    checkValidity(s);

    Map<String, WhiteList.TypeDefinition> types
        = new HashMap<String, WhiteList.TypeDefinition>();
    for (Map.Entry<String, List<JSONObject>> def : s.definitions.entrySet()) {
      types.put(def.getKey(), makeTypeDefinition(def.getValue().get(0)));
    }
    return new WhiteListImpl(src.source(), s.allowed, types);
  }

  WhiteList.TypeDefinition makeTypeDefinition(JSONObject def) {
    return new TypeDefinitionImpl(immutable(def));
  }

  JSONObject expectJSONObject(Object obj, String part) throws ParseException {
    return expect(obj, JSONObject.class, part);
  }

  String expectString(Object obj, String part) throws ParseException {
    return expect(obj, String.class, part);
  }

  JSONArray optionalJSONArray(Object obj, String part) throws ParseException {
    return optional(obj, JSONArray.class, part);
  }

  /**
   * Converts a {@code JSONObject} to a {@code Map<String, Object>} where
   * values are JSONObjects, JSONArrays, or JSON primitives.
   */
  static Map<String, Object> immutable(JSONObject json) {
    Map<String, Object> map = new HashMap<String, Object>();
    for (Map.Entry<?, ?> e : ((Map<?, ?>) json).entrySet()) {
      map.put((String) e.getKey(), immutable(e.getValue()));
    }
    return Collections.unmodifiableMap(map);
  }

  /**
   * Converts a {@code JSONArray} to a {@code List<Object>} where
   * values are JSONObjects, JSONArrays, or JSON primitives.
   */
  static List<Object> immutable(JSONArray json) {
    List<Object> list = new ArrayList<Object>();
    for (Object item : json) {
      list.add(immutable(item));
    }
    return Collections.unmodifiableList(list);
  }

  /**
   * Converts a JSON value to an equivalent immutable java representation.
   */
  static Object immutable(Object obj) {
    if (obj instanceof JSONObject) {
      return immutable((JSONObject) obj);
    } else if (obj instanceof JSONArray) {
      return immutable((JSONArray) obj);
    } else if (obj == null || obj instanceof Boolean || obj instanceof Number
               || obj instanceof String) {
      return obj;
    } else {
      throw new SomethingWidgyHappenedError(obj.getClass().getSimpleName());
    }
  }

  /**
   * Abort with a ParseException if obj is not of type clazz,
   * returning obj otherwise.
   */
  <T> T expect(Object obj, Class<T> clazz, String part)
      throws ParseException {
    if (clazz.isInstance(obj)) { return clazz.cast(obj); }
    throw new ParseException(
        new Message(ConfigMessageType.MALFORMED_CONFIG, src,
                    MessagePart.Factory.valueOf(part),
                    MessagePart.Factory.valueOf(String.valueOf(obj))));
  }

  /**
   * Abort with a ParseException if obj is not either null or of type clazz,
   * returning obj otherwise.
   */
  <T> T optional(Object obj, Class<T> clazz, String part)
      throws ParseException {
    if (obj == null) { return null; }
    return expect(obj, clazz, part);
  }

  /**
   * Adds values for {@code key} to the multimap m maintaining the invariant:
   * <ul>
   * <li>{@code m.containsKey(key)} is true iff at least one value has been
   *   added for the key {@code key}.
   * <li>If a value has been added for {@code key}, {@code m.get(key)} contains
   *   all the values added for {@code key} in the order they were added.
   * </ul>
   */
  static <K, V> void multimapAdd(Map<K, List<V>> m, K key, List<V> newValues) {
    if (newValues.isEmpty()) { return; }
    List<V> values = m.get(key);
    if (values == null) {
      m.put(key, values = new ArrayList<V>());
    }
    values.addAll(newValues);
  }
}

class WhiteListSkeleton {
  /** The set of items allowed and not denied. */
  final Set<String> allowed = new HashSet<String>();
  /** The set of items positively denied. */
  final Set<String> denied = new HashSet<String>();
  /** Multimap of item's keys to their type definitions. */
  final Map<String, List<JSONObject>> definitions
      = new HashMap<String, List<JSONObject>>();

  @Override
  public String toString() {
    return "[Skeleton allowed=" + allowed + ", denied=" + denied
        + ", definitions=" + definitions + "]";
  }
}

class WhiteListImpl implements WhiteList {
  private final InputSource src;
  private final Set<String> allowed;
  private final Map<String, TypeDefinition> defs;

  WhiteListImpl(
      InputSource src, Set<String> allowed, Map<String, TypeDefinition> defs) {
    this.src = src;
    this.allowed = Collections.unmodifiableSet(allowed);
    this.defs = Collections.unmodifiableMap(defs);
  }

  public Set<String> allowedItems() { return allowed; }

  public Map<String, TypeDefinition> typeDefinitions() { return defs; }

  @Override
  public String toString() {
    return "[WhiteList " + src.getUri() + "]";
  }
}

class TypeDefinitionImpl implements WhiteList.TypeDefinition {
  private final Map<String, Object> props;

  TypeDefinitionImpl(Map<String, Object> props) {
    this.props = props;
  }

  public Object get(String key, Object defaultValue) {
    Object value = props.get(key);
    if (value != null || props.containsKey(key)) { return value; }
    return defaultValue;
  }

  @Override
  public String toString() {
    return "[TypeDefinition: " + props + "]";
  }
}
