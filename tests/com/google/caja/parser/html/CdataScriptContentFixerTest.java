// Copyright (C) 2011 Google Inc.
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

package com.google.caja.parser.html;

import javax.annotation.Nullable;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.caja.lexer.TokenConsumer;
import com.google.caja.render.Concatenator;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;


public class CdataScriptContentFixerTest extends CajaTestCase {

  public final void testFixup() throws Exception {
    assertFixedUp("<script>foo()</script>", "<script>foo()</script>");
    assertFixedUp(
        "<script type=\"text/javascript\">foo()</script>",
        "<script type=text/javascript>foo()</script>");
    // Don't touch non-JS script tags.
    assertFixedUp(
        null, "<script type=text/opensocial-template>i-->0</script>");
    // Fix up tokens by spacing them out.
    assertFixedUp(
        "<script>foo -- > bar</script>", "<script>foo-->bar</script>");
    // Leave newlines alone.
    assertFixedUp(
        "<script>return\ni -- > j</script>",
        "<script>return\ni-->j</script>");
    // Remove comments.
    assertFixedUp(
        "<script>foo ( ) </script>", "<script>foo()//--></script>");
    assertFixedUp(
        "<script>foo ( ) </script>", "<script>foo()/*-->*/</script>");
    // Don't remove comments that might be conditional compilation directives,
    assertFixedUp(
        "<script>/* @cc_on */\nfoo ( ) </script>",
        "<script>/* @cc_on */\nfoo()//--></script>");
    // even if that leads to breakage.
    assertFixedUp(
        null, "<script>foo()// @--></script>");
    // Fix string literals that contain problematic content.
    assertFixedUp(
        "<script>[ '<\\/style' , '<\\!--' , '--\\>' , '--\\>' ]</script>",
        "<script>['</style', '<!--', '-->', '-->']</script>");
    assertFixedUp(
        "<script>'<\\/script>\\n' </script>",
        "<script>'<\\/script>\\n'//--></script>");

    assertFixedUp(
        ("<script type=\"text/javascript\">"
         + "var arr = [ '&amp;foo=' ] ;\n"
         + "try { if ( typeof $x == \"object\" ) {"
         + " arr . push ( $x . getBar ( ) . join ( \",\" ) )"
         + " } } catch ( e ) { }\n"
         + "</script>"),
        ("<script type=\"text/javascript\">\n"
         + "  var arr = ['&amp;foo='];\n"
         + "  try{if(typeof $x == \"object\"){"
         + "arr.push($x.getBar().join(\",\"))"
         + "}}catch(e){}\n"
         + "// ]]> --></script>"));
  }

  private final void assertFixedUp(
      @Nullable String goldenOrNullForFailure, String html) throws Exception {
    Node root = htmlFragment(fromString(html));
    try {
      TokenConsumer tc = new Concatenator(new StringBuilder());
      RenderContext rc = new CdataContentFixupRenderContext(tc);
      Nodes.render(root, rc);
    } catch (UncheckedUnrenderableException ex) {
      if (goldenOrNullForFailure != null) {
        throw ex;
      }
    }
  }

  static final class CdataContentFixupRenderContext
      extends RenderContext implements MarkupFixupRenderContext {
    CdataContentFixupRenderContext(TokenConsumer out) {
      super(out);
    }

    @Override
    public String fixUnclosableCdataElement(
        Element el, String cdataContent, int problemIndex) {
      return CdataScriptContentFixer.fixUnclosableCdataElement(
          el, cdataContent);
    }
  }
}
