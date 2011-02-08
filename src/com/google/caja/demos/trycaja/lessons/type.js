var ithHeuristic = 0;
var mypages = mypages || [];
mypages.push({
      lesson: 2,
      title: 'Types; Primitives and Objects.',
      guide: '<h3>' 
          + rmsg(['Types of types']) 
          + '</h3>' 
          + "<p>Every value in JavaScript has are type.  The type system in JavaScript "
          + "is dynamic. This means type constraints in the language are enforced "
          + "at runtime.  Any variable can hold any value.</p>"
          + "<code>var myVar = 1; myVar = 'string';</code>"
      }, {
      guide: function (result) {
        return '<h3>' 
            + rmsg(['Type on typeof!']) 
            + '</h3>' 

            + "<p>JavaScript values are either primitives or object types.  "
            + "Primitive types are values like numbers, strings, and booleans."
            + "You can use the 'typeof' operator to get the type of a value:</p>"
            + "<ul>"
            + "  <li><code>typeof 'hello'</code>"
            + "  <li><code>typeof 1</code>"
            + "  <li><code>typeof true</code>"
            + "</ul>"
      },
      trigger: function (result) {
        "string" === typeof result.result;
        var result = String(result.result);
        if ("string" === result ||
             "object" === result ||
             "function" === result ||
            "number" === result) {
          ithHeuristic++;
        };
        if (ithHeuristic >= 2) {
          ithHeuristic = 0;
          return true;
        } else {
          return false;
        }
      }
    },
    {
      guide: function (result) {
        return '<h3>' 
            + rmsg(['Types of objects']) + '</h3>'
            + "<p>On the other hand, object types are objects literals, user-defined "
            + "types, arrays, functions, dates and many others.</p>"
            + "<ul>"
            + "  <li><code>typeof function () {}</code>"
            + "  <li><code>typeof { 'foo' : 1 }</code>"
            + "  <li><code>typeof [1, 2, 3]</code>"
            + "</ul>"
      },
      trigger: function (result) {
        "string" === typeof result.result;
        var result = String(result.result);
        if ("string" === result ||
             "object" === result ||
             "function" === result ||
            "number" === result) {
          ithHeuristic++;
        };
        if (ithHeuristic >= 2) {
          ithHeuristic = 0;
          return true;
        } else {
          return false;
        }
      }
    },
    {
      guide: function (result) {
        return '<h3>' + rmsg(["Unusually typed objects!"]) + '</h3>' 
            + "<p>Great, now you can determine the type of values."
            + " What about special values like <code>undefined</code>"
            + " and <code>null</code>?</p>"
            + "<p>What does the following code print:</p?>"
            + "<ul><li><code>typeof undefined</code>?</li></ul>";
      },
      trigger: function (result) {
        return "undefined" === result.result;
      }
    },
    {
      guide: function (result) {
        return '<h3>' + rmsg(["Unusually typed objects!"]) + '</h3>' 
            + "<p>Yes!  <code>undefined</code> has a type called 'undefined' with "
            + "just one value.  Can you guess what the <code>typeof null</code> is?</p>";
      },
      trigger: function (result) {
        return /typeof.*null/.test(result.expr) && "object" === result.result;
      }
    },
    {
      guide: function (result) {
        return '<h3>' + rmsg(["The 'null' object!"]) + '</h3>' 
            + "<p>Woah!  Were you surprised?  In JavaScript, <code>null</code>"
            + " is just an object although a special one.  It is distinct from <code>undefined</code>"
            + " which signals the <i>absense</i> of an object.</p>"
      },
      trigger: function (result) {
        return /typeof.*null/.test(result.expr) && "object" === result.result;
      }
    }

);
