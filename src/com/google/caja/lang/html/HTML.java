// Copyright (C) 2004 Google Inc.
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

import com.google.caja.parser.html.AttribKey;
import com.google.caja.parser.html.ElKey;
import com.google.caja.plugin.UriPolicy;
import com.google.caja.util.Lists;

import java.util.Collections;
import java.util.List;

/**
 * HTML class defines Element and Attribute classes.
 *
 * @author Jing Yee Lim
 */
public final class HTML {

  /**
   * HTML element
   */
  public static final class Element {
    private final ElKey key_;
    private final List<Attribute> attrs_;
    private final boolean empty_;
    private final boolean optionalEndTag_;
    private final boolean containsText_;

    /** Construct an Element */
    public Element(ElKey key, List<Attribute> attrs, boolean empty,
                   boolean optionalEndTag, boolean containsText) {
      assert key != null;
      this.key_ = key;
      this.attrs_ = Collections.unmodifiableList(Lists.newArrayList(attrs));
      this.empty_ = empty;
      this.optionalEndTag_ = optionalEndTag;
      this.containsText_ = containsText;
    }

    public List<Attribute> getAttributes() {
      return attrs_;
    }

    /** Identifies the element, e.g. "a", "br" */
    public ElKey getKey() {
      return key_;
    }

    /** True if it's empty, has no inner elements or end tag */
    public boolean isEmpty() {
      return empty_;
    }

    /** True if the end tag is optional */
    public boolean isEndTagOptional() {
      return optionalEndTag_;
    }

    /** True if the element can contain textual content. */
    public boolean canContainText() {
      return containsText_;
    }

    /**
     * @return just name, not proper HTML
     */
    @Override
    public String toString() {
      return key_.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Element)) { return false; }
      return this.key_.equals(((Element) o).key_);
    }

    @Override
    public int hashCode() {
      return this.key_.hashCode();
    }
  }

  /**
   * HTML attribute
   */
  public static final class Attribute {

    public enum Type {
      /** An attribute of a type not described elsewhere in this enum. */
      NONE,
      /**
       * A URI Reference, possibly relative to the pages base.
       * @see <a href=
       *       "http://www.w3.org/TR/html401/sgml/dtd.html#Script">
       *      The HTML 4.01 spec</a>
       */
      URI,
      /**
       * A URI that can only refer to a {@link #GLOBAL_NAME} or {@link #ID}
       * defined within the document.  Such as {@code #foo}.
       * Syntactically the same as an {@link #IDREF} but with a preceding '#'
       * sign.
       */
      URI_FRAGMENT,
      /**
       * Javascript or vbscript content.
       * @see <a href=
       *       "http://www.w3.org/TR/html401/types.html#h-6.14">
       *      The HTML 4.01 spec</a>
       */
      SCRIPT,
      /**
       * CSS content.
       * @see <a href=
       *       "http://www.w3.org/TR/html401/types.html#h-6.15">
       *      The HTML 4.01 spec</a>
       */
      STYLE,
      /**
       * An ID token
       * @see <a href=
       *       "http://www.w3.org/TR/html401/types.html#type-id">
       *      The HTML 4.01 spec</a>
       */
      ID,
      /**
       * A referencs to an ID token defined by another attribute.
       * @see <a href=
       *       "http://www.w3.org/TR/html401/types.html#type-idref">
       *      The HTML 4.01 spec</a>
       */
      IDREF,
      /**
       * References to ID tokens defined by other attributes.
       * @see <a href=
       *       "http://www.w3.org/TR/html401/types.html#type-idrefs">
       *      The HTML 4.01 spec</a>
       */
      IDREFS,
      /**
       * The semi-unique name of an element that ends up as a global variable in
       * IE.
       */
      GLOBAL_NAME,
      /** The name of an input, param, or meta tag. */
      LOCAL_NAME,
      /** a space separated list of classes. */
      CLASSES,
      /**
       * Name of a frame or a special value like {@code _blank}.
       * @see <a href=
       *       "http://www.w3.org/TR/html401/sgml/loosedtd.html#FrameTarget">
       *      The HTML 4.01 spec</a>
       */
      FRAME_TARGET,
      ;
    }

    private final AttribKey key_;

    /** Type of the attribute value, e.g. URI */
    private final Type type_;

    /** The value the attribute assumes if not specified or null. */
    private final String defaultValue_;

    /** A known safe value for the attribute. */
    private final String safeValue_;

    /**
     * Is the attribute's value irrelevant?
     * The state of the attribute is specified entirely by whether or not it is
     * present.
     */
    private final boolean valueless_;

    /** Is the attribute required on all elements on which it is allowed? */
    private final boolean optional_;

    /** For URI attributes. */
    private final UriPolicy.LoaderType loaderType_;

    /** For URI attributes. */
    private final UriPolicy.UriEffect effect_;

    private final RegularCriterion valueCriterion_;

    /** Construct an Attribute */
    public Attribute(
        AttribKey key, Type type, String defaultValue, String safeValue,
        boolean valueless, boolean optional, UriPolicy.LoaderType loaderType,
        UriPolicy.UriEffect effect, RegularCriterion valueCriterion) {
      assert key != null;
      assert type != null;
      // HACK: null should not be allowed
      assert safeValue == null || valueCriterion.accept(safeValue)
          : ("[" + safeValue + "] for " + key + " with criterion /"
             + valueCriterion.toRegularExpression() + "/");
      assert (type == Type.URI) == (effect != null) : key;
      assert (type == Type.URI) == (loaderType != null) : key;
      this.key_ = key;
      this.type_ = type;
      this.defaultValue_ = defaultValue;
      this.safeValue_ = safeValue;
      this.valueless_ = valueless;
      this.optional_ = optional;
      this.loaderType_ = loaderType;
      this.effect_ = effect;
      this.valueCriterion_ = valueCriterion;
    }

    public AttribKey getKey() { return key_; }

    /** Gets the type, e.g. URI. */
    public Type getType() {
      return type_;
    }

    /** The value the attribute assumes when it is not specified. */
    public String getDefaultValue() {
      return defaultValue_;
    }

    /**
     * A value that is known to match the {@link #getValueCriterion criterion}.
     */
    public String getSafeValue() {
      return safeValue_;
    }


    /** Accepts values that are allowed for this attribute. */
    public RegularCriterion getValueCriterion() {
      return valueCriterion_;
    }

    /**
     * Like the CHECKED attribute on the INPUT element, true iff the
     * attribute's state is specified entirely by whether or not it is
     * present.  True for optional attributes that have only one legal value.
     */
    public boolean isValueless() {
      return valueless_;
    }

    /** True if the attribute is optional. */
    public boolean isOptional() {
      return optional_;
    }

    public UriPolicy.UriEffect getUriEffect() { return effect_; }

    public UriPolicy.LoaderType getLoaderType() { return loaderType_; }

    /**
     * @return attribute name (name only, not proper HTML).
     */
    @Override
    public String toString() {
      return key_.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Attribute)) { return false; }
      Attribute that = (HTML.Attribute) o;
      return this.key_.equals(that.key_);
    }

    @Override
    public int hashCode() {
      return key_.hashCode();
    }
  }
}
