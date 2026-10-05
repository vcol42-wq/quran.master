@echo off
chcp 65001 > nul
setlocal
set "APP_EXE=D:\quran.master\desktop\dist_store\win-unpacked\سبح بخشوع.exe"

if not exist "%APP_EXE%" (
    echo [خطأ] لم يتم العثور على مشغل التطبيق في: %APP_EXE%
    pause
    exit /b 1
)

echo جاري تشغيل تطبيق "سبح بخشوع" لسطح المكتب...
start "" "%APP_EXE%"
endlocal
exit /b 0
