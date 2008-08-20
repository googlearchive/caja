/* Tests typeof and instanceof on wrappers, and also takes a look
 * at the consequences of inheritance on shadow objects.
 *
 * @author: Adrienne Felt (adriennefelt@gmail.com)
 */
 
// Wrapper TYPES
document.write("<span class='comment'>Tests wrapper TYPES.<br />" +
    "Should not be undefined.<br /></span>");

function printTypes(obj,ind) {
    var indent = "";
    for (var i = 0; i < ind; i++) { 
        indent += "&nbsp;&nbsp;&nbsp;&nbsp;";
    }
    for (var a in obj) {
        if (caja.canInnocentEnum(obj,a)) {
            var wraptype = typeof(obj[a]);
            document.write(indent + "<b>" + a + "</b>, ");
            if (wraptype == "undefined") {
                document.write("ERROR, "); 
            }
            document.write(wraptype + "<br />");
            if (wraptype == "object") {
                printTypes(obj[a],ind+1);
            }
        }
    }
}

printTypes(wrapmap.Alice,0);

// Wrapper CLASSES
document.write("<br /><span class='comment'>Tests wrapper CLASSES.<br />" +
    "Wrappers should be \"Pet\", not \"Object\".<br /></span>");

function printClasses(obj,ind) {
    var indent = "";
    for (var i = 0; i < ind; i++) { 
        indent += "&nbsp;&nbsp;&nbsp;&nbsp;";
    }
    for (var a in obj) {
        if (caja.canInnocentEnum(obj,a)) {
            if (typeof(obj[a]) == "object") {
                var wrapclass = getClass(obj[a]);
                document.write(indent + "<b>" + a + "</b>, ");
                document.write(wrapclass + "<br />");
                printClasses(obj[a],ind+1);
            }
        }
    }
}

printClasses(wrapmap.Alice,0);

// Shows how the wrapper inherits properties and values from
// the parent object
document.write("<br /><span class='comment'>Inherited Alice wrapper properties<br /></span>");        
for (var el in wrapmap.Alice) {
    if (caja.canInnocentEnum(wrapmap.Alice, el)) { 
        document.write(el + ": " + wrapmap.Alice[el] + "<br />");
    }
}

// Shows how the inherited properties fail the canReadProp test because
// they aren't hasOwnProp
document.write("<br /><span class='comment'>___.canReadProp(wrapmap.Alice,name): </span>" + 
    ___.canReadProp(wrapmap.Alice,"name"));
document.write("<br /><span class='comment'>___.hasOwnProp(wrapmap.Alice,name): </span>" + 
    ___.hasOwnProp(wrapmap.Alice,"name"));