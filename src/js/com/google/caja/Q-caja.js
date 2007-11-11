// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html

// Adapted from Tyler's original at
// https://vsci.hpl.hp.com/res/ref_send.js in order work on Caja
// Depends on JSON.sj -- the Caja-friendly safe JSON library

var Q = function() {
    function enqueue(task) { setTimeout(task, 0); }

    /** The abstract supertype of all possibly-eventual references. */
    function Promise() {}

    /** A rejected promise. */
    function Rejected(reason) {
        this.reason = reason;
        caja.freeze(this);
    }
    caja.def(Rejected, Promise, {
        toJSON: function() { 
            return {$: this.$, reason: this.reason}; 
        },
        $: ['org.ref_send.promise.Rejected'],
        cast: function() { throw this.reason; },
        when: function(fulfill, reject) {
            if (undefined !== reject) {
                var reason = this.reason;
                enqueue(function() { reject(reason); });
            }
        },
        get: function() { return new Rejected(this.reason); },
        post: function() { return new Rejected(this.reason); }
    });

    /** A fulfilled promise. */
    function Fulfilled(value) {
        this.value = value;
        caja.freeze(this);
    }
    caja.def(Fulfilled, Promise, {
        toJSON: function() { return this.value; },
        $: ['org.ref_send.promise.Fulfilled'],
        cast: function() { return this.value; },
        when: function(fulfill, reject) {
            var value = this.value;
            enqueue(function() { fulfill(value); });
        },
        get: function(noun) {
            var pQ = new Tail();
            var r = new Head(pQ);
            var target = this.value;
            enqueue(function() { 
                r.fulfill("*"===noun ? target : target[noun]); 
            });
            return pQ;
        },
        post: function(verb, argv) {
            var pQ = new Tail();
            var r = new Head(pQ);
            var target = this.value;
            enqueue(function() {
                var f = target[verb];
                if (undefined === f) { return r.resolve(indeterminateQ); }
                var x;
                try {
                    x = f.apply(target, argv);
                } catch (reason) { return r.reject(reason); }
                r.fulfill(x);
            });
            return pQ;
        }
    });

    var indeterminateQ = new Rejected({
        $: ['org.ref_send.promise.Indeterminate']
    });
    function ref(value) {
        if (undefined === value) { return indeterminateQ; }
        if (null === value) { return indeterminateQ; }
        if (Promise === caja.directConstructor(value)) { return value; }
        return new Fulfilled(value);
    }

    // a deferred promise
    function Tail() {
        this.valueQ = indeterminateQ;
        this.observers = [];
    }
    caja.def(Tail, Promise, {
        toJSON: function() { return this.valueQ; },
        cast: function() { return this.valueQ.cast(); },
        when: function(fulfill, reject) {
            if (undefined === this.observers) {
                this.valueQ.when(fulfill, reject);
            } else {
                this.observers.push(function(valueQ) {
                    valueQ.when(fulfill, reject);
                });
            }
        },
        get: function(noun) {
            if (undefined === this.observers || 
                indeterminateQ !== this.valueQ) {
                return this.valueQ.get(noun);
            }
            var pQ = new Tail();
            var r = new Head(pQ);
            this.observers.push(function(valueQ) {
                r.resolve(valueQ.get(noun));
            });
            return pQ;
        },
        post: function(verb, argv) {
            if (undefined === this.observers || 
                indeterminateQ !== this.valueQ) {
                return this.valueQ.post(verb, argv);
            }
            var pQ = new Tail();
            var r = new Head(pQ);
            this.observers.push(function(valueQ) {
                r.resolve(valueQ.post(verb, argv));
            });
            return pQ;
        }
    });

    // a promise resolver
    function Resolver() {}
    caja.def(Resolver, Object, {
        fulfill: function(value) { this.resolve(ref(value)); },
        reject: function(reason) {
            this.resolve(new Rejected(reason));
        }
    });

    // a deferred promise resolver
    function Head(tail) {
        this.tail = tail;
    }
    caja.def(Head, Resolver, {
        resolve: function(valueQ) {
            if (undefined === this.tail) { return; }
            this.tail.valueQ = valueQ;
            var observers = this.tail.observers;
            delete this.tail.observers;
            delete this.tail;
            for (var i = 0; i !== observers.length; ++i) {
                observers[i](valueQ);
            }
        }
    });

    // a remote promise
    function Remote(URL) {
        this['@'] = URL;
    }
    caja.def(Remote, Promise, {
        cast: function() { return this; }
        when: function(fulfill, reject) {
            var proxy = this;
            var urlref = proxy['@'];
            var iFragment = urlref.indexOf('#');
            var url = -1 === iFragment ? urlref :
                urlref.substring(0, iFragment);
            var iQuery = url.indexOf('?src=');
            if (-1 === iQuery) {
                enqueue(function() { fulfill(proxy); });
            } else {
                var i = iQuery + '?src='.length;
                var j = url.indexOf('&', i);
                var src = -1 !== j ? url.substring(i, j) : url.substring(i);
                var path = url.substring(0, iQuery);
                var iFolder = path.lastIndexOf('/') + 1;
                var folder = path.substring(0, iFolder);
                var target = resolveURI(folder, decodeURIComponent(src));
                target += '?s=';
                if (-1 === iFragment) {
                    target += path.substring(iFolder);
                } else {
                    target += urlref.substring(iFragment + 1);
                }
                origin.send(new Message('GET', target, null, function(http) {
                    var base = target.substring(0, target.indexOf('?'));
                    ref(deserialize(base, http)).when(fulfill, reject);
                }));
            }
        },
        get: function(noun) {
            var proxy = this;
            var target = request(proxy['@'], noun);
            var pQ = new Tail();
            var r = new Head(pQ);
            origin.send(new Message('GET', target, null, function(http) {
                if (404 === http.status && -1 !== proxy['@'].indexOf('?src=')) {
                    proxy.when(function(value) {
                        r.resolve(ref(value).get(noun));
                    }, function(reason) { r.reject(reason); });
                } else {
                    var base = target.substring(0, target.indexOf('?'));
                    r.fulfill(deserialize(base, http));
                }
            }));
            return pQ;
        },
        post: function(verb, argv) {
            var proxy = this;
            var target = request(proxy['@'], verb);
            var pQ = new Tail();
            var r = new Head(pQ);
            origin.send(new Message('POST', target, argv, function(http) {
                if (404 === http.status && -1 !== proxy['@'].indexOf('?src=')) {
                    proxy.when(function(value) {
                        r.resolve(ref(value).post(verb, argv));
                    }, function(reason) { r.reject(reason); });
                } else {
                    var base = target.substring(0, target.indexOf('?'));
                    r.fulfill(deserialize(base, http));
                }
            }));
            return pQ;
        }
    });

    function Message(method, URL, argv, receive) {
        this.method = method;
        this.URL = URL;
        this.argv = argv;
        this.receive = receive;
    }
    function makeHost() {
        var http;
        if (window.XMLHttpRequest) {
            http = new XMLHttpRequest();
        } else {
            http = new ActiveXObject('MSXML2.XMLHTTP.3.0');
        }
        var active = false;
        var pending = [ /* Message */ ];
        var output = function() {
            var m = pending[0];
            http.open(m.method, m.URL, true);
            http.onreadystatechange = function() {
                if (4 !== http.readyState) { return; }
                if (m !== pending.shift()) { throw 'problem'; }
                if (0 === pending.length) {
                    active = false;
                } else {
                    enqueue(output);
                }
                m.receive(http);
            };
            if (null === m.argv) {
                http.send(null);
            } else {
                http.setRequestHeader('Content-Type','application/jsonrequest');
                http.send(JSON.serialize(m.argv));
            }
        };

        return caja.freeze({
            send: function(msg) {
                pending.push(msg);
                if (!active) {
                    enqueue(output);
                    active = true;
                }
            }
        });
    }
    var origin = makeHost();

    function resolveURI(base, url) {
        // not complete, but good enough for URLs returned by our server
        if ('' === url) { return base; }
        if (/^[a-zA-Z][\w\-\.\+]*:/.test(url)) { return url; }
        if (/^\//.test(url)) {
            var service = /^.*:\/\/.*\//.exec(base);
            if (null === service) { return url; }
            return service[0] + url.substring(1);
        }
        base = base.substring(0, base.lastIndexOf('/') + 1);
        if (/^\.\//.test(url)) { return base + url.substring(2); }
        while ('../' === url.substring(0, '../'.length)) {
            base = base.substring(0, base.lastIndexOf('/', base.length - 2)+1);
            url = url.substring('../'.length);
        }
        return base + url;
    }
    function deserialize(base, http) {
        if (200 === http.status || 201 === http.status ||
            202 === http.status || 203 === http.status) {
            return http.responseText.parseJSON(function(key, value) {
                if (null === value) { return value; }
                if ('object' !== typeof value) { return value; }
                if (value.hasOwnProperty('@')) {
                    return new Remote(resolveURI(base, value['@']));
                }
                if (value.hasOwnProperty('$')) {
                    var $ = value.$;
                    for (var i = 0; i !== $.length; ++i) {
                        if ($[i] === 'org.ref_send.promise.Rejected') {
                            return new Rejected(value.reason);
                        }
                    }
                }
                return value;
            })[0];
        }
        if (204 === http.status || 205 === http.status) {
            return null;
        }
        if (303 === http.status) {
            var see = http.getResponseHeader('Location');
            return see ? new Remote(see) : null;
        }
        return new Rejected({
            $: [ 'org.waterken.http.Failure',
                 'org.ref_send.promise.Indeterminate' ],
            status: http.status,
            phrase: http.statusText
        });
    }

    function request(urlref, p) {
        var iFragment = urlref.indexOf('#');
        var url = -1 === iFragment ? urlref : urlref.substring(0, iFragment);
        var iQuery = url.indexOf('?');
        var path = -1 === iQuery ? url : url.substring(0, iQuery);
        var iFolder = path.lastIndexOf('/') + 1;
        var folder = path.substring(0, iFolder);
        var target = folder + '?';
        if (undefined !== p) {
            target += 'p=' + encodeURIComponent(p) + '&';
        }
        target += 's=';
        if (-1 === iFragment) {
            target += path.substring(iFolder);
        } else {
            target += urlref.substring(iFragment + 1);
        }
        return target;
    }

    // export the public API
    return caja.freeze({
        enqueue: enqueue,
        ref: ref,
        when: function(value, fulfill, reject) {
            var pQ = new Tail();
            var r = new Head(pQ);
            ref(value).when(function(value) {
                var x;
                try {
                    x = fulfill(value);
                } catch (e) {
                    return r.reject(e);
                }
                return r.fulfill(x);
            }, function(reason) {
                if (undefined === reject) { return r.reject(reason); }
                var x;
                try {
                    x = reject(reason);
                } catch (e) {
                    return r.reject(e);
                }
                return r.fulfill(x);
            });
            return pQ;
        },
        defer: function() {
            var pQ = new Tail();
            return caja.freeze({ promiseQ: pQ, resolver: new Head(pQ) });
        },
        connect: function(URL) { return new Remote(URL); }
    });
}();
