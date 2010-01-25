// Copyright (C) 2009 Google Inc.
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

package com.google.caja.parser.html;

import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.Token;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;

final class AttrStub {
  final Token<HtmlTokenType> nameTok;
  final Token<HtmlTokenType> valueTok;
  final String value;

  AttrStub(Token<HtmlTokenType> nameTok,
           Token<HtmlTokenType> valueTok, String value) {
    this.nameTok = nameTok;
    this.valueTok = valueTok;
    this.value = value;
  }

  Namespaces toNamespace(Namespaces ns, MessageQueue mq) {
    String rawName = nameTok.text;
    if (rawName.startsWith("xmlns")) {
      if (rawName.length() == 5) {
        return new Namespaces(ns, "", value);
      } else if (':' == rawName.charAt(5)) {
        String prefix = rawName.substring(6);
        if ("".equals(prefix) || "xml".equals(prefix)
            || "xmlns".equals(prefix)) {
          mq.addMessage(
              MessageType.ILLEGAL_NAMESPACE_NAME, nameTok.pos,
              MessagePart.Factory.valueOf(prefix));
        } else {
          return new Namespaces(ns, prefix, value);
        }
      }
    }
    return null;
  }

  Attr toAttr(Document doc, String attrUri, String attrQName) {
    Attr attrNode = doc.createAttributeNS(attrUri, attrQName);
    attrNode.setValue(value);
    Nodes.setFilePositionFor(attrNode, nameTok.pos);
    Nodes.setFilePositionForValue(attrNode, valueTok.pos);
    Nodes.setRawValue(attrNode, valueTok.text);
    return attrNode;
  }
}
