// Copyright (C) 2007 Google Inc.
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

import java.util.Map;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.MessageContext;

/**
 * Simple test harness for experimenting with quasiliteral matches during
 * development. This is not part of an automated test suite.
 * 
 * @author ihab.awad@gmail.com
 */
public class MatchExperiments {
  public static void main(String[] argv) throws Exception {
    run("x = @foo;",
        "x = 3;");
    /*
    run("function @f(@ps*) { @f.Super.call(this, @as*); @bs*; }",
    "function foo(x, y, z) { foo.Super.call(this, p, q, r); x = 3; y = 4; z = 5; }");
    */
  }

  private static void run(String pattern, String specimen) throws Exception {
    ParseTreeNode patternNode = MatchTest.parse(pattern);
    System.out.println(format(patternNode));
    ParseTreeNode specimenNode = MatchTest.parse(specimen);
    System.out.println(format(specimenNode));
    
    Map<String, ParseTreeNode> result = patternNode.matchHere(specimenNode);
    
    if (result != null) {
      System.out.println("Match succeeded");
      System.out.println(format(result));
    } else {
      System.out.println("Match failed");
    }
  }
  
  private static String format(ParseTreeNode n) throws Exception {
    MessageContext mc = new MessageContext();
    StringBuilder output = new StringBuilder();
    n.format(mc, output);
    return output.toString();
  }
  
  private static String format(Map<String, ParseTreeNode> map) throws Exception {
    String s = "{\n";
    for (String k : map.keySet()) {
      s += k + " = ";
      s += format(map.get(k));
      s += "\n";
    }
    s += "}";
    return s;
  }
}
