@echo off
echo Compiling ShopEditor...
javac ShopEditor.java
if %errorlevel% == 0 (
    echo Compilation successful!
) else (
    echo Compilation failed!
)
pause
