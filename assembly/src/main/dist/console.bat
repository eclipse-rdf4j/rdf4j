@rem ***************************************************************************
@rem Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
@rem All rights reserved. This program and the accompanying materials
@rem are made available under the terms of the Eclipse Distribution License v1.0
@rem which accompanies this distribution, and is available at
@rem http://www.eclipse.org/org/documents/edl-v10.php.
@rem ***************************************************************************
@echo off

rem Set the lib dir relative to the batch file's directory
set LIB_DIR=%~dp0\..\lib
rem echo LIB_DIR = %LIB_DIR%

rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway).
set CMD_LINE_ARGS=%1
if ""%1""=="""" goto setupArgsEnd
shift
:setupArgs
if ""%1""=="""" goto setupArgsEnd
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setupArgs

:setupArgsEnd

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
goto javaHome

:noJavaHome
set JAVA=java
goto javaHomeEnd

:javaHome
set JAVA=%JAVA_HOME%\bin\java

:javaHomeEnd

:checkJdk14
"%JAVA%" -version 2>&1 | findstr "1.4" >NUL
IF ERRORLEVEL 1 goto checkJdk15
echo Java 5 or newer required to run the console
goto end

:checkJdk15
"%JAVA%" -version 2>&1 | findstr "1.5" >NUL
IF ERRORLEVEL 1 goto java6
rem use java.ext.dirs hack
rem echo Using java.ext.dirs to set classpath
"%JAVA%" -Djava.ext.dirs="%LIB_DIR%" org.eclipse.rdf4j.console.Console %CMD_LINE_ARGS%
goto end

:java6
rem use java 6 wildcard feature
rem echo Using wildcard to set classpath
"%JAVA%" -cp "%LIB_DIR%\*" org.eclipse.rdf4j.console.Console %CMD_LINE_ARGS%
goto end

:end
