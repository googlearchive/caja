/**
A tamed version of the
<a href="http://www.w3.org/TR/DOM-Level-2-Core/core.html">DOM Level 2 APIs</a>.

<style>
.todo { font-weight: bold; color: red }
dt { font-weight: bold }
.diagram { margin-left: auto; margin-right: auto; display: table;
           vertical-align: middle; border: 2px dotted #aaa; padding: 1em;
           background-color: #eee }
.divider, .box { text-align: center; vertical-align: middle; height: 5em;
                display: table-cell }
.box { width: 6em; border: 1px solid black; background-color: #fff }
.divider { padding: .25em }
</style>
<script
 src="http://google-code-prettify.googlecode.com/svn/trunk/src/prettify.js"
></script>
<script
 src="http://google-code-prettify.googlecode.com/svn/trunk/src/lang-js.js"
></script>
<link rel=stylesheet
 href="http://google-code-prettify.googlecode.com/svn/trunk/src/prettify.css">

<h2>Goals</h2>
A mechanism for taming the DOM2 EcmaScript wrappers as implemented on
widely used browsers that is consistent with object capability discipline.

<h2>Glossary</h2>
<dl>
<dt>Descendant Node
  <dd>A node N is a descendant of M iff N is M or N is reachable from
  M by recursively traversing members of the <code>childNodes</code>
  list.  N cannot be a descendant of M if it is not M and it is an
  ancestor of N</dd>
<dt>Virtual Document
  <dd>A node which prevents root-wards navigation by cajoled code.
<dt>Tame
  <dd>To present an API similar to an existing one, but that follows
  Object Capability discipline.  Caveat: except where outlined in <a
  href="#legacy">legacy considerations</a>.</dd>
<dt>Uncontained Node
  <dd>A node N is not contained in M iff N is not a descendant of M.
  If N is not contained in M, then N may be a strict ancestor of M or
  it may be a descendant of a sibling of a strict ancestor of M, or
  in a disjoint tree.</dd>
<dt>Virtual Table
  <dd>A mapping from property names to means by which each property
  can be read/written/deleted/called.  The table also specifies how an
  object's properties can be enumerated.
</dl>

<h2>Examples</h2>
<p>
DOMita attenuates a DOM by intercepting reads, writes, and method calls
on DOM nodes, and by partitioning the DOM into multiple virtual documents.
<div class="diagram">
  <div class="box">Untamed DOM</div>
  <div class="divider">&harr;</div>
  <div class="box">DOMita / Virtual Docs</div>
  <div class="divider">&harr;</div>
  <div class="box">Cajoled JavaScript</div>
</div>
<p>
Below I show an untamed DOM, and several snippets of caja code and I walk
through the interactions between the cajoled code and the untamed DOM.
<h3>The untamed DOM after loading two modules</h3>
<pre class="prettyprint"
>&lt;head&gt;
  &lt;style&gt;
    /&#42; Styles created as a result of loading module 1. &#42;/
    .cajaModule-0123___ p { color: purple }
    /&#42; Styles created as a result of loading module 2. &#42;/
    .cajaModule-4567___ p { color: pink }

    /&#42;
     &#42; These styles do not interfere with one-another since different
     &#42; virtual documents' bodies have different unmentionable class markers.
     &#42;/
  &lt;/style&gt;
&lt;/head&gt;
&lt;body&gt;
  &lt;h1 id="foo"&gt;Container Title&lt;/h1&gt;
  &lt;div class="vdoc-doc___" id="module-a-root"&gt;
    &lt;div class="vdoc-html___"&gt;
      &lt;div class="vdoc-body___ cajaModule-0123___"&gt;
        &lt;p&gt;&lt;a id=":foo"&gt;Module A Link&lt;/a&gt;&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
  &lt;/div&gt;
  &lt;div class="vdoc-doc___" id="module-b-root"&gt;
    &lt;div class="vdoc-html___"&gt;
      &lt;div class="vdoc-body___ cajaModule-4567___"&gt;
        &lt;p&gt;&lt;a id=":foo"&gt;Module B Link&lt;/a&gt;&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
  &lt;/div&gt;
&lt;/body&gt;
</pre>

<h3>Code example 1 &mdash; modifying an element retrieved by ID</h3>
<pre class="prettyprint">
var myLink = document.getElementById('foo');
myLink.href = 'foo.png';
myLink.innerHTML = 'My Image';
</pre>

<ol>
  <li><code>document</code> is resolved against the gadget's outers object.
    In the case of module A, this returns a direct reference to
    <code>&lt;div id="module-a-root" ...&gt;</code>.
    This binding was initialized by <code>attachDocument</code> when
    the container was setting up the various gadgets' virtual documents.
  <li><code>cajita.callPub</code> is invoked with the virtual document root
    and the string <code>'getElementById'</code> as arguments.
    <ol>
      <li>There is no fasttrack bit or call grant present so no early exit there
      <li>The node is not a JSON container so that path does not exit
      <li>Possible optimization: On Firefox and other browsers that have
        <code>Node.prototype</code> on the member lookup chain, and on native
        caja implementations, a <code>vtable___</code> member points to a
        vtable which causes the next step to be skipped.
      <li>This ends up being handled by the generic handlers which performs the
        <code>isDomNode()</code> check as defined in
        <a href="#vtable-js">vtable</a> and returns the
        <code>HtmlDocument</code> vtable based on the presence of
        <code>vdoc-doc___</code> in the node's class.
    </ol>
  <li>The virtual document vtable's <code>getElementById</code> method
    prepends the argument <code>'foo'</code> with a colon to move it out of
    the <a href="#renaming">namespace</a> of IDs and NAMEs used by container
    code.  The vtable <code>getElementById</code> knows to do this since the
    <a href="#tamed-dom-js">taming rules</a> specify the type of that
    parameter as GLOBAL_NAME.  The function that does the prefixing is specified
    in <code>sanitizers.js</code></li>
  <li>After prefixing the ID, the tamed <code>getElementById</code> calls the
    untamed version of <code>document.getElementById(':foo')</code>.
    Because of the global nature of GLOBAL names, this may return
    either of the two elements with <code>id=":foo"</code> above.
    <code>HTMLDocument.getElementById</code> is specified in
    <code>tamed-dom.js</code> as having type <code>HTMLElement</code>
    so the code in <code>sanitizers.js</code> which deals with that
    type knows to check that the return value is in the same virtual
    document as the method was invoked on.  If it is not, a fallback
    method is tried which will return the correct virtual document's
    <code>&lt;a id=":foo"&gt;</code>
  <li>Control proceeds to the second line in the caja code which assigns
    <code>foo.png</code> to <code>myLink.href</code>.
  <li>A similar vtable lookup is done for the link element.  This returns
    the <code>HTMLLinkElement</code> vtable which defines a setter for the
    <code>href</code> element.
    The <code>href</code> property has type URI and so the sanitizer invokes
    the container's URI policy to validate and possibly rewrite the URL before
    doing the set.  The URI policy can proxy the URL through a server that
    does additional security checks.
  <li>The assignment succeeds, and returns <code>'foo.png'</code> instead of
    the actual sanitized version, so that assignments chain properly.
  <li>Similarly, the assignment to <code>myLink.innerHTML</code> involves
    looking up a setter on a vtable and invoking the runtime HTML sanitizer.
    If the assigned value included a <code>&lt;script&gt;</code> tag
    instead of the innocuous "Hello World", it would be stripped out at this
    stage instead of being actually assigned.  The HTML sanitizer applies the
    same ID renaming policy described above.
</ol>

<h3>Code example 2 &mdash; building an orphaned DOM subtree and adding it
to a document</h3>
<pre class="prettyprint">
var myDiv = document.createElement('DIV');
myDiv.appendChild(document.createTextNode('Hello World');
document.body.appendChild(myDiv);
</pre>
<ol>
  <li>Lookup of <code>document</code> proceeds as described in
    code example 1 above.
  <li>Then <code>document.createElement</code> is resolved in a similar manner
    to <code>document.getElementById</code>.
  <li>The type for the argument to <code>createElement</code> is TAG_NAME
    which is checked against the HTML element
    <a href="http://code.google.com/p/google-caja/wiki/CajaWhitelists">whitelist<a/>.
     Element names like <code>OBJECT</code> are rejected
     as are unrecognized names.
  <li>The return type for <code>createElement</code> is ORPHAN_NODE which
    side-steps the usual in-same-document check.
    CAVEAT: under the virtual document system it is not always true that
    <code class=prettyprint>document.createElement('DIV').ownerDocument
      === document</code>
  <li>The text node is created in a similar manner.
  <li><code>document.body</code> resolves to a getter that looks for the
    descendant of the virtual document that has <a href="#vdocs">vdoc class</a>
    <code>vdoc-body___</code>.
  <li>The <code>appendChild</code> method is resolved against the
    <code>HTMLBodyElement</code> virtual table which just checks that the
    argument is a valid DOM node and has no vdoc class.  It is assumed that
    anyone with access to an element can move it to another place in the DOM so
    it does not bother checking that the argument is orphaned.
</ol>

<h3>Code example 3 &mdash; traversing the DOM</h3>
<pre class="prettyprint">
var ancestors = [];
for (var p1 = document.getElementsByTagName('p')[0]; p1; p1 = p1.parentNode) {
  ancestors.push(p1);
}
</pre>
<p>The ancestors list will contain the paragraph element, the virtual
document body, the virtual HTML node, and nothing else.  Specifically
it will not include the real BODY or HTML elements.

<h3>Code example 4 &mdash; using a DOM node from another gadget's document</h3>
<pre class="prettyprint">
var myParagraph = channelToOtherGadget.getNode();
myParagraph.style.color = 'blue';
</pre>
<ol>
  <li>In the first step, one gadget gets a DOM node from another gadget through
    a channel that was provided by some container mechanism.
    By allowing access to that DOM node, the other gadget is granting access to
    any DOM node in the same virtual document.
  <li>Style returns a regular caja object that uses the CSS schema to validate
    set values.
</ol>

<h3>Code example 5 &mdash; secure decomposition</h3>
<pre class="prettyprint">
// Create a virtual document
var myDoc = document.createVirtualDocument(myElement);
// A virtual document can be added to another tree if it is not rooted
// somewhere.
// Since a virtual document's parentNode is null, granting access to it
// does not grant authority to remove it from the parentNode.
myElement.appendChild(myDoc);
// Pass it to a piece of code of which we are suspicious to prevent the other
// code from unduly interfering with this module's UI.
exportToOtherModule(myDoc.body);
</pre>


<h2>Code organization</h2>
<h3 id="tamed-dom-js"><code>tamed-dom.js</code></h3>
<p>The DOM Level 2 Spec is defined in terms of IDL interfaces like
<blockquote><pre class=prettyprint>
interface Document : Node {
  readonly attribute DocumentType       doctype;
  readonly attribute DOMImplementation  implementation;
  readonly attribute Element            documentElement;
  Element            createElement(in DOMString tagName)
                                        raises(DOMException);
  &hellip;
</pre></blockquote>
and semantic relationships between those interfaces.
The <code>tamed-dom.js</code> file provides a mechanism for
defining a <dfn>tame</dfn> JavaScript implementation of an interface
backed by an untamed instance of the interface.

<p>Each IDL interface defines a number of properties common to
instances of that interfaces.  Some properties are readable, some are
writable, and some are callable.  Each property also has a signature
&ndash; a nominal type for an attribute; and a function signature
consisting of a single nominal return type, zero or more nominal
parameter types, and zero or more nominal exception types.

<p>Each interface also fits into an inheritance graph (forest
actually).  The inheritance hierarchies in this forest are
<code>Node</code>, <code>NodeList</code>, <code>Event</code>,
<code>DOMException</code>.  The <code>DOMImplementation</code> is not
tamed by this module.

<p>To provide tame interface constructors, we need to specify
several things:
<ul>
<li>its position in the inheritance graph.  Each interface derives
    from at most one nominal type.
<li>the names, types, and attributes of its properties
<li>additional semantic relationships that are not maintained by the
    primitive operations: <code>appendChild</code>, et al.  E.g.
    that the <code>autofill</code> property be set for all inputs
    modified via the tamed API.
</ul>

<h3><code>sanitizers.js</code></h3>
<p>Defining an interface via <code>tamedDom.tameInterface</code> adds
another type to the set of types usable in the taming declarations.</p>

<p><code>sanitizers.js</code> provides a limited number of nominal
types.  When an interface element defined via
<code>tamedDom.tameInterface</code> is read or set, the nominal type
is used to decide how to vet/sanitize the input, or scrub/tame the
output.

<h3 id="vtable-js"><code>vtable.js</code></h3>
<p>Defines a mapping from javascript objects to vtables
or <code>null</code>, and generic Cajita property handlers that lookup
vtables for properties that cannot otherwise be found.  A vtable is a
mapping from property names to read/write/call/delete/enum handlers.

<h3><code>vdoc.js</code></h3>
<p>The <code>vdoc.js</code> file defines a mechanism by which a
container can separate a DOM into multiple virtual documents, so that
a tame node's <code>parentNode</code>, <code>documentElement</code>,
and similar properties do not allow navigation past a specially marked
<code>DIV</code> element.
See <a href="#node-getters">Legacy Considerations</a> for details.

<p>This file defines several operations grouped together under the
<code>vdoc</code> namespace.
<ul>
  <li><code>vdoc.getVdocClass(node)</code> &mdash; given an element
    with a class <code>vdoc-&lt;XYZ&gt;___</code> returns
    <code>&lt;XYZ&gt;</code>.  These are used to mark special nodes in
    <a href="#vdocs">virtual documents</a>.  Returns <code>null</code>
    if no such class exists.  This method also treats real documents
    as virtual documents, so it will return <code>'body'</code> if
    node is a <code>BODY</code> element without
    a <code>vdoc-&lt;XYZ&gt;___</code> class, and will
    return <code>'doc'</code> for a document node (though not for a
    document fragment).
  <li><code>vdoc.createVirtualDocument()</code> &mdash; returns a
    virtual document root.
  <li><code>vdoc.getVirtualDocumentBody(node)</code> &mdash; given a
    node, returns the "body" of the closest containing virtual
    document, where "body" is defined as a node <code>n</code> for
    which <code>vdoc.getVdocClass(n) === 'body'</code>.
    Returns <code>null</code> if no such node exists.  Even
    though <code>vdoc.getVdocClass</code> equivocates between real and
    virtual documents, the value returned is always an element or null.
</ul>

<h2 id="legacy">Legacy Considerations</h2>
<h3 id="node-getters">Parent navigation</h3>
<p>Object capability discipline requires that a client of the API be
able to reason about the amount of authority they are granting away by
giving a reference to an object.  For it to be effective there has to
be a way to grant partial authority.  In DOMita, passing a reference
to a node, grants the authority to traverse, and perhaps modify, that
DOM subtree.

<p>The
<code><a href="http://www.w3.org/TR/DOM-Level-2-Core/core.html#ID-1060184317"
>parentNode</a></code> and
<code><a href="http://www.w3.org/TR/DOM-Level-2-Core/core.html#node-ownerDoc"
>ownerDocument</a></code>
members of the <code>Node</code> interface and some sub-interface
members (e.g.
<a href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-63239895"
><code>HTMLInputElement::form</code></a>) can allow upward navigation
which would allow code that received a node to expand the sub-tree
granted perhaps to include the entire document.
But a lot of existing code uses <code>parentNode</code>.

<p>The <code>ownerDocument</code> getter returns the closest containing
virtual document.</p>

<p>There is a legitimate use of <code>parentNode</code> that cannot be
restricted by the virtual document scheme.
The DOM tree mutators (e.g.
<a href="http://www.w3.org/TR/DOM-Level-2-Core/core.html#ID-184E7107"><code
>Node::appendChild</code></a>) use the following language
<blockquote>
  <code>appendChild</code>
  <blockquote>
    Adds the node newChild to the end of the list of children of this
    node. If the newChild is already in the tree, it is first removed.
  </blockquote>
</blockquote>

<p>If a failure to reach a parent returned <code>null</code> when a
cursor traversal exceeds the virtual document's scope, then existing
idioms for checking whether <code>appendChild</code> will modify a
distant DOM node
<blockquote>
  assert(myNode.parentNode == null);
  myOtherNode.appendChild(myNode);
</blockquote>
would falsely suggest that the operation would have less far-reaching
side-effects than it does.

<p>But since we have virtual documents at well defined locations, we can
maintain the fiction that there is no parent without breaking common
idioms.

<h3>GUI Focus</h3>
<p>Having the authority to add a key listener to a node is necessary
but not sufficient to receive key events.  That node must also have
focus.  But the
<a href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-65996295"
><code>focus()</code></a> method of <code>HTMLInputElement</code>
and friends grabs focus.  If we were to allow any piece of code to grab
keyboard focus, then it could steal input intended for another widget
of code in the same page.
<p>But proper focus handling is critical to usability in web
applications, so we need to provide a functional <code>focus()</code>
mechanism.
<p>The user implicitly conveys authority to receive input when they
interact with a piece of code, e.g. by clicking on it, or using the
TAB key to focus on part of it.</p>
<p>So the tamed API presents the same <code>focus()</code> method,
but attenuates it by making it fail unless the code that calls it
was called inside an event handler.  This means that the authority
to add a UI event handler (e.g.
<code>addEventListener('onclick', myFn)</code>) implies the authority
to parlay received events into the authority to grant focus to any
node reachable by the event handler.
<p>This approach is similar to the way browsers restrict
<code>window.open</code>.  To prevent runaway popups,
<code>window.open</code> fails unless the user interacted with a page
element recently.  This approach works well for legacy code, and
gives the end user some level of confidence that they are not giving
information to some entity that they never interacted with
intentionally.


<h3>Node Type Constructors</h3>
<p>Section 1.1.2 of DOM2 Core says
<blockquote>
    Most of the APIs defined by this specification are interfaces
    rather than classes. That means that an implementation need only
    expose methods with the defined names and specified operation, not
    implement classes that correspond directly to the interfaces. This
    allows the DOM APIs to be implemented as a thin veneer on top of
    legacy applications with their own data structures, or on top of
    newer applications with different class hierarchies.
</blockquote>

but most browsers expose the interface types as JavaScript
<code>function</code>s so that they can be used in
<code>instanceof</code> (e.g. <code>myNode instanceof
HTMLDivElement</code>) checks.  This is a useful enough feature that
some common JS libraries try to provide an equivalent API for
compatibility with IE6 which does not have this feature.

<p>Since DOMita does not wrap nodes, we don't need to worry about a
node's constructor being callable avoiding checks
in <code>document.createElement</code>.


<h2 id="vdocs">Multiple Documents</h2>
A virtual document is represented in the untamed DOM by markup like the below:
<pre class=prettyprint>
&lt;div class="vdoc-doc___"&gt;&lt;!-- Corresponds to the document --&gt;
  &lt;div class="vdoc-html___"&gt;&lt;!-- Corresponds to the HTML element --&gt;
    &lt;div class="vdoc-body___"&gt;&lt;!-- Corresponds to the BODY element --&gt;
      Virtual document body
    &lt;/div&gt;
  &lt;/div&gt;
&lt;/div&gt;
</pre>
The vdoc's virtual HTML and BODY elements cannot be removed from their
parent, appear to have no attributes, and cannot have attributes
added.

<p>The tamed <code>parentNode</code> attribute of a virtual document
(a node <code>n</code> for which <code>vdoc.getVdocClass(n)</code>
returns <code>'doc'</code>) is <code>null</code> as specified in DOM2.
This means that virtual documents serve as limits on root-wards
navigation.

<p>DOM2 specifies that for all nodes <code>n</code>,
and <code>i</code> in <code>[0,
n.childNodes.length)</code>, <code>n.childNodes[i] instanceof Node
&amp;&amp; n.childNodes[i].parentNode === n</code>.  CAVEAT: If a
virtual document is nested inside another, it is possible that this
will not hold.  This, and the fact that a non-root node may
have <code>nodeType === 9 /&#42; DOCUMENT &#42;/</code> may confuse legacy code
that assumes otherwise.  Containers may decline to provide a mechanism
by which cajoled code can create virtual documents, which should
ensure that legacy code assumptions are not violated.  Containers that
elect to allow this should document it as a quirk.

<p>Since virtual documents limit root-wards navigation, a reference to
a node encapsulates authority to any node in the same virtual document
or any contained virtual document, but not to any in a containing or
disjoint document.

<h2>Attenuated DOM Nodes</h2>
<p>We cannot wrap DOM nodes without breaking EQ in a way that would
either introduce memory leaks, or require Cajita to virtualize the
<code>===</code>, <code>!==</code>, <code>==</code>, <code>!=</code>,
<code>instanceof</code> which would seriously complicate supporting
and container scripts.

<p>So we use Cajita property handlers to attenuate the host objects
that implement browsers' DOM bindings.

<p>When cajoled code attempts a property read/write/call/enumeration,
it does the following:
<ol>
  <li>Fastpath check
  <li>Grant check
  <li>Handler check
</ol>

<p>The fastpath check will pass for array indices and the
<code>length</code> member (but we should check on IE6 or IE7
since <code>Node</code>s do not have <code>Object.prototype</code>
on its prototype chain).

<p>On IE, setting properties on DOM nodes fails with an exception, so
grants cannot be relied upon.  We cannot use direct handlers on DOM
nodes either, so below we outline a vtable like scheme using generic
handlers.  If grants exist on DOM nodes they would circumvent the
vtable scheme.

<p><code>vtable.js</code> defines a vtable scheme, and
<code>tamed-dom.js</code> defines the vtables.  (<code>vtable.js</code>
depends on changes to <code>cajita.js</code> fault handling hooks that
have not been made as of this writing.)
When a non-fasttracked property access on a DOM node occurs, the following
happens:
<ol>

  <li>Generic handler looks up vtable, uses that to find a property
    handler, and delegates to that.
    <pre class="prettyprint lang-js">
var vtable = vtable.lookupVTable(node);
if (vtable) {
  var property = vtable[propertyName];
  if (property &amp;&amp; vtable.hasOwnProperty(propertyName)) {
    // Using this form of invocation allows us to use the same read-fault
    // handler functions we'd use to handle faults on the node itself.
    return property.handleRead.call(node, propertyName);
  }
}
&hellip;
</pre>
  <li><code>vtable.lookupVTable</code> finds a vtable for a javascript object.
    <pre class="prettyprint lang-js">
if (isDomNode(obj)) {
  if (obj.nodeType === 1 /&#42; ELEMENT &#42;/) {
    // check vdoc.getClassName() and see whether to use the virtual doc,
    // BODY element, and HTML element vtables.
    // check tagName against the schema
  } else {
    // handle non-element types.
    // check attributes against the schema.
  }
} else if (isNodeList(obj)) {
  // handle id/name aliases properly
}
return null;
</pre>

  <li>Checking whether an object is a DOM node is not simple across
    browsers, but checking whether or not the <code>nodeType</code> property is
    readonly is a good test:<pre class="prettyprint lang-js">
function isDomNode(candidate) {
  switch (typeof candidate) {
    case 'object':
      if (candidate === null) { return false; }
      break;
    case 'function': break;
    default: return false;
  }
  var nodeType = candidate.nodeType;
  if (nodeType !== +nodeType) { return false; }
  // If an attacker can invoke Object.watch or define{Getter,Setter}
  // or create their own host objects, they can spoof this step.
  // In ES5, properties and objects can be frozen.
  // For that, we'd have to detect the language version
  // and use type checks.
  try {
    candidate.nodeType = null;  // should be read-only for Nodes
  } catch (ex) {
    // An ES5 or SpiderMonkey setter can throw an exception
    // after mutating the object, but we have no way to recover
    // from that side-effect.
    return true;
  }
  if (candidate.nodeType === nodeType) { return true; }

  candidate.nodeType = nodeType;
  return false;
}
</pre>
</ol>

<h2><a href="http://www.w3.org/TR/DOM-Level-2-Core/core.html#ID-1780488922"
><code>NamedNodeMap</code></a>s and
<a href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-75708506"
><code>HTMLCollection</code></a>s, and
<code>HTMLOptionsCollection</code>s</h2>

<p><tt>NamedNodeMap</tt>s, <tt>HTMLCollection</tt>s, and
<tt>HTMLOptionsCollection</tt>s all act like arrays, but alias property names
based on the <code>id</code> or <code>name</code> attribute.
<blockquote>
  An individual node may be accessed by either ordinal index or the node's name
  or id attributes.
</blockquote>

<p>Node lists and HTML collections appear in a number of places:
<ul>
<li><code>Node.childNodes</code>
<li><code>Node.attributes</code>
<li><code>document.anchors</code>
<li><code>document.applets</code>
<li><code>document.forms</code>
<li><code>document.images</code>
<li><code>document.links</code>
<li><code>&lt;FORM&gt;.elements</code>
<li><code>&lt;MAP&gt;.areas</code>
<li><code>&lt;SELECT&gt;.options</code>
<li><code>{&lt;TABLE&gt;,&lt;TBODY&gt;,&lt;TFOOT&gt;,&lt;THEAD&gt;}.rows</code>
<li><code>&lt;TABLE&gt;.tBodies</code>
<li><code>&lt;TR&gt;.cells</code>
<li>results of <code>document.getElementsByClassName</code>
<li>results of <code>document.getElementsByName</code>
<li>results of <code>document.getElementsByTagName</code>
<li>results of xpath expressions
</ul>
We don't need to filter results from any of these since, in no case does a node
list contain a node that is not a descendant of a node that was involved in the
operation that produced the node list.

<p>Some node types act as if they were HTML containers:
<blockquote>
Interface <a href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-40002357"
>HTMLFormElement</a>
  <blockquote>
    The <code>FORM</code> element encompasses behavior similar to a collection
    and an element.
  </blockquote>
</blockquote>
<blockquote>
Interface <a href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-94282980"
>HTMLSelectElement</a>
  <blockquote>
    The select element allows the selection of an option. The contained options
    can be directly accessed through the select element as a collection.
  </blockquote>
</blockquote>
CAVEAT: We will not support this behavior since there is too much risk of
namespace collision.  Developers can use <code>&lt;FORM&gt;.elements</code> and
<code>&lt;SELECT&gt;.options</code> instead, as recommended in
<a href="http://www.javascripttoolbox.com/bestpractices/">JS best practices</a>.


<h2 id="masking">HTML Container member masking</h2>
See the discussion at
<a href="http://code.google.com/p/google-caja/issues/detail?id=935"
>Issue 935</a>.

<h2 id="renaming"><code>CLASS</code>, <code>ID</code>, and <code>NAME</code>
renaming</h2>
<p>Some HTML attributes' values form a namespace that might mask elements in a
namespace visible to the container or another gadget.

<p>If the container or another gadget relies on
<code>document.getElementById</code> to return a node that it created with a
particular ID.

<p>Also, on IE6, IE7, and possibly IE8, the IDs and NAMEs intrude on the global
scope effectively treating the <code>window</code> object as a combination
<code>HTMLElement</code> and <code>HTMLCollection</code> in the same way as
<code>FORM</code> and <code>SELECT</code> elements.
<blockquote>
<pre class="prettyprint">
&lt;div id="div"&gt;&lt;/div&gt;
&lt;form id="formid"&gt;
  &lt;!--
    - IDs and NAMEs of form elements inside a form or options inside a select
    - do not intrude on
    - the global scope
    --&gt;
  &lt;input id="inpid_in_formid"/&gt;
  &lt;input name="inpname_in_formid"/&gt;
&lt;/form&gt;
&lt;form <b>name</b>="formname"&gt;
  &lt;input id="inpid_in_formname"/&gt;
  &lt;input name="inpname_in_formname"/&gt;
&lt;/form&gt;
&lt;!--
  - But IDs and NAMEs of form elements outside a form or options inside a select
  - do intrude on the global scope
  --&gt;
&lt;input id="inpid"/&gt;
&lt;input name="inpname"/&gt;
&lt;script&gt;(function () {
  // All the below are true on IE6, IE7, and possibly other versions of IE
  var names = ['div', 'formid', 'inpid_in_formid', 'inpname_in_formid',
               'formname', 'inpid_in_formname', 'inpname_in_formname', 'inpid',
               'inpname'];
  var masked = [];
  var notMasked = [];
  for (var i = 0; i &lt; names.length; ++i) {
    (eval(window[names[i]]) ? masked : notMasked).push(names[i]);
  }
  alert('masked=' + masked + ', notMasked=' + notMasked);
  <b>// On IE6,
  // masked=div,formid,formname,inpid,inpname
  // notMasked=inpid_in_formid,inpname_in_formid,inpid_in_formname,
  //     inpname_in_formname</b>
})();&lt;/script&gt;
</pre>
</blockquote>

<p>Our HTML schema defines the types (ID, CLASS, LOCAL_NAME, GLOBAL_NAME) and
space separated lists of those types.  The LOCAL_NAME, GLOBAL_NAME distinction
is only important if we want to try and enforce a different rewriting when
an element with a local name is inside an element that scopes it properly
(e.g. an INPUT inside a FORM), but maintaining that would be hugely complex
since there are so many ways to remove an element from the DOM.
We rewrite these attributes using the following scheme:
<table border=1 cellpadding=3 cellspacing=0>
<tr><th>attribute type
<th>value as apparent to cajoled code
<th>sanitized value</tr>
<tr><td>ID<td><code>foo</code><td><code>:foo</tr>
<tr><td>NAME<td><code>foo</code><td><code>:foo</tr>
<tr><td>CLASS<td><code>foo</code><td><code>foo</tr>
</table>

<p>Cajoled code cannot mention IDs, CLASSes, or NAMEs that end in
double-underscore.

<p>Tamed styles do not depend on CLASS rewriting.  Instead, there is an
unmentionable class related to the module instance ID that can be attached
to a virtual document root to enable styling of any nodes under that virtual
document's root.

<p><code>document.getElementById</code> does need to be restricted to the
virtual document though.  When cajoled code calls
<code>document.getElementById</code>, the untamed version is applied to the
prefixed ID, and if it falls in that document then the result is returned.
Otherwise another strategy is applied to find a node with the given id that
is properly contained.

<p>Since we are rewriting INPUT names, we need to virtualize FORM submission.
We could try and create a mirror FORM that does not have NAMEs and IDs prefixed,
but since most containers are already rewriting FORM ACTIONs to point to a
proxy, the proxy can rewrite names as appropriate.  If a FORM contains a mixture
of container created INPUTs (that don't have prefixed names) and INPUTs created
by cajoled code (with prefixed names), the proxy will be able to distinguish
based on ID/NAME.


<h2>Likely Failure Modes</h2>
TODO

<h2>See Also</h2>
TODO

<script>
// Red so I don't forget to TODO the things that need to be TODOne.
(function () {
  function walk(node) {
    if (node.nodeType === 3) {
      var parts = node.nodeValue.split(/\b(TODO|CAVEAT)\b/g);
      if (parts.length > 1) {
        setTimeout(
            (function (parts) {
              return function () {
                for (var i = 0, n = parts.length; i !== n; ++i) {
                  if (!parts[i]) { continue; }
                  var part = parts[i];
                  var partNode;
                  var textNode = node.ownerDocument.createTextNode(part);
                  if (part === 'TODO' || part === 'CAVEAT') {
                    var partNode = node.ownerDocument.createElement('SPAN');
                    partNode.appendChild(textNode);
                    partNode.className = 'todo';
                  } else {
                    partNode = textNode;
                  }
                  node.parentNode.insertBefore(partNode, node);
                }
                node.parentNode.removeChild(node);
              };
            })(parts), 0);
      }
    } else if (node.nodeType === 1) {
      for (var child = node.firstChild; child; child = child.nextSibling) {
        walk(child);
      }
    }
  }
  walk(document.body);
})();
prettyPrint()</script>
*/
package com.google.caja.domita;