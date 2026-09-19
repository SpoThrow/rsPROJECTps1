@echo off
title Start Biohazard V3 Server and Mystic Client
echo ============================================
echo Starting Biohazard V3 Proxy Server...
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