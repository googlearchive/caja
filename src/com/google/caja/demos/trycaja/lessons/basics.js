var mypages = mypages || [];
mypages.push({
      lesson: 1,
      title: 'Basics; numbers, strings, etc.',
      guide: '<h3>'
          + rmsg(['Learning By Numbers', 'Music is Math', 'Back to Basics'])
          + '</h3>'

          + "<p>Caja is an html, css and javascript compiler.  The javascript"
          + " that Caja accepts is a lot like regular javascript that you might"
          + " already be familiar with.</p>"
          + "<p>If you are, feel free to skip to <code>lesson2</code>.</p>"
          + "<p>Otherwise lets get started.  To kick off let's try some maths"
          + " out. Up there you can type in JavaScript expressions. For example, "
          + " try this out: <code>5 + 7</code></p>"
    }, {
      guide: function (result) {
        if (!result) result = {
          expr: '5+7',
          result: 12
        };
        var complied = result.expr.replace(/ /g, '') == "5+7";
        var who = complied ? 'we' : 'you';
        return '<h3>'
            + rmsg(['Your first Caja expression', "First Time's a Charm"])
            + '</h3>'
            + '<p>Well done, you typed it perfect! You got back the number'
            + ' <code>' + result.result + '</code>. Just what ' + who
            + ' wanted. '
            + "</p><p>Let's try something completely different."
            + " Type in your name like this:" + ' <code>"chris"</code></p>'
      },
      trigger: function (result) {
        return result.result === 12;
      }
    },
    {
      guide: function (result) {
        if (!result) result = {
          expr: '"chris"',
          result: "\"chris\""
        };
        var n = unString(result.result);
        if (n) n = ", " + n;
        n += "!";
        return '<h3>'
            + rmsg(['Types of values', "What's in a name?"]) + '</h3>'
            + '<p>Hi there' + htmlEncode(n)
            + (n != "!" ? " That's a pretty name. Honest." : "")
            + " You're getting the hang of this! </p>"
            + "<p>Each time, you're getting back the value of the expression."
            + "So far, just a number and a list of characters.</p>"
            + "<p>You can have lists of other stuff, too. Let's see your "
            + " lottery numbers: <code>[42,13,22]</code></p>"
      },
      trigger: function (result) {
        return "string" === typeof result.result;
      }
    },
    {
      guide: function (result) {
        return '<h3>' + rmsg(["Verible variety of various variables"]) + '</h3>'
            + "<p>JavaScript is a dynamically typed language.  You can declare</p>"
            + "variables with the keyword <code>var</code></p>"
            + "<p>For example:<ul>"
            + "<li><code>var x = 1;</code>"
            + "<li><code>var y = 'This is a string';</code>"
            + "<li><code>var z = \"This is also a string\";</code>"
            + "</ul></p>"
      },
      trigger: function (result) {
        return true;
      }
    },
    {
      guide: function (result) {
        return '<h3>' + rmsg(["Verible variety of various variables"]) + '</h3>'
            + "<p>Caja and modern versions of JavaScript help you by warning"
            + " you if you use a variable without declaring it.  This helps"
            + " protect you from creating hard to find bugs and making"
            + " accidental errors like typos.</p>"
            + "<p>What happens if you use or assign to a variable that you have"
            + " not yet declared?  For example:<ul>"
            + "<li><code>myMisspeltCta = 1</code> or"
            + "<li><code>var y = myMisspeltDgo</code>"
            + "</ul></p>"
            + "<p>Caja warns you with a runtime error.</p>"
       },
      trigger: function (result) {
        return true;
      }
    },
    {
      guide: function (result) {
        return '<h3>' + rmsg(["Assorted arrays"]) + '</h3>'
            + "<p>In addition to scalar values, JavaScript has builtin support"
            + " for collections of objects.  You've already seen arrays:<ul>"
            + " <li><code>var myArray = [];</code>"
            + " <li><code>var myNewArray = [1, 2, 3];</code>"
            + "</ul></p>"
            + "<p>An array is just a sequence of JavaScript objects.</p>"
      },
      trigger: function (result) {
        return result.result instanceof Array;
      }
    },
    {
      guide: function (result) {
        return '<h3>' + rmsg(["Object literals are literally objects"]) + '</h3>'
            + "<p>What about unsequenced collections of objects?  JavaScript"
            + " has builtin support for 'object literals' which are just a map from"
            + " strings to values:<ul>"
            + "<li><code>var capitalsMap = {"
            + "\"fiji\" : \"suva\", \"new zealand\" : \"wellington\" };</code>"
            + "</ul></p>"
            + "<p>Once you've defined such a map, you can access the value of"
            + " a property using the keys either with the dot (<code>.</code>)"
            + " notation like this: <ul>"
            + "<li><code>capitalsMap.fiji;</code></li>"
            + "</ul>or using brackets (<code>[]</code>) like this:<ul>"
            + " <li><code>capitalsMap['new zealand'];</code></li>"
            + "</ul>";
      },
      trigger: function (result) {
        alert(result);
        return "number" === typeof result;
      }
    },
    {
      guide: function (result) {
        if (!result) result = {
          result: "[42,13,22]"
        };
        return '<h3>' + rmsg(["Lesson 1 done already!"]) + '</h3>'
            + "<p>Great, you made a list of numbers! If you win we'll split"
            + " the winnings, right?</p>"
            + "<p>Let's see what you've learned so far:</p>"
            + "<ol>"
            + "<li>How to write math expressions"
            + "<li>How to create lists of things"
            + "<li>How to use object literals"
            + "</ol>"
            + "<p>You can do stuff with lists. Maybe you want the lottery "
            + "numbers sorted in the right order, try this: "
            + "<code>[42,13,22].sort()</code></p>"
            + "<p>You're now ready to move on to <code>lesson2</code>!"
      },
      trigger: function (result) {
        return true;
      }
    });
