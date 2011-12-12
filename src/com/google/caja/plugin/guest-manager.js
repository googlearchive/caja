// Copyright (C) 2011 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @provides GuestManager
 */

/**
 * A GuestManager is a handle to an instance of a Caja sandbox.
 *
 * Methods on GuestManager are somewhat redundant because this consolidates
 * what used to be two different but similar objects.
 *
 * API variant 1:
 *    caja.makeFrameGroup(..., function (frameGroup) {
 *        frameGroup.makeES5Frame(..., function (frame) {
 *            frame.url(...).run(api, callback);
 *
 * API variant 2:
 *    caja.load(..., function (frame) {
 *        frame.code(...).api(api).run(callback);
 *    });
 *
 * The "frame" parameters were once different objects with subtly different
 * semantics that don't matter in practice.  GuestManager combines the two.
 */

function GuestManager(divs, domicile, guestWin, runImpl) {
  var args = {
    // When !isCajoled, use [url, mimeType, uncajoledContent].
    // When isCajoled, use [url, cajoledJs, cajoledHtml].
    isCajoled: false,

    // url to fetch, or imputed origin of cajoled or uncajoled content
    url: undefined,

    // Content type for the url or the uncajoledContent.
    // If not specified, uncajoledContent assumes text/html,
    // and url fetch assumes type based on filename suffix.
    mimeType: undefined,

    uncajoledContent: undefined,

    cajoledJs: undefined,
    cajoledHtml: undefined,

    moreImports: undefined,

    // Enable Flash support
    flash: true
  };

  var self = {
    // Public state
    div: divs.outer && divs.outer.parentNode,
    idSuffix: divs.idSuffix,
    iframe: guestWin.frameElement,
    imports: (domicile
              ? domicile.window
              : (guestWin.___
                 ? guestWin.___.copy(guestWin.___.sharedImports) // for es53
                 : {})),                                         // for ses
    innerContainer: divs.inner,
    outerContainer: divs.outer,

    // Internal state
    domicile: domicile,      // Currently exposed only for the test suite

    api: function (imports) {
      args.moreImports = imports;
      return self;
    },

    flash: function(flag) {
      args.flash = !!flag;
      return self;
    },

    code: function (url, opt_mimeType, opt_content) {
      args.isCajoled = false;
      args.url = url;
      args.mimeType = opt_mimeType;
      args.uncajoledContent = opt_content;
      return self;
    },

    cajoled: function (url, js, opt_html) {
      args.isCajoled = true;
      args.url = url;
      args.cajoledJs = js;
      args.cajoledHtml = opt_html;
      return self;
    },

    content: function (url, content, opt_mimeType) {
      return self.code(url, opt_mimeType, content);
    },

    contentCajoled: function (url, js, opt_html) {
      return self.cajoled(url, js, opt_html);
    },

    url: function (url, opt_mimeType) {
      return self.code(url, opt_mimeType, undefined);
    },

    urlCajoled: function (baseUrl, jsUrl, opt_htmlUrl) {
      throw new Error("Not yet implemented");  // TODO(felix8a)
    },

    run: run
  };

  return self;

  //----------------

  function run(opt_arg1, opt_arg2) {
    var moreImports, opt_runDone;
    if (opt_arg2) {
      moreImports = opt_arg1 || args.moreImports || {};
      opt_runDone = opt_arg2;
    } else {
      moreImports = args.moreImports || {};
      opt_runDone = opt_arg1;
    }
    return runImpl(self, args, moreImports, opt_runDone);
  }
}
