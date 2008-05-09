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

package com.google.caja.ecajalipse.builder;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.quasiliteral.DefaultCajaRewriter;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageTypeInt;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Map;

/**
 * Eclipse builder plugin which continuously cajoles all appropriate files 
 * in a project.
 *  
 * @author jasvir@gmail.com (Jasvir Nagra)
 */
public class CajaBuilder extends IncrementalProjectBuilder {
  protected InputSource is;
  protected MessageContext mc;
  protected MessageQueue mq;

  class CajaDeltaVisitor implements IResourceDeltaVisitor {
    public boolean visit(IResourceDelta delta) throws CoreException {
      IResource resource = delta.getResource();
      switch (delta.getKind()) {
        case IResourceDelta.ADDED:
          // handle added resource
          checkJs(resource);
          break;
        case IResourceDelta.REMOVED:
          // handle removed resource
          break;
        case IResourceDelta.CHANGED:
          // handle changed resource
          checkJs(resource);
          break;
      }
      //return true to continue visiting children.
      return true;
    }
  }

  class CajaResourceVisitor implements IResourceVisitor {
    public boolean visit(IResource resource) {
      try {
        checkJs(resource);
      } catch (Exception e) {
        e.printStackTrace();
      }
      //return true to continue visiting children.
      return true;
    }
  }

  class EclipseMessageQueue extends SimpleMessageQueue {

    private IFile file;

    public EclipseMessageQueue(IFile file) {
      this.file = file;
    }

    /**
     * Map between Caja rewriter message level and Eclipse message level
     * @param level - rewriter message level
     * @return severity of the message level for markup in eclipse
     */
    private int mapError(MessageLevel level) {
      if (level.compareTo(MessageLevel.WARNING) < 0)
        return IMarker.SEVERITY_INFO;
      else if (level.compareTo(MessageLevel.CRITICAL_WARNING) > 0)
        return IMarker.SEVERITY_WARNING;
      else
        return IMarker.SEVERITY_ERROR;        
    }

    private void addEclipseMessage(Message msg, MessageTypeInt type, 
        MessagePart... parts) {
      try {
        StringBuilder message = new StringBuilder();
        MessagePart part = parts[0];
        type.format(parts, mc, message);
        // TODO(jasvir): Once RewriterMessageType is refactored
        // this regular expression substitution should be removed
        String errorMessage = msg.format(mc).replaceFirst(":.*?:", "");
        int lineNumber = 0;
        int start = 0;
        int end = 0;
        if (part instanceof FilePosition) {
          FilePosition filePosition = (FilePosition)part;
          lineNumber = filePosition.startLineNo();
          
          // Eclipse buffer is zero-indexed
          // Caja fileposition is one-indexed
          start = filePosition.startCharInFile() - 1;
          end = filePosition.endCharInFile() - 1;
        }
        CajaBuilder.this.addMarker(file, errorMessage, lineNumber, 
            start, end, mapError(type.getLevel()));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }    

    @Override
    public void addMessage(MessageTypeInt type, MessagePart... parts) {
      Message msg = new Message(type,parts);
      getMessages().add(msg);
      addEclipseMessage(msg, type, parts);
    }    

    @Override
    public void addMessage(MessageTypeInt type, MessageLevel lvl,
        MessagePart... parts) {
      Message msg = new Message(type,parts);
      getMessages().add(msg);
      addEclipseMessage(msg, type, parts);
    }
  }

  public static final String BUILDER_ID = "com.google.caja.ecajalipse.cajaBuilder";
  private static final String MARKER_TYPE = "com.google.caja.ecajalipse.syntaxErrorProblem";

  private void addMarker(IFile file, String message, int lineNumber, 
      int start, int end, int severity) {
    try {
      IMarker marker = file.createMarker(MARKER_TYPE);
      marker.setAttribute(IMarker.MESSAGE, message);
      marker.setAttribute(IMarker.SEVERITY, severity);
      if (lineNumber == -1) {
        lineNumber = 1;
      }
      // If the error is at a point, then underline
      // the character before the error location as is the convention
      // in Eclipse
      if (start == end && start > 0) {
        start -= 1;
      }

      marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
      marker.setAttribute(IMarker.CHAR_START, start);
      marker.setAttribute(IMarker.CHAR_END, end);
    } catch (CoreException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
  throws CoreException {
    if (kind == FULL_BUILD) {
      fullBuild(monitor);
    } else {
      IResourceDelta delta = getDelta(getProject());
      if (delta == null) {
        fullBuild(monitor);
      } else {
        incrementalBuild(delta, monitor);
      }
    }
    return null;
  }

  private String render(ParseTreeNode node) {
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = node.makeRenderer(sb, null);
    node.render(new RenderContext(mc, tc));
    tc.noMoreTokens();
    return sb.toString();
  }

  private void cajoleJs(URI inputUri, Reader cajaInput, 
      Appendable output) {
    InputSource is = new InputSource (inputUri);    
    CharProducer cp = CharProducer.Factory.create(cajaInput,is);
    try {
      ParseTreeNode input;
      JsLexer lexer = new JsLexer(cp);
      JsTokenQueue tq = new JsTokenQueue(lexer, is);
      Parser p = new Parser(tq, mq);
      input = p.parse();
      tq.expectEmpty();

      DefaultCajaRewriter dcr = new DefaultCajaRewriter();
      output.append(render(dcr.expand(input, mq)));
    } catch (ParseException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void checkJs(IResource resource) throws CoreException {
    if (resource instanceof IFile && resource.getName().endsWith(".js")) {
      IFile file = (IFile) resource;
      deleteMarkers(file);
      EclipseMessageQueue reporter = new EclipseMessageQueue(file);
      Reader inputReader = new InputStreamReader(file.getContents());
      mq = new EclipseMessageQueue(file);
      mc = new MessageContext();
      cajoleJs(resource.getLocationURI(), inputReader, new StringBuffer());
    }
  }

  private void deleteMarkers(IFile file) {
    try {
      file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
    } catch (CoreException ce) {
    }
  }

  protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
    try {
      getProject().accept(new CajaResourceVisitor());
    } catch (CoreException e) {
    }
  }

  protected void incrementalBuild(IResourceDelta delta,
      IProgressMonitor monitor) throws CoreException {
    delta.accept(new CajaDeltaVisitor());
  }
}
