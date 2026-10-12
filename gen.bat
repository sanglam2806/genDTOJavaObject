@echo off
rem Build and run the generator without Gradle/Maven.
rem Usage: gen.bat [--db=oracle/postgres/mysql] [--package=...] [--out=...] TABLE...
rem Put the JDBC driver jar(s) in lib\
setlocal
cd /d "%~dp0"

set CP=freemarker-2.3.34.jar;lib\*
if exist out rmdir /s /q out
javac -encoding UTF-8 -cp "%CP%" -d out src\main\java\com\mitsuru\codegen\*.java
if errorlevel 1 exit /b 1
java -cp "out;src\main\resources;%CP%" com.mitsuru.codegen.CodegenApplication %*
