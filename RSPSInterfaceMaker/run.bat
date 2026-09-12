@echo off
cd /d "%~dp0"

echo Checking for Java...

REM Try to auto-detect Java from common locations
if "%JAVA_HOME%"=="" (
    echo JAVA_HOME not set, attempting auto-detection...
    
    REM Check for extracted JDK in current directory
    for /d %%d in (jdk-*) do (
        if exist "%%d\bin\java.exe" (
            set "JAVA_HOME=%%~fd"
            echo Found Java at: %%~fd
            goto :found_java
        )
    )
    
    REM Check common Java installation paths
    if exist "C:\Program Files\Java\jdk-25\bin\java.exe" (
        set JAVA_HOME=C:\Program Files\Java\jdk-25
        echo Found Java at: C:\Program Files\Java\jdk-25
        goto :found_java
    ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe" (
        set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot
        echo Found Java at: C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot
        goto :found_java
    ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.1-hotspot\bin\java.exe" (
        set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.1-hotspot
        echo Found Java at: C:\Program Files\Eclipse Adoptium\jdk-25.0.4.1-hotspot
        goto :found_java
    ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-25\bin\java.exe" (
        set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25
        echo Found Java at: C:\Program Files\Eclipse Adoptium\jdk-25
        goto :found_java
    ) else if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
        set JAVA_HOME=C:\Program Files\Java\jdk-17
        echo Found Java at: C:\Program Files\Java\jdk-17
        goto :found_java
    ) else if exist "C:\Program Files\Java\jdk-11\bin\java.exe" (
        set JAVA_HOME=C:\Program Files\Java\jdk-11
        echo Found Java at: C:\Program Files\Java\jdk-11
        goto :found_java
    ) else if exist "C:\Users\%USERNAME%\Downloads\jdk-25.0.4.1-hotspot\bin\java.exe" (
        set JAVA_HOME=C:\Users\%USERNAME%\Downloads\jdk-25.0.4.1-hotspot
        echo Found Java at: C:\Users\%USERNAME%\Downloads\jdk-25.0.4.1-hotspot
        goto :found_java
    ) else if exist "%USERPROFILE%\Downloads\OpenJDK25U-jdk_x64_windows_hotspot_25.0.4.1_1\bin\java.exe" (
        set JAVA_HOME=%USERPROFILE%\Downloads\OpenJDK25U-jdk_x64_windows_hotspot_25.0.4.1_1
        echo Found Java at: %USERPROFILE%\Downloads\OpenJDK25U-jdk_x64_windows_hotspot_25.0.4.1_1
        goto :found_java
    )
    
    echo ERROR: Could not auto-detect Java installation.
    echo.
    echo Please extract your downloaded JDK and set JAVA_HOME to that folder.
    echo Example: set JAVA_HOME=C:\path\to\OpenJDK25U-jdk_x64_windows_hotspot_25.0.4.1_1
    echo.
    echo Or extract it to the RSPSInterfaceMaker folder and rename it to "jdk"
    pause
    exit /b 1
)

:found_java

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: JAVA_HOME is set but java.exe not found at: %JAVA_HOME%\bin\java.exe
    echo Please check your JAVA_HOME setting.
    echo Current JAVA_HOME: %JAVA_HOME%
    pause
    exit /b 1
)

echo Java found at: %JAVA_HOME%
echo.
echo Starting RSPS Interface Maker...
echo.

call mvnw.cmd javafx:run

if ERRORLEVEL 1 (
    echo.
    echo Error running the application. Check the error messages above.
    pause
    exit /b 1
)

pause
