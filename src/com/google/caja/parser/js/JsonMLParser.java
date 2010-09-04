package com.google.caja.parser.js;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.javascript.jscomp.jsonml.JsonML;

public class JsonMLParser {

  public static JsonML parse(String source) throws IOException, ParseException {
    return parse(new StringReader(source));
  }

  public static JsonML parse(Reader in) throws IOException, ParseException {
    return parse(in, InputSource.UNKNOWN.getUri());
  }

  public static JsonML parse(Reader in, URI source)
      throws IOException, ParseException {
    return parse(CharProducer.Factory.create(in, new InputSource(source)));
  }

  public static JsonML parse(CharProducer cp) throws ParseException {
    JsLexer lexer = new JsLexer(cp, false);
    JsTokenQueue tq = new JsTokenQueue(
        lexer, cp.getCurrentPosition().source());
    tq.setInputRange(cp.filePositionForOffsets(cp.getOffset(), cp.getLimit()));
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(System.err), new MessageContext());
    Parser p = new Parser(tq, mq);
    Block program = p.parse();
    tq.expectEmpty();
    return program.toJsonMLAsProgram();
  }
}
