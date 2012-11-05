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

function getUriEffectName(uriEffect) {
  for (var n in html4.ueffects) {
    if (html4.ueffects[n] === uriEffect) { return n; }
  }
  throw new TypeError('Unknown URI effect ' + uriEffect);
}

function getLoaderTypeName(loaderType) {
  for (var n in html4.ltypes) {
    if (html4.ltypes[n] === loaderType) { return n; }
  }
  throw new TypeError('Unknown loader type ' + loaderType);
}

var testCase = getUrlParam('test-case');
var jQuery = (getUrlParam('jQuery') === 'true');

if (testCase) {
  caja.makeFrameGroup({
    cajaServer: '/caja',
    debug: true,
    forceES5Mode: inES5Mode
  }, function(frameGroup) {
    frameGroup.makeES5Frame(
        createDiv(),
        jQuery ? caja.policy.net.ALL : {
          fetch: caja.policy.net.ALL.fetch,
          rewrite: function (uri, uriEffect, loaderType, hints) {
            if (uri.indexOf('test-image-41x13.png') !== -1) {
              // used by es53-test-domado-dom-guest.html
              return 'test-image-41x13.png';
            }
            return URI.create(
                'http',
                null,
                'example.com',
                null,
                '/',
                [
                  'effect', getUriEffectName(uriEffect),
                  'loader', getLoaderTypeName(loaderType),
                  'uri',    uri
                ])
                .toString();
            }
        },
        function(frame) {
          frame.url(testCase)
               .run(jQuery
                        ? null /* don't define, e.g., $ */
                        : createExtraImportsForTesting(frameGroup, frame),
                   function(result) {
                     if (!jQuery) {
                       readyToTest();
                       jsunitRun();
                     }
                     asyncRequirements.evaluate();
                   });
        });
  });
} else {
  console.log('Parameter "test-case" not specified in URL');
}
