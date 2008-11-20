{
  ___.loadModule(function (___, IMPORTS___) {
                   var moduleResult___ = ___.NO_RESULT;
                   var Array = ___.readImport(IMPORTS___, 'Array');
                   var Object = ___.readImport(IMPORTS___, 'Object');
                   var ReferenceError = ___.readImport(IMPORTS___, 'ReferenceError',{
                                                       });
                   var RegExp = ___.readImport(IMPORTS___, 'RegExp',{
                                               });
                   var TypeError = ___.readImport(IMPORTS___, 'TypeError',{
                                                  });
                   var cajita = ___.readImport(IMPORTS___, 'cajita',{
                                                 'construct': {
                                                   '()': {
                                                   }
                                                 },
                                                 'getFuncCategory': {
                                                   '()': {
                                                   }
                                                 },
                                                 'directConstructor': {
                                                   '()': {
                                                   }
                                                 },
                                                 'beget': {
                                                   '()': {
                                                   }
                                                 },
                                                 'inheritsFrom': {
                                                   '()': {
                                                   }
                                                 },
                                                 'getSuperCtor': {
                                                   '()': {
                                                   }
                                                 },
                                                 'readOwn': {
                                                   '()': {
                                                   }
                                                 },
                                                 'freeze': {
                                                   '()': {
                                                   }
                                                 },
                                                 'newTable': {
                                                   '()': {
                                                   }
                                                 },
                                                 'enforceType': {
                                                   '()': {
                                                   }
                                                 },
                                                 'getProtoPropertyValue': {
                                                   '()': {
                                                   }
                                                 },
                                                 'forAllKeys': {
                                                   '()': {
                                                   }
                                                 },
                                                 'getProtoPropertyNames': {
                                                   '()': {
                                                   }
                                                 },
                                                 'USELESS': {
                                                 },
                                                 'getOwnPropertyNames': {
                                                   '()': {
                                                   }
                                                 }
                                               });
                   var loader = ___.readImport(IMPORTS___, 'loader');
                   var valijaMaker = ___.frozenFunc(function (outers) {
                                                      var x0___;
                                                      var x1___;
                                                      function getShadow(func) {
                                                        cajita.enforceType(func, 'function');
                                                        var cat = cajita.getFuncCategory(func);
                                                        var result = myPOE.get_canCall___? myPOE.get(cat): ___.callPub(myPOE, 'get', [ cat ]);
                                                        if (void 0 === result) {
                                                          result = cajita.beget(DisfunctionPrototype);
                                                          var parentFunc = cajita.getSuperCtor(func);
                                                          var parentShadow;
                                                          if (___.typeOf(parentFunc) === 'function') {
                                                            parentShadow = getShadow.CALL___(parentFunc);
                                                          } else {
                                                            parentShadow = ObjectShadow;
                                                          }
                                                          var proto = cajita.beget(parentShadow.prototype_canRead___? parentShadow.prototype: ___.readPub(parentShadow, 'prototype'));
                                                          result.prototype_canSet___? (result.prototype = proto): ___.setPub(result, 'prototype', proto);
                                                          proto.constructor_canSet___? (proto.constructor = func): ___.setPub(proto, 'constructor', func);
                                                          var statics = cajita.getOwnPropertyNames(func);
                                                          for (var i = 0; i < (statics.length_canRead___? statics.length: ___.readPub(statics, 'length')); i++) {
                                                            var k = ___.readPub(statics, i);
                                                            if (k !== 'valueOf') {
                                                              ___.setPub(result, k, ___.readPub(func, k));
                                                            }
                                                          }
                                                          var meths = cajita.getProtoPropertyNames(func);
                                                          for (var i = 0; i < (meths.length_canRead___? meths.length: ___.readPub(meths, 'length')); i++) {
                                                            var k = ___.readPub(meths, i);
                                                            if (k !== 'valueOf') {
                                                              var v = cajita.getProtoPropertyValue(func, k);
                                                              if (___.typeOf(v) === 'object' && v !== null && ___.typeOf(v.call_canRead___? v.call: ___.readPub(v, 'call')) === 'function') {
                                                                v = dis.CALL___(v.call_canRead___? v.call: ___.readPub(v, 'call'), k);
                                                              }
                                                              ___.setPub(proto, k, v);
                                                            }
                                                          }
                                                          myPOE.set_canCall___? myPOE.set(cat, result): ___.callPub(myPOE, 'set', [ cat, result ]);
                                                        }
                                                        return result;
                                                      }
                                                      ___.func(getShadow, 'getShadow');
                                                      function getFakeProtoOf(func) {
                                                        if (___.typeOf(func) === 'function') {
                                                          var shadow = getShadow.CALL___(func);
                                                          return shadow.prototype_canRead___? shadow.prototype: ___.readPub(shadow, 'prototype');
                                                        } else if (___.typeOf(func) === 'object' && func !== null) {
                                                          return func.prototype_canRead___? func.prototype: ___.readPub(func, 'prototype');
                                                        } else {
                                                          return void 0;
                                                        }
                                                      }
                                                      ___.func(getFakeProtoOf, 'getFakeProtoOf');
                                                      function typeOf(obj) {
                                                        var result = ___.typeOf(obj);
                                                        if (result !== 'object') {
                                                          return result;
                                                        }
                                                        if (null === obj) {
                                                          return result;
                                                        }
                                                        if (cajita.inheritsFrom(obj, DisfunctionPrototype)) {
                                                          return 'function';
                                                        }
                                                        return result;
                                                      }
                                                      ___.func(typeOf, 'typeOf');
                                                      function instanceOf(obj, func) {
                                                        if (___.typeOf(func) === 'function' && obj instanceof func) {
                                                          return true;
                                                        } else {
                                                          return cajita.inheritsFrom(obj, getFakeProtoOf.CALL___(func));
                                                        }
                                                      }
                                                      ___.func(instanceOf, 'instanceOf');
                                                      function read(obj, name) {
                                                        var result = cajita.readOwn(obj, name, pumpkin);
                                                        if (result !== pumpkin) {
                                                          return result;
                                                        }
                                                        if (___.typeOf(obj) === 'function') {
                                                          return ___.readPub(getShadow.CALL___(obj), name);
                                                        }
                                                        if (obj === null || obj === void 0) {
                                                          throw ___.construct(TypeError, [ 'Cannot read property \"' + name + '\" from ' + obj ]);
                                                        }
                                                        if (___.inPub(name, obj)) {
                                                          return ___.readPub(obj, name);
                                                        }
                                                        var stepParent = getFakeProtoOf.CALL___(cajita.directConstructor(obj));
                                                        if (stepParent !== void 0 && ___.inPub(name, stepParent) && name !== 'valueOf') {
                                                          return ___.readPub(stepParent, name);
                                                        }
                                                        return ___.readPub(obj, name);
                                                      }
                                                      ___.func(read, 'read');
                                                      function set(obj, name, newValue) {
                                                        if (___.typeOf(obj) === 'function') {
                                                          ___.setPub(getShadow.CALL___(obj), name, newValue);
                                                        } else {
                                                          ___.setPub(obj, name, newValue);
                                                        }
                                                        return newValue;
                                                      }
                                                      ___.func(set, 'set');
                                                      function callFunc(func, args) {
                                                        var x0___;
                                                        return x0___ = cajita.USELESS, func.apply_canCall___? func.apply(x0___, args): ___.callPub(func, 'apply', [ x0___, args ]);
                                                      }
                                                      ___.func(callFunc, 'callFunc');
                                                      function callMethod(obj, name, args) {
                                                        var m = read.CALL___(obj, name);
                                                        if (!m) {
                                                          throw ___.construct(TypeError, [ 'callMethod: ' + obj + ' has no method ' + name ]);
                                                        }
                                                        return m.apply_canCall___? m.apply(obj, args): ___.callPub(m, 'apply', [ obj, args ]);
                                                      }
                                                      ___.func(callMethod, 'callMethod');
                                                      function construct(ctor, args) {
                                                        if (___.typeOf(ctor) === 'function') {
                                                          return cajita.construct(ctor, args);
                                                        }
                                                        var result = cajita.beget(ctor.prototype_canRead___? ctor.prototype: ___.readPub(ctor, 'prototype'));
                                                        var altResult = ctor.apply_canCall___? ctor.apply(result, args): ___.callPub(ctor, 'apply', [ result, args ]);
                                                        switch (___.typeOf(altResult)) {
                                                        case 'object':
                                                          {
                                                            if (null !== altResult) {
                                                              return altResult;
                                                            }
                                                            break;
                                                          }
                                                        case 'function':
                                                          {
                                                            return altResult;
                                                          }
                                                        }
                                                        return result;
                                                      }
                                                      ___.func(construct, 'construct');
                                                      function dis(callFn, opt_name) {
                                                        var x0___;
                                                        var x1___;
                                                        var x2___;
                                                        var x3___;
                                                        cajita.enforceType(callFn, 'function');
                                                        var result = cajita.beget(DisfunctionPrototype);
                                                        result.call_canSet___? (result.call = callFn): ___.setPub(result, 'call', callFn);
                                                        x0___ = ___.frozenFunc(function (self, args) {
                                                                                 var x0___;
                                                                                 var x1___;
                                                                                 var x2___;
                                                                                 var x3___;
                                                                                 return x0___ = cajita.USELESS, x3___ = (x1___ = [ self ], x2___ = Array.slice(args, 0), x1___.concat_canCall___? x1___.concat(x2___): ___.callPub(x1___, 'concat', [ x2___ ])), callFn.apply_canCall___? callFn.apply(x0___, x3___): ___.callPub(callFn, 'apply', [ x0___, x3___ ]);
                                                                               }), result.apply_canSet___? (result.apply = x0___): ___.setPub(result, 'apply', x0___);
                                                        x1___ = cajita.beget(ObjectPrototype), result.prototype_canSet___? (result.prototype = x1___): ___.setPub(result, 'prototype', x1___);
                                                        x2___ = result.prototype_canRead___? result.prototype: ___.readPub(result, 'prototype'), x2___.constructor_canSet___? (x2___.constructor = result): ___.setPub(x2___, 'constructor', result);
                                                        x3___ = (callFn.length_canRead___? callFn.length: ___.readPub(callFn, 'length')) - 1, result.length_canSet___? (result.length = x3___): ___.setPub(result, 'length', x3___);
                                                        if (opt_name !== void 0 && opt_name !== '') {
                                                          result.name_canSet___? (result.name = opt_name): ___.setPub(result, 'name', opt_name);
                                                        }
                                                        return result;
                                                      }
                                                      ___.func(dis, 'dis');
                                                      var x2___;
                                                      var x3___;
                                                      var x4___;
                                                      function getOuters() {
                                                        cajita.enforceType(outers, 'object');
                                                        return outers;
                                                      }
                                                      ___.func(getOuters, 'getOuters');
                                                      function readOuter(name) {
                                                        var result = cajita.readOwn(outers, name, pumpkin);
                                                        if (result !== pumpkin) {
                                                          return result;
                                                        }
                                                        if (canReadRev.CALL___(name, outers)) {
                                                          return read.CALL___(outers, name);
                                                        } else {
                                                          throw ___.construct(ReferenceError, [ 'not found: ' + name ]);
                                                        }
                                                      }
                                                      ___.func(readOuter, 'readOuter');
                                                      function readOuterSilent(name) {
                                                        if (canReadRev.CALL___(name, outers)) {
                                                          return read.CALL___(outers, name);
                                                        } else {
                                                          return void 0;
                                                        }
                                                      }
                                                      ___.func(readOuterSilent, 'readOuterSilent');
                                                      function setOuter(name, val) {
                                                        return ___.setPub(outers, name, val);
                                                      }
                                                      ___.func(setOuter, 'setOuter');
                                                      function initOuter(name) {
                                                        if (canReadRev.CALL___(name, outers)) {
                                                          return;
                                                        }
                                                        set.CALL___(outers, name, void 0);
                                                      }
                                                      ___.func(initOuter, 'initOuter');
                                                      function remove(obj, name) {
                                                        if (___.typeOf(obj) === 'function') {
                                                          var shadow = getShadow.CALL___(obj);
                                                          return ___.deletePub(shadow, name);
                                                        } else {
                                                          return ___.deletePub(obj, name);
                                                        }
                                                      }
                                                      ___.func(remove, 'remove');
                                                      function keys(obj) {
                                                        var result = [ ];
                                                        cajita.forAllKeys(obj, ___.frozenFunc(function (name) {
                                                                                                result.push_canCall___? result.push(name): ___.callPub(result, 'push', [ name ]);
                                                                                              }));
                                                        cajita.forAllKeys(getSupplement.CALL___(obj), ___.frozenFunc(function (name) {
                                                                                                                       if (!___.inPub(name, obj) && name !== 'constructor') {
                                                                                                                         result.push_canCall___? result.push(name): ___.callPub(result, 'push', [ name ]);
                                                                                                                       }
                                                                                                                     }));
                                                        return result;
                                                      }
                                                      ___.func(keys, 'keys');
                                                      function canReadRev(name, obj) {
                                                        if (___.inPub(name, obj)) {
                                                          return true;
                                                        }
                                                        return ___.inPub(name, getSupplement.CALL___(obj));
                                                      }
                                                      ___.func(canReadRev, 'canReadRev');
                                                      function getSupplement(obj) {
                                                        if (___.typeOf(obj) === 'function') {
                                                          return getShadow.CALL___(obj);
                                                        } else {
                                                          var ctor = cajita.directConstructor(obj);
                                                          return getFakeProtoOf.CALL___(ctor);
                                                        }
                                                      }
                                                      ___.func(getSupplement, 'getSupplement');
                                                      var ObjectPrototype = ___.initializeMap([ 'constructor', Object ]);
                                                      var DisfunctionPrototype = cajita.beget(ObjectPrototype);
                                                      var Disfunction = cajita.beget(DisfunctionPrototype);
                                                      Disfunction.prototype_canSet___? (Disfunction.prototype = DisfunctionPrototype): ___.setPub(Disfunction, 'prototype', DisfunctionPrototype), Disfunction.length_canSet___? (Disfunction.length = 1): ___.setPub(Disfunction, 'length', 1);
                                                      DisfunctionPrototype.constructor_canSet___? (DisfunctionPrototype.constructor = Disfunction): ___.setPub(DisfunctionPrototype, 'constructor', Disfunction);
                                                      outers.Function_canSet___? (outers.Function = Disfunction): ___.setPub(outers, 'Function', Disfunction);
                                                      var ObjectShadow = cajita.beget(DisfunctionPrototype);
                                                      ObjectShadow.prototype_canSet___? (ObjectShadow.prototype = ObjectPrototype): ___.setPub(ObjectShadow, 'prototype', ObjectPrototype);
                                                      var FuncHeader = ___.construct(RegExp, [ '^\\s*function\\s*([^\\s\\(]*)\\s*\\(' + '(?:\\$dis,?\\s*)?' + '([^\\)]*)\\)' ]);
                                                      x0___ = dis.CALL___(___.frozenFunc(function ($dis) {
                                                                                           var callFn = $dis.call_canRead___? $dis.call: ___.readPub($dis, 'call');
                                                                                           if (callFn) {
                                                                                             var printRep = callFn.toString_canCall___? callFn.toString(): ___.callPub(callFn, 'toString', [ ]);
                                                                                             var match = FuncHeader.exec_canCall___? FuncHeader.exec(printRep): ___.callPub(FuncHeader, 'exec', [ printRep ]);
                                                                                             if (null !== match) {
                                                                                               var name = $dis.name_canRead___? $dis.name: ___.readPub($dis, 'name');
                                                                                               if (name === void 0) {
                                                                                                 name = ___.readPub(match, 1);
                                                                                               }
                                                                                               return 'function ' + name + '(' + ___.readPub(match, 2) + ') {\n  [cajoled code]\n}';
                                                                                             }
                                                                                             return printRep;
                                                                                           }
                                                                                           return 'disfunction(var_args){\n   [cajoled code]\n}';
                                                                                         }), 'toString'), DisfunctionPrototype.toString_canSet___? (DisfunctionPrototype.toString = x0___): ___.setPub(DisfunctionPrototype, 'toString', x0___);
                                                      outers.Function_canSet___? (outers.Function = Disfunction): ___.setPub(outers, 'Function', Disfunction);
                                                      var myPOE = cajita.newTable();
                                                      x1___ = cajita.getFuncCategory(Object), myPOE.set_canCall___? myPOE.set(x1___, ObjectShadow): ___.callPub(myPOE, 'set', [ x1___, ObjectShadow ]);
                                                      ;
                                                      ;
                                                      ;
                                                      ;
                                                      var pumpkin = ___.initializeMap([ ]);
                                                      ;
                                                      ;
                                                      ;
                                                      ;
                                                      ;
                                                      ;
                                                      x2___ = dis.CALL___(___.frozenFunc(function ($dis, self, var_args) {
                                                                                           var a___ = ___.args(arguments);
                                                                                           var x0___;
                                                                                           return x0___ = Array.slice(a___, 2), $dis.apply_canCall___? $dis.apply(self, x0___): ___.callPub($dis, 'apply', [ self, x0___ ]);
                                                                                         }), 'call'), DisfunctionPrototype.call_canSet___? (DisfunctionPrototype.call = x2___): ___.setPub(DisfunctionPrototype, 'call', x2___);
                                                      x3___ = dis.CALL___(___.frozenFunc(function ($dis, self, args) {
                                                                                           return $dis.apply_canCall___? $dis.apply(self, args): ___.callPub($dis, 'apply', [ self, args ]);
                                                                                         }), 'apply'), DisfunctionPrototype.apply_canSet___? (DisfunctionPrototype.apply = x3___): ___.setPub(DisfunctionPrototype, 'apply', x3___);
                                                      x4___ = dis.CALL___(___.frozenFunc(function ($dis, self, var_args) {
                                                                                           var a___ = ___.args(arguments);
                                                                                           var leftArgs = Array.slice(a___, 2);
                                                                                           return ___.frozenFunc(function (var_args) {
                                                                                                                   var a___ = ___.args(arguments);
                                                                                                                   var x0___;
                                                                                                                   var x1___;
                                                                                                                   return x1___ = (x0___ = Array.slice(a___, 0), leftArgs.concat_canCall___? leftArgs.concat(x0___): ___.callPub(leftArgs, 'concat', [ x0___ ])), $dis.apply_canCall___? $dis.apply(self, x1___): ___.callPub($dis, 'apply', [ self, x1___ ]);
                                                                                                                 });
                                                                                         }), 'bind'), DisfunctionPrototype.bind_canSet___? (DisfunctionPrototype.bind = x4___): ___.setPub(DisfunctionPrototype, 'bind', x4___);
                                                      ;
                                                      ;
                                                      ;
                                                      ;
                                                      ;
                                                      ;
                                                      ;
                                                      ;
                                                      ;
                                                      return cajita.freeze(___.initializeMap([ 'typeOf', ___.primFreeze(typeOf), 'instanceOf', ___.primFreeze(instanceOf), 'r', ___.primFreeze(read), 's', ___.primFreeze(set), 'cf', ___.primFreeze(callFunc), 'cm', ___.primFreeze(callMethod), 'construct', ___.primFreeze(construct), 'getOuters', ___.primFreeze(getOuters), 'ro', ___.primFreeze(readOuter), 'ros', ___.primFreeze(readOuterSilent), 'so', ___.primFreeze(setOuter), 'initOuter', ___.primFreeze(initOuter), 'remove', ___.primFreeze(remove), 'keys', ___.primFreeze(keys), 'canReadRev', ___.primFreeze(canReadRev), 'dis', ___.primFreeze(dis) ]));
                                                    });
                   if (___.typeOf(loader) !== 'undefined') {
                     moduleResult___ = loader.provide_canCall___? loader.provide(valijaMaker): ___.callPub(loader, 'provide', [ valijaMaker ]);
                   }
                   return moduleResult___;
                 });
}