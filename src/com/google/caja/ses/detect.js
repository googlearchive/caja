// Derived from http://www.quirksmode.org/js/detect.html by Peter-Paul Koch.
// License terms at http://www.quirksmode.org/about/copyright.html

/**
 * @fileoverview Tries to figure out what browser we're on.
 *
 * @author Peter-Paul Koch, with modifications by Mark S. Miller
 * @provides BrowserDetect
 * @requires navigator
 * @overrides window
 */

var BrowserDetect;

(function() {
  "use strict";

  BrowserDetect = {
    init: function() {
      this.browser = this.searchString(this.dataBrowser) ||
        "An unknown browser";
      this.version = this.searchVersion(navigator.userAgent) ||
        this.searchVersion(navigator.appVersion) ||
        "an unknown version";
      this.OS = this.searchString(this.dataOS) || "an unknown OS";
    },
    searchString: function(data) {
      for (var i = 0; i < data.length; i++) {
        var dataString = data[i].string;
        var dataProp = data[i].prop;
        this.versionSearchString = data[i].versionSearch || data[i].identity;
        if (dataString) {
          if (dataString.indexOf(data[i].subString) != -1) {
            return data[i].identity;
          }
        } else if (dataProp) {
          return data[i].identity;
        }
      }
      return void 0;
    },
    searchVersion: function(dataString) {
      var index = dataString.indexOf(this.versionSearchString);
      if (index == -1) { return void 0; }
      var startsWithVersion = dataString.substring(
        index + this.versionSearchString.length + 1);
      var match = (/^([0-9\.]*)/).exec(startsWithVersion);
      return match && match[1];
    },
    dataBrowser: [
      {
        string: navigator.userAgent,
        subString: "Chrome",
        identity: "Chrome"
      },
      {   string: navigator.userAgent,
          subString: "OmniWeb",
          versionSearch: "OmniWeb/",
          identity: "OmniWeb"
      },
      {
        string: navigator.vendor,
        subString: "Apple",
        identity: "Safari",
        versionSearch: "Version"
      },
      {
        prop: window.opera,
        identity: "Opera",
        versionSearch: "Version"
      },
      {
        string: navigator.vendor,
        subString: "iCab",
        identity: "iCab"
      },
      {
        string: navigator.vendor,
        subString: "KDE",
        identity: "Konqueror"
      },
      {
        string: navigator.userAgent,
        subString: "Firefox",
        identity: "Firefox"
      },
      {
        string: navigator.vendor,
        subString: "Camino",
        identity: "Camino"
      },
      {           // for newer Netscapes (6+)
        string: navigator.userAgent,
        subString: "Netscape",
        identity: "Netscape"
      },
      {
        string: navigator.userAgent,
        subString: "MSIE",
        identity: "Explorer",
        versionSearch: "MSIE"
      },
      {
        string: navigator.userAgent,
        subString: "Gecko",
        identity: "Mozilla",
        versionSearch: "rv"
      },
      {           // for older Netscapes (4-)
        string: navigator.userAgent,
        subString: "Mozilla",
        identity: "Netscape",
        versionSearch: "Mozilla"
      }
    ],
    dataOS : [
      {
        string: navigator.platform,
        subString: "Win",
        identity: "Windows"
      },
      {
        string: navigator.platform,
        subString: "Mac",
        identity: "Mac"
      },
      {
        string: navigator.userAgent,
        subString: "iPhone",
        identity: "iPhone/iPod"
      },
      {
        string: navigator.platform,
        subString: "Linux",
        identity: "Linux"
      }
    ]
  };
  BrowserDetect.init();
})();

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['BrowserDetect'] = BrowserDetect;
}
