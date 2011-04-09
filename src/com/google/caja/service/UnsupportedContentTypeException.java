//Copyright (C) 2008 Google Inc.
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

package com.google.caja.service;
/**
 * Thrown by cajoling service when it is requested to retrieve content
 * for which there is no associated checker.
 * 
 * Only the cajoling service should catch this exception.
 * 
 * @author jasvir@gmail.com (Jasvir Nagra)
 */
public class UnsupportedContentTypeException extends Exception {
  private static final long serialVersionUID = -6268704959998061330L;

    public UnsupportedContentTypeException() {
      this("Unknown content type requested");
    }
  
    public UnsupportedContentTypeException(String msg) {
      super(msg);
    }
}