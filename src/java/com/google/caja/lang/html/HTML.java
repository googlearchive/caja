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

    private final String name_;
    private final boolean empty_;
    private final boolean optionalEndTag_;

    /** Construct an Element */
    public Element(String name, boolean empty, boolean optionalEndTag) {
      assert name.equals(name.toLowerCase());
      this.name_ = name;
      this.empty_ = empty;
      this.optionalEndTag_ = optionalEndTag;
    }

    /** Name of the element, in lowercase, e.g. "a", "br" */
    public String getName() {
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
      return name_;
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
    private final String elementName_;
    /** Name of the attribute, e.g. "href" */
    private final String attributeName_;

    /** Type of the attribute value, e.g. URI */
    private final Type type_;

    /** Mime-Type for URI attributes. */
    private final String mimeTypes_;

    /** Construct an Attribute */
    public Attribute(String elementName, String attributeName, Type type,
                     String mimeTypes) {
      assert elementName.toLowerCase().equals(elementName);
      assert attributeName.toLowerCase().equals(attributeName);
      assert type != null;
      this.elementName_ = elementName;
      this.attributeName_ = attributeName;
      this.type_ = type;
      this.mimeTypes_ = mimeTypes;
    }

    /** Gets the name of the attribute, in lowercase. */
    public String getAttributeName() {
      return attributeName_;
    }

    /** Gets the name of the element, in lowercase, or the special value "*". */
    public String getElementName() {
      return attributeName_;
    }

    /** Gets the type, e.g. URI. */
    public Type getType() {
      return type_;
    }

    /** The mime-types or null. */
    public String getMimeTypes() {
      return mimeTypes_;
    }

    /**
     * @return attribute name (name only, not proper HTML).
     */
    @Override
    public String toString() {
      return attributeName_;
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
