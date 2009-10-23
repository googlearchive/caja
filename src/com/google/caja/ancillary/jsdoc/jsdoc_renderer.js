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
 * @fileoverview
 * Generate HTML files for each input file and API element container.
 * Uses the JSDoc HTML formatter and runs under Rhino.
 *
 * <p>
 * The file structure produced looks like:
 * <pre>
 * index.html    (a frameset)
 * toc.html      (<u>t</u>able <u>o</u>f <u>c</u>ontents)
 * package.html  (directory info)
 * </pre>
 *
 * <p>
 * Expects that {@code this.fileSystem} points to an instance of the Java class
 * {@code FileSystem}, that {@code this.htmlBuilder} points to an instance
 * of the Java class {@code HtmlQuasiBuilder}, that {@code this.docTitle}
 * is a string pointing to a plain text title string, and that
 * {@code this.htmlDir} is the directory that should contain the output.
 *
 * @author mikesamuel@gmail.com
 */

// Invoked by HtmlRenderer.java.
var renderHtml;
// A JSON object as produced by jsdoc.js.  Set by HtmlRenderer.java.
var jsdocJson;
// Provided for compatibility with jsdoc_html_formatter.js.
var linkToFile;
var linkToApiElement;
var linkToSource;
function getJsdocRoot() {
  return jsdocJson;
}

(function () {
  var STDERR = stderr;
  
  function outputFileForInputFile(path, prefix) {
    var isDir = false;
    var json = lookupFile(path);
    for (var k in json) {
      if (Object.hasOwnProperty.call(json, k) && !/^@/.test(k)) {
        isDir = true;
        break;
      }
    }
    var docHtmlFile;
    if (isDir) {
      return fileSystem.join(path, 'package.html');
    } else {
      var dirName = fileSystem.dirname(path);
      var baseName = prefix + fileSystem.basename(path) + '.html';
      return fileSystem.join(dirName, baseName);
    }
  }

  function outputFileForApiElement(path) {
    if (!path) { return 'globals.html'; }
    return 'api-' + path + '.html';
  }

  var relativeFileToRelativeUrl;
  if ('a/b' === fileSystem.join('a', 'b')) {
    relativeFileToRelativeUrl = function (path) { return path; };
  } else {
    relativeFileToRelativeUrl = (function () {
      var p = String(fileSystem.join('a', 'b'));
      p = p.substring(1, p.length - 1);
      p = new RegExp(p.replace(/[\s\S]/g, '\\$&'), 'g');
      return function (path) { return String(path).replace(p, '/'); };
    })();
  }
  
  function emitHtml(path, html) {
    var os = fileSystem.write(path);
    try {
      os.append('<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"'
               + ' "http://www.w3.org/TR/html4/strict.dtd">');
      os.append(htmlBuilder.toHtml(html));
    } finally {
      os.close();
    }
  }

  function emitSimpleFile(path, body) {
    emitHtml(
        path,
        htmlBuilder.substV(
            ''
            + '<html>\n'
            + '  <head>\n'
            + '    <meta http-equiv="Content-Type"'
            + '     content="text/html; charset=utf-8" />\n'
            + '    <title>@title</title>\n'
            + '</script>\n'
            + '  </head>\n'
            + '  <body>@body</body>\n'
            + '</html>',
            'title', docTitle,
            'body', body));
  }
  
  function emitCodeFile(path, body) {
    emitHtml(
        path,
        htmlBuilder.substV(
            ''
            + '<html>\n'
            + '  <head>\n'
            + '    <meta http-equiv="Content-Type"'
            + '     content="text/html; charset=utf-8" />\n'
            + '    <title>@title</title>\n'
            + '    <link rel=stylesheet href="@docRootPath/jsdoc.css" />\n'
            + '    <link rel=stylesheet href="@docRootPath/prettify.css" />\n'
            + '    <script type=text/javascript\n'
            + '     src="@docRootPath/prettify.js"></script>\n'
            + '  </head>\n'
            + '  <body onload=prettyPrint()>@body</body>\n'
            + '</html>',
            'title', docTitle,
            'docRootPath', urlToDocRoot,
            'body', body));
  }

  function applyRenderer(renderer, args) {
    var buf = [];
    var argsWithBuf = args.slice(0);
    argsWithBuf.push(buf);
    renderer.apply({}, argsWithBuf);
    return buf.join('');
  }

  /**
   * Called by HTML renderer to generate HTML files for each input file, and
   * each API element container.
   */
  this.renderHtml = function renderHtml(jsdocJson) {
    this.jsdocJson = jsdocJson;
    urlToDocRoot = '.';
    emitCodeFile(fileSystem.join(htmlDir, 'toc.html'),
             htmlBuilder.substV(
                 (''
                  + '<script type="text/javascript">\n'
                  + 'document.documentElement.className += " scriptEnabled";\n'
                  // Handler for clicks on API element tree.
                  + 'function expand(n) {\n'
                  + '  var p = n.parentNode;\n'
                  + '  var cn = p.className;\n'
                  + '  var wo = cn.replace(/\\bexpanded\\b/g, "");\n'
                  + '  p.className = (cn === wo) ? cn + " expanded" : wo;\n'
                  + '}'
                  + '</script>\n'
                  + '@toc\n'
                  + '<script type="text/javascript"'
                  + ' src="@docRootPath/searchbox.js"></script>\n'
                  + '<script>'
                  + 'attachSearchBox(document.getElementById("compactindex"));'
                  + '</script>'),
                 'toc', htmlBuilder.toFragment(
                            applyRenderer(renderCompactIndex, [jsdocJson])),
                 'docRootPath', urlToDocRoot));
    emitSimpleFile(fileSystem.join(htmlDir, 'index.html'),
             htmlBuilder.toFragment(
                 '<frameset cols="20%,80%">'
               + '  <frame src="toc.html" />'
               + '  <frame name="jsdoc_main" src="package.html" />'
               + '</frameset>'));
    forEachFile(
        docItemHandler(
            function (path) { return outputFileForInputFile(path, 'file-'); },
            renderFile));
    forEachApiElement(
        docItemHandler(outputFileForApiElement, renderApiElement));

    function docItemHandler(docItemToOutputFile, renderer) {
      return function (docItemPath, json) {
        // Figure out the path of the file to write relative to htmlDir.
        var docHtmlFile = docItemToOutputFile(docItemPath);
        // Make sure there exists a directive to contain it.
        var docHtmlDir = fileSystem.dirname(docHtmlFile) || '';
        fileSystem.mkdir(fileSystem.join(htmlDir, docHtmlDir));

        // Make sure jsdoc_html_formatter produces links that resolve relative
        // to the the file we're writing.
        urlToDocRoot = relativeFileToRelativeUrl(String(docHtmlDir))
            .replace(/^\/+|\/+$/g, '')
            .replace(/\/{2,}/g, '/').replace(/[^\/]+/g, '..') || '.';

        // Actually write the file content.
        emitCodeFile(fileSystem.join(htmlDir, docHtmlFile),
                     htmlBuilder.toFragment(
                         applyRenderer(renderer, [docItemPath, json])));
      };
    }
  };

  /**
   * Set before rendering starts so that {@code linkTo{File,ApiElement}} can
   * produce URLs relative to the output file.
   */
  var urlToDocRoot;
  this.linkToFile = function (path) {
    return {
      href: ((urlToDocRoot ? urlToDocRoot + '/' : '')
             + relativeFileToRelativeUrl(outputFileForInputFile(path, 'file-'))),
      target: 'jsdoc_main'
    };
  };
  this.linkToApiElement = function (path) {
    return {
      href: ((urlToDocRoot ? urlToDocRoot + '/' : '')
             + relativeFileToRelativeUrl(outputFileForApiElement(path))),
      target: 'jsdoc_main'
    };
  };
  this.linkToSource = function (path, lineNumber) {
    // Dual of code in HtmlRenderer.numberLines().
    var lineNumberStr = '' + Math.abs(lineNumber);
    var sign = lineNumber < 0 ? '-' : '';
    while (lineNumberStr.length + sign.length < 4) {
      lineNumberStr = '0' + lineNumberStr;
    }
    lineNumberStr = sign + lineNumberStr;
    return {
      href: ((urlToDocRoot ? urlToDocRoot + '/' : '')
             + relativeFileToRelativeUrl(outputFileForInputFile(path, 'src-')))
             + '#line' + lineNumberStr,
      target: 'jsdoc_main'
    };
  };
})();
