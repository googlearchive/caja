Description
-----------

Grand is a tool to create visual representation of ant target dependencies.
It works by taking a ant build file and creating a "dot" file. This file
need to be post-processed with Graphviz to produce the actual graph.


Usage
-----

Grand's front end is an ant task that must be defined with the following line:

	<typedef resource="net/ggtools/grand/antlib.xml"/>
	
You may have to had classpath="path_to_grand.jar" if the grand jar isn't
already in the classpath.

The grand task takes the following attributes:

  output  			name of the output file (mandatory).
  
  buildfile 		name of the build file to graph. If omitted, the current
  					build file will be used.
  					
  outputconfigfile 	a property file to configure the output.
  
  showgraphname 	if true, the generated graph will have the ant project's
  					name displayed a the graph label.
  					
  inheritall 		If true, the properties of the current build file will be
  					passed to the graphed build file. The default is not to
  					pass the properties to the graphed project.

Beside these attributes, the task can take as nested elements: property,
propertyset and filter. The first two are the standard Ant elements and are
used to pass properties to the graphed project.

The filter element is used to apply filter on the full graph. The filter
element accepts the following attributes:

  name  	filter's name. Can be one of isolatednode, fromnode, tonode,
  			missingnode or connect, mandatory.
  			
  node	 	a node's name. Depending of the selected filter, this attribute
  			can have different meanings and can or cannot be mandatory.

Isolated node
    Removes isolated nodes (i.e.: nodes with no links) from the graph. The
    node attribute is not used by this filter.
    
Missing node
    Removes nodes created when a link make reference to a non existing one.
    The node attribute is not used by this filter.
    
From node
    Keeps only a selected node a the nodes it depends upon. The node parameter
    is the name of the node to start from.
    
To node
    Keeps only a selected node a the nodes depending upon it. The node
    parameter is the name of the node to start from.
    
Connect
    Keeps only a selected node all the nodes connected to it. The node
    parameter is the name of the node to start from.


Examples
--------

The simplest form:

	<grand output="build.dot"/>

Create a build.dot graph file using the current build file. Add a buildfile
attribute to process another file:

	<grand output="build.dot" buildfile="ant-build.xml"/>

A two filters example:

	<grand output="build.dot" buildfile="ant-build.xml">
	    <filter name="fromnode" node="dist"/>
	    <filter name="tonode" node="prepare"/>
	</grand>



Known issues
------------

Subant dependencies on tasks not yet processed before invoking the grand task.
For instance in the following pseudo-build.xml:

    <target name="subant-task-1">
        <subant .../>
    </target>

    <target name="subant-task-2">
        <subant .../>
    </target>

	<target name="dograph">
		<grand .../>
	</target>

Running "ant dograph" will get all the subant dependencies, while running
"ant subant-task-1 dograph" will only get dependencies from "subant-task-2"
and running "ant subant-task-1 subant-task-2 dograph" won't get any subant
dependencies at all.


More information
----------------

The grand web site (http://www.ggtools.net/grand) contains the full
documentation for Grand with some illustrated examples and the last version.

You can contact me at grand@ggtools.net.


Licensing
---------
 
This software is licensed under the terms you may find in the file
named "LICENSE" in this directory.
