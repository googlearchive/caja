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

package com.google.caja.html;

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

    public enum Type {
      NONE,
      TABLE,
      ;
    }

    private final String name_;
    private final Type type_;
    private final boolean empty_;
    private final boolean optionalEndTag_;
    private final boolean breaksFlow_;

    /** Construct an Element */
    public Element(String name, Type type, boolean empty,
                   boolean optionalEndTag, boolean breaksFlow) {
      assert name.equals(name.toLowerCase());
      assert type != null;
      this.name_ = name;
      this.type_ = type;
      this.empty_ = empty;
      this.optionalEndTag_ = optionalEndTag;
      this.breaksFlow_ = breaksFlow;
    }

    /** Name of the element, in lowercase, e.g. "a", "br" */
    public String getName() {
      return name_;
    }

    /** Type, e.g. TABLE_TYPE */
    public Type getType() {
      return type_;
    }

    /** True if it's empty, has no inner elements or end tag */
    public boolean isEmpty() {
      return empty_;
    }

    /** True if the end tag is optional */
    public boolean isEndTagOptional() {
      return optionalEndTag_;
    }

    /** True if it breaks the flow, and may force a new line before/after
     * the tag */
    public boolean breaksFlow() {
      return breaksFlow_;
    }

    /**
     * @return just name, not proper HTML
     */
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
      NONE,
      URI,
      SCRIPT,
      ;
    }

    /** Name of the element, e.g. "HREF" */
    private final String name_;

    /** Type of the attribute value, e.g. URI */
    private final Type type_;

    /** Construct an Attribute */
    public Attribute(String name, Type type) {
      assert name.toLowerCase().equals(name);
      assert type != null;
      this.name_ = name;
      this.type_ = type;
    }

    /** Gets the name of the attribute, in lowercase */
    public String getName() {
      return name_;
    }

    /** Gets the type, e.g. URI_TYPE */
    public Type getType() {
      return type_;
    }

    /**
     * @return Element name (name only, not proper HTML).
     */
    public String toString() {
      return name_;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o instanceof HTML.Attribute) {
        HTML.Attribute that = (HTML.Attribute) o;
        return this.name_.equals(that.name_);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return this.name_.hashCode();
    }
  }
}
