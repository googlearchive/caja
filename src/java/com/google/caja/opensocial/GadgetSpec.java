// Copyright (C) 2007 Google Inc.
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

package com.google.caja.opensocial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A parsed gadget specification. This object serves as the interface between
 * user-supplied input and our rewriting machinery to guard against attacks
 * based on malformed input XML.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class GadgetSpec {
  private final Map<String, String> modulePrefs = new HashMap<String, String>();
  private final List<String> requiredFeatures = new ArrayList<String>();
  private String contentType = null;
  private String content = null;

  public Map<String, String> getModulePrefs() { return modulePrefs; }

  public List<String> getRequiredFeatures() { return requiredFeatures; }

  public String getContentType() { return contentType; }

  public void setContentType(String contentType) { this.contentType = contentType; }

  public String getContent() { return content; }

  public void setContent(String content) { this.content = content; }
}
