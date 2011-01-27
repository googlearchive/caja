// TODO(jasvir): Some of these are not secure.  Please secure them.

function renderObj(obj) {
  jsDump.multiline = false;
  var result = jsDump.parse(obj);
  if (result.length > 45) {
    jsDump.multiline = true;
    return jsDump.parse(obj);
  } else {
    return result;
  }
}

function unString(str){
  return str.replace(/^"(.*)"$/,"$1").replace(/\\"/,'"'); //'
}

function rmsg(choices) {
  return choices[Math.floor((Math.random()*100) % choices.length)];
}

function htmlEncode(text,shy){
  return (
      (''+text).replace(/&/g,'&amp;')
              .replace(/</g,'&lt;')
              .replace(/</g,'&lt;')
              .replace(/ /g,'&nbsp;')
      );
}

function concat(args) {
  var result = "";
  for (var i in args) {
    result += String(i);
  }
  return result;
}

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
