// Copyright (C) 2011 Google Inc.
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

package com.google.caja.demos.gwtbeans.compile;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.caja.demos.gwtbeans.shared.AbstractTaming;
import com.google.caja.demos.gwtbeans.shared.ElementTaming;
import com.google.caja.demos.gwtbeans.shared.ElementTamingImpl;
import com.google.caja.demos.gwtbeans.shared.Frame;
import com.google.caja.demos.gwtbeans.shared.HasTaming;
import com.google.caja.demos.gwtbeans.shared.Taming;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.reporting.JsIdentifierSyntax;
import com.google.caja.reporting.RenderContext;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.dom.client.Element;

public class TamingGenerator extends Generator {
  
  private static final String PROXY_COMMON_INTERFACE = Taming.class.getCanonicalName();
  private static final String PROXY_COMMON_BASE_CLASS = AbstractTaming.class.getCanonicalName();
  private static final String FRAME_CLASS = Frame.class.getCanonicalName();
  private static final String JSO_CLASS = JavaScriptObject.class.getCanonicalName();
  private static final String GWT_CLASS = GWT.class.getCanonicalName();
  private static final String UNDEFINED = "undefined";
  private static final String ELEMENT_CLASS = Element.class.getCanonicalName();  
  private static final String ELEMENT_PROXY_CLASS = ElementTaming.class.getCanonicalName();
  private static final String ELEMENT_PROXY_IMPL_CLASS = ElementTamingImpl.class.getCanonicalName();
  private static final String PROP_USER_AGENT = "user.agent";
  
  private class ProxyMeta {
    private String proxyImplTypeName;
    private Set<JClassType> toWrap = new HashSet<JClassType>();
    private Set<JClassType> toUnwrap = new HashSet<JClassType>();    

    public ProxyMeta(String proxyImplTypeName) {
      this.proxyImplTypeName = proxyImplTypeName;
    }
    
    public Reference getTypeWrapperAccessor(TypeOracle to, JClassType beanType) {
      toWrap.add(beanType);
      return new Reference(
          new Identifier(
              FilePosition.UNKNOWN,
              "@" + proxyImplTypeName + "::" + getTypeWrapperMethodName(beanType) + "(" + to.findType(FRAME_CLASS).getJNISignature() + beanType.getJNISignature() + ")"));
    }
    
    public Reference getTypeUnwrapperAccessor(TypeOracle to, JClassType beanType) {
      toUnwrap.add(beanType);
      return new Reference(
          new Identifier(
              FilePosition.UNKNOWN,
              "@" + proxyImplTypeName + "::" + getTypeUnwrapperMethodName(beanType) + "(" + to.findType(FRAME_CLASS).getJNISignature() + to.findType(JSO_CLASS).getJNISignature() + ")"));
    }
    
    public Set<JClassType> getToWrap() {
      return toWrap;
    }
    
    public Set<JClassType> getToUnwrap() {
      return toUnwrap;
    }
  }
  
  @Override
  public String generate(
      TreeLogger logger,
      GeneratorContext context,
      String proxyTypeName)
      throws UnableToCompleteException {
    try {
      return generateImpl(logger, context, proxyTypeName);
    } catch (UnableToCompleteException e) {
      logger.log(Type.ERROR, e.toString());
      e.printStackTrace(System.err);
      throw e;
    } catch (Throwable e) {
      logger.log(Type.ERROR, e.toString());
      e.printStackTrace(System.err);
      throw new UnableToCompleteException();
    }
  }
  
  private String generateImpl(
      TreeLogger logger,
      GeneratorContext context,
      String proxyTypeName)
      throws UnableToCompleteException, BadPropertyValueException {
    if (proxyTypeName.equals(ELEMENT_PROXY_CLASS)) {
      return ELEMENT_PROXY_IMPL_CLASS;
    }

    TypeOracle to = context.getTypeOracle();
    
    JClassType proxyType = to.findType(proxyTypeName);

    if (proxyType == null) {
      logger.log(Type.ERROR, "Taming type " + proxyTypeName + " not found in source path");
      throw new UnableToCompleteException();
    }
    
    if (proxyType.isInterface() == null) {
      logger.log(Type.ERROR, "Taming type " + proxyTypeName + " must must be an interface");
      throw new UnableToCompleteException();      
    }
    
    if (proxyType.isGenericType() != null) {
      logger.log(Type.ERROR, "Taming type " + proxyTypeName + " cannot be generic");
      throw new UnableToCompleteException();      
    }
    
    if (proxyType.getImplementedInterfaces().length != 1) {
      logger.log(Type.ERROR, "Taming type " + proxyTypeName + " must only extend one interface, " + PROXY_COMMON_INTERFACE);
      throw new UnableToCompleteException();      
    }
    
    if (proxyType.getImplementedInterfaces()[0].isParameterized() == null) {
      logger.log(Type.ERROR, "Taming type " + proxyTypeName + " must extend " + PROXY_COMMON_INTERFACE + " parameterized by bean class or interface");
      throw new UnableToCompleteException();      
    }
    
    JParameterizedType proxySuperParameterized = proxyType.getImplementedInterfaces()[0].isParameterized();
    
    JClassType proxySuperRaw = to.findType(PROXY_COMMON_INTERFACE).isGenericType().getRawType();
    
    if (proxySuperParameterized.getRawType() != proxySuperRaw) {
      logger.log(Type.ERROR, "Taming type " + proxyTypeName + " must extend " + PROXY_COMMON_INTERFACE);
      throw new UnableToCompleteException();      
    }

    JClassType beanType = proxySuperParameterized.getTypeArgs()[0];
    String beanTypeName = beanType.getParameterizedQualifiedSourceName();
    
    if (beanType.getAnnotation(HasTaming.class) == null) {
      logger.log(Type.ERROR, "Bean type " + beanTypeName + " referred to by proxy type " + proxyTypeName + " must have an annotation of type " + HasTaming.class.getCanonicalName());
      throw new UnableToCompleteException();      
    }

    if (beanType.isGenericType() != null) {
      logger.log(Type.ERROR, "Bean type " + beanTypeName + " may not be generic");
      throw new UnableToCompleteException();      
    }
    
    if (beanType.isInterface() != null) {
      logger.log(Type.ERROR, "Bean type " + beanTypeName + " may not be an interface because GWT RTTI (java.lang.Class) does not support getInterfaces()");
      throw new UnableToCompleteException();      
    }
    
    GwtBeanInfo beanInfo;
    try {
      beanInfo = new GwtBeanInfo(beanType);
    } catch (Exception e) {
      logger.log(Type.ERROR, e.getMessage());
      throw new UnableToCompleteException();
    }
    
    String userAgent;
    try {
      userAgent = context.getPropertyOracle().getSelectionProperty(logger,
          PROP_USER_AGENT).getCurrentValue();
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.ERROR, "Unable to find value for '"
          + PROP_USER_AGENT + "'", e);
      throw new UnableToCompleteException();
    }

    userAgent = userAgent.substring(0, 1).toUpperCase()
        + userAgent.substring(1);
    
    String proxyTypeShortName = proxyType.getSimpleSourceName();
    String proxyTypePackageName = proxyType.getPackage().getName();

    String proxyImplTypeName = proxyTypeName + userAgent + "Impl";    
    String proxyImplTypeShortName = proxyTypeShortName + userAgent + "Impl";

    ProxyMeta pm = new ProxyMeta(proxyImplTypeName);
    ParseTreeNode jsBody = makeJsBody(to, pm, beanInfo);
    
    StringBuilder sb = new StringBuilder();
    sb.append(
        "package " + proxyTypePackageName + ";\n" +
        "public class " + proxyImplTypeShortName + "\n" +
        "    extends " + PROXY_COMMON_BASE_CLASS + "<" + beanTypeName + ">\n" + 
        "    implements " + proxyTypeShortName + "\n" + 
        "{\n" +
        "  @Override\n" +
        "  public String getBeanClassName() {\n" +
        "    return \"class " + beanType.getQualifiedSourceName() + "\";\n" +
        "  }\n" +
        "  @Override\n" +
        "  public native " + JSO_CLASS + " getNative\n" +
        "      (\n" +
        "          " + FRAME_CLASS + " m,\n" +
        "          " + beanTypeName + " bean\n" +
        "      )\n" +
        "  /*-{\n" +
        "    " + render(jsBody) + "\n" +
        "  }-*/;\n" +
        "  " + getWrapperUnwrapperMethods(to, pm) + "\n" +
        "}\n");
    
    System.err.println("========== generate(" + proxyTypeName + ") ====================");
    System.err.println(sb.toString());
    
    PrintWriter pw = context.tryCreate(logger, proxyTypePackageName, proxyImplTypeShortName);
    pw.write(sb.toString());
    context.commit(logger, pw);

    return proxyImplTypeName;
  }
  
  private String getWrapperUnwrapperMethods(TypeOracle to, ProxyMeta pm) {
    StringBuilder sb = new StringBuilder();
    for (JClassType beanType : pm.getToWrap()) {
      sb.append(getWrapperMethod(to, beanType)).append("\n");
    }
    for (JClassType beanType : pm.getToUnwrap()) {    
      sb.append(getUnwrapperMethod(to, beanType)).append("\n");      
    }
    return sb.toString();
  }

  private JClassType getProxyTypeForBean(TypeOracle to, JClassType beanType) {
    if (beanType == to.findType(ELEMENT_CLASS)) {
      return to.findType(ELEMENT_PROXY_CLASS);
    }
    if (beanType.getAnnotation(HasTaming.class) == null) {
      // TODO(ihab): Thread logger thru and log error properly
      throw new RuntimeException("Bean type " + beanType.getQualifiedSourceName() + " must have an annotation of type " + HasTaming.class.getCanonicalName());
    }
    HasTaming hp = beanType.getAnnotation(HasTaming.class);
    return to.findType(hp.typeName());
  }
  
  private String getWrapperMethod(TypeOracle to, JClassType beanType) {
    JClassType proxyType = getProxyTypeForBean(to, beanType);
    return
        "public static " + JSO_CLASS + "\n" +
        "    " + getTypeWrapperMethodName(beanType) + "\n" +
        "    (\n" + 
        "         " + FRAME_CLASS + " m,\n" +
        "         " + beanType.getQualifiedSourceName() + " bean\n" +
        "    )\n" +
        "  {\n" +
        "    return \n" +
        "        (\n" +        
        "            (" + proxyType.getParameterizedQualifiedSourceName() + ")\n" +
        "             " + GWT_CLASS + ".create(" + proxyType.getParameterizedQualifiedSourceName() + ".class)\n" +
        "        )\n" +
        "        .getJso(m, bean);\n" +        
        "  }\n";        
  }
  
  private String getUnwrapperMethod(TypeOracle to, JClassType beanType) {
    JClassType proxyType = getProxyTypeForBean(to, beanType);
    return
        "public static " + beanType.getQualifiedSourceName() + "\n" +
        "    " + getTypeUnwrapperMethodName(beanType) + "\n" +
        "    (\n" + 
        "         " + FRAME_CLASS + " m,\n" +        
        "         " + JSO_CLASS + " jso\n" +
        "    )\n" +        
        "  {\n" +
        "    return \n" +
        "        (\n" +        
        "            (" + proxyType.getParameterizedQualifiedSourceName() + ")\n" +
        "             " + GWT_CLASS + ".create(" + proxyType.getParameterizedQualifiedSourceName() + ".class)\n" +
        "        )\n" +
        "        .getBean(m, jso);\n" +        
        "  }\n";        
  }
  
  private ParseTreeNode makeJsBody(TypeOracle to, ProxyMeta pm, GwtBeanInfo beanInfo) {
    List<StringLiteral> keys = new ArrayList<StringLiteral>();
    List<Expression> vals = new ArrayList<Expression>();
    for (JMethod m : beanInfo.getMethods()) {
      keys.add(new StringLiteral(FilePosition.UNKNOWN, m.getName()));
      vals.add(getPropertyDescriptorForMethod(to, pm, m));
    }
    for (GwtBeanInfo.PropertyDescriptor pd : beanInfo.getProperties()) {
      keys.add(new StringLiteral(FilePosition.UNKNOWN, pd.name));
      vals.add(getPropertyDescriptorForProperty(to, pm, pd));      
    }
    return QuasiBuilder.substV(
        "var w = $wnd.caja.iframe.contentWindow;" +
        "return w.___.makeTrampolineObject({ @keys*: @vals* });",
        "keys",  new ParseTreeNodeContainer(keys),
        "vals", new ParseTreeNodeContainer(vals));
  }
  
  private Expression getPropertyDescriptorForMethod(TypeOracle to, ProxyMeta pm, JMethod m) {
    List<FormalParam> formals = new ArrayList<FormalParam>();
    List<Reference> actuals = new ArrayList<Reference>();
    for (JParameter p : m.getParameters()) {
      formals.add(new FormalParam(new Identifier(FilePosition.UNKNOWN, p.getName())));
      actuals.add(new Reference(new Identifier(FilePosition.UNKNOWN, p.getName())));
    }
    Expression method;
    if (JPrimitiveType.VOID == m.getReturnType()) {
      method = (Expression) QuasiBuilder.substV(
          "function (@formals*) { @unwrapStatements*; bean.@methodRef(@actuals*); }",
          "unwrapStatements", getUnwrapStatements(to, pm, m.getParameters()),
          "methodRef", getMethodAccessor(m),
          "formals", new ParseTreeNodeContainer(formals),
          "actuals", new ParseTreeNodeContainer(actuals));
    } else if (isGwtPrimitiveType(to, m.getReturnType())) {
      method = (Expression) QuasiBuilder.substV(
          "function (@formals*) { @unwrapStatements*; return bean.@methodRef(@actuals*); })",
          "unwrapStatements", getUnwrapStatements(to, pm, m.getParameters()),          
          "methodRef", getMethodAccessor(m),
          "formals", new ParseTreeNodeContainer(formals),
          "actuals", new ParseTreeNodeContainer(actuals));
    } else {
      method = (Expression) QuasiBuilder.substV(
          "function (@formals*) { @unwrapStatements*; return @proxyWrapAccessor(bean.@methodRef(@actuals*)); }",
          "unwrapStatements", getUnwrapStatements(to, pm, m.getParameters()),
          "proxyWrapAccessor", pm.getTypeWrapperAccessor(to, m.getReturnType().isClassOrInterface()),
          "methodRef", getMethodAccessor(m),
          "formals", new ParseTreeNodeContainer(formals),
          "actuals", new ParseTreeNodeContainer(actuals));
    }
    return (Expression) QuasiBuilder.substV(
        "({" +
        "  value: w.___.makeTrampolineFunction(@method)," +
        "  enumerable: true," +
        "  writable: false," +
        "  configurable: false" +        
        "})",          
        "method", method);
  }

  private ParseTreeNodeContainer getUnwrapStatements(TypeOracle to, ProxyMeta pm, JParameter[] params) {
    List<ExpressionStmt> stmts = new ArrayList<ExpressionStmt>();
    for (JParameter p : params) {
      if (!isGwtPrimitiveType(to, p.getType())) {
        stmts.add(
            new ExpressionStmt(
                (Expression) QuasiBuilder.substV(
                    "@argRef = @proxyUnwrapAccessor(m, @argRef)",
                    "proxyUnwrapAccessor", pm.getTypeUnwrapperAccessor(to, p.getType().isClassOrInterface()),
                    "argRef", new Reference(new Identifier(FilePosition.UNKNOWN, p.getName())))));
      }
    }
    return new ParseTreeNodeContainer(stmts);
  }
  
  private Expression getPropertyDescriptorForProperty(TypeOracle to, ProxyMeta pm, GwtBeanInfo.PropertyDescriptor pd) {
    Expression get, set;
    if (isGwtPrimitiveType(to, pd.type)) {
      get = (pd.readMethod == null) ?
          new Reference(new Identifier(FilePosition.UNKNOWN, UNDEFINED)) :
          (Expression) QuasiBuilder.substV(
              "w.___.makeTrampolineFunction(function () { return bean.@methodRef(); })",
              "methodRef", getMethodAccessor(pd.readMethod));
      set = (pd.writeMethod == null) ?
          new Reference(new Identifier(FilePosition.UNKNOWN, UNDEFINED)) :          
          (Expression) QuasiBuilder.substV(
              "w.___.makeTrampolineFunction(function (v) { bean.@methodRef(v); })",
              "methodRef", getMethodAccessor(pd.writeMethod));
    } else {
      get = (pd.readMethod == null) ?
          new Reference(new Identifier(FilePosition.UNKNOWN, UNDEFINED)) :          
          (Expression) QuasiBuilder.substV(
              "w.___.makeTrampolineFunction(function () { return @proxyWrapAccessor(m, bean.@methodRef()); })",
              "proxyWrapAccessor", pm.getTypeWrapperAccessor(to, pd.type.isClassOrInterface()),
              "methodRef", getMethodAccessor(pd.readMethod));
      set = (pd.writeMethod == null) ?
          new Reference(new Identifier(FilePosition.UNKNOWN, UNDEFINED)) :          
          (Expression) QuasiBuilder.substV(
              "w.___.makeTrampolineFunction(function (v) { bean.@methodRef(@proxyUnwrapAccessor(m, v)); })",
              "proxyUnwrapAccessor", pm.getTypeUnwrapperAccessor(to, pd.type.isClassOrInterface()),              
              "methodRef", getMethodAccessor(pd.writeMethod));
    }
    return (Expression) QuasiBuilder.substV(
        "({" +
        "  get: @get," +
        "  set: @set," +
        "  enumerable: true," +
        "  configurable: false" +
        "})",          
        "get", get,
        "set", set);
  }
  
  private Reference getMethodAccessor(JMethod m) {
    return new Reference(new Identifier(FilePosition.UNKNOWN, m.getJsniSignature()));
  }
  
  /**
   * Whether the supplied type is a valid GWT primitive type. 
   */
  private boolean isGwtPrimitiveType(TypeOracle to, JType type) {
    // Note that we do not include GWT class Element in this list, though it is
    // treated by GWT JSNI as a primitive. Instead, we hard-code an actual Taming
    // class for class Element that does the necessary DOM taming.
    return
        (type instanceof JPrimitiveType) ||  // TODO(ihab.awad): |long| primitives are weird in GWT
        (type == to.findType("java.lang.Number")) ||  // TODO(ihab.awad): |Number| has no direct instances, right?
        (type == to.findType("java.lang.AtomicInteger")) ||  // TODO(ihab.awad): wtf is that?
        (type == to.findType("java.lang.AtomicLong")) ||  // TODO(ihab.awad): |Long| objects are weird in GWT
        (type == to.findType("java.lang.BigDecimal")) ||  // TODO(ihab.awad): |Long| objects are weird in GWT
        (type == to.findType("java.lang.BigInteger")) ||  // TODO(ihab.awad): |Long| objects are weird in GWT
        (type == to.findType("java.lang.Byte")) ||
        (type == to.findType("java.lang.Double")) ||
        (type == to.findType("java.lang.Float")) ||
        (type == to.findType("java.lang.Integer")) ||
        (type == to.findType("java.lang.Long")) ||  // TODO(ihab.awad): |Long| objects are weird in GWT
        (type == to.findType("java.lang.Short")) ||
        (type == to.findType("java.lang.String"));
  }
  
  private static String getTypeWrapperMethodName(JType beanType) {
    return getTypeSpecificMethodName(beanType) + "_getJso";
  }
  
  private static String getTypeUnwrapperMethodName(JType beanType) {
    return getTypeSpecificMethodName(beanType) + "_getBean";
  }
  
  private static String getTypeSpecificMethodName(JType beanType) {
    return
        beanType.getQualifiedSourceName()
        .replace(".", "_");
  }
  
  private String render(ParseTreeNode node) {
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = node.makeRenderer(sb, null);
    node.render(new RenderContext(tc)
        .withJsIdentiferSyntax(JsIdentifierSyntax.GWT));
    tc.noMoreTokens();
    return sb.toString();
  }
}
