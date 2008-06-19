import com.sun.web.core.*;
import com.sun.web.server.*;
import java.net.*;

/**
 * This is a very simple example of how to start up the HttpServer
 * from any Java based program. Note that this example uses APIs
 * which are not yet finalized and subject to change. Use such
 * information at your own risk.
 */

public class SimpleStartup {

    public static void main(String[] args) {
	int port = 8080;
	InetAddress inet = null; // null uses all inets on the machine
	String hostname = null;
	HttpServer server = new HttpServer(port, inet, hostname);
	try {
	    URL url = resolveURL("webpages");
	    server.setDocumentBase(url);
	    System.out.println("Starting with docbase of: " + url);
	    server.start();
	} catch (MalformedURLException mue) {
	    System.out.println("Malformed URL Exception for doc root");
	    System.out.println(mue.getMessage());
	} catch (HttpServerException hse) {
	    System.out.println("Server threw an exception while running");
	    System.out.println(hse.getMessage());
	}

	// when you want to stop the server, simply call
	// server.stop();
    }
    

    private static URL resolveURL(String s) throws MalformedURLException {
	// if the string contains the magic :/, then we assume
	// that it's a real URL and do nothing
	
	if (s.indexOf(":/") > -1) {
	    return new URL(s);
	}
	    
	// otherwise, we assume that we've got a file name and
	// need to construct a file url appropriatly.
	
	if (s.startsWith("/")) {
	    return new URL("file", null, s);
	} else {
	    String pwd = System.getProperty("user.dir");
	    return new URL("file", null, pwd + "/" + s);
	}
    }


}








