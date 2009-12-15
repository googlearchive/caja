/**
 * ID counter for gadget <DIV> IDs.
 */
var gadgetIdCounter = 0;

/**
 * Creates a <DIV> for a new gadget.
 *
 * @return the ID of the newly created <DIV>.
 */
var createGadgetContainer = function(title) {
  var id = 'gadget' + gadgetIdCounter++;
  var div = document.createElement('DIV');
  div.setAttribute('class', 'gadgetFrame');
  div.innerHTML =
      '<div class="gadgetHeader">' + title + '</div>' +
      '<div class="gadgetContainer" id="' + id + '"></div>';
  document.getElementById('gadgets').appendChild(div);
  return {
    id: id,
    element: document.getElementById(id),
    dispose: function() { div.parent.removeChild(div); }
  };
}

/**
 * Given a set of imports, associates it with a new
 * DOM container and attaches Domita environment.
 */
var attachWindow = function(title, imports) {
  // Create a container for our gadget to live in
  var gadgetContainer = createGadgetContainer(String(title));

  // This is where we attach a Domita instance to the DIV
  // in which the untrusted code is sandboxed
  attachDocumentStub(
      '-' + gadgetContainer.id,
      { rewrite: function () { return null; } },
      imports,
      gadgetContainer.element);

  // We attach an HTMLEmitter so that HTML embedded in the
  // untrusted code is also emitted into the sandbox DIV
  imports.htmlEmitter___ = new HtmlEmitter(gadgetContainer.element);

  return {
    dispose: ___.markFuncFreeze(function() {
        gadgetContainer.dispose();
    }),
    toString: ___.markFuncFreeze(function() {
        return 'window "' + title + '"' + ' (id=' + gadgetContainer.id + ')';
    })
  };
}

var crackCompositeBaseUrl = function(base) {
  var re = /\?url\=([^&]+)\&.*/;
  return re.test(base) ? re.exec(base)[1] : base;
}

var basedUrlToAbsolute = function(base, input) {
  if (base.indexOf('/') !== -1) {
    base = base.substring(0, base.lastIndexOf('/'));
  }
  return base + '/' + input;
}

var moduleIdResolver = function(base, input) {
  base = crackCompositeBaseUrl(base);

  var inputMimeType;
  if (/\.js$/.test(input)) {
    inputMimeType = 'application/javascript';
  } else if (/\.html$/.test(input)) {
    inputMimeType = 'text/html';
  } else {
    input = input + '.js';
    inputMimeType = 'application/javascript';
  }

  input = basedUrlToAbsolute(base, input);

  return 'http://localhost:8887/' +
      '?url=' + input +
      '&input-mime-type=' + inputMimeType +
      '&output-mime-type=application/javascript';
}

var scriptModuleLoad =
    scriptModuleLoadMaker(document.location.toString(),
                          moduleIdResolver);

var valijaVow = scriptModuleLoad.async(
    '../google-caja/src/com/google/caja/valija-cajita');

var powerboxVow = scriptModuleLoad.async('powerbox');

var tameSetTimeout = function(f, delay) {
  setTimeout(function() {
    ___.callPub(f, 'call', [___.USELESS]);
  }, Number(delay));
};

Q.when(valijaVow, function(valija) {
  Q.when(powerboxVow, function(powerbox) {
    var imports = ___.copy(___.sharedImports);
    imports.Q = Q;
    imports.attachWindow = ___.markFuncFreeze(attachWindow);
    imports.setTimeout = ___.markFuncFreeze(tameSetTimeout);
    imports.valija = valija;
    powerbox.CALL___(imports);
  });
});
