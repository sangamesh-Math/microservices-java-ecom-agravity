@echo off
@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Batch Script
@REM ----------------------------------------------------------------------------

setlocal

set "DIRNAME=%~dp0"
if "%DIRNAME%" == "" set "DIRNAME=."

powershell -NoProfile -ExecutionPolicy Bypass -File "%DIRNAME%mvnw.ps1" %*
exit /b %ERRORLEVEL%
