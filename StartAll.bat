@echo off
title Start Soul-Trail Server and Mystic Client
echo ============================================
echo Starting Soul-Trail Proxy Server...
echo ============================================
cd "Biohazard v3 server client cache\Proxy Server"
start cmd /k CompileAndRun.bat
cd ..\..\..
echo.
echo ============================================
echo Starting Mystic Client...
echo ============================================
cd "C:\Users\llrbi\Documents\GitHub\rsPROJECTps\mystic-updatedclient"
start cmd /k CompileAndRun.bat
echo.
echo ============================================
echo Both Server and Client started in separate windows!
echo ============================================
pause