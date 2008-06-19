var TestDoc = {
	fails: 0,
	all: 0,
	passes: 0,
	results: []
};

TestDoc.record = function(result) {
	TestDoc.results.push(result);
	if (result.verdict === false) TestDoc.fails++;
	if (result.verdict === true) TestDoc.passes++;
	TestDoc.all++;
}

TestDoc.add = function(path) {
	load(path);
	var src = readFile(path);
	var chunks = src.split(/\/\*~t/);
	
	var run = function(chunk) { eval(chunk); }
	for (var start = -1, end = 0; (start = src.indexOf("/*~t", end)) > end; start = end) {
		run(
			src.substring(
				start+4,
				(end = src.indexOf("*/", start))
			)
		);
	}
}

TestDoc.Result = function(verdict, message) {
	this.verdict = verdict;
	this.message = message;
}

TestDoc.Result.prototype.toString = function() {
	return this.verdict + " - " + this.message;
}

TestDoc.report = function() {
	print(TestDoc.results.join("\n"));
	print("------------------------");
	print(TestDoc.fails + "/" + TestDoc.all + " tests failed.")
}

function assert(message) {
	TestDoc.record(new TestDoc.Result(null, message));
}

function assertEqual(a, b, message) {
	var result = (a == b);
	if (!result) message += "\n# " + a + " does not equal " + b;
	TestDoc.record(new TestDoc.Result(result, message));
}

function assertNotEqual(a, b, message) {
	var result = (a != b);
	if (!result) message += "\n# " + a + " equals " + b;
	TestDoc.record(new TestDoc.Result(result, message));
}

function requires(file) {
	if (!requires.loaded[file]) {
		load(file);
		requires.loaded[file] = true;
	}
}
requires.loaded = {};