var mypages = mypages || [];
mypages.push({
      lesson: 1,
      title: 'Basics; numbers, strings, etc.',
      guide: '<h3>' 
          + rmsg(['Learning By Numbers', 'Music is Math', 'Back to Basics']) 
          + '</h3>' 

          + "Caja is an html, css and javascript compiler.  The javascript that "
          + "Caja accepts is a lot like regular javascript that you know and love."
          + "<p>So lets get started.  To kick off let's try some maths out. Up there you can" 
          + " type in Caja expressions. Try this out: <code>5 + 7</code></p>"
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
        if (!result) result = {
          result: "[42,13,22]"
        };
        return '<h3>' + rmsg(["Lesson 1 done already!"]) + '</h3>' 
            + "<p>Great, you made a list of numbers! If you win we'll split" 
            + " the winnings, right?</p>" 
            + "<p>Let's see what you've learned so far:</p>" 
            + "<ol>" 
            + "<li>How to write maths and lists of things.</li>" 
            + "</ol>" 
            + "<p>You can do stuff with lists. Maybe you want the lottery " 
            + "numbers sorted in the right order, try this: " 
            + "<code>[42,13,22].sort()</code></p>"
      },
      trigger: function (result) {
        return true;
      }
    });
