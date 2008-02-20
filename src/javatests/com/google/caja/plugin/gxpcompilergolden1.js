function aSquareMeal(beer, mayonnaise, cheetos) {
  var out___ = [];
  out___.push('<div id=\"pre-b\" bgcolor=\"');
  if (isTuesday()) {
    out___.push('red');
  } else {
    out___.push('white');
  }
  out___.push('\">');
  if (beer) {
    out___.push('<h1><a name=\"pre-beer\">beer</a></h1>', ___OUTERS___.plugin_html___(beer), 'bottles of beer');
  } else {
    out___.push('<div>Time for a beer run</div>');
    if (mayonnaise >= 50) {
      out___.push('<h1 class=\"pre-smooth pre-eggy\">mayonnaise</h1>', ___OUTERS___.plugin_html___(mayonnaise / 50 | 0), ' kilolitres of mayo');
    } else {
      out___.push('<div>Scraping the bottom of the barrel</div>');
      if (!cheetos.isEmpty()) {
        out___.push('<h1 class=\"', ___OUTERS___.plugin_prefix___(cheetos.flavor()), '\">Cheetos</h1>');
        var c2___ = cheetos.cheesyGoodness();
        if (c2___) {
          for (var c3___ in c2___) {
            if (!___.canEnumPub(c2___, c3___))
              continue;
            var cheeto = c2___[c3___];
            out___.push('<p>', ___OUTERS___.plugin_html___(cheeto.munch()), '</p>');
          }
        }
      } else {
        out___.push('<h1 onclick=\"return plugin_dispatchEvent___(this, event || window.event, ', ___.getId(___OUTERS___), ', \'c4___\');\">PANIC</h1>');
      }
    }
  }
  out___.push('Empty <p></p>\n\n  <form name=\"pre-hello\" onsubmit=\"return false\">\n    <input type=\"text\" name=\"widgy\">\n    <textarea id=\"pre-hellota\" name=\"howAreYouToday\">\n      How&#39;s it going?\n    </textarea>\n    <input type=\"submit\" value=\"whack\" id=\"');
  var c5___ = [];
  if (saidHello) {
    c5___.push('reparte');
  } else {
    c5___.push('salutations');
  }
  out___.push(___OUTERS___.plugin_htmlAttr___(___OUTERS___.plugin_prefix___(c5___.join(''))), '\">\n  </form>\n  <a href=\"/testplugin/help.html\" target=\"_new\">Help</a>\n\n</div>');
  return ___OUTERS___.plugin_blessHtml___(out___.join(''));
}
function c4___(thisNode___, event) {
  panic();
}
