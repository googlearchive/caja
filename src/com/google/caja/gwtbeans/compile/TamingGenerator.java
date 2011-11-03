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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.caja.gwtbeans.shared.AbstractTaming;
import com.google.caja.gwtbeans.shared.Frame;
import com.google.caja.gwtbeans.shared.Taming;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.Reference;
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
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.util.collect.HashSet;

public class TamingGenerator {
  private static final String OBJECT_CLASS =
      Object.class.getCanonicalName();
  private static final String TAMING_INTERFACE =
      Taming.class.getCanonicalName();
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
  private final Set<JClassType> toGenerateTamingAccessors =
      new HashSet<JClassType>();
  private final Set<JType> toArrayCreateAndAssign = new HashSet<JType>();
  private final Map<JType, Expression> primitiveTamingObjects =
      new HashMap<JType, Expression>();

  public TamingGenerator(
      TreeLogger logger,
      GeneratorContext context,
      String tamingInterfaceName)
      throws UnableToCompleteException {
    this.logger = logger;
    this.context = context;
    this.tamingInterfaceName = tamingInterfaceName;
    this.introspector = new GwtBeanIntrospector(logger, context);
    initializePrimitiveTamingObjects();
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

    ParseTreeNode jsBody = makeJsBody(beanInfo);
    
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
        + "  public native ${jso}\n"
        + "  getNative(${frame} frame, ${bean} bean) /*-{\n"
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
        "helperMethods", getJavaHelperMethods()));

    logger.log(Type.INFO, "Creating class " + tamingImplClassName);
    logger.log(Type.INFO, sb.toString());

    PrintWriter pw = context.tryCreate(
        logger, tamingInterfacePackageName, tamingImplClassShortName);
    pw.write(sb.toString());
    context.commit(logger, pw);

    return tamingImplClassName;
  }
  
  private String getJavaHelperMethods()
      throws UnableToCompleteException {
    StringBuilder sb = new StringBuilder();
    for (JClassType beanType : toGenerateTamingAccessors) {
      sb.append(getTamingGetterMethod(beanType)).append("\n");
    }
    for (JType type : toArrayCreateAndAssign) {
      sb.append(getArrayCreationMethod(type)).append("\n");
      sb.append(getArrayAssignmentMethod(type)).append("\n");
      sb.append(getArrayLengthQueryMethod(type)).append("\n");
      sb.append(getArrayItemQueryMethod(type)).append("\n");
    }
    return sb.toString();
  }

  private String getTamingGetterMethod(
      JClassType beanType)
      throws UnableToCompleteException {
    JClassType ti = getTamingInterfaceOrFail(beanType);
    return strV(""
        + "public static ${taming} ${meth}() {\n"
        + "  return ((${taming}) ${gwt}.create(${taming}.class));\n"
        + "}\n",
        "gwt", GWT_CLASS,
        "meth", getTamingGetterMethodName(beanType),
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
      GwtBeanInfo beanInfo)
      throws UnableToCompleteException {
    List<StringLiteral> keys = new ArrayList<StringLiteral>();
    List<Expression> vals = new ArrayList<Expression>();
    Map<String, Set<JMethod>> methodGroups =
        getMethodGroups(beanInfo.getMethods());
    for (String name : methodGroups.keySet()) {
      keys.add(new StringLiteral(FilePosition.UNKNOWN, name));
      vals.add(getPropertyDescriptorForMethodGroup(methodGroups.get(name)));
    }
    for (GwtBeanPropertyDescriptor pd : beanInfo.getProperties()) {
      keys.add(new StringLiteral(FilePosition.UNKNOWN, pd.name));
      vals.add(getPropertyDescriptorForProperty(pd));
    }
    return QuasiBuilder.substV(
        "var w = $wnd.caja.iframe.contentWindow;" +
        "return w.___.makeDefensibleObject({ @keys*: @vals* });",
        "keys",  new ParseTreeNodeContainer(keys),
        "vals", new ParseTreeNodeContainer(vals));
  }
  
  private Expression getPropertyDescriptorForMethodGroup(
      Set<JMethod> methods)
      throws UnableToCompleteException {
    return (Expression) QuasiBuilder.substV(""
        + "({"
        + "  value: w.___.makeDefensibleFunction(function(_) {"
        + "    var dispatchTable = @dispatchTable;"
        + "    return @dispatch()(frame, dispatchTable, arguments);"
        + "  }),"
        + "  enumerable: true,"
        + "  writable: false,"
        + "  configurable: false"
        + "})",
        "dispatchTable", getDispatchTable(methods),
        "dispatch", getMethodDispatcherAccessor());
  }

  private Expression getDispatchTable(Set<JMethod> methods)
      throws UnableToCompleteException {
    List<ParseTreeNode> records = new ArrayList<ParseTreeNode>();
    for (JMethod m : methods) {
      records.add(getDispatchRecord(m));
    }
    return (Expression) QuasiBuilder.substV(""
        + "[ @records* ]",
        "records", new ParseTreeNodeContainer(records));
  }

  private Expression getDispatchRecord(JMethod m)
      throws UnableToCompleteException {
    return (Expression) QuasiBuilder.substV(""
        + "({"
        + "  signature: @signature,"
        + "  invoke: @invoke,"
        + "  unwrap: @unwrap,"
        + "  wrap: @wrap"
        + "})",
        "signature", new StringLiteral(
            FilePosition.UNKNOWN, m.getJsniSignature()),
        "invoke", getInvoke(m),
        "unwrap", getUnwrap(m),
        "wrap", getWrap(m));
  }

  private ParseTreeNode getVarArgsDestructuring(JMethod m)
      throws UnableToCompleteException {
    if (!m.isVarArgs()) {
      return QuasiBuilder.substV(""
          + "function(args) { return args; }");
    }
    List<ParseTreeNode> vals = new ArrayList<ParseTreeNode>();
    for (int i = 0; i < m.getParameters().length - 1; i++) {
      vals.add(QuasiBuilder.substV(""
          + "args[@idx];",
          "idx", new IntegerLiteral(FilePosition.UNKNOWN, i)));
    }
    vals.add(QuasiBuilder.substV(""
        + "Array.prototype.slice.call(args, @idx, args.length);",
        "idx", new IntegerLiteral(
            FilePosition.UNKNOWN, m.getParameters().length - 1)));
    return QuasiBuilder.substV(""
        + "function(args) { return [ @vals* ]; }",
        "vals", new ParseTreeNodeContainer(vals));
  }

  private ParseTreeNode getInvoke(JMethod m)
      throws UnableToCompleteException {
    List<ParseTreeNode> formals = new ArrayList<ParseTreeNode>();
    List<ParseTreeNode> actuals = new ArrayList<ParseTreeNode>();
    for (int i = 0; i < m.getParameters().length; i++) {
      formals.add(new FormalParam(
          new Identifier(FilePosition.UNKNOWN, makeArgName(i))));
      actuals.add(new Reference(
          new Identifier(FilePosition.UNKNOWN, makeArgName(i))));
    }
    return QuasiBuilder.substV(""
        + "function (@formals*) { return bean.@methodRef(@actuals*); }",
        "methodRef", getMethodAccessor(m),
        "formals", new ParseTreeNodeContainer(formals),
        "actuals", new ParseTreeNodeContainer(actuals));
  }

  private ParseTreeNode getUnwrap(JMethod m)
      throws UnableToCompleteException {
    List<ParseTreeNode> unwraps = new ArrayList<ParseTreeNode>();
    for (int i = 0; i < m.getParameters().length; i++) {
      unwraps.add(QuasiBuilder.substV(""
          + "@taming.getBean(frame, args[@idx]);",
          "taming", getTamingObject(m.getParameters()[i].getType()),
          "idx", new IntegerLiteral(FilePosition.UNKNOWN, i)));
    }
    return QuasiBuilder.substV(""
        + "function (frame, args) {"
        + "  @arityCheck;"
        + "  args = @varArgsDestructuring(args);"
        + "  return [ @unwraps* ];"
        + "}",
        "arityCheck", getArityCheck(m),
        "varArgsDestructuring", getVarArgsDestructuring(m),
        "unwraps", new ParseTreeNodeContainer(unwraps));
  }

  private ParseTreeNode getArityCheck(JMethod m) {
    return m.isVarArgs()
        ? QuasiBuilder.substV(""
            + "if (args.length < @num) {"
            + "  throw new TypeError(''"
            + "      + 'Method called with incorrect arity:'"
            + "      + ' expected at least ' + @num + ' arguments'"
            + "      + ' but was ' + args.length);"
            + "}",
            "num", new IntegerLiteral(
                FilePosition.UNKNOWN, m.getParameters().length - 1))
        : QuasiBuilder.substV(""
            + "if (args.length !== @num) {"
            + "  throw new TypeError(''"
            + "      + 'Method called with incorrect arity:'"
            + "      + ' expected ' + @num + ' arguments'"
            + "      + ' but was ' + args.length);"
            + "}",
            "num", new IntegerLiteral(
                FilePosition.UNKNOWN, m.getParameters().length));
  }

  private ParseTreeNode getWrap(JMethod m)
      throws UnableToCompleteException {
    return QuasiBuilder.substV(""
        + "function (frame, retval) {"
        + "  return @taming.getJso(frame, retval);"
        + "}",
        "taming", getTamingObject(m.getReturnType()));
  }

  private Expression getTamingObject(JType type)
      throws UnableToCompleteException {
    if (type.isArray() != null) {
      return getArrayTamingObject(type.isArray().getComponentType());
    } else {
      return getNonArrayTamingObject(type);
    }
  }

  private Expression getNonArrayTamingObject(JType type)
      throws UnableToCompleteException {
    if (primitiveTamingObjects.containsKey(type)) {
      return primitiveTamingObjects.get(type);
    }
    if (!(type instanceof JClassType)) {
      logger.log(Type.ERROR,
          "Cannot tame non-class type " + type.getQualifiedSourceName());
      throw new UnableToCompleteException();
    }
    return getClassTamingObject((JClassType) type);
  }

  private Expression getClassTamingObject(JClassType type) {
    return (Expression) QuasiBuilder.substV(""
        + "({"
        + "  getJso: function(frame, bean) {"
        + "    return @t().@getJso(frame, bean);"
        + "  },"
        + "  getBean: function(frame, jso) {"
        + "    return @t().@getBean(frame, jso);"
        + "  }"
        + "})",
        "t", getTamingGetterAccessor(type),
        "getJso", getGetJsoAccessor(),
        "getBean", getGetBeanAccessor());
  }

  private Expression getArrayTamingObject(JType componentType)
      throws UnableToCompleteException {
    ParseTreeNode getJso = QuasiBuilder.substV(""
        + "(function(frame, bean) {"
        + "  if (bean === null) { return null; }"
        + "  var arr = [];"
        + "  var taming = @taming;"
        + "  for (var i = 0; i < @queryArrayLength(bean); i++) {"
        + "    arr[i] = taming.getJso(frame, @queryArrayItem(bean, i));"
        + "  }"
        + "  return arr;"
        + "})",
        "queryArrayLength", getArrayLengthQueryAccessor(componentType),
        "queryArrayItem", getArrayItemQueryAccessor(componentType),
        "taming", getTamingObject(componentType));
    ParseTreeNode getBean = QuasiBuilder.substV(""
        + "(function(frame, jso) {"
        + "  if (jso === null || jso === undefined) { return null; }"
        + "  var arr = @newArray(jso.length);"
        + "  var taming = @taming;"
        + "  for (var i = 0; i < jso.length; i++) {"
        + "    @assignToArray("
        + "        arr,"
        + "        i,"
        + "        taming.getBean(frame, jso[i]));"
        + "  }"
        + "  return arr;"
        + "})",
        "newArray", getNewArrayAccessor(componentType),
        "assignToArray", getArrayAssignmentAccessor(componentType),
        "taming", getTamingObject(componentType));
    return (Expression) QuasiBuilder.substV(""
        + "({"
        + "  getJso: @getJso,"
        + "  getBean: @getBean"
        + "})",
        "getJso", getJso,
        "getBean", getBean);
  }

  private Expression getPropertyDescriptorForProperty(
      GwtBeanPropertyDescriptor pd)
      throws UnableToCompleteException {
    Expression get = (pd.readMethod == null) ?
        new Reference(new Identifier(FilePosition.UNKNOWN, UNDEFINED)) :
        (Expression) QuasiBuilder.substV(""
            + "w.___.makeDefensibleFunction(function () {"
            + "  return @taming.getJso(frame, bean.@methodRef());"
            + "})",
            "taming", getTamingObject(pd.readMethod.getReturnType()),
            "methodRef", getMethodAccessor(pd.readMethod));
    Expression set = (pd.writeMethod == null) ?
        new Reference(new Identifier(FilePosition.UNKNOWN, UNDEFINED)) :
        (Expression) QuasiBuilder.substV(""
            + "w.___.makeDefensibleFunction(function (arg) {"
            + "  bean.@methodRef(@taming.getBean(frame, arg));"
            + "})",
            "taming", getTamingObject(
                pd.writeMethod.getParameters()[0].getType()),
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

  public Reference getTamingGetterAccessor(JClassType beanType) {
    toGenerateTamingAccessors.add(beanType);
    return new Reference(
        new Identifier(
            FilePosition.UNKNOWN,
            "@" + tamingImplClassName
                + "::" + getTamingGetterMethodName(beanType)
                + "("
                + ")"));
  }

  private Reference getNewArrayAccessor(JType type) {
    toArrayCreateAndAssign.add(type);
    return new Reference(
        new Identifier(
            FilePosition.UNKNOWN,
            "@" + tamingImplClassName + "::"
                + getArrayCreationMethodName(type)
                + "(" 
                + "I"
                + ")"));
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

  private Reference getMethodDispatcherAccessor() {
    return new Reference(
        new Identifier(
            FilePosition.UNKNOWN,
            "@" + TAMING_COMMON_BASE_CLASS + "::"
                + "getMethodDispatcher"
                + "("
                + ")"));
  }

  private Reference getGetJsoAccessor() {
    return new Reference(
        new Identifier(
            FilePosition.UNKNOWN,
            "@" + TAMING_INTERFACE + "::"
                + "getJso"
                + "("
                + context.getTypeOracle().findType(FRAME_CLASS)
                    .getJNISignature()
                + context.getTypeOracle().findType(OBJECT_CLASS)
                    .getJNISignature()
                + ")"));

  }

  private Reference getGetBeanAccessor() {
    return new Reference(
        new Identifier(
            FilePosition.UNKNOWN,
            "@" + TAMING_INTERFACE + "::"
                + "getBean"
                + "("
                + context.getTypeOracle().findType(FRAME_CLASS)
                    .getJNISignature()
                + context.getTypeOracle().findType(JSO_CLASS)
                    .getJNISignature()
                + ")"));
  }

  private static String getTamingGetterMethodName(JType beanType) {
    return getTypeSpecificMethodName(beanType) + "_getTaming";
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

  private Map<String, Set<JMethod>> getMethodGroups(JMethod[] methods) {
    Map<String, Set<JMethod>> methodGroups =
        new HashMap<String, Set<JMethod>>();
    for (JMethod m : methods) {
      if (!methodGroups.containsKey(m.getName())) {
        methodGroups.put(m.getName(), new HashSet<JMethod>());
      }
      methodGroups.get(m.getName()).add(m);
    }
    return methodGroups;
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

  private void initializePrimitiveTamingObjects() {
    primitiveTamingObjects.put(
        JPrimitiveType.BOOLEAN,
        (Expression) QuasiBuilder.substV(""
            + "({"
            + "  getJso: function(frame, bean) { return bean; },"
            + "  getBean: function(frame, jso) {"
            + "    if ((typeof jso) === 'boolean') return jso;"
            + "    throw new TypeError('Not a boolean: ' + jso);"
            + "  }"
            + "})"));
    primitiveTamingObjects.put(
        JPrimitiveType.BYTE,
        (Expression) QuasiBuilder.substV(""
            + "({"
            + "  getJso: function(frame, bean) { return bean; },"
            + "  getBean: function(frame, jso) {"
            + "    if ((typeof jso) === 'number') return jso;"
            + "    throw new TypeError("
            + "        'Not a number (cannot pass as byte): ' + jso);"
            + "  }"
            + "})"));
    primitiveTamingObjects.put(
        JPrimitiveType.CHAR,
        (Expression) QuasiBuilder.substV(""
            + "({"
            + "  getJso: function(frame, bean) { return bean; },"
            + "  getBean: function(frame, jso) {"
            + "    if ((typeof jso) === 'number') return jso;"
            + "    throw new TypeError("
            + "        'Not a number (cannot pass as char): ' + jso);"
            + "  }"
            + "})"));
    primitiveTamingObjects.put(
        JPrimitiveType.DOUBLE,
        (Expression) QuasiBuilder.substV(""
            + "({"
            + "  getJso: function(frame, bean) { return bean; },"
            + "  getBean: function(frame, jso) {"
            + "    if ((typeof jso) === 'number') return jso;"
            + "    throw new TypeError("
            + "        'Not a number (cannot pass as double): ' + jso);"
            + "  }"
            + "})"));
    primitiveTamingObjects.put(
        JPrimitiveType.FLOAT,
        (Expression) QuasiBuilder.substV(""
            + "({"
            + "  getJso: function(frame, bean) { return bean; },"
            + "  getBean: function(frame, jso) {"
            + "    if ((typeof jso) === 'number') return jso;"
            + "    throw new TypeError("
            + "        'Not a number (cannot pass as float): ' + jso);"
            + "  }"
            + "})"));
    primitiveTamingObjects.put(
        JPrimitiveType.INT,
        (Expression) QuasiBuilder.substV(""
            + "({"
            + "  getJso: function(frame, bean) { return bean; },"
            + "  getBean: function(frame, jso) {"
            + "    if ((typeof jso) === 'number') return jso;"
            + "    throw new TypeError("
            + "        'Not a number (cannot pass as int): ' + jso);"
            + "  }"
            + "})"));
    primitiveTamingObjects.put(
        JPrimitiveType.SHORT,
        (Expression) QuasiBuilder.substV(""
            + "({"
            + "  getJso: function(frame, bean) { return bean; },"
            + "  getBean: function(frame, jso) {"
            + "    if ((typeof jso) === 'number') return jso;"
            + "    throw new TypeError("
            + "        'Not a number (cannot pass as short): ' + jso);"
            + "  }"
            + "})"));
    primitiveTamingObjects.put(
        JPrimitiveType.VOID,
        (Expression) QuasiBuilder.substV(""
            + "({"
            + "  getJso: function(frame, bean) { },"
            + "  getBean: function(frame, jso) { }"
            + "})"));
    primitiveTamingObjects.put(
        context.getTypeOracle().findType("java.lang.String"),
        (Expression) QuasiBuilder.substV(""
            + "({"
            + "  getJso: function(frame, bean) { return bean; },"
            + "  getBean: function(frame, jso) {"
            + "    if (jso === null) { return null; }"
            + "    if ((typeof jso) === 'string') return jso;"
            + "    throw new TypeError('Not a string: ' + jso);"
            + "  }"
            + "})"));
  }
}
