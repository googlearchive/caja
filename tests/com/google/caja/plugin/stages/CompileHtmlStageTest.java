// Copyright (C) 2009 Google Inc.
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
import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;

/**
 * @author ihab.awad@gmail.com
 */
public final class CompileHtmlStageTest extends PipelineStageTestCase {
  public final void testEmitHtmlAsJsStaticOnly() throws Exception {
    meta.setOnlyJsEmitted(true);
    assertPipeline(
        job("<p>Hello world</p>", ContentType.HTML),
        job("IMPORTS___.htmlEmitter___.emitStatic('<p>Hello world</p>')",
            ContentType.JS));
  }

  public final void testEmitHtmlAsJsAttributes() throws Exception {
    meta.setOnlyJsEmitted(true);
    assertPipeline(
        job("<p id=\"foo\">Hello world</p>", ContentType.HTML),
        job(""
            + "IMPORTS___.htmlEmitter___.emitStatic('<p id=\\\"id_1___\\\">"
                + "Hello world</p>')",
            ContentType.JS),
        job(""
            + "{ /* Start translated code */\n"
            + "  throw 'Translated code must never be executed';\n"
            + "  {\n"
            + "    var el___;\n"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;\n"
            + "    el___ = emitter___.byId('id_1___');\n"
            + "    emitter___.setAttr(el___, 'id', "
                + "'foo-' + IMPORTS___.getIdClass___());\n"
            + "    el___ = emitter___.finish();\n"
            + "    emitter___.signalLoaded();\n"
            + "  } /* End translated code */\n"
            + "}",
            ContentType.JS));
  }

  @Override
  protected boolean runPipeline(Jobs jobs) throws Exception {
    mq.getMessages().clear();
    return new CompileHtmlStage(
        CssSchema.getDefaultCss21Schema(mq),
        HtmlSchema.getDefault(mq))
        .apply(jobs);
  }
}