package com.google.caja.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

public class FileIO {

  public static boolean write(String string, File output, PrintWriter logger) {
    try {
      Writer w = new OutputStreamWriter(new FileOutputStream(output));
      try {
        w.write(string);
      } finally {
        w.close();
      }
    } catch (IOException ex) {
      logger.println("Failed to write " + output + ": " + ex);
      return false;
    }
    return true;
  }

}
