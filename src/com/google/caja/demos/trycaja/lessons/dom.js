var mypages = mypages || [];
mypages.push({
    lesson: 3,
    title: 'DOM functions',
    guide: function (result) {
      if (!result) result = {
        result: "[13,23,30]"
      };
      return '<h3>' 
          + rmsg(["DOMita: Its spanish for a little dom"]) + '</h3>' 
          + "<p>Congratulations, you just used a <strong>function</strong>." 
          + " Now lets play with the DOM.  "
          + "First we'll need some space on the page to play with."

          + "Type <code>display</code> to show the part of the page that "
          + " cajoled code can modify (this should show up on the right "
          + " but currently floats to the bottom.)"
          + "<p>You can now try dom functions like <code>top.location.href</code>";
    },
    trigger: function (result) {
      return /top.location/.test(result.expr);
    }
  },
  {
    guide: function (result) {
      return '<h3>' 
          + rmsg(["DOMita: Faking out the world!"]) 
        + '</h3>' + "Notice that you got back " + result.result + " and not " + top.location + " which you might have expected."
        + "Caja replaces all global variables and functions with alternatives provided by the host page."
        + "<p>How about manipulating the DOM?  Caja provides a taming of the DOM called 'domita'.  It provides cajoled code "
        + " safe attenuated access to the DOM.  For example, try <code>document.createTextNode('foo').innerText</code>."
    },
    trigger: function (result) {
      return true
    }
  },
  {
    guide: function (result) {
      return '<h3>' 
          + rmsg(["DOMinatrix: Because some puns are painful!"]) 
        + '</h3>' + "How about adding DOM nodes?  Try <code>document.body.appendChild(document.createTextNode('hello domintator'))</code></p>"
    },
    trigger: function (result) {
      return true
    }
  }
);
