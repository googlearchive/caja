// Copyright (C) 2006 Google Inc.
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

package com.google.caja.parser.css;

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;

import java.io.IOException;
import java.net.URI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A node in a CSS parse tree.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class CssTree extends AbstractParseTreeNode<CssTree> {
  // TODO(mikesamuel): ensure that the rendered form does not include IE6 style
  // /*nesting /*block*/ comments*/.  Maybe escape asterisks in string literals?

  CssTree(FilePosition pos, List<? extends CssTree> children) {
    this.setFilePosition(pos);
    this.children.addAll(children);
    childrenChanged();
    parentify(false);
  }

  @Override
  public Object getValue() {
    return null;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    try {
      formatSelf(new MessageContext(), sb);
    } catch (IOException ex) {
      throw new AssertionError("StringBuilders shouldn't throw IOExceptions");
    }
    return sb.toString();
  }

  /**
   * The top level parsetree node.
   * <pre>
   * stylesheet
   *   : [ CHARSET_SYM S* STRING S* ';' ]?
   *     [S|CDO|CDC]* [ import [S|CDO|CDC]* ]*
   *     [ [ ruleset | media | page | font_face ] [S|CDO|CDC]* ]*
   * </pre>
   */
  public static final class StyleSheet extends CssTree {
    public StyleSheet(FilePosition pos, List<? extends CssStatement> rulesets) {
      super(pos, rulesets);
    }

    public void render(RenderContext r) throws IOException {
      boolean first = true;
      for (CssTree child : children()) {
        if (!first) {
          r.newLine();
        } else {
          first = false;
        }
        child.render(r);
      }
    }
  }

  /**
   * A root node with no equivalent in the grammar.
   * This node is like a ruleset, but without the selector, so it works as
   * the root node of css parsed from an xhtml <code>style</code> attribute.
   */
  public static final class DeclarationGroup extends CssTree {
    DeclarationGroup(FilePosition pos, List<? extends Declaration> decls) {
      super(pos, decls);
    }

    @Override
    protected void childrenChanged() {
      super.childrenChanged();
      for (CssTree child : children) {
        if (!(child instanceof Declaration)) {
          throw new IllegalArgumentException();
        }
      }
    }

    public void render(RenderContext r) throws IOException {
      boolean first = true;
      for (CssTree t : children()) {
        if (!first) {
          r.out.append(';');
          r.newLine();
        } else {
          first = false;
        }
        t.render(r);
      }
    }
  }

  /** Part of a stylesheet. */
  public abstract static class CssStatement extends CssTree {
    CssStatement(FilePosition pos, List<? extends CssTree> children) {
      super(pos, children);
    }
  }
  /**
   * <pre>
   * import
   *   : IMPORT_SYM S*
   *     [STRING|URI] S* [ medium [ ',' S* medium]* ]? ';' S*
   * </pre>
   */
  public static final class Import extends CssStatement {
    static <T> List<T> join(List<? extends T> a, List<? extends T> b) {
      List<T> l = new ArrayList<T>(a.size() + b.size());
      l.addAll(a);
      l.addAll(b);
      return l;
    }

    Import(FilePosition pos, UriLiteral uri, List<? extends Medium> media) {
      super(pos, join(Collections.singletonList(uri), media));
    }

    public void render(RenderContext r) throws IOException {
      r.out.append("@import ");
      List<? extends CssTree> children = children();
      children.get(0).render(r); // the uri
      children = children.subList(1, children.size());

      if (!children.isEmpty()) {  // the media
        r.out.append(' ');
        renderCommaGroup(children(), r);
      }
      r.out.append(";");
    }
  }

  /**
   * <pre>
   * media
   *   : MEDIA_SYM S* medium [ ',' S* medium ]* '{' S* ruleset* '}' S*
   * </pre>
   */
  public static final class Media extends CssStatement {
    Media(FilePosition pos, List<? extends CssTree> mediaAndRuleset) {
      super(pos, mediaAndRuleset);
    }
    public void render(RenderContext r) throws IOException {
      r.out.append("@media ");
      List<? extends CssTree> children = children();
      int i = 0;
      while (i < children.size() && children.get(i) instanceof Medium) { ++i; }
      renderCommaGroup(children.subList(0, i), r);
      r.out.append(" {");
      r.indent += 2;
      for (CssTree ruleset : children.subList(i, children.size())) {
        r.newLine();
        ruleset.render(r);
      }
      r.indent -= 2;
      r.newLine();
      r.out.append("}");
    }
  }

  /**
   * <pre>
   * medium
   *   : IDENT S*
   * </pre>
   */
  public static final class Medium extends CssTree {
    final String ident;

    Medium(FilePosition pos, String ident) {
      super(pos, Collections.<CssTree>emptyList());
      this.ident = ident;
    }

    @Override
    public String getValue() { return ident; }

    public void render(RenderContext r) throws IOException {
      escapeCssIdent(ident, r.out);
    }
  }

  /**
   * <pre>
   * page
   *   : PAGE_SYM S* IDENT? pseudo_page? S*
   *     '{' S* declaration [ ';' S* declaration ]* '}' S*
   * </pre>
   */
  public static final class Page extends CssStatement {
    final String ident;

    Page(FilePosition pos, String ident, List<? extends PageElement> decls) {
      super(pos, decls);
      this.ident = ident;
    }

    @Override
    public String getValue() { return ident; }

    public void render(RenderContext r) throws IOException {
      r.out.append("@page");
      if (null != ident) {
        r.out.append(' ');
        escapeCssIdent(ident, r.out);
      }
      List<? extends CssTree> children = children();
      if (children.get(0) instanceof PseudoPage) {
        children.get(0).render(r);
        children = children.subList(1, children.size());
      }
      renderStatements(children, r);
    }
  }

  /**
   * A part of a CSS statement.
   */
  public abstract static class PageElement extends CssTree {
    PageElement(FilePosition pos, List<? extends CssTree> rulesets) {
      super(pos, rulesets);
    }
  }

  /**
   * <pre>
   * pseudo_page
   *   : ':' IDENT
   * </pre>
   */
  public static final class PseudoPage extends PageElement {
    final String ident;

    PseudoPage(FilePosition pos, String ident) {
      super(pos, Collections.<CssTree>emptyList());
      this.ident = ident;
    }

    @Override
    public String getValue() { return ident; }

    public void render(RenderContext r) throws IOException {
      r.out.append(':');
      escapeCssIdent(ident, r.out);
    }
  }

  /**
   * <pre>
   * font_face
   *   : FONT_FACE_SYM S*
   *     '{' S* declaration [ ';' S* declaration ]* '}' S*
   * </pre>
   */
  public static final class FontFace extends CssStatement {
    FontFace(FilePosition pos, List<? extends Declaration> decls) {
      super(pos, decls);
    }

    public void render(RenderContext r) throws IOException {
      r.out.append("@font-face");
      renderStatements(children(), r);
    }
  }

  /**
   * <pre>
   * property
   *   : IDENT S*
   * </pre>
   */
  public static final class Property extends CssTree {
    final String ident;

    Property(FilePosition pos, String ident) {
      super(pos, Collections.<CssTree>emptyList());
      this.ident = ident;
    }

    @Override
    public String getValue() { return ident; }

    public String getPropertyName() { return ident; }

    public void render(RenderContext r) throws IOException {
      escapeCssIdent(ident, r.out);
    }
  }

  /**
   * <pre>
   * ruleset
   *   : selector [ ',' S* selector ]*
   *     '{' S* declaration [ ';' S* declaration ]* '}' S*
   * </pre>
   */
  public static final class RuleSet extends CssStatement {
    RuleSet(FilePosition pos, List<? extends CssTree> selectorsAndDecls) {
      super(pos, selectorsAndDecls);
    }

    public void render(RenderContext r) throws IOException {
      int i = 0;
      while (i < children.size() && !(children.get(i) instanceof Declaration)) {
        ++i;
      }
      renderCommaGroup(children.subList(0, i), r);
      renderStatements(children.subList(i, children.size()), r);
    }
  }

  /**
   * <pre>
   * selector
   *   : simple_selector [ combinator simple_selector ]*
   * </pre>
   */
  public static final class Selector extends CssTree {
    Selector(FilePosition pos, List<? extends CssTree> children) {
      super(pos, children);
    }
    public void render(RenderContext r) throws IOException {

      boolean needSpace = false;
      for (CssTree child : children) {
        if (!(child instanceof Combination
              && null == ((Combination) child).getCombinator().symbol)) {
          if (needSpace) { r.out.append(" "); }
          child.render(r);
          needSpace = true;
        }
      }
    }
  }

  /**
   * <pre>
   * simple_selector
   *   : element_name? [ HASH | class | attrib | pseudo ]* S*
   * </pre>
   */
  public static final class SimpleSelector extends CssTree {
    public SimpleSelector(FilePosition pos, List<? extends CssTree> children) {
      super(pos, children);
    }

    public String getElementName() {
      CssTree first = children.get(0);
      if (first instanceof IdentLiteral) {
        return ((IdentLiteral) first).getValue();
      }
      return null;
    }

    public void render(RenderContext r) throws IOException {
      // no spaces between because space is the DESCENDANT operator in Selector
      for (CssTree child : children) { child.render(r); }
    }
  }

  /**
   * <pre>
   * element_name
   *   : IDENT | '*'
   * </pre>
   */
  public static final class WildcardElement extends CssTree {
    public WildcardElement(FilePosition pos) {
      super(pos, Collections.<CssTree>emptyList());
    }

    public void render(RenderContext r) throws IOException {
      // start with a space to make sure that rendering couldn't introduce a
      // comment.  I know of no parse tree that would otherwise do this, but
      // comments could be used to introduce security holes.
      r.out.append(" *");
    }
  }

  /**
   * <pre>
   * attrib
   *   : '[' S* IDENT S* [ [ '=' | INCLUDES | DASHMATCH ] S*
   *     [ IDENT | STRING ] S* ]? ']'
   * </pre>
   */
  public static final class Attrib extends CssTree {
    final String ident;

    Attrib(FilePosition pos, String ident,
           AttribOperation operator, CssLiteral value) {
      super(pos, null == operator
            ? Collections.<CssTree>emptyList()
            : Collections.unmodifiableList(Arrays.asList(operator, value)));
      this.ident = ident;
    }

    @Override
    public String getValue() { return ident; }
    public String getIdent() { return ident; }

    public void render(RenderContext r) throws IOException {
      r.out.append('[');
      escapeCssIdent(ident, r.out);
      if (!children().isEmpty()) {
        r.out.append(' ');
        renderSpaceGroup(children(), r);
      }
      r.out.append(']');
    }
  }

  /** Operator used in {@link Attrib} */
  public static enum AttribOperator {
    EQUAL("="),
    INCLUDES("~="),
    DASHMATCH("|="),
    ;
    private final String op;
    AttribOperator(String op) { this.op = op; }

    public String getToken() { return op; }
  }

  /** <pre>[ '=' | INCLUDES | DASHMATCH ]</pre> */
  public static final class AttribOperation extends CssTree {
    final AttribOperator op;
    AttribOperation(FilePosition pos, AttribOperator op) {
      super(pos, Collections.<CssTree>emptyList());
      this.op = op;
    }

    @Override
    public AttribOperator getValue() { return op; }

    public void render(RenderContext r) throws IOException {
      r.out.append(op.getToken());
    }
  }

  /**
   * <pre>
   * pseudo
   *   : ':' [ IDENT | FUNCTION S* IDENT S* ')' ]
   * </pre>
   */
  public static final class Pseudo extends CssTree {
    Pseudo(FilePosition pos, CssExprAtom child) {
      super(pos, Collections.singletonList(child));
    }

    public void render(RenderContext r) throws IOException {
      r.out.append(':');
      children.get(0).render(r);
    }
  }

  /**
   * <pre>
   * declaration
   *   : property ':' S* expr prio?
   *   | <i>empty</i>
   * </pre>
   */
  public static final class Declaration extends PageElement {
    private Property prop;
    private Expr expr;
    private Prio prio;

    public Declaration(FilePosition pos, List<? extends CssTree> children) {
      super(pos, children);
    }

    @Override
    protected void childrenChanged() {
      super.childrenChanged();
      List<? extends CssTree> children = children();
      if (!children.isEmpty()) {
        prop = (Property) children.get(0);
        expr = (Expr) children.get(1);
        prio = children.size() > 2 ? (Prio) children.get(2) : null;
        assert children.size() <= 3 && null != prop && null != expr;
      } else {
        prop = null;
        expr = null;
        prio = null;
      }
    }

    public Property getProperty() { return prop; }
    public Expr getExpr() { return expr; }
    public Prio getPrio() { return prio; }

    public void render(RenderContext r) throws IOException {
      if (null != prop) {
        prop.render(r);
        r.out.append(": ");
        expr.render(r);
        if (null != prio) {
          r.out.append(' ');
          prio.render(r);
        }
      }
    }
  }

  /**
   * <pre>
   * prio
   *   : IMPORTANT_SYM S*
   * </pre>
   */
  public static final class Prio extends CssTree {
    final String value;
    Prio(FilePosition pos, String value) {
      super(pos, Collections.<CssTree>emptyList());
      this.value = value;
    }

    @Override
    public String getValue() { return value; }

    public void render(RenderContext r) throws IOException {
      r.out.append('!');
      escapeCssIdent(value.substring(1), r.out);
    }
  }

  /**
   * <pre>
   * expr
   *   : term [ operator term ]*
   * </pre>
   */
  public static final class Expr extends CssTree {
    public Expr(FilePosition pos, List<? extends CssTree> children) {
      super(pos, children);
    }

    @Override
    protected void childrenChanged() {
      super.childrenChanged();
      assert 1 == children().size() % 2;
    }

    public int getNTerms() { return (children().size() + 1) >> 1; }
    public Term getNthTerm(int n) { return (Term) children().get(n * 2); }
    public Operation getNthOperation(int n) {
      return (Operation) children().get(1 + n * 2);
    }
    public Operator getNthOperator(int n) {
      return ((Operation) children().get(1 + n * 2)).getOperator();
    }

    public void render(RenderContext r) throws IOException {
      boolean needSpace = false;
      for (CssTree child : children) {
        if (!(child instanceof Operation
              && null == ((Operation) child).getOperator().symbol)) {
          if (needSpace) {
            r.out.append(" ");
          }
          child.render(r);
          needSpace = true;
        }
      }
    }
  }

  /**
   * <pre>
   * term
   *   : unary_operator?
   *     [ NUMBER S* | PERCENTAGE S* | LENGTH S* | EMS S* | EXS S* | ANGLE S* |
   *       TIME S* | FREQ S* | function ]
   *   | STRING S* | IDENT S* | URI S* | RGB S* | UNICODERANGE S* | hexcolor
   * </pre>
   */
  public static final class Term extends CssTree {
    private final UnaryOperator op;
    public Term(FilePosition pos, UnaryOperator op, CssExprAtom expr) {
      super(pos, Collections.singletonList(expr));
      this.op = op;
    }
    @Override
    public UnaryOperator getValue() { return op; }

    public UnaryOperator getOperator() { return op; }

    public CssExprAtom getExprAtom() { return (CssExprAtom) children().get(0); }

    public void render(RenderContext r) throws IOException {
      if (null != op && null != op.symbol) { r.out.append(op.symbol); }
      children().get(0).render(r);
    }
  }

  /**
   * A primitive CSS literal expression or function call. It is an atom in the
   * sense that cannot be divided into an operator and an operand.
   * See also http://www.w3.org/TR/REC-CSS2/syndata.html#values
   */
  public abstract static class CssExprAtom extends CssTree {
    CssExprAtom(FilePosition pos, List<? extends Expr> children) {
      super(pos, children);
    }
  }

  // these patterns match unescaped values
  private static final Pattern IDLITERAL = Pattern.compile("^#.+$");
  private static final Pattern CLASSLITERAL = Pattern.compile("^\\..+$");
  private static final Pattern IDENTLITERAL = Pattern.compile("^.+$");
  private static final Pattern HASHLITERAL = Pattern.compile(
      "^#[a-fA-F0-9]{3,6}$");
  private static final Pattern QUANTITYLITERAL = Pattern.compile(
      "^(?:\\.\\d+|\\d+(?:\\.\\d+)?)([a-zA-Z]+|%)?$");
  private static final Pattern UNICODERANGELITERAL = Pattern.compile(
      "^U\\+(?:[0-9a-fA-F]{1,6}-[0-9a-fA-F]{1,6}|[0-9a-fA-F?]{1,6})$",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern SUBSTITUTION = Pattern.compile(
      "^\\$\\(.*\\)(?:%|[a-z]+)?$", Pattern.DOTALL);

  /**
   * TODO(ihab): Javadoc.
   */
  public abstract static class CssLiteral extends CssExprAtom {
    private String value;
    /**
     * @param inputValue the unescaped inputValue.  Any unicode escapes have been
     *   converted to the corresponding character.
     */
    CssLiteral(FilePosition pos, String inputValue) {
      super(pos, Collections.<Expr>emptyList());
      setValue(inputValue);
    }
    @Override
    public String getValue() { return value; }
    /**
     * @param newValue the unescaped value.  Any unicode escapes have been
     *   converted to the corresponding character.
     */
    public void setValue(String newValue) {
      if (!checkValue(newValue)) {
        throw new IllegalArgumentException(newValue);
      }
      this.value = newValue;
    }
    protected abstract boolean checkValue(String value);
  }

  /**
   * TODO(ihab): Javadoc.
   */
  public static final class IdLiteral extends CssLiteral {
    public IdLiteral(FilePosition pos, String value) { super(pos, value); }
    @Override
    protected boolean checkValue(String value) {
      return IDLITERAL.matcher(value).matches();
    }
    public void render(RenderContext r) throws IOException {
      r.out.append('#');
      escapeCssIdent(getValue().substring(1), r.out);
    }
  }

  /**
   * TODO(ihab): Javadoc.
   */
  public static final class ClassLiteral extends CssLiteral {
    public ClassLiteral(FilePosition pos, String value) { super(pos, value); }
    @Override
    protected boolean checkValue(String value) {
      return CLASSLITERAL.matcher(value).matches();
    }
    public void render(RenderContext r) throws IOException {
      r.out.append('.');
      escapeCssIdent(getValue().substring(1), r.out);
    }
  }

  /**
   * TODO(ihab): Javadoc.
   */
  public static final class StringLiteral extends CssLiteral {
    public StringLiteral(FilePosition pos, String value) { super(pos, value); }
    @Override
    protected boolean checkValue(String value) {
      return value != null;
    }
    public void render(RenderContext r) throws IOException {
      r.out.append('\'');
      escapeCssString(getValue(), r.out);
      r.out.append('\'');
    }
  }

  /**
   * TODO(ihab): Javadoc.
   */
  public static final class HashLiteral extends CssLiteral {
    public HashLiteral(FilePosition pos, String value) { super(pos, value); }
    @Override
    protected boolean checkValue(String value) {
      return HASHLITERAL.matcher(value).matches();
    }
    public void render(RenderContext r) throws IOException {
      r.out.append('#').append(getValue().substring(1));
    }
  }

  /**
   * TODO(ihab): Javadoc.
   */
  public static final class QuantityLiteral extends CssLiteral {
    QuantityLiteral(FilePosition pos, String value) { super(pos, value); }
    @Override
    protected boolean checkValue(String value) {
      return QUANTITYLITERAL.matcher(value).matches();
    }
    public void render(RenderContext r) throws IOException {
      r.out.append(getValue());
    }
  }

  /**
   * TODO(ihab): Javadoc.
   */
  public static final class UnicodeRangeLiteral extends CssLiteral {
    UnicodeRangeLiteral(FilePosition pos, String value) { super(pos, value); }
    @Override
    protected boolean checkValue(String value) {
      return UNICODERANGELITERAL.matcher(value).matches();
    }
    public void render(RenderContext r) throws IOException {
      r.out.append(getValue());
    }
  }

  /**
   * TODO(ihab): Javadoc.
   */
  public static final class UriLiteral extends CssLiteral {
    UriLiteral(FilePosition pos, URI value) { super(pos, value.toString()); }
    @Override
    protected boolean checkValue(String value) {
      try {
        URI.create(value);
        return true;
      } catch (IllegalArgumentException ex) {
        return false;
      }
    }
    public void render(RenderContext r) throws IOException {
      r.out.append("url('");
      escapeCssString(getValue(), r.out);
      r.out.append("')");
    }
  }

  /**
   * TODO(ihab): Javadoc.
   */
  public static final class IdentLiteral extends CssLiteral {
    IdentLiteral(FilePosition pos, String value) { super(pos, value); }
    @Override
    protected boolean checkValue(String value) {
      return IDENTLITERAL.matcher(value).matches();
    }
    public void render(RenderContext r) throws IOException {
      escapeCssIdent(getValue(), r.out);
    }
  }

  /**
   * <pre>
   *   function
   *   : FUNCTION S* expr ')' S*
   * </pre>
   */
  public static final class FunctionCall extends CssExprAtom {
    private final String name;
    public FunctionCall(FilePosition pos, String name, Expr expr) {
      super(pos, Collections.singletonList(expr));
      this.name = name;
    }
    @Override
    public String getValue() { return name; }
    public String getName() { return name; }
    public Expr getArguments() { return (Expr) children().get(0); }

    @Override
    protected void childrenChanged() {
      super.childrenChanged();
      assert 1 == children().size() && (children().get(0) instanceof Expr);
    }
    public void render(RenderContext r) throws IOException {
      escapeCssIdent(name, r.out);
      r.out.append('(');
      children.get(0).render(r);
      r.out.append(')');
    }

  }

  /**
   * A template substitution in a CSS stylesheet.  This is not part of the
   * CSS language, and will only be produced if
   * {@link com.google.caja.lexer.CssLexer#allowSubstitutions}
   * is set.
   */
  public static final class Substitution extends CssLiteral {
    public Substitution(FilePosition pos, String value) {
      super(pos, value);
    }

    public String getBody() {
      String value = getValue();
      return value.substring(2, value.lastIndexOf(')'));
    }

    public String getSuffix() {
      String value = getValue();
      return value.substring(value.lastIndexOf(')') + 1);
    }

    @Override
    public boolean checkValue(String value) {
      // TODO(msamuel): maybe enforce the convention that there are matched
      // parentheses outside C-style strings.
      return SUBSTITUTION.matcher(value).matches();
    }

    public void render(RenderContext r) throws IOException {
      r.out.append(getValue());
    }
  }

  /** See http://www.w3.org/TR/REC-CSS2/selector.html#q2 */
  public static final class Combination extends CssTree {
    final Combinator comb;

    public Combination(FilePosition pos, Combinator comb) {
      super(pos, Collections.<CssTree>emptyList());
      this.comb = comb;
    }

    @Override
    public Combinator getValue() { return comb; }
    public Combinator getCombinator() { return comb; }
    public void render(RenderContext r) throws IOException {
      if (null != comb.symbol) { r.out.append(comb.symbol); }
    }
  }

  /** See http://www.w3.org/TR/REC-CSS2/selector.html#q2 */
  public static final class Operation extends CssTree {
    final Operator op;

    Operation(FilePosition pos, Operator op) {
      super(pos, Collections.<CssTree>emptyList());
      this.op = op;
    }

    @Override
    public Operator getValue() { return op; }

    public Operator getOperator() { return op; }

    public void render(RenderContext r) throws IOException {
      if (null != op.symbol) { r.out.append(op.symbol); }
    }
  }

  /*
   * TODO(ihab): Javadoc.
   */
  public static final class UnaryOperation extends CssTree {
    final UnaryOperator op;

    UnaryOperation(FilePosition pos, UnaryOperator op) {
      super(pos, Collections.<CssTree>emptyList());
      this.op = op;
    }

    @Override
    public UnaryOperator getValue() { return op; }

    public void render(RenderContext r) throws IOException {
      r.out.append(op.symbol);
    }
  }

  /**
   * <pre>
   * operator
   *   : '/' S* | ',' S* | <i>empty</i>
   * </pre>
   */
  public static enum Operator {
    DIV("/"),
    COMMA(","),
    NONE(null),
    ;
    private final String symbol;
    Operator(String symbol) { this.symbol = symbol; }
    public String getSymbol() { return symbol; }
  }

  /**
   * <pre>
   * unary_operator
   *   : '-' | '+'
   * </pre>
   */
  public static enum UnaryOperator {
    NEGATION("-"),
    IDENTITY("+"),
    ;
    private final String symbol;
    UnaryOperator(String symbol) { this.symbol = symbol; }
    public String getSymbol() { return symbol; }
  }

  /**
   * <pre>
   * combinator
   *   : '+' S* | '>' S* | <i>empty</i>
   * </pre>
   */
  public static enum Combinator {
    SIBLING("+"),
    CHILD(">"),
    DESCENDANT(null),
    ;
    private final String symbol;
    Combinator(String symbol) { this.symbol = symbol; }
    public String getSymbol() { return symbol; }
  }

  private static void renderStatements(
      List<? extends CssTree> children, RenderContext r)
      throws IOException {
    r.out.append(" {");
    r.indent += 2;
    boolean first = true;
    for (CssTree decl : children) {
      if (!first) {
        r.out.append(';');
      } else {
        first = false;
      }
      r.newLine();
      decl.render(r);
    }
    r.indent -= 2;
    r.newLine();
    r.out.append("}");
  }

  private static void renderCommaGroup(
      List<? extends CssTree> children, RenderContext r) throws IOException {
    boolean first = true;
    for (CssTree child : children) {
      if (!first) {
        r.out.append(", ");
      } else {
        first = false;
      }
      child.render(r);
    }
  }

  private static void renderSpaceGroup(
      List<? extends CssTree> children, RenderContext r) throws IOException {
    boolean needSpace = false;
    for (CssTree child : children) {
      if (needSpace) {
        r.out.append(" ");
      } else {
        needSpace = true;
      }
      child.render(r);
    }
  }

  private static void escapeCssString(String s, Appendable out)
      throws IOException {
    int pos = 0;
    int n = s.length();
    boolean lastHex = false;
    for (int i = 0; i < n; ++i) {
      char ch = s.charAt(i);
      if (ch < 0x20 || ch >= 0x7f || ch == '\\' || ch == '\'' || ch == '\"') {
        out.append(s, pos, i);
        pos = i + 1;
        hexEscape(ch, pos < n ? s.charAt(pos) : -1, out);
      }
    }
    out.append(s, pos, n);
  }

  private static void escapeCssIdent(String s, Appendable out)
      throws IOException {
    int pos = 0;
    int n = s.length();
    char ch0 = s.charAt(0);
    // if the first character is a dash or a digit, we need to escape it.
    // otherwise, tokenization might produce a number.
    if (!(ch0 >= 'a' && ch0 <= 'z' || ch0 >= 'A' && ch0 <= 'Z')) {
      pos = 1;
      hexEscape(ch0, pos < n ? s.charAt(pos) : -1, out);
    }
    for (int i = 1; i < n; ++i) {
      char ch = s.charAt(i);
      if (!(ch == '-'
            || ch >= 'a' && ch <= 'z'
            || ch >= 'A' && ch <= 'Z'
            || ch >= '0' && ch <= '9')) {
        out.append(s, pos, i);
        pos = i + 1;
        hexEscape(ch, pos < n ? s.charAt(pos) : -1, out);
      }
    }
    out.append(s, pos, n);
  }

  private static void hexEscape(char ch, int nextChar, Appendable out)
      throws IOException {
    out.append('\\');
    int nChars = 1;
    while (0 != ((0xf << (nChars << 2)) & ch)) { ++nChars; }
    for (int i = nChars; --i >= 0;) {
      out.append("0123456789ABCDEF".charAt((ch >> (i << 2)) & 0xf));
    }
    // We need a space between if the character following is a hex digit or
    // a space character since the CSS {unicode} production specifies that any
    // following space character is part of the escape.
    // From http://www.w3.org/TR/CSS21/syndata.html#tokenization
    // unicode        \\[0-9a-f]{1,6}(\r\n|[ \n\r\t\f])?
    if ((nextChar >= '0' && nextChar <= '9')
        || (nextChar >= 'a' && nextChar <= 'f')
        || (nextChar >= 'A' && nextChar <= 'F')
        || nextChar == ' ' || nextChar == '\r'
        || nextChar == '\t' || nextChar == '\f') {
      out.append(' ');
    }
  }
}
