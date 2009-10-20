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

import com.google.caja.util.Name;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HTML class defines Element and Attribute classes.
 *
 * @author Jing Yee Lim
 */
public final class HTML {

  /**
   * Html element
   */
  public static final class Element {
    private final Name name_;
    private final List<Attribute> attrs_;
    private final boolean empty_;
    private final boolean optionalEndTag_;

    /** Construct an Element */
    public Element(
        Name name, List<Attribute> attrs,
        boolean empty, boolean optionalEndTag) {
      assert name != null;
      this.name_ = name;
      this.attrs_ = Collections.unmodifiableList(
          new ArrayList<Attribute>(attrs));
      this.empty_ = empty;
      this.optionalEndTag_ = optionalEndTag;
    }

    public List<Attribute> getAttributes() {
      return attrs_;
    }

    /** Name of the element, e.g. "a", "br" */
    public Name getName() {
      return name_;
    }

    /** True if it's empty, has no inner elements or end tag */
    public boolean isEmpty() {
      return empty_;
    }

    /** True if the end tag is optional */
    public boolean isEndTagOptional() {
      return optionalEndTag_;
    }

    /**
     * @return just name, not proper HTML
     */
    @Override
    public String toString() {
      return name_.getCanonicalForm();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o instanceof HTML.Element) {
        HTML.Element that = (HTML.Element) o;
        return this.name_.equals(that.name_);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return this.name_.hashCode();
    }
  }

  /**
   * Html attribute
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

    /** Name of the element, e.g. "a" or "*" */
    private final Name elementName_;
    /** Name of the attribute, e.g. "href" */
    private final Name attributeName_;

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

    /** Mime-Type for URI attributes. */
    private final String mimeTypes_;

    private final RegularCriterion valueCriterion_;

    /** Construct an Attribute */
    public Attribute(
        Name elementName, Name attributeName, Type type, String defaultValue,
        String safeValue, boolean valueless, boolean optional, String mimeTypes,
        RegularCriterion valueCriterion) {
      assert elementName != null;
      assert attributeName != null;
      assert type != null;
      // HACK: null should not be allowed
      assert safeValue == null || valueCriterion.accept(safeValue)
          : "[" + safeValue + "] for " + elementName + "::" + attributeName
            + " with criterion /" + valueCriterion.toRegularExpression() + "/";
      this.elementName_ = elementName;
      this.attributeName_ = attributeName;
      this.type_ = type;
      this.defaultValue_ = defaultValue;
      this.safeValue_ = safeValue;
      this.valueless_ = valueless;
      this.optional_ = optional;
      this.mimeTypes_ = mimeTypes;
      this.valueCriterion_ = valueCriterion;
    }

    /** Gets the name of the attribute. */
    public Name getAttributeName() {
      return attributeName_;
    }

    /** Gets the name of the element, or the special value "*". */
    public Name getElementName() {
      return elementName_;
    }

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

    /** The mime-types or null. */
    public String getMimeTypes() {
      return mimeTypes_;
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

    /**
     * @return attribute name (name only, not proper HTML).
     */
    @Override
    public String toString() {
      return elementName_ + "::" + attributeName_;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o instanceof HTML.Attribute) {
        HTML.Attribute that = (HTML.Attribute) o;
        return this.attributeName_.equals(that.attributeName_)
            && this.elementName_.equals(that.elementName_);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return this.attributeName_.hashCode() * 31 + this.elementName_.hashCode();
    }
  }
}
