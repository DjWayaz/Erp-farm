#!/usr/bin/env sh

##############################################################################
# Gradle start up script for UN*X
##############################################################################

PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

warn () { echo "$*"; }
die () { echo; echo "$*"; echo; exit 1; }

if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    [ ! -x "$JAVACMD" ] && die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
fi

WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "gradle-wrapper.jar not found. Attempting download..."
    mkdir -p "$APP_HOME/gradle/wrapper"
    WRAPPER_URL="https://github.com/gradle/gradle/raw/v8.6.0/gradle/wrapper/gradle-wrapper.jar"
    if command -v wget >/dev/null 2>&1; then
        wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR" 2>/dev/null || true
    elif command -v curl >/dev/null 2>&1; then
        curl -sL "$WRAPPER_URL" -o "$WRAPPER_JAR" 2>/dev/null || true
    fi
    [ ! -f "$WRAPPER_JAR" ] && die "ERROR: Could not obtain gradle-wrapper.jar. The GitHub Actions workflow will download it automatically."
fi

CLASSPATH=$WRAPPER_JAR

eval set -- $(
    printf '%s\n' "$DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS" |
    xargs -n1 |
    sed 's~[^-[:alnum:]+,.@/]~\\&~g' |
    tr '\n' ' '
) '"$@"'

exec "$JAVACMD" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
