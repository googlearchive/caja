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
 * @requires setTimeout URI
 * @provides GuestManager
 * @overrides window
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

function GuestManager(frameTamingSchema, frameTamingMembrane, divs, hostBaseUrl,
  domicile, htmlEmitter, guestWin, USELESS, uriPolicy, runImpl) {
  // TODO(felix8a): this api needs to be simplified; it's difficult to
  // explain what all the parameters mean in different situations.
  var args = {
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
    idClass: divs.idClass,
    getUrl: function() { return args.url; },
    getUriPolicy: function() { return uriPolicy; },

    // deprecated; idSuffix in domado means '-' + idClass, but idSuffix
    // exposed here is without the leading '-'.  Future code should use the
    // idClass property instead.
    idSuffix: divs.idClass,

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
    htmlEmitter: htmlEmitter,

    rewriteUri: domicile ? domicile.rewriteUri : function() { return null; },

    // Taming utilities
    tame: frameTamingMembrane.tame,
    untame: frameTamingMembrane.untame,
    tamesTo: frameTamingMembrane.tamesTo,
    reTamesTo: frameTamingMembrane.reTamesTo,
    hasTameTwin: frameTamingMembrane.hasTameTwin,

    markReadOnlyRecord: frameTamingSchema.published.markTameAsReadOnlyRecord,
    markFunction: frameTamingSchema.published.markTameAsFunction,
    markCtor: frameTamingSchema.published.markTameAsCtor,
    markXo4a: frameTamingSchema.published.markTameAsXo4a,
    grantMethod: frameTamingSchema.published.grantTameAsMethod,
    grantRead: frameTamingSchema.published.grantTameAsRead,
    grantReadWrite: frameTamingSchema.published.grantTameAsReadWrite,
    grantReadOverride: frameTamingSchema.published.grantTameAsReadOverride,
    adviseFunctionBefore: frameTamingSchema.published.adviseFunctionBefore,
    adviseFunctionAfter: frameTamingSchema.published.adviseFunctionAfter,
    adviseFunctionAround: frameTamingSchema.published.adviseFunctionAround,

    USELESS: USELESS,

    api: function (imports) {
      args.moreImports = imports;
      return self;
    },

    flash: function(flag) {
      args.flash = !!flag;
      return self;
    },

    code: function (url, opt_mimeType, opt_content) {
      args.url = url;
      args.mimeType = opt_mimeType;
      args.uncajoledContent = opt_content;
      return self;
    },

    cajoled: function (url, js, opt_html) {
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
    if (domicile) {
      domicile.setBaseUri(URI.utils.resolve(hostBaseUrl, args.url));
    }
    return runImpl(self, args, moreImports, function(result) {
      setTimeout(function() { 
          if (opt_runDone) {
            opt_runDone(result);
          }
      }, 0);
    });
  }
}

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['GuestManager'] = GuestManager;
}
