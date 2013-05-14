// Copyright (C) 2013 Google Inc.
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.caja.util.Executor;
import com.google.caja.util.RhinoTestBed;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

/**
 * A collection of local URLs for browser-based tests to run.
 *
 * @author kpreid@switchb.org
 */
public class BrowserTestCatalog {
  private static final ConcurrentHashMap<URI, BrowserTestCatalog> memo =
      new ConcurrentHashMap<URI, BrowserTestCatalog>();

  private final List<Entry> entries;
  private final Map<String, Entry> entriesByLabel;

  /**
   * Obtain a catalog parsed from JSON at the given <em>resource</em> URL.
   * @throws IOException
   */
  public static BrowserTestCatalog get(URL url) throws IOException {
    // thread-safe sloppy memoization
    URI key;
    try {
      key = url.toURI();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    BrowserTestCatalog value = memo.get(key);
    if (value == null) {
      value = new BrowserTestCatalog(url);
      memo.putIfAbsent(key, value);
    }
    return value;
  }

  public BrowserTestCatalog(URL url) throws IOException {
    String json = Resources.toString(url, Charsets.UTF_8);
    List<Entry> entriesAcc = new ArrayList<Entry>();
    RhinoTestBed.runJs(
        new ParserOutput(entriesAcc),
        new Executor.Input(BrowserTestCatalog.class, "catalog-parser.js"),
        new Executor.Input("parseTestCatalog(" + json + ", caja___, true)",
            "<BrowserTestCatalog stub>"));
    entries = Collections.unmodifiableList(entriesAcc);
    entriesByLabel = Maps.uniqueIndex(entries, new Function<Entry, String>() {
      @Override
      public String apply(Entry entry) {
        return entry.getLabel();
      }
    });
  }

  // Note: This class is explicitly whitelisted in RhinoExecutor.
  public static class ParserOutput {
    private final List<Entry> entries;
    private ParserOutput(List<Entry> entries) {
      this.entries = entries;
    }
    public ParserOutput addGroup(String label, String comment) {
      return this;
    }
    public ParserOutput addMiniGroup(String label, String comment) {
      return this;
    }
    public void addTest(String url, String label, String longLabel,
        String comment, boolean manual, String expectedFailure) {
      entries.add(new Entry(url, longLabel, expectedFailure));
    }
    public void addNonTest(String label, String comment) {}
  }

  public List<Entry> entries() {
    return entries;
  }

  public Entry getTest(String label) {
    return entriesByLabel.get(label);
  }

  public static class Entry {
    private final String url;
    private final String longLabel;
    private final String expectedFailure;

    /**
     * @param expectedFailure May be null
     */
    public Entry(String url, String longLabel, String expectedFailure) {
      this.url = url;
      this.longLabel = longLabel;
      this.expectedFailure = expectedFailure;
    }

    public String getURL() {
      return this.url;
    }

    public String getLabel() {
      return this.longLabel;
    }

    public boolean mayFail() {
      return expectedFailure != null;
    }

    public String getExpectedFailureReason() {
      return expectedFailure;
    }
  }
}
