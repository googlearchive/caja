package com.google.caja.tools;

import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.DiagnosticGroup;
import com.google.javascript.jscomp.DiagnosticGroups;
import com.google.javascript.jscomp.MessageFormatter;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.WarningLevel;
import com.google.javascript.jscomp.ant.AntErrorManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class ClosureCompiler {

  /*
     trunk=http://closure-compiler.googlecode.com/svn/trunk
     curl $trunk/src/com/google/javascript/jscomp/DiagnosticGroups.java |
       perl -ne 'm(public static DiagnosticGroup (\w+) =) &&
          print qq(DiagnosticGroups.$1,\n)' | sort
   */
  private static DiagnosticGroup diagnosticGroups[] = {
    DiagnosticGroups.ACCESS_CONTROLS,
    DiagnosticGroups.AMBIGUOUS_FUNCTION_DECL,
    DiagnosticGroups.CHECK_PROVIDES,
    DiagnosticGroups.CHECK_REGEXP,
    DiagnosticGroups.CHECK_TYPES,
    DiagnosticGroups.CHECK_USELESS_CODE,
    DiagnosticGroups.CHECK_VARIABLES,
    DiagnosticGroups.CONST,
    DiagnosticGroups.CONSTANT_PROPERTY,
    DiagnosticGroups.DEBUGGER_STATEMENT_PRESENT,
    DiagnosticGroups.DEPRECATED,
    DiagnosticGroups.DUPLICATE_MESSAGE,
    DiagnosticGroups.DUPLICATE_VARS,
    DiagnosticGroups.ES5_STRICT,
    DiagnosticGroups.EXTERNS_VALIDATION,
    DiagnosticGroups.FILEOVERVIEW_JSDOC,
    DiagnosticGroups.GLOBAL_THIS,
    DiagnosticGroups.INTERNET_EXPLORER_CHECKS,
    DiagnosticGroups.INVALID_CASTS,
    DiagnosticGroups.MISSING_PROPERTIES,
    DiagnosticGroups.NON_STANDARD_JSDOC,
    DiagnosticGroups.STRICT_MODULE_DEP_CHECK,
    DiagnosticGroups.TWEAKS,
    DiagnosticGroups.TYPE_INVALIDATION,
    DiagnosticGroups.UNDEFINED_NAMES,
    DiagnosticGroups.UNDEFINED_VARIABLES,
    DiagnosticGroups.UNKNOWN_DEFINES,
    DiagnosticGroups.VISIBILITY,
  };

  public String build(Task task, List<File> inputs, PrintWriter logger) {
    List<SourceFile> externs;
    try {
      externs = CommandLineRunner.getDefaultExterns();
    } catch (IOException e) {
      throw new BuildException(e);
    }

    List<SourceFile> jsInputs = new ArrayList<SourceFile>();
    for (File f : inputs) {
      jsInputs.add(SourceFile.fromFile(f));
    }

    CompilerOptions options = new CompilerOptions();
    CompilationLevel.ADVANCED_OPTIMIZATIONS
        .setOptionsForCompilationLevel(options);
    WarningLevel.VERBOSE.setOptionsForWarningLevel(options);
    for (DiagnosticGroup dg : diagnosticGroups) {
      options.setWarningLevel(dg, CheckLevel.ERROR);
    }

    Compiler compiler = new Compiler();
    MessageFormatter formatter =
        options.errorFormat.toFormatter(compiler, false);
    AntErrorManager errorManager = new AntErrorManager(formatter, task);
    compiler.setErrorManager(errorManager);

    Result r = compiler.compile(externs, jsInputs, options);
    if (!r.success) {
      return null;
    }

    String wrapped = "(function(){" + compiler.toSource() + "})();\n";
    return wrapped;
  }

}
