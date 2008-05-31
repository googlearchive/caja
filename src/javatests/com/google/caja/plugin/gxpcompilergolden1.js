{
  function aSquareMeal(beer, mayonnaise, cheetos) {
    var out___ = [ ];
    c1___.call(IMPORTS___, out___, beer, mayonnaise, cheetos);
    return IMPORTS___.blessHtml___(out___.join(''));
  }
  function c1___(out___, beer, mayonnaise, cheetos) {
    out___.push('<div id=\"b-', IMPORTS___.getIdClass___(), '\" bgcolor=\"');
    if (isTuesday()) {
      out___.push('red');
    } else {
      out___.push('white');
    }
    out___.push('\">');
    if (beer) {
      out___.push('<h1><a name=\"beer-', IMPORTS___.getIdClass___(), '\">beer</a></h1>', IMPORTS___.html___(beer), 'bottles of beer');
    } else {
      out___.push('<div>Time for a beer run</div>');
      if (mayonnaise >= 50) {
        out___.push('<h1 class=\"smooth eggy\">mayonnaise</h1>', IMPORTS___.html___(mayonnaise / 50 | 0), ' kilolitres of mayo');
      } else {
        out___.push('<div>Scraping the bottom of the barrel</div>');
        if (!cheetos.isEmpty()) {
          out___.push('<h1 class=\"', IMPORTS___.ident___(cheetos.flavor()), '\">Cheetos</h1>');
          var c2___ = cheetos.cheesyGoodness();
          if (c2___) {
            for (var c3___ in c2___) {
              if (___.canEnumPub(c2___, c3___)) {
                var cheeto = c2___[ c3___ ];
                out___.push('<p>', IMPORTS___.html___(cheeto.munch()), '</p>');
              }
            }
          }
        } else {
          out___.push('<h1 onclick=\"', 'return plugin_dispatchEvent___(this, event || window.event, ' + ___.getId(IMPORTS___) + ', \'c4___\')', '\">PANIC</h1>');
        }
      }
    }
    out___.push('Empty <p></p>\n\n  <form name=\"hello-', IMPORTS___.getIdClass___(), '\" onsubmit=\"return false\">\n    <input type=\"text\" name=\"widgy\">\n    <textarea id=\"hellota-', IMPORTS___.getIdClass___(), '\" name=\"howAreYouToday\">\n      How&#39;s it going?\n    </textarea>\n    <input type=\"submit\" value=\"whack\" id=\"');
    var c5___ = [ ];
    if (saidHello) {
      c5___.push('reparte');
    } else {
      c5___.push('salutations');
    }
    out___.push(IMPORTS___.htmlAttr___(IMPORTS___.suffix___(c5___.join(''))), '\">\n  </form>\n  <a href=\"/testplugin/help.html\" target=\"_new\">Help</a>\n\n</div>');
  }
  IMPORTS___.c4___ = ___.simpleFunc(function (thisNode___, event) {
                                      panic();
                                    });
}
