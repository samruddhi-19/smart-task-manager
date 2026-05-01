@echo off
echo.
echo ==================================
echo   Smart Task Manager - Starting
echo ==================================
echo.

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java not found. Install Java 17+ from https://adoptium.net/
    pause
    exit /b 1
)

where javac >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: javac not found. Install a JDK from https://adoptium.net/
    pause
    exit /b 1
)

echo Compiling...
if not exist out mkdir out
javac -d out src\com\tecnomate\*.java

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Compilation failed. See errors above.
    pause
    exit /b 1
)

echo Compilation successful!
echo.

if "%ANTHROPIC_API_KEY%"=="" (
    echo WARNING: ANTHROPIC_API_KEY not set. Priority defaults to 'medium'.
    echo To enable AI: set ANTHROPIC_API_KEY=sk-ant-your-key-here
) else (
    echo Anthropic API key detected - AI priority suggestions enabled!
)

echo.
echo Server starting on http://localhost:8080
echo Press Ctrl+C to stop.
echo ==================================
echo.

java -cp out com.tecnomate.TaskManagerApp
pause
