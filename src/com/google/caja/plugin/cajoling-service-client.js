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
 * Client for the cajoling service which provides access to, and caches, the
 * JSON response from the service.
 *
 * @param serviceUrl the URL of the cajoling service.
 * @param jsonRequestChannel a JSON request channel for the communication.
 * @param emitHtmlInJs whether HTML in cajoleable input should be embedded as
 *     imperative statements in the output JS. The alternative is that the
 *     response JSON contains a field, 'html', containing the sanitized static
 *     HTML from the input content.
 * @param debug whether debuggable cajoled code is desired (larger but more
 *     readable).
 * @param console [optional] a console-like object to which errors are written.
 *
 * @requires Q, encodeURIComponent, cajaBuildVersion
 * @overrides window
 * @provides cajolingServiceClientMaker
 */
var cajolingServiceClientMaker = function(serviceUrl,
                                          jsonRequestChannel,
                                          emitHtmlInJs,
                                          debug,
                                          console) {
  // Map from full module URLs to module JSON records.
  var cache = {};

  var makeServiceReference = function(
      uncajoledSourceUrl, mimeType, domOpts)
  {
    var opt_idClass = domOpts && domOpts.idClass;
    return serviceUrl +
        '?url=' + encodeURIComponent(uncajoledSourceUrl) +
        '&build-version=' + cajaBuildVersion +
        '&directive=ES53' +
        '&emit-html-in-js=' + emitHtmlInJs +
        '&renderer=' + (debug ? 'pretty' : 'minimal') +
        '&input-mime-type=' + mimeType +
        (opt_idClass ? '&id-class=' + opt_idClass : '');
  };

  var messagesToLog = function(moduleURL, cajolerMessages) {
    if (!cajolerMessages) { return; }
    if (!console) { return; }
    var msg;
    for (var i = 0; i < cajolerMessages.length; i++) {
      msg = cajolerMessages[i];
      console.log(
          msg.name + '(' + msg.level + ') '
          + msg.type + ': ' + msg.message);
    }
  };

  var handleRequest = function (fullUrl, request, deferred) {
    Q.when(
        request,
        function(moduleJson) {
          messagesToLog(fullUrl, moduleJson.messages);
          if (moduleJson.js) {
            deferred.resolve(moduleJson);
          } else {
            deferred.resolve(Q.reject('Cajoling errors'));
          }
        },
        function(err) {
          deferred.resolve(Q.reject(err));
        });
  };

  /**
   * Cajole the content available at a specified URL.
   *
   * The cajoled result from a given URL is cached by this object beween
   * invocations.
   *
   * @param url the url of the content.
   * @param mimeType the MIME type of the content.
   *
   * @return a promise for the module JSON returned from the cajoler.
   */
  var cajoleUrl = function (url, mimeType) {
    if (!cache['$' + url]) {
      cache['$' + url] = Q.defer();
      handleRequest(
          url,
          jsonRequestChannel.request(makeServiceReference(url, mimeType)),
          cache['$' + url]);
    }
    return cache['$' + url].promise;
  };

  /**
   * Cajole a given block of content.
   *
   * The results of content cajoling are never cached between invocations.
   *
   * @param url the url of the content.
   * @param content the content to be cajoled.
   * @param mimeType the MIME type of the content.
   * @param domOpts optional DOM related config options
   *     - property opt_idClass the id/class suffix to use in static html
   *
   * @return a promise for the module JSON returned from the cajoler.
   */
  var cajoleContent = function (url, content, mimeType, domOpts) {
    var result = Q.defer();
    handleRequest(
        url,
        jsonRequestChannel.request(
            makeServiceReference(url, mimeType, domOpts),
            content,
            mimeType),
        result);
    return result.promise;
  };

  return {
    cajoleUrl: cajoleUrl,
    cajoleContent: cajoleContent
  };
};

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['cajolingServiceClientMaker'] = cajolingServiceClientMaker;
}
