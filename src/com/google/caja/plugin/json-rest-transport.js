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
 * A transport layer that queries RESTful URLs and returns the results as
 * JSON values. This depends on the following _de facto_ "standard" URL
 * parameters to RESTful services:
 *
 *   &alt=json               means that the reply should be JSON formatted.
 *
 *   &callback=cbName        means that the request expects a JSONP reply
 *                           suitable for embedding in a <script> tag, and
 *                           'cbName' is the name of the desired callback
 *                           function.
 *
 * TODO(ihab.awad): Test IE XDomainRequest transport
 *
 * @param opt_transportType the transport chosen, which can be any of
 *     'w3cxhr', 'msxdr', or 'jsonp'. Omit this parameter to allow this
 *     module to select the most suitable for the platform.
 *
 * @requires XDomainRequest, XMLHttpRequest, Q, JSON, document, URI
 * @overrides window
 * @provides jsonRestTransportMaker
 */
var jsonRestTransportMaker = function(opt_transportType) {
  if (!Object.hasOwnProperty.call(window, '___caja_mod_count___')) {
    window['___caja_mod_count___'] = 0;
  }

  var addParamsToUrl = function(url, params) {
    var parsedUri = URI.parse(url);
    var i = 0;
    while (i < params.length) {
      parsedUri.setParameterValues(params[i++], [params[i++]]);
    }
    return parsedUri.toString();
  };

  var requestFunctions = {};

  /**
   * W3C standard XHR transport.
   */
  requestFunctions['w3cxhr'] = function(url, content, contentType) {
    url = addParamsToUrl(url, [ 'alt', 'json' ]);
    var result = Q.defer();
    var request = new XMLHttpRequest();

    request.onreadystatechange = function() {
      if (request.readyState === 4) {
        if (request.status === 200) {
          result.resolve(JSON.parse(request.responseText));
        } else {
          try {
            result.resolve(Q.reject('HTTP error '+ request.status));
          } catch (ex) {
            // Failure code 0x80040111 on Gecko browsers
            result.resolve(Q.reject('Unknown HTTP error'));
          }
        }
      }
    };
    request.open(content ? 'POST' : 'GET', url, true);
    if (content) {
      request.setRequestHeader('Content-Type', contentType);
      request.send(content);
    } else {
      request.send();
    }
    return result.promise;
  };

  /**
   * MS XDomainRequest transport for supporting browsers (Internet Explorer).
   */
  requestFunctions['msxdr'] = function(url, content, contentType) {
    url = addParamsToUrl(url, [ 'alt', 'json' ]);
    var result = Q.defer();
    var xdr = new XDomainRequest();
    xdr.onerror = function() {
      result.resolve(Q.reject('XDomainRequest error'));
    };
    xdr.ontimeout = function() {
      result.resolve(Q.reject('XDomainRequest timeout'));
    };
    xdr.onload = function() {
      result.resolve(JSON.parse(xdr.responseText));
    };
    xdr.open(content ? 'post' : 'get', url);
    if (content) {
      xdr.send(content);
    } else {
      xdr.send();
    }
    return result.promise;
  };

  /**
   * JSONP transport for cross-browser support. Currently does not support the
   * posting of content.
   */
  requestFunctions['jsonp'] = (function() {
    var jsonpCallbackCount = 0;

    return function(url, content, contentType) {
      if (content) {
        // TODO(ihab.awad): We might still be able to support a *limited* amount
        // of posted content if we encode it into the URL. Is this worth doing?
        throw 'Posted content not supported by this transport';
      }

      var jsonpCallbackName =
          '___caja_mod_' + window.___caja_mod_count___++ + '___';
      var jsonUrl =
          addParamsToUrl(url, [ 'alt', 'json', 'callback', jsonpCallbackName ]);
      var w = window;  // Caja linter rejects direct assignment to 'window'
      var result = Q.defer();

      var script = document.createElement('script');
      script.src = jsonUrl;
      script.onerror = function() {
        result.resolve(Q.reject('Error fetching ' + url + ' via JSONP'));
      };
      document.getElementsByTagName('head')[0].appendChild(script);

      w[jsonpCallbackName] = function(responseJson) {
        delete w[jsonpCallbackName];
        document.getElementsByTagName('head')[0].removeChild(script);
        result.resolve(responseJson);
      };

      return result.promise;
    };
  })();

  var request;

  if (opt_transportType) {
    request = requestFunctions[opt_transportType];
    if (!request) {
      throw new Error('Transport type not found: ' + opt_transportType);
    }
  } else {
    if (window.XMLHttpRequest && 'withCredentials' in new XMLHttpRequest()) {
      // FF 3.5+ and Safari 4
      request = requestFunctions['w3cxhr'];
    } else if (window.XDomainRequest) {
      // IE8
      request = requestFunctions['msxdr'];
    } else {
      // Older browser; fallback
      request = requestFunctions['jsonp'];
    }
  }

  return {
    request: request
  };
};
