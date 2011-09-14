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
import com.google.caja.demos.gwtbeans.shared.Frame;
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
import com.google.gwt.core.ext.ConfigurationProperty;
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

public class TamingGenerator {
  
  private static final String PROXY_COMMON_INTERFACE = Taming.class.getCanonicalName();
  private static final String PROXY_COMMON_BASE_CLASS = AbstractTaming.class.getCanonicalName();
  private static final String FRAME_CLASS = Frame.class.getCanonicalName();
  private static final String JSO_CLASS = JavaScriptObject.class.getCanonicalName();
  private static final String GWT_CLASS = GWT.class.getCanonicalName();
  private static final String UNDEFINED = "undefined";
  private static final String PROP_USER_AGENT = "user.agent";
  private static final String GWT_BEAN_INFO_INTROSPECTOR_CLASS_NAME_PROP = "com.google.caja.demos.gwtbeans.introspector";
  private static final String DEFAULT_GWT_BEAN_INFO_CLASS_NAME = com.google.caja.demos.gwtbeans.compile.DefaultGwtBeanIntrospectorImpl.class.getCanonicalName();  
  
  private class TamingMeta {
    private String tamingImplTypeName;
    private Set<JClassType> toWrap = new HashSet<JClassType>();
    private Set<JClassType> toUnwrap = new HashSet<JClassType>();    

    public TamingMeta(String proxyImplTypeName) {
      this.tamingImplTypeName = proxyImplTypeName;
    }
    
    public Reference getTypeWrapperAccessor(TypeOracle to, JClassType beanType) {
      toWrap.add(beanType);
      return new Reference(
          new Identifier(
              FilePosition.UNKNOWN,
              "@" + tamingImplTypeName + "::" + getTypeWrapperMethodName(beanType) + "(" + to.findType(FRAME_CLASS).getJNISignature() + beanType.getJNISignature() + ")"));
    }
    
    public Reference getTypeUnwrapperAccessor(TypeOracle to, JClassType beanType) {
      toUnwrap.add(beanType);
      return new Reference(
          new Identifier(
              FilePosition.UNKNOWN,
              "@" + tamingImplTypeName + "::" + getTypeUnwrapperMethodName(beanType) + "(" + to.findType(FRAME_CLASS).getJNISignature() + to.findType(JSO_CLASS).getJNISignature() + ")"));
    }
    
    public Set<JClassType> getToWrap() {
      return toWrap;
    }
    
    public Set<JClassType> getToUnwrap() {
      return toUnwrap;
    }
  }
  
  private final TreeLogger logger;
  private final GeneratorContext context;
  private final String tamingInterfaceName;
  
  public TamingGenerator(
      TreeLogger logger,
      GeneratorContext context,
      String tamingInterfaceName) {
    this.logger = logger;
    this.context = context;
    this.tamingInterfaceName = tamingInterfaceName;
  }  
  
  public String generate() throws UnableToCompleteException {
    TypeOracle to = context.getTypeOracle();
    
    JClassType proxyType = to.findType(tamingInterfaceName);

    if (proxyType == null) {
      logger.log(Type.ERROR, "Taming type " + tamingInterfaceName + " not found in source path");
      throw new UnableToCompleteException();
    }
    
    if (proxyType.isInterface() == null) {
      logger.log(Type.ERROR, "Taming type " + tamingInterfaceName + " must must be an interface");
      throw new UnableToCompleteException();      
    }
    
    if (proxyType.isGenericType() != null) {
      logger.log(Type.ERROR, "Taming type " + tamingInterfaceName + " cannot be generic");
      throw new UnableToCompleteException();      
    }
    
    if (proxyType.getImplementedInterfaces().length != 1) {
      logger.log(Type.ERROR, "Taming type " + tamingInterfaceName + " must only extend one interface, " + PROXY_COMMON_INTERFACE);
      throw new UnableToCompleteException();      
    }
    
    if (proxyType.getImplementedInterfaces()[0].isParameterized() == null) {
      logger.log(Type.ERROR, "Taming type " + tamingInterfaceName + " must extend " + PROXY_COMMON_INTERFACE + " parameterized by bean class or interface");
      throw new UnableToCompleteException();      
    }
    
    JParameterizedType proxySuperParameterized = proxyType.getImplementedInterfaces()[0].isParameterized();
    
    JClassType proxySuperRaw = to.findType(PROXY_COMMON_INTERFACE).isGenericType().getRawType();
    
    if (proxySuperParameterized.getRawType() != proxySuperRaw) {
      logger.log(Type.ERROR, "Taming type " + tamingInterfaceName + " must extend " + PROXY_COMMON_INTERFACE);
      throw new UnableToCompleteException();      
    }

    JClassType beanType = proxySuperParameterized.getTypeArgs()[0];
    String beanTypeName = beanType.getParameterizedQualifiedSourceName();
    
    if (beanType.isGenericType() != null) {
      logger.log(Type.ERROR, "Bean type " + beanTypeName + " may not be generic");
      throw new UnableToCompleteException();      
    }
    
    if (beanType.isInterface() != null) {
      logger.log(Type.ERROR, 
          "Bean type " + beanTypeName + 
          " may not be an interface because GWT RTTI (java.lang.Class)" +
          " does not support getInterfaces()");
      throw new UnableToCompleteException();      
    }
    
    GwtBeanIntrospector introspector = getIntrospector(context, logger);
    GwtBeanInfo beanInfo = introspector.create(context, logger, beanType);
    
    if (beanInfo.getTamingInterface() != proxyType) {
      logger.log(Type.ERROR,
          "Taming type " + proxyType +
          " claims it is the taming for bean class " + beanType +
          " but BeanInfo says the taming is " + beanInfo.getTamingInterface());
      throw new UnableToCompleteException();      
    }

    if (beanInfo.getTamingImplementation() != null) {
      return beanInfo.getTamingImplementation().getQualifiedSourceName();
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

    String proxyImplTypeName = tamingInterfaceName + userAgent + "Impl";    
    String proxyImplTypeShortName = proxyTypeShortName + userAgent + "Impl";

    TamingMeta pm = new TamingMeta(proxyImplTypeName);
    ParseTreeNode jsBody = makeJsBody(introspector, pm, beanInfo);
    
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
        "  " + getWrapperUnwrapperMethods(introspector, pm) + "\n" +
        "}\n");
    
    System.err.println("========== generate(" + tamingInterfaceName + ") ====================");
    System.err.println(sb.toString());
    
    PrintWriter pw = context.tryCreate(logger, proxyTypePackageName, proxyImplTypeShortName);
    pw.write(sb.toString());
    context.commit(logger, pw);

    return proxyImplTypeName;
  }
  
  private String getWrapperUnwrapperMethods(GwtBeanIntrospector introspector, TamingMeta pm) 
      throws UnableToCompleteException {
    StringBuilder sb = new StringBuilder();
    for (JClassType beanType : pm.getToWrap()) {
      sb.append(getWrapperMethod(introspector, beanType)).append("\n");
    }
    for (JClassType beanType : pm.getToUnwrap()) {    
      sb.append(getUnwrapperMethod(introspector, beanType)).append("\n");      
    }
    return sb.toString();
  }

  private String getWrapperMethod(GwtBeanIntrospector introspector, JClassType beanType) 
      throws UnableToCompleteException {
    JClassType ti = getTamingInterfaceOrFail(introspector, beanType);    
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
        "            (" + ti.getParameterizedQualifiedSourceName() + ")\n" +
        "             " + GWT_CLASS + ".create(" + ti.getParameterizedQualifiedSourceName() + ".class)\n" +
        "        )\n" +
        "        .getJso(m, bean);\n" +        
        "  }\n";        
  }
  
  private String getUnwrapperMethod(GwtBeanIntrospector introspector, JClassType beanType) 
      throws UnableToCompleteException {
    JClassType ti = getTamingInterfaceOrFail(introspector, beanType);
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
        "            (" + ti.getParameterizedQualifiedSourceName() + ")\n" +
        "             " + GWT_CLASS + ".create(" + ti.getParameterizedQualifiedSourceName() + ".class)\n" +
        "        )\n" +
        "        .getBean(m, jso);\n" +        
        "  }\n";        
  }
  
  private JClassType getTamingInterfaceOrFail(GwtBeanIntrospector introspector, JClassType beanType) 
      throws UnableToCompleteException {
    JClassType ti = introspector.create(context, logger, beanType).getTamingInterface();
    if (ti == null) {
      logger.log(Type.ERROR, "Unable to proceed because no taming interface found for bean type " + beanType);
      throw new UnableToCompleteException();
    }
    return ti;
  }
  
  private ParseTreeNode makeJsBody(GwtBeanIntrospector introspector, TamingMeta pm, GwtBeanInfo beanInfo) 
      throws UnableToCompleteException {
    List<StringLiteral> keys = new ArrayList<StringLiteral>();
    List<Expression> vals = new ArrayList<Expression>();
    for (JMethod m : beanInfo.getMethods()) {
      keys.add(new StringLiteral(FilePosition.UNKNOWN, m.getName()));
      vals.add(getPropertyDescriptorForMethod(introspector, pm, m));
    }
    for (GwtBeanPropertyDescriptor pd : beanInfo.getProperties()) {
      keys.add(new StringLiteral(FilePosition.UNKNOWN, pd.name));
      vals.add(getPropertyDescriptorForProperty(introspector, pm, pd));      
    }
    return QuasiBuilder.substV(
        "var w = $wnd.caja.iframe.contentWindow;" +
        "return w.___.makeDefensibleObject({ @keys*: @vals* });",
        "keys",  new ParseTreeNodeContainer(keys),
        "vals", new ParseTreeNodeContainer(vals));
  }
  
  private Expression getPropertyDescriptorForMethod(GwtBeanIntrospector introspector, TamingMeta pm, JMethod m) 
      throws UnableToCompleteException {
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
          "unwrapStatements", getUnwrapStatements(introspector, pm, m.getParameters()),
          "methodRef", getMethodAccessor(m),
          "formals", new ParseTreeNodeContainer(formals),
          "actuals", new ParseTreeNodeContainer(actuals));
    } else if (introspector.create(context, logger, m.getReturnType()).isTamingPrimitiveType()) {
      method = (Expression) QuasiBuilder.substV(
          "function (@formals*) { @unwrapStatements*; return bean.@methodRef(@actuals*); })",
          "unwrapStatements", getUnwrapStatements(introspector, pm, m.getParameters()),          
          "methodRef", getMethodAccessor(m),
          "formals", new ParseTreeNodeContainer(formals),
          "actuals", new ParseTreeNodeContainer(actuals));
    } else {
      method = (Expression) QuasiBuilder.substV(
          "function (@formals*) { @unwrapStatements*; return @proxyWrapAccessor(bean.@methodRef(@actuals*)); }",
          "unwrapStatements", getUnwrapStatements(introspector, pm, m.getParameters()),
          "proxyWrapAccessor", pm.getTypeWrapperAccessor(context.getTypeOracle(), m.getReturnType().isClassOrInterface()),
          "methodRef", getMethodAccessor(m),
          "formals", new ParseTreeNodeContainer(formals),
          "actuals", new ParseTreeNodeContainer(actuals));
    }
    return (Expression) QuasiBuilder.substV(
        "({" +
        "  value: w.___.makeDefensibleFunction(@method)," +
        "  enumerable: true," +
        "  writable: false," +
        "  configurable: false" +        
        "})",          
        "method", method);
  }

  private ParseTreeNodeContainer getUnwrapStatements(GwtBeanIntrospector introspector, TamingMeta pm, JParameter[] params) 
      throws UnableToCompleteException {
    List<ExpressionStmt> stmts = new ArrayList<ExpressionStmt>();
    for (JParameter p : params) {
      if (!introspector.create(context, logger, p.getType()).isTamingPrimitiveType()) {
        stmts.add(
            new ExpressionStmt(
                (Expression) QuasiBuilder.substV(
                    "@argRef = @proxyUnwrapAccessor(m, @argRef)",
                    "proxyUnwrapAccessor", pm.getTypeUnwrapperAccessor(context.getTypeOracle(), p.getType().isClassOrInterface()),
                    "argRef", new Reference(new Identifier(FilePosition.UNKNOWN, p.getName())))));
      }
    }
    return new ParseTreeNodeContainer(stmts);
  }
  
  private Expression getPropertyDescriptorForProperty(GwtBeanIntrospector introspector, TamingMeta pm, GwtBeanPropertyDescriptor pd)
      throws UnableToCompleteException {
    Expression get, set;
    if (introspector.create(context, logger, pd.type).isTamingPrimitiveType()) {
      get = (pd.readMethod == null) ?
          new Reference(new Identifier(FilePosition.UNKNOWN, UNDEFINED)) :
          (Expression) QuasiBuilder.substV(
              "w.___.makeDefensibleFunction(function () { return bean.@methodRef(); })",
              "methodRef", getMethodAccessor(pd.readMethod));
      set = (pd.writeMethod == null) ?
          new Reference(new Identifier(FilePosition.UNKNOWN, UNDEFINED)) :          
          (Expression) QuasiBuilder.substV(
              "w.___.makeDefensibleFunction(function (v) { bean.@methodRef(v); })",
              "methodRef", getMethodAccessor(pd.writeMethod));
    } else {
      get = (pd.readMethod == null) ?
          new Reference(new Identifier(FilePosition.UNKNOWN, UNDEFINED)) :          
          (Expression) QuasiBuilder.substV(
              "w.___.makeDefensibleFunction(function () { return @proxyWrapAccessor(m, bean.@methodRef()); })",
              "proxyWrapAccessor", pm.getTypeWrapperAccessor(context.getTypeOracle(), pd.type.isClassOrInterface()),
              "methodRef", getMethodAccessor(pd.readMethod));
      set = (pd.writeMethod == null) ?
          new Reference(new Identifier(FilePosition.UNKNOWN, UNDEFINED)) :          
          (Expression) QuasiBuilder.substV(
              "w.___.makeDefensibleFunction(function (v) { bean.@methodRef(@proxyUnwrapAccessor(m, v)); })",
              "proxyUnwrapAccessor", pm.getTypeUnwrapperAccessor(context.getTypeOracle(), pd.type.isClassOrInterface()),              
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

  private GwtBeanIntrospector getIntrospector(GeneratorContext context, TreeLogger logger)
      throws UnableToCompleteException {
    String introspectorClassName;
    try {
      ConfigurationProperty cp = context.getPropertyOracle()
          .getConfigurationProperty(GWT_BEAN_INFO_INTROSPECTOR_CLASS_NAME_PROP);
      if (cp.getValues().size() != 1) {
        logger.log(Type.ERROR, "Must specify exactly one value for property " + GWT_BEAN_INFO_INTROSPECTOR_CLASS_NAME_PROP);
        throw new UnableToCompleteException();
      }
      introspectorClassName = cp.getValues().get(0);
    } catch (BadPropertyValueException e) {
      introspectorClassName = DEFAULT_GWT_BEAN_INFO_CLASS_NAME;
    }
    if (introspectorClassName == null) {
      introspectorClassName = DEFAULT_GWT_BEAN_INFO_CLASS_NAME;      
    }
    try {
      return (GwtBeanIntrospector)
          Class.forName(introspectorClassName).newInstance();
    } catch (Exception e) {
      logger.log(Type.ERROR, "Unable to construct GwtBeanIntrospector (" + introspectorClassName + "): " + e.toString());
      throw new UnableToCompleteException();
    }
  }
}
