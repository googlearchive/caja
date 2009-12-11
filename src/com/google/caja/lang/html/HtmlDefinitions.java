// Copyright (C) 2008 Google Inc.
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

package com.google.caja.lang.html;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.config.AllowedFileResolver;
import com.google.caja.config.ConfigUtil;
import com.google.caja.config.ImportResolver;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlTextEscapingMode;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.html.AttribKey;
import com.google.caja.parser.html.ElKey;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.tools.BuildCommand;
import com.google.caja.util.Maps;

/**
 * Generates a JavaScript tree with tables mapping HTML element and attribute
 * names to information about those elements and attributes.
 *
 * @author mikesamuel@gmail.com
 */
public final class HtmlDefinitions {

  /** Combined to form a bit-field describing an element. */
  public enum EFlag {
    OPTIONAL_ENDTAG(1),
    EMPTY(2),
    CDATA(4),
    RCDATA(8),
    UNSAFE(0x10),
    /**
     * Elements that can be removed from the DOM without changing behavior as
     * long as their children are folded into the element's parent.
     * The set of FOLDABLE elements should be kept in sync with
     * HtmlSanitizer.java#isElementFoldable.
     */
    FOLDABLE(0x20),
    SCRIPT(0x40),
    STYLE(0x80),
    ;

    public final int bitMask;

    EFlag(int bitMask) { this.bitMask = bitMask; }
  }

  private static final AttribKey SCRIPT_SRC = AttribKey.forHtmlAttrib(
      ElKey.forHtmlElement("script"), "src");
  public static Block generateJavascriptDefinitions(HtmlSchema schema) {
    FilePosition unk = FilePosition.UNKNOWN;
    Map<AttribKey, HTML.Attribute.Type> atypes = attributeTypes(schema);
    Map<ElKey, EnumSet<EFlag>> eflags = elementFlags(schema);

    Block definitions = new Block();
    definitions.appendChild(QuasiBuilder.substV("var html4 = {};"));

    EnumSet<HTML.Attribute.Type> atypesUsed
        = EnumSet.noneOf(HTML.Attribute.Type.class);
    atypesUsed.addAll(atypes.values());
    {
      List<StringLiteral> keys = new ArrayList<StringLiteral>();
      List<IntegerLiteral> values = new ArrayList<IntegerLiteral>();
      for (HTML.Attribute.Type t : atypesUsed) {
        keys.add(StringLiteral.valueOf(unk, t.name()));
        values.add(new IntegerLiteral(unk, A_TYPE_MAP.get(t)));
      }
      definitions.appendChild(new ExpressionStmt(unk, (Expression)
          QuasiBuilder.substV(
              "html4.atype = { @k*: @v* };",
              "k", new ParseTreeNodeContainer(keys),
              "v", new ParseTreeNodeContainer(values))));
    }
    {
      List<StringLiteral> keys = new ArrayList<StringLiteral>();
      List<IntegerLiteral> values = new ArrayList<IntegerLiteral>();
      for (Map.Entry<AttribKey, HTML.Attribute.Type> e : atypes.entrySet()) {
        AttribKey key = e.getKey();
        if (ElKey.HTML_WILDCARD.equals(key.el)
            || schema.isElementAllowed(key.el)
            // Whitelisted to allow dynamic script loading via proxy
            || SCRIPT_SRC.equals(key)) {
          keys.add(StringLiteral.valueOf(unk, key.toString()));
          values.add(new IntegerLiteral(unk, A_TYPE_MAP.get(e.getValue())));
        }
      }
      definitions.appendChild(new ExpressionStmt(unk, (Expression)
          QuasiBuilder.substV(
              "html4.ATTRIBS = { @k*: @v* };",
              "k", new ParseTreeNodeContainer(keys),
              "v", new ParseTreeNodeContainer(values))));
    }

    EnumSet<EFlag> eflagsUsed = EnumSet.noneOf(EFlag.class);
    for (EnumSet<EFlag> flags : eflags.values()) { eflagsUsed.addAll(flags); }
    {
      List<StringLiteral> keys = new ArrayList<StringLiteral>();
      List<IntegerLiteral> values = new ArrayList<IntegerLiteral>();
      for (EFlag f : eflagsUsed) {
        keys.add(StringLiteral.valueOf(unk, f.name()));
        values.add(new IntegerLiteral(unk, f.bitMask));
      }
      definitions.appendChild(new ExpressionStmt(unk, (Expression)
          QuasiBuilder.substV(
              "html4.eflags = { @k*: @v* };",
              "k", new ParseTreeNodeContainer(keys),
              "v", new ParseTreeNodeContainer(values))));
    }
    {
      List<StringLiteral> keys = new ArrayList<StringLiteral>();
      List<IntegerLiteral> values = new ArrayList<IntegerLiteral>();
      for (Map.Entry<ElKey, EnumSet<EFlag>> e : eflags.entrySet()) {
        ElKey key = e.getKey();
        int value = 0;
        for (EFlag f : e.getValue()) { value |= f.bitMask; }
        keys.add(StringLiteral.valueOf(unk, key.toString()));
        values.add(new IntegerLiteral(unk, value));
      }
      definitions.appendChild(new ExpressionStmt(unk, (Expression)
          QuasiBuilder.substV(
              "html4.ELEMENTS = { @k*: @v* };",
              "k", new ParseTreeNodeContainer(keys),
              "v", new ParseTreeNodeContainer(values))));
    }

    return definitions;
  }

  /** Maps attribute types to integers for use in the JavaScript output. */
  private static final Map<HTML.Attribute.Type, Integer> A_TYPE_MAP
      = new EnumMap<HTML.Attribute.Type, Integer>(HTML.Attribute.Type.class);
  static {
    // Under no circumstances should this be changed to use Enum.ordinal().
    // This type to integer mapping must stay the same, or version skew
    // will mean that the HTML definitions JavaScript can only be used with
    // the same version of the cajoler that cajoled the gadget.
    A_TYPE_MAP.put(HTML.Attribute.Type.NONE, 0);
    A_TYPE_MAP.put(HTML.Attribute.Type.URI, 1);
    A_TYPE_MAP.put(HTML.Attribute.Type.SCRIPT, 2);
    A_TYPE_MAP.put(HTML.Attribute.Type.STYLE, 3);
    A_TYPE_MAP.put(HTML.Attribute.Type.ID, 4);
    A_TYPE_MAP.put(HTML.Attribute.Type.IDREF, 5);
    A_TYPE_MAP.put(HTML.Attribute.Type.IDREFS, 6);
    A_TYPE_MAP.put(HTML.Attribute.Type.GLOBAL_NAME, 7);
    A_TYPE_MAP.put(HTML.Attribute.Type.LOCAL_NAME, 8);
    A_TYPE_MAP.put(HTML.Attribute.Type.CLASSES, 9);
    A_TYPE_MAP.put(HTML.Attribute.Type.FRAME_TARGET, 10);
    A_TYPE_MAP.put(HTML.Attribute.Type.URI_FRAGMENT, 11);
    for (HTML.Attribute.Type t : HTML.Attribute.Type.values()) {
      if (!A_TYPE_MAP.containsKey(t)) {
        throw new IllegalStateException("Not all Attribute Types mapped");
      }
    }
  }

  public static int getJavascriptValueForAType(HTML.Attribute.Type atype) {
    return A_TYPE_MAP.get(atype);
  }

  private static Map<AttribKey, HTML.Attribute.Type> attributeTypes(
      HtmlSchema schema) {
    Map<AttribKey, HTML.Attribute.Type> attributeFlags = Maps.newTreeMap();
    for (AttribKey attribKey : schema.getAttributeNames()) {
      if (schema.isAttributeAllowed(attribKey)) {
        HTML.Attribute a = schema.lookupAttribute(attribKey);
        HTML.Attribute.Type type = a.getType();
        attributeFlags.put(attribKey, type);
      }
    }
    return attributeFlags;
  }

  private static Map<ElKey, EnumSet<EFlag>> elementFlags(HtmlSchema schema) {
    final ElKey SCRIPT = ElKey.forHtmlElement("script");
    final ElKey STYLE = ElKey.forHtmlElement("style");
    Map<ElKey, EnumSet<EFlag>> elementFlags = Maps.newTreeMap();
    for (ElKey elementName : schema.getElementNames()) {
      HTML.Element el = schema.lookupElement(elementName);
      EnumSet<EFlag> flags = EnumSet.noneOf(EFlag.class);
      if (el.isEndTagOptional()) { flags.add(EFlag.OPTIONAL_ENDTAG); }
      if (el.isEmpty()) { flags.add(EFlag.EMPTY); }
      if (elementName.isHtml()) {
        switch (HtmlTextEscapingMode.getModeForTag(elementName.localName)) {
          case CDATA:
            flags.add(EFlag.CDATA);
            break;
          case RCDATA:
            flags.add(EFlag.RCDATA);
            break;
          default: break;
        }
      }
      if (!schema.isElementAllowed(elementName)) {
        flags.add(EFlag.UNSAFE);
        if (SCRIPT.equals(elementName)) {
          flags.add(EFlag.SCRIPT);
        } else if (STYLE.equals(elementName)) {
          flags.add(EFlag.STYLE);
        }
      }
      if (HtmlSchema.isElementFoldable(elementName)) {
        flags.add(EFlag.FOLDABLE);
      }
      elementFlags.put(elementName, flags);
    }
    return elementFlags;
  }

  public static class Builder implements BuildCommand {
    public boolean build(List<File> inputs, List<File> deps, File output)
        throws IOException {
      File elementsFile = null;
      File attrsFile = null;
      for (File input : inputs) {
        if (input.getName().endsWith(".json")) {
          if (elementsFile == null) {
            elementsFile = input;
          } else if (attrsFile == null) {
            attrsFile = input;
          } else {
            throw new IOException("Unused input " + input);
          }
        }
      }
      if (elementsFile == null) {
        throw new IOException("No JSON whitelist for HTML elements");
      }
      if (attrsFile == null) {
        throw new IOException("No JSON whitelist for HTML attributes");
      }

      FilePosition elements = FilePosition.startOfFile(new InputSource(
          elementsFile.getAbsoluteFile().toURI()));
      FilePosition attrs = FilePosition.startOfFile(new InputSource(
          attrsFile.getAbsoluteFile().toURI()));

      MessageContext mc = new MessageContext();
      mc.addInputSource(elements.source());
      mc.addInputSource(attrs.source());
      MessageQueue mq = new EchoingMessageQueue(
          new PrintWriter(new OutputStreamWriter(System.err), true), mc, false);

      Set<File> inputsAndDeps = new HashSet<File>();
      for (File f : inputs) { inputsAndDeps.add(f.getAbsoluteFile()); }
      for (File f : deps) { inputsAndDeps.add(f.getAbsoluteFile()); }

      ImportResolver resolver = new AllowedFileResolver(inputsAndDeps);

      HtmlSchema schema;
      try {
        schema = new HtmlSchema(
            ConfigUtil.loadWhiteListFromJson(
                elements.source().getUri(), resolver, mq),
            ConfigUtil.loadWhiteListFromJson(
                attrs.source().getUri(), resolver, mq));
      } catch (ParseException ex) {
        ex.toMessageQueue(mq);
        throw (IOException) new IOException("Failed to parse schema")
            .initCause(ex);
      }

      Writer out = new OutputStreamWriter(
          new FileOutputStream(output), "UTF-8");
      String currentDate = "" + new Date();
      if (currentDate.indexOf("*/") >= 0) {
        throw new SomethingWidgyHappenedError("Date should not contain '*/'");
      }
      out.write("/* Copyright Google Inc.\n");
      out.write(" * Licensed under the Apache Licence Version 2.0\n");
      out.write(" * Autogenerated at " + currentDate + "\n");
      out.write(" * @provides html4\n");
      out.write(" */\n");
      try {
        Block node = generateJavascriptDefinitions(schema);
        RenderContext rc = new RenderContext(node.makeRenderer(out, null));
        for (Statement s : node.children()) {
          s.render(rc);
          if (!s.isTerminal()) { rc.getOut().consume(";"); }
        }
        rc.getOut().noMoreTokens();
      } finally {
        out.close();
      }
      return true;
    }
  }

  public static void main(String[] args) {
    HtmlSchema schema = HtmlSchema.getDefault(new SimpleMessageQueue());
    Block node = generateJavascriptDefinitions(schema);
    RenderContext rc = new RenderContext(node.makeRenderer(System.out, null));
    for (Statement s : node.children()) {
      s.render(rc);
      if (!s.isTerminal()) { rc.getOut().consume(";"); }
    }
    rc.getOut().noMoreTokens();
  }
}
