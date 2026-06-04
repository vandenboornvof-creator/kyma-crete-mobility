#!/bin/sh
#
# Copyright © 2015-2021 the original authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# This script downloads the Gradle wrapper jar if not present, then executes gradle.

APP_NAME="CreteMobility"
APP_BASE_NAME="${0##*/}"
APP_HOME="$(cd "$(dirname "$0")" && pwd -P)"

GRADLE_OPTS="${GRADLE_OPTS:-""}"
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

cygwin=false
msys=false
darwin=false
nonstop=false
case "$(uname)" in
  CYGWIN*  ) cygwin=true  ;;
  Darwin*  ) darwin=true  ;;
  MSYS* | MINGW* ) msys=true ;;
  NONSTOP* ) nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ ! -e "$CLASSPATH" ]; then
    echo "Downloading Gradle wrapper JAR..."
    mkdir -p "$APP_HOME/gradle/wrapper"
    wget -q -O "$CLASSPATH" \
        "https://raw.githubusercontent.com/gradle/gradle/v8.6.0/gradle/wrapper/gradle-wrapper.jar" 2>/dev/null \
    || curl -sL -o "$CLASSPATH" \
        "https://raw.githubusercontent.com/gradle/gradle/v8.6.0/gradle/wrapper/gradle-wrapper.jar" 2>/dev/null \
    || die "Could not download Gradle wrapper JAR. Please download manually."
fi

exec java ${DEFAULT_JVM_OPTS} ${JAVA_OPTS} ${GRADLE_OPTS} \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
