#!/bin/sh
#
# Copyright 2011 Google Inc. All Rights Reserved.
#
# Google Cloud SQL command line tool.
# Examples:
#   $0 instance [database]

SQL_SH_DIR="$(cd $(dirname $0); pwd)"
JAR="${SQL_SH_DIR}/../lib/impl/google_sql.jar"
JAVA="${JAVA_HOME}/bin/java"

die() {
  echo $1
  exit 2
}

main() {
  if [ ! -x "${JAVA}" ]; then
    JAVA=$(command -v "java")
  fi
  [ -x "${JAVA}" ] || die "Unable to find JVM. Please set JAVA_HOME"
  ${JAVA} -jar "${JAR}" "$@"
  exit
}

main "$@"
