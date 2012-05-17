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

if (testCase) {
  caja.makeFrameGroup({
    cajaServer: '/caja',
    debug: true,
    forceES5Mode: inES5Mode
  }, function(frameGroup) {
    frameGroup.makeES5Frame(
        createDiv(),
        {
          fetch: caja.policy.net.ALL.fetch,
          rewrite: function (uri, uriEffect, loaderType, hints) {
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
               .run(createExtraImportsForTesting(frameGroup, frame),
                   function(result) {
                     readyToTest();
                     jsunitRun();
                     asyncRequirements.evaluate();
                   });
        });
  });
} else {
  console.log('Parameter "test-case" not specified in URL');
}
