/**
 * @fileoverview
 * Support JS for LintPage.java.
 *
 * @author mikesamuel@gmail.com
 */

/**
 * Called by the select code link in the floating menu to the top right of the
 * lint page output.
 */
var selectCode = (function () {
  function selectRange(node) {
    // The below is a blending of two snippets from Simon Scarfe as seen at
    // http://stackoverflow.com/questions/1173194/select-all-div-text-with-single-mouse-click
    if (document.selection) {
      document.selection.empty();
      var range = document.body.createTextRange();
      range.moveToElementText(node);
      range.select();
    } else if (window.getSelection) {
      window.getSelection().removeAllRanges();
      var range = document.createRange();
      range.selectNode(node);
      window.getSelection().addRange(range);
    }
  }

  return function () {
    window.location = '#src';  // scroll into view
    selectRange(document.getElementById('src'));
  };
})();
