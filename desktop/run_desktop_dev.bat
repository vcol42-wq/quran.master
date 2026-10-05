@echo off
chcp 65001 > nul
setlocal
cd /d "%~dp0"
set "APP_EXE=%~dp0dist_store\win-unpacked\سبح بخشوع.exe"
set "ELECTRON_EXE=%~dp0node_modules\electron\dist\electron.exe"

if exist "%APP_EXE%" (
    echo جاري تشغيل تطبيق سبح بخشوع لسطح المكتب...
    start "" "%APP_EXE%"
    exit /b 0
)

if exist "%ELECTRON_EXE%" (
    start "" "%ELECTRON_EXE%" "%~dp0."
    exit /b 0
)

echo [خطأ] لم يتم العثور على مشغل التطبيق.
pause
exit /b 1
