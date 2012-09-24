window.globalGuestTest = function globalGuestTest(expectTitle, expectBody) {
  var expectHTML = '<head>';
  if (expectTitle !== null) expectHTML += '<title>' + expectTitle + '</title>';
  expectHTML += '</head><body>' + expectBody + '</body>';
  assertEquals(expectHTML, document.documentElement.innerHTML);
  assertEquals('HTML', document.documentElement.tagName);
}
