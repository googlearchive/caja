/**
Optimizations that take into account knowledge about specific
user-agents to reduce download size

<h1 id="User-Agent_Specific_Cajoling">User-Agent Specific Cajoling</h1>

<h2 id="Background">Background</h2>

<p>Our supporting JS includes some code that is browser-specific ; we
back-port JSON onto older browsers, and do a lot of hacks that we know
will work on some browsers but not others.  Having multiple code
paths, only one of which will ever be live in a given environment is
wasteful in bandwidth and execution time.</p>

<p>E.g., in</p>
<pre class="prettyprint">if (typeof JSON === &#x27;undefined&#x27;) {
  JSON = (function () {
    // snip 600 lines of code
  })();
}</pre>

<p>on any given browser, a fresh page will always have JSON present or
absent.  Since the EcmaScript 5 draft mandates JSON, the number of
browsers to which this code needs to be shipped will only
decrease.</p>

<p>Similarly, in</p>
<pre class="prettyprint">if (window.addEventListener) {
  // snip code
} else if (window.attachEvent) {
  // snip
}</pre>

<p>on any given browser, the test will always run the same way.</p>
<p>Further, some browsers have bugs and features that make it hard to
optimize code for them.  As these bugs are fixed, the set of browsers
for which we cannot optimize code should dwindle for any given
bug. <tt>a[+i] = b[+i]</tt> can be significantly optimized on all
browsers except certain older Firefox versions.  And browsers that
implement EcmaScript 5 strict mode will require vastly fewer code
transformations allowing smaller and faster cajoled code to work on
those browsers.</p>

<h2 id="Goals">Goals</h2>
<p>To reduce the amount of dead code shipped with our supporting JS,
to allow optimizations on user-agents that don&#x27;t have scary bugs,
and to optimize out unnecessary code forking.</p>

<h2 id="Overview">Overview</h2>
<p>We want to be able to take into account, statically, knowledge
about the environment that our supporting JS and cajoled output is
running in.</p>
<p>First we define a file &quot;environment-checks.js&quot; which
tests the environment and produces a JSON object like:</p>
<pre class="prettyprint">({
  &quot;navigator.userAgent&quot;: &quot;FooBrowser/1.0&quot;,
  &quot;typeof window.JSON&quot;: &#x27;undefined&#x27;,
  &quot;!!window.addEventListener&quot;: true,
  &quot;!!window.attachEventr&quot;: true,
  &quot;!!(function () { &#x27;use strict&#x27;; return this; })()&quot;: false
})</pre>

<p>where each key is a snippet of javascript that has no side-effect,
and where each value is the result produced by <tt>eval</tt>ing that
code in a fresh execution context.</p>
<p>We can generate JSON files for each user-agent we care about and
store them as artifacts in the source-code repository.</p>

<h2 id="Container_Assumptions">Container Assumptions</h2>
<p>We assume two things about containers that load our cajoled JS:
<ol><li>That they never delete a member of the global object</li>
<li>That any additions to the global object will behave as specified.
So if a container provides its own implementation of window.JSON it is
not observably different from that on a user-agent with a native
implementation.</li>
</ol></p>

<h2 id="Code_Eliminating_Minifier">Code Eliminating
Minifier</h2>
<p>Next, we define a rewriter that reads the JSON file, and finds
expressions in conditions that match those and that contain no
non-global references. We then inline any assertions or their
negations, and constant fold over comparison and logic operators.</p>
<p>Finally the rewriter eliminates code paths that are inside
conditionals or loops where the condition is falsey.  We need to
preserve any side effect in the condition.</p>
<p>This rewriter will be exposed as an optional pass in our Minifier.
If the Minifier has a <tt>--user-agent</tt> flag that specifies a
user-agent environment JSON file, it will start with a rewriting pass
to eliminate dead code before rendering the output.</p>

<h2 id="Allowing_Cajoler_to_Optimize">Allowing Cajoler to
Optimize</h2>
<p>The Cajoler receives a <tt>PluginEnvironment</tt> object which
describes the environment the gadget runs in.  We add a getter to the
PluginEnvironment to get the JSON environment object.</p>
<p>If the Array optimization wants to make sure that it is safe to
allow access to negative numeric indices instead of positive only, it
can check</p>
<pre class="prettyprint">enum UserAgentProperty {{
  NEGATIVE_INDICES_ON_FNS(&quot;void 0 !== (function () {})[-2]&quot;),
  ...
}
...
void optimize() {
  Boolean negativeIndicesOnFns = myPluginEnvironment.getUserAgentJson().get(
      UserAgentProperty.NEGATIVE_INDICES_ON_FNS.code);  // null &amp;rarr; don&#x27;t know
  if (!Boolean.FALSE.equals(negativeIndicesOnFns)) { return; }  // abort
  // optimize
}</pre>

<p>A gadget container that embeds the Cajoler can pick the appropriate
user-agent JSON based on the user agent header in the request
headers. Each JSON file should include the user agent as a property,
so we will probably have to write a function to strip out
insignificant info (OS version, locale, etc.) from the user agent
string.  It is always safe to use an empty JSON object as an
environment object because an undefined key means &quot;don&#x27;t
know.&quot;</p>

<h2 id="Validating">Validating</h2>
<p>We can check that the assumptions inherent in a user-agent JSON
file hold in a container, and in the presence of the caja support
code by doing the following:</p>
<ol>
  <li>Load the container and/or caja support code</li>
  <li>Run &quot;environment-checks.js&quot; to generate a new JSON object</li>
  <li>Compare the JSON from (2) with that from the environment file.</li>
</ol>

<p>This validation can be done by containers to make sure they&#x27;re
not violating the assumptions above, and could be done in the
beginning of a module to make sure that it only runs in the proper
environment.</p>

<h2 id="Effect_on_Cachability">Effect on Cachability</h2>
<p>The User Agent definitions passed to the cajoler affect the cajoler
output, so any cajoling service that caches the output of a module
must incorporate a proxy for the user-agent into the cache key.  A
good key to use would be
<tt>userAgentJson[&#x27;navigator.userAgent&#x27;]</tt> along with the
user&#x27;s locale if the input contained message strings.</p>
*/
package com.google.caja.ancillary.opt;
