@echo off
@rem Copyright 2015 the original author or authors.
@rem Licensed under the Apache License, Version 2.0.
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

if defined JAVA_HOME (
    set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
    set JAVA_EXE=java.exe
)

if not exist "%JAVA_EXE%" (
    echo ERROR: JAVA_HOME is not set and no java command could be found. 1>&2
    exit /b 1
)

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
if not exist "%CLASSPATH%" (
    echo Gradle Wrapper JAR is not present yet. 1>&2
    echo On GitHub Actions, the workflow generates it before building. 1>&2
    echo To generate it locally, install Gradle 8.7 and run: gradle wrapper --gradle-version 8.7 1>&2
    exit /b 1
)

"%JAVA_EXE%" %JAVA_OPTS% %GRADLE_OPTS% ^
  -Dorg.gradle.appname=%APP_BASE_NAME% ^
  -classpath "%CLASSPATH%" ^
  org.gradle.wrapper.GradleWrapperMain %*
endlocal
