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

package com.google.caja.plugin.stages;

import com.google.caja.plugin.Jobs;
import com.google.caja.util.ContentType;
import com.google.caja.util.Join;

/**
 *
 * @author felix8a@gmail.com
 */
public final class RewriteFlashStageTest extends PipelineStageTestCase {
  public final void testNoembed() throws Exception {
    assertPipeline(
        html("a<noembed>b</noembed>c"),
        html("a<noembed class=\"cajaEmbed1\">b</noembed>c"),
        js(
            "{\n",
            "  IMPORTS___.htmlEmitter___.handleEmbed({",
            " 'id': 'cajaEmbed1' });\n",
            "}"));
    assertNoErrors();

    assertPipeline(
        html("a<noembed class=\"x\">b</noembed>c"),
        html("a<noembed class=\"cajaEmbed2 x\">b</noembed>c"),
        js(
            "{\n",
            "  IMPORTS___.htmlEmitter___.handleEmbed({",
            " 'id': 'cajaEmbed2' });\n",
            "}"));
    assertNoErrors();
  }

  public final void testEmbed() throws Exception {
    assertPipeline(
        html("a<embed type=\"foo\">b"),
        html("a<embed type=\"foo\" />b"));
    assertNoErrors();

    assertPipeline(
        html(
            "a<embed type=\"application/x-shockwave-flash\"",
            " src=\"xx\">b"
        ),
        html("a<div class=\"cajaEmbed1\"></div>b"),
        js(
            "{\n",
            "  IMPORTS___.htmlEmitter___.handleEmbed({\n",
            "      'id': 'cajaEmbed1',\n",
            "      'src': 'xx',\n",
            "      'height': null,\n",
            "      'width': null\n",
            "    });\n",
            "}"));
    assertNoErrors();

    assertPipeline(
        html(
            "a<embed type=\"application/x-shockwave-flash\"",
            " src=\"xx\" width=\"11\" height=\"22\">b"
        ),
        html("a<div class=\"cajaEmbed2\"></div>b"),
        js(
            "{\n",
            "  IMPORTS___.htmlEmitter___.handleEmbed({\n",
            "      'id': 'cajaEmbed2',\n",
            "      'src': 'xx',\n",
            "      'height': '22',\n",
            "      'width': '11'\n",
            "    });\n",
            "}"));
    assertNoErrors();
  }

  public final void testObject() throws Exception {
    assertPipeline(
        html("a<object>b</object>c"),
        html("a<object>b</object>c"));
    assertNoErrors();

    assertPipeline(
        html(
            "a<object type=\"application/x-shockwave-flash\"",
            " data=\"xx\">b</object>c"
        ),
        html("a<div class=\"cajaEmbed1\">b</div>c"),
        js(
            "{\n",
            "  IMPORTS___.htmlEmitter___.handleEmbed({\n",
            "      'id': 'cajaEmbed1',\n",
            "      'src': 'xx',\n",
            "      'height': null,\n",
            "      'width': null\n",
            "    });\n",
            "}"));
    assertNoErrors();

    assertPipeline(
        html(
            "a<object type=\"application/x-shockwave-flash\">",
            " b",
            " <param name=\"movie\" value=\"xx\">",
            " c",
            "</object>d"
        ),
        html("a<div class=\"cajaEmbed2\"> b  c</div>d"),
        js(
            "{\n",
            "  IMPORTS___.htmlEmitter___.handleEmbed({\n",
            "      'id': 'cajaEmbed2',\n",
            "      'src': 'xx',\n",
            "      'height': null,\n",
            "      'width': null\n",
            "    });\n",
            "}"));
    assertNoErrors();
  }

  public final void testYoutube() throws Exception {
    // Note, the object tag doesn't have classid or type, so browsers always
    // fallback to the embed tag.  This is a bug in current YouTube.
    assertPipeline(
        html(
            "<object width=\"$w1\" height=\"$h1\">",
            "<param name=\"movie\" value=\"$url1\"></param>",
            "<param name=\"allowFullScreen\" value=\"true\"></param>",
            "<param name=\"allowscriptaccess\" value=\"always\"></param>",
            "<embed src=\"$url2\" type=\"application/x-shockwave-flash\"",
            " width=\"$w2\" height=\"$h2\" allowscriptaccess=\"always\"",
            " allowfullscreen=\"true\"></embed>",
            "</object>"),
        html(
            "<object height=\"$h1\" width=\"$w1\">",
            "<param name=\"movie\" value=\"$url1\" />",
            "<param name=\"allowFullScreen\" value=\"true\" />",
            "<param name=\"allowscriptaccess\" value=\"always\" />",
            "<div class=\"cajaEmbed1\"></div>",
            "</object>"),
        js(
            "{\n",
            "  IMPORTS___.htmlEmitter___.handleEmbed({\n",
            "      'id': 'cajaEmbed1',\n",
            "      'src': '$url2',\n",
            "      'height': '$h2',\n",
            "      'width': '$w2'\n",
            "    });\n",
            "}"));
    assertNoErrors();
  }

  private JobStub html(String... content) {
    return new JobStub(Join.join("", content), ContentType.HTML);
  }

  private JobStub js(String... content) {
    return new JobStub(Join.join("", content), ContentType.JS);
  }


  @Override
  protected boolean runPipeline(Jobs jobs) throws Exception {
    mq.getMessages().clear();
    return new RewriteFlashStage().apply(jobs);
  }
}
