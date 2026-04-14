#!/usr/bin/env python3
from __future__ import annotations

import argparse
import os
import platform
import shutil
import subprocess
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent
FRONTEND_DIR = PROJECT_ROOT / "frontend"
START_SCRIPT = PROJECT_ROOT / "start_app.py"


def run(cmd: list[str], cwd: Path | None = None, check: bool = True) -> int:
    print(f"[cmd] {' '.join(cmd)}")
    completed = subprocess.run(cmd, cwd=str(cwd) if cwd else None)
    if check and completed.returncode != 0:
        raise RuntimeError(f"Command failed ({completed.returncode}): {' '.join(cmd)}")
    return completed.returncode


def command_exists(name: str) -> bool:
    return shutil.which(name) is not None


def detect_linux_package_manager() -> str | None:
    for pm in ("apt-get", "dnf", "pacman"):
        if command_exists(pm):
            return pm
    return None


def install_dependency_windows(dep: str) -> None:
    winget_map = {
        "java": ["winget", "install", "--id", "EclipseAdoptium.Temurin.17.JDK", "--accept-source-agreements", "--accept-package-agreements"],
        "mvn": ["winget", "install", "--id", "Apache.Maven", "--accept-source-agreements", "--accept-package-agreements"],
        "node": ["winget", "install", "--id", "OpenJS.NodeJS.LTS", "--accept-source-agreements", "--accept-package-agreements"],
    }
    cmd = winget_map.get(dep)
    if not cmd:
        return
    run(cmd)


def install_dependency_macos(dep: str) -> None:
    if not command_exists("brew"):
        raise RuntimeError("Homebrew nao encontrado. Instale em https://brew.sh e rode novamente.")

    brew_map = {
        "java": ["brew", "install", "--cask", "temurin@17"],
        "mvn": ["brew", "install", "maven"],
        "node": ["brew", "install", "node"],
    }
    cmd = brew_map.get(dep)
    if not cmd:
        return
    run(cmd)


def install_dependency_linux(dep: str) -> None:
    pm = detect_linux_package_manager()
    if pm is None:
        raise RuntimeError("Nenhum gerenciador suportado encontrado (apt-get/dnf/pacman).")

    if pm == "apt-get":
        pkg_map = {"java": "openjdk-17-jdk", "mvn": "maven", "node": "nodejs npm"}
        run(["sudo", "apt-get", "update"])
        run(["sudo", "apt-get", "install", "-y", *pkg_map[dep].split()])
        return

    if pm == "dnf":
        pkg_map = {"java": "java-17-openjdk-devel", "mvn": "maven", "node": "nodejs npm"}
        run(["sudo", "dnf", "install", "-y", *pkg_map[dep].split()])
        return

    if pm == "pacman":
        pkg_map = {"java": "jdk17-openjdk", "mvn": "maven", "node": "nodejs npm"}
        run(["sudo", "pacman", "-Sy", "--noconfirm", *pkg_map[dep].split()])


def ensure_dependencies(auto_install: bool) -> None:
    required = ["java", "mvn", "node", "npm"]
    missing = [cmd for cmd in required if not command_exists(cmd)]

    if not missing:
        print("[ok] Dependencias encontradas: java, mvn, node, npm")
        return

    print(f"[warn] Dependencias ausentes: {', '.join(missing)}")
    if not auto_install:
        raise RuntimeError("Dependencias ausentes. Rode com --auto-install para tentativa de instalacao automatica.")

    system = platform.system().lower()
    install_targets = [dep for dep in ("java", "mvn", "node") if dep in missing or (dep == "node" and "npm" in missing)]

    for dep in install_targets:
        print(f"[setup] Instalando {dep}...")
        if system == "windows":
            install_dependency_windows(dep)
        elif system == "darwin":
            install_dependency_macos(dep)
        else:
            install_dependency_linux(dep)

    missing_after = [cmd for cmd in required if not command_exists(cmd)]
    if missing_after:
        raise RuntimeError(f"Ainda faltam dependencias: {', '.join(missing_after)}")


def create_start_wrappers() -> None:
    bat = PROJECT_ROOT / "start-app.bat"
    bat.write_text(
        "@echo off\r\n"
        "cd /d %~dp0\r\n"
        "py -3 start_app.py\r\n",
        encoding="utf-8",
    )

    sh = PROJECT_ROOT / "start-app.sh"
    sh.write_text(
        "#!/usr/bin/env bash\n"
        "set -euo pipefail\n"
        "cd \"$(dirname \"$0\")\"\n"
        "python3 start_app.py\n",
        encoding="utf-8",
    )
    try:
        os.chmod(sh, 0o755)
    except OSError:
        pass


def create_shortcut() -> None:
    system = platform.system().lower()
    home = Path.home()

    if system == "windows":
        desktop = home / "Desktop"
        lnk = desktop / "Game Dashboard.lnk"
        ps = (
            "$W = New-Object -ComObject WScript.Shell;"
            f"$S = $W.CreateShortcut('{str(lnk)}');"
            "$S.TargetPath = 'powershell.exe';"
            f"$S.Arguments = '-ExecutionPolicy Bypass -File \"{str(START_SCRIPT)}\"';"
            f"$S.WorkingDirectory = '{str(PROJECT_ROOT)}';"
            "$S.IconLocation = 'shell32.dll,44';"
            "$S.Save();"
        )
        run(["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", ps])
        print(f"[ok] Atalho criado: {lnk}")
        return

    if system == "darwin":
        desktop = home / "Desktop"
        cmd_file = desktop / "Game Dashboard.command"
        cmd_file.write_text(
            "#!/bin/bash\n"
            f"cd '{str(PROJECT_ROOT)}'\n"
            "python3 start_app.py\n",
            encoding="utf-8",
        )
        try:
            os.chmod(cmd_file, 0o755)
        except OSError:
            pass
        print(f"[ok] Launcher criado: {cmd_file}")
        return

    desktop = home / "Desktop"
    desktop.mkdir(parents=True, exist_ok=True)
    desktop_file = desktop / "Game Dashboard.desktop"
    desktop_file.write_text(
        "[Desktop Entry]\n"
        "Type=Application\n"
        "Name=Game Dashboard\n"
        f"Exec=bash -lc 'cd \"{str(PROJECT_ROOT)}\" && ./start-app.sh'\n"
        f"Path={str(PROJECT_ROOT)}\n"
        "Terminal=true\n"
        "Categories=Development;\n",
        encoding="utf-8",
    )
    try:
        os.chmod(desktop_file, 0o755)
    except OSError:
        pass
    print(f"[ok] Launcher criado: {desktop_file}")


def bootstrap_project(skip_tests: bool) -> None:
    print("[1/4] Instalando dependencias do frontend...")
    run(["npm", "install"], cwd=FRONTEND_DIR)

    print("[2/4] Validando backend Java...")
    if skip_tests:
        print("[skip] Testes backend ignorados por --skip-tests")
    else:
        run(["mvn", "-q", "test"], cwd=PROJECT_ROOT)

    print("[3/4] Criando launchers e atalhos...")
    create_start_wrappers()
    create_shortcut()

    print("[4/4] Finalizado.")


def maybe_run_now(run_now: bool) -> None:
    if not run_now:
        return
    print("[run] Iniciando app local agora...")
    run([sys.executable, str(START_SCRIPT)], cwd=PROJECT_ROOT)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Instalador unificado do Game Dashboard")
    parser.add_argument("--auto-install", action="store_true", help="Tenta instalar dependencias automaticamente pelo gerenciador do sistema")
    parser.add_argument("--skip-tests", action="store_true", help="Pula validacao de testes backend")
    parser.add_argument("--run-now", action="store_true", help="Inicia backend e frontend ao final")
    return parser.parse_args()


def main() -> int:
    try:
        args = parse_args()
        print(f"Sistema detectado: {platform.system()}")
        ensure_dependencies(auto_install=args.auto_install)
        bootstrap_project(skip_tests=args.skip_tests)
        maybe_run_now(run_now=args.run_now)
        print("Pronto. Use o atalho criado na area de trabalho para abrir o app.")
        return 0
    except Exception as exc:  # noqa: BLE001
        print(f"[erro] {exc}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
