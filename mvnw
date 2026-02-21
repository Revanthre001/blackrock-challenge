#!/bin/sh
# Maven wrapper script for Linux/Mac
# This script is required for Docker multi-stage build

set -e

# Locate Java
if [ -n "$JAVA_HOME" ]; then
  _java="$JAVA_HOME/bin/java"
elif type -p java > /dev/null; then
  _java=java
else
  echo "Java not found. Set JAVA_HOME or add Java to PATH."
  exit 1
fi

MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES=".mvn/wrapper/maven-wrapper.properties"

if [ -f "$MAVEN_WRAPPER_JAR" ]; then
  exec "$_java" -jar "$MAVEN_WRAPPER_JAR" "$@"
else
  # Fallback: download Maven wrapper
  if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    DISTRIBUTION_URL=$(grep "distributionUrl" "$MAVEN_WRAPPER_PROPERTIES" | sed "s/.*=//")
    echo "Downloading Maven from $DISTRIBUTION_URL"
  fi
  exec mvn "$@"
fi
