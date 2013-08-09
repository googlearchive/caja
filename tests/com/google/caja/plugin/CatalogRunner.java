// Copyright (C) 2013 Google Inc.
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

package com.google.caja.plugin;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URL;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import com.google.caja.plugin.BrowserTestCatalog.Entry;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.FailureIsAnOption;


/**
 * JUnit runner for browser test cases from a catalog. Usually used by
 * extending {@link CatalogTestCase}.
 *
 * @author kpreid@switchb.org
 */
public class CatalogRunner extends ParentRunner<Entry> {
  private final BrowserTestCatalog catalog;

  public CatalogRunner(Class<?> testClass) throws InitializationError {
    super(testClass);
    try {
      CatalogName annotation = testClass.getAnnotation(CatalogName.class);
      if (annotation == null) {
        throw new NullPointerException(testClass +
            " does not have CatalogName annotation");
      }
      String catalogName = annotation.value();
      URL url = testClass.getResource(catalogName);
      if (url == null) {
        throw new NullPointerException("resource missing: " + catalogName);
      }
      catalog = BrowserTestCatalog.get(url);
    } catch (Exception e) {
      throw new InitializationError(e);
    }
  }

  @Override
  protected List<Entry> getChildren() {
    return catalog.entries();
  }

  @Override
  protected Description describeChild(Entry entry) {
    return Description.createTestDescription(
        getTestClass().getJavaClass(),
        entry.getLabel(),
        entry.mayFail()
            ? new Annotation[] { new DynamicFailureAnnotation(entry
                .getExpectedFailureReason()) }
            : new Annotation[] {});
  }

  @Override
  protected void runChild(final Entry entry, RunNotifier notifier) {
    Description description = describeChild(entry);
    // Normal test methods are filtered by CajaTestCase.runTest() in JUnit 3
    // style.
    if (!CajaTestCase.isMethodInFilter(entry.getLabel())) {
      notifier.fireTestIgnored(description);
    } else {
      runLeaf(new Statement() {
        @Override
        public void evaluate() throws Throwable {
          CatalogTestCase testObject = (CatalogTestCase) getTestClass()
              .getOnlyConstructor().newInstance();
          testObject.setCatalogEntry(entry);
          testObject.runTest();
        }
      }, description, notifier);
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface CatalogName {
    public String value();
  }

  @SuppressWarnings("all")  // you-are-implementing-an-annotation warning
  private static final class DynamicFailureAnnotation implements Annotation,
      FailureIsAnOption {
    private final String value;
    public DynamicFailureAnnotation(String reason) {
      this.value = reason;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return FailureIsAnOption.class;
    }

    public String value() {
      return value;
    }

    @Override
    public boolean equals(Object obj) {
      // per Annotation#equals doc
      return obj instanceof FailureIsAnOption
          && value().equals(((FailureIsAnOption) obj).value());
    }

    @Override
    public int hashCode() {
      // per Annotation#hashCode doc
      return (127 * "value".hashCode()) ^ value.hashCode();
    }
  }
}