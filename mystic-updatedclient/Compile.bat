@echo off
title Compile Mystic Client
echo Compiling Mystic Client...
if not exist bin mkdir bin
cd src
javac -d ../bin -cp "../lib/substance-8.0.02.jar;../lib/trident-1.5.00.jar" com/runescape/Configuration.java com/runescape/sign/SignLink.java
for /r %%f in (*.java) do javac -d ../bin -cp "../lib/substance-8.0.02.jar;../lib/trident-1.5.00.jar;../bin" "%%f"
cd ..
echo Compilation complete!
pause