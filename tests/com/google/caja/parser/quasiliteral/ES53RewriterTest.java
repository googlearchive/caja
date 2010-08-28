package com.google.caja.parser.quasiliteral;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.Executor;
import com.google.caja.util.Lists;
import com.google.caja.util.RhinoTestBed;

public class ES53RewriterTest extends CommonJsRewriterTestCase {
  protected class TestUriFetcher implements UriFetcher {
    public FetchedData fetch(ExternalReference ref, String mimeType)
        throws UriFetchException {
      try {
        URI uri = ref.getReferencePosition().source().getUri()
            .resolve(ref.getUri());
        if ("resource".equals(uri.getScheme())) {
          return dataFromResource(uri.getPath(), new InputSource(uri));
        } else {
          throw new UriFetchException(ref, mimeType);
        }
      } catch (IOException ex) {
        throw new UriFetchException(ref, mimeType, ex);
      }
    }
  }

  private Rewriter es53Rewriter;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    es53Rewriter = new ES53Rewriter(TestBuildInfo.getInstance(), mq, false);
    setRewriter(es53Rewriter);
  }

  @Override
  protected Object executePlain(String caja) throws IOException {
    mq.getMessages().clear();
    return RhinoTestBed.runJs(
        new Executor.Input(
            getClass(), "../../../../../js/json_sans_eval/json_sans_eval.js"),
        new Executor.Input(getClass(), "/com/google/caja/es53.js"),
        new Executor.Input(
            getClass(), "../../../../../js/jsunit/2.2/jsUnitCore.js"),
        new Executor.Input(caja, getName() + "-uncajoled"));
  }

  @Override
  protected Object rewriteAndExecute(String pre, String caja, String post)
      throws IOException, ParseException {
    mq.getMessages().clear();

    List<Statement> children = Lists.newArrayList();
    children.add(js(fromString(caja, is)));
    String cajoledJs = render(rewriteTopLevelNode(
        new UncajoledModule(new Block(FilePosition.UNKNOWN, children))));

    assertNoErrors();

    final String[] assertFunctions = new String[] {
        "fail",
        "assertEquals",
        "assertTrue",
        "assertFalse",
        "assertLessThan",
        "assertNull",
        "assertThrows",
    };

    StringBuilder importsSetup = new StringBuilder();
    importsSetup.append(
        "var testImports = ___.copy(___.whitelistAll(___.sharedImports));");
    for (String f : assertFunctions) {
      importsSetup
          .append("testImports." + f + " = ___.markFuncFreeze(" + f + ");")
          .append("___.grantRead(testImports, '" + f + "');");
    }
    importsSetup.append(
        "testImports.handleSet___ = void 0;" +
        "___.getNewModuleHandler().setImports(___.whitelistAll(testImports));");

    Object result = RhinoTestBed.runJs(
        new Executor.Input(
            getClass(), "/com/google/caja/plugin/console-stubs.js"),
        new Executor.Input(
            getClass(), "../../../../../js/json_sans_eval/json_sans_eval.js"),
        new Executor.Input(getClass(), "/com/google/caja/es53.js"),
        new Executor.Input(
            getClass(), "../../../../../js/jsunit/2.2/jsUnitCore.js"),
        new Executor.Input(
            getClass(), "/com/google/caja/log-to-console.js"),
        new Executor.Input(
            importsSetup.toString(),
            getName() + "-test-fixture"),
        new Executor.Input(pre, getName()),
        // Load the cajoled code.
        new Executor.Input(cajoledJs, getName() + "-cajoled"),
        new Executor.Input(post, getName()),
        // Return the output field as the value of the run.
        new Executor.Input(
            "___.getNewModuleHandler().getLastValue();", getName()));

    assertNoErrors();
    return result;
  }
}
