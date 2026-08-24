@echo off
REM Batch files are not subject to the PowerShell execution policy, so this
REM wrapper runs the installer on a stock Windows box with no Set-ExecutionPolicy
REM and no admin rights. Double-click it, or run it from a terminal with the
REM same arguments the .ps1 takes (-ListDirs, -ModsDir "...", -Force, -Version).
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0install-oldanimations.ps1" %*
set EXITCODE=%ERRORLEVEL%
echo.
pause
exit /b %EXITCODE%
