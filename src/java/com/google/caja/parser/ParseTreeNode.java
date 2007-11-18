// Copyright (C) 2005 Google Inc.
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

package com.google.caja.parser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.Token;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.Renderable;
import com.google.caja.util.SyntheticAttributes;

/**
 * A node in a parse tree.
 *
 * @author mikesamuel@gmail.com
 * @author ihab.awad@gmail.com
 */
public interface ParseTreeNode extends MessagePart, Renderable {

  /**
   * @return a ParseTreeNode such that this is in
   *   <code>getParent().children()</code>, or null if this is a root node or
   *   the tree is in the process of being built.
   */
  ParseTreeNode getParent();
  /**
   * The node that occurs after this in its {@link #getParent parent}'s
   * {@link #children child list}.
   */
  ParseTreeNode getNextSibling();
  /**
   * The node that occurs before this in its {@link #getParent parent}'s
   * {@link #children child list}.
   */
  ParseTreeNode getPrevSibling();
  FilePosition getFilePosition();
  List<Token<?>> getComments();
  /**
   * @return null or a value with subclass specific meaning which encapsulates
   *     all parsed state separate from the children.
   */
  Object getValue();
  /**
   * A set of properties that may be used by visitors and inspectors to
   * store information computed about a node such as it's type, the symbol
   * it refers to, etc.
   */
  SyntheticAttributes getAttributes();

  void formatTree(MessageContext context, int depth, Appendable out)
      throws IOException;

  /** An immutable list of children. */
  List<? extends ParseTreeNode> children();

  /**
   * Applies the given visitor to children in a pre-order traversal, skipping
   * traversal of a subtree if {@link Visitor#visit} of the root node returns
   * false.
   *
   * @return true iff visiting the root node yielded true.
   */
  boolean acceptPreOrder(Visitor v);

  /**
   * Like {@link #acceptPreOrder}, but post-order.
   *
   * @return true iff visiting the root node yielded true.
   */
  boolean acceptPostOrder(Visitor v);

  /**
   * Like {@link #acceptPreOrder}, but in breadth-first order.
   *
   * @return true iff visiting the root node yielded true.
   */
  boolean acceptBreadthFirst(Visitor v);
  
  /**
   * Describes the quantifier of a quasiliteral node. 
   */
  enum QuasiliteralQuantifier {
    SINGLE(""),
    MULTIPLE("*"),
    MULTIPLE_NONEMPTY("+");
    
    private final String suffix;
    
    private QuasiliteralQuantifier(String suffix) {
      this.suffix = suffix;
    }
    
    public String getSuffix() {
      return suffix;
    }
  }

  /**
   * @return whether this node is a quasiliteral.
   */
  boolean isQuasiliteral();
  
  /**
   * If this node is a quasiliteral, returns the quantifier.
   *  
   * @return the quantifier, or null if this node is not a quasiliteral.
   */
  QuasiliteralQuantifier getQuasiliteralQuantifier();
  
  /**
   * If this node is a quasiliteral, returns the identifier.
   *  
   * @return the identifier, or null if this node is not a quasiliteral.
   */
  String getQuasiliteralIdentifier();
  
  /**
   * Return the common superclass of all nodes that should be matched by this
   * quasiliteral node.
   * 
   * @return a class, or null if this node is not a quasiliteral.
   */
  Class<? extends ParseTreeNode> getQuasiMatchedClass();
  
  /**
   * Do the same as the <code>match(...)</code> method, but only at the topmost
   * level of the specimen, i.e., do not recurse into subtrees of the specimen.
   * 
   * <p>Note that, if this method returns some QuasiliteralMatch object
   * <code>m</code>, it is guaranteed that
   * <code>m.getMatchRoot() == specimen</code>.
   *
   * <p>TODO(ihab): Make all implementations put *copies* of matched nodes
   * into the map.
   *
   * @param specimen another ParseTreeNode.
   * @return a map of bindings from quasiliteral variable names to values,
   * or null if this pattern did not match at the top level of the specimen.
   */
  Map<String, ParseTreeNode> matchHere(ParseTreeNode specimen);
  
  // Treating this node as a pattern, attempt to substitute nodes from the
  // supplied map into the pattern, and return a newly constructed tree with
  // the substitutions made.
  //
  // Make all implementations return a *copy* of the filled-in AST. Otherwise,
  // we are returning a partially filled-in tree if 'false'.
  //
  // @param map a map containing quasiliteral bindings.
  // @return whether the substitution succeeded.
  //
  // TODO(ihab): boolean substitute(Map<String, ParseTreeNode> map);
  
  /**
   * Determines whether this node is shallowly equal to 'specimen', i.e.,
   * whether the contents of 'this' and 'specimen' are equal without considering
   * the state of the children of either one.
   * 
   * @param specimen a ParseTreeNode.
   * @return whether 'this' is shallowly equal to 'specimen'.
   */
  boolean shallowEquals(ParseTreeNode specimen);

  /**
   * Determines whether this node and all its children are equal to 'specimen'.
   * 
   * @param specimen a ParseTreeNode.
   * @return whether 'this' is deeply equal to 'specimen'.
   */
  boolean deepEquals(ParseTreeNode specimen);
}
