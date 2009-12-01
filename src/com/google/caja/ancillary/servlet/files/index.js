// Copyright (C) 2009 Google Inc.
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

/**
 * Supporting JS for index.quasi.html and IndexPage.java.
 *
 * @requires console document filesDir
 * @overrides window
 * @provides
 */

(function () {
var toolsForm = document.forms['tools-form'];

/** On submit, redirect to the appropriate handler. */
toolsForm.onsubmit = function () {
  setTimeout(doSubmit, 0);
  return false;
};

function doSubmit() {
  // Don't submit the original since we don't want to redirect.
  // Instead, create a dummy form and submit it instead.
  var els = toolsForm.elements;
  var verb = els.verb.value;
  var toSubmit = document.createElement('form');
  toSubmit.action = verb;
  toSubmit.method = 'POST';
  var inlineCode = window.frames.inlineCode;
  if (!inlineCode) {
    inlineCode = document.createElement('iframe');
    inlineCode.name = inlineCode.id = 'inlineCode';
    document.body.appendChild(inlineCode);
  }
  toSubmit.target = inlineCode.name;

  var NO_SEND = {};
  var dontSend = ({ lint: { verb: NO_SEND },
                    echo: { verb: NO_SEND },
                    doc: { verb: NO_SEND, userAgent: NO_SEND,
                           lint: NO_SEND, opt: NO_SEND } });
  for (var i = 0, n = els.length; i < n; ++i) {
    var el = els[i];
    if (!el.name || dontSend[verb][el.name] === NO_SEND
        || (el.tagName === 'INPUT' && !el.checked
            && (el.type === 'checkbox' || el.type === 'radio'))) {
      continue;
    }
    var input = document.createElement('input');
    input.name = el.name;
    input.value = el.value;
    input.type = 'hidden';
    toSubmit.appendChild(input);
  }

  document.body.appendChild(toSubmit);
  setTimeout(function () { toSubmit.parentNode.removeChild(toSubmit); }, 0);
  toSubmit.submit();
}

/** Invokes action on each element with the given name under container. */
function foreachDesc(container, elName, action) {
  var matches = container.getElementsByTagName(elName);
  for (var i = 0, n = matches.length; i < n; ++i) {
    action(matches[i]);
  }
}

/** The index of the caret in the given input element. */
function getCaret(el) { 
  // This code courtesy CMS from
  // http://stackoverflow.com/questions/263743/how-to-get-cursor-position-in-textarea
  if (el.selectionStart) { 
    return el.selectionStart; 
  } else if (document.selection) { 
    el.focus(); 

    var r = document.selection.createRange(); 
    if (r == null) { 
      return 0; 
    } 

    var re = el.createTextRange(), 
        rc = re.duplicate(); 
    re.moveToBookmark(r.getBookmark()); 
    rc.setEndPoint('EndToStart', re); 

    return rc.text.length; 
  }  
  return 0; 
}

/** Attaches listeners to the given source code textarea. */
function installSourceHandler(ta) {
  var timeoutId = null;
  function checkSource() {
    timeoutId = null;
    var typeSelect = ta.parentNode.parentNode.getElementsByTagName('select')[0];
    if (typeSelect.name !== 'it') { 
      console.error('TAG STRUCTURE VIOLATED');
      return;
    }
    var code = ta.value;

    // If the user deletes all the code, reset the input type.
    if (/^\s*$/.test(code)) { typeSelect.value = ''; }

    // Count the lines so we can resize the textarea to include all
    // the input but not the whole thing.  Cap at 500 lines.
    var lines = code.split(/\r\n?|\n/, 500);
    var nLines = 0;
    for (var i = 0, n = lines.length; i < n; ++i) {
      nLines += Math.ceil((lines[i].length + 1) / 80);
    }

    var linesNeeded = Math.min(500, Math.max(6, nLines + 2));
    ta.style.height = ((linesNeeded * 2.3) | 0) + 'ex';

    // To the right of the input, display the cursor position
    // so the user can correlate file positions in error messages
    // with lines in the source.
    var cursorPos = ta.parentNode.nextSibling;
    while (cursorPos && !/\bcursor-pos\b/.test(cursorPos.className)) {
      cursorPos = cursorPos.nextSibling;
    }
    if (cursorPos) {
      var caret = getCaret(ta);
      var lineNo = 1;
      var lineStart = 0;
      for (var i = 0; i < caret; ++i) {
        var cc = code.charCodeAt(i);
        if (cc === 0xa || cc === 0xd) {
          ++lineNo;
          if (cc === 0xd && 0xa === code.charCodeAt(i + 1)) { ++i; }
          lineStart = i + 1;
        }
      }
      cursorPos.innerHTML = 'Ln ' + lineNo + '+' + (caret - lineStart + 1);
    } else {
      console.error('Failed to find cursor pos');
    }
  }
  ta.onkeypress = function onSourceChange() {
    if (timeoutId !== null) { return; }
    timeoutId = setTimeout(function () { checkSource(); }, 100);
  };
  return checkSource;
}

/**
 * Called from example links to update the form inputs.
 * @param {Object} opts a mapping of input names to values.
 */
this.setOptions = function (opts) {
  for (var name in opts) {
    if (!Object.prototype.hasOwnProperty.call(opts, name)) { continue; }
    toolsForm.elements[name].value = '' + opts[name];
  }
};

(function () {
  var inputs = document.getElementById('inputs');
  /** Create a new input field by cloning an existing one. */
  function addInput() {
    var template = inputs.getElementsByTagName('li')[0];
    var newInput = template.cloneNode(true);
    var sourceTextArea = null;
    foreachDesc(newInput, 'textarea', function (ta) {
      ta.value = '';
      installSourceHandler(ta);
      sourceTextArea = ta;
    });
    foreachDesc(newInput, 'input', function (input) { input.value = ''; });
    foreachDesc(newInput, 'select', function (sel) { sel.selectedIndex = 0; });

    template.parentNode.appendChild(newInput);
    sourceTextArea.focus();
  }
  var nUploads = 0;
  /**
   * Create a frame to upload a file by submitting an
   * {@code <input type=file>}.
   */
  function makeUploadFrame() {
    var item = document.createElement('li');
    var uploadFrame = document.createElement('iframe');
    uploadFrame.id = uploadFrame.name = 'upload-' + ++nUploads;
    uploadFrame.className = 'upload';
    item.appendChild(uploadFrame);
    inputs.appendChild(item);
    return uploadFrame;
  }
  function upload() {
    makeUploadFrame().src = filesDir + '/upload.html';
  }
  /** Load an example from a URL in an example link. */
  this.exampleFromUrl = function (url) {
    var frame = makeUploadFrame();
    var form = frame.contentWindow.document.createElement('form');
    form.action = 'upload';
    form.method = 'POST';
    form.ownerDocument.body.appendChild(form);
    var input = form.ownerDocument.createElement('input');
    input.type = 'hidden';
    input.name = 'url';
    input.value = '' + url;
    form.appendChild(input);
    form.submit();
  };
  function makeButton(className, text, title, action) {
    var newButton = document.createElement('button');
    newButton.type = 'button';
    newButton.className = className;
    newButton.appendChild(document.createTextNode(text));
    newButton.onclick = function () {
      setTimeout(action, 0);
      return false;  // Do not submit the form
    };
    if (title) { newButton.setAttribute('title', title); }
    return newButton;
  }
  inputs.parentNode.insertBefore(
      makeButton('new-input-button', 'Add Input \uFF0B', null, addInput),
      inputs.nextSibling);
  inputs.parentNode.insertBefore(
      makeButton('upload-button', 'Upload Source \u21E7', null, upload),
      inputs.nextSibling);

  var moreOptions = document.getElementById('more-options');
  moreOptions.parentNode.insertBefore(
      makeButton('more-options-button', 'More Options', null, toggleOptions),
      moreOptions);
  moreOptions.style.display = 'none';
  function toggleOptions() {
    moreOptions.style.display = moreOptions.style.display ? '' : 'none';
  }

  function hiddenInput(name, value) {
    var input = document.createElement('input');
    input.name = name;
    input.value = value;
    input.type = 'hidden';
    return input;
  }

  /**
   * Called from UploadPage.java to add an input to the form with the uploaded
   * file content.
   */
  this.uploaded = function (uploads, frameId) {
    for (var i = 0, n = uploads.length; i < n; ++i) {
      var upload = uploads[i];
      if (!upload.i) { continue; }
      var item = document.createElement('li');

      if (upload.ip) { item.appendChild(hiddenInput('ip', upload.ip)); }
      if (upload.it) { item.appendChild(hiddenInput('it', upload.it)); }
      item.appendChild(hiddenInput('i', upload.i));

      var uploadedFile = document.createElement('div');
      uploadedFile.className = 'uploadedFile';
      uploadedFile.appendChild(removeButton(item));
      var fileInfo = document.createElement('div');
      fileInfo.className = 'file';
      fileInfo.appendChild(document.createTextNode(
          upload.ip + ' (' + humanReadableSize(upload.i.length) + ')'));
      uploadedFile.appendChild(fileInfo);
      item.appendChild(uploadedFile);

      inputs.appendChild(item);
    }
    var uploadFrame = document.getElementById(frameId);
    uploadFrame.parentNode.removeChild(uploadFrame);
  };

  function removeButton(item) {
    return makeButton(
        'del-input', '\uff38', 'Discard uploaded file', function () {
        item.onclick = null;
        item.parentNode.removeChild(item);
      });
  }

  /** A human readable string like 10B, 4kB, or 16MB. */
  function humanReadableSize(nBytes) {
    if (nBytes < 1024) { return nBytes + 'B'; }
    if (nBytes < (1 << 20)) { return (nBytes / 1024).toFixed(2) + 'kB'; }
    return (nBytes / (1 << 20)).toFixed(2) + 'MB';
  }

  /**
   * Called by example code to replace the current inputs.
   * @param {string} newSource source code.
   * @param {string} the mime-type of newSource.  Should be one of the
   *    canonical ones from ContentType.java.
   */
  this.replaceSource = function (newSource, newType) {
    var sources = inputs.getElementsByTagName('li');
    for (var i = sources.length; --i >= 1;) {
      sources[i].parentNode.removeChild(sources[i]);
    }
    toolsForm.elements.i.value = newSource;
    toolsForm.elements.it.value = newType;
    toolsForm.elements.i.onkeypress();
  };
})();

(function () {
  var lastTa = null;
  foreachDesc(toolsForm, 'textarea', function (ta) {
    lastTa = ta;
    installSourceHandler(ta)();
  });

  // Focus last input
  if (lastTa) {
    lastTa.select();
    lastTa.focus();
  }
})();

(function () {
  (toolsForm.elements.verb.onchange = function () {
    var verb = toolsForm.elements.verb.value;
    if (toolsForm.elements.minLevel.value) {
      // When the user changes the action, switch to the appropriate
      // message level.
      toolsForm.elements.minLevel.value = verb === 'lint' ? 'LINT' : 'ERROR';
    }
    toolsForm.elements.userAgent.disabled = verb === 'doc';
  })();
})();
})();