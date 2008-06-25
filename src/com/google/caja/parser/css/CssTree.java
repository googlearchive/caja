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
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.render.CssPrettyPrinter;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;

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
  CssTree(FilePosition pos, List<? extends CssTree> children) {
    this.setFilePosition(pos);
    createMutation().appendChildren(children).execute();
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

  public final TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> exHandler) {
    return new CssPrettyPrinter(out, exHandler);
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

    public void render(RenderContext r) {
      for (CssTree child : children()) {
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
      for (CssTree child : children()) {
        if (!(child instanceof Declaration)) {
          throw new IllegalArgumentException();
        }
      }
    }

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      boolean first = true;
      for (CssTree t : children()) {
        if (!first) {
          r.getOut().consume(";");
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

    public UriLiteral getUri() { return (UriLiteral) children().get(0); }
    public List<Medium> getMedia() {
      List<Medium> media = new ArrayList<Medium>();
      for (CssTree t : children().subList(1, children().size())) {
        media.add((Medium) t);
      }
      return media;
    }

    public void render(RenderContext r) {
      TokenConsumer out = r.getOut();
      out.mark(getFilePosition());
      out.consume("@");
      out.consume("import");
      out.consume(" ");
      getUri().render(r);

      List<? extends CssTree> media = getMedia();
      if (!media.isEmpty()) {
        out.consume(" ");
        renderCommaGroup(media, r);
      }
      out.consume(";");
    }
  }

  /**
   * <pre>
   * media
   *   : MEDIA_SYM S* medium [ ',' S* medium ]* '{' S* ruleset* '}' S*
   * </pre>
   */
  public static final class Media extends CssStatement {
    public Media(FilePosition pos, List<? extends CssTree> mediaAndRuleset) {
      super(pos, mediaAndRuleset);
    }

    public List<Medium> getMedia() {
      List<Medium> media = new ArrayList<Medium>();
      for (CssTree t : children()) {
        if (!(t instanceof Medium)) { break; }
        media.add((Medium) t);
      }
      return media;
    }

    public void render(RenderContext r) {
      TokenConsumer out = r.getOut();
      out.mark(getFilePosition());
      out.consume("@");
      out.consume("media");
      out.consume(" ");
      List<? extends CssTree> children = children();
      int i = 0;
      while (i < children.size() && children.get(i) instanceof Medium) { ++i; }
      renderCommaGroup(children.subList(0, i), r);
      out.consume("{");
      for (CssTree ruleset : children.subList(i, children.size())) {
        ruleset.render(r);
      }
      out.mark(FilePosition.endOfOrNull(getFilePosition()));
      out.consume("}");
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

    public Medium(FilePosition pos, String ident) {
      super(pos, Collections.<CssTree>emptyList());
      this.ident = ident;
    }

    @Override
    public String getValue() { return ident; }

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      renderCssIdent(ident, r);
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

    public void render(RenderContext r) {
      TokenConsumer out = r.getOut();
      out.mark(getFilePosition());
      out.consume("@");
      out.consume("page");
      if (null != ident) {
        out.consume(" ");
        renderCssIdent(ident, r);
      }
      List<? extends CssTree> children = children();
      if (children.get(0) instanceof PseudoPage) {
        children.get(0).render(r);
        children = children.subList(1, children.size());
      }
      renderStatements(children, getFilePosition(), r);
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

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      r.getOut().consume(":");
      renderCssIdent(ident, r);
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

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      r.getOut().consume("@");
      r.getOut().consume("font-face");
      r.getOut().consume(" ");
      renderStatements(children(), getFilePosition(), r);
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

    public String getPropertyName() {
      return ident;
    }

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      renderCssIdent(ident, r);
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

    public void render(RenderContext r) {
      List<? extends CssTree> children = children();
      int i = 0;
      while (i < children.size() && !(children.get(i) instanceof Declaration)) {
        ++i;
      }
      renderCommaGroup(children.subList(0, i), r);
      FilePosition selectorEnd = children.get(i - 1).getFilePosition();
      FilePosition pos = selectorEnd != null && getFilePosition() != null
          ? FilePosition.span(FilePosition.endOf(selectorEnd),
                              FilePosition.endOf(getFilePosition()))
          : null;
      renderStatements(children.subList(i, children.size()), pos, r);
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
    public void render(RenderContext r) {
      renderSpaceGroup(children(), r);
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
      CssTree first = children().get(0);
      if (first instanceof IdentLiteral) {
        return ((IdentLiteral) first).getValue();
      }
      return null;
    }

    public void render(RenderContext r) {
      // no spaces between because space is the DESCENDANT operator in Selector
      for (CssTree child : children()) { child.render(r); }
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

    public void render(RenderContext r) {
      // Start with a space to make sure that rendering couldn't introduce a
      // comment.  I know of no parse tree that would otherwise do this, but
      // comments could be used to introduce security holes.
      TokenConsumer out = r.getOut();
      out.mark(getFilePosition());
      out.consume(" ");
      out.consume("*");
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

    public void render(RenderContext r) {
      TokenConsumer out = r.getOut();
      out.mark(getFilePosition());
      out.consume("[");
      renderCssIdent(ident, r);
      List<? extends CssTree> children = children();
      if (!children.isEmpty()) {
        out.consume(" ");
        renderSpaceGroup(children, r);
      }
      out.mark(FilePosition.endOfOrNull(getFilePosition()));
      out.consume("]");
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

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      r.getOut().consume(op.getToken());
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

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      r.getOut().consume(":");
      children().get(0).render(r);
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

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      if (null != prop) {
        prop.render(r);
        r.getOut().consume(":");
        r.getOut().consume(" ");
        expr.render(r);
        if (null != prio) {
          r.getOut().consume(" ");
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

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      r.getOut().consume("!");
      renderCssIdent(getValue().substring(1), r);
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

    public void render(RenderContext r) {
      renderSpaceGroup(children(), r);
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

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      if (null != op) {
        r.getOut().consume(op.symbol);
      }
      getExprAtom().render(r);
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
      "^\\$\\{.*\\}(?:%|[a-z]+)?$", Pattern.DOTALL);

  /**
   * TODO(ihab): Javadoc.
   */
  public abstract static class CssLiteral extends CssExprAtom {
    private String value;
    /**
     * @param inputValue the unescaped inputValue.  Any unicode escapes have
     *   been converted to the corresponding character.
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
    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      r.getOut().consume("#");
      renderCssIdent(getValue().substring(1), r);
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
    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      r.getOut().consume(".");
      renderCssIdent(getValue().substring(1), r);
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
    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      renderCssString(getValue(), r);
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
    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      r.getOut().consume(getValue());
    }
  }

  /**
   * TODO(ihab): Javadoc.
   */
  public static final class QuantityLiteral extends CssLiteral {
    public QuantityLiteral(FilePosition pos, String value) {
      super(pos, value);
    }
    @Override
    protected boolean checkValue(String value) {
      return QUANTITYLITERAL.matcher(value).matches();
    }
    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      r.getOut().consume(getValue());
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
    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      r.getOut().consume(getValue());
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
    public void render(RenderContext r) {
      TokenConsumer out = r.getOut();
      out.mark(getFilePosition());
      FilePosition start = FilePosition.startOfOrNull(getFilePosition());
      out.consume("url");
      out.consume("(");
      renderCssString(getValue(), r);
      out.mark(FilePosition.endOfOrNull(getFilePosition()));
      out.consume(")");
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
    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      renderCssIdent(getValue(), r);
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
    public void render(RenderContext r) {
      TokenConsumer out = r.getOut();
      out.mark(getFilePosition());
      renderCssIdent(name, r);
      out.consume("(");
      children().get(0).render(r);
      out.mark(FilePosition.endOfOrNull(getFilePosition()));
      out.consume(")");
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
      return value.substring(2, value.lastIndexOf('}'));
    }

    public String getSuffix() {
      String value = getValue();
      return value.substring(value.lastIndexOf('}') + 1);
    }

    @Override
    public boolean checkValue(String value) {
      // TODO(msamuel): maybe enforce the convention that there are matched
      // parentheses outside C-style strings.
      return SUBSTITUTION.matcher(value).matches();
    }

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      r.getOut().consume(getValue());
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
    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      if (null != comb.symbol) {
        r.getOut().consume(comb.symbol);
      }
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

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      if (null != op.symbol) {
        r.getOut().consume(op.symbol);
      }
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
    EQUAL("="),
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
      List<? extends CssTree> children, FilePosition pos, RenderContext r) {
    TokenConsumer out = r.getOut();
    out.mark(pos);
    out.consume("{");
    CssTree last = null;
    for (CssTree decl : children) {
      if (last != null) {
        out.consume(";");
      }
      decl.render(r);
      last = decl;
    }
    out.mark(FilePosition.endOfOrNull(pos));
    out.consume("}");
  }

  private static void renderCommaGroup(
      List<? extends CssTree> children, RenderContext r) {
    boolean first = true;
    for (CssTree child : children) {
      if (!first) {
        r.getOut().consume(",");
      } else {
        first = false;
      }
      child.render(r);
    }
  }

  private static void renderSpaceGroup(
      List<? extends CssTree> children, RenderContext r) {
    boolean needSpace = false;
    for (CssTree child : children) {
      if (needSpace) {
        r.getOut().consume(" ");
      } else {
        needSpace = true;
      }
      child.render(r);
    }
  }

  private static void renderCssIdent(String ident, RenderContext r) {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeCssIdent(ident, sb);
    r.getOut().consume(sb.toString());
  }

  private static void renderCssString(String s, RenderContext r) {
    StringBuilder sb = new StringBuilder();
    sb.append('\'');
    Escaping.escapeCssString(s, r.isParanoid(), sb);
    sb.append('\'');
    r.getOut().consume(sb.toString());
  }
}
