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
   var searchPath = fso.GetFolder(".").Path.replace(/\\/g, "\\\\") + "\\\\searchText.txt";
   var file = fso.GetFile(searchPath);
   var stream = file.OpenAsTextStream(1,0);
   var searchString = stream.ReadAll();
   stream.Close();

   var replacementPath = fso.GetFolder(".").Path.replace(/\\/g, "\\\\") + "\\\\replaceText.txt";
   file = fso.GetFile(replacementPath);
   stream = file.OpenAsTextStream(1,0);
   var replacementString = stream.ReadAll();
   stream.Close();


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
      WScript.StdOut.WriteLine("Processing: "+path);
      var file = fso.GetFile(path);
      var stream = file.OpenAsTextStream(1,0);
      var content = stream.ReadAll();
      stream.Close();
      var newContent = content.replace(searchString, replacementString);
      if (content !== newContent) {
         stream = file.OpenAsTextStream(2,0);
         stream.Write(newContent);
         stream.Close();
         }
      }
   }
})().process(".\\..\\..\\TestCases");

