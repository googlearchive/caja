// try: java -jar ../../jsrun.jar runner.js

load("TestDoc.js");

TestDoc.add("../frame/String.js");
TestDoc.add("../lib/JSDOC/DocTag.js");
TestDoc.add("../lib/JSDOC/DocComment.js");
TestDoc.add("../lib/JSDOC/TokenReader.js");
TestDoc.add("../lib/JSDOC/Symbol.js");

TestDoc.report();
