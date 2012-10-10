window.globalGuestTest = function globalGuestTest(expectHTML) {
  assertEquals(expectHTML, canonInnerHtml(document.documentElement.innerHTML));
  assertEquals('HTML', document.documentElement.tagName);
}
