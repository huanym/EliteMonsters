@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup script for Windows
@REM ----------------------------------------------------------------------------

@if "%DEBUG%"=="" @echo off
@setlocal

set MAVEN_PROJECTBASEDIR=%CD%
if not "%MAVEN_PROJECTBASEDIR%"=="" goto homeDetected
echo MAVEN_PROJECTBASEDIR not set >&2
exit /b 1
:homeDetected

set MAVEN_USER_HOME=%USERPROFILE%\.m2
set WRAPPER_DIR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper
set WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar
set WRAPPER_PROP=%WRAPPER_DIR%\maven-wrapper.properties

if exist "%WRAPPER_JAR%" goto wrapperExists
echo Downloading Maven Wrapper...
mkdir "%WRAPPER_DIR%" 2>nul
powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar' -OutFile '%WRAPPER_JAR%'"
if not exist "%WRAPPER_JAR%" (
    echo Failed to download Maven Wrapper. Please install Maven manually: https://maven.apache.org
    exit /b 1
)
:wrapperExists

set MAVEN_DISTRO=%MAVEN_USER_HOME%\wrapper\dists

if not exist "%MAVEN_DISTRO%" mkdir "%MAVEN_DISTRO%"

for /f "tokens=2 delims==" %%a in ('findstr "distributionUrl" "%WRAPPER_PROP%"') do set DIST_URL=%%a
set DIST_URL=%DIST_URL: =%

for %%a in ("%DIST_URL%") do set DIST_NAME=%%~na
set DIST_NAME=%DIST_NAME:-bin=%
set MAVEN_HOME=%MAVEN_DISTRO%\%DIST_NAME%

if exist "%MAVEN_HOME%\bin\mvn.cmd" goto runMaven
echo Downloading Maven %DIST_NAME%...
powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%TEMP%\maven-dist.zip'"
echo Extracting Maven...
powershell -Command "Expand-Archive -Path '%TEMP%\maven-dist.zip' -DestinationPath '%MAVEN_DISTRO%' -Force"
del "%TEMP%\maven-dist.zip" 2>nul
if not exist "%MAVEN_HOME%" (
    echo Failed to setup Maven. Please install Maven manually: https://maven.apache.org
    exit /b 1
)
:runMaven
set "M2_HOME=%MAVEN_HOME%"
set "PATH=%MAVEN_HOME%\bin;%PATH%"
"%MAVEN_HOME%\bin\mvn.cmd" %*
