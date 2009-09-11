['Howdy\x3cspan id=\"id_1___\"\x3e\x3c/span\x3eThere\x3cscript type=\"text/javascript\"\x3e{
  ___.loadModule({
      \'instantiate\': function (___, IMPORTS___) {
        var moduleResult___ = ___.NO_RESULT;
        var alert = ___.readImport(IMPORTS___, \'alert\');
        var onerror = ___.readImport(IMPORTS___, \'onerror\');
        {
          var el___;
          var emitter___ = IMPORTS___.htmlEmitter___;
          emitter___.discard(emitter___.attach(\'id_1___\'));
        }
        try {
          {
            moduleResult___ = alert.CALL___(2 + 2);
          }
        } catch (ex___) {
          ___.getNewModuleHandler().handleUncaughtException(ex___, onerror,
            \'example.com\', \'1\');
        }
        {
          el___ = emitter___.finish();
          emitter___.signalLoaded();
        }
        return moduleResult___;
      },
      \'includedModules\': [ ],
      \'cajolerName\': \'com.google.caja\',
      \'cajolerVersion\': \'testBuildVersion\',
      \'cajoledDate\': 0
    });
}\x3c/script\x3e','']