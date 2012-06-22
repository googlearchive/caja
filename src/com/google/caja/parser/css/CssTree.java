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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.lexer.escaping.UriUtil;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.render.Concatenator;
import com.google.caja.render.CssPrettyPrinter;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;
import com.google.caja.util.Name;
import com.google.caja.util.Strings;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * A node in a CSS parse tree.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class CssTree extends AbstractParseTreeNode {
  private static final long serialVersionUID = 6020901117890226169L;

  private CssTree(FilePosition pos, List<? extends CssTree> children) {
    super(pos, CssTree.class);
    createMutation().appendChildren(children).execute();
  }
  protected <T extends CssTree> CssTree(
      FilePosition pos, Class<T> subType, List<? extends T> children) {
    super(pos, subType);
    createMutation().appendChildren(children).execute();
  }

  @Override
  public Object getValue() {
    return null;
  }

  @Override
  public List<? extends CssTree> children() {
    return childrenAs(CssTree.class);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    try {
      formatSelf(new MessageContext(), 0, sb);
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(
          "StringBuilders shouldn't throw IOExceptions");
    }
    return sb.toString();
  }

  public final TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> exHandler) {
    return new CssPrettyPrinter(new Concatenator(out, exHandler));
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
    private static final long serialVersionUID = -8643612233981773251L;

    /** @param novalue ignored but required for reflection. */
    @ReflectiveCtor
    public StyleSheet(
        FilePosition pos, Void novalue, List<? extends CssStatement> rulesets) {
      this(pos, rulesets);
    }

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
   * the root node of CSS parsed from an XHTML <code>style</code> attribute.
   */
  public static final class DeclarationGroup extends CssTree {
    private static final long serialVersionUID = 8362287756047209631L;

    /** @param novalue ignored but required for reflection. */
    @ReflectiveCtor
    public DeclarationGroup(
        FilePosition pos, Void novalue, List<? extends Declaration> decls) {
      this(pos, decls);
    }

    public DeclarationGroup(
        FilePosition pos, List<? extends Declaration> decls) {
      super(pos, Declaration.class, decls);
    }

    @Override
    public List<? extends Declaration> children() {
      return childrenAs(Declaration.class);
    }

    @Override
    protected void childrenChanged() {
      super.childrenChanged();
      for (CssTree child : children()) {
        if (!(child instanceof Declaration)) {
          throw new ClassCastException(child.getClass().getName());
        }
      }
    }

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      boolean first = true;
      for (Declaration d : children()) {
        if (!first) {
          r.getOut().consume(";");
        } else {
          first = false;
        }
        d.render(r);
      }
    }
  }

  /** Part of a stylesheet. */
  public abstract static class CssStatement extends CssTree {
    private static final long serialVersionUID = 5015116074218591197L;

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
    private static final long serialVersionUID = 7450104631861052290L;

    private static <T> List<T> join(List<? extends T> a, List<? extends T> b) {
      List<T> l = new ArrayList<T>(a.size() + b.size());
      l.addAll(a);
      l.addAll(b);
      return l;
    }

    /** @param novalue ignored but required for reflection. */
    @ReflectiveCtor
    public Import(
        FilePosition pos, Void novalue, List<? extends Medium> media) {
      super(pos, media);
    }

    public Import(
        FilePosition pos, UriLiteral uri, List<? extends Medium> media) {
      super(pos, join(Collections.singletonList(uri), media));
    }

    public UriLiteral getUri() { return (UriLiteral) children().get(0); }
    public List<Medium> getMedia() {
      List<Medium> media = new ArrayList<Medium>(children().size() - 1);
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
      out.consume("\n");
    }
  }

  /**
   * <pre>
   * charset
   *   : CHARSET_SYM STRING ';'
   * </pre>
   */
  public static final class Charset extends CssStatement {
    private static final long serialVersionUID = -1098593776699942573L;

    final String charset;

    /** @param none ignored but required for reflection. */
    @ReflectiveCtor
    public Charset(
        FilePosition pos, String charset, List<? extends CssTree> none) {
      this(pos, charset);
    }
    public Charset(FilePosition pos, String charset) {
      super(pos, Collections.<CssTree>emptyList());
      this.charset = charset;
    }

    @Override
    public String getValue() { return charset; }

    public void render(RenderContext r) {
      TokenConsumer out = r.getOut();
      out.mark(getFilePosition());
      out.consume("@");
      out.consume("charset");
      out.consume(" ");
      renderCssString(charset, r);
      out.consume(";");
      out.consume("\n");
    }
  }

  /**
   * <pre>
   * media
   *   : MEDIA_SYM S* medium [ ',' S* medium ]* '{' S* ruleset* '}' S*
   * </pre>
   */
  public static final class Media extends CssStatement {
    private static final long serialVersionUID = 1634406326897941926L;

    /** @param novalue ignored but required for reflection. */
    @ReflectiveCtor
    public Media(
        FilePosition pos, Void novalue,
        List<? extends CssTree> mediaAndRuleset) {
      this(pos, mediaAndRuleset);
    }
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
    private static final long serialVersionUID = 2141132767716482740L;
    final Name ident;

    /** @param none ignored but required for reflection. */
    @ReflectiveCtor
    public Medium(FilePosition pos, Name ident, List<? extends CssTree> none) {
      this(pos, ident);
    }
    public Medium(FilePosition pos, Name ident) {
      super(pos, Collections.<CssTree>emptyList());
      this.ident = ident;
    }

    @Override
    public Name getValue() { return ident; }

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      renderCssIdent(ident.getCanonicalForm(), r);
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
    private static final long serialVersionUID = -7846795622671557446L;
    final Name ident;

    @ReflectiveCtor
    public Page(
        FilePosition pos, Name ident, List<? extends PageElement> decls) {
      super(pos, decls);
      this.ident = ident;
    }

    @Override
    public Name getValue() { return ident; }

    public void render(RenderContext r) {
      TokenConsumer out = r.getOut();
      out.mark(getFilePosition());
      out.consume("@");
      out.consume("page");
      if (null != ident) {
        out.consume(" ");
        renderCssIdent(ident.getCanonicalForm(), r);
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
    private static final long serialVersionUID = -8981557551867893004L;
    PageElement(FilePosition pos, List<? extends CssTree> children) {
      super(pos, children);
    }
    <T extends CssTree> PageElement(
        FilePosition pos, Class<T> childType, List<? extends T> children) {
      super(pos, childType, children);
    }
  }

  /**
   * <pre>
   * pseudo_page
   *   : ':' IDENT
   * </pre>
   */
  public static final class PseudoPage extends PageElement {
    private static final long serialVersionUID = -5954522226216988819L;
    final Name ident;

    /** @param none ignored but required for reflection. */
    @ReflectiveCtor
    public PseudoPage(
        FilePosition pos, Name ident, List<? extends CssTree> none) {
      this(pos, ident);
    }
    public PseudoPage(FilePosition pos, Name ident) {
      super(pos, Collections.<CssTree>emptyList());
      this.ident = ident;
    }

    @Override
    public Name getValue() { return ident; }

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      r.getOut().consume(":");
      renderCssIdent(ident.getCanonicalForm(), r);
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
    private static final long serialVersionUID = 3992274759318256076L;

    /** @param novalue ignored but required for reflection. */
    @ReflectiveCtor
    public FontFace(
        FilePosition pos, Void novalue, List<? extends Declaration> decls) {
      this(pos, decls);
    }

    public FontFace(FilePosition pos, List<? extends Declaration> decls) {
      super(pos, decls);
    }

    @Override
    protected void childrenChanged() {
      super.childrenChanged();
      for (CssTree child : children()) {
        if (!(child instanceof Declaration)) {
          throw new ClassCastException(child.getClass().getName());
        }
      }
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
    private static final long serialVersionUID = 5350602163562229685L;
    private final Name ident;
    /** @param none ignored but required for reflection. */
    @ReflectiveCtor
    public Property(
        FilePosition pos, Name ident, List<? extends CssTree> none) {
      this(pos, ident);
    }

    public Property(FilePosition pos, Name ident) {
      super(pos, Collections.<CssTree>emptyList());
      this.ident = ident;
    }

    @Override
    public Name getValue() { return ident; }

    public Name getPropertyName() {
      return ident;
    }

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      renderCssIdent(ident.getCanonicalForm(), r);
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
    private static final long serialVersionUID = -1370546279860143576L;

    /** @param novalue ignored but required for reflection. */
    @ReflectiveCtor
    public RuleSet(
        FilePosition pos, Void novalue,
        List<? extends CssTree> selectorsAndDecls) {
      this(pos, selectorsAndDecls);
    }
    public RuleSet(
        FilePosition pos, List<? extends CssTree> selectorsAndDecls) {
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
    private static final long serialVersionUID = -465995012590018227L;
    /** @param novalue ignored but required for reflection. */
    @ReflectiveCtor
    public Selector(
        FilePosition pos, Void novalue, List<? extends CssTree> children) {
      this(pos, children);
    }
    public Selector(FilePosition pos, List<? extends CssTree> children) {
      super(pos, children);
    }
    public void render(RenderContext r) {
      renderSpaceGroup(children(), r);
    }
    @Override
    protected void childrenChanged() {
      super.childrenChanged();
      List<? extends CssTree> children = children();
      int n = children.size();
      boolean needSelector = true;
      for (CssTree child : children) {
        if (child instanceof SimpleSelector) {
          needSelector = false;
        } else if (!needSelector && child instanceof Combination) {
          needSelector = true;
        } else {
          throw new ClassCastException(child.getClass().getName());
        }
      }
      if (needSelector && n != 0) {
        throw new IllegalArgumentException();
      }
    }
  }

  /**
   * <pre>
   * simple_selector
   *   : element_name? [ HASH | class | attrib | pseudo ]* S*
   * </pre>
   *
   * HASHes and classes may be wrapped in a special {@link SuffixedSelectorPart}
   * which indicates that they are in a name-space defined by a suffix
   * associated with the style-sheet.
   */
  public static final class SimpleSelector extends CssTree {
    private static final long serialVersionUID = -7674557532295492300L;

    /** @param novalue ignored but required for reflection. */
    @ReflectiveCtor
    public SimpleSelector(
        FilePosition pos, Void novalue, List<? extends CssTree> children) {
      this(pos, children);
    }

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
    private static final long serialVersionUID = -6481902550898050210L;

    /**
     * @param novalue ignored but required for reflection.
     * @param none ignored but required for reflection.
     */
    @ReflectiveCtor
    public WildcardElement(
        FilePosition pos, Void novalue, List<? extends CssTree> none) {
      this(pos);
    }

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
    private static final long serialVersionUID = 5081385158465914021L;
    final String ident;

    @ReflectiveCtor
    public Attrib(
        FilePosition pos, String ident,
        List<? extends CssTree> operatorAndValue) {
      super(pos, operatorAndValue);
      this.ident = ident;
    }

    public Attrib(FilePosition pos, String ident,
                  AttribOperation operator, CssLiteral value) {
      super(pos, null == operator
            ? Collections.<CssTree>emptyList()
            : Collections.unmodifiableList(Arrays.asList(operator, value)));
      this.ident = ident;
    }

    @Override
    public String getValue() { return ident; }
    public String getIdent() { return ident; }

    public AttribOperation getOperation() {
      return children().isEmpty() ? null : (AttribOperation) children().get(0);
    }

    public CssLiteral getRhsValue() {
      return children().isEmpty() ? null : (CssLiteral) children().get(1);
    }

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
    private static final long serialVersionUID = 5881502462250726237L;
    final AttribOperator op;
    /** @param none ignored but required for reflection. */
    @ReflectiveCtor
    public AttribOperation(
        FilePosition pos, AttribOperator op, List<? extends CssTree> none) {
      this(pos, op);
    }

    public AttribOperation(FilePosition pos, AttribOperator op) {
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
    private static final long serialVersionUID = -8355980992414290494L;

    /** @param novalue ignored but required for reflection. */
    @ReflectiveCtor
    public Pseudo(
        FilePosition pos, Void novalue, List<? extends CssExprAtom> oneAtom) {
      this(pos, oneAtom.get(0));
    }

    public Pseudo(FilePosition pos, CssExprAtom child) {
      super(pos, Collections.singletonList(child));
    }

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      r.getOut().consume(":");
      children().get(0).render(r);
    }
  }

  /**
   * A CSS property name, style value pair.
   * <pre>
   * declaration
   *   : property-declaration
   *   | empty-declaration
   *   | user-agent-hack
   * </pre>
   * The term "declaration" is used in the CSS2 spec to describe both
   * {@link PropertyDeclaration} and {@link EmptyDeclaration}.  Neither
   * of those terms appear in the spec, and the <code>user-agent-hack</code>
   * has no analog in the spec since it models a browser hack.
   */
  public static abstract class Declaration extends PageElement {
    /**
     *
     */
    private static final long serialVersionUID = -3579944514104809928L;

    Declaration(FilePosition pos, List<? extends CssTree> children) {
      super(pos, children);
    }

    <T extends CssTree> Declaration(
        FilePosition pos, Class<T> childType, List<? extends T> children) {
      super(pos, childType, children);
    }
  }

  /**
   * <pre>
   * empty-declaration
   *   : <i>empty</i>
   * </pre>
   */
  public static final class EmptyDeclaration extends Declaration {
    private static final long serialVersionUID = -2714362083044209218L;

    /**
     * @param novalue ignored but required for reflection.
     * @param none ignored but required for reflection.
     */
    @ReflectiveCtor
    public EmptyDeclaration(
        FilePosition pos, Void novalue, List<? extends CssTree> none) {
      this(pos);
      assert none.isEmpty();
    }

    public EmptyDeclaration(FilePosition pos) {
      super(pos, Collections.<CssTree>emptyList());
    }

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
    }
  }

  /**
   * <pre>
   * property-declaration
   *   : property ':' S* expr prio?
   * </pre>
   */
  public static final class PropertyDeclaration extends Declaration {
    private static final long serialVersionUID = 6744488790926852402L;

  // Local member variables are only changed in childrenChanged(),
  // so this class satisfies the immutability contract of the superclass.
    private Property prop;
    private Expr expr;
    private Prio prio;

    /** @param novalue ignored but required for reflection. */
    @ReflectiveCtor
    public PropertyDeclaration(
        FilePosition pos, Void novalue, List<? extends CssTree> children) {
      this(pos, children);
    }

    public PropertyDeclaration(
        FilePosition pos, List<? extends CssTree> children) {
      super(pos, children);
    }

    @Override
    protected void childrenChanged() {
      super.childrenChanged();
      List<? extends CssTree> children = children();
      prop = (Property) children.get(0);
      expr = (Expr) children.get(1);
      prio = children.size() > 2 ? (Prio) children.get(2) : null;
      assert children.size() <= 3 && null != prop && null != expr;
    }

    public Property getProperty() { return prop; }
    public Expr getExpr() { return expr; }
    public Prio getPrio() { return prio; }

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
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

  /**
   * <pre>
   * prio
   *   : IMPORTANT_SYM S*
   * </pre>
   */
  public static final class Prio extends CssTree {
    private static final long serialVersionUID = -882058488526090512L;
    final String value;

    /** @param none ignored but required for reflection. */
    @ReflectiveCtor
    public Prio(FilePosition pos, String value, List<? extends CssTree> none) {
      this(pos, value);
    }

    public Prio(FilePosition pos, String value) {
      super(pos, Collections.<CssTree>emptyList());
      this.value = value;
    }

    @Override
    public String getValue() { return value; }

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      r.getOut().consume("!");
      renderCssIdent(Strings.lower(getValue().substring(1)), r);
    }
  }

  /**
   * <pre>
   * expr
   *   : term [ operator term ]*
   * </pre>
   */
  public static final class Expr extends CssTree {
    private static final long serialVersionUID = 5011229222727740788L;

    /** @param novalue ignored but required for reflection. */
    @ReflectiveCtor
    public Expr(
        FilePosition pos, Void novalue, List<? extends CssTree> children) {
      this(pos, children);
    }

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
    private static final long serialVersionUID = 2376339117176355282L;
    private final UnaryOperator op;
    @ReflectiveCtor
    public Term(FilePosition pos, UnaryOperator op,
                List<? extends CssExprAtom> oneatom) {
      this(pos, op, oneatom.get(0));
    }
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
    private static final long serialVersionUID = 8065698386622189655L;
    CssExprAtom(FilePosition pos, List<? extends CssTree> children) {
      super(pos, children);
    }
    <T extends CssTree>
    CssExprAtom(
        FilePosition pos, Class<T> childType, List<? extends T> children) {
      super(pos, childType, children);
    }
  }

  // these patterns match unescaped values
  private static final Pattern IDLITERAL = Pattern.compile("^#.+$");
  private static final Pattern CLASSLITERAL = Pattern.compile("^\\..+$");
  private static final Pattern IDENTLITERAL = Pattern.compile("^.+$");
  private static final Pattern HASHLITERAL = Pattern.compile(
      // The CSS spec allows for 3 and 6 digit forms where "#ABC" is equivalent
      // to "#AABBCC".  IE filters use a non-standard 8 digit RRGGBB form.
      "^#[a-fA-F0-9]{3}(?:[a-fA-F0-9]{3}(?:[a-fA-F0-9]{2})?)?$");
  private static final Pattern QUANTITYLITERAL = Pattern.compile(
      "^(?:\\.\\d+|\\d+(?:\\.\\d+)?)([a-zA-Z]+|%)?$");
  private static final Pattern UNICODERANGELITERAL = Pattern.compile(
      "^U\\+(?:[0-9a-fA-F]{1,6}-[0-9a-fA-F]{1,6}|[0-9a-fA-F?]{1,6})$",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern SUBSTITUTION = Pattern.compile(
      "^\\$\\{.*\\}(?:%|[a-z]+)?$", Pattern.DOTALL);

  /**
   * Abstract base class for a literal value such as an ID, CLASS, URI, String,
   * Color, or keyword value.
   */
  public abstract static class CssLiteral extends CssExprAtom {
    private static final long serialVersionUID = -779702592080387558L;
    private String value;
    /**
     * @param inputValue the unescaped inputValue.  Any unicode escapes have
     *   been converted to the corresponding character.
     */
    protected CssLiteral(FilePosition pos, String inputValue) {
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
      if (isImmutable()) {
        throw new UnsupportedOperationException();
      }
      if (!checkValue(newValue)) {
        throw new IllegalArgumentException(newValue);
      }
      this.value = newValue;
    }
    protected abstract boolean checkValue(String value);
  }

  /**
   * An ID in a selector, like {@code #foo}.
   */
  public static final class IdLiteral extends CssLiteral {
    private static final long serialVersionUID = -165713497054691362L;
    /** @param none ignored but required for reflection. */
    @ReflectiveCtor
    public IdLiteral(
        FilePosition pos, String inputValue, List<? extends CssTree> none) {
      this(pos, inputValue);
    }
    public IdLiteral(FilePosition pos, String value) { super(pos, value); }
    @Override
    protected boolean checkValue(String value) {
      return IDLITERAL.matcher(value).matches();
    }
    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      r.getOut().consume("#");
      renderCssIdent(getIdentifier(), r);
    }
    public String getIdentifier() { return getValue().substring(1); }
  }

  public static final class SuffixedSelectorPart extends CssTree {
    private static final long serialVersionUID = -6616233114613786373L;

    /**
     * @param value not used but required for reflective tree copying.
     */
    @ReflectiveCtor
    public SuffixedSelectorPart(
        FilePosition pos, Void value, List<? extends CssLiteral> children) {
      super(pos, children);
      if (children.size() >= 2) { throw new IllegalArgumentException(); }
    }

    public SuffixedSelectorPart(FilePosition pos, IdLiteral prefix) {
      super(pos, Collections.singletonList(prefix));
      if (prefix == null) { throw new NullPointerException(); }
    }

    public SuffixedSelectorPart(
        FilePosition pos, @Nullable ClassLiteral prefix) {
      super(
          pos, prefix != null
          ? Collections.singletonList(prefix)
          : Collections.<CssLiteral>emptyList());
    }

    /** Equivalent to a class with a null suffix. */
    public SuffixedSelectorPart(FilePosition pos) {
      this(pos, (ClassLiteral) null);
    }

    @Override
    protected void childrenChanged() {
      List<? extends CssTree> children = children();
      if (children.size() >= 2) {
        throw new IllegalStateException();
      }
      if (!children.isEmpty()) {
        CssLiteral prefix = (CssLiteral) children.get(0);
        if (!(prefix instanceof IdLiteral || prefix instanceof ClassLiteral)) {
          throw new IllegalStateException();
        }
      }
    }

    @Override
    public void render(RenderContext r) {
      List<? extends CssTree> children = children();
      TokenConsumer tc = r.getOut();
      tc.mark(getFilePosition());
      if (!children.isEmpty()) {
        tc.mark(children.get(0).getFilePosition());
      }
      tc.consume(typePrefix());
      // Make sure the token consumer sees a single identifier.
      tc.consume(suffixedIdentifier(suffix()));
    }

    public String typePrefix() {
      List<? extends CssTree> children = children();
      // No child implies a class name that is the suffix.
      if (!children.isEmpty()) {
        CssLiteral prefixLit = ((CssLiteral) children.get(0));
        if (prefixLit instanceof IdLiteral) { return "#"; }
        assert prefixLit instanceof ClassLiteral;
      }
      return ".";
    }

    public @Nullable String prefix() {
      List<? extends CssTree> children = children();
      if (!children.isEmpty()) {
        CssLiteral prefixLit = ((CssLiteral) children.get(0));
        if (prefixLit instanceof IdLiteral) {
          return ((IdLiteral) prefixLit).getIdentifier();
        } else {
          return ((ClassLiteral) prefixLit).getIdentifier();
        }
      }
      // No child implies a class name that is the suffix.
      return null;
    }

    public String suffix() {
      return "namespace__";
    }

    public String suffixedIdentifier(String suffix) {
      String prefix = prefix();
      return prefix != null ? prefix + "-" + suffix : suffix;
    }
  }

  /**
   * A class name in a selector like {@code .foo}.
   */
  public static class ClassLiteral extends CssLiteral {
    private static final long serialVersionUID = 4976309939926023380L;
    /** @param none ignored but required for reflection. */
    @ReflectiveCtor
    public ClassLiteral(
        FilePosition pos, String inputValue, List<? extends CssTree> none) {
      this(pos, inputValue);
    }
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
    public String getIdentifier() { return getValue().substring(1); }
  }

  /**
   * A string literal in a property value like {@code 'foo'}.
   */
  public static final class StringLiteral extends CssLiteral {
    private static final long serialVersionUID = -4074928917829475004L;
    /** @param none ignored but required for reflection. */
    @ReflectiveCtor
    public StringLiteral(
        FilePosition pos, String inputValue, List<? extends CssTree> none) {
      this(pos, inputValue);
    }
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
   * A color value in a property value like {@code #AABBCC}.
   */
  public static final class HashLiteral extends CssLiteral {
    private static final long serialVersionUID = -7288354377390397932L;
    /** @param none ignored but required for reflection. */
    @ReflectiveCtor
    public HashLiteral(
        FilePosition pos, String inputValue, List<? extends CssTree> none) {
      this(pos, inputValue);
    }
    public HashLiteral(FilePosition pos, String value) { super(pos, value); }
    public static HashLiteral hex(FilePosition pos, int n, int digits) {
      StringBuilder sb = new StringBuilder(digits + 1);
      sb.append('#');
      while (--digits >= 0) {
        sb.append("0123456789abcdef".charAt((n >>> (digits * 4)) & 0xf));
      }
      return new HashLiteral(pos, sb.toString());
    }
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
   * A numeric quantity like {@code 5cm}, {@code 100%}, or {@code 0}.
   */
  public static final class QuantityLiteral extends CssLiteral {
    private static final long serialVersionUID = -5886777675781113368L;
    /** @param none ignored but required for reflection. */
    @ReflectiveCtor
    public QuantityLiteral(
        FilePosition pos, String inputValue, List<? extends CssTree> none) {
      this(pos, inputValue);
    }
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
   * A range of unicode code-points.
   */
  public static final class UnicodeRangeLiteral extends CssLiteral {
    private static final long serialVersionUID = -8514138506941567407L;
    /** @param none ignored but required for reflection. */
    @ReflectiveCtor
    public UnicodeRangeLiteral(
        FilePosition pos, String inputValue, List<? extends CssTree> none) {
      this(pos, inputValue);
    }
    public UnicodeRangeLiteral(FilePosition pos, String value) {
      super(pos, value);
    }
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
   * A uri literal like {@code url('foo/bar.css')}.
   */
  public static class UriLiteral extends CssLiteral {
    private static final long serialVersionUID = -8141374453739246763L;
    /** @param none ignored but required for reflection. */
    @ReflectiveCtor
    public UriLiteral(
        FilePosition pos, String value, List<? extends CssTree> none) {
      this(pos, URI.create(value));
    }
    public UriLiteral(FilePosition pos, URI value) {
      super(pos, value.toString());
    }
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
      out.consume("url");
      out.consume("(");
      String url;
      try {
        url = UriUtil.normalizeUri(getValue());
      } catch (URISyntaxException ex) {
        url = "data:,";
      }
      renderCssString(url, r);
      out.mark(FilePosition.endOfOrNull(getFilePosition()));
      out.consume(")");
    }
  }

  /**
   * An identifier in a selector like {@code div} or a keyword in a property
   * value like {@code auto}.
   */
  public static final class IdentLiteral extends CssLiteral {
    private static final long serialVersionUID = 1891747834449899600L;
    /** @param none ignored but required for reflection. */
    @ReflectiveCtor
    public IdentLiteral(
        FilePosition pos, String value, List<? extends CssTree> none) {
      this(pos, value);
    }
    public IdentLiteral(FilePosition pos, String value) {
      super(pos, value);
    }
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
    private static final long serialVersionUID = -3808281332587033458L;
    private final Name name;
    @ReflectiveCtor
    public FunctionCall(
        FilePosition pos, Name name, List<? extends Expr> expr) {
      this(pos, name, expr.get(0));
    }
    public FunctionCall(FilePosition pos, Name name, Expr expr) {
      super(pos, Collections.singletonList(expr));
      this.name = name;
    }
    @Override
    public Name getValue() { return name; }
    public Name getName() { return name; }
    public Expr getArguments() { return (Expr) children().get(0); }

    @Override
    protected void childrenChanged() {
      super.childrenChanged();
      assert 1 == children().size() && (children().get(0) instanceof Expr);
    }
    public void render(RenderContext r) {
      TokenConsumer out = r.getOut();
      out.mark(getFilePosition());
      renderCssIdent(name.getCanonicalForm(), r);
      out.consume("(");
      children().get(0).render(r);
      out.mark(FilePosition.endOfOrNull(getFilePosition()));
      out.consume(")");
    }
  }

  /**
   * An IE extension used in the filter property as described at
   * http://msdn.microsoft.com/en-us/library/ms532847(VS.85).aspx.
   */
  public static final class ProgId extends CssExprAtom {
    private static final long serialVersionUID = -3169418029705076457L;
    private final Name name;

    @ReflectiveCtor
    public ProgId(
        FilePosition pos, Name name, List<? extends ProgIdAttribute> attrs) {
      super(pos, ProgIdAttribute.class, attrs);
      this.name = name;
    }

    @Override
    public Name getValue() { return name; }
    public Name getName() { return name; }
    @Override
    public List<? extends ProgIdAttribute> children() {
      return childrenAs(ProgIdAttribute.class);
    }

    public void render(RenderContext r) {
      TokenConsumer tc = r.getOut();
      tc.mark(getFilePosition());
      tc.consume("progid");
      tc.consume(":");
      boolean dot = false;
      for (String part : name.getCanonicalForm().split("\\.")) {
        if (dot) { tc.consume("."); }
        dot = true;
        renderCssIdent(part, r);
      }
      tc.consume("(");
      renderCommaGroup(children(), r);
      tc.consume(")");
    }
  }

  public static final class ProgIdAttribute extends CssTree {
    private static final long serialVersionUID = 8691919563227649133L;
    private final Name name;

    @ReflectiveCtor
    public ProgIdAttribute(
        FilePosition pos, Name name, List<? extends Term> value) {
      super(pos, Term.class, value);
      this.name = name;
    }

    @Override
    public Name getValue() { return name; }
    public Name getName() { return name; }
    @Override
    public List<? extends Term> children() {
      return childrenAs(Term.class);
    }
    public Term getPropertyValue() { return children().get(0); }
    @Override
    protected void childrenChanged() {
      super.childrenChanged();
      List<? extends Term> terms = children();
      if (terms.size() != 1) { throw new IllegalStateException(); }
      CssExprAtom atom = terms.get(0).getExprAtom();
      if (!(atom instanceof CssLiteral)) {
        throw new ClassCastException(atom.getClass().getName());
      }
    }

    public void render(RenderContext r) {
      TokenConsumer tc = r.getOut();
      tc.mark(getFilePosition());
      renderCssIdent(name.getCanonicalForm(), r);
      tc.consume("=");
      getPropertyValue().render(r);
    }
  }

  /**
   * A template substitution in a CSS stylesheet.  This is not part of the
   * CSS language, and will only be produced if
   * {@link com.google.caja.lexer.CssLexer#allowSubstitutions}
   * is set.
   */
  public static final class Substitution extends CssLiteral {
    private static final long serialVersionUID = 805807688991145511L;

    /** @param none ignored but required for reflection. */
    public Substitution(
        FilePosition pos, String value, List<? extends CssTree> none) {
      this(pos, value);
    }
    public Substitution(FilePosition pos, String value) {
      super(pos, value);
    }

    public String getBody() {
      String value = getValue();
      // Produce a string of the same length, so that the file position makes
      // sense.
      StringBuilder sb = new StringBuilder("  ");  // skip ${
      int end = value.lastIndexOf('}');  // until }
      sb.append(value, 2, end);
      while (sb.length() < value.length()) { sb.append(' '); }
      return sb.toString();
    }

    public String getSuffix() {
      String value = getValue();
      return value.substring(value.lastIndexOf('}') + 1);
    }

    @Override
    public boolean checkValue(String value) {
      // TODO(mikesamuel): maybe enforce the convention that there are matched
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
    private static final long serialVersionUID = -1805916464027890613L;
    final Combinator comb;

    /** @param none ignored but required for reflection. */
    @ReflectiveCtor
    public Combination(
        FilePosition pos, Combinator comb, List<? extends CssTree> none) {
      this(pos, comb);
    }
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
    private static final long serialVersionUID = -3771062755116184740L;
    final Operator op;

    /** @param none ignored but required for reflection. */
    @ReflectiveCtor
    public Operation(
        FilePosition pos, Operator op, List<? extends CssTree> none) {
      this(pos, op);
    }
    public Operation(FilePosition pos, Operator op) {
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

  /**
   * A hack that uses syntactically invalid CSS to make a rule visible on some
   * user agents but invisible on others.
   * <pre>
   * user-agent-hack
   *   : '*' declaration
   * </pre>
   */
  public static final class UserAgentHack extends Declaration {
    private static final long serialVersionUID = -3920735297551152675L;

    // Local member variable is only set in the constructor, and the getter
    // returns a defensive copy, so this class satisfies the immutability
    // contract of the superclass.
    private final EnumSet<UserAgent> enabledOn;

    @ReflectiveCtor
    public UserAgentHack(
        FilePosition pos, Set<UserAgent> enabledOn,
        List<? extends PropertyDeclaration> decl) {
      super(pos, PropertyDeclaration.class, decl);
      this.enabledOn = EnumSet.copyOf(enabledOn);
    }

    @Override
    public Set<UserAgent> getValue() {
      return Collections.unmodifiableSet(enabledOn);
    }

    @Override
    protected void childrenChanged() {
      super.childrenChanged();
      List<? extends CssTree> children = children();
      if (children.size() != 1) { throw new IllegalStateException(); }
      if (!(children.get(0) instanceof PropertyDeclaration)) {
        throw new ClassCastException(children.get(0).getClass().getName());
      }
    }

    public PropertyDeclaration getDeclaration() {
      return (PropertyDeclaration) children().get(0);
    }

    public void render(RenderContext r) {
      TokenConsumer out = r.getOut();
      out.mark(getFilePosition());
      out.consume("*");
      getDeclaration().render(r);
    }
  }

  /** An identifier for a version of a supported browser. */
  public static enum UserAgent {
    IE6,
    IE7,
    IE8,
    ;

    public static EnumSet<UserAgent> ie7OrOlder() {
      return EnumSet.of(IE6, IE7);
    }
  }

  private static void renderStatements(
      List<? extends CssTree> children, @Nullable FilePosition pos,
      RenderContext r) {
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
    Escaping.escapeCssString(s, sb);
    sb.append('\'');
    r.getOut().consume(sb.toString());
  }
}
