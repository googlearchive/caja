// Copyright (C) 2011 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview Make this frame SES-safe or die trying.
 *
 * <p>Assumes ES5 plus a WeakMap that conforms to the anticipated ES6
 * WeakMap spec. Compatible with ES5-strict or anticipated ES6.
 *
 * @author Mark S. Miller,
 * @requires WeakMap
 * @overrides ses, console, eval, Function, cajaVM
 */
var ses;


/**
 * The global {@code eval} function available to script code, which
 * may or not be made safe.
 *
 * <p>The original global binding of {@code eval} is not
 * SES-safe. {@code cajaVM.eval} is a safe wrapper around this
 * original eval, enforcing SES language restrictions.
 *
 * <p>If TAME_GLOBAL_EVAL is true, both the global {@code eval}
 * variable, and the pseudo-global {@code "eval"} property of root,
 * are set to the safe wrapper. If TAME_GLOBAL_EVAL is false, in order
 * to work around a bug in the Chrome debugger, then the global {@code
 * eval} is unaltered and no {@code "eval"} property is available on
 * root. In either case, SES-evaled-code and SES-script-code can both
 * access the safe eval wrapper as {@code cajaVM.eval}.
 *
 * <p>By making the safe eval available on root only when we also make
 * it be the genuine global eval, we preserve the property that
 * SES-evaled-code differs from SES-script-code only by having a
 * subset of the same variables in globalish scope. This is a
 * nice-to-have that makes explanation easier rather than a hard
 * requirement. With this property, any SES-evaled-code that does not
 * fail to access a global variable (or to test whether it could)
 * should operate the same way when run as SES-script-code.
 *
 * <p>See doc-comment on cajaVM for the restriction on this API needed
 * to operate under Caja translation on old browsers.
 */
var eval;

/**
 * The global {@code Function} constructor is always replaced with a
 * safe wrapper, which is also made available as the {@code
 * "Function"} pseudo-global on root.
 *
 * <p>Both the original Function constructor and this safe wrapper
 * point at the original {@code Function.prototype}, so {@code
 * instanceof} works fine with the wrapper. {@code
 * Function.prototype.constructor} is set to point at the safe
 * wrapper, so that only it, and not the unsafe original, is
 * accessible.
 *
 * <p>See doc-comment on cajaVM for the restriction on this API needed
 * to operate under Caja translation on old browsers.
 */
var Function;

/**
 * A new global exported by SES, intended to become a mostly
 * compatible API between server-side Caja translation for older
 * browsers and client-side SES verification for newer browsers.
 *
 * <p>Under server-side Caja translation for old pre-ES5 browsers, the
 * synchronous interface of the evaluation APIs (currently {@code
 * eval, Function, cajaVM.{compile, compileModule, eval, Function}})
 * cannot reasonably be provided. Instead, under translation we expect
 * <ul>
 * <li>Not to have a binding for the pseudo-global {@code "eval"} on root,
 *     just as we would not if TAME_GLOBAL_EVAL is false.
 * <li>The global {@code eval} seen by scripts is either unaltered (to
 *     work around the Chrome debugger bug if TAME_GLOBAL_EVAL is
 *     false), or is replaced by a function that throws an appropriate
 *     EvalError diagnostic (if TAME_GLOBAL_EVAL is true).
 * <li>The global {@code Function} constructor, both as seen by script
 *     code and evaled code, to throw an appropriate diagnostic.
 * <li>The {@code Q} API to always be available, to handle
 *     asyncronous, promise, and remote requests.
 * <li>The evaluating methods on {@code cajaVM} -- currently {@code
 *     compile, compileModule, eval, and Function} -- to be remote
 *     promises for their normal interfaces, which therefore must be
 *     invoked with {@code Q.post}.
 * <li>Since {@code Q.post} can be used for asynchronously invoking
 *     non-promises, invocations like
 *     {@code Q.post(cajaVM, 'eval', ['2+3'])}, for example,
 *     will return a promise for a 5. This will work both under Caja
 *     translation and (TODO(erights)) under SES verification when
 *     {@code Q} is also installed, and so is the only portable
 *     evaluating API that SES code should use during this transition
 *     period.
 * <li>TODO(erights): {code Q.post(cajaVM, 'compileModule',
 *     [moduleSrc]} should eventually pre-load the transitive
 *     synchronous dependencies of moduleSrc before resolving the
 *     promise for its result. It currently does not, instead
 *     requiring its client to do so manually.
 * </ul>
 */
var cajaVM;

/**
 * <p>{@code ses.startSES} should be called before any other potentially
 * dangerous script is executed in this frame.
 *
 * <p>If {@code ses.startSES} succeeds, the evaluation operations on
 * {@code cajaVM}, the global {@code Function} contructor, and perhaps
 * the {@code eval} function (see doc-comment on {@code eval} and
 * {@code cajaVM}) will only load code according to the <i>loader
 * isolation</i> rules of the object-capability model, suitable for
 * loading untrusted code. If all other (trusted) code executed
 * directly in this frame (i.e., other than through these safe
 * evaluation operations) takes care to uphold object-capability
 * rules, then untrusted code loaded via these safe evaluation
 * operations will be constrained by those rules. TODO(erights):
 * explain concretely what the trusted code must do or avoid doing to
 * uphold object-capability rules.
 *
 * <p>On a pre-ES5 platform, this script will fail cleanly, leaving
 * the frame intact. Otherwise, if this script fails, it may leave
 * this frame in an unusable state. All following description assumes
 * this script succeeds and that the browser conforms to the ES5
 * spec. The ES5 spec allows browsers to implement more than is
 * specified as long as certain invariants are maintained. We further
 * assume that these extensions are not maliciously designed to obey
 * the letter of these invariants while subverting the intent of the
 * spec. In other words, even on an ES5 conformant browser, we do not
 * presume to defend ourselves from a browser that is out to get us.
 *
 * @param global ::Record(any) Assumed to be the real global object
 *        for this frame. Since {@code ses.startSES} will allow global
 *        variable references that appear at the top level of the
 *        whitelist, our safety depends on these variables being
 *        frozen as a side effect of freezing the corresponding
 *        properties of {@code global}. These properties are also
 *        duplicated onto the virtual global objects which are
 *        provided as the {@code this} binding for the safe
 *        evaluation calls -- emulating the safe subset of the normal
 *        global object.
 * @param whitelist ::Record(Permit) where Permit = true | "*" |
 *        "skip" | Record(Permit).  Describes the subset of naming
 *        paths starting from the root that should be accessible. The
 *        <i>accessible primordials</i> are all values found by
 *        navigating these paths starting from this root. All
 *        non-whitelisted properties of accessible primordials are
 *        deleted, and then the root and all accessible primordials
 *        are frozen with the whitelisted properties frozen as data
 *        properties. TODO(erights): fix the code and documentation to
 *        also support confined-ES5, suitable for confining
 *        potentially offensive code but not supporting defensive
 *        code, where we skip this last freezing step. With
 *        confined-ES5, each frame is considered a separate protection
 *        domain rather that each individual object.
 * @param atLeastFreeVarNames ::F([string], Record(true))
 *        Given the sourceText for a strict Program,
 *        atLeastFreeVarNames(sourceText) returns a Record whose
 *        enumerable own property names must include the names of all the
 *        free variables occuring in sourceText. It can include as
 *        many other strings as is convenient so long as it includes
 *        these. The value of each of these properties should be
 *        {@code true}. TODO(erights): On platforms with Proxies
 *        (currently only Firefox 4 and after), use {@code
 *        with(aProxy) {...}} to intercept free variables rather than
 *        atLeastFreeVarNames.
 * @param extensions ::F([], Record(any)]) A function returning a
 *        record whose own properties will be copied onto cajaVM. This
 *        is used for the optional components which bring SES to
 *        feature parity with the ES5/3 runtime at the price of larger
 *        code size. At the time that {@code startSES} calls {@code
 *        extensions}, {@code cajaVM} exists but should not yet be
 *        used. In particular, {@code extensions} should not call
 *        {@code cajaVM.def} when called, because def would then
 *        freeze priordials before startSES cleans them (removes
 *        non-whitelisted properties). The methods that
 *        {@code extensions} contributes can, of course, use
 *        {@code cajaVM}, since those methods will only be called once
 *        {@code startSES} finishes.
 */
ses.startSES = function(global, whitelist, atLeastFreeVarNames, extensions) {
  "use strict";


  /////////////// KLUDGE SWITCHES ///////////////

  /////////////////////////////////
  // The following are only the minimal kludges needed for the current
  // Firefox or the current Chrome Beta. At the time of
  // this writing, these are Firefox 4.0 and Chrome 12.0.742.5 dev
  // As these move forward, kludges can be removed until we simply
  // rely on ES5.

  /**
   * <p>TODO(erights): isolate and report this.
   *
   * <p>Workaround for Chrome debugger's own use of 'eval'
   *
   * <p>This kludge is safety preserving but not semantics
   * preserving. When TAME_GLOBAL_EVAL is false, no synchronous 'eval'
   * is available as a pseudo-global to untrusted (eval) code, and the
   * 'eval' available as a global to trusted (script) code is the
   * original 'eval', and so is not safe.
   */
  //var TAME_GLOBAL_EVAL = true;
  var TAME_GLOBAL_EVAL = false;


  //////////////// END KLUDGE SWITCHES ///////////


  var dirty = true;

  var hop = Object.prototype.hasOwnProperty;

  function fail(str) {
    debugger;
    throw new EvalError(str);
  }

  if (typeof WeakMap === 'undefined') {
    fail('No built-in WeakMaps, so WeakMap.js must be loaded first');
  }

  /**
   * Code being eval'ed sees {@code root} as its top-level
   * {@code this}, as if {@code root} were the global object.
   *
   * <p>Root's properties are exactly the whitelisted global variable
   * references. These properties, both as they appear on the global
   * object and on this root object, are frozen and so cannot
   * diverge. This preserves the illusion.
   */
  var root = Object.create(null);

  (function() {

    /**
     * The unsafe* variables hold precious values that must not escape
     * to untrusted code. When {@code eval} is invoked via {@code
     * unsafeEval}, this is a call to the indirect eval function, not
     * the direct eval operator.
     */
    var unsafeEval = eval;
    var UnsafeFunction = Function;

    /**
     * Fails if {@code programSrc} does not parse as a strict Program
     * production, or, almost equivalently, as a FunctionBody
     * production.
     *
     * <p>We use Crock's trick of simply passing {@code programSrc} to
     * the original {@code Function} constructor, which will throw a
     * SyntaxError if it does not parse as a FunctionBody. We used to
     * use Ankur's trick (need link) which is more correct, in that it
     * will throw if {@code programSrc} does not parse as a Program
     * production, which is the relevant question. However, the
     * difference -- whether return statements are accepted -- does
     * not matter for our purposes. And testing reveals that Crock's
     * trick executes over 100x faster on V8.
     */
    function verifyStrictProgram(programSrc) {
      UnsafeFunction('"use strict";' + programSrc);
    }

    /**
     * Fails if {@code exprSource} does not parse as a strict
     * Expression production.
     *
     * <p>To verify that exprSrc parses as a strict Expression, we
     * verify that (when followed by ";") it parses as a strict
     * Program, and that when surrounded with parens it still parses
     * as a strict Program. We place a newline before the terminal
     * token so that a "//" comment cannot suppress the close paren.
     */
    function verifyStrictExpression(exprSrc) {
      verifyStrictProgram(exprSrc + ';');
      verifyStrictProgram('( ' + exprSrc + '\n);');
    }

    /**
     * For all the own properties of {@code from}, copy their
     * descriptors to {@code virtualGlobal}, except that the property
     * added to {@code virtualGlobal} is unconditionally
     * non-enumerable and (for the moment) configurable.
     *
     * <p>By copying descriptors rather than values, any accessor
     * properties of {@code env} become accessors of {@code
     * virtualGlobal} with the same getter and setter. If these do not
     * use their {@code this} value, then the original and any copied
     * properties are effectively joined. (If the getter/setter do use
     * their {@code this}, when compiled (verified untrusted) code
     * accesses these with virtualGlobal as the base, their {@code
     * this} will be bound to the virtualGlobal rather than {@code
     * env}.)
     *
     * <p>We make these configurable so that {@code virtualGlobal} can
     * be further configured before being frozen. We make these
     * non-enumerable in order to emulate the normal behavior of
     * built-in properties of typical global objects, such as the
     * browser's {@code window} object.
     */
    function initGlobalProperties(from, virtualGlobal) {
      Object.getOwnPropertyNames(from).forEach(function(name) {
        var desc = Object.getOwnPropertyDescriptor(from, name);
        desc.enumerable = false;
        desc.configurable = true;
        Object.defineProperty(virtualGlobal, name, desc);
      });
    }

    /**
     * Make a frozen virtual global object whose properties are the
     * union of the whitelisted globals and the own properties of
     * {@code env}.
     *
     * <p>If there is a collision, the property from {@code env}
     * shadows the whitelisted global. We shadow by overwriting rather
     * than inheritance so that shadowing makes the original binding
     * inaccessible.
     */
    function makeVirtualGlobal(env) {
      var virtualGlobal = Object.create(null);
      initGlobalProperties(root, virtualGlobal);
      initGlobalProperties(env, virtualGlobal);
      return Object.freeze(virtualGlobal);
    }

    /**
     * Make a frozen scope object which inherits from
     * {@code virtualGlobal}, for use by {@code with} to prevent
     * access to any {@code freeNames} other than those found on the.
     * {@code virtualGlobal}.
     */
    function makeScopeObject(virtualGlobal, freeNames) {
      var scopeObject = Object.create(virtualGlobal);
      Object.keys(freeNames).forEach(function(name) {
        if (!(name in virtualGlobal)) {
          Object.defineProperty(scopeObject, name, {
            get: function() {
              throw new ReferenceError('"' + name + '" not in scope');
            },
            set: function(ignored) {
              throw new TypeError('Cannot set "' + name + '"');
            }
          });
        }
      });
      return Object.freeze(scopeObject);
    }


    /**
     * Compile {@code exprSrc} as a strict expression into a function
     * of an environment {@code env}, that when called evaluates
     * {@code exprSrc} in a virtual global environment consisting only
     * of the whitelisted globals and a snapshot of the own properties
     * of {@code env}.
     *
     * <p>When SES {@code compile} is provided primitively, it should
     * accept a Program and return a function that evaluates it to the
     * Program's completion value. Unfortunately, this is not
     * practical as a library without some non-standard support from
     * the platform such as a parser API that provides an AST.
     *
     * <p>Thanks to Mike Samuel and Ankur Taly for this trick of using
     * {@code with} together with RegExp matching to intercept free
     * variable access without parsing.
     *
     * <p>TODO(erights): Switch to Erik Corry's suggestion that we
     * bring each of these variables into scope by generating a
     * shadowing declaration, rather than using "with".
     *
     * <p>TODO(erights): Find out if any platforms have any way to
     * associate a file name and line number with eval'ed text, and
     * arrange to pass these through compile and all its relevant
     * callers.
     */
    function compile(exprSrc) {
      if (dirty) { fail('Initial cleaning failed'); }
      verifyStrictExpression(exprSrc);
      var freeNames = atLeastFreeVarNames(exprSrc);

      /**
       * Notice that the source text placed around exprSrc
       * <ul>
       * <li>brings no variable names into scope, avoiding any
       *     non-hygienic name capture issues, and
       * <li>does not introduce any newlines preceding exprSrc, so
       *     that all line number which a debugger might report are
       *     accurate wrt the original source text. And except for the
       *     first line, all the column numbers are accurate too.
       */
      var wrapperSrc =
        '(function() { ' +
        // non-strict code, where this === scopeObject
        '  with (this) { ' +
        '    return function() { ' +
        '      "use strict"; ' +
        '      return ( ' +
        // strict code, where this === virtualGlobal
        '        ' + exprSrc + '\n' +
        '      );\n' +
        '    };\n' +
        '  }\n' +
        '})';
      var wrapper = unsafeEval(wrapperSrc);
      return function(env) {
        var virtualGlobal = makeVirtualGlobal(env);
        var scopeObject = makeScopeObject(virtualGlobal, freeNames);
        return wrapper.call(scopeObject).call(virtualGlobal);
      };
    }

    var directivePattern = (/^['"](?:\w|\s)*['"]$/m);

    /**
     * A stereotyped form of the CommonJS require statement.
     */
    var requirePattern = (/^(?:\w*\s*(?:\w|\$|\.)*\s*=)?\s*require\s*\(\s*['"]((?:\w|\$|\.|\/)+)['"]\s*\)$/m);

    /**
     * As an experiment, recognize a stereotyped prelude of the
     * CommonJS module system.
     */
    function getRequirements(modSrc) {
      var result = [];
      var stmts = modSrc.split(';');
      var stmt;
      var i = 0, ilen = stmts.length;
      for (; i < ilen; i++) {
        stmt = stmts[i].trim();
        if (stmt !== '') {
          if (!directivePattern.test(stmt)) { break; }
        }
      }
      for (; i < ilen; i++) {
        stmt = stmts[i].trim();
        if (stmt !== '') {
          var m = requirePattern.exec(stmt);
          if (!m) { break; }
          result.push(m[1]);
        }
      }
      return Object.freeze(result);
    }

    /**
     * A module source is actually any valid FunctionBody, and thus any valid
     * Program.
     *
     * <p>In addition, in case the module source happens to begin with
     * a streotyped prelude of the CommonJS module system, the
     * function resulting from module compilation has an additional
     * {@code "requirements"} property whose value is a list of the
     * module names being required by that prelude. These requirements
     * are the module's "immediate synchronous dependencies".
     *
     * <p>This {@code "requirements"} property is adequate to
     * bootstrap support for a CommonJS module system, since a loader
     * can first load and compile the transitive closure of an initial
     * module's synchronous depencies before actually executing any of
     * these module functions.
     *
     * <p>With a similarly lightweight RegExp, we should be able to
     * similarly recognize the {@code "load"} syntax of <a href=
     * "http://wiki.ecmascript.org/doku.php?id=strawman:simple_modules#syntax"
     * >Sam and Dave's module proposal for ES-Harmony</a>. However,
     * since browsers do not currently accept this syntax,
     * {@code getRequirements} above would also have to extract these
     * from the text to be compiled.
     */
    function compileModule(modSrc) {
      var moduleMaker = compile('(function() {' + modSrc + '}).call(this)');
      moduleMaker.requirements = getRequirements(modSrc);
      Object.freeze(moduleMaker.prototype);
      return Object.freeze(moduleMaker);
    }

    /**
     * A safe form of the {@code Function} constructor, which
     * constructs strict functions that can only refer freely to the
     * whitelisted globals.
     *
     * <p>The returned function is strict whether or not it declares
     * itself to be.
     */
    function FakeFunction(var_args) {
      var params = [].slice.call(arguments, 0);
      var body = params.pop();
      body = String(body || '');
      params = params.join(',');
      var exprSrc = '(function(' + params + '\n){' + body + '})';
      return compile(exprSrc)({});
    }
    FakeFunction.prototype = UnsafeFunction.prototype;
    FakeFunction.prototype.constructor = FakeFunction;
    global.Function = FakeFunction;

    /**
     * A safe form of the indirect {@code eval} function, which
     * evaluates {@code src} as strict code that can only refer freely
     * to the whitelisted globals.
     *
     * <p>Given our parserless methods of verifying untrusted sources,
     * we unfortunately have no practical way to obtain the completion
     * value of a safely evaluated Program. Instead, we adopt a
     * compromise based on the following observation. All Expressions
     * are valid Programs, and all Programs are valid
     * FunctionBodys. If {@code src} parses as a strict expression,
     * then we evaluate it as an expression it and correctly return its
     * completion value, since that is simply the value of the
     * expression.
     *
     * <p>Otherwise, we evaluate {@code src} as a FunctionBody and
     * return what that would return from its implicit enclosing
     * function. If {@code src} is simply a Program, then it would not
     * have an explicit {@code return} statement, and so we fail to
     * return its completion value. This is sufficient for using
     * {@code eval} to emulate script tags, since script tags also
     * lose the completion value.
     *
     * <p>When SES {@code eval} is provided primitively, it should
     * accept a Program and evaluate it to the Program's completion
     * value. Unfortunately, this is not practical as a library
     * without some non-standard support from the platform such as an
     * parser API that provides an AST.
     */
    function fakeEval(src) {
      try {
        verifyStrictExpression(src);
      } catch (x) {
        src = '(function() {' + src + '\n}).call(this)';
      }
      return compile(src)({});
    }

    if (TAME_GLOBAL_EVAL) {
      global.eval = fakeEval;
    }

    var defended = WeakMap();
    /**
     * To define a defended object is to freeze it and all objects
     * transitively reachable from it via transitive reflective
     * property and prototype traversal.
     */
    function def(node) {
      var defending = WeakMap();
      var defendingList = [];
      function recur(val) {
        if (val !== Object(val) || defended.get(val) || defending.get(val)) {
          return;
        }
        defending.set(val, true);
        defendingList.push(val);
        Object.freeze(val);
        recur(Object.getPrototypeOf(val));
        Object.getOwnPropertyNames(val).forEach(function(p) {
          if (typeof val === 'function' &&
              (p === 'caller' || p === 'arguments')) {
            return;
          }
          var desc = Object.getOwnPropertyDescriptor(val, p);
          recur(desc.value);
          recur(desc.get);
          recur(desc.set);
        });
      }
      recur(node);
      defendingList.forEach(function(obj) {
        defended.set(obj, true);
      });
      return node;
    }

    global.cajaVM = {
      log: function(str) {
        if (typeof console !== 'undefined' && 'log' in console) {
          // We no longer test (typeof console.log === 'function') since,
          // on IE9 and IE10preview, in violation of the ES5 spec, it
          // is callable but has typeof "object". TODO(erights):
          // report to MS.
          console.log(str);
        }
      },
      def: def,
      compile: compile,
      compileModule: compileModule,
      eval: fakeEval,
      Function: FakeFunction
    };
    var extensionsRecord = extensions();
    Object.getOwnPropertyNames(extensionsRecord).forEach(function (p) {
      Object.defineProperty(cajaVM, p,
          Object.getOwnPropertyDescriptor(extensionsRecord, p));
    });

  })();

  var cantNeuter = [];

  /**
   * Read the current value of base[name], and freeze that property as
   * a data property to ensure that all further reads of that same
   * property from that base produce the same value.
   *
   * <p>The algorithms in {@code ses.startSES} traverse the graph of
   * primordials multiple times. These algorithms rely on all these
   * traversals seeing the same graph. By freezing these as data
   * properties the first time they are read, we ensure that all
   * traversals see the same graph.
   *
   * <p>The frozen property should preserve the enumerability of the
   * original property.
   */
  function read(base, name) {
    var desc = Object.getOwnPropertyDescriptor(base, name);
    if (!desc) { return undefined; }
    if ('value' in desc && !desc.writable && !desc.configurable) {
      return desc.value;
    }

    var result = base[name];
    try {
      Object.defineProperty(base, name, {
        value: result, writable: false, configurable: false
      });
    } catch (ex) {
      cantNeuter.push({base: base, name: name, err: ex});
    }
    return result;
  }

  /**
   * Initialize accessible global variables and {@code root}.
   *
   * For each of the whitelisted globals, we {@code read} its value,
   * freeze that global property as a data property, and mirror that
   * property with a frozen data property of the same name and value
   * on {@code root}, but always non-enumerable. We make these
   * non-enumerable since ES5.1 specifies that all these properties
   * are non-enumerable on the global object.
   */
  Object.keys(whitelist).forEach(function(name) {
    var desc = Object.getOwnPropertyDescriptor(global, name);
    if (desc) {
      var permit = whitelist[name];
      if (permit) {
        var value = read(global, name);
        Object.defineProperty(root, name, {
          value: value,
          writable: true,
          enumerable: false,
          configurable: false
        });
      }
    }
  });
  if (TAME_GLOBAL_EVAL) {
    Object.defineProperty(root, 'eval', {
      value: cajaVM.eval,
      writable: true,
      enumerable: false,
      configurable: false
    });
  }

  /**
   * The whiteTable should map from each path-accessible primordial
   * object to the permit object that describes how it should be
   * cleaned.
   *
   * <p>To ensure that each subsequent traversal obtains the same
   * values, these paths become paths of frozen data properties. See
   * the doc-comment on {@code read}.
   *
   * We initialize the whiteTable only so that {@code getPermit} can
   * process "*" and "skip" inheritance using the whitelist, by
   * walking actual superclass chains.
   */
  var whiteTable = WeakMap();
  function register(value, permit) {
    if (value !== Object(value)) { return; }
    if (typeof permit !== 'object') {
      return;
    }
    var oldPermit = whiteTable.get(value);
    if (oldPermit) {
      fail('primordial reachable through multiple paths');
    }
    whiteTable.set(value, permit);
    Object.keys(permit).forEach(function(name) {
      if (permit[name] !== 'skip') {
        var sub = read(value, name);
        register(sub, permit[name]);
      }
    });
  }
  register(root, whitelist);

  /**
   * Should the property named {@code name} be whitelisted on the
   * {@code base} object, and if so, with what Permit?
   *
   * <p>If it should be permitted, return the Permit (where Permit =
   * true | "*" | "skip" | Record(Permit)), all of which are
   * truthy. If it should not be permitted, return false.
   */
  function getPermit(base, name) {
    var permit = whiteTable.get(base);
    if (permit) {
      if (permit[name]) { return permit[name]; }
    }
    while (true) {
      base = Object.getPrototypeOf(base);
      if (base === null) { return false; }
      permit = whiteTable.get(base);
      if (permit && hop.call(permit, name)) {
        var result = permit[name];
        if (result === '*' || result === 'skip') {
          return result;
        } else {
          return false;
        }
      }
    }
  }

  var cleaning = WeakMap();

  var skipped = [];
  var goodDeletions = [];
  var badDeletions = [];

  /**
   * Assumes all super objects are otherwise accessible and so will be
   * independently cleaned.
   */
  function clean(value, prefix) {
    if (value !== Object(value)) { return; }
    if (cleaning.get(value)) { return; }
    cleaning.set(value, true);
    Object.getOwnPropertyNames(value).forEach(function(name) {
      var path = prefix + (prefix ? '.' : '') + name;
      var p = getPermit(value, name);
      if (p) {
        if (p === 'skip') {
          skipped.push(path);
        } else {
          var sub = read(value, name);
          clean(sub, path);
        }
      } else {
        // Strict delete throws on failure, which we can't count on yet
        var success;
        try {
          success = delete value[name];
        } catch (x) {
          success = false;
        }
        if (success) {
          goodDeletions.push(path);
        } else {
          badDeletions.push(path);
        }
      }
    });
    Object.freeze(value);
  }
  clean(root, '');

  function diagnose(severity, desc, problemList) {
    if (problemList.length >= 1) {
      ses.logger.reportDiagnosis(severity, desc, problemList);
      if (severity.level > ses.maxSeverity.level) {
        ses.maxSeverity = severity.level;
      }
    }
  }

  diagnose(ses.severities.SAFE, 'Skipped', skipped);
  diagnose(ses.severities.SAFE, 'Deleted', goodDeletions);

  if (cantNeuter.length >= 1) {
    var complaint = cantNeuter.map(function(p) {
      var desc = Object.getOwnPropertyDescriptor(p.base, p.name);
      if (!desc) {
        return '  Missing ' + p.name;
      }
      return p.name + '(' + p.err + '): ' +
        Object.getOwnPropertyNames(desc).map(function(attrName) {
          var v = desc[attrName];
          if (v === Object(v)) { v = 'a ' + typeof v; }
          return attrName + ': ' + v;
        }).join(', ');

    });
    diagnose(ses.severities.NEW_SYMPTOM, 'Cannot neuter', complaint);
  }

  diagnose(ses.severities.NEW_SYMPTOM, 'Cannot delete', badDeletions);

  if (ses.ok()) {
    // We succeeded. Enable safe Function, eval, and compile to work.
    dirty = false;
    ses.logger.log('initSES succeeded.');
  } else {
    ses.logger.error('initSES failed.');
  }
};
