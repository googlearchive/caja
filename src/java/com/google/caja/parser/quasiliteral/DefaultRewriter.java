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

package com.google.caja.parser.quasiliteral;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Conditional;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.Loop;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.ThrowStmt;
import com.google.caja.parser.js.TryStmt;
import com.google.caja.parser.js.UndefinedLiteral;
import com.google.caja.plugin.ReservedNames;
import com.google.caja.plugin.SyntheticNodes;
import com.google.caja.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rewrites a JavaScript parse tree to comply with Caja rules.
 *
 * <p>This class is separate from its superclass to (a) make the split between "rules" and
 * plumbing a bit clearer; and (b) allow us to experiment with alternative sets of rules if
 * we so choose.
 *
 * <p>TODO(ihab.awad): All exceptions must be CajaExceptions.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class DefaultRewriter extends AbstractRewriter {
  private ParseTreeNode getFunctionHeadDeclarations(Scope scope) {
    List<ParseTreeNode> stmts = new ArrayList<ParseTreeNode>();

    if (scope.hasFreeArguments()) {
      stmts.add(substV(
          "la", new Identifier(ReservedNames.LOCAL_ARGUMENTS),
          "ga", new Reference(new Identifier(ReservedNames.ARGUMENTS)),
          "var @la = ___.args(@ga);"));
    }
    if (scope.hasFreeThis()) {
      stmts.add(substV(
          "lt", new Identifier(ReservedNames.LOCAL_THIS),
          "gt", new Reference(new Identifier(ReservedNames.THIS)),
          "var @lt = @gt;"));
    }

    return new ParseTreeNodeContainer(stmts);
  }

  private Pair<ParseTreeNode, ParseTreeNode> reuse(
      String variableName,
      ParseTreeNode value,
      Scope scope) {
    return new Pair<ParseTreeNode, ParseTreeNode>(
        new Reference(new Identifier(variableName)),
        substV(
            "ref", new Identifier(variableName),
            "rhs", expand(value, scope),
            "var @ref = @rhs;"));    
  }

  private Pair<ParseTreeNode, ParseTreeNode> reuseAll(
      ParseTreeNode arguments,
      Scope scope) {
    List<ParseTreeNode> refs = new ArrayList<ParseTreeNode>();
    List<ParseTreeNode> rhss = new ArrayList<ParseTreeNode>();

    for (int i = 0; i < arguments.children().size(); i++) {
      Pair<ParseTreeNode, ParseTreeNode> p = reuse(
          "x" + i + "___",
          arguments.children().get(i),
          scope);
      refs.add(p.a);
      rhss.add(p.b);
    }

    return new Pair<ParseTreeNode, ParseTreeNode>(
        new ParseTreeNodeContainer(refs),
        new ParseTreeNodeContainer(rhss));
  }

  private ParseTreeNode expandDef(
      ParseTreeNode symbol,
      ParseTreeNode value,
      Scope scope) {
    if (!(symbol instanceof Reference)) {
      throw new RuntimeException("expandDef on non-Reference: " + symbol);
    }
    String name = getReferenceName(symbol);
    return scope.isGlobal(name) || !scope.isDefined(name) ?
        new ExpressionStmt((Expression)substV(
            "s", symbol,
            "v", value,
            "___OUTERS___.@s = @v")) :
        substV(
            "s", symbol.children().get(0),
            "v", value,
            "var @s = @v");
  }

  private ParseTreeNode expandMember(
      ParseTreeNode fname,
      ParseTreeNode member,
      Scope scope) {
    if (!scope.isDeclaredFunction(getReferenceName(fname))) {
      throw new RuntimeException("Internal: not statically a function name: " + fname);
    }

    Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();

    if (match(member, bindings, "function(@ps*) { @bs*; }")) {
      Scope s2 = new Scope(scope, (FunctionConstructor)member);
      if (s2.hasFreeThis()) {
        return substV(
            "fname", fname,
            "ps",    bindings.get("ps"),
            "bs",    expand(bindings.get("bs"), s2),
            "fh",    getFunctionHeadDeclarations(s2),
            "___.method(@fname, function(@ps*) {" +
            "  @fh*;" +
            "  @bs*;" +
            "});");
      }
    }

    return expand(member, scope);
  }

  private ParseTreeNode expandMemberMap(
      ParseTreeNode clazz,
      ParseTreeNode rhsValue,
      Scope scope) {
    return null;  // TODO(ihab.awad) -- what's to do here?
  }

  private boolean isSynthetic(ParseTreeNode node) {
    Boolean value = node.getAttributes().get(SyntheticNodes.SYNTHETIC);
    return value != null && value.booleanValue();
  }

  private String getReferenceName(ParseTreeNode ref) {
    return ((Reference)ref).getIdentifierName();
  }

  private String getIdentifierName(ParseTreeNode id) {
    return ((Identifier)id).getValue();
  }

  public DefaultRewriter() {
    // TODO(ihab.awad): BUG: Throw CajaException since these will eventually bubble
    // up as "compiler" error messages against user code.

    ////////////////////////////////////////////////////////////////////////
    // Do nothing if the node is already the result of some translation
    ////////////////////////////////////////////////////////////////////////

    addRule("synthetic0", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (isSynthetic(node)) {
          if (node instanceof FunctionConstructor) {
            scope = new Scope(scope, (FunctionConstructor)node);
          }
          return expandAll(node, scope);
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // with - disallow the 'with' construct
    ////////////////////////////////////////////////////////////////////////

    addRule("with0", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        // Our parser does not recognize "with" at all.
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // variable - variable name handling
    ////////////////////////////////////////////////////////////////////////

    addRule("variable0", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, ReservedNames.ARGUMENTS)) {
          return subst(bindings, ReservedNames.LOCAL_ARGUMENTS);
        }
        return NONE;
      }
    });

    addRule("variable1", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, ReservedNames.THIS)) {
          return subst(bindings, ReservedNames.LOCAL_THIS);
        }
        return NONE;
      }
    });

    addRule("variable2", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@x__")) {
          throw new RuntimeException("Variables cannot end in \"__\": " + node);
        }
        return NONE;
      }
    });

    addRule("variable3", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@x_")) {
          String symbol = ((Identifier)bindings.get("x")).getValue() + "_";
          if (scope.isGlobal(symbol)) {
            throw new RuntimeException("Globals cannot end in \"_\": " + node);
          }
        }
        return NONE;
      }
    });

    addRule("variable4", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@x") &&
            bindings.get("x") instanceof Reference) {
          String name = getReferenceName(bindings.get("x"));
          if (scope.isDeclaredFunction(name)) {
            // TODO(ihab.awad): Cannot execute this rule or else every time we call "new Foo()", we
            // end up bombing out. This maybe should be after the "new" rules. :/
            // throw new RuntimeException("Constructors are not first class: " + node);
          }
        }
        return NONE;
      }
    });

    addRule("variable5", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@x") &&
            bindings.get("x") instanceof Reference) {
          String name = getReferenceName(bindings.get("x"));
          if (scope.isFunction(name)) {
            // TODO(ihab.awad): Figure out how to implement this without repeating variable6
            return (scope.isGlobal(name) || !scope.isDefined(name))  ?
                substV(
                  "x", bindings.get("x"),
                  "___.primFreeze(___OUTERS___.@x)") :
                substV(
                    "x", bindings.get("x"),
                    "___.primFreeze(@x)");
          }
        }
        return NONE;
      }
    });

    addRule("variable6", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@x") &&
            bindings.get("x") instanceof Reference) {
          String name = getReferenceName(bindings.get("x"));
          if (scope.isGlobal(name) || !scope.isDefined(name)) {
            return subst(
                bindings,
                "___OUTERS___.@x");
          }
        }
        return NONE;
      }
    });

    addRule("variable7", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@x") &&
            bindings.get("x") instanceof Reference) {
          return bindings.get("x");
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // read - reading values
    ////////////////////////////////////////////////////////////////////////

    addRule("read0", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@x.@y__")) {
          throw new RuntimeException("Properties cannot end in \"__\": " + bindings.get("y"));
        }
        return NONE;
      }
    });

    addRule("read1", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "this.@p")) {
          String propertyName = ((Reference)bindings.get("p")).getIdentifierName();
          return substV(
            "p",  bindings.get("p"),
            "fp", new Reference(new Identifier(propertyName + "_canRead___")),
            "rp", new StringLiteral(propertyName),
            "this.@fp ? this.@p : ___.readProp(this, @rp)");
        }
        return NONE;
      }
    });

    addRule("read2", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@x.@y_")) {
          throw new RuntimeException("Public properties cannot end in \"_\": " + bindings.get("y"));
        }
        return NONE;
      }
    });

    addRule("read3", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@o.@p")) {
          String propertyName = ((Reference)bindings.get("p")).getIdentifierName();
          return substV(
              "o",  expand(bindings.get("o"), scope),
              "p",  bindings.get("p"),
              "fp", new Reference(new Identifier(propertyName + "_canRead___")),
              "rp", new StringLiteral("'" + propertyName + "'"),
              "(function() {" +
              "  var x___ = @o;" +
              "  x___.@fp ? x___.@p : ___.readPub(x___, @rp);" +
              "})()");
        }
        return NONE;
      }
    });

    addRule("read4", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "this[@s]")) {
          return substV(
              "s", expand(bindings.get("s"), scope),
              "___.readProp(t___, @s)");
        }
        return NONE;
      }
    });

    addRule("read5", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@o[@s]")) {
          return substV(
              "o", expand(bindings.get("o"), scope),
              "s", expand(bindings.get("s"), scope),
              "___.readPub(@o, @s)");
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // set - assignments
    ////////////////////////////////////////////////////////////////////////

    addRule("set0", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@x.@y__ = @z")) {
          throw new RuntimeException("Properties cannot end in \"__\": " + bindings.get("y"));
        }
        return NONE;
      }
    });

    addRule("set1", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "this.@p = @r")) {
          String propertyName = ((Reference)bindings.get("p")).getIdentifierName();
          return substV(
              "r",  expand(bindings.get("r"), scope),
              "p",  bindings.get("p"),
              "fp", new Reference(new Identifier(propertyName + "_canSet___")),
              "rp", new StringLiteral("'" + propertyName + "'"),
              "(function() {" +
              "  var x___ = @r;" +
              "  t___.@fp ? (t___.@p = x___) : ___.setProp(t___, @rp, x___);" +
              "})()");
        }
        return NONE;
      }
    });

    addRule("set2", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@fname.prototype.@p = @m;")) {
          String fname = getReferenceName(bindings.get("fname"));
          if (scope.isDeclaredFunction(fname)) {
            String propertyName = getReferenceName(bindings.get("p"));
            if (!"constructor".equals(propertyName)) {
              return substV(
                  "fname", bindings.get("fname"),
                  "m",     expandMember(bindings.get("fname"), bindings.get("m"), scope),
                  "rp",    new StringLiteral("'" + propertyName + "'"),
                  "(function() {" +
                  "  var x___ = @m;" +
                  "  ___.setMember(@fname, @rp, x___);" +
                  "})();");
            }
          }
        }
        return NONE;
      }
    });

    addRule("set3", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@x.@y_ = @z")) {
          throw new RuntimeException("Public properties cannot end in \"_\": " + node);
        }
        return NONE;
      }
    });

    addRule("set4", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@fname.prototype = @mm") &&
            scope.isFunction(getReferenceName(bindings.get("fname")))) {
          return substV(
              "fname", bindings.get("fname"),
              "mm", expandMemberMap(bindings.get("fname"), bindings.get("mm"), scope),
              "___.setMemberMap(@fname, @mm)");
        }
        return NONE;
      }
    });

    addRule("set5", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@fname.@p = @r") &&
            bindings.get("fname") instanceof Reference &&
            scope.isFunction(getReferenceName(bindings.get("fname")))) {
          String propertyName = ((Reference)bindings.get("p")).getIdentifierName();
          if (!"Super".equals(propertyName)) {
            return substV(
                "fname", bindings.get("fname"),
                "rp", new StringLiteral("'" + propertyName + "'"),                
                "r", expand(bindings.get("r"), scope),
                "___.setPub(@fname, @rp, @r)");
          }
        }
        return NONE;
      }
    });

    addRule("set6", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@o.@p = @r")) {
          String propertyName = ((Reference)bindings.get("p")).getIdentifierName();
          Pair<ParseTreeNode, ParseTreeNode> po = reuse("x___", bindings.get("o"), scope);
          Pair<ParseTreeNode, ParseTreeNode> pr = reuse("x0___", bindings.get("r"), scope);
          return substV(
              "pName", new StringLiteral("'" + propertyName + "'"),
              "p", bindings.get("p"),
              "pCanSet", new Reference(new Identifier(propertyName + "_canSet___")),
              "poa", po.a,
              "pob", po.b,
              "pra", pr.a,
              "prb", pr.b,
              "(function() {" +
              "  @pob;" +
              "  @prb;" +
              "  @poa.@pCanSet ? (@poa.@p = @pra) : ___.setPub(@poa, @pName, @pra);" +
              "})();");
        }
        return NONE;
      }
    });

    addRule("set7", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "this[@s] = @r")) {
          return substV(
              "s", expand(bindings.get("s"), scope),
              "r", expand(bindings.get("r"), scope),
              "___.setProp(t___, @s, @r)");
        }
        return NONE;
      }
    });

    addRule("set8", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@o[@s] = @r")) {
          return substV(
              "o", expand(bindings.get("o"), scope),
              "s", expand(bindings.get("s"), scope),
              "r", expand(bindings.get("r"), scope),
              "___.setPub(@o, @s, @r)");
        }
        return NONE;
      }
    });

    addRule("set9", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "var @v = @r") &&
            !scope.isFunction(getIdentifierName(bindings.get("v")))) {
          return expandDef(
              new Reference((Identifier)bindings.get("v")),
              expand(bindings.get("r"), scope),
              scope);
        }
        return NONE;
      }
    });

    addRule("set11", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "var @v") &&
            !scope.isFunction(getIdentifierName(bindings.get("v")))) {
          return expandDef(
              new Reference((Identifier)bindings.get("v")),
              new UndefinedLiteral(),
              scope);
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // new - new object creation
    ////////////////////////////////////////////////////////////////////////

    addRule("new0", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "new @ctor(@as*)") &&
            scope.isDeclaredFunction(getReferenceName(bindings.get("ctor")))) {
          return substV(
              "ctor", bindings.get("ctor"),
              "as", expandAll(bindings.get("as"), scope),
              "new (___.asCtor(@ctor))(@as*)");
        }
        return NONE;
      }
    });

    addRule("new1", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "new @f(@as*)")) {
          return substV(
              "f", expand(bindings.get("f"), scope),
              "as", expandAll(bindings.get("as"), scope),
              "new (___.asCtor(@f))(@as*)");
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // call - function calls
    ////////////////////////////////////////////////////////////////////////

    addRule("call0", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@o.@s__(@as*)")) {
          throw new RuntimeException("Selectors cannot end in \"__\": " + bindings.get("s"));
        }
        return NONE;
      }
    });

    addRule("call1", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "this.@m(@as*)")) {
          Pair<ParseTreeNode, ParseTreeNode> aliases =
              reuseAll(bindings.get("as"), scope);
          String methodName = ((Reference)bindings.get("m")).getIdentifierName();
          return substV(
              "as", aliases.b,
              "vs", aliases.a,
              "m",  bindings.get("m"),
              "fm", new Reference(new Identifier(methodName + "_canCall___")),
              "rm", new StringLiteral("'" + methodName + "'"),
              "(function() {" +
              "  @as*;" +
              "  t___.@fm ? this.@m(@vs*) : ___.callProp(t___, @rm, [@vs*]);" +
              "})()");
        }
        return NONE;
      }
    });

    addRule("call2", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@o.@s_(@as*)")) {
          throw new RuntimeException("Public selectors cannot end in \"_\": " + bindings.get("s"));
        }
        return NONE;
      }
    });

    addRule("call3", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "caja.def(@fname, @base)") &&
            scope.isFunction(getReferenceName(bindings.get("fname"))) &&
            scope.isFunction(getReferenceName(bindings.get("base")))) {
          return subst(
              bindings,
              "caja.def(@fname, @base)");
        }
        return NONE;
      }
    });

    addRule("call4", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // TODO(ihab.awad): Make object literal expressions work!!!
        // TODO(ihab.awad): Make "optional" quasis ("?" suffix) work!!!
        if (match(node, bindings, "caja.def(@fname, @base, @mm, @ss" + /* ? */ ")") &&
            scope.isFunction(getReferenceName(bindings.get("fname"))) &&
            scope.isFunction(getReferenceName(bindings.get("base")))) {
          // TODO(ihab.awad): Need match(...) ignoring bindings so don't have to pass dummy value.
          Map<String, ParseTreeNode> b2 = new HashMap<String, ParseTreeNode>();
          if (match(bindings.get("mm"), b2, "{ @x* : @y* }")) {
            return substV(
                "fname", bindings.get("fname"),
                "base", bindings.get("base"),
                "mm", expandMemberMap(bindings.get("fname"), bindings.get("mm"), scope),
                "ss", expandAll(bindings.get("ss"), scope),
                "caja.def(@fname, @base, @mm, @ss" + /* ? */ "");
          }
        }
        return NONE;
      }
    });

    addRule("call5", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@o.@m(@as*)")) {
          Pair<ParseTreeNode, ParseTreeNode> aliases =
              reuseAll(bindings.get("as"), scope);
          String methodName = ((Reference)bindings.get("m")).getIdentifierName();
          return substV(
              "o",  expand(bindings.get("o"), scope),
              "as", aliases.b,
              "vs", aliases.a,
              "m",  bindings.get("m"),
              "fm", new Reference(new Identifier(methodName + "_canCall___")),
              "rm", new StringLiteral("'" + methodName + "'"),
              "(function() {" +
              "  var x___ = @o;" +
              "  @as*;" +
              "  x___.@fm ? x___.@m(@vs*) : ___.callPub(x___, @rm, [@vs*]);" +
              "})()");
        }
        return NONE;
      }
    });

    addRule("call6", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "this[@s](@as*)")) {
          expandEntries(bindings, scope);
          return subst(
              bindings,
              "___.callProp(t___, @s, [@as*])");
        }
        return NONE;
      }
    });

    addRule("call7", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@o[@s](@as*)")) {
          expandEntries(bindings, scope);
          return subst(
              bindings,
              "___.callPub(@o, @s, [@as*])");
        }
        return NONE;
      }
    });

    addRule("call8", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@f(@as*)")) {
          expandEntries(bindings, scope);
          return subst(
              bindings,
              "___.asSimpleFunc(@f)(@as*)");
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // function - function definitions
    ////////////////////////////////////////////////////////////////////////

    addRule("function0", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // Anonymous simple function constructor
        if (match(node, bindings, "function(@ps*) { @bs*; }")) {
          Scope s2 = new Scope(scope, (FunctionConstructor)node);
          if (!s2.hasFreeThis()) {
            return substV(
                "ps", bindings.get("ps"),
                "bs", expand(bindings.get("bs"), s2),
                "fh", getFunctionHeadDeclarations(s2),
                "___.primFreeze(" +
                "  ___.simpleFunc(" +
                "    function(@ps*) {" +
                "      @fh*;" +
                "      @bs*;" +
                "}))");
          }
        }
        return NONE;
      }
    });

    addRule("function1", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // Named simple function declaration
        if (node.getClass() == FunctionDeclaration.class &&
            match(node.children().get(1), bindings, "function @f(@ps*) { @bs*; }")) {
          Scope s2 = new Scope(scope, (FunctionConstructor)node.children().get(1));
          if (!s2.hasFreeThis()) {
            return expandDef(
                new Reference((Identifier)bindings.get("f")),
                substV(
                    "ps", bindings.get("ps"),
                    "bs", expand(bindings.get("bs"), s2),
                    "fh", getFunctionHeadDeclarations(s2),
                    "___.simpleFunc(" +
                    "  function(@ps*) {" +
                    "    @fh*;" +
                    "    @bs*;" +
                    "});"),
                scope);
          }
        }
        return NONE;
      }
    });

    addRule("function2", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // Named simple function constructor
        if (match(node, bindings, getPatternNode("function @f(@ps*) { @bs* }"))) {
          Scope s2 = new Scope(scope, (FunctionConstructor)node);
          if (!s2.hasFreeThis()) {
            return substV(
                "ps", bindings.get("ps"),
                "fh", getFunctionHeadDeclarations(s2),
                "bs", expand(bindings.get("bs"), s2),
                "f",  bindings.get("f"),
                "___.primFreeze(" +
                "  ___.simpleFunc(" +
                "    function @f(@ps*) {" +
                "      @fh*;" +
                "      @bs*;" +
                "}));");
          }
        }        
        return NONE;
      }
    });

    addRule("function3", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "function(@ps*) { @bs*; }")) {
          Scope s2 = new Scope(scope, (FunctionConstructor)node);
          if (s2.hasFreeThis()) {
            throw new RuntimeException("Method in non-method context: " + node);
          }
        }
        return NONE;
      }
    });

    addRule("function4", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // This catches a case where a named function is *not* part of a declaration.
        if (match(node, bindings, "function @f(@ps*) { @bs*; }")) {
          Scope s2 = new Scope(scope, (FunctionConstructor)node);
          if (s2.hasFreeThis()) {
            throw new RuntimeException("Constructor cannot escape: " + node);
          }
        }
        return NONE;
      }
    });

    addRule("function5", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (node instanceof FunctionDeclaration &&
            match(node.children().get(1), bindings, "function @f(@ps*) { @sf.Super.call(this, @as*); @bs*; }")) {
          // The following test checks that Reference "@sf" has the same name as Identifier "@f":
          if (bindings.get("sf").children().get(0).getValue().equals(bindings.get("f").getValue())) {
            if (!new Scope(scope, bindings.get("as")).hasFreeThis()) {
              Scope s2 = new Scope(scope, (FunctionConstructor)node.children().get(1));
              return expandDef(
                  new Reference((Identifier)bindings.get("f")),
                  substV(
                      "ps", bindings.get("ps"),
                      "fh", getFunctionHeadDeclarations(s2),
                      "sf", bindings.get("sf"),
                      "as", expand(bindings.get("as"), s2),
                      "bs", expand(bindings.get("bs"), s2),
                      "th", new Reference(new Identifier(ReservedNames.LOCAL_THIS)),
                      "___.ctor(function(@ps*) {" +
                      "  @fh*;" +
                      "  @sf.Super.call(@th, @as*);" +
                      "  @bs*;" +
                      "});"),
                  scope);
            }
          }
        }
        return NONE;
      }
    });

    addRule("function6", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (node instanceof FunctionDeclaration &&
            match(node.children().get(1), bindings, "function @f(@ps*) { @bs*; }")) {
          Scope s2 = new Scope(scope, (FunctionConstructor)node.children().get(1));
          if (s2.hasFreeThis()) {
            return expandDef(
                new Reference((Identifier)bindings.get("f")),
                substV(
                    "ps", bindings.get("ps"),
                    "fh", getFunctionHeadDeclarations(s2),
                    "bs", expand(bindings.get("bs"), s2),
                    "___.ctor(function(@ps*) {" +
                    "  @fh*;" +
                    "  @bs*;" +
                    "});"),
                scope);
          }
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // map - object literals
    ////////////////////////////////////////////////////////////////////////

    addRule("map0", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        // TODO(ihab.awad): What does "{}" parse to - a Block or an ObjectLiteral?
        if (match(node, bindings, "{}")) {
          return node.clone();
        }
        return NONE;
      }
    });

    // TODO(ihab.awad): What is the specific object literal match/subst syntax...?
    
    addRule("map1", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@key_ : @val, @rests*")) {
          throw new RuntimeException("Key may not end in \"_\": " + bindings.get("key"));
        }
        return NONE;
      }
    });

    addRule("map2", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "{ @key : @val, @rests* }") &&
            bindings.get("key") instanceof Identifier) {
          ParseTreeNode restsNode = expand(
              substV(
                  "rests", bindings.get("rests"),
                  "{ @rests* }"),
              scope);
          return substV(
              "key", bindings.get("key"),
              "val", expand(bindings.get("val"), scope),
              "rests", new ParseTreeNodeContainer(restsNode.children()),
              "{ @key : @val, @rests* }");
        }
        return NONE;
      }
    });

    addRule("map3", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "{ @keyExpr : @val, @rests* }")) {
          throw new RuntimeException("Key expressions not yet supported: " + bindings.get("keyExpr"));
        }
        return NONE;
      }
    });

    ////////////////////////////////////////////////////////////////////////
    // other - things not otherwise covered
    ////////////////////////////////////////////////////////////////////////

    addRule("other0", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@o instanceof @f")) {
          if (scope.isFunction(getReferenceName(bindings.get("f")))) {
            return substV(
                "o", expand(bindings.get("o"), scope),
                "f", expand(bindings.get("f"), scope),
                "@o instanceof @f");
          }
        }
        return NONE;
      }
    });

    addRule("other1", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        Map<String, ParseTreeNode> bindings = new LinkedHashMap<String, ParseTreeNode>();
        if (match(node, bindings, "@o instanceof @f")) {
          throw new RuntimeException("Invoked instanceof on non-function: " + bindings.get("f"));
        }
        return NONE;
      }
    });
      
    ////////////////////////////////////////////////////////////////////////
    // Automatically recurse into some structures
    ////////////////////////////////////////////////////////////////////////

    addRule("recurse0", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof ParseTreeNodeContainer ||
            node instanceof Block ||
            node instanceof CatchStmt ||
            node instanceof Conditional ||
            node instanceof ExpressionStmt ||
            node instanceof Identifier ||
            node instanceof Literal ||
            node instanceof Loop ||
            node instanceof Noop ||
            node instanceof Operation ||
            node instanceof ReturnStmt ||
            node instanceof ThrowStmt ||
            node instanceof TryStmt) {
          return expandAll(node, scope);
        }
        return NONE;
      }
    });

    // TODO(ihab.awad): This is provisional pending object literal impl
    addRule("recurse1", new Rule() {
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof ObjectConstructor) {
          return expandAll(node, scope);
        }
        return NONE;
      }  
    });
  }
}
