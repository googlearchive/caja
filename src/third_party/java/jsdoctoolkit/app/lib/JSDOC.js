/**
	This is the main container for the JSDOC application.
	@namespace
*/
JSDOC = {
};

JSDOC.opt = Opt.get(
	arguments, 
	{
		d: "directory",
		c: "conf",
		t: "template",
		r: "recurse",
		x: "ext",
		p: "private",
		a: "allfunctions", 
		e: "encoding",
		n: "nocode",
		o: "out",
		s: "suppress",
		T: "testmode",
		h: "help",
		v: "verbose",
		"D[]": "define",
		"H[]": "handler"
	}
);

/** The current version string of this application. */
JSDOC.VERSION = "2.0.0";

/** Print out usage information and quit. */
JSDOC.usage = function() {
	print("USAGE: java -jar jsrun.jar app/run.js [OPTIONS] <SRC_DIR> <SRC_FILE> ...");
	print("");
	print("OPTIONS:");
	print("  -a or --allfunctions\n          Include all functions, even undocumented ones.\n");
	print("  -c or --conf\n          Load a configuration file.\n");
	print("  -d=<PATH> or --directory=<PATH>\n          Output to this directory (defaults to \"out\").\n");
	print("  -D=\"myVar:My value\" or --define=\"myVar:My value\"\n          Multiple. Define a variable, available in JsDoc as JSDOC.opt.D.myVar\n");
	print("  -e=<ENCODING> or --encoding=<ENCODING>\n          Use this encoding to read and write files.\n");
	print("  -h or --help\n          Show this message and exit.\n");
	//print("  -H=ext:handler or --handler=ext:handler\n          Multiple. Load handlers/handler.js to handle files with .ext names.\n");
	print("  -n or --nocode\n          Ignore all code, only document comments with @name tags.\n");
	print("  -o=<PATH> or --out=<PATH>\n          Print log messages to a file (defaults to stdout).\n");
	print("  -p or --private\n          Include symbols tagged as private, underscored and inner symbols.\n");
	print("  -r=<DEPTH> or --recurse=<DEPTH>\n          Descend into src directories.\n");
	print("  -s or --suppress\n          Suppress source code output.\n");
	print("  -t=<PATH> or --template=<PATH>\n          Required. Use this template to format the output.\n");
	print("  -T or --test\n          Run all unit tests and exit.\n");
	print("  -v or --verbose\n          Provide verbose feedback about what is happening.\n");

	print("  -x=<EXT>[,EXT]... or --ext=<EXT>[,EXT]...\n          Scan source files with the given extension/s (defaults to js).\n");
	
	quit();
}

IO.includeDir("lib/JSDOC/");
