['Howdy\x3cspan id=\"id_1___\"\x3e\x3c/span\x3eThere\x3cscript type=\"text/javascript\"\x3e{
  ___.loadModule({
      \'instantiate\': function (___, IMPORTS___) {
        var moduleResult___ = ___.NO_RESULT;
        var $v = ___.readImport(IMPORTS___, \'$v\', {
            \'getOuters\': { \'()\': {} },
            \'initOuter\': { \'()\': {} },
            \'cf\': { \'()\': {} },
            \'ro\': { \'()\': {} }
          });
        var $dis;
        $dis = $v.getOuters();
        $v.initOuter(\'onerror\');
        {
          var el___;
          var emitter___ = IMPORTS___.htmlEmitter___;
          emitter___.discard(emitter___.attach(\'id_1___\'));
        }
        try {
          {
            moduleResult___ = $v.cf($v.ro(\'alert\'), [ 2 + 2 ]);
          }
        } catch (ex___) {
          ___.getNewModuleHandler().handleUncaughtException(ex___,
            $v.ro(\'onerror\'), \'example.com\', \'1\');
        }
        {
          var el___;
          var emitter___ = IMPORTS___.htmlEmitter___;
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