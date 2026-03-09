@echo off
:: =============================================================================
:: arthas-manager 一键启动脚本 (Windows)
:: 用法: 双击运行，或在 CMD 中执行 start.bat
:: =============================================================================
chcp 65001 >nul
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "BACKEND_DIR=%SCRIPT_DIR%backend"
set "FRONTEND_DIR=%SCRIPT_DIR%frontend"
set "LOG_DIR=%SCRIPT_DIR%logs"

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo ============================================
echo   arthas-manager 启动脚本 (Windows)
echo ============================================
echo.

:: ── 依赖检查 ──────────────────────────────────────────────────────────────────
where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 java 命令，请安装 JDK 17+ 并配置 PATH
    pause
    exit /b 1
)

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 mvn 命令，请安装 Maven 并配置 PATH
    pause
    exit /b 1
)

where node >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 node 命令，请安装 Node.js 并配置 PATH
    pause
    exit /b 1
)

where npm >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 npm 命令，请安装 Node.js 并配置 PATH
    pause
    exit /b 1
)

echo [OK]    Java / Maven / Node.js 检查通过
echo.

:: ── 构建后端 ──────────────────────────────────────────────────────────────────
echo [INFO]  >>> 构建后端 (Maven)...
cd /d "%BACKEND_DIR%"

call mvn package -DskipTests -q ^
    -Dfile.encoding=UTF-8 ^
    -Dmaven.compiler.encoding=UTF-8 ^
    > "%LOG_DIR%\backend-build.log" 2>&1

if errorlevel 1 (
    echo [ERROR] 后端构建失败，请查看 logs\backend-build.log
    pause
    exit /b 1
)
echo [OK]    后端构建完成
echo.

:: 查找 JAR（排除 original-* 文件）
set "JAR="
for %%f in ("%BACKEND_DIR%\target\arthas-manager-backend-*.jar") do (
    echo %%f | findstr /i "original" >nul || set "JAR=%%f"
)

if not defined JAR (
    echo [ERROR] 未找到可执行 JAR，请查看 logs\backend-build.log
    pause
    exit /b 1
)

:: ── 启动后端（新窗口）────────────────────────────────────────────────────────
echo [INFO]  >>> 启动后端: %JAR%
start "arthas-manager [后端]" cmd /c ^
    "chcp 65001 >nul && java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar "%JAR%" > "%LOG_DIR%\backend.log" 2>&1"
echo [OK]    后端已在新窗口启动，日志: logs\backend.log
echo.

:: 等待后端启动（最多 60 秒）
echo [INFO]  等待后端启动 (最多 60 秒)...
set /a COUNT=0
:WAIT_BACKEND
timeout /t 1 /nobreak >nul
set /a COUNT+=1
curl -s -o nul -w "%%{http_code}" http://localhost:8080/actuator/health 2>nul | findstr /c:"200" /c:"404" >nul 2>&1
if not errorlevel 1 (
    echo [OK]    后端已就绪 (!COUNT!s)
    goto BACKEND_READY
)
if !COUNT! geq 60 (
    echo [WARN]  后端启动超时，继续启动前端（后端可能仍在加载中）
    goto BACKEND_READY
)
goto WAIT_BACKEND
:BACKEND_READY
echo.

:: ── 安装前端依赖 ──────────────────────────────────────────────────────────────
echo [INFO]  >>> 安装前端依赖 (npm install)...
cd /d "%FRONTEND_DIR%"
call npm install --prefer-offline > "%LOG_DIR%\frontend-install.log" 2>&1
if errorlevel 1 (
    echo [ERROR] npm install 失败，请查看 logs\frontend-install.log
    pause
    exit /b 1
)
echo [OK]    前端依赖安装完成
echo.

:: ── 启动前端（新窗口）────────────────────────────────────────────────────────
echo [INFO]  >>> 启动前端开发服务器...
start "arthas-manager [前端]" cmd /c ^
    "chcp 65001 >nul && cd /d "%FRONTEND_DIR%" && npm run dev > "%LOG_DIR%\frontend.log" 2>&1"
echo [OK]    前端已在新窗口启动，日志: logs\frontend.log
echo.

:: ── 完成提示 ──────────────────────────────────────────────────────────────────
echo ============================================
echo   arthas-manager 启动成功！
echo   后端: http://localhost:8080
echo   前端: http://localhost:3000
echo   关闭对应窗口可停止相应服务
echo ============================================
echo.
pause
endlocal
