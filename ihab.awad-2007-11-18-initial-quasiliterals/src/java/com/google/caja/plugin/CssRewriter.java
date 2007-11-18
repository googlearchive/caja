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

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssTree;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;

import java.io.IOException;
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
final class CssRewriter {
  private final PluginMeta meta;
  private final MessageQueue mq;

  CssRewriter(PluginMeta meta, MessageQueue mq) {
    assert null != mq;
    assert null != meta;
    this.meta = meta;
    this.mq = mq;
  }

  /**
   * Rewrite the given CSS tree to be safer and shorter.
   *
   * @param t non null.  modified in place.
   * @return true if the resulting tree is safe.
   */
  boolean rewrite(CssTree t) {
    boolean valid = true;
    // Once at the beginning, and again at the end.
    valid &= removeUnsafeConstructs(t);
    removeEmptyDeclarations(t);
    // After we remove declarations, we may have some rulesets without any
    // declarations which is technically illegal, so we remove rulesets without
    // declarations
    removeEmptyRuleSets(t);
    simplifyExprs(t);
    collapseDeclarations(t);
    collapseRulesets(t);
    if (null != meta.namespacePrefix) { namespaceIdents(t); }
    // Do this again to make sure no earlier changes introduce unsafe constructs
    valid &= removeUnsafeConstructs(t);

    translateUrls(t);

    return valid;
  }

  private void removeEmptyDeclarations(CssTree t) {
    t.acceptPreOrder(new Visitor() {
        public boolean visit(ParseTreeNode node) {
          if (!(node instanceof CssTree.Declaration)) { return true; }
          CssTree.Declaration decl = (CssTree.Declaration) node;
          if (null == decl.getProperty()) {
            ParseTreeNode parent = decl.getParent();
            if (parent instanceof MutableParseTreeNode) {
              ((MutableParseTreeNode) parent).removeChild(decl);
            }
          }
          return false;
        }
      });
  }
  private void removeEmptyRuleSets(CssTree t) {
    t.acceptPreOrder(new Visitor() {
        public boolean visit(ParseTreeNode node) {
          if (!(node instanceof CssTree.RuleSet)) { return true; }
          CssTree.RuleSet rset = (CssTree.RuleSet) node;
          List<? extends CssTree> children = rset.children();
          if (children.isEmpty()
              || (children.get(children.size() - 1)
                  instanceof CssTree.Selector)) {
            // No declarations, so get rid of it.
            ParseTreeNode parent = rset.getParent();
            if (parent instanceof MutableParseTreeNode) {
              ((MutableParseTreeNode) parent).removeChild(rset);
            }
          }
          return false;
        }
      });
  }
  private void simplifyExprs(CssTree t) {
    t.acceptPreOrder(new Visitor() {
      public boolean visit(ParseTreeNode node) {
        if (!(node instanceof CssTree.Term)) { return true; }
        // #ffffff -> #fff
        // lengths such as 0 0 0 0 -> 0
        // rgb(0, 0, 0) -> #000
        // TODO
        return true;
      }
    });
  }
  private void collapseRulesets(@SuppressWarnings("unused") CssTree t) {
    // Walk over the declarations, sort the properties, use them to generate a
    // fingerprint as a key into a mapping from fingerprints -> declaration
    // lists.
    // For each list with more than 1 declaration, pull all the selectors into
    // the first declaration and get rid of the rest.
    // TODO(msamuel): implement
  }
  private void collapseDeclarations(@SuppressWarnings("unused") CssTree t) {
    // If the same set of selectors appears in multiple places, collapse the
    // properties
    // TODO(msamuel): implement
  }
  private void namespaceIdents(CssTree t) {
    // Namespace classes and ids
    t.acceptPreOrder(new Visitor() {
        public boolean visit(ParseTreeNode node) {
          if (!(node instanceof CssTree.SimpleSelector)) { return true; }
          CssTree.SimpleSelector ss = (CssTree.SimpleSelector) node;
          for (CssTree child : ss.children()) {
            if (child instanceof CssTree.ClassLiteral) {
              CssTree.ClassLiteral classLit = (CssTree.ClassLiteral) child;
              if (classLit.getPrevSibling() instanceof CssTree.IdentLiteral
                  && "BODY".equalsIgnoreCase(
                      ((CssTree.IdentLiteral) classLit.getPrevSibling())
                      .getValue())) {
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
      });
    // Make sure that each selector prefixed by a root rule
    t.acceptPreOrder(new Visitor() {
        public boolean visit(ParseTreeNode node) {
          if (!(node instanceof CssTree.Selector)) { return true; }
          CssTree.Selector sel = (CssTree.Selector) node;
          if (sel.children().isEmpty()
              || !(sel.children().get(0) instanceof CssTree.SimpleSelector)) {
            // Remove from parent
            ParseTreeNode parent = sel.getParent();
            if (parent instanceof MutableParseTreeNode) {
              ((MutableParseTreeNode) parent).removeChild(sel);
            }
          } else {
            CssTree.SimpleSelector first =
                (CssTree.SimpleSelector) sel.children().get(0);
            // If this selector is like body.ie or body.firefox, move over
            // it so that it remains topmost
            if ("BODY".equalsIgnoreCase(first.getElementName())) {
              // the next part had better be a DESCENDANT combinator
              ParseTreeNode it = first.getNextSibling();
              if (it instanceof CssTree.Combination
                  && (CssTree.Combinator.DESCENDANT
                      == ((CssTree.Combination) it).getCombinator())) {
                first = (CssTree.SimpleSelector) it.getNextSibling();
              }
            }

            // Use the start position of the first item as the position of the
            // synthetic parts.
            FilePosition pos = FilePosition.startOf(first.getFilePosition());

            CssTree.Combination op = new CssTree.Combination(
                pos, CssTree.Combinator.DESCENDANT);
            op.getAttributes().set(ExpressionSanitizer.SYNTHETIC, Boolean.TRUE);

            CssTree.ClassLiteral prefixId = new CssTree.ClassLiteral(
                pos, "." + meta.namespacePrefix);
            prefixId.getAttributes().set(
                ExpressionSanitizer.SYNTHETIC, Boolean.TRUE);
            CssTree.SimpleSelector prefixSel = new CssTree.SimpleSelector(
                pos, Collections.singletonList(prefixId));
            prefixSel.getAttributes().set(
                ExpressionSanitizer.SYNTHETIC, Boolean.TRUE);

            sel.insertBefore(op, first);
            sel.insertBefore(prefixSel, op);
          }
          return false;
        }
      });
  }

  private static final Set<String> ALLOWED_PSEUDO_SELECTORS =
      new HashSet<String>(Arrays.asList(
          "link", "visited", "hover", "active", "first-child", "first-letter"
          ));
  boolean removeUnsafeConstructs(CssTree t) {
    final Switch rewrote = new Switch();

    // 1) Check that all classes, ids, property names, etc. are valid
    //    css identifiers.
    t.acceptPreOrder(new Visitor() {
        public boolean visit(ParseTreeNode node) {
          if (node instanceof CssTree.SimpleSelector) {
            for (CssTree child : ((CssTree.SimpleSelector) node).children()) {
              if (child instanceof CssTree.Pseudo) {
                child = child.children().get(0);
                // TODO: check argument if child now instanceof FunctionLiteral
              }
              String value = (String) child.getValue();
              if (!isSafeSelectorPart(value)) {
                mq.addMessage(PluginMessageType.UNSAFE_CSS_IDENTIFIER,
                              child.getFilePosition(),
                              MessagePart.Factory.valueOf(value));
                // Will be deleted by a later pass after all messages have been
                // generated
                node.getAttributes().set(CssValidator.INVALID, Boolean.TRUE);
                rewrote.set();
                return false;
              }
            }
          } else if (node instanceof CssTree.Property) {
            CssTree.Property p = (CssTree.Property) node;
            if (!isSafeCssIdentifier(p.getPropertyName())) {
              mq.addMessage(PluginMessageType.UNSAFE_CSS_IDENTIFIER,
                            p.getFilePosition(),
                            MessagePart.Factory.valueOf(p.getPropertyName()));
              declarationFor(p).getAttributes().set(
                  CssValidator.INVALID, Boolean.TRUE);
              rewrote.set();
              return false;
            }
          }
          return true;
        }
      });

    // 2) Ban content properties, and attr pseudo selectors, and any other
    //    pseudo selectors that don't match the whitelist
    t.acceptPreOrder(new Visitor() {
        public boolean visit(ParseTreeNode node) {
          if (node instanceof CssTree.Property) {
            if ("content".equalsIgnoreCase(
                ((CssTree.Property) node).getPropertyName())) {
              mq.addMessage(PluginMessageType.UNSAFE_CSS_PROPERTY,
                            node.getFilePosition(),
                            MessagePart.Factory.valueOf("content"));
              declarationFor(node).getAttributes().set(
                  CssValidator.INVALID, Boolean.TRUE);
              rewrote.set();
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
                rewrote.set();
                remove = true;
              }
            } else {
              StringBuilder rendered = new StringBuilder();
              try {
                node.render(new RenderContext(new MessageContext(), rendered));
              } catch (IOException ex) {
                throw (AssertionError) new AssertionError(
                    "IOException writing to StringBuilder").initCause(ex);
              }
              mq.addMessage(PluginMessageType.UNSAFE_CSS_PSEUDO_SELECTOR,
                            node.getFilePosition(),
                            MessagePart.Factory.valueOf(rendered.toString()));
              rewrote.set();
              remove = true;
            }
            if (remove) {
              // Delete the containing selector, since otherwise we'd broaden
              // the rule.
              selectorFor(node).getAttributes().set(
                  CssValidator.INVALID, Boolean.TRUE);
            }
          }
          return true;
        }
      });
    // 3) Remove any properties and attributes that didn't validate
    t.acceptPreOrder(new Visitor() {
        public boolean visit(ParseTreeNode node) {
          if (node instanceof CssTree.Property) {
            if (Boolean.TRUE.equals(node.getAttributes().get(
                                        CssValidator.INVALID))) {
              declarationFor(node).getAttributes().set(
                  CssValidator.INVALID, Boolean.TRUE);
              rewrote.set();
            }
          } else if (node instanceof CssTree.Attrib) {
            if (Boolean.TRUE.equals(node.getAttributes().get(
                                        CssValidator.INVALID))) {
              simpleSelectorFor(node).getAttributes().set(
                  CssValidator.INVALID, Boolean.TRUE);
              rewrote.set();
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
            String uriStr = content.getValue();
            try {
              URI uri = new URI(uriStr);
              // the same url check as GxpCompiler
              if (!UrlUtil.isDomainlessUrl(uri)) {
                removeMsg = new Message(
                    PluginMessageType.EXPECTED_RELATIVE_URL,
                    node.getFilePosition(),
                    MessagePart.Factory.valueOf(uriStr));
                rewrote.set();
                remove = true;
              }
            } catch (URISyntaxException ex) {
              removeMsg = new Message(
                  PluginMessageType.MALFORMED_URL,
                  node.getFilePosition(), MessagePart.Factory.valueOf(uriStr));
              rewrote.set();
              remove = true;
            }

            if (remove) {
              // condemn the containing declaration
              CssTree.Declaration decl = declarationFor(term);
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
      });

    // 4) Remove invalid nodes
    t.acceptPreOrder(new Visitor() {
        public boolean visit(ParseTreeNode node) {
          if (node.getAttributes().is(CssValidator.INVALID)) {
            ((MutableParseTreeNode) node.getParent()).removeChild(node);
            return false;
          }
          return true;
        }
      });

    // 5) Cleanup.  Remove any rulesets with empty selectors
    // Since this is a post order traversal, we will first remove empty
    // selectors, and then consider any rulesets that have become empty due to
    // a lack of selectors.
    t.acceptPostOrder(new Visitor() {
        public boolean visit(ParseTreeNode node) {
          if ((node instanceof CssTree.Selector && node.children().isEmpty())
              || (node instanceof CssTree.RuleSet
                  && (node.children().isEmpty()
                      || node.children().get(0) instanceof CssTree.Declaration))
              ) {
            ((MutableParseTreeNode) node.getParent()).removeChild(node);
            return false;
          }
          return true;
        }
      });

    return !rewrote.get();
  }

  private void translateUrls(CssTree t) {
      t.acceptPreOrder(new Visitor() {
          public boolean visit(ParseTreeNode node) {
            if (node instanceof CssTree.Term
                && CssPropertyPartType.URI ==
                node.getAttributes().get(
                    CssValidator.CSS_PROPERTY_PART_TYPE)) {
              CssTree term = (CssTree.Term) node;

              CssTree.CssLiteral content =
                  (CssTree.CssLiteral) term.children().get(0);
              String uriStr = content.getValue();
              try {
                URI uri = new URI(uriStr);
                // prefix the uri properly
                content.setValue(UrlUtil.translateUrl(uri, meta.pathPrefix));
              } catch (URISyntaxException ex) {
                // should've been checked in removeUnsafeConstructs
                throw new AssertionError();
              }
            }
            return true;
          }
        });
  }

  private static CssTree.Declaration declarationFor(ParseTreeNode node) {
    for (ParseTreeNode p = node; null != p; p = p.getParent()) {
      if (p instanceof CssTree.Declaration) {
        return (CssTree.Declaration) p;
      }
    }
    return null;
  }

  private static CssTree.SimpleSelector simpleSelectorFor(ParseTreeNode node) {
    for (ParseTreeNode p = node; null != p; p = p.getParent()) {
      if (p instanceof CssTree.SimpleSelector) {
        return (CssTree.SimpleSelector) p;
      }
    }
    return null;
  }

  private static CssTree.Selector selectorFor(ParseTreeNode node) {
    for (ParseTreeNode p = node; null != p; p = p.getParent()) {
      if (p instanceof CssTree.Selector) {
        return (CssTree.Selector) p;
      }
    }
    return null;
  }

  private static final Pattern SAFE_SELECTOR_PART =
    Pattern.compile("^[#!\\.]?[a-zA-Z][a-zA-Z0-9\\-]*$");
  /**
   * Restrict selectors to ascii characters until we can test browser handling
   * of escape sequences.
   */
  private static boolean isSafeSelectorPart(String s) {
    return SAFE_SELECTOR_PART.matcher(s).matches();
  }
  private static final Pattern SAFE_CSS_IDENTIFIER =
    Pattern.compile("^[a-zA-Z][a-zA-Z0-9\\-]*$");
  /**
   * Restrict identifiers to ascii characters until we can test browser handling
   * of escape sequences.
   */
  private static boolean isSafeCssIdentifier(String s) {
    return SAFE_CSS_IDENTIFIER.matcher(s).matches();
  }

  private static final class Switch {
    private boolean on;

    public boolean get() { return on; }
    public void set() { this.on = true; }
  }
}
