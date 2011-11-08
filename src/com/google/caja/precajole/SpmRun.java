package com.google.caja.precajole;

import com.google.caja.parser.js.CajoledModule;

public class SpmRun {
  private static final String JQUERY =
      "http://code.jquery.com/jquery-1.6.4.js";

  public static void main(String[] args) {
    PrecajoleMap pm = StaticPrecajoleMap.getInstance();
    CajoledModule cm = pm.lookupUri(JQUERY, false);
    if (cm == null) {
      throw new Error("forUri failed");
    }
    int n = Integer.valueOf(args[0]);
    long t0 = System.currentTimeMillis();
    for (int k = 0; k < n; k++) {
      cm = pm.lookupUri(JQUERY, false);
    }
    long t1 = System.currentTimeMillis();
    System.out.println(t1 - t0);
    System.out.println((double)(t1 - t0) / n);
  }
}
