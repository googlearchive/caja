package com.google.caja.ancillary.servlet;

import com.google.caja.util.Maps;

import java.util.Map;

/**
 * The type of operation requested by a browser of the tools servlet.
 *
 * @author mikesamuel@gmail.com
 */
enum Verb {
  DOC("doc", "generates JSDoc documentation"),
  ECHO("echo", "echoes the input after optimizing&|minifying"),
  HELP("help", "shows help info"),
  INDEX("index", "an interactive HTML form"),
  LINT("lint", "checks the input for problematic coding practices"),
  ;

  /** Relative request path. */
  final String relRequestPath;
  /** Human readable description. */
  final String manual;

  Verb(String relRequestPath, String manual) {
    this.relRequestPath = relRequestPath;
    this.manual = manual;
  }

  private static final Map<String, Verb> BY_REQUEST_PATH = Maps.newHashMap();
  static {
    for (Verb verb : Verb.values()) {
      BY_REQUEST_PATH.put(verb.relRequestPath, verb);
    }
  }
  /** @param relReqPath relative request path. */
  static Verb fromRelReqPath(String relReqPath) {
    return BY_REQUEST_PATH.get(relReqPath);
  }

  @Override
  public String toString() { return relRequestPath; }
}
