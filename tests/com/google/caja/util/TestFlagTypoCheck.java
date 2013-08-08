package com.google.caja.util;

import java.util.Collections;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class TestFlagTypoCheck extends Task {
  @Override
  public void execute() throws BuildException {
    StringBuilder err = new StringBuilder();
    Properties props = System.getProperties();
    for (Object key : Collections.list(props.propertyNames())) {
      check(key, err);
    }
    for (Object key : getProject().getProperties().keySet()) {
      check(key, err);
    }
    if (err.length() != 0) {
      throw new BuildException(err.toString());
    }
  }

  private static void check(Object key, StringBuilder err) {
    if (key instanceof String) {
      String name = (String) key;
      if (name.startsWith("caja.test.")) {
        err.append("Obsolete property " + name + "\n");
      }
      if (name.startsWith("test.") && !TestFlag.all().contains(name)) {
        err.append("Unknown property " + name + "\n");
      }
    }
  }
}
