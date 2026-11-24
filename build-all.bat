@echo off
rem Builds Pearl X for every supported Minecraft version.
rem Requires JDK 25 (Fabric Loom 1.19.4 needs it); 1.21.x jars are still compiled with --release 21.
setlocal
cd /d "%~dp0"

java -version >nul 2>&1
if errorlevel 1 (
    echo Java not found. Install JDK 25 and set JAVA_HOME.
    exit /b 1
)

call gradlew.bat buildAll %*
if errorlevel 1 exit /b 1

echo.
echo Jars written to build\dist:
dir /b build\dist
endlocal
