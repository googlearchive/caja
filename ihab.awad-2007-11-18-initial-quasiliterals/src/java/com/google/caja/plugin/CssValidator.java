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

import com.google.caja.html.HTML4;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssPropertySignature;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.css.Css2;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageTypeInt;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.SyntheticAttributeKey;
import com.google.caja.util.SyntheticAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A class that validates a CSS parse tree and annotates the terms with type
 * information.
 *
 * @author mikesamuel@gmail.com
 */
public final class CssValidator {

  /**
   * Which part did a term, match?   For example, a term in a font property
   * might be a font-weight, a font-size, or a line-height, among others.
   * @see com.google.caja.parser.css.CssTree.Term
   */
  public static final SyntheticAttributeKey<String>
    CSS_PROPERTY_PART = new SyntheticAttributeKey<String>(
        String.class, "cssPropertyPart");
  /**
   * What type is a term?   A term might be an absolute-size, a uri, etc.
   * @see com.google.caja.parser.css.CssTree.Term
   */
  public static final SyntheticAttributeKey<CssPropertyPartType>
    CSS_PROPERTY_PART_TYPE = new SyntheticAttributeKey<CssPropertyPartType>(
        CssPropertyPartType.class, "cssPropertyPartType");

  /**
   * Used to mark invalid nodes.  Default to false.
   */
  public static final SyntheticAttributeKey<Boolean> INVALID =
    new SyntheticAttributeKey<Boolean>(Boolean.class, "cssValidator-invalid");

  private final MessageQueue mq;
  public CssValidator(MessageQueue mq) {
    if (null == mq) { throw new NullPointerException(); }
    this.mq = mq;
  }

  /** True iff the given css tree is valid according to CSS2. */
  public boolean validateCss(CssTree css) {
    if (css instanceof CssTree.Declaration) {
      return validateDeclaration((CssTree.Declaration) css);
    } else if (css instanceof CssTree.Attrib) {
      return validateAttrib((CssTree.Attrib) css);
    } else if (css instanceof CssTree.SimpleSelector) {
      if (!validateSimpleSelector((CssTree.SimpleSelector) css)) {
        return false;
      }
    }

    boolean valid = true;
    for (CssTree child : css.children()) {
      if (null != child) {
        valid &= validateCss(child);
      }
    }
    return valid;
  }

  /**
   * For each property, apply the signature, and try to identify which parts
   * are URLs, etc., so that we can maintain invariants across terms, such
   * as the "urls must be under a particular domain invariant".
   */
  private boolean validateDeclaration(CssTree.Declaration decl) {
    // Is it an empty declaration?  Effectively a noop, but the CSS2 spec
    // insists that a noop is a full-class declaration.
    if (decl.children().isEmpty()) { return true; }
    CssTree.Property prop = decl.getProperty();
    Css2.CssPropertyInfo pinfo = Css2.getCssProperty(prop.getPropertyName());
    if (null == pinfo) {
      mq.addMessage(
          PluginMessageType.UNKNOWN_CSS_PROPERTY, prop.getFilePosition(),
          MessagePart.Factory.valueOf(prop.getPropertyName()));
      decl.getAttributes().set(INVALID, Boolean.TRUE);
      return false;
    }
    // Apply the signature
    if (!applySignature(pinfo.name, decl.getExpr(), pinfo.sig)) {
      // Apply takes care of adding the error message
      decl.getAttributes().set(INVALID, Boolean.TRUE);
      return false;
    }
    return true;
  }

  /**
   * Tags must exist in html 4 whitelist.
   */
  private boolean validateSimpleSelector(CssTree.SimpleSelector sel) {
    String tagName = sel.getElementName();
    if (null == tagName) { return true; }
    tagName = tagName.toUpperCase();
    if (null != HTML4.lookupElement(tagName)) {
      if (HtmlWhitelist.ALLOWED_TAGS.contains(tagName)
          // Make an exception for BODY which is handled specially by the
          // rewriter and which can be used as the basis for browser specific
          // rules, e.g.  body.ie6 p { ... }
          // TODO(msamuel): parameterize the whitelist
          || "BODY".equals(tagName)) {
        return true;
      }
      mq.addMessage(PluginMessageType.UNSAFE_TAG, sel.getFilePosition(),
                    MessagePart.Factory.valueOf(tagName));
    } else {
      mq.addMessage(
          PluginMessageType.UNKNOWN_TAG, sel.getFilePosition(),
          MessagePart.Factory.valueOf(sel.getElementName()));
    }
    sel.getAttributes().set(INVALID, Boolean.TRUE);
    return false;
  }

  /**
   * Attrib must exist in html 4 whitelist.
   */
  private boolean validateAttrib(CssTree.Attrib attr) {
    String attribName = attr.getIdent().toUpperCase();
    if (null != HTML4.getWhitelist().lookupAttribute(attribName)) {
      return true;
    } else {
      mq.addMessage(
          PluginMessageType.UNKNOWN_ATTRIBUTE, attr.getFilePosition(),
          MessagePart.Factory.valueOf(attr.getIdent()),
          MessagePart.Factory.valueOf("{css selector}"));
      attr.getAttributes().set(INVALID, Boolean.TRUE);
      return false;
    }
  }

  /**
   * Applies the given signature to the given css expression, returning true if
   * the expression fits.
   * @param propertyName the name of the css property that has sig as its
   *   signature.  Used to generate the {@link #CSS_PROPERTY_PART} attribute
   *   for the terms in expr.
   * @param expr the expression to apply to.  non null.
   * @param sig the signature that expr should match.
   * @return true if sig applies to expr.  If true, then the terms in expr will
   *   have their {@link #CSS_PROPERTY_PART} and {@link #CSS_PROPERTY_PART_TYPE}
   *   attributes set.
   */
  private boolean applySignature(
      String propertyName, CssTree.Expr expr, CssPropertySignature sig) {
    SignatureResolver resolver = new SignatureResolver(expr);
    List<Candidate> matches = resolver.applySignature(
        Collections.singletonList(new Candidate(0, null, null)),
        propertyName, sig);

    // Filter out matches that haven't consumed the entire expr
    int end = expr.children().size();
    for (Iterator<Candidate> it = matches.iterator(); it.hasNext();) {
      Candidate match = it.next();
      if (match.exprIdx != end) { it.remove(); }
    }

    if (matches.isEmpty()) {
      // Use the longest match attempted match to generate an error message
      Candidate best = resolver.getBestAttempt();
      int exprIdx = null != best ? best.exprIdx : 0;

      StringBuilder buf = new StringBuilder();
      RenderContext rc = new RenderContext(new MessageContext(), buf);
      boolean needsSpace = false;
      int k = 0;
      for (CssTree child : expr.children()) {
        if (needsSpace) {
          buf.append(' ');
        }
        int len = buf.length();
        try {
          if (k++ == exprIdx) {
            buf.append(" ==>");
            child.render(rc);
            buf.append("<== ");
          } else {
            child.render(rc);
          }
        } catch (IOException ex) {
          throw (AssertionError) new AssertionError(
              "IOException thrown writing to StringBuilder").initCause(ex);
        }
        needsSpace = (len < buf.length());
      }
      mq.addMessage(
          PluginMessageType.MALFORMED_CSS_PROPERTY_VALUE,
          expr.getFilePosition(), MessagePart.Factory.valueOf(propertyName),
          MessagePart.Factory.valueOf(buf.toString().trim()));

      expr.getAttributes().set(INVALID, Boolean.TRUE);
      return false;
    }

    Candidate c = matches.get(0);
    // Apply matches
    for (Match m = c.match; null != m; m = m.prev) {
      SyntheticAttributes attribs = m.term.getAttributes();
      attribs.set(CSS_PROPERTY_PART_TYPE, m.type);
      attribs.set(CSS_PROPERTY_PART, m.propertyName);
    }
    // Deliver warnings
    if (null != c.warning) { c.warning.toMessageQueue(mq); }
    return true;
  }
}

/** A possible match of a css expression to a css property signature. */
final class Candidate {
  int exprIdx;
  Match match;
  MessageSList warning;

  Candidate(int exprIdx, Match match, MessageSList warning) {
    this.exprIdx = exprIdx;
    this.match = match;
    this.warning = warning;
  }

  void match(
      CssTree.Term term, CssPropertyPartType type, String propertyName) {
    this.match = new Match(term, type, propertyName, this.match);
  }

  void warn(MessageTypeInt msgType, MessagePart... parts) {
    warning = new MessageSList(new Message(msgType, parts), this.warning);
  }

  @Override
  protected Candidate clone() {
    return new Candidate(exprIdx, match, warning);
  }
}

/**
 * For each term that matches part of a property signature, the part it
 * matches and the type it takes.
 * Used to fill {@link CssValidator#CSS_PROPERTY_PART} and
 * {@link CssValidator#CSS_PROPERTY_PART_TYPE}
 */
final class Match {
  final CssTree.Term term;
  final CssPropertyPartType type;
  final String propertyName;
  final Match prev;

  Match(CssTree.Term term, CssPropertyPartType type,
        String propertyName, Match prev) {
    this.term = term;
    this.type = type;
    this.propertyName = propertyName;
    this.prev = prev;
  }
}

final class MessageSList {
  final Message msg;
  final MessageSList prev;

  MessageSList(Message msg, MessageSList prev) {
    this.msg = msg;
    this.prev = prev;
  }

  void toMessageQueue(MessageQueue mq) {
    if (null != prev) { prev.toMessageQueue(mq); }
    mq.getMessages().add(msg);
  }
}

/**
 * Resolves a css property signature against a Css expression, marking
 * each of the terms with a type, and the sub-rule that matched it.
 */
final class SignatureResolver {

  /**
   * The best match so far.  Used to generate an informative error
   * message if the applifcation fails.
   */
  private Candidate best;
  /** The css expression.  Non null. */
  private final CssTree.Expr expr;

  SignatureResolver(CssTree.Expr expr) {
    this.expr = expr;
  }

  Candidate getBestAttempt() { return best; }

  /**
   * Given a list of candidates, apply the given signature and return any more
   * candidates.  The candidates may multiple when a signature can be applied
   * in multiple ways.
   * @param candidates the candidates to apply to the signature.
   * @param propertyName the name of the property that we're applying this
   *   signature for.  This will appear in {@link Match#propertyName} and
   *   eventually in {@link CssValidator#CSS_PROPERTY_PART}.
   * @param sig the signature to apply expr to.  Non null.
   * @return the candidates that still match, some possibly modified in place.
   *   The output list may be larger or smaller than the input list.  An empty
   *   list indicates no possible matches.
   */
  List<Candidate> applySignature(
      List<Candidate> candidates, String propertyName,
      CssPropertySignature sig) {

    List<Candidate> passed = new ArrayList<Candidate>();

    for (Candidate candidate : candidates) {

      // Have we reached the end of the input?
      if (checkEnd(candidate, sig, passed)) {
        continue;
      }

      skipBlank(candidate);

      // The exprIdx after sig has been processed.
      if (sig instanceof CssPropertySignature.SetSignature) {
        applySetSignature((CssPropertySignature.SetSignature) sig,
                          candidate, propertyName, passed);
      } else if (sig instanceof CssPropertySignature.SeriesSignature) {
        applySeriesSignature(
            (CssPropertySignature.SeriesSignature) sig,
            candidate, propertyName, passed);
      } else if (sig instanceof CssPropertySignature.RepeatedSignature) {
        applyRepeatedSignature(
            (CssPropertySignature.RepeatedSignature) sig,
            candidate, propertyName, passed);
      } else if (sig instanceof CssPropertySignature.LiteralSignature) {
        applyLiteralSignature(
            (CssPropertySignature.LiteralSignature) sig,
            candidate, propertyName, passed);
      } else if (sig instanceof CssPropertySignature.SymbolSignature) {
        applySymbolSignature(
            (CssPropertySignature.SymbolSignature) sig,
            candidate, propertyName, passed);
      } else if (sig instanceof CssPropertySignature.PropertyRefSignature) {
        applyPropertyRefSignature(
            (CssPropertySignature.PropertyRefSignature) sig,
            candidate, propertyName, passed);
      } else if (sig instanceof CssPropertySignature.CallSignature) {
        applyCallSignature(
            (CssPropertySignature.CallSignature) sig,
            candidate, propertyName, passed);
      } else {
        throw new AssertionError(sig.getClass().getName());
      }
    }

    for (Candidate candidate : passed) {
      if (null == best || best.exprIdx < candidate.exprIdx) {
        best = candidate;
      }
    }
    return passed;
  }

  private boolean checkEnd(
      Candidate candidate, CssPropertySignature sig, List<Candidate> passed) {
    if (candidate.exprIdx == expr.children().size()) {
      if (sig instanceof CssPropertySignature.RepeatedSignature
          && 0 == ((CssPropertySignature.RepeatedSignature) sig).minCount) {
        // A repeating item that requires 0 still passes
        passed.add(candidate);
      }
      return true;
    }
    return false;
  }

  private void skipBlank(Candidate candidate) {
    // Skip over any blank operators
    CssTree child = expr.children().get(candidate.exprIdx);
    if (child instanceof CssTree.Operation
        && (CssTree.Operator.NONE
            == ((CssTree.Operation) child).getOperator())) {
      ++candidate.exprIdx;
    }
  }

  private void applySetSignature(
      CssPropertySignature.SetSignature ssig,
      Candidate candidate, String propertyName, List<Candidate> passed) {
    List<Candidate> toApply = Collections.singletonList(candidate);
    for (CssPropertySignature setElement : ssig.children()) {
      List<Candidate> elementsPassed = applySignature(
          toApply, propertyName, setElement);
      // lazy
      if (!elementsPassed.isEmpty()) {
        passed.addAll(elementsPassed);
        break;
      }
    }
  }

  private void applyExclusiveSetSignature(
      CssPropertySignature.ExclusiveSetSignature ssig,
      Candidate candidate, String propertyName,
      BitSet used, List<Candidate> passed) {
    List<Candidate> toApply = Collections.singletonList(candidate);
    int k = -1;
    for (CssPropertySignature setElement : ssig.children()) {
      if (used.get(++k)) { continue; }
      List<Candidate> elementsPassed = applySignature(
          toApply, propertyName, setElement);
      // lazy
      if (!elementsPassed.isEmpty()) {
        passed.addAll(elementsPassed);
        used.set(k);
        break;
      }
    }
  }

  private void applySeriesSignature(
      CssPropertySignature.SeriesSignature ssig,
      Candidate candidate, String propertyName,
      List<Candidate> passed) {
    List<Candidate> toApply = Collections.singletonList(candidate);
    for (CssPropertySignature seriesElement : ssig.children()) {
      toApply = applySignature(toApply, propertyName, seriesElement);
      if (toApply.isEmpty()) { break; }
    }
    passed.addAll(toApply);
  }

  private void applyRepeatedSignature(
      CssPropertySignature.RepeatedSignature rsig,
      Candidate candidate, String propertyName, List<Candidate> passed) {

    // The maximum branching factor for a repetition.  This is the
    // greatest number of contiguous ambiguous elements we might encounter
    // as in { font: inherit inherit inherit inherit }
    final int MAX_BRANCHING_FACTOR = 4;

    List<Candidate> toApply = Collections.singletonList(candidate);
    int k = 0;
    for (; k < rsig.minCount; ++k) {
      toApply = applySignature(
          toApply, propertyName, rsig.getRepeatedSignature());
      if (toApply.isEmpty()) { break; }
    }
    if (!toApply.isEmpty()) {
      CssPropertySignature repeated = rsig.getRepeatedSignature();
      BitSet used = null;
      if (repeated instanceof CssPropertySignature.ExclusiveSetSignature) {
        used = new BitSet(repeated.children().size());
      }

      toApply = new ArrayList<Candidate>(toApply);
      for (; k < rsig.maxCount; ++k) {
        if (k < MAX_BRANCHING_FACTOR) {
          // Try not following the extra repetitions
          passed.addAll(toApply);
          for (int i = toApply.size(); --i >= 0;) {
            toApply.set(i, toApply.get(i).clone());
          }
        } else {
          // greedy
        }
        if (null == used) {
          toApply = applySignature(toApply, propertyName, repeated);
        } else {
          // Special handling for || groups
          List<Candidate> passedSet  = new ArrayList<Candidate>();
          for (Candidate setCandidate : toApply) {
            if (setCandidate.exprIdx == expr.children().size()) { continue; }

            skipBlank(setCandidate);

            applyExclusiveSetSignature(
                (CssPropertySignature.ExclusiveSetSignature) repeated,
                setCandidate, propertyName, used, passedSet);
          }
          toApply = passedSet;
        }
        if (toApply.isEmpty()) { break; }
      }
      passed.addAll(toApply);
    }
  }

  private void applyLiteralSignature(
      CssPropertySignature.LiteralSignature literal,
      Candidate candidate, String propertyName, List<Candidate> passed) {

    if (0 == (candidate.exprIdx & 1)) {  // a term
      CssTree.Term term =
        (CssTree.Term) expr.children().get(candidate.exprIdx);
      CssTree.CssExprAtom atom = term.getExprAtom();
      if (null == term.getOperator()
          && (atom instanceof CssTree.IdentLiteral
              || atom instanceof CssTree.QuantityLiteral)
          && literal.value.equals(atom.getValue())) {
        candidate.match(term, CssPropertyPartType.IDENT, propertyName);
        ++candidate.exprIdx;
        passed.add(candidate);
      }
    } else {  // A punctuation mark
      CssTree.Operation op =
        (CssTree.Operation) expr.children().get(candidate.exprIdx);
      if (op.getOperator().getSymbol().equals(literal.getValue())) {
        ++candidate.exprIdx;
        passed.add(candidate);
      }
    }
  }

  private void applySymbolSignature(
      CssPropertySignature.SymbolSignature ssig,
      Candidate candidate, String propertyName, List<Candidate> passed) {

    Css2.SymbolInfo symbolInfo = Css2.getSymbol(
        ssig.symbolName.toLowerCase());
    if (null != symbolInfo) {
      if (false) {
        System.err.println(
            "symbol " + symbolInfo.name + " from " + propertyName + "\n"
            + dump(symbolInfo.sig));
      }
      passed.addAll(applySignature(
                        Collections.singletonList(candidate),
                        propertyName + "::" + symbolInfo.name,
                        symbolInfo.sig));
    } else if (symbolMatch(candidate, propertyName, ssig)) {
      passed.add(candidate);
    }
  }

  private void applyPropertyRefSignature(
      CssPropertySignature.PropertyRefSignature ssig,
      Candidate candidate, String propertyName, List<Candidate> passed) {

    Css2.CssPropertyInfo info =
      Css2.getCssProperty(ssig.getPropertyName().toLowerCase());
    if (null == info) {
      throw new AssertionError(
          "Unknown property in css property signature: " + propertyName);
    }
    check(info.sig);
    passed.addAll(applySignature(Collections.singletonList(candidate),
                                 info.name, info.sig));
  }

  private void applyCallSignature(
      CssPropertySignature.CallSignature call,
      Candidate candidate, String propertyName, List<Candidate> passed) {

    if (0 == (candidate.exprIdx & 1)) {  // a term
      CssTree.Term term =
        (CssTree.Term) expr.children().get(candidate.exprIdx);
      CssTree.CssExprAtom atom = term.getExprAtom();
      if (null == term.getOperator()
          && atom instanceof CssTree.FunctionCall) {
        CssTree.FunctionCall fn = (CssTree.FunctionCall) atom;
        if (fn.getName().equals(call.children().get(0).getValue())) {
          CssPropertySignature formals = call.children().get(1);
          CssTree.Expr actuals = fn.getArguments();
          if (false) {
            System.err.println("formals=\n" + dump(formals) +
                               "\nactuals=\n" + dump(actuals));
          }
          Candidate inFnSpace = new Candidate(
              0, candidate.match, candidate.warning);
          for (Candidate resultInFnSpace :
               new SignatureResolver(actuals).applySignature(
                   Collections.singletonList(inFnSpace), propertyName,
                   formals)) {
            passed.add(new Candidate(
                           candidate.exprIdx + 1, resultInFnSpace.match,
                           resultInFnSpace.warning));
          }
        }
      }
    }
  }

  /**
   * http://www.w3.org/TR/CSS21/syndata.html#q15
   * http://www.w3.org/TR/REC-CSS2/syndata.html#value-def-number
   * This syntax disallows a decimal point without any digits following, as
   * per the spec.
   */
  private static final String REAL_NUMBER_RE = "(?:\\d+(?:\\.\\d+)?|\\.\\d+)";
  /** According to http://www.w3.org/TR/CSS21/syndata.html#length-units */
  private static final Pattern LENGTH_RE = Pattern.compile(
      "^(?:" + REAL_NUMBER_RE + "(?:in|cm|mm|pt|pc|em|ex|px)|0+)$",
      Pattern.CASE_INSENSITIVE);
  /** http://www.w3.org/TR/REC-CSS2/syndata.html#value-def-number */
  private static final Pattern NUMBER_RE = Pattern.compile(
      "^" + REAL_NUMBER_RE + "$");
  /** http://www.w3.org/TR/REC-CSS2/syndata.html#value-def-integer */
  private static final Pattern INTEGER_RE = Pattern.compile("^\\d+$");
  /** http://www.w3.org/TR/CSS21/syndata.html#percentage-units */
  private static final Pattern PERCENTAGE_RE = Pattern.compile(
      "^" + REAL_NUMBER_RE + "%$");
  /** http://www.w3.org/TR/CSS21/fonts.html#value-def-family-name */
  private static final Pattern FAMILY_NAME_RE = Pattern.compile(
      "^\\s*(?:[\\w\\-]+(?:\\s+[\\w\\-]+)*)\\s*$", Pattern.CASE_INSENSITIVE);
  /** http://www.w3.org/TR/CSS21/aural.html#value-def-specific-voice */
  private static final Pattern SPECIFIC_VOICE_RE = Pattern.compile(
      "^\\s*(?:[\\w\\-]+(?:\\s+[\\w\\-]+)*)\\s*$", Pattern.CASE_INSENSITIVE);
  /** http://www.w3.org/TR/CSS21/aural.html#value-def-angle */
  private static final Pattern ANGLE_RE = Pattern.compile(
      "^(?:" + REAL_NUMBER_RE + "(?:deg|grad|rad)|0+)$",
      Pattern.CASE_INSENSITIVE);
  /** http://www.w3.org/TR/CSS21/aural.html#value-def-time */
  private static final Pattern TIME_RE = Pattern.compile(
      "^(?:" + REAL_NUMBER_RE + "(?:ms|s)|0+)$", Pattern.CASE_INSENSITIVE);
  /** http://www.w3.org/TR/CSS21/aural.html#value-def-frequency */
  private static final Pattern FREQUENCY_RE = Pattern.compile(
      "^(?:" + REAL_NUMBER_RE + "(?:hz|kHz)|0+)$",
      Pattern.CASE_INSENSITIVE);

  // Suffixes for substitutions.  A subsitution like $(x * 4)em can only be
  // a length.  Substitutions without a suffix can only be of certain kinds
  private static final Pattern LENGTH_SUFFIX_RE = Pattern.compile(
      "\\)(?:in|cm|mm|pt|pc|em|ex|px)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern PERCENTAGE_SUFFIX_RE = Pattern.compile("\\)%$");
  private static final Pattern NUMBER_SUFFIX_RE = Pattern.compile("\\)$");
  private static final Pattern COLOR_SUFFIX_RE = NUMBER_SUFFIX_RE;
  private static final Pattern ANGLE_SUFFIX_RE = Pattern.compile(
      "\\)(?:deg|grad|rad)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern TIME_SUFFIX_RE = Pattern.compile(
      "\\)(?:ms|s)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern FREQUENCY_SUFFIX_RE = Pattern.compile(
      "\\)(?:hz|kHz)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern URI_SUFFIX_RE = Pattern.compile(
      "\\)(?:uri)?$", Pattern.CASE_INSENSITIVE);

  /**
   * Handles symbols for which we don't have a signature.  Anything not handled
   * by {@link Css2#getSymbol}.
   */
  private boolean symbolMatch(
      Candidate candidate, String propertyName,
      CssPropertySignature.SymbolSignature sig) {
    if (0 != (candidate.exprIdx & 1)) { return false; }  // not a term
    CssTree.Term term = (CssTree.Term) expr.children().get(candidate.exprIdx);
    CssTree.CssExprAtom atom = term.getExprAtom();

    if (false) {
      System.err.println(
          "symbol " + sig.symbolName + " : matching " + propertyName + "\n"
          + dump(sig) + "\nagainst exprIdx=" + candidate.exprIdx + "\n"
          + dump(atom));
    }

    // If this is supposed to be a positive identifier, then disallow the
    // negation unary operator.
    // Positive is a bit of a misnomer since this really means non-negative.
    String symbolName = sig.symbolName;
    String constraints = null;
    // Check for any constraints
    {
      int colon = symbolName.indexOf(":");
      if (colon >= 0) {
        constraints = symbolName.substring(colon + 1);
        symbolName = symbolName.substring(0, colon);
      }
    }

    Object atomValue = atom.getValue();
    String atomSValue = atomValue instanceof String ? (String) atomValue : "";

    // Operators such as negation cannot be applied to substitutions.
    // The substitution itself should return a negative value.
    if (atom instanceof CssTree.Substitution && term.getOperator() != null) {
      return false;
    }

    // Try each symbol type we know how to handle
    if ("length".equals(symbolName)) {
      if (!(atom instanceof CssTree.QuantityLiteral &&
            LENGTH_RE.matcher(atomSValue).matches()) &&
          !(atom instanceof CssTree.Substitution &&
            LENGTH_SUFFIX_RE.matcher(atomSValue).find())) {
        return false;
      }
      candidate.match(term, CssPropertyPartType.LENGTH, propertyName);
      ++candidate.exprIdx;
    } else if ("number".equals(symbolName)) {
      if (!(atom instanceof CssTree.QuantityLiteral &&
            NUMBER_RE.matcher(atomSValue).matches()) &&
            !(atom instanceof CssTree.Substitution &&
                NUMBER_SUFFIX_RE.matcher(atomSValue).find())) {
        return false;
      }
      candidate.match(term, CssPropertyPartType.NUMBER, propertyName);
      ++candidate.exprIdx;
    } else if ("integer".equals(symbolName)) {
      if (!(atom instanceof CssTree.QuantityLiteral &&
            INTEGER_RE.matcher(atomSValue).matches()) &&
          !(atom instanceof CssTree.Substitution &&
            NUMBER_SUFFIX_RE.matcher(atomSValue).find())) {
        return false;
      }
      candidate.match(term, CssPropertyPartType.INTEGER, propertyName);
      ++candidate.exprIdx;
    } else if ("percentage".equals(symbolName)) {
      if (!(atom instanceof CssTree.QuantityLiteral
            && PERCENTAGE_RE.matcher(atomSValue).matches()) &&
          !(atom instanceof CssTree.Substitution &&
            PERCENTAGE_SUFFIX_RE.matcher(atomSValue).find())) {
        return false;
      }
      candidate.match(term, CssPropertyPartType.PERCENTAGE, propertyName);
      ++candidate.exprIdx;
    } else if ("family-name".equals(symbolName)) {
      if (null != term.getOperator()) { return false; }
      String name;
      if (atom instanceof CssTree.IdentLiteral) {
        name = ((CssTree.IdentLiteral) atom).getValue();
        if (Css2.isKeyword(name)) { return false; }
      } else if (atom instanceof CssTree.StringLiteral) {
        name = ((CssTree.StringLiteral) atom).getValue();
      } else {
        return false;
      }
      if (!FAMILY_NAME_RE.matcher(name).matches()) { return false; }
      candidate.match(term, CssPropertyPartType.FAMILY_NAME, propertyName);
      ++candidate.exprIdx;
    } else if ("hex-color".equals(symbolName)) {
      if (atom instanceof CssTree.HashLiteral) {
        // Require 3 or 6 hex digits
        String hex = ((CssTree.HashLiteral) atom).getValue();
        if (hex.length() != 4 && hex.length() != 7) { return false; }
        candidate.match(term, CssPropertyPartType.COLOR, propertyName);
        ++candidate.exprIdx;
      } else if (atom instanceof CssTree.Substitution) {
        if (!COLOR_SUFFIX_RE.matcher(atomSValue).find()) {
          return false;
        }
        candidate.match(term, CssPropertyPartType.COLOR, propertyName);
        ++candidate.exprIdx;
      } else {
        return false;
      }
    } else if ("angle".equals(symbolName)) {
      if (!(atom instanceof CssTree.QuantityLiteral &&
            ANGLE_RE.matcher(atomSValue).matches()) &&
          !(atom instanceof CssTree.Substitution &&
            ANGLE_SUFFIX_RE.matcher(atomSValue).find())) {
        return false;
      }
      candidate.match(term, CssPropertyPartType.ANGLE, propertyName);
      ++candidate.exprIdx;
    } else if ("time".equals(symbolName)) {
      if (!(atom instanceof CssTree.QuantityLiteral &&
            null == term.getOperator() &&
            TIME_RE.matcher(atomSValue).matches()) &&
          !(atom instanceof CssTree.Substitution &&
            TIME_SUFFIX_RE.matcher(atomSValue).find())) {
        return false;
      }
      candidate.match(term, CssPropertyPartType.TIME, propertyName);
      ++candidate.exprIdx;
    } else if ("frequency".equals(symbolName)) {
      if (!(atom instanceof CssTree.QuantityLiteral &&
            null == term.getOperator() &&
            FREQUENCY_RE.matcher(
                ((CssTree.QuantityLiteral) atom).getValue()).matches()) &&
          !(atom instanceof CssTree.Substitution &&
            FREQUENCY_SUFFIX_RE.matcher(atomSValue).find())) {
        return false;
      }
      candidate.match(term, CssPropertyPartType.FREQUENCY, propertyName);
      ++candidate.exprIdx;
    } else if ("specific-voice".equals(symbolName)) {
      if (null != term.getOperator()) { return false; }
      String name;
      if (atom instanceof CssTree.IdentLiteral) {
        name = ((CssTree.IdentLiteral) atom).getValue();
        if (Css2.isKeyword(name)) { return false; }
      } else if (atom instanceof CssTree.StringLiteral) {
        name = ((CssTree.StringLiteral) atom).getValue();
      } else {
        return false;
      }
      if (!SPECIFIC_VOICE_RE.matcher(name).matches()) { return false; }
      candidate.match(term, CssPropertyPartType.SPECIFIC_VOICE, propertyName);
      ++candidate.exprIdx;
    } else if ("uri".equals(symbolName)) {
      if (null != term.getOperator()) { return false; }
      if (!(atom instanceof CssTree.UriLiteral
            // This may not be per spec, but it is safest to interpret strings
            // as URIs, since many user-agents seem to do this, and we want to
            // apply constraints to URIs.
            || atom instanceof CssTree.StringLiteral
            // Uri substitutions can be fixed at runtime
            || (atom instanceof CssTree.Substitution &&
                URI_SUFFIX_RE.matcher(atomSValue).find()))) {
        return false;
      }
      candidate.match(term, CssPropertyPartType.URI, propertyName);
      ++candidate.exprIdx;
    } else if ("string".equals(symbolName)) {
      if (!(null == term.getOperator()
            && atom instanceof CssTree.StringLiteral)) {
        return false;
      }
      candidate.match(term, CssPropertyPartType.STRING, propertyName);
      ++candidate.exprIdx;
    } else if ("identifier".equals(symbolName)) {
      if (!(null == term.getOperator()
            && atom instanceof CssTree.IdentLiteral)) {
        return false;
      }
      candidate.match(term, CssPropertyPartType.IDENT, propertyName);
      ++candidate.exprIdx;
    } else {
      throw new AssertionError(
          "unhandled symbol " + sig.symbolName + "\n" + dump(atom));
    }

    if (null != constraints
        // TODO(msamuel): Could try to enforce these constraints at runtime or
        // just log them in debugging mode.  Violations of these constraints are
        // not security problems though
        && !(atom instanceof CssTree.Substitution)) {
      int comma = constraints.indexOf(",");
      double min = Double.parseDouble(constraints.substring(0, comma)),
             max = comma + 1 == constraints.length()
                 ? Double.POSITIVE_INFINITY
                 : Double.parseDouble(constraints.substring(comma + 1));
      String valueStr = ((CssTree.QuantityLiteral) atom).getValue();
      int numEnd = 0;
      for (char ch; numEnd < valueStr.length()
           && (((ch = valueStr.charAt(numEnd)) >= '0' && ch <= '9')
               || ch == '.');) {
        ++numEnd;
      }
      double value = Double.parseDouble(valueStr.substring(0, numEnd));
      if (CssTree.UnaryOperator.NEGATION == term.getOperator()) { value *= -1; }
      if (false) {
        System.err.println("min=" + min + ", max=" + max + ", value=" + value
                           + ", valueStr=" + valueStr + ", op="
                           + term.getOperator());
      }
      if (value < min || value > max) {
        candidate.warn(PluginMessageType.CSS_VALUE_OUT_OF_RANGE,
                       term.getFilePosition(),
                       MessagePart.Factory.valueOf(propertyName),
                       MessagePart.Factory.valueOf(value),
                       MessagePart.Factory.valueOf(min),
                       MessagePart.Factory.valueOf(max));
        // If this were a validation failure, it might cause us to improperly
        // match another rule later, so issue a warning instead
      }
    }

    return true;
  }

  private static final SyntheticAttributeKey<Integer> NUM =  // HACK: debug
      new SyntheticAttributeKey<Integer>(Integer.class, "serialno");

  /** debugging */
  private static int serialno = 0;
  /** debugging */
  private static void check(ParseTreeNode node) {
    if (true) { return; }
    if (!node.getAttributes().containsKey(NUM)) {
      node.acceptPreOrder(new Visitor() {
          public boolean visit(ParseTreeNode n) {
            n.getAttributes().set(NUM, Integer.valueOf(serialno++));
            return true;
          }
        });
    }
  }

  /** debugging */
  private static String dump(ParseTreeNode node) {
    check(node);
    StringBuilder sb = new StringBuilder();
    MessageContext mc = new MessageContext();
    mc.relevantKeys = Collections.<SyntheticAttributeKey>singleton(NUM);
    try {
      node.formatTree(mc, 2, sb);
    } catch (java.io.IOException ex) {
      ex.printStackTrace();
    }
    return sb.toString();
  }
}
