@REM Maven wrapper script for Windows
@REM Uses Maven 3.9.6 from the local wrapper cache
@ECHO OFF
SETLOCAL

@REM Set JAVA_HOME to JDK 17 if not already set
IF "%JAVA_HOME%"=="" (
    SET "JAVA_HOME=C:\Users\revan\AppData\Local\Programs\Microsoft\jdk-17.0.13.11-hotspot"
)

@REM Maven location — uses the downloaded distribution
SET "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6"
SET "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

@REM Check Maven exists; if not, download it
IF NOT EXIST "%MAVEN_HOME%\bin\mvn.cmd" (
    ECHO Downloading Maven 3.9.6...
    MKDIR "%MAVEN_HOME%" 2>NUL
    PowerShell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip' -OutFile '%TEMP%\maven396.zip'; Expand-Archive '%TEMP%\maven396.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists\' -Force"
    IF EXIST "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6\bin\mvn.cmd" (
        ECHO Maven downloaded successfully.
    ) ELSE (
        ECHO Failed to download Maven. Please install Maven manually.
        EXIT /B 1
    )
)

@REM Run Maven with all passed arguments
"%MAVEN_HOME%\bin\mvn.cmd" %*
