// Derived from Try Haskell 1.0.1
// Tue Feb 23 18:34:48 GMT 2010
//
// Copyright 2010 Chris Done. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//
//    1. Redistributions of source code must retain the above
//       copyright notice, this list of conditions and the following
//       disclaimer.

//    2. Redistributions in binary form must reproduce the above
//       copyright notice, this list of conditions and the following
//       disclaimer in the documentation and/or other materials
//       provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY CHRIS DONE ``AS IS'' AND ANY EXPRESS
// OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL CHRIS DONE OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
// OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
// BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
// USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
// DAMAGE.

// The views and conclusions contained in the software and
// documentation are those of the authors and should not be
// interpreted as representing official policies, either expressed or
// implied, of Chris Done.
//
// TESTED ON
//   Internet Explorer 6
//   Opera 10.01
//   Chromium 4.0.237.0 (Ubuntu build 31094)
//   Firefox 3.5.8

// Temporary fix
function opera(){ return navigator.userAgent.indexOf("Opera") == 0; }

function encodeHex(str){
    var result = "";
    for (var i=0; i<str.length; i++){
        result += "%" + pad(toHex(str.charCodeAt(i)&0xff),2,'0');
    }
    return result;
}

var handleJSON = function(a){ alert('Unassigned JSONP: ' + a); }

function pad(str, len, pad){
    var result = str;
    for (var i=str.length; i<len; i++){
        result = pad + result;
    }
    return result;
}

var digitArray = new Array('0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f');

function toHex(n){
    var result = ''
    var start = true;
    for (var i=32; i>0;){
        i-=4;
        var digit = (n>>i) & 0xf;
        if (!start || digit != 0){
            start = false;
            result += digitArray[digit];
        }
    }
    return (result==''?'0':result);
}

(function($){
    var tutorialGuide;
    // Page variables
    //
    var nemesis = 'chirs';
    var showTypes = false;
    var pages =
        [
            ////////////////////////////////////////////////////////////////////////
            // Lesson 1

            // Simple addition
            {lesson:1,
             title:'Basics; numbers, strings, etc.',
             guide:
             '<h3>' + rmsg(['Learning By Numbers','Music is Math','Back to Basics'])
             + '</h3>'
             + "<p>To kick off let's try some maths out. Up there you can"
             + " type in Caja expressions. Try this out: <code>5 + 7</code></p>"
            },
            {guide:function(result){
                if (!result) result = {expr:'5+7',result:12};
                var complied = result.expr.replace(/ /g,'')=="5+7";
                var who = complied? 'we' : 'you';
                return '<h3>' + rmsg(['Your first Caja expression',
                                      "First Time's a Charm"]) + '</h3>'
                    + '<p>Well done, you typed it perfect! You got back the number'+
                    ' <code>' + result.result + '</code>. Just what '+who+' wanted. '
                    + "</p><p>Let's try something completely different."+
                    " Type in your name like this:" +
                    ' <code>"chris"</code></p>'
            },
             trigger:function(result){
                 return result.type == "(Num t) => t" ||
                     result.type == "Integer" ||
                     result.type == "Int";
             }
            },
            // Strings & types
            {guide:function(result){
                if (!result) result = {expr:'"chris"',result:"\"chris\""};
                var n = unString(result.result); if (n) n = ", " +n;
                n += "!";
                return '<h3>' + rmsg(['Types of values',"What's in a name?"]) +
                    '</h3>'
                    + '<p>Hi there' + htmlEncode(n)
                    + (n!="!"? " That's a pretty name. Honest." : "")
                    + " You're getting the hang of this! </p>" +
                    "<p><strong>Note:</strong> You can chat to Haskell programmers while learning here, enter <code>chat</code> to start it."+
                    " You will join the official IRC channel of the Haskell community!</p>"
                    + "<p>Each time, you're getting back the value of the expression. So "+
                    "far, just a number and a list of characters.</p>" +
                    "<p>You can have lists of other stuff, too. Let's see your " +
                    " lottery numbers: <code>[42,13,22]</code></p>"
            },
             trigger:function(result){
                 return result.type == "[Char]"
                     || result.type == "String";
             }
            },
            // Overview of lesson 1
            {guide:function(result){
                if (!result) result = {result:"[42,13,22]"};
                return '<h3>' + rmsg(["Lesson 1 done already!"]) +
                    '</h3>' +
                    "<p>Great, you made a list of numbers! If you win we'll split" +
                    " the winnings, right?</p>" +
                    "<p>Let's see what you've learned so far:</p>" +
                    "<ol>"+
                    "<li>How to write maths and lists of things.</li>"+
                    "</ol>" +
                    "<p>You can do stuff with lists. Maybe you want the lottery "+
                    "numbers sorted in the right order, try this: " +
                    "<code>sort " + result.result + "</code></p>"
            },
             trigger:function(result){
                 return result.expr.match(/^[ ]*\[[0-9, ]+\][ ]*$/) &&
                     result.type == "(Num t) => [t]";
             }
            },
            ////////////////////////////////////////////////////////////////////////
            // Lesson 2 - Functions
            // Functions on lists
            {lesson:2,
             title: 'Simple Functions',
             guide:function(result){
                 if (!result) result = {result:"[13,23,30]"};
                 return '<h3>' + rmsg(["We put the funk in function"]) +
                     '</h3>' +
                     "<p>Congratulations, you just used a <strong>function</strong>."+
                     " They're how you get things done in Haskell." +
                     "<p>As you might've guessed, we got back <code>" +
                     htmlEncode(result.result)
                     + "</code>.</p><p>Ever wanted an evil twin nemesis? Me too. "+
                     "Luckily, you can sort lists of characters, or "+
                     "<strong>strings</strong>" +
                     ", in the same way as numbers! <code>sort \"chris\"</code></p>"
             },
             trigger:function(result){
                 return result.expr.match(/sort/) &&
                     result.type == "(Num t, Ord t) => [t]";
             }
            },
            // Tuples
            {guide:function(result){
                if (!result) result = {result:"\"chirs\""};
                nemesis = htmlEncode(unString(result.result));
                return '<h3>' +
                    rmsg(["Tuples, because sometimes one value ain't enough!"]) +
                    '</h3>' +
                    "<p>Watch out for "+nemesis+"! You should keep their credentials for the police.</p>" +
                    "<p>My nemesis is 28 years of age: "+
                    "<code>(28,\"chirs\")</code></p>"
            },
             trigger:function(result){
                 return result.expr.match(/sort/) &&
                     result.type == "[Char]";
             }
            },
            // Functions on tuples
            {guide:function(result){
                if (!result) result = {result:"(28,\"chirs\")"};
                var age = result.result.match(/^\(([0-9]+)+/);
                var villain = htmlEncode(result.result.replace(/\\"/g,'"'));
                return '<h3>' +
                    rmsg(["We'll keep them safe, sir."]) +
                    '</h3>' +
                    "<p>Is "+(age?age[1]:"that")+" a normal age for a " +
                    "super-villain?</p>" +
                    "<p>You just wrote a <em>tuple</em>. It's a way to keep a bunch of values together in Haskell. " +
                    "You can put as many as you like in there:</p>" +
                    "<ul><li><code>(1,\"hats\",23/35)</code></li><li><code>(\"Shaggy\",\"Daphnie\",\"Velma\")</code></li></ul>" +
                    "<p>Actually, let's say our villain <em>is</em> " +
                    "<code>" + villain + "</code>" +
                    ", how do you get their age?</p>" +
                    "<code>fst " + villain + "</code>"
            },
             trigger:function(result){
                 return result.expr.match(/\([0-9]+,[ ]*"[^"]+"\)/) &&
                     result.type == "(Num t) => (t, [Char])";
             }
            },
            // Summary of lesson 2
            {guide:function(result){
                return '<h3>' +
                    rmsg(["Lesson 2 done! Wow, great job!",
                          "Lesson 2 completo!"]) +
                    '</h3>' +

                "<p>Good job! You got the age back from the tuple! Didn't " +
                    " even break a sweat, did you? The <code>fst</code> function "+
                    "just gets the <em>first</em> value. It's called \"fst\" because " +
                    "it's used <em>a lot</em> in Haskell so it really needs to be short!</p>" +

                "<p>Time to take a rest and see what you learned:</p>" +
                    "<ol>"+
                    "<li>Functions can be used on lists of any type.</li>" +
                    "<li>We can stuff values into tuples.</li>" +
                    "<li>Getting the values back from tuples is easy.</li>"+
                    "</ol>" +

                "<p>Now let's say you want " +
                    " to use a value more than once, how would you do it? "+
                    "To make our lives easier, we can say:</p>" +

                "<code>let x = 4 in x * x</code>"
            },
             trigger:function(result){
                 return result.expr.match(/fst/) &&
                     result.type == "(Num t) => t";
             }
            },
            {guide:function(result){
                return "<h3>Let them eat cake</h3>" +

                "<p>You just <em>bound</em> a <em>variable</em>. " +
                    "That is, you bound <code>x</code> to the expression <code>4</code>, " +
                    " and then you can write <code>x</code> in some code (the <em>body</em>) and " +
                    " it will mean the same as if you'd written <code>4</code>.</p>" +

                "<p>It's like this: <code>let <em>var</em> = <em>expression</em> in <em>body</em></code></p>" +

                "The <code>in</code> part just separates the expression from the body.</p>" +

                "<p>For example try: " +
                    "<code><span class='highlight'>let</span> x <span class='highlight'>=</span> 8 * 10 <span class='highlight'>in</span> x + x</code></p>" +

                "<p>So if we wanted to get the age of our villain, we could do:</p>" +

                "<code><span class='highlight'>let</span> villain <span class='highlight'>=</span> (28,\"chirs\") <span class='highlight'>in</span> fst villain</code>"

            },trigger:function(result){
                return result.expr.match(/^[ ]*let[ ]+x[ ]*=[ ]*[0-9]+[ ]*in[ ]*x[ ]*\*[ ]*x/) &&
                    result.type == "(Num t) => t";
            }
            },
            {guide:function(result){
                return "<h3>Basics over, let's go!</h3>" +
                    "<p>Next, let's take a short detour to learn about " +
                    "<strong>syntactic sugar</strong>. " +
                    "Try typing this out:</p>" +
                    "<p><code>'a' : []</code></p>" +
                    "<p>Or skip to <code>lesson4</code> to learn about functions," +
                    " the meat of Haskell!";
            },trigger:function(result){
                return result.expr.match(/^[ ]*let[ ]+villain[ ]*=[ ]*\([0-9]+,[ ]*"[^"]+"\)[ ]*in[ ]+fst[ ]+villain[ ]*/) &&
                    result.type == "(Num t) => t";
            }
            },
            // Lesson 3: Syntactic sugar
            {lesson:3,
             title:'Syntactic Sugar',
             guide:function(result){
                 return '<h3>' +
                     rmsg(["You constructed a list!"]) +
                     '</h3>' +
                     "<p>Well done, that was tricky syntax. You used the <code>(:)</code> " +
                     "function. It takes two values, some value and a list, and " +
                     " constructs a new list" +
                     " out of them. We call it 'cons' for short.</p>" +
                     "<p><code>'a'</code> is " +
                     "the character 'a', <code>[]</code> is an empty list. So " +
                     "tacking <code>'a'</code> at the start of an empty list just "+
                     "makes a list <code>['a']</code>!</p>" +
                     "<p>But thankfully we don't have to type out " +
                     "<code>'a' : 'b' : []</code> every time to we want to make a "+
                     "list of characters; we can use " +
                     "<strong>syntactic sugar</strong> and just write"+
                     " <code>['a','b']</code>. Don't believe me, check this!</p>" +
                     "<code>'a' : 'b' : [] == ['a','b']</code>"
             },
             trigger:function(result){
                 return result.expr.match(/^[ ]*'a'[ ]*:[ ]*\[\][ ]*/) &&
                     result.type == "[Char]";
             }
            },
            // Booleans and string syntactic sugar
            {guide:function(result){
                return '<h3>' +
                    rmsg(["You're on fire!"]) +
                    '</h3>' +
                    "<p>You're handling this syntax really well, nice!</p>" +
                    "<p>You just got a boolean value back, and it said " +
                    "<code>True</code>. That means they're equal!</p>" +
                    "<p>One final demonstration on syntactic sugar for now:</p>" +
                    "<code>['a','b','c'] == \"abc\"</code>"
            },
             trigger:function(result){
                 return result.type == "Bool" &&
                     result.expr.replace(/[^':\[\]\=,]/g,'') == "'':'':[]==['','']";
             }
            },
            // Summary of syntactic sugar section
            {guide:function(result){
                return '<h3>' +
                    rmsg(["Lesson 3 over! Syntactic sugar is sweet"]) +
                    '</h3>' +
                    "<p>Let's have a gander at what you learned:</p>" +
                    "<ol>" +
                    "<li>In <code>'a' : []</code>, <code>:</code> is really just " +
                    " another function, just clever looking.</li>" +
                    "<li>Pretty functions like this are written like <code>(:)</code> when " +
                    " you talk about them.</li>" +
                    "<li>A list of characters <code>['a','b']</code> can just be written " +
                    "<code>\"ab\"</code>. Much easier!</li>"
                    + "</ol>" +
                    "<p>Phew! You're getting pretty deep! Your arch nemesis, " +
                    nemesis + ", is gonna try to steal your " + rmsg(['mojo',
                                                                      'pizza']) +
                    "! Let's learn a bit more about functions and passing " +
                    "them around. Try this:</p> <code>map (+1) [1..5]</code></p>";
            },
             trigger:function(result){
                 return result.expr.replace(/[^\]\[',=\"]?/g,'') == "['','','']==\"\"" &&
                     result.type == "Bool";
             }
            },
            {lesson:4,
             title:'Functions, reloaded; passing, defining, etc.',
             guide:function(){
                 var title =
                     rmsg(["Functions [of a Geisha]",
                           "Functions, functors, functoids, funky",
                           "Functions: Expanded fo' real"]);
                 return "<h3>" + title + "</h3>" +

                 "<p>Here's where the magic begins!</p>" +

                 "<p>You just passed the <code>(+1)</code> " +
                     "function to the <code>map</code> function.</p>" +

                 "<p>You can try other things like <small class='note'>(remember: click to insert them)</small>:</p>" +

                 "<ul>" +
                     "<li><code>map (*99) [1..10]</code></li>" +
                     "<li><code>map (/5) [13,24,52,42]</code></li>" +
                     "<li><code>filter (>5) [62,3,25,7,1,9]</code></li>" +
                     "</ul>" +

                 "<p>Note that a tuple is different to a list because you can do this:</p>" +
                     "<code>(1,\"George\")</code>"
             },
             trigger:function(result){
                 return result.expr.match(/^[ ]*map[ ]+\(\+1\)[ ]*\[1..5\][ ]*$/) &&
                     result.type == "(Num a, Enum a) => [a]";
             }},
            {guide:function(result){
                return "<h3>Lists and Tuples</h3>" +

                "<p>You can only " +
                    " have a list of numbers or a list of characters, whereas in a tuple you can throw anything in! </p>" +

                "<p>We've also seen that you can make a new list with <code>(:)</code> that joins two values together, like: </p>" +
                    "<p><code>1 : [2,3]</code></p>" +

                "<p>But we can't do this with tuples! You can only write a tuple and then look at what's inside. You can't make new ones on the fly like a list." +

                "<p>Let's write our own functions! It's really easy. How about something simple:</p>" +
                    "<code>let square x = x * x in square "+rmsg([52,10,3])+"</code>"

            },
             trigger:function(result){
                 return result.expr.match(/^[ ]*\(1,"[^"]+"\)[ ]*$/) &&
                     result.type == "(Num t) => (t, [Char])";
             }},
            {guide:function(result){
                return "<h3>Let there be functions</h3>" +
                    "<p>Nice one! I think you're getting used to the <code>let</code> syntax.</p>" +
                    "<p>You defined a function. You can read it as, as for a given " +
                    "<em>parameter</em> called <code>x</code>, <code>square</code> of " +
                    "<code>x</code> is <code>x * x</code>." +
                    "<p>Some others you can try are:</p>" +
                    "<ul><li><code>let add1 x = x + 1 in add1 5</code></li>" +
                    "<li><code>let second x = snd x in second (3,4)</code></li>" +
                    "</ul>" +
                    "<p>Let's go crazy and use our <code>square</code> function with map:</p>" +
                    "<code>let square x = x * x in map square [1..10]</code>"
            },
             trigger:function(result){
                 return result.expr.match(/^[ ]*let[ ]*square[ ]+x[ ]*=[ ]*x[ ]*\*[ ]*x[ ]*in[ ]*square[ ]+[0-9]+/) &&
                     result.type == "(Num t) => t";
             }},
            {guide:function(result){
                if (!result || !result.value) result = { value: "[1,4,9,16,25,36,49,64,81,100]" };
                return "<h3>Let there be functions</h3>" +

                "<p>That's so cool! You described a simple function <code>square</code> and then " +
                    "you just passed it to another function (<code>map</code>) and got back <code>" +
                    htmlEncode(result.value) + "</code>, exactly what you expected!</p>" +

                "<p>Haskell is pretty good at composing things together like this. " +
                    "Some other things you can try are:</p>" +

                "<ul>" +
                    "<li><code>let add1 x = x + 1 in map add1 [1,5,7]</code></li>" +
                    "<li><code>let take5s = filter (==5) in take5s [1,5,2,5,3,5]</code></li>" +
                    "<li><code>let take5s = filter (==5) in map take5s [[1,5],[5],[1,1]]</code></li>" +
                    "</ul>" +

                "<p>Did you get back what you expected?</p>" +

                "<p>One more example for text; how do you upcase a letter?</p>" +

                "<p><code>toUpper 'a'</code></p>"
            },
             trigger:function(result){
                 return result.expr.match(/^[ ]*let[ ]+square[ ]+x[ ]*=[ ]*x[ ]*\*[ ]*x[ ]*in[ ]+map[ ]+square[ ]*\[1..10\][ ]*$/) &&
                     result.type == "(Num a, Enum a) => [a]";
             }},
            {guide:function(result){
                return "<h3>Exercise time!</h3>" +

                "<p>Easy! Remember: characters are written like <code>'a'</code> and " +
                    "strings (lists of characters) are written like <code>\"a\"</code>." +

                "<p>I need you to use <code>toUpper</code> capitalise my whole name, " +
                    "<code>\"Chris\"</code>. Give it a try." +
                    " You can do it, I believe in you!</p>" +

                '<p>Spoiler: <code class="spoiler">map toUpper "Chris"</code></p>'
            },
             trigger:function(result){
                 return result.expr.match(/^toUpper 'a'$/) &&
                     result.type == "Char";
             }},
            {guide:function(result){
                return "<h3>Lesson 4 complete!</h3>" +

                "<p>Brilliant! You're making excellent progress! " +
                    "You just passed <code>toUpper</code> to <code>map</code>. No problem.</p>" +

                "<p>Let's go over what you've learned in this lesson:</p>" +

                "<ol>" +
                    "<li>Functions like <code>map</code> take other functions as parameters.</li>" +
                    "<li>Functions like <code>(+1)</code>, <code>(>5)</code> and "+
                    "<code>square</code> can be passed to other functions.</li>" +
                    "<li>Defining functions is just a case of writing what "+
                    "to do with the parameters.</li>"  + "</ol>" +

                "<p>Let's check out <em>pattern matching</em>; a way to "+
                    "get values from other values using patterns. Try this: </p>" +
                    "<p><code>let (a,b) = (10,12) in a * 2</code></p>" +

                "<p>Or you can skip this section and go to straight to <code>lesson6</code>; <em>types!</em></p>"
            },
             trigger:function(result){
                 return result.type == "[Char]" &&
                     result.expr.match(/^map[ ]+toUpper/);
             }},
            {lesson:5,
             title:'Pattern Matching',
             guide:function(result){
                 var title =
                     rmsg(["And therefore, patterns emerge in nature.",
                           "And Then Patterns",
                           "Pattern matching!"])
                 return "<h3>" + title + "</h3>" +

                 "<p>Good typing, sir!</p>" +
                     "<p>So you had a value <code>(10,12)</code> and matched " +
                     "it against a pattern <code>(a,b)</code>, then you were able" +
                     " to do stuff with the <code>a</code> and <code>b</code>!" +

                 "<p>Note: Pattern matching <code>(a,b)</code> against "+
                     "<code>(1,2)</code> to get the <code>a</code> is the same as" +
                     " doing <code>fst (1,2)</code>, like you did in <code>step7</code>!</p>" +

                 "<p>A pattern always matches the way the "+
                     "value was originally constructed. Remember that <code>\"abc\"</code> is " +
                     "syntactic sugar for <code>'a' : 'b' : 'c' : []</code>.</p>" +

                 "<p>So you can get the characters from a string with patterns:</p>" +

                 "<code>let (a:b:c:[]) = \"xyz\" in a</code>"
             },
             trigger:function(result){
                 return result.expr.match(/^[ ]*let[ ]+\(a,b\)[ ]+=[ ]+\(10,12\)[ ]+in[ ]+a[ ]*\*[ ]*2[ ]*$/) &&
                     result.type == "(Num t) => t";
             }},
            {guide:function(result){
                return "<h3>"+rmsg(["Ignorance is bliss","Ignoring values"])+"</h3>" +

                "<p>You're getting into tricky syntax, huh? I know you can handle it!</p>" +

                "<p>If you just want some of the values, you can ignore the others with <code>_</code> (underscore) like this:</p>" +

                "<p><code>let (a:_:_:_) = \"xyz\" in a</code></p>" +

                "<p>In fact, <code>(a:b:c:d)</code> is short-hand for " +
                    "<code>(a:(b:(c:d)))</code>, so you can just ignore the rest in one go:</p>" +

                "<code>let (a:_) = \"xyz\" in a</code>"
            },
             trigger:function(result){
                 return result.expr.match(/^[ ]*let[ ]+\(a:b:c:\[\]\)[ ]*=[ ]*\"xyz\"[ ]*in[ ]+a[ ]*$/) &&
                     result.type == "Char";
             }},
            {guide:function(result){
                return "<h3>"+rmsg(["Exercise!","Show me the money!"])+"</h3>" +

                "<p>Try to get the <code>'a'</code> value from this value using pattern matching:</p>" +
                    "<p><code>(10,\"abc\")</code></p>" +

                "<p>Spoiler: <code class='spoiler'>let (_,(a:_)) = (10,\"abc\") in a</code></p>"
            },
             trigger:function(result){
                 return result.expr.match(/^[ ]*let[ ]*\(a:_\)[ ]*=[ ]*"xyz"[ ]*in[ ]*a[ ]*$/) &&
                     result.type == "Char";
             }},
            {guide:function(result){
                return "<h3>"+rmsg(["Well done!","Brilliant!","Perfetto!"])+"</h3>" +

                "<p>Wizard! I think you've got pattern-matching down.</p>" +

                "<p>If you're still a bit unsure, here are some other things you can try:</p>" +

                "<ul>" +
                    "<li><code>let _:_:c:_ = \"abcd\" in c</code></li>" +
                    "<li><code>let [a,b,c] = \"cat\" in (a,b,c)</code></li>" +
                    "</ul>" +

                "<p>You can also grab a whole value <em>and</em> pattern match on it (have your cake and eat it too):</p>" +

                "<code>let abc@(a,b,c) = (10,20,30) in (abc,a,b,c)</code>"
            },
             trigger:function(result){
                 return result.expr.match(/^[ ]*let[ ]*\(_,\(?a:_\)?\)[ ]*=[ ]*\(10,\"abc\"\)[ ]*in[ ]*a[ ]*$/) &&
                     result.type == "Char";
             }},
            {guide:function(result){
                return "<h3>"+rmsg(["And that's the end of that chapter"])+"</h3>" +

                "<p>That was easy, right?</p>" +

                "<p>Let's go over what you've learned in this lesson:</p>" +

                "<ol>" +
                    "<li>Values are pattern matched, or <em>deconstructed</em>, by writing however they were constructed.</li>" +
                    "<li>Patterns let you use the values that you match.</li>" +
                    "<li>You can ignore whichever values you want.</li>" +
                    "<li>You can pattern match and keep hold of the original value too.</li>" +
                    "</ol>" +

                "<p>Now we get to the Deep Ones. Types!</p>" +

                "<p>Consider the following value: <code>'a'</code></p>"

            },
             trigger:function(result){
                 return result.type == "(Num t, Num t1, Num t2) => ((t, t1, t2), t, t1, t2)";
             }},
            {lesson:6,
             title:'Types',
             guide:function(result){
                 showTypes = true;
                 return "<h3>"+rmsg(["Types","What's in a Type?","Types & Values"])+"</h3>" +
                     "<p>What's this? Something new!</p>" +

                 "<p>In Haskell there are types of values. Every value belongs to a type. To demonstrate this fact, I've sneakily enabled types to be " +
                     "shown of every value in the console from now on.</p>" +

                 "<p>The type of the value <code>'a'</code> is <code>Char</code> (short for 'character', but you guessed that, right?).</p>" +

                 "<p>You've seen the type of a character, now what about" +
                     " a list of characters?</p>" +
                     "<code>\"Spartacus\"</code>"
             },
             trigger:function(result){
                 return result.type == 'Char';
             }},
            {guide:function(result){
                showTypes = true;
                return "<h3>"+rmsg(["Lists of stuff, types"])+"</h3>" +

                "<p>I'm Spartacus!</p>" +

                "<p>Okay, so a list of characters has type <code>[Char]</code>.</p>" +

                "<p>Notice that when we write <code>a :: X</code> it means <em>the value a has type X</em>. It's just a short-hand called a <em>signature</em>.</p>" +

                "<p>If you just want the type of a value, without actually evaluating it, you can just type: </p>" +
                    "<code>:t toUpper</code>"
            },
             trigger:function(result){
                 return result.expr.match(/"[^"]+"/) &&
                     result.type == '[Char]';
             }},
            {guide:function(result){
                showTypes = true;
                return "<h3>"+rmsg(["Function types"])+"</h3>" +

                "<p>Woah! Hold your blinkin' 'orses! The type of <code>toUpper</code> reads: <code>Char -> Char</code></p>" +

                "<p>It's pretty easy; <code>a -> b</code> means <em>function from <code>a</code> to <code>b</code></em>. " +
                    "So</p><p><code>toUpper :: Char -> Char</code> means: for a" +
                    " given character (<code>Char</code> value) <code>a</code>, <code>toUpper a</code> has type <code>Char</code>.</p>" +

                "<p>Some other things you can try are:</p>" +

                "<ul><li><code>:t words</code></li>"+
                    "<li><code>:t unwords</code></li>" +
                    "<li><code>:t True</code></li>" +
                    "<li><code>:t not</code></li>"
                    +"</ul>" +

                "<p>The words function is pretty handy. Want to get a list of words from a sentence?</p>" +
                    "<code>words \"There's jam in my pants.\"</code>"
            },
             trigger:function(result){
                 return result.type == 'Char -> Char';
             }},
            {guide:function(result){
                showTypes = true;
                return "<h3>"+rmsg(["Mid-way review"])+"</h3>" +

                "<p>The type of <code>words</code> was <code>String -> [String]</code>. You got a list of strings back! Just what you expected, right?</p>" +

                "<p>Let's take a rest in the middle of this lesson and go over what we've learned:</p>" +

                "<ol>"+
                    "<li>All values in Haskell have a <em>type</em>. We describe the types of values with <em>signatures</em>, like <code>True :: Bool</code>.</li>"+
                    "<li>Functions are values too, and they have types, notated <code>a -> b</code>.</li>"+
                    "<li>Functions can be defined for any type to any other type.</li>"+
                    "<li>Humble reader has a thing for jammy pants.</li>"+
                    "</ol>" +

                "<p>But what if you have a type that can contain values of <em>any</em> type, like a tuple?</p>" +
                    "<code>:t fst</code>"

            },
             trigger:function(result){
                 return result.expr.match(/^[ ]*words[ ]*\"[^"]+\"[ ]*$/) &&
                     result.type == '[String]';
             }},
            {guide:function(result){
                showTypes = true;
                return "<h3>"+rmsg(["Polymorphic functions"])+"</h3>" +

                "<p>Remember this one? I know you do! <code>fst (1,2)</code> is <code>1</code>, right?</p>" +
                    "<p>We read its type</p>" +
                    "<p><code>fst :: (a, b) -> a</code></p>" +
                    "<p>as: <em>for all types <code>a</code> and <code>b</code>, the <code>fst</code> has type <code>(a,b)</code> to <code>a</code>. </em>So the <code>fst</code> "+
                    "function works on a pair of values of any types! We call such a function <em>polymorphic</em>."+
                    "</p>" +
                    "<p>Remember the <code>drop</code> function? Maybe you don't. I don't! Let's check out its type:</p>" +
                    "<p><code>:t drop</code></p>"
            },
             trigger:function(result){
                 return result.type == '(a, b) -> a';
             }},
            {guide:function(result){
                showTypes = true;
                return "<h3>"+rmsg(["Multi parameter functions"])+"</h3>" +

                "<p>So the <code>drop</code> function has type</p><p><code>Int -> [a] -> [a]</code>.</p>" +

                "<p>This is something new. You've got two arrows! Relax. You can read</p>" +
                    "<p><code>a -> b -> c</code> as <code>a -> (b -> c)</code></p>" +

                "<p>In other words, <code>drop</code> is a function from integers (<code>Int</code> values) to functions of lists to lists (<code>[a] -> [a]</code> values). Drop is a function to another function.</p>" +

                "<p>Check for yourself! <code>:t drop 3</code></p>"
            },
             trigger:function(result){
                 return result.type == 'Int -> [a] -> [a]';
             }},
            {guide:function(result){
                showTypes = true;
                return "<h3>"+rmsg(["Partial application"])+"</h3>" +

                "<p>You've got a function of type <code>[a] -> [a]</code>! The <code>drop</code> function is considered a multi-parameter function. Remember the <code>map</code> function? Its parameters were a function and a list. Just another multi-parameter function.</p>" +

                "<p>You can add another parameter and, hey presto, you get a list!</p>" +

                "<code>drop 3 \"hello!\"</code>"

            },
             trigger:function(result){
                 return result.type == '[a] -> [a]';
             }},
            {guide:function(result){
                showTypes = true;
                return "<h3>"+rmsg(["Higher order functions"])+"</h3>" +

                "<p>'Lo bob! You've already used the <code>map</code> function loads. I wonder if you can guess its type?</p>" +

                "<p><code>map :: <code class=\"spoiler\">(a -> b) -> [a] -> [b]</code></code> (spoiler)</p>" +

                "<p>It's okay to peek! Have a go at guessing these: <code>filter</code>, <code>take</code></p>" +

                "<p>Tip: You can use parantheses to use more than one function. You want to double all the numbers over five? Psch! </p>" +
                    "<code>map (*2) (filter (>5) [10,2,16,9,4])</code>"
            },
             trigger:function(result){
                 return result.expr.match(/^[ ]*drop[ ]*[0-9]+[ ]*"[^"]+"[ ]*$/) &&
                     result.type == '[Char]';
             }},
            {guide:function(result){
                showTypes = true;
                return "<h3>"+rmsg(["Phew! Rest time!"])+"</h3>" +

                "<p>Wow! You're doing so great! Have a look at what you know now!</p>" +

                "<ol>" +
                    "<li>Function parameters can be <em>polymorphic</em>; any type!</li>" +
                    "<li>Functions can have multiple parameters by returning more functions.</li>" +
                    "<li>You can wrap expressions in parentheses and apply functions to them as a whole value.</li>" +
                    "</ol>" +

                "<p>You're really making great progress. Don't hesitate to sit and play in the console between chapters to get a good feel of it!</p>" +

                "<p>Stay tuned for more chapters on <em>type classes</em> and the meaning of <code>:t 1</code>, <code>:t (*)</code>, etc.</p>" +
                    learnMore
            },
             trigger:function(result){
                 return result.type == '(Num a, Ord a) => [a]';
             }}
        ];

    var webchat;

    function runWebchat() {
        if (!webchat) {
            // Create webchat frame
            var webchat =
                $('<iframe id="webchat" src="http://webchat.freenode.net?channels=haskell&uio=Mz1mYWxzZSYxMD10cnVlJjExPTI0Nge9"></iframe>');
            webchat.attr('width',635);
            webchat.attr('height',500);
            webchat.css('float','left');
            webchat.css('webkit-border-radius','3px');
            webchat.css('moz-border-radius','3px');
            webchat.css('border-radius','3px');
            webchat.css('border','5px solid #eeeeee');

            // Extend page wrap to fit console and chat
            $('.page-wrap').css({width:'1250px'});
            $('.primary-content').css('margin-left',0);
            $('.page-wrap').append(webchat.css('margin-left','5px'));
        }
    }

    var pageTrigger = -1;
    var notices = [];
    var controller; // Console controller
    var learnMore;

    ////////////////////////////////////////////////////////////////////////
    // Unshow a string
    function unString(str){
        return str.replace(/^"(.*)"$/,"$1").replace(/\\"/,'"');
    }

    ////////////////////////////////////////////////////////////////////////
    // Random message from a list of messages
    function rmsg(choices) {
        return choices[Math.floor((Math.random()*100) % choices.length)];
    }

    // Simple HTML encoding
    // Simply replace '<', '>' and '&'
    // TODO: Use jQuery's .html() trick, or grab a proper, fast
    // HTML encoder.
    function htmlEncode(text,shy){
        return (
            (''+text).replace(/&/g,'&amp;')
                .replace(/</g,'&lt;')
                .replace(/</g,'&lt;')
                .replace(/ /g,'&nbsp;')
        );
    }

    var lastLine;

    $(document).ready(function(){
        learnMore = $('#learn-more').children('div').html();
        $('.reset-btn').click(function(){
            if (confirm("Are you sure you want to reset? " +
                        "You will lose your current state.")) {
                controller.reset();
                tutorialGuide.animate({opacity:0,height:0},'fast',function(){
                    tutorialGuide.html(initalGuide);
                    tutorialGuide.css({height:'auto'});
                    tutorialGuide.animate({opacity:1},'fast');
                });
            }
        });

        $('.load-btn').click(function(){
            alert(encodeHex("a bcd"));
            /*
              $('#editor').focus();
              var line = $('#editor').val();
              // /haskell-eval.json?jsonrpc=2.0&method=load&id=1&params={"contents":"x=1"}
              $.get("/tryhaskell/haskell-eval.json?jsonrpc=2.0&id=1&method=load&params="
              + JSON.stringify({expr:line.replace(/\+/g,'%2b')
              .replace(/\#/g,'%23')}),
              function(resp){

              });
            */
        });

        ////////////////////////////////////////////////////////////////////////
        // Guide globals
        // Get the guide element.
        tutorialGuide = $('.guide');
        var initalGuide = tutorialGuide.html();
        var toldAboutRet = false;
        var tellAboutRet;

        function jsonp(url,func) {
            var script = $('<script type="text/javascript" src="'+url+'"></script>');
            handleJSON = function(r){
                script.remove();
                func(r);
            };
            script.attr('src',url);
            $('body').append(script);
        }

        ////////////////////////////////////////////////////////////////////////
        // Create console
        var console = $('.console');
        controller = console.console({
            promptLabel: '> ',
            commandValidate:function(line){
                if (line == "") return false; // Empty line is invalid
                else return true;
            },
            cancelHandle:function(){
                controller.commandRef.ignore = true;
                controller.finishCommand();
                controller.report();
            },
            commandHandle:function(line,report){
                controller.ajaxloader = $('<p class="ajax-loader">Loading...</p>');
                var commandRef = {};
                controller.currentLine = line;
                controller.commandRef = commandRef;
                controller.report = report;
                if (tellAboutRet) tellAboutRet.fadeOut(function(){
                    $(this).remove();
                });
                if (libTrigger(line,report)) return;
                controller.inner.append(controller.ajaxloader);
                controller.scrollToBottom();
                jsonp("http://localhost:8080/cajole?input-mime-type=text/javascript&callback=handleJSON&alt=json-in-script&directive=ES53&content=0;" + encodeURIComponent(line) + "&random=" + Math.random(),
                      function(resp){
                          if (commandRef.ignore) { return; }
                          controller.finishCommand();
                          var result = resp;
                          caja___.enable(true, document.getElementById('result'), "", "", result.js, function(runtimeResult) {
                            if (pageTrigger > -1 && runtimeResult.success) {
                                triggerTutorialPage(pageTrigger,runtimeResult.result); 
                            }
                            // Not used by caja
                            if (result.type) {
                                if (pageTrigger == 24) showTypes = true;
                                handleSuccess(report,result,showTypes);
                            } else if (!result.js) {
                                report(
                                    [{msg:result.messages[0].message,
                                      className:"jquery-console-message-error jquery-console-message-compile-error"}]
                                );
                                notice('compile-error',
                                       "A compile-time error! "+
                                       "It just means the expression wasn't quite right. " +
                                       "Try again.",
                                       'prompt');
                            } else if (runtimeResult.exception) {
                                var err = limitsError(runtimeResult.exception);
                                report(
                                    [{msg:err.message,
                                      className:"jquery-console-message-error jquery-console-message-exception"}]
                                );
                                if (err == runtimeResult.exception) {
                                    notice('compile-error',
                                           "A run-time error! The expression was right but the"+
                                           " result didn't make sense. Check your expression and try again.",
                                           'prompt');
                                }
                            } else if (result.internal) {
                                report(
                                    [{msg:limitsError(result.internal),
                                      className:"jquery-console-message-error jquery-console-message-internal"}]
                                );
                            } else if (result.bind) {
                                report();
                            } else if (runtimeResult.success) {
                              report(
                                     [{msg:jsDump.parse(runtimeResult.result),
                                          className:"jquery-console-message-type"}]);
                                }
                            });
                      });
            },
            charInsertTrigger:function(){
                var t = notice('tellaboutreturn',
                               "Hit Return when you're "+
                               "finished typing your expression.");
                if (t) tellAboutRet = t;
                return true;
            },
            autofocus:true,
            promptHistory:true,
            historyPreserveColumn:true,
            welcomeMessage:'Type Caja expressions in here.'
        });

        controller.finishCommand = function() {
            controller.ajaxloader.remove();
            $('.jquery-console-prompt :last').each(function(){
                lastLine = controller.currentLine;
                if (!$(this).hasClass('prompt-done')) {
                    $(this).addClass('prompt-done');
                    $(this).click(function(){
                        controller.promptText(controller.currentLine);
                    });
                }
            });
        }

        makeGuidSamplesClickable();

        var match = window.location.href.match(/#([0-9]+)$/);
        if (match) {
            pageTrigger = match[1]-1;
            setTutorialPage(undefined,match[1]-1);
        }

        var match = window.location.href.match(/\?input=([^&]+)/);
        if (match) {
            controller.promptText(urlDecode(match[1]));
            controller.inner.click();
            controller.typer.consoleControl(13);
        }
    });

    function urlDecode (encodedString) {
        var output = encodedString;
        var binVal, thisString;
        var myregexp = /(%[^%]{2})/;
        while ((match = myregexp.exec(output)) != null
               && match.length > 1
               && match[1] != '') {
            binVal = parseInt(match[1].substr(1),16);
            thisString = String.fromCharCode(binVal);
            output = output.replace(match[1], thisString);
        }
        return output;
    }

    function makeGuidSamplesClickable() {
        $('.guide code').each(function(){
            $(this).css('cursor','pointer');
            $(this).attr('title','Click me to insert "' +
                         $(this).text() + '" into the console.');
            $(this).click(function(){
                controller.promptText($(this).text());
                controller.inner.click();
            });
        });
    }

    String.prototype.trim = function() {
        return this.replace(/^[\t ]*(.*)[\t ]*$/,'$1');
    };

    ////////////////////////////////////////////////////////////////////////
    // Trigger console commands
    function libTrigger(line,report) {
        switch (line.trim()) {
        case 'help': {
            setTutorialPage(undefined,0);
            report();
            pageTrigger = 0;
            return true;
        }
        case 'back': {
            if (pageTrigger > 0) {
                setTutorialPage(undefined,pageTrigger-1);
                pageTrigger--;
                report();
                return true;
            }
            break;
        }
        case 'lessons': {
            var lessons = $('<ol></ol>');
            for (var i = 0; i < pages.length; i++) {
                if (pages[i].lesson) {
                    lessons.append($('<li></li>').
                                   html('<code>lesson'+pages[i].lesson+'</code> - ' +
                                        pages[i].title));
                }
            }
            var lessonsList = '<h3>Lessons</h3>' + lessons.html();
            tutorialGuide.animate({opacity:0,height:0},'fast',function(){
                tutorialGuide.html(lessonsList);
                tutorialGuide.css({height:'auto'});
                tutorialGuide.animate({opacity:1},'fast');
                makeGuidSamplesClickable();
            });
            report();
            return true;
        }
        default: {
            if (line.trim() == 'chat') {
                notice('irc',
                       'Enter your nick on the right hand side and hit Connect!',
                      'prompt');
                report();
                runWebchat();
                return true;
            }

            var m = line.trim().match(/^link(.*)/);
            if (m) {
                var data;
                if (m[1]) data = m[1].trim();
                else if (lastLine) data = lastLine;
                if (data) {
                    var addr = '?input=' + encodeHex(data);
                    report([{msg:'',className:'latest-link'}]);
                    var link = $('<a href="' + addr + '"></a>').
                        text('link for ' + data).click(function(){
                            window.location.href = $(this).attr('href');
                            return false;
                        });
                    $('.latest-link').html(link).removeClass('latest-link');
                    return true;
                }
            }

            var m = line.trim().match(/^step([0-9]+)/);
            if (m) {
                if ((m[1]*1) <= pages.length) {
                    setTutorialPage(undefined,m[1]-1);
                    report();
                    pageTrigger = m[1]-1;
                    return true;
                }
            }
            var m = line.trim().match(/^lesson([0-9]+)/);
            if (m) {
                for (var i = 0; i < pages.length; i++) {
                    if (pages[i].lesson == m[1]*1) {
                        setTutorialPage(undefined,i);
                        report();
                        pageTrigger = i;
                        return true;
                    }
                }
            }
        }
        };
    };

    ////////////////////////////////////////////////////////////////////////
    // Change the tutorial page

    function setTutorialPage(result,n) {
        if (pages[n]) {
            window.location.href = '#' + (1*n + 1);
            tutorialGuide.find('.lesson').remove();
            tutorialGuide.animate({opacity:0,height:0},'fast',function(){
                if (typeof(pages[n].guide) == 'function')
                    tutorialGuide.html(pages[n].guide(result));
                else
                    tutorialGuide.html(pages[n].guide);
                var back = '';
                if (pageTrigger>0)
                    back = 'You\'re at <code>step' + (n+1)
                    + '</code>. Type <code>back</code> to go back.';
                else
                    back = 'You\'re at step' + (n+1) + '. Type <code>step' + (n+1)
                    + '</code> to return here.';
                if (true) tutorialGuide
                    .append('<div class="note">' + back + '</div>')
                    .append('<div class="lesson">Lesson: ' +
                            searchLessonBack(n) +
                            '</div>');
                tutorialGuide.css({height:'auto'});
                tutorialGuide.animate({opacity:1},'fast');
                makeGuidSamplesClickable();
            });
        }
    };

    function searchLessonBack(page) {
        for (var i = page; i >= 0; i--) {
            if (pages[i].lesson) return pages[i].lesson;
        }
        return "1";
    }

    ////////////////////////////////////////////////////////////////////////
    // Trigger a page according to a result

    function triggerTutorialPage(n,result) {
        n++;
        if (pages[n] && (typeof (pages[n].trigger) == 'function')
            && pages[n].trigger(result)) {
            pageTrigger++;
            setTutorialPage(result,n);
        }
    };

    ////////////////////////////////////////////////////////////////////////
    // Trigger various libraries after JSONRPC returned
    function handleSuccess(report,result,showType) {
        if (result.result) {
            var type = [];
            if (showType) {
                type = [{msg:':: ' + result.type,
                         className:"jquery-console-message-type"}];
            }
            report(
                [{msg:'=> ' + result.result,
                  className:"jquery-console-message-value"}].concat(type)
            );
        } else {
            report(
                [{msg:':: ' + result.type,
                  className:"jquery-console-message-type"}]
            );
        }
    };

    ////////////////////////////////////////////////////////////////////////
    function notice(name,msg,style) {
      if (opera()) return;
        if (!notices[name]) {
            notices[name] = name;
            return controller.notice(msg,style);
        }
    }

    function limitsError(str) {
        if (str == "Terminated!") {
            notice('terminated',
                   "This error means it took to long to work" +
                   " out on the server.",
                   'fadeout');
            return "Terminated!";
        } else if (str == "Time limit exceeded.") {
            notice('exceeded',
                   "This error means it took to long to work out on the server. " +
                   "Try again.",
                   'fadeout');
            return "Terminated! Try again.";
        }
        return str;
    }

})(jQuery);
