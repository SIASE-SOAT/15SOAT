@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0") ELSE (SET "BASE_DIR=%__MVNW_ARG0_NAME__%")

@SET MAVEN_PROJECTBASEDIR=%BASE_DIR%
@SET MAVEN_HOME=
@SET JAVA_HOME=%JAVA_HOME%

@IF "%JAVA_HOME%"=="" (
  FOR /F "usebackq tokens=*" %%a IN (`where java 2^>nul`) DO (
    IF NOT "%%a"=="" (
      SET "JAVA_HOME=%%~dpa.."
    )
  )
)

@SET MVNW_VERBOSE=false
@SET MVNW_USERNAME=
@SET MVNW_PASSWORD=
@SET MVNW_REPOURL=

@SET WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
@SET WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

@IF EXIST %WRAPPER_JAR% (
  SET DOWNLOAD_URL=
) ELSE (
  SET DOWNLOAD_URL=%WRAPPER_URL%
)

@IF "%DOWNLOAD_URL%"=="" GOTO execute

@ECHO Downloading Maven Wrapper from: %DOWNLOAD_URL%
@"%JAVA_HOME%\bin\java.exe" -classpath "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper" MavenWrapperDownloader "%DOWNLOAD_URL%" "%WRAPPER_JAR%" || (
  @"%JAVA_HOME%\bin\java.exe" -jar "%WRAPPER_JAR%" --download-wrapper 2>nul || (
    CALL :downloadMaven "%DOWNLOAD_URL%" "%WRAPPER_JAR%"
  )
)

:execute
@IF NOT "%JAVA_HOME%"=="" SET JAVA_HOME=%JAVA_HOME:"=%
@SET JAVA_EXE="%JAVA_HOME%\bin\java.exe"

@IF NOT EXIST %JAVA_EXE% (
  @ECHO ERROR: JAVA_HOME is not set or is pointing to a wrong Java installation.
  EXIT /B 1
)

%JAVA_EXE% -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %MAVEN_CONFIG% %*
IF ERRORLEVEL 1 EXIT /B 1

:downloadMaven
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('%~1', '%~2')}"
EXIT /B 0
