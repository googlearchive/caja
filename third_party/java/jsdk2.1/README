$Id: readme,v 1.5 1999/04/21 17:09:14 duncan Exp $
	
Java(tm) Servlet Development Kit
Version 2.1

Released: April 1999

This is version 2.1 of the Java(tm) Servlet Development Kit from Sun
Microsystems, Inc. All classes are written exclusively in the Java(tm)
Programming Language and may be used with and JRE 1.1 conformant
system, including Java2 conformant systems.

This release has been developed and tested on the following systems:

    Sparc Solaris 2.6 JDK 1.1.7 & Java2 SDK 1.2
    Windows98/NT JDK 1.1.7 & Java2 SDK 1.2

You should read the License Agreement which applies to this software. 

At this time, the documentation for this release is primarily:

     This README document
     The JavaDoc documentation
     API Examples

RUNNING THE JSDK
----------------

In order to run the JSDK it is necessary to have installed a compliant
JDK. You must also have the "java" program available in your path (set
using the PATH environment variable). More information can be found at
http://java.sun.com/products/jdk.

There is a Unix based Korn Shell Script and a Windows based Batch File
provided in the installation directory of the JSDK. To startup the
server on Unix:

     % startserver

To start up the server on Windows

     C:\jsdk\> startserver

Once the server is running, you can use any web browser to browse

     http://localhost:8080

and you will be browsing pages served from the server. You can read
the JavaDoc documentation for the javax.servlet packages as well as
see some examples.

To stop the server, use the following commands:

    Unix: 		% stopserver
    Windows: 		> stopserver

CHANGES TO NOTE
---------------
The JSDK configuration files have changed significantly. To set
general server properties, such as port and hostname, edit the
default.cfg file in the base directory of the JSDK. To edit servlet
mappings, mime types and other properties, edit the respective files
in the <install_dir>/webpagtes/WEB-INF directory.

The servlets directory is located at

    <install_dir>/webpages/WEB-INF/servlets

Automatic servlet reloading is not operational in this release of the
JSDK.

TROUBLESHOOTING
---------------

On a Windows 95/98 machine you may see an "Out of Environment Space"
error message when starting the server. This is caused by Windows
providing too small a space for environment variables. To work around
this limitation:

    Close the DOS window (the error can corrupt its CLASSPATH variable)
    Open a new MS-DOS window
    Click on the MS-DOS icon at the top-left of the Window
    Select teh Properties option
    Click on the "Memory" tab
    Adjust the "Initial Environment" drop-down box from "Auto" to "2816"
    Click on OK
    Start the server

FEEDBACK
--------

Please send feedback on this software to
<servletapi-feedback@eng.sun.com>.

________________________________________________________________________
Java is a trademark of Sun Microsystems, Inc.
