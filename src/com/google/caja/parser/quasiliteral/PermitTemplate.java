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

/**
 * Represent which static access paths are presumed safe.
 * <p>
 * Only {@class Permit} should access PermitTemplate directly. All other
 * inquiries should go through Permit.
 *
 * @author erights
 */
final class PermitTemplate {

  final static PermitTemplate CanRead = new PermitTemplate();
  final static PermitTemplate CanCall = new PermitTemplate("()", CanRead);

  final Map<String, PermitTemplate> templates;

  PermitTemplate(Object... pairs) {
    Map<String, PermitTemplate> templates =
      new HashMap<String, PermitTemplate>();
    for (int i = 0; i < pairs.length; i += 2) {
      templates.put((String)pairs[i], (PermitTemplate)pairs[i+1]);
    }
    this.templates = templates;
  }

  // TODO(erights): to be read in from a JSON config
  static PermitTemplate DefaultTemplate = new PermitTemplate(
      "valija", new PermitTemplate(
          "typeOf", CanCall,
          "instanceOf", CanCall,
          "read", CanCall,
          "set", CanCall,
          "callFunc", CanCall,
          "callMethod", CanCall,
          "construct", CanCall,
          "getOuters", CanCall,
          "readOuter", CanCall,
          "setOuter", CanCall,
          "initOuter", CanCall,
          "remove", CanCall,
          "keys", CanCall,
          "canReadRev", CanCall,
          "dis", CanCall),
      "caja", new PermitTemplate(
          "log", CanCall,
          "fail", CanCall,
          "enforce", CanCall,
          "enforceType", CanCall,
          "enforceNat", CanCall,
          "directConstructor", CanCall,
          "getFuncCategory", CanCall,
          "isDirectInstanceOf", CanCall,
          "isInstanceOf", CanCall,
          "isRecord", CanCall,
          "isArray", CanCall,
          "isJSONContainer", CanCall,
          "freeze", CanCall,
          "copy", CanCall,
          "snapshot", CanCall,
          "canReadPub", CanCall,
          "readPub", CanCall,
          "canEnumPub", CanCall,
          "canEnumOwn", CanCall,
          "canInnocentEnum", CanCall,
          "BREAK", CanCall,
          "each", CanCall,
          "canCallPub", CanCall,
          "callPub", CanCall,
          "canSetPub", CanCall,
          "setPub", CanCall,
          "canDeletePub", CanCall,
          "deletePub", CanCall,
          "hasTrademark", CanCall,
          "guard", CanCall,
          "makeSealerUnsealerPair", CanCall,
          "def", CanCall,
          "USELESS", CanCall,
          "manifest", CanCall,
          "construct", CanCall,
          "newTable", CanCall,
          "inheritsFrom", CanCall,
          "getSuperCtor", CanCall,
          "getOwnPropertyNames", CanCall,
          "getMethodNames", CanCall,
          "beget", CanCall),
      "null", CanRead,
      "false", CanRead,
      "true", CanRead,
      "NaN", CanRead,
      "Infinity", CanRead,
      "undefined", CanRead,
      "parseInt", CanCall,
      "parseFloat", CanCall,
      "isNaN", CanCall,
      "isFinite", CanCall,
      "decodeURI", CanCall,
      "decodeURIComponent", CanCall,
      "encodeURI", CanCall,
      "encodeURIComponent", CanCall,
      "Math", new PermitTemplate(
          "E", CanRead,
          "LN10", CanRead,
          "LN2", CanRead,
          "LOG2E", CanRead,
          "LOG10E", CanRead,
          "PI", CanRead,
          "SQRT1_2", CanRead,
          "SQRT2", CanRead,
          "abs", CanCall,
          "acos", CanCall,
          "asin", CanCall,
          "atan", CanCall,
          "atan2", CanCall,
          "ceil", CanCall,
          "cos", CanCall,
          "exp", CanCall,
          "floor", CanCall,
          "log", CanCall,
          "max", CanCall,
          "min", CanCall,
          "pow", CanCall,
          "random", CanCall,
          "round", CanCall,
          "sin", CanCall,
          "sqrt", CanCall,
          "tan", CanCall),
      "Object", CanCall,
      "Array", new PermitTemplate(
          "()", CanRead,
          "slice", CanCall),
      "String", new PermitTemplate(
          "()", CanRead,
          "fromCharCode", CanCall),
      "Boolean", CanCall,
      "Number", new PermitTemplate(
          "()", CanRead,
          "MAX_VALUE", CanRead,
          "MIN_VALUE", CanRead,
          "NaN", CanRead,
          "NEGATIVE_INFINITY", CanRead,
          "POSITIVE_INFINITY", CanRead),
      "Date", new PermitTemplate(
          "()", CanRead,
          "parse", CanCall,
          "UTC", CanCall),
      "RegExp", CanCall,
      "Error", CanCall,
      "EvalError", CanCall,
      "RangeError", CanCall,
      "ReferenceError", CanCall,
      "SyntaxError", CanCall,
      "TypeError", CanCall,
      "URIError", CanCall);
}
