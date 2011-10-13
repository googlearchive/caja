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

package com.google.caja.gwtbeans.compile;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.caja.gwtbeans.shared.AbstractTaming;
import com.google.caja.gwtbeans.shared.Frame;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.reporting.JsIdentifierSyntax;
import com.google.caja.reporting.RenderContext;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.dev.util.collect.HashSet;

public class TamingGenerator {
  private static final String TAMING_COMMON_BASE_CLASS =
      AbstractTaming.class.getCanonicalName();
  private static final String FRAME_CLASS =
      Frame.class.getCanonicalName();
  private static final String JSO_CLASS =
      JavaScriptObject.class.getCanonicalName();
  private static final String GWT_CLASS =
      GWT.class.getCanonicalName();
  private static final String UNDEFINED =
      "undefined";
  private static final String PROP_USER_AGENT =
      "user.agent";

  private final TreeLogger logger;
  private final GeneratorContext context;
  private final String tamingInterfaceName;
  private final GwtBeanIntrospector introspector;
  private String tamingImplClassName;
  private final Set<JClassType> toWrap = new HashSet<JClassType>();
  private final Set<JClassType> toUnwrap = new HashSet<JClassType>();
  private final Set<JType> toArrayCreateAndAssign = new HashSet<JType>();

  public TamingGenerator(
      TreeLogger logger,
      GeneratorContext context,
      String tamingInterfaceName)
      throws UnableToCompleteException {
    this.logger = logger;
    this.context = context;
    this.tamingInterfaceName = tamingInterfaceName;
    this.introspector = new GwtBeanIntrospector(logger, context);
  }  
  
  public String generate() throws UnableToCompleteException {
    JClassType tamingInterface =
        context.getTypeOracle().findType(tamingInterfaceName);

    if (tamingInterface == null) {
      logger.log(Type.ERROR, ""
          + "Taming interface " + tamingInterfaceName
          + " cannot be found in the class path");
        throw new UnableToCompleteException();
    }

    GwtBeanInfo beanInfo =
        introspector.getBeanInfoByTamingInterface(tamingInterface);

    if (beanInfo == null) {
      logger.log(Type.ERROR, ""
          + "Failed to find metadata for taming interface "
          + tamingInterfaceName + " -- did you forget to declare it "
          + "in the application GWT XML configuration?");
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
      logger.log(TreeLogger.ERROR, ""
          + "Unable to find value for '" + PROP_USER_AGENT + "'", e);
      throw new UnableToCompleteException();
    }

    userAgent = userAgent.substring(0, 1).toUpperCase()
        + userAgent.substring(1);
    
    String tamingInterfaceShortName = tamingInterface.getSimpleSourceName();
    String tamingInterfacePackageName = tamingInterface.getPackage().getName();

    tamingImplClassName = tamingInterfaceName + userAgent + "Impl";
    String tamingImplClassShortName =
        tamingInterfaceShortName + userAgent + "Impl";

    ParseTreeNode jsBody = makeJsBody(introspector, beanInfo);
    
    StringBuilder sb = new StringBuilder();
    sb.append(strV(""
        + "package ${tamingPkg};\n"
        + "public class ${tamingImpl}\n"
        + "    extends ${tamingImplBase}<${bean}>\n"
        + "    implements ${tamingIfc}\n"
        + "{\n"
        + "  @Override\n"
        + "  public String getBeanClassName() {\n"
        + "    return \"class ${bean}\";\n"
        + "  }\n"
        + "  @Override\n"
        + "  public native ${jso} getNative(${frame} m, ${bean} bean) /*-{\n"
        + "    ${jsBody}\n"
        + "  }-*/;\n"
        + "  ${helperMethods}\n"
        + "}\n",
        "tamingPkg", tamingInterfacePackageName,
        "tamingImpl", tamingImplClassShortName,
        "tamingIfc", tamingInterfaceShortName,
        "tamingImplBase", TAMING_COMMON_BASE_CLASS,
        "bean", beanInfo.getType().getQualifiedSourceName(),
        "frame", FRAME_CLASS,
        "jso", JSO_CLASS,
        "jsBody", render(jsBody),
        "helperMethods", getJavaHelperMethods(introspector)));

    logger.log(Type.INFO, "Creating class " + tamingImplClassName);
    logger.log(Type.INFO, sb.toString());

    PrintWriter pw = context.tryCreate(
        logger, tamingInterfacePackageName, tamingImplClassShortName);
    pw.write(sb.toString());
    context.commit(logger, pw);

    return tamingImplClassName;
  }
  
  private String getJavaHelperMethods(
      GwtBeanIntrospector introspector)
      throws UnableToCompleteException {
    StringBuilder sb = new StringBuilder();
    for (JClassType beanType : toWrap) {
      sb.append(getTypeWrapperMethod(introspector, beanType)).append("\n");
    }
    for (JClassType beanType : toUnwrap) {
      sb.append(getTypeUnwrapperMethod(introspector, beanType)).append("\n");
    }
    for (JType type : toArrayCreateAndAssign) {
      sb.append(getArrayCreationMethod(type)).append("\n");
      sb.append(getArrayAssignmentMethod(type)).append("\n");
      sb.append(getArrayLengthQueryMethod(type)).append("\n");
      sb.append(getArrayItemQueryMethod(type)).append("\n");
    }
    return sb.toString();
  }

  private String getTypeWrapperMethod(
      GwtBeanIntrospector introspector,
      JClassType beanType)
      throws UnableToCompleteException {
    JClassType ti = getTamingInterfaceOrFail(introspector, beanType);    
    return strV(""
        + "public static ${jso} ${meth}(${frame} m, ${bean} bean) {\n"
        + "  return\n"
        + "      ((${taming}) ${gwt}.create(${taming}.class))\n"
        + "      .getJso(m, bean);\n"
        + "}\n",
        "jso", JSO_CLASS,
        "frame", FRAME_CLASS,
        "gwt", GWT_CLASS,
        "meth", getTypeWrapperMethodName(beanType),
        "bean",  beanType.getQualifiedSourceName(),
        "taming", ti.getParameterizedQualifiedSourceName());
  }
  
  private String getTypeUnwrapperMethod(
      GwtBeanIntrospector introspector,
      JClassType beanType)
      throws UnableToCompleteException {
    JClassType ti = getTamingInterfaceOrFail(introspector, beanType);
    return strV(""
        + "public static ${bean} ${meth}(${frame} m, ${jso} jso) {\n"
        + "  return\n"
        + "      ((${taming}) ${gwt}.create(${taming}.class))\n"
        + "      .getBean(m, jso);\n"
        + "}\n",
        "jso", JSO_CLASS,
        "frame", FRAME_CLASS,
        "gwt", GWT_CLASS,
        "meth", getTypeUnwrapperMethodName(beanType),
        "bean",  beanType.getQualifiedSourceName(),
        "taming", ti.getParameterizedQualifiedSourceName());
  }

  private String getArrayCreationMethod(
      JType componentType)
      throws UnableToCompleteException {
    return strV(""
        + "private static ${t}[] ${meth} (int length) {\n"
        + "  return new ${t}[length];\n"
        + "}\n",
        "t", componentType.getQualifiedSourceName(),
        "meth", getArrayCreationMethodName(componentType));
  }

  private String getArrayAssignmentMethod(
      JType componentType)
      throws UnableToCompleteException {
    return strV(""
        + "private static void ${meth}(${t}[] a, int i, ${t} o) {\n"
        + "  a[i] = o;\n"
        + "}\n",
        "t", componentType.getQualifiedSourceName(),
        "meth", getArrayAssignmentMethodName(componentType));
  }

  private String getArrayLengthQueryMethod(
      JType componentType)
      throws UnableToCompleteException {
    return strV(""
        + "private static int ${meth} (${t}[] a) {\n"
        + "  return a.length;\n"
        + "}\n",
        "t", componentType.getQualifiedSourceName(),
        "meth", getArrayLengthQueryMethodName(componentType));
  }

  private String getArrayItemQueryMethod(
      JType componentType)
      throws UnableToCompleteException {
    return strV(""
        + "private static ${t} ${meth}(${t}[] a, int i) {\n"
        + "  return a[i];\n"
        + "}\n",
        "t", componentType.getQualifiedSourceName(),
        "meth", getArrayItemQueryMethodName(componentType));
  }

  private JClassType getTamingInterfaceOrFail(
      GwtBeanIntrospector introspector,
      JClassType beanClass)
      throws UnableToCompleteException {
    JClassType ti = introspector.getBeanInfoByBeanType(beanClass)
        .getTamingInterface();
    if (ti == null) {
      logger.log(Type.ERROR,
          "Unable to proceed because no taming interface "
              + "found for bean type " + beanClass);
      throw new UnableToCompleteException();
    }
    return ti;
  }
  
  private ParseTreeNode makeJsBody(
      GwtBeanIntrospector introspector,
      GwtBeanInfo beanInfo)
      throws UnableToCompleteException {
    List<StringLiteral> keys = new ArrayList<StringLiteral>();
    List<Expression> vals = new ArrayList<Expression>();
    for (JMethod m : beanInfo.getMethods()) {
      keys.add(new StringLiteral(FilePosition.UNKNOWN, m.getName()));
      vals.add(getPropertyDescriptorForMethod(introspector, m));
    }
    for (GwtBeanPropertyDescriptor pd : beanInfo.getProperties()) {
      keys.add(new StringLiteral(FilePosition.UNKNOWN, pd.name));
      vals.add(getPropertyDescriptorForProperty(introspector, pd));
    }
    return QuasiBuilder.substV(
        "var w = $wnd.caja.iframe.contentWindow;" +
        "return w.___.makeDefensibleObject({ @keys*: @vals* });",
        "keys",  new ParseTreeNodeContainer(keys),
        "vals", new ParseTreeNodeContainer(vals));
  }
  
  private Expression getPropertyDescriptorForMethod(
      GwtBeanIntrospector introspector,
      JMethod m)
      throws UnableToCompleteException {
    List<Reference> args = new ArrayList<Reference>();
    for (int i = 0; i < m.getParameters().length; i++) {
      args.add(new Reference(
          new Identifier(FilePosition.UNKNOWN, makeArgName(i))));
    }
    Expression method;
    if (JPrimitiveType.VOID == m.getReturnType()) {
      method = (Expression) QuasiBuilder.substV(""
          + "function (_) {"
          + "  @argDestructuringStatements*;"
          + "  @parameterUnwrapStatements*;"
          + "  bean.@methodRef(@args*);"
          + "}",
          "parameterUnwrapStatements",
              getParameterUnwrapStatements(introspector, m.getParameters()),
          "argDestructuringStatements",
              getArgDestructuringStatements(m),
          "methodRef", getMethodAccessor(m),
          "args", new ParseTreeNodeContainer(args));
    } else {
      method = (Expression) QuasiBuilder.substV(""
          + "function (_) {"
          + "  @argDestructuringStatements*;"
          + "  @parameterUnwrapStatements*;"
          + "  var returnValue = bean.@methodRef(@args*);"
          + "  @returnValueWrapStatement;"
          + "  return returnValue;"
          + "}",
          "parameterUnwrapStatements",
              getParameterUnwrapStatements(introspector, m.getParameters()),
          "argDestructuringStatements",
              getArgDestructuringStatements(m),
          "returnValueWrapStatement",
              getReturnValueWrapStatement(introspector, m.getReturnType()),
          "methodRef", getMethodAccessor(m),
          "args", new ParseTreeNodeContainer(args));
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

  private Statement getArrayParameterUnwrapStatement(
      GwtBeanIntrospector introspector,
      JType componentType,
      ParseTreeNode argRef)
      throws UnableToCompleteException {
    GwtBeanInfo bi = introspector.getBeanInfoByBeanType(
        componentType);
    if (!bi.isTamingPrimitiveType()) {
      ParseTreeNode unwrapAccessor = getTypeUnwrapperAccessor(
          context.getTypeOracle(),
          componentType.isClassOrInterface());
      return new ExpressionStmt(
          (Expression) QuasiBuilder.substV(""
              + "(function() {"
              + "  if (@argRef !== null) {"
              + "    var arr = @newArray(@argRef.length);"
              + "    for (var i = 0; i < @argRef.length; i++) {"
              + "      @assignToArray(arr, i, @unwrapAccessor(m, @argRef[i]));"
              + "    }"
              + "    @argRef = arr;"
              + "  }"
              + "})()",
              "newArray", getNewArrayAccessor(componentType),
              "assignToArray", getArrayAssignmentAccessor(componentType),
              "unwrapAccessor", unwrapAccessor,
              "argRef", argRef));
    } else {
      return new ExpressionStmt(
          (Expression) QuasiBuilder.substV(""
              + "(function() {"
              + "  if (@argRef != null) {"
              + "    var arr = @newArray(@argRef.length);"
              + "    for (var i = 0; i < @argRef.length; i++) {"
              + "      @assignToArray(arr, i, @argRef[i]);"
              + "    }"
              + "    @argRef = arr;"
              + "  }"
              + "})()",
              "newArray", getNewArrayAccessor(componentType),
              "assignToArray", getArrayAssignmentAccessor(componentType),
              "argRef", argRef));
    }
  }

  private Statement getNonArrayParameterUnwrapStatement(
      GwtBeanIntrospector introspector,
      JType type,
      ParseTreeNode argRef)
      throws UnableToCompleteException {
    GwtBeanInfo bi = introspector.getBeanInfoByBeanType(type);
    if (!bi.isTamingPrimitiveType()) {
      ParseTreeNode unwrapAccessor = getTypeUnwrapperAccessor(
          context.getTypeOracle(),
          type.isClassOrInterface());
      return new ExpressionStmt(
          (Expression) QuasiBuilder.substV(
              "@argRef = @unwrapAccessor(m, @argRef)",
              "unwrapAccessor", unwrapAccessor,
              "argRef", argRef));
    } else {
      return new Noop(FilePosition.UNKNOWN);
    }
  }

  private ParseTreeNode getArgDestructuringStatements(
      JMethod m) {
    List<ParseTreeNode> destructurings = new ArrayList<ParseTreeNode>();
    int nDestructurings = m.isVarArgs()
        ? m.getParameters().length - 1
        : m.getParameters().length;
    for (int i = 0; i < nDestructurings; i++) {
      destructurings.add(QuasiBuilder.substV(
          "var @name = arguments[@idx];",
          "name", new Identifier(FilePosition.UNKNOWN, makeArgName(i)),
          "idx", new IntegerLiteral(FilePosition.UNKNOWN, i)));
    }
    if (m.isVarArgs()) {
      destructurings.add(QuasiBuilder.substV(""
          + "var @name = Array.prototype.slice.call("
          + "    arguments,"
          + "    @start,"
          + "    arguments.length);",
          "name", new Identifier(
              FilePosition.UNKNOWN, makeArgName(nDestructurings)),
          "start", new IntegerLiteral(
              FilePosition.UNKNOWN, nDestructurings)));
    }
    return new ParseTreeNodeContainer(destructurings);
  }

  private ParseTreeNodeContainer getParameterUnwrapStatements(
      GwtBeanIntrospector introspector,
      JParameter[] params)
      throws UnableToCompleteException {
    List<Statement> stmts = new ArrayList<Statement>();
    for (int i = 0; i < params.length; i++) {
      JParameter p = params[i];
      JType t = p.getType();
      ParseTreeNode argRef =
          new Reference(new Identifier(
              FilePosition.UNKNOWN, makeArgName(i)));
      if (t.isArray() != null) {
        JType ct = t.isArray().getComponentType();
        stmts.add(getArrayParameterUnwrapStatement(introspector, ct, argRef));
      } else {
        stmts.add(getNonArrayParameterUnwrapStatement(introspector, t, argRef));
      }
    }
    return new ParseTreeNodeContainer(stmts);
  }

  private Statement getArrayReturnValueWrapStatement(
      GwtBeanIntrospector introspector,
      JType componentType)
      throws UnableToCompleteException {
    GwtBeanInfo bi = introspector.getBeanInfoByBeanType(componentType);
    if (!bi.isTamingPrimitiveType()) {
      ParseTreeNode wrapAccessor = getTypeWrapperAccessor(
          context.getTypeOracle(),
          componentType.isClass());
      return new ExpressionStmt(
          (Expression) QuasiBuilder.substV(""
              + "(function() {"
              + "  if (returnValue !== null) {"
              + "    var arr = [];"
              + "    for (var i = 0; i < @queryArrayLength(returnValue); i++) {"
              + "      arr[i] = @wrapAccessor("
              + "          m, @queryArrayItem(returnValue, i));"
              + "    }"
              + "    returnValue = arr;"
              + "  }"
              + "})()",
              "queryArrayLength", getArrayLengthQueryAccessor(componentType),
              "queryArrayItem", getArrayItemQueryAccessor(componentType),
              "wrapAccessor", wrapAccessor));
    } else {
      return new ExpressionStmt(
          (Expression) QuasiBuilder.substV(""
              + "(function() {"
              + "  if (returnValue !== null) {"
              + "    var arr = [];"
              + "    for (var i = 0; i < @queryArrayLength(returnValue); i++) {"
              + "      arr[i] = @queryArrayItem(returnValue, i);"
              + "    }"
              + "    returnValue = arr;"
              + "  }"
              + "})()",
              "queryArrayLength", getArrayLengthQueryAccessor(componentType),
              "queryArrayItem", getArrayItemQueryAccessor(componentType)));
    }
  }

  private Statement getNonArrayReturnValueWrapStatement(
      GwtBeanIntrospector introspector,
      JType type)
      throws UnableToCompleteException {
    GwtBeanInfo bi = introspector.getBeanInfoByBeanType(type);
    if (!bi.isTamingPrimitiveType()) {
      ParseTreeNode wrapAccessor = getTypeWrapperAccessor(
          context.getTypeOracle(),
          type.isClassOrInterface());
      return new ExpressionStmt(
          (Expression) QuasiBuilder.substV(
              "returnValue = @wrapAccessor(m, returnValue)",
              "wrapAccessor", wrapAccessor));
    } else {
      return new Noop(FilePosition.UNKNOWN);
    }
  }

  private Statement getReturnValueWrapStatement(
      GwtBeanIntrospector introspector,
      JType type)
      throws UnableToCompleteException {
    if (type.isArray() != null) {
      JType ct = type.isArray().getComponentType();
      return getArrayReturnValueWrapStatement(introspector, ct);
    } else {
      return getNonArrayReturnValueWrapStatement(introspector, type);
    }
  }

  private Expression getPropertyDescriptorForProperty(
      GwtBeanIntrospector introspector,
      GwtBeanPropertyDescriptor pd)
      throws UnableToCompleteException {
    Expression get = (pd.readMethod == null) ?
        new Reference(new Identifier(FilePosition.UNKNOWN, UNDEFINED)) :
        (Expression) QuasiBuilder.substV(""
            + "w.___.makeDefensibleFunction(function () {"
            + "  var returnValue = bean.@methodRef();"
            + "  @returnValueWrapStatement;"
            + "  return returnValue;"
            + "})",
            "returnValueWrapStatement",
                getReturnValueWrapStatement(
                    introspector, pd.readMethod.getReturnType()),
            "methodRef", getMethodAccessor(pd.readMethod));
    Expression set = (pd.writeMethod == null) ?
        new Reference(new Identifier(FilePosition.UNKNOWN, UNDEFINED)) :
        (Expression) QuasiBuilder.substV(""
            + "w.___.makeDefensibleFunction(function (@formal) {"
            + "  @parameterUnwrapStatements*;"
            + "  bean.@methodRef(@actual);"
            + "})",
            "formal", new FormalParam(
                new Identifier(FilePosition.UNKNOWN, makeArgName(0))),
            "actual",
                new Reference(
                    new Identifier(FilePosition.UNKNOWN, makeArgName(0))),
            "parameterUnwrapStatements",
                getParameterUnwrapStatements(
                    introspector, pd.writeMethod.getParameters()),
            "methodRef", getMethodAccessor(pd.writeMethod));
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
    return new Reference(new Identifier(
        FilePosition.UNKNOWN, m.getJsniSignature()));
  }
  public Reference getTypeWrapperAccessor(
      TypeOracle to,
      JClassType beanType) {
    toWrap.add(beanType);
    return new Reference(
        new Identifier(
            FilePosition.UNKNOWN,
            "@" + tamingImplClassName
                + "::" + getTypeWrapperMethodName(beanType) + "("
                + to.findType(FRAME_CLASS).getJNISignature()
                + beanType.getJNISignature()
                + ")"));
  }

  private Reference getTypeUnwrapperAccessor(
      TypeOracle to,
      JClassType beanType) {
    toUnwrap.add(beanType);
    return new Reference(
        new Identifier(
            FilePosition.UNKNOWN,
            "@" + tamingImplClassName + "::"
                + getTypeUnwrapperMethodName(beanType) + "("
                + to.findType(FRAME_CLASS).getJNISignature()
                + to.findType(JSO_CLASS).getJNISignature()
                + ")"));
  }

  private Reference getNewArrayAccessor(JType type) {
    toArrayCreateAndAssign.add(type);
    return new Reference(
        new Identifier(
            FilePosition.UNKNOWN,
            "@" + tamingImplClassName + "::"
                + getArrayCreationMethodName(type) + "(I)"));
  }

  private Reference getArrayAssignmentAccessor(JType type) {
    toArrayCreateAndAssign.add(type);
    return new Reference(
        new Identifier(
            FilePosition.UNKNOWN,
            "@" + tamingImplClassName + "::"
                + getArrayAssignmentMethodName(type)
                + "("
                + "[" + type.getJNISignature()
                + "I"
                + type.getJNISignature()
                + ")"));
  }

  private Reference getArrayLengthQueryAccessor(JType type) {
    toArrayCreateAndAssign.add(type);
    return new Reference(
        new Identifier(
            FilePosition.UNKNOWN,
            "@" + tamingImplClassName + "::"
                + getArrayLengthQueryMethodName(type)
                + "("
                + "[" + type.getJNISignature()
                + ")"));
  }

  private Reference getArrayItemQueryAccessor(JType type) {
    toArrayCreateAndAssign.add(type);
    return new Reference(
        new Identifier(
            FilePosition.UNKNOWN,
            "@" + tamingImplClassName + "::"
                + getArrayItemQueryMethodName(type)
                + "("
                + "[" + type.getJNISignature()
                + "I"
                + ")"));
  }

  private static String getTypeWrapperMethodName(JType beanType) {
    return getTypeSpecificMethodName(beanType) + "_getJso";
  }
  
  private static String getTypeUnwrapperMethodName(JType beanType) {
    return getTypeSpecificMethodName(beanType) + "_getBean";
  }

  private static String getArrayCreationMethodName(JType componentType) {
    return getTypeSpecificMethodName(componentType) + "_newArray";
  }
  
  private static String getArrayAssignmentMethodName(JType componentType) {
    return getTypeSpecificMethodName(componentType) + "_assignToArray";
  }

  private static String getArrayLengthQueryMethodName(JType componentType) {
    return getTypeSpecificMethodName(componentType) + "_getArrayLength";
  }

  private static String getArrayItemQueryMethodName(JType componentType) {
    return getTypeSpecificMethodName(componentType) + "_getArrayItem";
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

  private static String makeArgName(int idx) {
    return "arg_" + idx;
  }

  private String strV(String template, String... args)
      throws UnableToCompleteException {
    if ((args.length % 2) != 0) {
      // Four armed is forewarned!
      logger.log(Type.ERROR, ""
          + "String interpolation error: odd number of parameters specified"
          + " for template <" + template + ">");
      throw new UnableToCompleteException();
    }
    String result = template;
    for (int i = 0; i < args.length; i += 2) {
      if (args[i + 1].contains("${")) {
        logger.log(Type.ERROR, ""
            + "String interpolation error: in template <" + template + ">"
            + " parameter <" + args[i] + ">"
            + " has malformed value <" + args[i + 1] + ">");
        throw new UnableToCompleteException();
      }
      result = result.replace("${" + args[i] + "}", args[i + 1]);
    }
    if (result.contains("${")) {
      logger.log(Type.ERROR, ""
          + "String interpolation error: template <" + template + ">"
          + " incompletely expanded to <" + result + ">");
      throw new UnableToCompleteException();
    }
    return result;
  }
}
