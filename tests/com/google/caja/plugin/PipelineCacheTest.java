// Copyright (C) 2010 Google Inc.
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

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.InputSource;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.quasiliteral.ModuleManager;
import com.google.caja.parser.quasiliteral.RewriterMessageType;
import com.google.caja.plugin.stages.JobCache;
import com.google.caja.plugin.stages.JobCache.Key;
import com.google.caja.plugin.stages.JobCache.Keys;
import com.google.caja.plugin.stages.PipelineStageTestCase;
import com.google.caja.plugin.stages.PipelineStoreStage;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.ContentType;
import com.google.caja.util.Join;
import com.google.caja.util.Lists;
import com.google.caja.util.Pipeline;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class PipelineCacheTest extends PipelineStageTestCase {
  private TestJobCache cache;

  static final class TestKey implements JobCache.Key {
    final ContentType type;
    final String content;

    TestKey(ContentType type, String content) {
      this.type = type;
      this.content = content;
    }

    public Keys asSingleton() {
      return new TestKeys(Collections.singleton(this));
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof TestKey)) { return false; }
      TestKey that = (TestKey) o;
      return this.type == that.type && this.content.equals(that.content);
    }

    @Override public int hashCode() { return content.hashCode(); }

    @Override public String toString() {
      return "(Key " + type + " " + (content.hashCode() & 0xffffffffL) + " : "
          + content.replace("\n", "\\n") + ")";
    }
  }

  static final class TestKeys implements JobCache.Keys {
    final Set<TestKey> keys;

    TestKeys(Set<TestKey> keys) { this.keys = keys; }

    public Keys union(Keys other) {
      if (!other.iterator().hasNext()) { return this; }
      Set<TestKey> all = Sets.newHashSet();
      all.addAll(keys);
      all.addAll(((TestKeys) other).keys);
      return new TestKeys(all);
    }

    public Iterator<JobCache.Key> iterator() {
      final Iterator<TestKey> it = keys.iterator();
      return new Iterator<JobCache.Key>() {
        public boolean hasNext() { return it.hasNext(); }
        public Key next() { return it.next(); }
        public void remove() { throw new UnsupportedOperationException(); }
      };
    }

    @Override public String toString() {
      return keys.toString();
    }

    @Override
    public int hashCode() { return keys.hashCode(); }

    @Override
    public boolean equals(Object o) {
      return o instanceof TestKeys && keys.equals(((TestKeys) o).keys);
    }
  }

  static final class TestJobCache extends JobCache {
    final Map<TestKey, List<Job>> jobMap = Maps.newLinkedHashMap();
    int nServedFromCache = 0;

    @Override public List<Job> fetch(Key k) {
      List<Job> cached = jobMap.get(k);
      if (cached == null) { return null; }
      nServedFromCache += cached.size();
      System.err.println("FETCHING " + k);
      // HACK DEBUG
      for (Job job : cached) {
        System.err.println("    " + render(job.getRoot()).replace("\n", "\\n"));
      }
      // END HACK
      return cloneJobList(cached);
    }

    private static List<Job> cloneJobList(List<? extends Job> jobs) {
      List<Job> out = Lists.newArrayList();
      for (Job job : jobs) {
        out.add(Job.job(job.getRoot().clone(), job.getBaseUri()));
      }
      return out;
    }

    @Override
    public Key forJob(ContentType type, ParseTreeNode node) {
      return new TestKey(type, render(node));
    }

    @Override
    public void store(Key k, List<? extends Job> derivatives) {
      System.err.println("STORING  " + k);
      for (Job derivative : derivatives) {
        System.err.println(
            "    " + render(derivative.getRoot()).replace("\n", "\\n"));
      }
      List<Job> old = jobMap.put((TestKey) k, cloneJobList(derivatives));
      if (old != null) {
        throw new IllegalStateException(
            "Tests shouldn't overwrite cache entries");
      }
    }
  }

  @Override protected PluginMeta createPluginMeta() {
    PluginMeta meta = super.createPluginMeta();
    meta.setIdClass("foo123___");
    return meta;
  }

  @Override protected void setUp() throws Exception {
    super.setUp();
    cache = new TestJobCache();
    is = new InputSource(URI.create(
        "test://example.org/" + getClass().getSimpleName()));
  }

  @Override protected void tearDown() throws Exception {
    super.tearDown();
    cache = null;
  }

  private static final String CACHEABLE_HELLO_WORLD_CSS
      = "<style>b { color: blue; font-weight: inherit }</style>";
  private static final String REWRITTEN_HELLO_WORLD_CSS = Join.join(
      "\n",
      "<style type=\"text/css\">",
      ".foo123___ b {",
      "  color: blue;",
      "  font-weight: inherit",
      "}</style>");

  private static final String CACHEABLE_HELLO_WORLD_CSS_VARIANT
      = "<style>p { color: purple }</style>";
  private static final String REWRITTEN_HELLO_WORLD_CSS_VARIANT = Join.join(
      "\n",
      "<style type=\"text/css\">",
      ".foo123___ p {",
      "  color: purple",
      "}</style>");

  private static final String REWRITTEN_HELLO_WORLD_CSS_AND_VARIANT = Join.join(
      "\n",
      "<style type=\"text/css\">",
      ".foo123___ b {",
      "  color: blue;",
      "  font-weight: inherit",
      "}",
      ".foo123___ p {",
      "  color: purple",
      "}</style>");

  private static final String REWRITTEN_HELLO_WORLD_VARIANT_AND_CSS = Join.join(
      "\n",
      "<style type=\"text/css\">",
      ".foo123___ p {",
      "  color: purple",
      "}",
      ".foo123___ b {",
      "  color: blue;",
      "  font-weight: inherit",
      "}</style>");


  private static final String jsModulePrefix(
      String[] importMembers, String[] vars) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    sb.append("  var dis___ = IMPORTS___;\n");
    for (String importMember : importMembers) {
      sb.append(importMember).append("\n");
    }
    sb.append("  var moduleResult___");
    for (String var : vars) {
      sb.append(", ").append(var);
    }
    sb.append(";\n");
    sb.append("  moduleResult___ = ___.NO_RESULT;\n");
    return sb.toString();
  }

  private static final String JS_MODULE_SUFFIX = Join.join(
      "\n",
      "",
      "    el___ = emitter___.finish();",
      "    emitter___.signalLoaded();",
      "  }",
      "  return moduleResult___;",
      "}");

  private static final String CACHEABLE_HELLO_WORLD_HTML
      = "<b onclick=alert('Hello')>Hello, World!</b>";
  private static final String REWRITTEN_HELLO_WORLD_HTML
      = "<caja-v-html><caja-v-head id=@head_brk></caja-v-head><caja-v-body>" +
      "<b id=\"@b_brk\">Hello, World!</b>";
  private static final String REWRITTEN_HELLO_WORLD_HTML_HELPER_JS
      = jsModulePrefix(
          new String[] {},
          new String[] { "el___", "emitter___", "@handler___" })
      + Join.join(
      "\n",
      "  {",
      "    emitter___ = IMPORTS___.htmlEmitter___;",
      "    el___ = emitter___.byId(@b_brk);",
      "    @handler___ = ___.markConstFunc(function (event, thisNode___) {",
      "        (IMPORTS___.alert_v___? IMPORTS___.alert:"
        + " ___.ri(IMPORTS___, 'alert'))",
      "        .i___('Hello');",
      "      });",
      "    el___.onclick = function (event) {",
      ("      return ___.plugin_dispatchEvent___"
       + "(this, event, ___.getId(IMPORTS___),"),
      "        @handler___, 2);",
      "    };",
      "    emitter___.rmAttr(el___, 'id');")
      + JS_MODULE_SUFFIX;

  private static final String BREAK_AT_HEAD = Join.join(
      "\n",
      "{",
      "  var dis___ = IMPORTS___;",
      "  var moduleResult___, el___, emitter___;",
      "  moduleResult___ = ___.NO_RESULT;",
      "  {",
      "    emitter___ = IMPORTS___.htmlEmitter___;",
      "    el___ = emitter___.byId(@head_brk);",
      "    emitter___.attach(@head_brk);",
      "    emitter___.rmAttr(el___, 'id');",
      "  }",
      "  return moduleResult___;",
      "}");

  private static final String REWRITTEN_HELLO_WORLD_HTML_HELPER_JS_NO_LOAD
      = REWRITTEN_HELLO_WORLD_HTML_HELPER_JS
        .replaceFirst("\n *emitter___\\.signalLoaded\\(\\);", "");


  private static final String CACHEABLE_HELLO_WORLD_JS
      = "<script>alert('Hello');</script>";
  private static final String CACHEABLE_HELLO_WORLD_JS_DEFERRED
      = "<script defer>alert('Hello');</script>";
  private static final String REWRITTEN_HELLO_WORLD_JS
      = jsModulePrefix(
          new String[] {},
          new String[] {})
      + Join.join(
      "\n",
      "  try {",
      "    {",
      "      moduleResult___ = (IMPORTS___.alert_v___? IMPORTS___.alert:",
      "        ___.ri(IMPORTS___, 'alert')).i___('Hello');",
      "    }",
      "  } catch (ex___) {",
      "    ___.getNewModuleHandler().handleUncaughtException(ex___,",
      "      IMPORTS___.onerror_v___? IMPORTS___.onerror: ___.ri(IMPORTS___,",
      "        'onerror'), 'PipelineCacheTest', '1');",
      "  }",
      "  return moduleResult___;",
      "}");

  private static final String CACHEABLE_HELLO_WORLD_JS_VARIANT
      = "<script>alert('Howdy');</script>";
  private static final String CACHEABLE_HELLO_WORLD_JS_VARIANT_DEFERRED
      = "<script defer>alert('Howdy');</script>";
  private static final String REWRITTEN_HELLO_WORLD_JS_VARIANT
      = jsModulePrefix(
          new String[] {},
          new String[] {})
      + Join.join(
      "\n",
      "  try {",
      "    {",
      "      moduleResult___ = (IMPORTS___.alert_v___? IMPORTS___.alert:",
      "        ___.ri(IMPORTS___, 'alert')).i___('Howdy');",
      "    }",
      "  } catch (ex___) {",
      "    ___.getNewModuleHandler().handleUncaughtException(ex___,",
      "      IMPORTS___.onerror_v___? IMPORTS___.onerror: ___.ri(IMPORTS___,",
      "        'onerror'), 'PipelineCacheTest', '1');",
      "  }",
      "  return moduleResult___;",
      "}");

  private static final String SIGNAL_LOADED_JS = Join.join(
      "\n",
      "{",
      "  var dis___ = IMPORTS___;",
      "  var moduleResult___;",
      "  moduleResult___ = ___.NO_RESULT;",
      "  {",
      "    IMPORTS___.htmlEmitter___.signalLoaded();",
      "  }",
      "  return moduleResult___;",
      "}");

  public final void testEmptyCache() throws Exception {
    ((EchoingMessageQueue) mq).setDumpStack(true);
    JobStub[] golden = new JobStub[] {
        job(REWRITTEN_HELLO_WORLD_CSS, ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML, ContentType.HTML),
        job(BREAK_AT_HEAD, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_JS, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_HTML_HELPER_JS, ContentType.JS),
        //job(SIGNAL_LOADED_JS, ContentType.JS),
    };
    assertPipeline(
        job(
            CACHEABLE_HELLO_WORLD_CSS + CACHEABLE_HELLO_WORLD_JS
            + CACHEABLE_HELLO_WORLD_HTML,
            ContentType.HTML),
        golden);
    assertEquals(0, cache.nServedFromCache);

    meta = createPluginMeta();
    assertPipeline(
        job(
            CACHEABLE_HELLO_WORLD_CSS + CACHEABLE_HELLO_WORLD_JS
            + CACHEABLE_HELLO_WORLD_HTML,
            ContentType.HTML),
        golden);
    // The CSS and two JS files are served from the cache,
    // but not the HTML since the JS has to be integrated back into the HTML.
    assertEquals(3, cache.nServedFromCache);
  }

  public final void testMixAndMatchJs() throws Exception {
    // Prime the cache.
    assertPipeline(
        job(
            CACHEABLE_HELLO_WORLD_CSS + CACHEABLE_HELLO_WORLD_HTML
            + CACHEABLE_HELLO_WORLD_JS_DEFERRED,
            ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_CSS, ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML, ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML_HELPER_JS_NO_LOAD, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_JS, ContentType.JS),
        job(SIGNAL_LOADED_JS, ContentType.JS));
    assertEquals(0, cache.nServedFromCache);

    meta = createPluginMeta();
    assertPipeline(
        job(
            CACHEABLE_HELLO_WORLD_CSS + CACHEABLE_HELLO_WORLD_HTML
            + CACHEABLE_HELLO_WORLD_JS_VARIANT_DEFERRED,
            ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_CSS, ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML, ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML_HELPER_JS_NO_LOAD, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_JS_VARIANT, ContentType.JS),
        job(SIGNAL_LOADED_JS, ContentType.JS));
    // Some compiled JS was served from the cache.
    assertEquals(2, cache.nServedFromCache);
  }

  public final void testMixAndMatchCss() throws Exception {
    // Prime the cache.
    assertPipeline(
        job(
            CACHEABLE_HELLO_WORLD_CSS + CACHEABLE_HELLO_WORLD_HTML
            + CACHEABLE_HELLO_WORLD_JS_DEFERRED,
            ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_CSS, ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML, ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML_HELPER_JS_NO_LOAD, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_JS, ContentType.JS),
        job(SIGNAL_LOADED_JS, ContentType.JS));
    assertEquals(0, cache.nServedFromCache);
    System.err.println("Done");

    meta = createPluginMeta();
    assertPipeline(
        job(
            CACHEABLE_HELLO_WORLD_CSS_VARIANT + CACHEABLE_HELLO_WORLD_HTML
            + CACHEABLE_HELLO_WORLD_JS_DEFERRED,
            ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_CSS_VARIANT, ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML, ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML_HELPER_JS_NO_LOAD, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_JS, ContentType.JS),
        job(SIGNAL_LOADED_JS, ContentType.JS));
    // The JS was served from the cache, but not the CSS.
    assertEquals(3, cache.nServedFromCache);
  }

  public final void testCssCacheOrdering() throws Exception {
    // Prime the cache.
    assertPipeline(
        job(
            CACHEABLE_HELLO_WORLD_CSS + CACHEABLE_HELLO_WORLD_CSS_VARIANT
            + CACHEABLE_HELLO_WORLD_HTML + CACHEABLE_HELLO_WORLD_JS_DEFERRED,
            ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_CSS_AND_VARIANT, ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML, ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML_HELPER_JS_NO_LOAD, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_JS, ContentType.JS),
        job(SIGNAL_LOADED_JS, ContentType.JS));
    assertEquals(0, cache.nServedFromCache);

    // Now render, but with the order of CSS changed.
    // CSS order is significant because when rule overlap, the later one wins.
    // So the text is blue in
    //   <style>p{color:red}</style><style>p{color:blue}</style><p>Hi</p>.
    meta = createPluginMeta();
    assertPipeline(
        job(
            CACHEABLE_HELLO_WORLD_CSS_VARIANT + CACHEABLE_HELLO_WORLD_CSS
            + CACHEABLE_HELLO_WORLD_HTML + CACHEABLE_HELLO_WORLD_JS_DEFERRED,
            ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_VARIANT_AND_CSS, ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML, ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML_HELPER_JS_NO_LOAD, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_JS, ContentType.JS),
        job(SIGNAL_LOADED_JS, ContentType.JS));
    // The JS was served from the cache as well as both CSS blocks.
    assertEquals(3, cache.nServedFromCache);
  }

  public final void testDeferredJsCacheOrdering() throws Exception {
    // Prime the cache.
    assertPipeline(
        job(
            CACHEABLE_HELLO_WORLD_JS + CACHEABLE_HELLO_WORLD_JS_VARIANT_DEFERRED
            + CACHEABLE_HELLO_WORLD_HTML,
            ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML, ContentType.HTML),
        job(BREAK_AT_HEAD, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_JS, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_HTML_HELPER_JS_NO_LOAD,
            ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_JS_VARIANT, ContentType.JS),
        job(SIGNAL_LOADED_JS, ContentType.JS));
    assertEquals(0, cache.nServedFromCache);

    // Make sure deferring scripts doesn't cause them to execute out of order.
    meta = createPluginMeta();
    assertPipeline(
        job(
            CACHEABLE_HELLO_WORLD_JS_DEFERRED + CACHEABLE_HELLO_WORLD_JS_VARIANT
            + CACHEABLE_HELLO_WORLD_HTML,
            ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML, ContentType.HTML),
        job(BREAK_AT_HEAD, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_JS_VARIANT, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_HTML_HELPER_JS_NO_LOAD, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_JS, ContentType.JS),
        job(SIGNAL_LOADED_JS, ContentType.JS));
    // Little JS blocks were served from the cache.
    assertEquals(5, cache.nServedFromCache);
  }

  public final void testJsCacheOrdering() throws Exception {
    // Prime the cache.
    assertPipeline(
        job(
            CACHEABLE_HELLO_WORLD_JS + CACHEABLE_HELLO_WORLD_JS_VARIANT
            + CACHEABLE_HELLO_WORLD_HTML,
            ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML, ContentType.HTML),
        job(BREAK_AT_HEAD, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_JS, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_JS_VARIANT, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_HTML_HELPER_JS, ContentType.JS));
    assertEquals(0, cache.nServedFromCache);

    // Make sure reordering scripts doesn't cause them to execute out of order.
    meta = createPluginMeta();
    assertPipeline(
        job(
            CACHEABLE_HELLO_WORLD_JS_VARIANT + CACHEABLE_HELLO_WORLD_JS
            + CACHEABLE_HELLO_WORLD_HTML,
            ContentType.HTML),
        job(REWRITTEN_HELLO_WORLD_HTML, ContentType.HTML),
        job(BREAK_AT_HEAD, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_JS_VARIANT, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_JS, ContentType.JS),
        job(REWRITTEN_HELLO_WORLD_HTML_HELPER_JS, ContentType.JS));
    // Some JS blocks were served from the cache.
    assertEquals(4, cache.nServedFromCache);
  }

  public final void testIhabsHeadache() throws Exception {
    JobStub[] goldens = {
        job("<caja-v-html><caja-v-head></caja-v-head><caja-v-body>" +
            "<p id=\"id_2___\">1337</p>" +
            "</caja-v-body></caja-v-html>",
            ContentType.HTML),
        job(jsModulePrefix(
            new String[] {},
            new String[] { "el___", "emitter___", "c_1___" })
            + Join.join(
                "\n",
                "  {",
                "    emitter___ = IMPORTS___.htmlEmitter___;",
                "    el___ = emitter___.byId('id_2___');",
                "    c_1___ = ___.markConstFunc(function (event, thisNode___) {",
                "        (IMPORTS___.alert_v___? IMPORTS___.alert:"
                + " ___.ri(IMPORTS___, 'alert'))",
                "        .i___(1337);",
                "      });",
                "    el___.onclick = function (event) {",
                "      return ___.plugin_dispatchEvent___(this, event, ___.getId(IMPORTS___),",
                "        c_1___, 2);",
                "    };",
                "    emitter___.rmAttr(el___, 'id');")
                + JS_MODULE_SUFFIX, ContentType.JS)
    };

    // Prime the cache.
    assertPipeline(
        job("<p onclick=\"alert(1337);\">1337</p>", ContentType.HTML),
        goldens);
    assertEquals(0, cache.nServedFromCache);

    // Make sure that rerunning doesn't cause problems.
    meta = createPluginMeta();
    assertPipeline(
        job("<p onclick=\"alert(1337);\">1337</p>", ContentType.HTML),
        goldens);
    // The JS block was served from the cache.
    assertEquals(1, cache.nServedFromCache);
  }

  public final void testCachedRewriterError() throws Exception {
    assertPipelineFails(
        job("with(e){}", ContentType.JS));
    assertMessage(
        true, RewriterMessageType.WITH_BLOCKS_NOT_ALLOWED,
        MessageLevel.ERROR);
    assertNoWarnings();

    assertPipelineFails(
        job("with(e){}", ContentType.JS));
    assertMessage(
        true, RewriterMessageType.WITH_BLOCKS_NOT_ALLOWED,
        MessageLevel.ERROR);
    assertNoWarnings();
  }


  @Override
  protected boolean runPipeline(Jobs jobs) throws Exception {
    Pipeline<Jobs> pl = new Pipeline<Jobs>();
    PipelineMaker plm = new PipelineMaker(
        CssSchema.getDefaultCss21Schema(mq),
        HtmlSchema.getDefault(mq),
        new ModuleManager(
            meta, TestBuildInfo.getInstance(), UriFetcher.NULL_NETWORK, mq),
        cache,
        PipelineMaker.DEFAULT_PRECONDS,
        PipelineMaker.CAJOLED_MODULES.with(PipelineMaker.HTML_SAFE_STATIC)
            .with(PipelineMaker.SANITY_CHECK)
        );
    plm.populate(pl.getStages());
    pl.getStages().add(new PipelineStoreStage(cache));
    pl.getStages().add(new Pipeline.Stage<Jobs>() {
      public boolean apply(Jobs jobs) {
        for (ListIterator<JobEnvelope> it = jobs.getJobs().listIterator();
             it.hasNext();) {
          JobEnvelope env = it.next();
          Job j = env.job;
          if (env.fromCache || j.getType() != ContentType.JS) { continue; }
          if (!(j.getRoot() instanceof CajoledModule)) {
            throw new IllegalStateException("Some content not cajoled");
          }
          CajoledModule m = (CajoledModule) j.getRoot();
          Statement body = m.getInstantiateMethod().getBody();
          it.set(env.withJob(Job.jsJob(body, j.getBaseUri())));
        }
        return true;
      }
    });
    return pl.apply(jobs);
  }
}
