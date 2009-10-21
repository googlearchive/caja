/*
 * Simulated browser environment for Rhino
 *   By John Resig <http://ejohn.org/>
 * Copyright 2007 John Resig, under the MIT License
 */

// The window Object
var window = this;

(function(){

  // Browser Navigator

  window.navigator = {
    get userAgent() {
      return "Mozilla/5.0 (Macintosh; U; Intel Mac OS X; en-US; rv:1.8.1.3) Gecko/20070309 Firefox/2.0.0.3";
    },
    get appName() {
      return 'Netscape';
    }
  };

  var curLocation = scriptEngine___.currentLocation();

  window.__defineSetter__("location", function(url){
    var xhr = new XMLHttpRequest();
    xhr.open("GET", url);
    xhr.onreadystatechange = function(){
      curLocation = curLocation.resolve(url);
      window.document = xhr.responseXML;

      var event = window.document.createEvent();
      event.initEvent("load");
      window.dispatchEvent( event );
    };
    xhr.send();
  });

  window.__defineGetter__("location", function(url){
    return {
      get hash() {
        return curLocation.getFragment() || '';
      },
      get host() {
        var hostname = this.hostname, port = this.port;
        return hostname + (port ? ':' + port : '');
      },
      get hostname() {
        return String(curLocation.getHost()) || '';
      },
      get href() {
        return String(curLocation.toString()) || '';
      },
      get pathname() {
        return String(curLocation.getPath()) || '';
      },
      get port() {
        return String(curLocation.getPort()) || 0;
      },
      get protocol() {
        return String(curLocation.getScheme()) + ":";
      },
      get search() {
        return String(curLocation.getQuery()) || '';
      },
      toString: function () {
        return this.href;
      }
    };
  });

  // Timers

  var timers = [];

  window.setTimeout = function(fn, time){
    var num;
    return num = setInterval(function(){
      fn();
      clearInterval(num);
    }, time);
  };

  window.setInterval = function(fn, time){
    var num = timers.length;

    timers[num] = scriptEngine___.timer(fn, time);
    timers[num].start();

    return num;
  };

  window.clearInterval = function(num){
    if ( timers[num] ) {
      timers[num].stop();
      delete timers[num];
    }
  };

  // Window Events

  var events = [{}];

  window.addEventListener = function(type, fn){
    if ( !this.uuid || this == window ) {
      this.uuid = events.length;
      events[this.uuid] = {};
    }

    if ( !events[this.uuid][type] )
      events[this.uuid][type] = [];

    if ( events[this.uuid][type].indexOf( fn ) < 0 )
      events[this.uuid][type].push( fn );
  };

  window.removeEventListener = function(type, fn){
     if ( !this.uuid || this == window ) {
         this.uuid = events.length;
         events[this.uuid] = {};
     }

     if ( !events[this.uuid][type] )
      events[this.uuid][type] = [];

    events[this.uuid][type] =
      events[this.uuid][type].filter(function(f){
        return f != fn;
      });
  };

  window.dispatchEvent = function(event){
    if ( event.type ) {
      if ( this.uuid && events[this.uuid][event.type] ) {
        var self = this;

        events[this.uuid][event.type].forEach(function(fn){
          fn.call( self, event );
        });
      }

      if ( this["on" + event.type] )
        this["on" + event.type].call( self, event );
    }
  };

  // DOM Document

  window.DOMDocument = function(file) {
    this._file = file;
    this._dom = scriptEngine___.parseDom(file);
    scriptEngine___.dontEnum(this, '_file');
    scriptEngine___.dontEnum(this, '_dom');
    if ( !obj_nodes.containsKey( this._dom ) )
      obj_nodes.put( this._dom, this );
  };

  DOMDocument.prototype = {
    createTextNode: function(text){
      return makeNode( this._dom.createTextNode(
        text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")) );
    },
    createElement: function(name){
      return makeNode( this._dom.createElement(name.toLowerCase()) );
    },
    getElementsByTagName: function(name){
      return new DOMNodeList( this._dom.getElementsByTagName(
        name.toLowerCase()) );
    },
    getElementById: function(id){
      var elems = this._dom.getElementsByTagName("*");

      for ( var i = 0; i < elems.length; i++ ) {
        var elem = elems.item(i);
        if ( elem.getAttribute("id") == id )
          return makeNode(elem);
      }

      return null;
    },
    get body(){
      return this.getElementsByTagName("body")[0];
    },
    get documentElement(){
      return makeNode( this._dom.getDocumentElement() );
    },
    get ownerDocument(){
      return null;
    },
    addEventListener: window.addEventListener,
    removeEventListener: window.removeEventListener,
    dispatchEvent: window.dispatchEvent,
    get nodeName() {
      return "#document";
    },
    importNode: function(node, deep){
      return makeNode( this._dom.importNode(node._dom, deep) );
    },
    toString: function(){
      return "Document" + (typeof this._file == "string" ?
        ": " + this._file : "");
    },
    get innerHTML(){
      return this.documentElement.outerHTML;
    },
    get title() {
      var titleNode = this.getElementsByTagName("title")[0];
      return titleNode ? titleNode.innerHTML : '';
    },

    get defaultView(){
      return {
        getComputedStyle: function(elem){
          return {
            getPropertyValue: function(prop){
              prop = prop.replace(/\-(\w)/g,function(m,c){
                return c.toUpperCase();
              });
              var val = elem.style[prop];

              if ( prop == "opacity" && val == "" )
                val = "1";

              return val;
            }
          };
        }
      };
    },

    createEvent: function(){
      return {
        type: "",
        initEvent: function(type){
          this.type = type;
        }
      };
    }
  };

  function getDocument(node){
    return obj_nodes.get(node);
  }

  // DOM NodeList

  window.DOMNodeList = function(list){
    this._dom = list;
    if ('scriptEngine___' in window) {
      window.scriptEngine___.dontEnum(this, '_dom');
    }

    this.length = list.getLength();

    for ( var i = 0; i < this.length; i++ ) {
      var node = list.item(i);
      this[i] = makeNode( node );
    }
  };

  DOMNodeList.prototype = {
    toString: function(){
      return "[ " +
        Array.prototype.join.call( this, ", " ) + " ]";
    },
    get outerHTML(){
      return Array.prototype.map.call(
          this, function(node){ return node.outerHTML; }).join('');
    }
  };

  // DOM Node

  window.DOMNode = function(node){
    this._dom = node;
    if ('scriptEngine___' in window) {
      window.scriptEngine___.dontEnum(this, '_dom');
    }
  };

  DOMNode.prototype = {
    get nodeType(){
      return this._dom.getNodeType();
    },
    get nodeValue(){
      return String(this._dom.getNodeValue());
    },
    get nodeName() {
      return String(this._dom.getNodeName());
    },
    cloneNode: function(deep){
      return makeNode( this._dom.cloneNode(deep) );
    },
    get ownerDocument(){
      return getDocument( this._dom.ownerDocument );
    },
    get documentElement(){
      return makeNode( this._dom.documentElement );
    },
    get parentNode() {
      return makeNode( this._dom.getParentNode() );
    },
    get nextSibling() {
      return makeNode( this._dom.getNextSibling() );
    },
    get previousSibling() {
      return makeNode( this._dom.getPreviousSibling() );
    },
    toString: function(){
      return '"' + this.nodeValue + '"';
    },
    get outerHTML(){
      return this.nodeValue
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
    }
  };

  // DOM Element

  window.DOMElement = function(elem){
    this._dom = elem;
    if ('scriptEngine___' in window) {
      window.scriptEngine___.dontEnum(this, '_dom');
    }
  };

  DOMElement.prototype = extend( new DOMNode(), {
    get nodeName(){
      return String(this.tagName.toUpperCase());
    },
    get tagName(){
      return String(this._dom.getTagName());
    },
    toString: function(){
      return "<" + this.tagName + (this.id ? "#" + this.id : "" ) + ">";
    },
    get outerHTML(){
      var ret = "<" + this.tagName, attrs = this._dom.getAttributes();

      for ( var i = 0; i < attrs.getLength(); i++ ) {
        var item = attrs.item(i);
        ret += " " + item.nodeName + '="'
          + String(item.nodeValue)
          .replace(/&/g, '&amp;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
          .replace(/\"/g, '&quot;') + '"';
      }

      if ( this.childNodes.length || this.nodeName == "SCRIPT" )
        ret += ">" + this.childNodes.outerHTML +
          "</" + this.tagName + ">";
      else
        ret += "/>";

      return ret;
    },

    get attributes(){
      var attr = {}, attrs = this._dom.getAttributes();

      for ( var i = 0; i < attrs.getLength(); i++ )
        attr[ attrs.item(i).nodeName ] = String(attrs.item(i).nodeValue);

      return attr;
    },

    get innerHTML(){
      return this.childNodes.outerHTML;
    },
    set innerHTML(html){
      html = html.replace(/<\/?([A-Z]+)/g, function(m){
        return m.toLowerCase();
      });

      var nodes = this.ownerDocument.importNode(
          new DOMDocument(
              scriptEngine___.streamFromString("<wrap>" + html + "</wrap>"))
          .documentElement, true).childNodes;

      while (this.firstChild)
        this.removeChild( this.firstChild );

      for ( var i = 0; i < nodes.length; i++ )
        this.appendChild( nodes[i] );
    },

    get textContent(){
      return nav(this.childNodes);

      function nav(nodes){
        var str = "";
        for ( var i = 0; i < nodes.length; i++ )
          if ( nodes[i].nodeType == 3 )
            str += nodes[i].nodeValue;
          else if ( nodes[i].nodeType == 1 )
            str += nav(nodes[i].childNodes);
        return str;
      }
    },
    set textContent(text){
      while (this.firstChild)
        this.removeChild( this.firstChild );
      this.appendChild( this.ownerDocument.createTextNode(text));
    },

    get style() {
      var thisNode = this;
      var style = {
        get opacity() { return this._opacity; },
        set opacity(val) { this._opacity = val + ""; },
        get cssText() { return thisNode.getAttribute('style') || ''; },
        set cssText(val) { thisNode.setAttribute('style', val + ""); }
      };

      // Load CSS info
      var styles = style.cssText.split(/\s*;\s*/);

      for ( var i = 0; i < styles.length; i++ ) {
        var styleDeclaration = styles[i].split(/\s*:\s*/);
        if ( styleDeclaration.length === 2 ) {
          var k = styleDeclaration[0].replace(
              /-([a-z])/g,
              function (_, letter) { return letter.toUpperCase(); });
          if (k === 'float') { k = 'cssFloat'; }
          style[k] = styleDeclaration[1];
        }
      }
      return style;
    },

    clientHeight: 0,
    clientWidth: 0,
    offsetHeight: 0,
    offsetWidth: 0,

    get disabled() {
      var val = this.getAttribute("disabled");
      return val != "false" && !!val;
    },
    set disabled(val) { return this.setAttribute("disabled",val); },

    get checked() {
      var val = this.getAttribute("checked");
      return val != "false" && !!val;
    },
    set checked(val) { return this.setAttribute("checked",val); },

    get selected() {
      if ( !this._selectDone ) {
        this._selectDone = true;

        if ( this.nodeName == "OPTION" && !this.parentNode.getAttribute("multiple") ) {
          var opt = this.parentNode.getElementsByTagName("option");

          if ( this == opt[0] ) {
            var select = true;

            for ( var i = 1; i < opt.length; i++ )
              if ( opt[i].selected ) {
                select = false;
                break;
              }

            if ( select )
              this.selected = true;
          }
        }
      }

      var val = this.getAttribute("selected");
      return val != "false" && !!val;
    },
    set selected(val) { return this.setAttribute("selected",val); },

    get className() { return this.getAttribute("class") || ""; },
    set className(val) {
      return this.setAttribute("class",
        val.replace(/(^\s*|\s*$)/g,""));
    },

    get type() { return this.getAttribute("type") || ""; },
    set type(val) { return this.setAttribute("type",val); },

    get value() { return this.getAttribute("value") || ""; },
    set value(val) { return this.setAttribute("value",val); },

    get href() {
      if (!this._dom.hasAttribute("href")) { return undefined; }
      return resolveUri(this, this.getAttribute("href"));
    },
    set href(val) { return this.setAttribute("href",val); },

    get src() {
      if (!this._dom.hasAttribute("src")) { return undefined; }
      return resolveUri(this, this.getAttribute("src"));
    },
    set src(val) { return this.setAttribute("src",val); },

    get id() { return this.getAttribute("id") || ""; },
    set id(val) { return this.setAttribute("id",val); },

    getAttributeNode: function(name) {
      return this._dom.getAttributeNode(name);
    },

    getAttribute: function(name){
      name = name.toLowerCase();
      return this._dom.hasAttribute(name) ?
        String( this._dom.getAttribute(name) ) :
        null;
    },
    setAttribute: function(name,value){
      this._dom.setAttribute(name.toLowerCase(),value);
    },
    removeAttribute: function(name){
      this._dom.removeAttribute(name);
    },

    get childNodes(){
      return new DOMNodeList( this._dom.getChildNodes() );
    },
    get firstChild(){
      return makeNode( this._dom.getFirstChild() );
    },
    get lastChild(){
      return makeNode( this._dom.getLastChild() );
    },
    appendChild: function(node){
      this._dom.appendChild( node._dom );
    },
    insertBefore: function(node,before){
      this._dom.insertBefore( node._dom, before ? before._dom : before );
    },
    removeChild: function(node){
      this._dom.removeChild( node._dom );
    },
    replaceChild: function (replacement, child) {
      this._dom.replaceChild(replacement._dom, child._dom);
    },

    getElementsByTagName: DOMDocument.prototype.getElementsByTagName,

    addEventListener: window.addEventListener,
    removeEventListener: window.removeEventListener,
    dispatchEvent: window.dispatchEvent,

    click: function(){
      var event = document.createEvent();
      event.initEvent("click");
      this.dispatchEvent(event);
    },
    submit: function(){
      var event = document.createEvent();
      event.initEvent("submit");
      this.dispatchEvent(event);
    },
    focus: function(){
      var event = document.createEvent();
      event.initEvent("focus");
      this.dispatchEvent(event);
    },
    blur: function(){
      var event = document.createEvent();
      event.initEvent("blur");
      this.dispatchEvent(event);
    },
    get elements(){
      return this.getElementsByTagName("*");
    },
    get contentWindow(){
      return this.nodeName == "IFRAME" ? {
        document: this.contentDocument
      } : null;
    },
    get contentDocument(){
      if ( this.nodeName == "IFRAME" ) {
        if (!this._doc) {
          this._doc = new DOMDocument(scriptEngine___.streamFromString(
              "<html><head><title></title></head><body></body></html>"));
        }
        return this._doc;
      } else
        return null;
    }
  });

  // Helper method for extending one object with another

  function extend(a,b) {
    for ( var i in b ) {
      var g = b.__lookupGetter__(i), s = b.__lookupSetter__(i);

      if ( g || s ) {
        if ( g )
          a.__defineGetter__(i, g);
        if ( s )
          a.__defineSetter__(i, s);
      } else
        a[i] = b[i];
    }
    return a;
  }

  /**
   * Resolves a relative uri to an absolute one in the context of
   * this document.
   * This will resolve relative to any <base> or document.location
   * as appropriate.
   */
  function resolveUri(node, uri) {
    var doc = node.ownerDocument;
    var bases = doc.getElementsByTagName("base");
    var baseUri = null;
    if ( bases.length && bases[0]._dom.hasAttribute("href") ) {
      // Don't access "href" property to avoid inf. recursion
      baseUri = bases[0].getAttribute("href");
    } else {
      baseUri = String(doc.location);
    }
    return String((scriptEngine___.uri(baseUri)).resolve(uri));
  }

  // Helper method for generating the right
  // DOM objects based upon the type

  var obj_nodes = scriptEngine___.weakMap();

  function makeNode(node){
    if (node) {
      if (!obj_nodes.containsKey(node)) {
        obj_nodes.put(
            node,
            node.getNodeType() === 1
            ? new DOMElement(node) : new DOMNode(node));
      }
      return obj_nodes.get(node);
    } else
      return null;
  }

  // XMLHttpRequest
  // Originally implemented by Yehuda Katz

  window.XMLHttpRequest = function(){
    this.headers = {};
    this.responseHeaders = {};
  };

  XMLHttpRequest.prototype = {
    open: function(method, url, async, user, password){
      this.readyState = 1;
      this.async = !!async;
      this.method = method || "GET";
      this.url = url;
      this.onreadystatechange();
    },
    setRequestHeader: function(header, value){
      this.headers[header] = value;
    },
    getResponseHeader: function(header){ },
    send: function(data){
      var self = this;

      function makeRequest(){
        var url = curLocation.resolve(self.url);

        var protocol = url.getScheme();
        var connection = scriptEngine___.openConnection(
            url, self.headers, self.responseHeaders);
        handleResponse();

        function handleResponse(){
          self.readyState = 4;
          self.status = parseInt(connection.status) || undefined;
          self.statusText = connection.statusText || "";
          self.responseText = connection.responseBody;
          self.responseXML = null;
          if ( self.responseText.match(/^\s*</) ) {
            try {
              self.responseXML = new DOMDocument(
                  scriptEngine___.streamFromString(self.responseText));
            } catch(e) {}
          }
        }

        self.onreadystatechange();
      }

      if (this.async)
        setTimeout(makeRequest, 0);
      else
        makeRequest();
    },
    abort: function(){},
    onreadystatechange: function(){},
    getResponseHeader: function(header){
      if (this.readyState < 3)
        throw new Error("INVALID_STATE_ERR");
      else {
        var returnedHeaders = [];
        for (var rHeader in this.responseHeaders) {
          if (rHeader.match(new Regexp(header, "i")))
            returnedHeaders.push(this.responseHeaders[rHeader]);
        }

        if (returnedHeaders.length)
          return returnedHeaders.join(", ");
      }

      return null;
    },
    getAllResponseHeaders: function(header){
      if (this.readyState < 3)
        throw new Error("INVALID_STATE_ERR");
      else {
        var returnedHeaders = [];

        for (var header in this.responseHeaders)
          returnedHeaders.push( header + ": " + this.responseHeaders[header] );

        return returnedHeaders.join("\r\n");
      }
    },
    async: true,
    readyState: 0,
    responseText: "",
    status: 0
  };
})();
