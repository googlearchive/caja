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

package com.google.caja.plugin;

import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssTree;
import com.google.caja.render.CssPrettyPrinter;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import static com.google.caja.plugin.SyntheticNodes.s;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Rewrites CSS to be safer and shorter.
 * Namespaces css ids and classes, excises disallowed constructs, removes
 * extraneous nodes, and collapses duplicate ruleset selectors.
 *
 * @author mikesamuel@gmail.com
 */
public final class CssRewriter {
  private final PluginMeta meta;
  private final MessageQueue mq;
  private MessageLevel invalidNodeMessageLevel = MessageLevel.ERROR;

  public CssRewriter(PluginMeta meta, MessageQueue mq) {
    assert null != mq;
    assert null != meta;
    this.meta = meta;
    this.mq = mq;
  }

  /**
   * Specifies the level of messages issued when nodes are marked
   * {@link CssValidator#INVALID}.
   * If you are dealing with noisy CSS and later remove invalid nodes, then
   * this can be set to {@link MessageLevel#WARNING}.
   * @return this
   */
  public CssRewriter withInvalidNodeMessageLevel(MessageLevel messageLevel) {
    this.invalidNodeMessageLevel = messageLevel;
    return this;
  }

  /**
   * Rewrite the given CSS tree to be safer and shorter.
   *
   * If the tree could not be made safe, then there will be
   * {@link MessageLevel#ERROR error}s on the {@link MessageQueue} passed
   * to the constructor.
   *
   * @param t non null.  modified in place.
   */
  public void rewrite(AncestorChain<? extends CssTree> t) {
    quoteLooseWords(t);
    fixUnitlessLengths(t);
    // Once at the beginning, and again at the end.
    removeUnsafeConstructs(t);
    removeEmptyDeclarations(t);
    // After we remove declarations, we may have some rulesets without any
    // declarations which is technically illegal, so we remove rulesets without
    // declarations.
    removeEmptyRuleSets(t);
    if (null != meta.namespacePrefix) { namespaceIdents(t); }
    // Do this again to make sure no earlier changes introduce unsafe constructs
    removeUnsafeConstructs(t);

    translateUrls(t);
  }

  /**
   * Turn a run of unquoted identifiers into a single string, where the property
   * description says "Names containing space *should* be quoted", but does not
   * require it.
   * <p>
   * This is important for font {@code family-name}s where
   * {@code font: Times New Roman} should be written as
   * {@code font: "Times New Roman"} to avoid any possible ambiguity between
   * the individual terms and special values such as {@code serif}.
   *
   * @see CssPropertyPartType#LOOSE_WORD
   */
  private void quoteLooseWords(AncestorChain<? extends CssTree> t) {
    if (t.node instanceof CssTree.Expr) {
      combineLooseWords(t.cast(CssTree.Expr.class).node);
    }
    for (CssTree child : t.node.children()) {
      quoteLooseWords(new AncestorChain<CssTree>(t, child));
    }
  }

  private void combineLooseWords(CssTree.Expr e) {
    for (int i = 0, n = e.getNTerms(); i < n; ++i) {
      CssTree.Term t = e.getNthTerm(i);
      if (!isLooseWord(t)) { continue; }

      String propertyPart = t.getAttributes().get(
          CssValidator.CSS_PROPERTY_PART);
      StringBuilder sb = new StringBuilder();
      sb.append(t.getExprAtom().getValue());

      // Compile a mutation that removes all the extraneous terms and that
      // replaces t with a string literal.
      MutableParseTreeNode.Mutation mut = e.createMutation();

      // Compute end, the term index after the last of the run of loose terms
      // for t's property part.
      int start = i;
      int end = i + 1;
      while (end < n) {
        CssTree.Operation op = e.getNthOperation(end - 1);
        CssTree.Term t2 = e.getNthTerm(end);
        if (!(CssTree.Operator.NONE == op.getOperator() && isLooseWord(t2)
              && propertyPart.equals(
                     t2.getAttributes().get(CssValidator.CSS_PROPERTY_PART)))) {
          break;
        }
        mut.removeChild(op);
        mut.removeChild(t2);
        sb.append(' ').append(e.getNthTerm(end).getExprAtom().getValue());
        ++end;
      }

      // Create a string literal to replace all the terms [start:end-1].
      // Make sure it has the same synthetic attributes and file position.
      String text = sb.toString();
      FilePosition pos = FilePosition.span(
          t.getFilePosition(), e.getNthTerm(end - 1).getFilePosition());
      CssTree.StringLiteral quotedWords = new CssTree.StringLiteral(pos, text);
      CssTree.Term quotedTerm = new CssTree.Term(pos, null, quotedWords);
      quotedTerm.getAttributes().putAll(t.getAttributes());
      quotedTerm.getAttributes().set(CssValidator.CSS_PROPERTY_PART_TYPE,
                                     CssPropertyPartType.STRING);

      mut.replaceChild(quotedTerm, t);
      mut.execute();

      // If we made a substantive change, combining multiple terms into one,
      // then issue a line message.  We don't need to issue a warning on all
      // changes, since we only reach this code if we passed validation.
      if (end - start > 1) {
        mq.addMessage(PluginMessageType.QUOTED_CSS_VALUE,
                      pos, MessagePart.Factory.valueOf(text));
      }

      n = e.getNTerms();
    }
  }

  /** @see CssPropertyPartType#LOOSE_WORD */
  private static boolean isLooseWord(CssTree.Term t) {
    return t.getOperator() == null
        && t.getExprAtom() instanceof CssTree.IdentLiteral
        && (t.getAttributes().get(CssValidator.CSS_PROPERTY_PART_TYPE)
            == CssPropertyPartType.LOOSE_WORD);
  }

  /**
   * <a href="http://www.w3.org/TR/CSS21/syndata.html#length-units">Lengths</a>
   * require units unless the value is zero.  All browsers assume px if the
   * suffix is missing.
   */
  private void fixUnitlessLengths(AncestorChain<? extends CssTree> t) {
    t.node.acceptPreOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          if (!(ancestors.node instanceof CssTree.Term)) {
            return true;
          }
          CssTree.Term term = (CssTree.Term) ancestors.node;
          if (!(CssPropertyPartType.LENGTH == term.getAttributes().get(
                    CssValidator.CSS_PROPERTY_PART_TYPE)
                && term.getExprAtom() instanceof CssTree.QuantityLiteral)) {
            return true;
          }
          CssTree.QuantityLiteral quantity = (CssTree.QuantityLiteral)
              term.getExprAtom();
          String value = quantity.getValue();
          if (!isZeroOrHasUnits(value)) {
            // Missing units. 
            CssTree.QuantityLiteral withUnits = new CssTree.QuantityLiteral(
                quantity.getFilePosition(), value + "px");
            withUnits.getAttributes().putAll(quantity.getAttributes());
            term.replaceChild(withUnits, quantity);
            mq.addMessage(PluginMessageType.ASSUMING_PIXELS_FOR_LENGTH,
                          quantity.getFilePosition(),
                          MessagePart.Factory.valueOf(value));
          }
          return false;
        }
      }, t.parent);
  }
  private static boolean isZeroOrHasUnits(String value) {
    int len = value.length();
    char ch = value.charAt(len - 1);
    if (ch == '.' || ('0' <= ch && ch <= '9')) {  // Missing units
      for (int i = len; --i >= 0;) {
        ch = value.charAt(i);
        if ('1' <= ch && ch <= '9') { return false; }
      }
    }
    return true;
  }

  /** Get rid of rules like <code>p { }</code>. */
  private void removeEmptyDeclarations(AncestorChain<? extends CssTree> t) {
    t.node.acceptPreOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          ParseTreeNode node = ancestors.node;
          if (!(node instanceof CssTree.Declaration)) { return true; }
          CssTree.Declaration decl = (CssTree.Declaration) node;
          if (null == decl.getProperty()) {
            ParseTreeNode parent = ancestors.getParentNode();
            if (parent instanceof MutableParseTreeNode) {
              ((MutableParseTreeNode) parent).removeChild(decl);
            }
          }
          return false;
        }
      }, t.parent);
  }
  private void removeEmptyRuleSets(AncestorChain<? extends CssTree> t) {
    t.node.acceptPreOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          ParseTreeNode node = ancestors.node;
          if (!(node instanceof CssTree.RuleSet)) { return true; }
          CssTree.RuleSet rset = (CssTree.RuleSet) node;
          List<? extends CssTree> children = rset.children();
          if (children.isEmpty()
              || (children.get(children.size() - 1)
                  instanceof CssTree.Selector)) {
            // No declarations, so get rid of it.
            ParseTreeNode parent = ancestors.getParentNode();
            if (parent instanceof MutableParseTreeNode) {
              ((MutableParseTreeNode) parent).removeChild(rset);
            }
          }
          return false;
        }
      }, t.parent);
  }
  private void namespaceIdents(AncestorChain<? extends CssTree> t) {
    // Namespace classes and ids
    t.node.acceptPreOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          ParseTreeNode node = ancestors.node;
          if (!(node instanceof CssTree.SimpleSelector)) { return true; }
          CssTree.SimpleSelector ss = (CssTree.SimpleSelector) node;
          List<? extends CssTree> children = ss.children();
          for (int i = 0, n = children.size(); i < n; ++i) {
            CssTree child = children.get(i);
            if (child instanceof CssTree.ClassLiteral) {
              CssTree.ClassLiteral classLit = (CssTree.ClassLiteral) child;
              CssTree prevSibling = i > 0 ? children.get(i - 1) : null;
              if (prevSibling instanceof CssTree.IdentLiteral
                  && "BODY".equalsIgnoreCase(
                      ((CssTree.IdentLiteral) prevSibling).getValue())) {
                // Don't rename a class if it applies to BODY.  See the code
                // below that allows body.ie6 for browser handling.
                return true;
              }

              classLit.setValue("." + meta.namespacePrefix + "-"
                                + classLit.getValue().substring(1));
            } else if (child instanceof CssTree.IdLiteral) {
              CssTree.IdLiteral idLit = (CssTree.IdLiteral) child;
              idLit.setValue("#" + meta.namespacePrefix + "-"
                             + idLit.getValue().substring(1));
            }
          }
          return true;
        }
      }, t.parent);
    // Make sure that each selector prefixed by a root rule
    t.node.acceptPreOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          ParseTreeNode node = ancestors.node;
          if (!(node instanceof CssTree.Selector)) { return true; }
          CssTree.Selector sel = (CssTree.Selector) node;
          if (sel.children().isEmpty()
              || !(sel.children().get(0) instanceof CssTree.SimpleSelector)) {
            // Remove from parent
            ParseTreeNode parent = ancestors.getParentNode();
            if (parent instanceof MutableParseTreeNode) {
              ((MutableParseTreeNode) parent).removeChild(sel);
            }
          } else {
            CssTree.SimpleSelector first =
                (CssTree.SimpleSelector) sel.children().get(0);
            // If this selector is like body.ie or body.firefox, move over
            // it so that it remains topmost
            if ("BODY".equalsIgnoreCase(first.getElementName())) {
              // The next part had better be a DESCENDANT combinator.
              ParseTreeNode it = null;
              if (sel.children().size() > 1) { it = sel.children().get(1); }
              if (it instanceof CssTree.Combination
                  && (CssTree.Combinator.DESCENDANT
                      == ((CssTree.Combination) it).getCombinator())) {
                first = (CssTree.SimpleSelector) sel.children().get(2);
              }
            }

            // Use the start position of the first item as the position of the
            // synthetic parts.
            FilePosition pos = FilePosition.startOf(first.getFilePosition());

            CssTree.Combination op = s(new CssTree.Combination(
                pos, CssTree.Combinator.DESCENDANT));

            CssTree.ClassLiteral prefixId = s(new CssTree.ClassLiteral(
                pos, "." + meta.namespacePrefix));
            CssTree.SimpleSelector prefixSel = s(new CssTree.SimpleSelector(
                pos, Collections.singletonList(prefixId)));

            sel.insertBefore(op, first);
            sel.insertBefore(prefixSel, op);
          }
          return false;
        }
      }, t.parent);
  }

  private static final Set<String> ALLOWED_PSEUDO_SELECTORS =
      new HashSet<String>(Arrays.asList(
          "link", "visited", "hover", "active", "first-child", "first-letter"
          ));
  void removeUnsafeConstructs(AncestorChain<? extends CssTree> t) {

    // 1) Check that all classes, ids, property names, etc. are valid
    //    css identifiers.
    t.node.acceptPreOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          ParseTreeNode node = ancestors.node;
          if (node instanceof CssTree.SimpleSelector) {
            for (CssTree child : ((CssTree.SimpleSelector) node).children()) {
              if (child instanceof CssTree.Pseudo) {
                child = child.children().get(0);
                // TODO(mikesamuel): check argument if child now a FunctionCall
              }
              String value = (String) child.getValue();
              if (value != null && !isSafeSelectorPart(value)) {
                mq.addMessage(PluginMessageType.UNSAFE_CSS_IDENTIFIER,
                              child.getFilePosition(),
                              MessagePart.Factory.valueOf(value));
                // Will be deleted by a later pass after all messages have been
                // generated
                node.getAttributes().set(CssValidator.INVALID, Boolean.TRUE);
                return false;
              }
            }
          } else if (node instanceof CssTree.Property) {
            CssTree.Property p = (CssTree.Property) node;
            if (!isSafeCssIdentifier(p.getPropertyName())) {
              mq.addMessage(PluginMessageType.UNSAFE_CSS_IDENTIFIER,
                            p.getFilePosition(),
                            MessagePart.Factory.valueOf(p.getPropertyName()));
              declarationFor(ancestors).getAttributes().set(
                  CssValidator.INVALID, Boolean.TRUE);
              return false;
            }
          }
          return true;
        }
      }, t.parent);

    // 2) Ban content properties, and attr pseudo selectors, and any other
    //    pseudo selectors that don't match the whitelist
    t.node.acceptPreOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          ParseTreeNode node = ancestors.node;
          if (node instanceof CssTree.Property) {
            if ("content".equalsIgnoreCase(
                ((CssTree.Property) node).getPropertyName())) {
              mq.addMessage(PluginMessageType.UNSAFE_CSS_PROPERTY,
                            node.getFilePosition(),
                            MessagePart.Factory.valueOf("content"));
              declarationFor(ancestors).getAttributes().set(
                  CssValidator.INVALID, Boolean.TRUE);
            }
          } else if (node instanceof CssTree.Pseudo) {
            boolean remove = false;
            CssTree child = ((CssTree.Pseudo) node).children().get(0);
            if (child instanceof CssTree.IdentLiteral) {
              if (!ALLOWED_PSEUDO_SELECTORS.contains(
                  ((CssTree.IdentLiteral) child).getValue().toLowerCase())) {
                mq.addMessage(PluginMessageType.UNSAFE_CSS_PSEUDO_SELECTOR,
                              node.getFilePosition(),
                              node);
                remove = true;
              }
            } else {
              StringBuilder rendered = new StringBuilder();
              TokenConsumer tc = new CssPrettyPrinter(rendered, null);
              node.render(new RenderContext(new MessageContext(), tc));
              mq.addMessage(PluginMessageType.UNSAFE_CSS_PSEUDO_SELECTOR,
                            node.getFilePosition(),
                            MessagePart.Factory.valueOf(rendered.toString()));
              remove = true;
            }
            if (remove) {
              // Delete the containing selector, since otherwise we'd broaden
              // the rule.
              selectorFor(ancestors).getAttributes().set(
                  CssValidator.INVALID, Boolean.TRUE);
            }
          }
          return true;
        }
      }, t.parent);
    // 3) Remove any properties and attributes that didn't validate
    t.node.acceptPreOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          ParseTreeNode node = ancestors.node;
          if (node instanceof CssTree.Property) {
            if (node.getAttributes().is(CssValidator.INVALID)) {
              declarationFor(ancestors).getAttributes().set(
                  CssValidator.INVALID, Boolean.TRUE);
            }
          } else if (node instanceof CssTree.Attrib) {
            if (node.getAttributes().is(CssValidator.INVALID)) {
              simpleSelectorFor(ancestors).getAttributes().set(
                  CssValidator.INVALID, Boolean.TRUE);
            }
          } else if (node instanceof CssTree.Term
                     && (CssPropertyPartType.URI ==
                         node.getAttributes().get(
                             CssValidator.CSS_PROPERTY_PART_TYPE))) {

            boolean remove = false;
            Message removeMsg = null;

            CssTree term = (CssTree.Term) node;
            CssTree.CssLiteral content =
                (CssTree.CssLiteral) term.children().get(0);

            if (content instanceof CssTree.Substitution) {
              return true;  // Handled by later pass.
            }

            String uriStr = content.getValue();
            try {
              URI uri = new URI(uriStr);
              ExternalReference ref
                  = new ExternalReference(uri, content.getFilePosition());
              // the same url check as GxpCompiler
              if (meta.getPluginEnvironment().rewriteUri(ref, "image/*")
                  == null) {
                removeMsg = new Message(
                    PluginMessageType.DISALLOWED_URI,
                    node.getFilePosition(),
                    MessagePart.Factory.valueOf(uriStr));
                remove = true;
              }
            } catch (URISyntaxException ex) {
              removeMsg = new Message(
                  PluginMessageType.DISALLOWED_URI,
                  node.getFilePosition(), MessagePart.Factory.valueOf(uriStr));
              remove = true;
            }

            if (remove) {
              // condemn the containing declaration
              CssTree.Declaration decl = declarationFor(ancestors);
              if (null != decl) {
                if (!decl.getAttributes().is(CssValidator.INVALID)) {
                  if (null != removeMsg) { mq.getMessages().add(removeMsg); }
                  decl.getAttributes().set(CssValidator.INVALID, Boolean.TRUE);
                }
              }
            }
          }
          return true;
        }
      }, t.parent);

    // 4) Remove invalid nodes
    t.node.acceptPreOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          ParseTreeNode node = ancestors.node;
          if (node.getAttributes().is(CssValidator.INVALID)) {
            ((MutableParseTreeNode) ancestors.parent.node).removeChild(node);
            return false;
          }
          return true;
        }
      }, t.parent);

    // 5) Cleanup.  Remove any rulesets with empty selectors
    // Since this is a post order traversal, we will first remove empty
    // selectors, and then consider any rulesets that have become empty due to
    // a lack of selectors.
    t.node.acceptPostOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          ParseTreeNode node = ancestors.node;
          if ((node instanceof CssTree.Selector && node.children().isEmpty())
              || (node instanceof CssTree.RuleSet
                  && (node.children().isEmpty()
                      || node.children().get(0) instanceof CssTree.Declaration))
              ) {
            ((MutableParseTreeNode) ancestors.parent.node).removeChild(node);
            return false;
          }
          return true;
        }
      }, t.parent);
  }

  private void translateUrls(AncestorChain<? extends CssTree> t) {
      t.node.acceptPreOrder(new Visitor() {
          public boolean visit(AncestorChain<?> ancestors) {
            ParseTreeNode node = ancestors.node;
            if (node instanceof CssTree.Term
                && CssPropertyPartType.URI ==
                node.getAttributes().get(
                    CssValidator.CSS_PROPERTY_PART_TYPE)) {
              CssTree term = (CssTree.Term) node;

              CssTree.CssLiteral content =
                  (CssTree.CssLiteral) term.children().get(0);
              if (content instanceof CssTree.Substitution) {
                return true;  // Handled by later pass.
              }

              String uriStr = content.getValue();
              try {
                URI uri = new URI(uriStr);
                // Rewrite the URI.
                // TODO(mikesamuel): for content: and other uri types, use
                // mime-type of text/*.
                ExternalReference ref
                    = new ExternalReference(uri, content.getFilePosition());
                String rewrittenUri = meta.getPluginEnvironment().rewriteUri(
                    ref, "image/*");
                content.setValue(rewrittenUri);
              } catch (URISyntaxException ex) {
                // Should've been checked in removeUnsafeConstructs.
                throw new AssertionError();
              }
            }
            return true;
          }
        }, t.parent);
  }

  private static CssTree.Declaration declarationFor(AncestorChain<?> chain) {
    for (AncestorChain<?> c = chain; null != c; c = c.parent) {
      if (c.node instanceof CssTree.Declaration) {
        return (CssTree.Declaration) c.node;
      }
    }
    return null;
  }

  private static CssTree.SimpleSelector simpleSelectorFor(
      AncestorChain<?> chain) {
    for (AncestorChain<?> c = chain; null != c; c = c.parent) {
      if (c.node instanceof CssTree.SimpleSelector) {
        return (CssTree.SimpleSelector) c.node;
      }
    }
    return null;
  }

  private static CssTree.Selector selectorFor(AncestorChain<?> chain) {
    for (AncestorChain<?> c = chain; null != c; c = c.parent) {
      if (c.node instanceof CssTree.Selector) {
        return (CssTree.Selector) c.node;
      }
    }
    return null;
  }

  private static final Pattern SAFE_SELECTOR_PART =
    Pattern.compile("^[#!\\.]?[a-zA-Z][_a-zA-Z0-9\\-]*$");
  /**
   * Restrict selectors to ascii characters until we can test browser handling
   * of escape sequences.
   */
  private static boolean isSafeSelectorPart(String s) {
    return SAFE_SELECTOR_PART.matcher(s).matches();
  }
  private static final Pattern SAFE_CSS_IDENTIFIER =
    Pattern.compile("^[a-zA-Z][_a-zA-Z0-9\\-]*$");
  /**
   * Restrict identifiers to ascii characters until we can test browser handling
   * of escape sequences.
   */
  private static boolean isSafeCssIdentifier(String s) {
    return SAFE_CSS_IDENTIFIER.matcher(s).matches();
  }
}
