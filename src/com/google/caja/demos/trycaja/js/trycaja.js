(function($){
  var cajaDisplayed = false;  
  var tutorialGuide;

  // Page variables
  var pages = mypages;
  
  function showDomita(report) {
    if (!cajaDisplayed) {
      cajaDisplayed = true;
      document.getElementById('cajaDisplay').style.display = '';
      document.getElementById('cajaDisplayContainer').style.display = '';
      // Extend page wrap to fit console and chat
      $('.page-wrap').css({width:'1000px'});
      $('.primary-content').css('margin-left',50);
    }
    report();
  }

  var pageTrigger = -1;
  var notices = [];
  var controller; // Console controller
  var lastLine;
  var debug = false;

  $(document).ready(function(){
      $('.reset-btn').click(function(){
          if (confirm("Are you sure you want to reset? " +
              "You will lose your current state.")) {
              controller.reset();
              tutorialGuide.animate({opacity:0,height:0},'fast',function(){
                  tutorialGuide.html(initalGuide);
                  tutorialGuide.css({height:'auto'});
                  tutorialGuide.animate({opacity:1},'fast');
              });
              makeGuidSamplesClickable();
          }
      });

      ////////////////////////////////////////////////////////////////////////
      // Guide globals
      // Get the guide element.
      tutorialGuide = $('.guide');
      var initalGuide = tutorialGuide.html();
      var tellAboutRet;

      function jsonp(url,func) {
        var script = $('<script type="text/javascript" src="' + encodeURI(url) + '"></script>');
        handleJSON = function(r){
          script.remove();
          func(r);
        };
        script.attr('src',url);
        $('body').append(script);
      }

      function cajole(line, callback) {
        jsonp("http://caja.appspot.com/cajole?"
              + "input-mime-type=text/javascript&"
              + "callback=handleJSON&"
              + "alt=json-in-script&"
              + "directive=ES53&"
              // work around for "use " directives
              + "content=0;" + encodeURIComponent(line) + "&"
              // Bust cache
              + "random=" + Math.random(), callback);
      }


      ////////////////////////////////////////////////////////////////////////
      // Create console
      var console = $('.console');
      controller = console.console({
        promptLabel: '> ',
        commandValidate: function(line){
          if (line == "") return false; // Empty line is invalid
          return true;
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
          cajole(line, function(resp){
              if (commandRef.ignore) { return; }
              controller.finishCommand();
              caja___.enable(true, document.getElementById("cajaDisplay"), "",
                  "", resp.js, function(runtimeResult) {
                      var result = { 
                        result: runtimeResult.result, 
                        success: runtimeResult.success,
                        exception: runtimeResult.exception,
                        expr: line,
                        js: resp.js,
                        messages: resp.messages
                      };
                      if (pageTrigger > -1 && result.success) {
                        triggerTutorialPage(pageTrigger,result);
                      }
                      if (!result.js) {
                        report([{
                          msg:result.messages[0].message,
                          className: "jquery-console-message-error jquery-console-message-compile-error"
                        }]);
                        notice('compile-error',
                               "A compile-time error! "+
                               "It just means the expression wasn't quite right. " +
                               "Try again.",
                               'prompt');
                      } else if (result.exception) {
                        var err = explainServerError(result.exception);
                        report([{
                            msg:err.message,
                            className:"jquery-console-message-error jquery-console-message-exception"
                        }]);
                        if (err == result.exception) {
                          notice('compile-error',
                                 "A run-time error! The expression was right but the"+
                                 " result didn't make sense. Check your expression and try again.",
                                 'prompt');
                        }
                      } else if (result.internal) {
                        report([{
                            msg: explainServerError(result.internal),
                            className:"jquery-console-message-error jquery-console-message-internal"
                        }]);
                      } else if (result.success) {
                        if (debug) {
                          report([{
                              msg: renderObj(result.result),
                              className:"jquery-console-message-value"
                            }, { 
                              msg: result.js,
                              className:"jquery-console-message-type"
                            }]);
                        } else {
                          report([{
                            msg: renderObj(result.result),
                            className:"jquery-console-message-value"
                          }]);
                        }
                      } else {
                        notice('compile-error',
                               "A run-time error! The expression was right but the"+
                               " result didn't make sense. Check your expression and try again.",
                               'prompt');
                      }
              });
          })
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
          $('.jquery-console-prompt :last').each(function() {
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

    match = window.location.href.match(/\?input=([^&]+)/);
    if (match) {
      controller.promptText(urlDecode(match[1]));
      controller.inner.click();
      controller.typer.consoleInsert(13);
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
    case 'debug': {
      debug = !debug;
      report([{
             msg: "Toggled " + (debug ? "on" : "off") + " debugging",
        className:"jquery-console-message-alert"
      }]);
      return true;
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
      if (line.trim() == 'display') {
        showDomita(report);
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
      window.location.href.hash = (1*n + 1);
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
  function notice(name,msg,style) {
    if (!notices[name]) {
      notices[name] = name;
      return controller.notice(msg,style);
    }
  }
  
  function explainServerError(str) {
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
