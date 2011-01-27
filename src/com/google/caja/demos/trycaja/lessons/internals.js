var mypages = mypages || [];
mypages.push({
    lesson: 4,
    title: 'Internals',
    guide: function (result) {
      if (!result) result = {
        result: "[13,23,30]"
      };
      return '<h3>' 
          + rmsg(["Internal machinations of Caja"]) + '</h3>' 
          + "<p>How do the internals of Caja work?" 
          + " Caja rewrites functions to make them safe.  To see what the"
          + " rewritten code looks like, type <code>debug</code> to toggle"
          + " debugging.<br />"
          + " Then you can see what <code>var x = [1,2,3].slice()</code> gets"
          + " rewritten to."
    },
    trigger: function (result) {
      return result.result instanceof Array;
    }
  }
);
