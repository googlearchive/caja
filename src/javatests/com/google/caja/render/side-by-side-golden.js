/*                                                */ ___.loadModule(function (IMPORTS___) {
/***************************************************** test-input3.css *****************************************************/
/* p {                                            */                  ___.defineStyles___('.prefix p {\n  color: purple\n}');
/*   color: purple                                */
/* }                                              */
/**************************************************** test-input2.html *****************************************************/
/* <link rel="stylesheet" href="test-input3.css"> */                  IMPORTS___.htmlEmitter___.ih('\n<h1>Hello\n\n');
/* <h1>Hello                                      */
/***************************************************** test-input1.js ******************************************************/
/* function sep() {                               */                  IMPORTS___.sep = ___.simpleFunc(function sep() {
/*   return '<hr />';                             */                                                    return '<hr />';
/* }                                              */                                                  });
/* if (foo()) { bar(); }                          */                  if (___.asSimpleFunc(IMPORTS___.foo)()) {
/*                                                */                    ___.asSimpleFunc(IMPORTS___.bar)();
/*                                                */                  }
/* if (document) {                                */                  if (IMPORTS___.document) {
/*   document                                     */                    var tmp___ = IMPORTS___.document;
/*       .getElementById                          */                    var tmp0___ = (___.asMethod(tmp, 'getElementById'))
/*       ('x').innerHTML                          */                        ('x');
/*                                                */                    ___.setPub(tmp0___, 'innerHTML',
/*       = sep();                                 */                               ___.asSimpleFunc(sep)());
/* }                                              */                  }
/**************************************************** test-input2.html *****************************************************/
/* <script src="test-input1.js"><@script>         */                  IMPORTS___.htmlEmitter___.ih('\n\nWorld</h1>\n');
/*                                                */                }
/* World<@h1>                                     */
