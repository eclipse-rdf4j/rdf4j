@rem ***************************************************************************
@rem Copyright (c) 2026 Eclipse RDF4J contributors.
@rem
@rem All rights reserved. This program and the accompanying materials
@rem are made available under the terms of the Eclipse Distribution License v1.0
@rem which accompanies this distribution, and is available at
@rem http://www.eclipse.org/org/documents/edl-v10.php.
@rem
@rem SPDX-License-Identifier: BSD-3-Clause
@rem ***************************************************************************
@echo off

set "LIB_DIR=%~dp0\..\lib"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
	set "JAVA=%JAVA_HOME%\bin\java.exe"
) else (
	set "JAVA=java"
)

if "%~1"=="" (
	"%JAVA%" --enable-native-access=ALL-UNNAMED -cp "%LIB_DIR%\*" org.eclipse.rdf4j.tools.lmdb.bulk.LmdbBulkLoad --interactive
) else (
	"%JAVA%" --enable-native-access=ALL-UNNAMED -cp "%LIB_DIR%\*" org.eclipse.rdf4j.tools.lmdb.bulk.LmdbBulkLoad %*
)
