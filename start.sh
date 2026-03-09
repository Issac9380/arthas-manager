#!/usr/bin/env bash
# =============================================================================
# arthas-manager 一键启动脚本 (Linux / macOS)
# 用法: bash start.sh
# =============================================================================
set -euo pipefail
export LANG=zh_CN.UTF-8
export LC_ALL=zh_CN.UTF-8

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend"
FRONTEND_DIR="$SCRIPT_DIR/frontend"
LOG_DIR="$SCRIPT_DIR/logs"

mkdir -p "$LOG_DIR"

# ── 颜色输出 ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; }

# ── 依赖检查 ──────────────────────────────────────────────────────────────────
check_cmd() {
    if ! command -v "$1" &>/dev/null; then
        error "未找到命令: $1，请先安装后重试"
        exit 1
    fi
}
check_cmd java
check_cmd mvn
check_cmd node
check_cmd npm

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VER" -lt 17 ] 2>/dev/null; then
    error "需要 Java 17+，当前版本: $JAVA_VER"
    exit 1
fi

# ── 清理旧进程 ────────────────────────────────────────────────────────────────
cleanup() {
    info "正在停止所有服务..."
    [ -n "${BACKEND_PID:-}" ]  && kill "$BACKEND_PID"  2>/dev/null && info "后端已停止 (PID $BACKEND_PID)"
    [ -n "${FRONTEND_PID:-}" ] && kill "$FRONTEND_PID" 2>/dev/null && info "前端已停止 (PID $FRONTEND_PID)"
    exit 0
}
trap cleanup SIGINT SIGTERM

# ── 构建后端 ──────────────────────────────────────────────────────────────────
info ">>> 构建后端 (Maven)..."
cd "$BACKEND_DIR"
mvn package -DskipTests -q \
    -Dfile.encoding=UTF-8 \
    -Dmaven.compiler.encoding=UTF-8 \
    2>&1 | tee "$LOG_DIR/backend-build.log"
success "后端构建完成"

JAR=$(ls "$BACKEND_DIR"/target/arthas-manager-backend-*.jar 2>/dev/null | grep -v 'original' | head -1)
if [ -z "$JAR" ]; then
    error "未找到可执行 JAR，构建可能失败，请查看 logs/backend-build.log"
    exit 1
fi

# ── 启动后端 ──────────────────────────────────────────────────────────────────
info ">>> 启动后端: $JAR"
java \
    -Dfile.encoding=UTF-8 \
    -Dstdout.encoding=UTF-8 \
    -jar "$JAR" \
    > "$LOG_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
success "后端已启动 (PID $BACKEND_PID)，日志: logs/backend.log"

# 等待后端就绪
info "等待后端启动 (最多 60 秒)..."
for i in $(seq 1 60); do
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null | grep -q '200\|404'; then
        success "后端已就绪 (${i}s)"
        break
    fi
    # 简单检测进程是否在运行
    if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
        error "后端进程意外退出，请查看 logs/backend.log"
        exit 1
    fi
    sleep 1
done

# ── 安装前端依赖 ──────────────────────────────────────────────────────────────
info ">>> 安装前端依赖 (npm install)..."
cd "$FRONTEND_DIR"
npm install --prefer-offline 2>&1 | tee "$LOG_DIR/frontend-install.log"
success "前端依赖安装完成"

# ── 启动前端 ──────────────────────────────────────────────────────────────────
info ">>> 启动前端开发服务器..."
npm run dev \
    > "$LOG_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
success "前端已启动 (PID $FRONTEND_PID)，日志: logs/frontend.log"

# ── 显示访问地址 ──────────────────────────────────────────────────────────────
sleep 2
echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  arthas-manager 启动成功！${NC}"
echo -e "${GREEN}  后端: http://localhost:8080${NC}"
echo -e "${GREEN}  前端: http://localhost:3000${NC}"
echo -e "${GREEN}  按 Ctrl+C 停止所有服务${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""

# 保持脚本运行，等待子进程
wait "$BACKEND_PID" "$FRONTEND_PID"
