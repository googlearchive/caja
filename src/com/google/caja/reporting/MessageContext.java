// Copyright (C) 2005 Google Inc.
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

package com.google.caja.reporting;

import com.google.caja.lexer.InputSource;
import com.google.caja.util.Abbreviator;
import com.google.caja.util.SyntheticAttributeKey;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Context needed to render a {@link MessagePart message part}.
 *
 * @author mikesamuel@gmail.com
 */
public class MessageContext {
  private Collection<InputSource> inputSources =
      Collections.<InputSource>emptySet();
  private Abbreviator abbreviator;

  /**
   * The group of input sources supplied by the viewer of messages.  Used to
   * make sure that shortened file names in messages are still unambiguous.
   * @see #abbreviate
   */
  public Collection<InputSource> getInputSources() {
    return inputSources;
  }

  public void addInputSource(InputSource src) {
    if (inputSources.isEmpty()) {
      inputSources = new LinkedHashSet<InputSource>();
    }
    if (inputSources.add(src)) {
      abbreviator = null;
    }
  }

  public String abbreviate(InputSource toAbbreviate) {
    if (abbreviator == null) {
      Set<String> uris = new HashSet<String>();
      for (InputSource is : inputSources) {
        uris.add(is.getUri().toString());
      }
      abbreviator = new Abbreviator(uris, "/");
    }
    return abbreviator.unambiguousAbbreviationFor(
        toAbbreviate.getUri().toString());
  }

  /**
   * The synthetic attribute keys which should be output when dumping an AST for
   * debugging.
   */
  public Set<? extends SyntheticAttributeKey<?>> relevantKeys =
      Collections.<SyntheticAttributeKey<?>>emptySet();
}
