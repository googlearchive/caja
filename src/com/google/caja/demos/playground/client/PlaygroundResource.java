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

package com.google.caja.demos.playground.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;

/**
 * Client bundle for the playground
 *
 * @author Jasvir Nagra <jasvir@gmail.com>
 */

public interface PlaygroundResource extends ClientBundle {
  public static final PlaygroundResource INSTANCE =
    GWT.create(PlaygroundResource.class);

  @Source("caja_logo_small.png")
  public ImageResource logo();

  @Source("policy.js")
  public TextResource defaultPolicy();

  @Source("ajax-loader.gif")
  public ImageResource loading();
}