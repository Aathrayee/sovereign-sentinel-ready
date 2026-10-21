@echo off
cd /d "%~dp0"
echo Starting Sovereign Sentinel...
echo Open http://localhost:8080 after the server starts.
call mvnw.cmd spring-boot:run
if errorlevel 1 (
  echo.
  echo Startup failed. Check that Java 21 is installed, and that port 8080 is free.
)
pause
