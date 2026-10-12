#!/bin/sh
# Build and run the generator without Gradle/Maven.
# Usage: ./gen.sh [--db=oracle|postgres|mysql] [--package=...] [--out=...] TABLE...
# Put the JDBC driver jar(s) in lib/.
set -e
cd "$(dirname "$0")"

CP="freemarker-2.3.34.jar:lib/*"
rm -rf out
javac -encoding UTF-8 -cp "$CP" -d out src/main/java/com/mitsuru/codegen/*.java
java -cp "out:src/main/resources:$CP" com.mitsuru.codegen.CodegenApplication "$@"
