/**
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview
 * Extensions to slidy to make it easier to use for demos
 *
 * @author jasvir@gmail.com
 */

var gotoSlide = (function() {
  return function(targetSlideNum) {
    var slide = slides[slidenum];
    hideSlide(slide);
    slidenum = targetSlideNum;
    slide = slides[slidenum];
    lastShown = null;
    setLocation();
    setVisibilityAllIncremental("hidden");
    setEosStatus(!nextIncrementalItem(lastShown));
    showSlide(slide);
  }
})();

