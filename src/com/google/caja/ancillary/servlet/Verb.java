package com.google.caja.ancillary.servlet;

import com.google.caja.util.Maps;

import java.util.Map;

/**
 * The type of operation requested by a browser of the tools servlet.
 *
 * @author mikesamuel@gmail.com
 */
enum Verb {
  DOC("/doc", "generates JSDoc documentation"),
  ECHO("/echo", "echoes the input after optimizing&|minifying"),
  HELP("/help", "shows help info"),
  INDEX("/index", "an interactive HTML form"),
  LINT("/lint", "checks the input for problematic coding practices"),
  ;

  final String requestPath;
  final String manual;

  Verb(String requestPath, String manual) {
    this.requestPath = requestPath;
    this.manual = manual;
  }

  private static final Map<String, Verb> BY_REQUEST_PATH = Maps.newHashMap();
  static {
    for (Verb verb : Verb.values()) {
      BY_REQUEST_PATH.put(verb.requestPath, verb);
    }
  }
  static Verb fromReqPath(String reqPath) {
    return BY_REQUEST_PATH.get(reqPath);
  }

  @Override
  public String toString() { return requestPath; }
}
