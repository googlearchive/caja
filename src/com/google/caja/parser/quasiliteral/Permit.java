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

package com.google.caja.parser.quasiliteral;

import java.util.HashMap;
import java.util.Map;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Reference;

/**
 * Answer queries about what static paths are permitted, and report
 * which static paths were checked.
 *
 * @author erights
 */
final class Permit {
  final private Map<String, Permit> permitsUsed;
  final private PermitTemplate template;

  Permit() {
    this(PermitTemplate.DefaultTemplate);
  }

  Permit(PermitTemplate template) {
    permitsUsed = new HashMap<String, Permit>();
    this.template = template;
  }

  private Permit has(String name) {
    Permit result = permitsUsed.get(name);
    if (null != result) {
      return result;
    }
    PermitTemplate subTemplate = template.templates.get(name);
    if (null != subTemplate) {
      result = new Permit(subTemplate);
      permitsUsed.put(name, result);
      return result;
    }
    return null;
  }

  Permit canRead(ParseTreeNode path) {
    if (path instanceof Reference) {
      return has(((Reference)path).getIdentifierName());
    }
    if (path instanceof Identifier) {
      return has(((Identifier)path).getName());
    }
    // TODO(erights): Add case for dotted path expression.
    return null;
  }

  Permit canCall(ParseTreeNode path) {
    Permit p = canRead(path);
    if (null == p) { return null; }
    return p.has("()");
  }

  public String getPermitsUsedAsJSONString() {
    StringBuilder myBuf = new StringBuilder();
    getPermitsUsedAsJSONString(myBuf, ",\n  ");
    return myBuf.toString();
  }

  private void getPermitsUsedAsJSONString(StringBuilder myBuf, String sep) {
    myBuf.append("{");
    boolean first = true;
    for (Map.Entry<String, Permit> assoc : permitsUsed.entrySet()) {
      if (!first) {
        myBuf.append(sep);
      }
      // TODO(erights): encode (or at least sanitize) key
      myBuf.append("\"").append(assoc.getKey()).append("\":");
      assoc.getValue().getPermitsUsedAsJSONString(myBuf, sep + "  ");
      first = false;
    }
    myBuf.append("}");
  }
}
