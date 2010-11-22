// Copyright (C) 2007 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.caja.parser.quasiliteral;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import com.google.caja.lexer.CharProducer;
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
import com.google.caja.util.FailureIsAnOption;
import com.google.caja.util.Lists;
import com.google.caja.util.RhinoTestBed;

public class ES53ConformanceTest extends RewriterTestCase {
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

  public final void testEval() throws Exception {
    rewriteAndExecute("assertEquals(1, Number(eval('1;')));");
  }

  @FailureIsAnOption
  // We already assume we're in strict mode, so octal is irrelevant
  public final void test7_8_4_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter07/7.8/7.8.4/7.8.4-1-s.js"));
  }

  public final void test8_7_2_3_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter08/8.7/8.7.2/8.7.2-3-1-s.js"));
  }

  public final void test10_4_2_3_c_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter10/10.4/10.4.2/10.4.2-3-c-1-s.js"));
  }

  public final void test10_4_2_3_c_2_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter10/10.4/10.4.2/10.4.2-3-c-2-s.js"));
  }

  @FailureIsAnOption
  // Uses eval incompatibly
  public final void test10_4_3_1_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter10/10.4/10.4.3/10.4.3-1-1-s.js"));
  }

  @FailureIsAnOption
  // Uses eval incompatibly
  public final void test10_4_3_1_2_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter10/10.4/10.4.3/10.4.3-1-2-s.js"));
  }

  @FailureIsAnOption
  // Uses eval incompatibly
  public final void test10_4_3_1_3_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter10/10.4/10.4.3/10.4.3-1-3-s.js"));
  }

  @FailureIsAnOption
  // Uses eval incompatibly
  public final void test10_4_3_1_4_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter10/10.4/10.4.3/10.4.3-1-4-s.js"));
  }

  public final void test10_4_3_1_5_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter10/10.4/10.4.3/10.4.3-1-5-s.js"));
  }

  public final void test10_6_10_c_ii_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter10/10.6/10.6-10-c-ii-1-s.js"));
  }

  public final void test10_6_10_c_ii_2_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter10/10.6/10.6-10-c-ii-2-s.js"));
  }

  public final void test10_6_13_b_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter10/10.6/10.6-13-b-1-s.js"));
  }

  public final void test10_6_13_b_2_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter10/10.6/10.6-13-b-2-s.js"));
  }

  public final void test10_6_13_b_3_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter10/10.6/10.6-13-b-3-s.js"));
  }

  public final void test10_6_13_c_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter10/10.6/10.6-13-c-1-s.js"));
  }

  public final void test10_6_13_c_2_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter10/10.6/10.6-13-c-2-s.js"));
  }

  public final void test10_6_13_c_3_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter10/10.6/10.6-13-c-3-s.js"));
  }

  public final void test11_1_5_4_4_a_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.1/11.1.5/11.1.5_4-4-a-1-s.js"));
  }

  public final void test11_13_1_1_5_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-1-5-s.js"));
  }

  public final void test11_13_1_1_6_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-1-6-s.js"));
  }

  public final void test11_13_1_1_7_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-1-7-s.js"));
  }

  public final void test11_13_1_4_10_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-10-s.js"));
  }

  public final void test11_13_1_4_11_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-11-s.js"));
  }

  public final void test11_13_1_4_12_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-12-s.js"));
  }

  public final void test11_13_1_4_13_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-13-s.js"));
  }

  public final void test11_13_1_4_14_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-14-s.js"));
  }

  public final void test11_13_1_4_15_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-15-s.js"));
  }

  @FailureIsAnOption
  // Can't prevent writes to numeric properties while allowing
  // Number to be extensible.
  public final void test11_13_1_4_16_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-16-s.js"));
  }

  public final void test11_13_1_4_17_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-17-s.js"));
  }

  public final void test11_13_1_4_18_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-18-s.js"));
  }

  public final void test11_13_1_4_19_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-19-s.js"));
  }

  public final void test11_13_1_4_2_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-2-s.js"));
  }

  public final void test11_13_1_4_20_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-20-s.js"));
  }

  public final void test11_13_1_4_21_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-21-s.js"));
  }

  public final void test11_13_1_4_22_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-22-s.js"));
  }

  public final void test11_13_1_4_23_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-23-s.js"));
  }

  public final void test11_13_1_4_24_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-24-s.js"));
  }

  public final void test11_13_1_4_25_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-25-s.js"));
  }

  public final void test11_13_1_4_26_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-26-s.js"));
  }

  public final void test11_13_1_4_27_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-27-s.js"));
  }

  public final void test11_13_1_4_3_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-3-s.js"));
  }

  public final void test11_13_1_4_4_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-4-s.js"));
  }

  public final void test11_13_1_4_5_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-5-s.js"));
  }

  public final void test11_13_1_4_6_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-6-s.js"));
  }

  public final void test11_13_1_4_7_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-7-s.js"));
  }

  public final void test11_13_1_4_8_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-8-s.js"));
  }

  public final void test11_13_1_4_9_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.13/11.13.1/11.13.1-4-9-s.js"));
  }

  public final void test11_4_1_4_a_3_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.4/11.4.1/11.4.1-4.a-3-s.js"));
  }

  public final void test11_4_1_4_a_4_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.4/11.4.1/11.4.1-4.a-4-s.js"));
  }

  public final void test11_4_1_4_a_9_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.4/11.4.1/11.4.1-4.a-9-s.js"));
  }

  @FailureIsAnOption
  // We fail early on deleting globals and parameters.
  public final void test11_4_1_5_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.4/11.4.1/11.4.1-5-1-s.js"));
  }

  @FailureIsAnOption
  // We fail early on deleting globals and parameters.
  public final void test11_4_1_5_2_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.4/11.4.1/11.4.1-5-2-s.js"));
  }

  @FailureIsAnOption
  // We fail early on deleting globals and parameters.
  public final void test11_4_1_5_3_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.4/11.4.1/11.4.1-5-3-s.js"));
  }

  @FailureIsAnOption
  // We fail early on deleting globals and parameters.
  public final void test11_4_1_5_4_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter11/11.4/11.4.1/11.4.1-5-4-s.js"));
  }

  @FailureIsAnOption
  // We fail early on 'with'.
  public final void test12_10_1_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.10/12.10.1/12.10.1-1-s.js"));
  }

  @FailureIsAnOption
  // We fail early on 'with'.
  public final void test12_10_1_10_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.10/12.10.1/12.10.1-10-s.js"));
  }

  @FailureIsAnOption
  // We fail early on 'with'.
  public final void test12_10_1_12_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.10/12.10.1/12.10.1-12-s.js"));
  }

  @FailureIsAnOption
  // We fail early on 'with'.
  public final void test12_10_1_2_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.10/12.10.1/12.10.1-2-s.js"));
  }

  @FailureIsAnOption
  // We fail early on 'with'.
  public final void test12_10_1_3_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.10/12.10.1/12.10.1-3-s.js"));
  }

  @FailureIsAnOption
  // Function constructor not allowed.
  public final void test12_10_1_4_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.10/12.10.1/12.10.1-4-s.js"));
  }

  @FailureIsAnOption
  // Function constructor not allowed.
  public final void test12_10_1_5_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.10/12.10.1/12.10.1-5-s.js"));
  }

  @FailureIsAnOption
  // We fail early on 'with'.
  public final void test12_10_1_7_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.10/12.10.1/12.10.1-7-s.js"));
  }

  @FailureIsAnOption
  // Function constructor not allowed.
  public final void test12_10_1_8_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.10/12.10.1/12.10.1-8-s.js"));
  }

  @FailureIsAnOption
  // We fail early on 'with'.
  public final void test12_10_1_9_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.10/12.10.1/12.10.1-9-s.js"));
  }

  @FailureIsAnOption
  // Can't assign to 'eval'.
  public final void test12_2_1_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.2/12.2.1/12.2.1-1-s.js"));
  }

  @FailureIsAnOption
  // Can't assign to 'eval'.
  public final void test12_2_1_10_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.2/12.2.1/12.2.1-10-s.js"));
  }

  @FailureIsAnOption
  // Can't assign to 'arguments'.
  public final void test12_2_1_12_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.2/12.2.1/12.2.1-12-s.js"));
  }

  @FailureIsAnOption
  // Can't assign to 'arguments'.
  public final void test12_2_1_13_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.2/12.2.1/12.2.1-13-s.js"));
  }

  @FailureIsAnOption
  // Can't assign to 'eval'.
  public final void test12_2_1_2_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.2/12.2.1/12.2.1-2-s.js"));
  }

  @FailureIsAnOption
  // Can't assign to 'eval'.
  public final void test12_2_1_3_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.2/12.2.1/12.2.1-3-s.js"));
  }

  @FailureIsAnOption
  // Can't assign to 'eval'.
  public final void test12_2_1_4_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.2/12.2.1/12.2.1-4-s.js"));
  }

  @FailureIsAnOption
  // Can't assign to 'eval' or use Function constructor.
  public final void test12_2_1_5_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.2/12.2.1/12.2.1-5-s.js"));
  }

  @FailureIsAnOption
  // Can't assign to 'eval'.
  public final void test12_2_1_6_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.2/12.2.1/12.2.1-6-s.js"));
  }

  @FailureIsAnOption
  // Can't assign to 'eval'.
  public final void test12_2_1_7_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.2/12.2.1/12.2.1-7-s.js"));
  }

  @FailureIsAnOption
  // Can't assign to 'eval'.
  public final void test12_2_1_8_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.2/12.2.1/12.2.1-8-s.js"));
  }

  @FailureIsAnOption
  // Can't assign to 'eval'.
  public final void test12_2_1_9_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter12/12.2/12.2.1/12.2.1-9-s.js"));
  }

  @FailureIsAnOption
  // Can't repeat parameters.
  public final void test13_1_1_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-1-1-s.js"));
  }

  @FailureIsAnOption
  // Can't repeat parameters.
  public final void test13_1_1_2_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-1-2-s.js"));
  }

  @FailureIsAnOption
  // Can't use eval as a parameter.
  public final void test13_1_2_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-2-1-s.js"));
  }

  @FailureIsAnOption
  // Can't use eval as a parameter.
  public final void test13_1_2_2_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-2-2-s.js"));
  }

  @FailureIsAnOption
  // Can't use eval as a parameter.
  public final void test13_1_2_3_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-2-3-s.js"));
  }

  @FailureIsAnOption
  // Can't use arguments as a parameter.
  public final void test13_1_2_4_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-2-4-s.js"));
  }

  @FailureIsAnOption
  // Can't use arguments as a parameter.
  public final void test13_1_2_5_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-2-5-s.js"));
  }

  @FailureIsAnOption
  // Can't use arguments as a parameter.
  public final void test13_1_2_6_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-2-6-s.js"));
  }

  @FailureIsAnOption
  // Can't use arguments as a parameter.
  public final void test13_1_2_7_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-2-7-s.js"));
  }

  @FailureIsAnOption
  // Can't use arguments as a parameter.
  public final void test13_1_2_8_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-2-8-s.js"));
  }

  @FailureIsAnOption
  // Can't use arguments as a function name.
  public final void test13_1_3_10_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-3-10-s.js"));
  }

  @FailureIsAnOption
  // Can't use arguments as a function name.
  public final void test13_1_3_11_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-3-11-s.js"));
  }

  @FailureIsAnOption
  // Can't use arguments as a function name.
  public final void test13_1_3_12_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-3-12-s.js"));
  }

  @FailureIsAnOption
  // Can't use eval as a function name.
  public final void test13_1_3_3_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-3-3-s.js"));
  }

  @FailureIsAnOption
  // Can't use eval as a function name.
  public final void test13_1_3_4_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-3-4-s.js"));
  }

  @FailureIsAnOption
  // Can't use eval as a function name.
  public final void test13_1_3_5_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-3-5-s.js"));
  }

  @FailureIsAnOption
  // Can't use eval as a function name.
  public final void test13_1_3_6_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-3-6-s.js"));
  }

  @FailureIsAnOption
  // Can't use arguments as a function name.
  public final void test13_1_3_9_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter13/13.1/13.1-3-9-s.js"));
  }

  @FailureIsAnOption
  // We swap undefined for USELESS.
  public final void test14_1_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter14/14.1/14.1-1-s.js"));
  }

  @FailureIsAnOption
  // We swap undefined for USELESS.
  public final void test14_1_10_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter14/14.1/14.1-10-s.js"));
  }

  @FailureIsAnOption
  // We swap undefined for USELESS.
  public final void test14_1_11_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter14/14.1/14.1-11-s.js"));
  }

  @FailureIsAnOption
  // We swap undefined for USELESS.
  public final void test14_1_12_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter14/14.1/14.1-12-s.js"));
  }

  @FailureIsAnOption
  // We swap undefined for USELESS.
  public final void test14_1_13_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter14/14.1/14.1-13-s.js"));
  }

  @FailureIsAnOption
  // We swap undefined for USELESS.
  public final void test14_1_14_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter14/14.1/14.1-14-s.js"));
  }

  @FailureIsAnOption
  // We swap undefined for USELESS.
  public final void test14_1_15_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter14/14.1/14.1-15-s.js"));
  }

  @FailureIsAnOption
  // We swap undefined for USELESS.
  public final void test14_1_2_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter14/14.1/14.1-2-s.js"));
  }

  @FailureIsAnOption
  // We swap undefined for USELESS.
  public final void test14_1_8_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter14/14.1/14.1-8-s.js"));
  }

  @FailureIsAnOption
  // We swap undefined for USELESS.
  public final void test14_1_9_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter14/14.1/14.1-9-s.js"));
  }

  @FailureIsAnOption
  // Can't use Function constructor.
  public final void test15_3_2_1_11_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter15/15.3/15.3.2/15.3.2.1/15.3.2.1-11-1-s.js"));
  }

  @FailureIsAnOption
  // Can't use Function constructor.
  public final void test15_3_2_1_11_2_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter15/15.3/15.3.2/15.3.2.1/15.3.2.1-11-2-s.js"));
  }

  @FailureIsAnOption
  // Can't use Function constructor.
  public final void test15_3_2_1_11_3_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter15/15.3/15.3.2/15.3.2.1/15.3.2.1-11-3-s.js"));
  }

  @FailureIsAnOption
  // Can't use Function constructor.
  public final void test15_3_2_1_11_4_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter15/15.3/15.3.2/15.3.2.1/15.3.2.1-11-4-s.js"));
  }

  @FailureIsAnOption
  // Can't use Function constructor.
  public final void test15_3_2_1_11_5_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter15/15.3/15.3.2/15.3.2.1/15.3.2.1-11-5-s.js"));
  }

  @FailureIsAnOption
  // Can't use Function constructor.
  public final void test15_3_2_1_11_6_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter15/15.3/15.3.2/15.3.2.1/15.3.2.1-11-6-s.js"));
  }

  @FailureIsAnOption
  // Can't use Function constructor.
  public final void test15_3_2_1_11_7_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter15/15.3/15.3.2/15.3.2.1/15.3.2.1-11-7-s.js"));
  }

  @FailureIsAnOption
  // We swap undefined for USELESS.
  public final void test15_4_4_16_5_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter15/15.4/15.4.4/15.4.4.16/15.4.4.16-5-1-s.js"));
  }

  @FailureIsAnOption
  // We swap undefined for USELESS.
  public final void test15_4_4_17_5_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter15/15.4/15.4.4/15.4.4.17/15.4.4.17-5-1-s.js"));
  }

  @FailureIsAnOption
  // We swap undefined for USELESS.
  public final void test15_4_4_18_5_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter15/15.4/15.4.4/15.4.4.18/15.4.4.18-5-1-s.js"));
  }

  @FailureIsAnOption
  // We swap undefined for USELESS.
  public final void test15_4_4_19_5_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter15/15.4/15.4.4/15.4.4.19/15.4.4.19-5-1-s.js"));
  }

  @FailureIsAnOption
  // We swap undefined for USELESS.
  public final void test15_4_4_20_5_1_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter15/15.4/15.4.4/15.4.4.20/15.4.4.20-5-1-s.js"));
  }

  @FailureIsAnOption
  // We swap null for USELESS.
  public final void test15_4_4_21_9_c_ii_4_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter15/15.4/15.4.4/15.4.4.21/15.4.4.21-9-c-ii-4-s.js"));
  }

  @FailureIsAnOption
  // We swap null for USELESS.
  public final void test15_4_4_22_9_c_ii_4_s() throws Exception {
    rewriteAndExecute(fromResource("../../../../../js/es5conform/" +
        "TestCases/./chapter15/15.4/15.4.4/15.4.4.22/15.4.4.22-9-c-ii-4-s.js"));
  }

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

  public class Caja {
    public Caja() {}
    public String cajole(String source) throws ParseException {
      List<Statement> children = Lists.newArrayList();
      children.add(js(fromString(source, is)));
      String cajoledJs = render(rewriteTopLevelNode(
          new UncajoledModule(new Block(FilePosition.UNKNOWN, children))));
      // TODO: return code to construct the appropriate error object when there
      // are errors during translation.
      return cajoledJs;
    }
  }

  @Override
  protected Object rewriteAndExecute(String pre, String caja, String post) 
      throws ParseException, IOException {
    return rewriteAndExecute(null, pre, caja, post);
  }

  protected Object rewriteAndExecute(CharProducer testPath) 
      throws ParseException, IOException {
    return rewriteAndExecute(testPath, "", null, "");
  }

  protected Object rewriteAndExecute(
      CharProducer testPath,
      String pre,
      String caja,
      String post)
      throws IOException, ParseException {
    mq.getMessages().clear();

    List<Statement> children = Lists.newArrayList();
    children.add(js(
        testPath == null ?
        fromString(caja, is) : 
        testPath));
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
        "___.getNewModuleHandler().setImports(___.whitelistAll(testImports));");
    StringBuilder es5Harness = new StringBuilder();
    es5Harness.append("testImports.w___('ES5Harness', ___.iM(['registerTest',")
              .append("    ___.markFunc(function(test) {")
              .append("  if (!test.test.f___(null,[])) {")
              .append("    throw new Error(test.description);")
              .append("  }")
              .append("})]));")
              .append("testImports.w___('eval', ")
              .append("  ___.markFunc(function (source) {")
              .append("    return eval(''+caja___.cajole(source));")
              .append("  }));");

    Object result = RhinoTestBed.runJs(
        new Caja(),
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
            new Executor.Input(
                es5Harness.toString(),
                getName() + "-harness"),
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
