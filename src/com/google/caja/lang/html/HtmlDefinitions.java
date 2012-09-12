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

package com.google.caja.lang.html;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.config.AllowedFileResolver;
import com.google.caja.config.ConfigUtil;
import com.google.caja.config.ImportResolver;
import com.google.caja.lang.html.HTML.Attribute;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlTextEscapingMode;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.html.AttribKey;
import com.google.caja.parser.html.ElKey;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.plugin.LoaderType;
import com.google.caja.plugin.UriEffect;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.PropertyNameQuotingMode;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.tools.BuildCommand;
import com.google.caja.util.Charsets;
import com.google.caja.util.Function;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;

/**
 * Generates a JavaScript tree with tables mapping HTML element and attribute
 * names to information about those elements and attributes.
 *
 * @author mikesamuel@gmail.com
 */
public final class HtmlDefinitions {

  /** Combined to form a bit-field describing an element. */
  public enum EFlag {
    OPTIONAL_ENDTAG(1),
    EMPTY(2),
    CDATA(4),
    RCDATA(8),
    UNSAFE(0x10),
    /**
     * Elements that can be removed from the DOM without changing behavior as
     * long as their children are folded into the element's parent. Should
     * always be paired with UNSAFE.
     */
    FOLDABLE(0x20),
    SCRIPT(0x40),
    STYLE(0x80),
    /**
     * Elements that are unsafe to pass through, but which may be kept in the
     * DOM by renaming. Should always be paired with UNSAFE.
     */
    VIRTUALIZED(0x100),
    ;

    public final int bitMask;

    EFlag(int bitMask) { this.bitMask = bitMask; }
  }

  private static final AttribKey SCRIPT_SRC = AttribKey.forHtmlAttrib(
      ElKey.forHtmlElement("script"), "src");

  private static <U> ExpressionStmt mapFromEnum(
      Iterable<U> entries, String key,
      Function<U, String> keyMaker, Function<U, Integer> valueMaker) {
    FilePosition unk = FilePosition.UNKNOWN;
    List<StringLiteral> keys = Lists.newArrayList();
    List<IntegerLiteral> values = Lists.newArrayList();
    for (U e : entries) {
      // Since enum values are public, we don't want Closure compiler
      // to rewrite them, so we need quoted keys.
      String quoted = StringLiteral.toQuotedValue(keyMaker.apply(e));
      keys.add(new StringLiteral(unk, quoted));
      values.add(new IntegerLiteral(unk, valueMaker.apply(e)));
    }
    return new ExpressionStmt(unk,
        (Expression) QuasiBuilder.substV(
            "html4.@i = { @k*: @v* };",
            "i", new Reference(new Identifier(unk, key)),
            "k", new ParseTreeNodeContainer(keys),
            "v", new ParseTreeNodeContainer(values)));
  }

  public static Block generateJavascriptDefinitions(HtmlSchema schema) {
    final FilePosition unk = FilePosition.UNKNOWN;
    Map<AttribKey, HTML.Attribute.Type> atypes = attributeTypes(schema);
    Map<ElKey, EnumSet<EFlag>> eflags = elementFlags(schema);
    Map<AttribKey, UriEffect> uriEffects = uriEffects(schema);
    Map<AttribKey, LoaderType> ltypes = loaderTypes(schema);

    Block definitions = new Block();
    definitions.appendChild(QuasiBuilder.substV("var html4 = {};"));

    definitions.appendChild(mapFromEnum(
        EnumSet.allOf(HTML.Attribute.Type.class),
        "atype",
        new Function<HTML.Attribute.Type, String>() {
          public String apply(HTML.Attribute.Type f) {
            return f.name();
          }
        },
        new Function<HTML.Attribute.Type, Integer>() {
          public Integer apply(HTML.Attribute.Type f) {
            return A_TYPE_MAP.get(f);
          }
        })
    );

    {
      List<StringLiteral> keys = new ArrayList<StringLiteral>();
      List<IntegerLiteral> values = new ArrayList<IntegerLiteral>();
      for (Map.Entry<AttribKey, HTML.Attribute.Type> e : atypes.entrySet()) {
        AttribKey key = e.getKey();
        if (ElKey.HTML_WILDCARD.equals(key.el)
            || schema.isElementAllowed(key.el)
            // Whitelisted to allow dynamic script loading via proxy
            || SCRIPT_SRC.equals(key)) {
          keys.add(StringLiteral.valueOf(unk, key.toString()));
          values.add(new IntegerLiteral(unk, A_TYPE_MAP.get(e.getValue())));
        }
      }
      definitions.appendChild(new ExpressionStmt(unk, (Expression)
          QuasiBuilder.substV(
              "html4.ATTRIBS = { @k*: @v* };",
              "k", new ParseTreeNodeContainer(keys),
              "v", new ParseTreeNodeContainer(values))));
    }

    definitions.appendChild(mapFromEnum(
        EnumSet.allOf(EFlag.class),
        "eflags",
        new Function<EFlag, String>() {
          public String apply(EFlag f) {
            return f.name();
          }
        },
        new Function<EFlag, Integer>() {
          public Integer apply(EFlag f) {
            return f.bitMask;
          }
        })
    );

    {
      List<StringLiteral> keys = new ArrayList<StringLiteral>();
      List<IntegerLiteral> values = new ArrayList<IntegerLiteral>();
      for (Map.Entry<ElKey, EnumSet<EFlag>> e : eflags.entrySet()) {
        ElKey key = e.getKey();
        int value = 0;
        for (EFlag f : e.getValue()) { value |= f.bitMask; }
        keys.add(StringLiteral.valueOf(unk, key.toString()));
        values.add(new IntegerLiteral(unk, value));
      }
      definitions.appendChild(new ExpressionStmt(unk, (Expression)
          QuasiBuilder.substV(
              "html4.ELEMENTS = { @k*: @v* };",
              "k", new ParseTreeNodeContainer(keys),
              "v", new ParseTreeNodeContainer(values))));
    }

    definitions.appendChild(mapFromEnum(
        EnumSet.allOf(UriEffect.class),
        "ueffects",
        new Function<UriEffect, String>() {
          public String apply(UriEffect f) {
            return f.name();
          }
        },
        new Function<UriEffect, Integer>() {
          public Integer apply(UriEffect f) {
            return A_UEFFECT_MAP.get(f);
          }
        })
    );

    definitions.appendChild(mapFromEnum(
        uriEffects.entrySet(),
        "URIEFFECTS",
        new Function<Entry<AttribKey, UriEffect>, String>() {
          public String apply(Entry<AttribKey, UriEffect> f) {
            return f.getKey().toString();
          }
        },
        new Function<Entry<AttribKey, UriEffect>, Integer>() {
          public Integer apply(Entry<AttribKey, UriEffect> f) {
            return A_UEFFECT_MAP.get(f.getValue());
          }
        })
    );

    definitions.appendChild(mapFromEnum(
        EnumSet.allOf(LoaderType.class),
        "ltypes",
        new Function<LoaderType, String>() {
          public String apply(LoaderType f) {
            return f.name();
          }
        },
        new Function<LoaderType, Integer>() {
          public Integer apply(LoaderType f) {
            return L_TYPE_MAP.get(f);
          }
        })
    );

    definitions.appendChild(mapFromEnum(
        ltypes.entrySet(),
        "LOADERTYPES",
        new Function<Entry<AttribKey, LoaderType>, String>() {
          public String apply(Entry<AttribKey, LoaderType> f) {
            return f.getKey().toString();
          }
        },
        new Function<Entry<AttribKey, LoaderType>, Integer>() {
          public Integer apply(Entry<AttribKey, LoaderType> f) {
            return L_TYPE_MAP.get(f.getValue());
          }
        })
    );

    return definitions;
  }

  /** Maps attribute types to integers for use in the JavaScript output. */
  private static final Map<HTML.Attribute.Type, Integer> A_TYPE_MAP
      = new EnumMap<HTML.Attribute.Type, Integer>(HTML.Attribute.Type.class);
  static {
    // Under no circumstances should this be changed to use Enum.ordinal().
    // This type to integer mapping must stay the same, or version skew
    // will mean that the HTML definitions JavaScript can only be used with
    // the same version of the cajoler that cajoled the gadget.
    A_TYPE_MAP.put(HTML.Attribute.Type.NONE, 0);
    A_TYPE_MAP.put(HTML.Attribute.Type.URI, 1);
    A_TYPE_MAP.put(HTML.Attribute.Type.SCRIPT, 2);
    A_TYPE_MAP.put(HTML.Attribute.Type.STYLE, 3);
    A_TYPE_MAP.put(HTML.Attribute.Type.ID, 4);
    A_TYPE_MAP.put(HTML.Attribute.Type.IDREF, 5);
    A_TYPE_MAP.put(HTML.Attribute.Type.IDREFS, 6);
    A_TYPE_MAP.put(HTML.Attribute.Type.GLOBAL_NAME, 7);
    A_TYPE_MAP.put(HTML.Attribute.Type.LOCAL_NAME, 8);
    A_TYPE_MAP.put(HTML.Attribute.Type.CLASSES, 9);
    A_TYPE_MAP.put(HTML.Attribute.Type.FRAME_TARGET, 10);
    A_TYPE_MAP.put(HTML.Attribute.Type.URI_FRAGMENT, 11);
    for (HTML.Attribute.Type t : HTML.Attribute.Type.values()) {
      if (!A_TYPE_MAP.containsKey(t)) {
        throw new IllegalStateException("Not all Attribute Types mapped");
      }
    }
  }

  /** Maps urieffects to integers for use in the JavaScript output. */
  private static final Map<UriEffect, Integer> A_UEFFECT_MAP
      = new EnumMap<UriEffect, Integer>(UriEffect.class);
  static {
    // Under no circumstances should this be changed to use Enum.ordinal().
    // This type to integer mapping must stay the same, or version skew
    // will mean that the HTML definitions JavaScript can only be used with
    // the same version of the cajoler that cajoled the gadget.
    A_UEFFECT_MAP.put(UriEffect.NOT_LOADED, 0);
    A_UEFFECT_MAP.put(UriEffect.SAME_DOCUMENT, 1);
    A_UEFFECT_MAP.put(UriEffect.NEW_DOCUMENT, 2);
    for (UriEffect u : UriEffect.values()) {
      if (!A_UEFFECT_MAP.containsKey(u)) {
        throw new IllegalStateException("Not all UriEffects mapped");
      }
    }
  }

  /** Maps attribute types to integers for use in the JavaScript output. */
  private static final Map<LoaderType, Integer> L_TYPE_MAP
      = new EnumMap<LoaderType, Integer>(LoaderType.class);
  static {
    // Under no circumstances should this be changed to use Enum.ordinal().
    // This type to integer mapping must stay the same, or version skew
    // will mean that the HTML definitions JavaScript can only be used with
    // the same version of the cajoler that cajoled the gadget.
    L_TYPE_MAP.put(LoaderType.DATA, 0);
    L_TYPE_MAP.put(LoaderType.SANDBOXED, 1);
    L_TYPE_MAP.put(LoaderType.UNSANDBOXED, 2);
    for (LoaderType t : LoaderType.values()) {
      if (!L_TYPE_MAP.containsKey(t)) {
        throw new IllegalStateException("Not all Loader Types mapped");
      }
    }
  }

  public static int getJavascriptValueForAType(HTML.Attribute.Type atype) {
    return A_TYPE_MAP.get(atype);
  }

  public static int getJavascriptValueForUEffect(UriEffect uEffect) {
    return A_UEFFECT_MAP.get(uEffect);
  }

  public static int getJavascriptValueForLType(LoaderType ltype) {
    return L_TYPE_MAP.get(ltype);
  }

  private static Map<AttribKey, HTML.Attribute.Type> attributeTypes(
      HtmlSchema schema) {
    Map<AttribKey, HTML.Attribute.Type> attributeFlags = Maps.newTreeMap();
    for (AttribKey attribKey : schema.getAttributeNames()) {
      if (schema.isAttributeAllowed(attribKey)) {
        HTML.Attribute a = schema.lookupAttribute(attribKey);
        HTML.Attribute.Type type = a.getType();
        attributeFlags.put(attribKey, type);
      }
    }
    return attributeFlags;
  }

  private interface SchemaExtractor<Result> { Result extract(HTML.Attribute attr); }

  private static <A> Map<AttribKey, A> deriveMapFromSchema(
      HtmlSchema schema, SchemaExtractor<A> extractor) {
    Map<AttribKey, A> result = Maps.newTreeMap();
    for (AttribKey attribKey : schema.getAttributeNames()) {
      if (schema.isAttributeAllowed(attribKey)) {
        HTML.Attribute a = schema.lookupAttribute(attribKey);
        A type = extractor.extract(a);
        if (null != type) {
          result.put(attribKey, type);
        }
      }
    }
    return result;
  }

  private static Map<AttribKey, UriEffect> uriEffects(
      HtmlSchema schema) {
    return deriveMapFromSchema(schema, new SchemaExtractor<UriEffect>() {
      public UriEffect extract(Attribute attr) {
        return attr.getUriEffect();
      }
    });
  }

  private static Map<AttribKey, LoaderType> loaderTypes(
      HtmlSchema schema) {
    return deriveMapFromSchema(schema, new SchemaExtractor<LoaderType>() {
      public LoaderType extract(Attribute attr) {
        return attr.getLoaderType();
      }
    });
  }

  private static Map<ElKey, EnumSet<EFlag>> elementFlags(HtmlSchema schema) {
    final ElKey SCRIPT = ElKey.forHtmlElement("script");
    final ElKey STYLE = ElKey.forHtmlElement("style");
    Map<ElKey, EnumSet<EFlag>> elementFlags = Maps.newTreeMap();
    for (ElKey elementName : schema.getElementNames()) {
      HTML.Element el = schema.lookupElement(elementName);
      EnumSet<EFlag> flags = EnumSet.noneOf(EFlag.class);
      if (el.isEndTagOptional()) { flags.add(EFlag.OPTIONAL_ENDTAG); }
      if (el.isEmpty()) { flags.add(EFlag.EMPTY); }
      if (elementName.isHtml()) {
        switch (HtmlTextEscapingMode.getModeForTag(elementName.localName)) {
          case CDATA:
            flags.add(EFlag.CDATA);
            break;
          case RCDATA:
            flags.add(EFlag.RCDATA);
            break;
          default: break;
        }
      }
      if (!schema.isElementAllowed(elementName)) {
        flags.add(EFlag.UNSAFE);
        if (SCRIPT.equals(elementName)) {
          flags.add(EFlag.SCRIPT);
        } else if (STYLE.equals(elementName)) {
          flags.add(EFlag.STYLE);
        }
      }
      if (HtmlSchema.isElementFoldable(elementName)) {
        flags.add(EFlag.FOLDABLE);
      }
      if (HtmlSchema.isElementVirtualized(elementName)) {
        flags.add(EFlag.VIRTUALIZED);
      }
      elementFlags.put(elementName, flags);
    }
    return elementFlags;
  }

  public static class Builder implements BuildCommand {
    public boolean build(List<File> inputs, List<File> deps, Map<String, Object> options,
        File output) throws IOException {
      File elementsFile = null;
      File attrsFile = null;
      for (File input : inputs) {
        if (input.getName().endsWith(".json")) {
          if (elementsFile == null) {
            elementsFile = input;
          } else if (attrsFile == null) {
            attrsFile = input;
          } else {
            throw new IOException("Unused input " + input);
          }
        }
      }
      if (elementsFile == null) {
        throw new IOException("No JSON whitelist for HTML elements");
      }
      if (attrsFile == null) {
        throw new IOException("No JSON whitelist for HTML attributes");
      }

      FilePosition elements = FilePosition.startOfFile(new InputSource(
          elementsFile.getAbsoluteFile().toURI()));
      FilePosition attrs = FilePosition.startOfFile(new InputSource(
          attrsFile.getAbsoluteFile().toURI()));

      MessageContext mc = new MessageContext();
      mc.addInputSource(elements.source());
      mc.addInputSource(attrs.source());
      MessageQueue mq = new EchoingMessageQueue(
          new PrintWriter(new OutputStreamWriter(System.err), true), mc, false);

      Set<File> inputsAndDeps = new HashSet<File>();
      for (File f : inputs) { inputsAndDeps.add(f.getAbsoluteFile()); }
      for (File f : deps) { inputsAndDeps.add(f.getAbsoluteFile()); }

      ImportResolver resolver = new AllowedFileResolver(inputsAndDeps);

      HtmlSchema schema;
      try {
        schema = new HtmlSchema(
            ConfigUtil.loadWhiteListFromJson(
                elements.source().getUri(), resolver, mq),
            ConfigUtil.loadWhiteListFromJson(
                attrs.source().getUri(), resolver, mq));
      } catch (ParseException ex) {
        ex.toMessageQueue(mq);
        throw (IOException) new IOException("Failed to parse schema")
            .initCause(ex);
      }

      Writer out = new OutputStreamWriter(
          new FileOutputStream(output), Charsets.UTF_8.name());
      String currentDate = "" + new Date();
      if (currentDate.indexOf("*/") >= 0) {
        throw new SomethingWidgyHappenedError("Date should not contain '*/'");
      }
      out.write("// Copyright Google Inc.\n");
      out.write("// Licensed under the Apache Licence Version 2.0\n");
      out.write("// Autogenerated at " + currentDate + "\n");
      out.write("// @overrides window\n");
      out.write("// @provides html4\n");
      try {
        Block node = generateJavascriptDefinitions(schema);
        RenderContext rc = new RenderContext(node.makeRenderer(out, null))
            .withPropertyNameQuotingMode(
                PropertyNameQuotingMode.PRESERVE_QUOTES);
        for (Statement s : node.children()) {
          s.render(rc);
          if (!s.isTerminal()) { rc.getOut().consume(";"); }
        }
        rc.getOut().noMoreTokens();
        out.write("\n");
        out.write("// exports for Closure Compiler\n");
        out.write("html4['ATTRIBS'] = html4.ATTRIBS;\n");
        out.write("html4['ELEMENTS'] = html4.ELEMENTS;\n");
        out.write("html4['URIEFFECTS'] = html4.URIEFFECTS;\n");
        out.write("html4['LOADERTYPES'] = html4.LOADERTYPES;\n");
        out.write("html4['atype'] = html4.atype;\n");
        out.write("html4['eflags'] = html4.eflags;\n");
        out.write("html4['ltypes'] = html4.ltypes;\n");
        out.write("html4['ueffects'] = html4.ueffects;\n");
        out.write("if (typeof window !== 'undefined') {\n");
        out.write("  window['html4'] = html4;\n");
        out.write("}\n");
      } finally {
        out.close();
      }
      return true;
    }
  }

  public static void main(String[] args) {
    HtmlSchema schema = HtmlSchema.getDefault(new SimpleMessageQueue());
    Block node = generateJavascriptDefinitions(schema);
    RenderContext rc = new RenderContext(node.makeRenderer(System.out, null));
    for (Statement s : node.children()) {
      s.render(rc);
      if (!s.isTerminal()) { rc.getOut().consume(";"); }
    }
    rc.getOut().noMoreTokens();
  }
}
