window.assertTrue = function(cond, msg) {
  if (!cond) {
    log('<font color="orange">expected true but was false:' +
        ' (' + msg + ')</font><br>');
  } else {
    log('<font color="green">verfied ' + msg + '</font><br>');
  }
};

window.assertEquals = function(a, b, msg) {
  if (a !== b) {
    log('<font color="orange">expected \u00ab' + a + '\u00bb' +
        ' but found \u00ab' + b + '\u00bb (' + msg + ')</font><br>');
  } else {
    log('<font color="green">verfied ' + msg + '</font><br>');
  }
};

window.log = function(msg) {
  document.getElementById('log').innerHTML += msg;
};