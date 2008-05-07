// Copyright 2008 Google Inc. All Rights Reserved.
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

package com.google.caja.opensocial.service;

import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

/**
 * Tests if two content-types denoted the same type of content.
 * 
 * A "loose" content-type check is for javascript because while 
 * "text/javascript" is recognized by all major browsers it is not a 
 * <a href="http://www.iana.org/assignments/media-types/">registered</a> MIME 
 * type.  
 * 
 * Different server return other MIME types for javascript and xml files. This 
 * checker maps other variants of content-type to the canonical one.
 * 
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class LooseContentTypeCheck extends ContentTypeCheck {
  
  final private Map<String,String> canonicalMimeType = new HashMap<String,String>();
  
  public LooseContentTypeCheck () {
    canonicalMimeType.put("application/x-javascript", "text/javascript");
    canonicalMimeType.put("text/xml", "application/xml");
  }
    
  /**
   * Checks if the {@code candidate} is consistent with {@code spec}
   * @return true {@code candidate} is consistent with {@code spec}
   */
  @Override
  public boolean check(String spec, String candidate) {
    boolean result = false;
    ContentType ctSpec;
    ContentType ctCandidate;
    try {
      ctSpec = new ContentType(spec);
      ctCandidate = new ContentType(candidate);
      result = ctSpec.match(ctCandidate) 
          || ctSpec.match(canonicalMimeType.get(ctCandidate.getBaseType()));
    } catch (ParseException e) {
      e.printStackTrace();
      result = false;
    }
    return result;
  }

}
