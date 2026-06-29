#!/usr/bin/env python3
"""
BBS 离线部署包一键构建脚本（Windows / Linux 通用）
在开发机（有网络）上执行，将所有产物打包为 tar.gz

依赖:
  - Maven (mvn.cmd / mvn)
  - Node.js 18+ (npm)
  - Podman 或 Docker (podman / docker)
  - Python 3.8+ (tarfile, subprocess, shutil, pathlib)
"""

import argparse
import os
import shutil
import subprocess
import sys
import tarfile
from pathlib import Path

# ── 配置 ──────────────────────────────────────────────
SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_DIR = SCRIPT_DIR.parent
OUTPUT_DIR = PROJECT_DIR / "daqian-bbs-offline"
OUTPUT_TAR = PROJECT_DIR / "daqian-bbs-offline.tar.gz"

BBS_SERVER_IMAGE = "daqian-bbs-server:offline"
BBS_NGINX_IMAGE = "daqian-bbs-nginx:offline"
POSTGRES_IMAGE = "docker.io/postgres:12"

CONTAINER_TOOL = "podman"  # 可通过 --docker 参数切换


# ── 工具函数 ──────────────────────────────────────────
def step(msg: str):
    print(f"\n{'='*50}")
    print(f"  {msg}")
    print(f"{'='*50}")


def run(cmd: list, cwd: Path, desc: str = ""):
    print(f"  -> {desc or ' '.join(cmd)}")
    result = subprocess.run(cmd, cwd=str(cwd), shell=(sys.platform == "win32"))
    if result.returncode != 0:
        print(f"  [错误] 命令失败: {' '.join(cmd)}", file=sys.stderr)
        sys.exit(result.returncode)


def check_tool(name: str, install_hint: str = ""):
    """检查命令是否存在"""
    cmd = ["where", name] if sys.platform == "win32" else ["which", name]
    try:
        subprocess.run(cmd, capture_output=True, check=True)
    except subprocess.CalledProcessError:
        print(f"  [错误] 未找到 {name}，请先安装。{install_hint}", file=sys.stderr)
        sys.exit(1)


# ── 主构建流程 ────────────────────────────────────────
def main():
    global CONTAINER_TOOL

    parser = argparse.ArgumentParser(description="BBS 离线部署包一键构建")
    parser.add_argument("--docker", action="store_true", help="使用 Docker 替代 Podman")
    parser.add_argument("--skip-build", action="store_true", help="跳过构建步骤，仅打包已有产物")
    args = parser.parse_args()

    if args.docker:
        CONTAINER_TOOL = "docker"

    print("=" * 60)
    print("  BBS 离线部署包打包工具")
    print(f"  项目路径: {PROJECT_DIR}")
    print(f"  容器工具: {CONTAINER_TOOL}")
    print("=" * 60)

    # ── 检查依赖 ──
    print("\n[检查] 环境依赖...")
    if not args.skip_build:
        check_tool("npm", "https://nodejs.org/")
        check_tool("mvn" if sys.platform != "win32" else "mvn.cmd",
                    "https://maven.apache.org/install.html")
    check_tool(CONTAINER_TOOL,
               "https://podman.io/docs/installation (或使用 --docker 切换为 Docker)")

    # ── Step 1: 清理 ──
    step("1/6  清理旧的输出目录")
    if OUTPUT_DIR.exists():
        shutil.rmtree(OUTPUT_DIR)
    OUTPUT_DIR.mkdir(parents=True)

    (OUTPUT_DIR / "images").mkdir()
    (OUTPUT_DIR / "podman").mkdir()
    (OUTPUT_DIR / "scripts").mkdir()

    # ── Step 2: 构建后端 JAR ──
    if not args.skip_build:
        step("2/6  构建后端 JAR")
        mvn = "mvn.cmd" if sys.platform == "win32" else "mvn"
        run([mvn, "clean", "package", "-DskipTests", "-B"],
            PROJECT_DIR / "bbs-server", "mvn clean package")
        shutil.copy(PROJECT_DIR / "bbs-server" / "target" / "bbs-server.jar",
                    OUTPUT_DIR)
        print("       ✓ bbs-server.jar")

    # ── Step 3: 构建前端 ──
    if not args.skip_build:
        step("3/6  构建前端 bbs-ui")
        npm = "npm.cmd" if sys.platform == "win32" else "npm"
        run([npm, "install", "--silent"], PROJECT_DIR / "bbs-ui", "npm install")
        run([npm, "run", "build"], PROJECT_DIR / "bbs-ui", "npm run build")
        print("       ✓ bbs-ui/dist")

        step("4/6  构建前端 bbs-admin-ui")
        run([npm, "install", "--silent"], PROJECT_DIR / "bbs-admin-ui", "npm install")
        run([npm, "run", "build"], PROJECT_DIR / "bbs-admin-ui", "npm run build")
        print("       ✓ bbs-admin-ui/dist")

    # ── Step 4: 构建容器镜像 ──
    if not args.skip_build:
        step("5/6  构建容器镜像")
        run([CONTAINER_TOOL, "build", "-t", BBS_SERVER_IMAGE,
             "-f", "bbs-server/Dockerfile", "."],
            PROJECT_DIR, "后端镜像")
        run([CONTAINER_TOOL, "build", "-t", BBS_NGINX_IMAGE,
             "-f", "nginx/Dockerfile", "."],
            PROJECT_DIR, "Nginx 镜像")

    # ── Step 5: 导出镜像 ──
    step("6/6  导出容器镜像")
    images = [
        (BBS_SERVER_IMAGE, "bbs-server.tar"),
        (BBS_NGINX_IMAGE, "bbs-nginx.tar"),
    ]

    # 检查 PostgreSQL 镜像
    pg_check = subprocess.run(
        [CONTAINER_TOOL, "image", "exists", POSTGRES_IMAGE],
        capture_output=True)
    if pg_check.returncode != 0:
        print(f"  -> 拉取 {POSTGRES_IMAGE}...")
        subprocess.run([CONTAINER_TOOL, "pull", POSTGRES_IMAGE],
                       cwd=str(PROJECT_DIR))
    images.append((POSTGRES_IMAGE, "postgres-12.tar"))

    for img_name, tar_name in images:
        tar_path = OUTPUT_DIR / "images" / tar_name
        run([CONTAINER_TOOL, "save", "-o", str(tar_path), img_name],
            PROJECT_DIR, f"导出 {tar_name}")

    # ── Step 6: 复制配置和脚本 ──
    print("\n[复制] 配置和脚本...")
    copy_files = [
        ("podman/create-pod.sh", "podman/"),
        ("podman/bbs-pod.service", "podman/"),
        ("podman/README.md", "podman/"),
        ("scripts/bbs-pg-schema.sql", "scripts/"),
        ("scripts/init-pg-database.sh", "scripts/"),
        ("scripts/migrate-mysql-to-pg.sh", "scripts/"),
        ("podman/部署说明.md", ""),
        ("podman/deploy.sh", ""),
    ]

    for rel_src, rel_dst in copy_files:
        src = PROJECT_DIR / rel_src
        dst = OUTPUT_DIR / rel_dst
        if src.exists():
            shutil.copy2(src, dst)
            print(f"       ✓ {rel_src}")

    # 设置可执行权限（Windows 上忽略）
    if sys.platform != "win32":
        for f in ["podman/create-pod.sh", "scripts/init-pg-database.sh",
                   "scripts/migrate-mysql-to-pg.sh"]:
            p = OUTPUT_DIR / f
            if p.exists():
                p.chmod(0o755)

    # ── Step 7: 打包 ──
    print("\n[打包] 压缩为 tar.gz...")
    with tarfile.open(str(OUTPUT_TAR), "w:gz") as tar:
        tar.add(str(OUTPUT_DIR), arcname=OUTPUT_DIR.name)

    # 计算大小
    size = OUTPUT_TAR.stat().st_size
    size_str = f"{size / 1024 / 1024:.1f} MB" if size > 1024 * 1024 else f"{size / 1024:.1f} KB"

    print("\n" + "=" * 60)
    print("  完成！离线部署包已生成")
    print("=" * 60)
    print(f"  打包文件: {OUTPUT_TAR}")
    print(f"  大小:     {size_str}")
    print()
    print("  RHEL 7 服务器上部署步骤：")
    print("  1. 将 daqian-bbs-offline.tar.gz 传到服务器")
    print("  2. tar -xzf daqian-bbs-offline.tar.gz")
    print("  3. cd daqian-bbs-offline")
    print("  4. for f in images/*.tar; do podman load -i \"$f\"; done")
    print("  5. ./deploy.sh --db-password r123456 --db-port 15432 --start-pg")
    print("=" * 60)


if __name__ == "__main__":
    main()
