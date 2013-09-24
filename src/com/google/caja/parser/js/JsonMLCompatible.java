// Copyright (C) 2010 Google Inc.
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

package com.google.caja.parser.js;

import com.google.caja.lexer.FilePosition;
import com.google.common.collect.Lists;
import com.google.javascript.jscomp.jsonml.JsonML;
import com.google.javascript.jscomp.jsonml.TagAttr;
import com.google.javascript.jscomp.jsonml.TagType;

import java.util.EnumMap;
import java.util.List;

/**
 * An interface that allows parse tree nodes to be converted to an intermediate
 * format that can then be converted into a parse tree for another tool such as
 * closure compiler.
 *
 * @author Mike Samuel <mikesamuel@gmail.com>
 */
public interface JsonMLCompatible {

  /**
   * Creates a <a href="http://code.google.com/p/es-lab/wiki/JsonMLASTFormat">
   * JsonML representation of this AST</a>.
   */
  JsonML toJsonML();

  /**
   * A builder object.
   */
  public static final class JsonMLBuilder {
    private final TagType type;
    private final EnumMap<TagAttr, Object> attrs = new EnumMap<TagAttr, Object>(
        TagAttr.class);
    private final List<JsonML> children = Lists.newArrayList();

    private JsonMLBuilder(TagType type, FilePosition pos) {
      this.type = type;
      if (!FilePosition.UNKNOWN.equals(pos)) {
        attrs.put(TagAttr.SOURCE, pos.source().getUri().toString());
        int packedPosition = (pos.startCharInFile() << 16)
            | Math.min(0xffff, pos.length());
        attrs.put(TagAttr.OPAQUE_POSITION, packedPosition);
      }
    }

    public static JsonMLBuilder builder(TagType type, FilePosition pos) {
      return new JsonMLBuilder(type, pos);
    }

    public JsonMLBuilder setAttribute(TagAttr a, Object value) {
      assert !attrs.containsKey(a);
      attrs.put(a, value);
      return this;
    }

    public JsonMLBuilder setAttributeIfNotBlank(TagAttr a, Object value) {
      return (value != null && !"".equals(value))
          ? setAttribute(a, value) : this;
    }

    public JsonMLBuilder addChild(JsonML child) {
      assert child != null;
      children.add(child);
      return this;
    }

    public JsonMLBuilder addChild(JsonMLCompatible child) {
      return addChild(child.toJsonML());
    }

    public JsonMLBuilder addChildIfNotNull(JsonMLCompatible child) {
      return child != null ? addChild(child.toJsonML()) : this;
    }

    public JsonMLBuilder addChildren(
        Iterable<? extends JsonMLCompatible> children) {
      for (JsonMLCompatible child : children) {
        addChild(child);
      }
      return this;
    }

    public boolean hasChildren() { return !children.isEmpty(); }

    public JsonML build() {
      return new JsonML(type, attrs, children);
    }
  }
}
