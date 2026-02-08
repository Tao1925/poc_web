#!/usr/bin/env bash
set -euo pipefail

JAR_PATH="${1:-target/poc_web-0.0.1-SNAPSHOT.jar}"

DEFAULT_JVM_ARGS="-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=20 -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./data/heapdump.hprof"
JVM_ARGS="${APP_JVM_ARGS:-$DEFAULT_JVM_ARGS}"

exec java $JVM_ARGS -jar "$JAR_PATH"
