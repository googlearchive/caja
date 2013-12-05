#!/bin/bash
# Copyright 2012 Google Inc. All Rights Reserved.
#
# Tool for Google Cloud Endpoints.

[ -z "${DEBUG}" ] || set -x  # trace if $DEBUG env. var. is non-zero
SDK_BIN="`dirname "$0" | sed -e "s#^\\([^/]\\)#${PWD}/\\1#"`" # sed makes absolute
SDK_LIB="$SDK_BIN/../lib"

JAR_FILE1="$SDK_LIB/opt/tools/appengine-local-endpoints/v1/appengine-local-endpoints.jar"
if [ ! -e "$JAR_FILE1" ]; then
  echo "$JAR_FILE1 not found"
  exit 1
fi

JAR_FILE2="$SDK_LIB/opt/user/appengine-endpoints/v1/appengine-endpoints.jar"
if [ ! -e "$JAR_FILE2" ]; then
  echo "$JAR_FILE2 not found"
  exit 1
fi

CLASSPATH="$JAR_FILE1:$JAR_FILE2:$SDK_LIB/shared/servlet-api.jar:$SDK_LIB/appengine-tools-api.jar:$SDK_LIB/opt/user/datanucleus/v1/jdo2-api-2.3-eb.jar"
for jar in "$SDK_LIB"/user/*.jar; do
  CLASSPATH="$CLASSPATH:$jar"
done

java -cp "$CLASSPATH" com.google.api.server.spi.tools.EndpointsTool "$@"
