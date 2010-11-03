/**

<p>TokenConsumers that format code to an output buffer.</p>

<p>There are three types of renderers:
<dl>
  <dt>JS Renderer</dt>
  <dd>Render languages that use <code>{...}</code> to delimit statement blocks;
    <code>(...)</code> and <code>[...]</code> to delimit expression blocks;
    and <code>/&#42;...&#42;/</code> and <code>//...</code> style comments.</dd>
  <dt>CSS Renderer</dt>
  <dd>Like the JS Renderer but sensitive to the ways in which adding whitespace
  between tokens can change the meaning of CSS.</dd>
  <dt>Simple token concatenator</dt>
  <dd>Suitable for XML and HTML tokens.</dd>
</dl>

<p>Some of the renderers <b>pretty print</b> code by indenting it to
make the block structure clear.  Others try to produce the most
<b>compact</b> code.</p>

<p>There are a few meta-renderers, such as the <b>line-break-matching</b>
renderer which tries to wrap code to appear on the same lines as the mark
positions, and the <b>side-by-side</b> renderer which interleaves original
and translated source code.</p>
*/
package com.google.caja.render;