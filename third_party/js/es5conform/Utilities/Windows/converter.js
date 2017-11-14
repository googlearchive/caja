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

// Process every file in the TestCases directory hierarchy, performing a search and replace
// The text to search for is in searchText.txt, the text to substitute is replaceText

(function () {
   var fso = new ActiveXObject("Scripting.FileSystemObject");

   return  {

   process: function(path) {
      if (fso.FolderExists(path)) {
         return this.processDirectory(path);
      }
      else {
         return this.ProcessFile(path);
      }
   },

   processDirectory: function (path) {
     var folder = fso.GetFolder(path);
     if (folder.attributes&2) return; //don't process invisible folders
     var subfolderEnumerator = new Enumerator(folder.SubFolders);
     var filesEnumerator     = new Enumerator(folder.Files);

      for (;!filesEnumerator.atEnd();filesEnumerator.moveNext()) {
       this.processFile(filesEnumerator.item().Path);
       }


     for (;!subfolderEnumerator.atEnd();subfolderEnumerator.moveNext()) {
       this.processDirectory(subfolderEnumerator.item().Path);
       }
     },
 
   processFile:  function (path) {
      var start,end=-1;
      if (path.slice(-3).toLowerCase() !== '.js') return;
//    WScript.StdOut.WriteLine("Processing: "+path);
      var file = fso.GetFile(path);
      var stream = file.OpenAsTextStream(1,0);
      var content = stream.ReadAll();
      var testId = file.Name.slice(0,file.Name.length-3)
      stream.Close();
      
        var bckslslshRE = /\\/g;
        var tcPath = path.slice(path.indexOf('TestCases')).replace(bckslslshRE,'/');
      
      if (content.match(/sth_addTest\s*\(\s*testName\s*,\s*testcase/) ===null) {
         WScript.StdOut.WriteLine("Skiping: "+path+" no sth_addTest call");
         return}
      var descMatch = content.match(/(var testName\s*=\s*)('[^']*')(\s*;\s*)/);
      if (descMatch ===null) descMatch = content.match(/(var testName\s*=\s*)("[^"]*")(\s*;\s*)/);
      if (descMatch ===null) descMatch = content.match(/(var testName\s*=\s*)('[^']*')\r?\n/);
      if (descMatch ===null) descMatch = content.match(/(var testName\s*=\s*)("[^"]*")\r?\n/);
      if (descMatch === null) {
         WScript.StdOut.WriteLine("Skiping: "+path+" no testName definition");
         return}
      var description = descMatch[2];
      var headerEnd = descMatch.index;
      var midStart = content.indexOf('\n',headerEnd)+1;
         
      start = content.indexOf('function testcase()');
      var midEnd=start;
      if (start !== -1) {
         end=content.indexOf('\n}\r',start);
         }
      if (start < 0 || end < start) {
         WScript.StdOut.WriteLine("Skiping: "+path+" no testcase definition ");
         return}
      var testFunc = content.slice(start,end+1)+' }';
      
      start = content.indexOf('function prereq()');
      if (start !== -1) {
         end=content.indexOf('\n}\r',start);
         }
      if (start >= 0 && end < start) {
         WScript.StdOut.WriteLine("Skiping: "+path+" malformed prereq definition ");
         return}
      if (start >= 0) {
         var prereqFunc = content.slice(start,end+1)+' }';
         }
      
      
     stream = file.OpenAsTextStream(2,0);
     stream.WriteLine(content.slice(0,headerEnd));
     if (midEnd-midStart>4) {
        WScript.StdOut.WriteLine("Processing: "+path+" with global declarations");
        stream.Write(content.slice(midStart,midEnd));
        stream.WriteLine();
        }
     stream.WriteLine('ES5Harness.registerTest( {');
     stream.WriteLine('id: "'+testId+'",');
     stream.WriteLine();
     stream.WriteLine('path: "'+tcPath+'",');
     stream.WriteLine();
     stream.WriteLine('description: '+description+',');
     stream.WriteLine();
     stream.Write('test: '+testFunc);
     if (prereqFunc) {
        stream.WriteLine(',');
        stream.WriteLine();
        stream.Write('precondition: '+prereqFunc);
        }
     stream.WriteLine();
     stream.WriteLine('});');
     stream.Close();
         
      }
   }
})().process(".\\..\\..\\TestCases");

