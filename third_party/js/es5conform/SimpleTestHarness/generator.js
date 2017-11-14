/// Copyright (c) 2009 Microsoft Corporation 
/// 
/// Redistribution and use in source and binary forms, with or without modification, are permitted provided
/// that the following conditions are met: 
///    * Redistributions of source code must retain the above copyright notice, this list of conditions and
///      the following disclaimer. 
///    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and 
///      the following disclaimer in the documentation and/or other materials provided with the distribution.  
///    * Neither the name of Microsoft nor the names of its contributors may be used to
///      endorse or promote products derived from this software without specific prior written permission.
/// 
/// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
/// IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
/// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
/// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
/// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
/// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
/// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
/// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 


// ===================================================================
// TestCase is the generic base class for a TestGroup and a UnitTest.
//
// A UnitTest is an actual test (a .js file) that gets run.
//
// Zero or more UnitTest are grouped together in a folder; each folder
// is represented as a TestGroup.
// ===================================================================
TestCase.Build = function (path) {
  if (fso.FolderExists(path)) {
    return new TestGroup(path);
  }
  else {
    return new UnitTest(path);
  }
}

function TestCase() {}
// ===================================================================


// ===================================================================
// TestGroup represents a folder containing zero or more TestCases
// ===================================================================
TestGroup.prototype = new TestCase();

TestGroup.prototype.Emit = function (writer) {
  for (var i = 0; i < this.tcList.length; i++) {
    this.tcList[i].Emit(writer)
  }
}

TestGroup.prototype.add = function (tc) {
  this.tcList.push(tc);
}

TestGroup.prototype.BuildTestCase = function (path) {
  var folder = fso.GetFolder(path);
  var subfolderEnumerator = new Enumerator(folder.SubFolders);
  var filesEnumerator     = new Enumerator(folder.Files);

  var flag = false;
  for (;!filesEnumerator.atEnd();filesEnumerator.moveNext()) {
    this.add(new UnitTest(filesEnumerator.item().Path));
    flag = true;
  }

  if (flag === true) {
    this.add(new UnitTest(null));
    flag = false;
  }


  for (;!subfolderEnumerator.atEnd();subfolderEnumerator.moveNext()) {
     var subfolder = subfolderEnumerator.item();
     if ((subfolder.attributes&2) ==0) //don't process invisible folders
       this.add(new TestGroup(subfolder.Path));
  }
}

function TestGroup(folderPath) {
  this.path   = folderPath;
  this.tcList = [];

  this.BuildTestCase(folderPath);
}
// ===================================================================


// ===================================================================
// UnitTest represents an actual test that can be run.
// A UnitTest maps to a single .js file.
// ===================================================================
UnitTest.prototype = new TestCase();

UnitTest.prototype.Emit = function (writer) {
  writer.writePath(this.path);
}

function UnitTest(filePath) {
  this.path = filePath;
}
// ===================================================================


// ===================================================================
// HTMLWriter writes HTML.
// ===================================================================
HTMLWriter.prototype.prologue = function () {
  WScript.echo('<html>');
  WScript.echo('<head>');
  WScript.echo('<meta http-equiv="X-UA-Compatible" content="IE=8"/>');
  WScript.echo('<script type=\"text/javascript\" src=\"'+(topLevel?'SimpleTestHarness/':'')+'sth.js\"></script>');
  WScript.echo('<script type=\"text/javascript\" src=\"'+(topLevel?'SimpleTestHarness/':'')+'sth_browser.js\"></script>');
  WScript.echo('<script>');
  WScript.echo('var ES5Harness = activeSth;');
  WScript.echo('</script>');
  WScript.echo('</head>');
  WScript.echo('<body>');
  WScript.echo('<script>');
  WScript.echo('var aryTestCasePaths = [');
}

HTMLWriter.prototype.writePath = function (s) {
  if (s===null) WScript.echo('null,');
  else {
     var base = s.indexOf(this.testRoot+"\\");
     if (base >= 0) {s = s.slice(base); prefix=this.testPrefix}
     else prefix='';
     var re = /\\/g;
     var path = '"' + prefix + s.replace(re, '/') + '"';
     testPaths.push(path);
     WScript.echo(path + ",");
     }
}

HTMLWriter.prototype.epilogue = function (static) {
  WScript.echo('];')
  if (!static) WScript.echo('sth_loadtests(aryTestCasePaths,'+(topLevel ? '"SimpleTestHarness"' : '"."')+');');
  WScript.echo('</script>');
  if (static) {
     for (var i=0; i<testPaths.length; i++) 
        WScript.echo('<script type=\"text/javascript\" src='+testPaths[i]+'></script>');
     WScript.echo('<script>');
     WScript.echo('ES5Harness.startTesting();');
     WScript.echo('</script>');
     }
  WScript.echo('</body>')
  WScript.echo('</html>');
}

function HTMLWriter(testRoot, testPrefix) {
   this.testRoot=testRoot;
   this.testPrefix=testPrefix;
   }
// ======================================
var staticTestLoad = true;
var topLevel = false;
(function(args) {
    for (var i = 0; i < args.length; i++)  {
       if (args(i) === '/toplevel') topLevel = true;
       else if (args(i) === '/dynamic')staticTestLoad = false
       }
    }) (WScript.arguments);
    
var startingFolderPath = "..\\TestCases";
var fso = new ActiveXObject("Scripting.FileSystemObject");

var tc = TestCase.Build(startingFolderPath);
var testPaths = [];

var w = new HTMLWriter("TestCases",topLevel ? "" : "../");
w.prologue();
tc.Emit(w);
w.epilogue(staticTestLoad);