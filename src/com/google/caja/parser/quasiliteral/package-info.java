/**
Quasiliterals for matching and transforming Javascript parse trees.

<h2>Glossary</h2>
<dl>
<dt>Exophoric Functions</dt>
  <dd>A function where {@code this} can be bound to any instance.  Contrast
  to a method, where {@code this} can only be an instance of the method's
  class.
  <p>In EcmaScript 3, <code>Array.prototype.push</code> is exophoric so it can
  be <code>call</code>ed on any object by design &mdash;
  not just <code>Array</code>s.
  So <code>o = {}, [].push.call(o, 'a', 'b');</code> is equivalent to
  <code>o = { 0: 'a', 1: 'b', length: 2 };</code>.
  </dd>
<dt>Quasiliteral</dt>
  <dd>
    Quasiliterals can be used to match patterns in parse trees and
    produce parse trees as output.<pre>
match("@clazz.prototype.@methodName = function (@params*) { @body*; }",
      node, bindings, scope)
</pre>
    will check whether node looks like a javascript method assignment,
    and if successful, will put entries into {@code bindings} that map
    "quasi-holes" like {@code "clazz"} to the corresponding
    descendents of node.

    <p>Quasiliterals can also be used to generate parse trees as in<pre>
substV("cajaVM.def(@clazz, @baseClazz, @methods, @statics)",
       "clazz", ..., "baseClazz", ..., "methods", ..., "statics", ...);
</pre>
  </dd>
</dl>

<h2>QuasiLiterals</h2>
<table>
<tr>
<th>QuasiSyntax</th>
<th>Match</th>
<th>Substitute</th>
</tr>
<tr>
<td><tt>@foo</tt></td>
<td>Matches any node and binds it to the name <tt>foo</tt>.  If there is an
existing binding for <tt>foo</tt>, the match only passes if the candidate
binding and the original are deeply equal.</td>
<td>Emits any binding for <tt>foo</tt>, or fails to substitute if no such
binding exists.
<tr>
<td><tt>@foo___</tt></td>
<td>Matches a reference or identifier with the suffix <tt>___</tt>, binding
the name <tt>foo</tt> to a reference or identifier with the <tt>___</tt> suffix
removed.
<td>If <tt>foo</tt> is bound to a reference or identifier, emits a reference
or identifier with the <tt>___</tt> suffix added.</td>
</tr>
<tr>
<td><tt>@foo?</tt></td>
<td>Like <tt>@foo</tt> but if no match is available, succeeds without creating
a binding or consuming any input.
<td>Like <tt>@foo</tt> but if no binding is available does not fail.</td>
</tr>
<tr>
<td><tt>@foo*</tt></td>
<td>Like <tt>foo</tt> but will match as many nodes as possible on the input
grouping them into a <tt>ParseTreeNodeContainer</tt> which is bound to
<tt>foo</tt>.</td>
<td>Emits all of the children of the binding of <tt>foo</tt> if it exists,
succeeding either way.</td>
</tr>
<tr>
<td><tt>@foo+</tt></td>
<td>Like <tt>@foo*</tt> but there must be at least one matching input
for the match to succeed.</td>
<td>Like <tt>@foo*</tt> but there must be a binding and it must have a non-empty
child list for the substitution to succeed.</td>
</tr>
<tr>
<td><tt>'use foo,bar';</tt></td>
<td>If it appears where a <tt>UseSubsetDirective</tt> is allowed, matches any
<tt>UseSubsetDirective</tt> that contains a super-set of the subsets named.</td>
<td>Emits a <tt>UseSubsetDirective</tt> with only the subsets named.</td>
</tr>
<tr>
<td><tt>'@foo'</tt></td>
<td>Matches a string literal whose content is a valid JS identifier.  Binds
<tt>foo</tt> to an <tt>Identifier</tt> with that identifier.</td>
<td>If <tt>foo</tt> is bound to an <tt>Identifier</tt> or a <tt>Reference</tt>
emits a <tt>StringLiteral</tt> whose content is the identifier name.  Fails to
substitute otherwise.</td>
</tr>
</table>
*/
package com.google.caja.parser.quasiliteral;
