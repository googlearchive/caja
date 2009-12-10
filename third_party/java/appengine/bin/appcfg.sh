#!/bin/bash
# Launches AppCfg
[ -z "${DEBUG}" ] || set -x  # trace if $DEBUG env. var. is non-zero
SDK_BIN=`dirname $0 | sed -e "s#^\\([^/]\\)#${PWD}/\\1#"` # sed makes absolute
SDK_LIB=$SDK_BIN/../lib
SDK_CONFIG=$SDK_BIN/../config/sdk

java -cp "$SDK_LIB/appengine-tools-api.jar" \
    com.google.appengine.tools.admin.AppCfg $*
