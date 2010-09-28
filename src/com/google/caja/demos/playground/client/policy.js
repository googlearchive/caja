// Playground policy
//  - exposes flash
//  - exposes alert
tamings___ = [];
/**
 * Simple flash taming
 *   - exposes a taming of the swfobject API
 *   - ensures version of flash > 9 (defaults to v10)
 *   - adds parameters to limit network and prevent script access
 */
tamings___.push(function tameSimpleFlash(___, imports) {
  imports.outers.swfobject = {};
  imports.outers.swfobject.embedSWF = function(swfUrl, id, width, height, 
      version, expressInstall, flashvars, params, attributes, cb) {
    var tameSwfUrl = !/^https?:\/\//i.test(swfUrl) ? null : swfUrl;
    var tameId = id + '-cajoled-output-' + cajaDomSuffix;
    var tameWidth = +width;
    var tameHeight = +height;
    // Default to 10.0 if unspecified or specified < 9
    // else use whatever version >9 the user suggests
    var tameVersion = version || "10.0";
    if (!/^9|([1-9][0-9])\./.test(tameVersion)) {
      tameVersion = "10.0";
    }
    var tameExpressInstall = false;
    var tameParams = { "allowScriptAccess" : "never", "allowNetworking" : "internal"};

    // TODO(jasvir): rewrite attributes
    var tameAttr = null;
    swfobject.embedSWF(tameSwfUrl, tameId, tameWidth, tameHeight, tameVersion,
        tameExpressInstall, flashvars, tameParams, tameAttr, ___.untame(cb));
  };
  ___.grantRead(imports.outers, 'swfobject');
  ___.grantFunc(imports.outers.swfobject, 'embedSWF');
});

/**
 * Throttled alert taming
 *   - exposes a taming of the alert function
 *   - ensures that after 10 alerts the user has the option of redirecting
 *     remaining calls to alert to cajita.log instead
 */
tamings___.push(function tameAlert(___, imports) {
  imports.outers.alert = (function() {
    var remainingAlerts = 10;
    function tameAlert(msg) {
      if (remainingAlerts > 0) {
        remainingAlerts--;
        alert("Untrusted gadget says: " + msg);
      } else if (remainingAlerts == 0) {
        remainingAlerts = confirm("Ignore remaining alerts?") ? -1 : 10;
      }
    };
    return tameAlert;
  })();
  ___.grantFunc(imports.outers, 'alert');
});
