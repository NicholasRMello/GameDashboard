#!/usr/bin/env python3
from __future__ import annotations

import os
import platform
import socket
import subprocess
import sys
import time
import webbrowser
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent
FRONTEND_DIR = PROJECT_ROOT / "frontend"
BACKEND_LOG = PROJECT_ROOT / "backend.log"
FRONTEND_LOG = PROJECT_ROOT / "frontend.log"


def is_port_open(port: int, host: str = "127.0.0.1") -> bool:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.settimeout(0.3)
        return sock.connect_ex((host, port)) == 0


def wait_port(port: int, timeout_seconds: int) -> bool:
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        if is_port_open(port):
            return True
        time.sleep(0.25)
    return False


def spawn_process(cmd: list[str], cwd: Path, log_file: Path) -> None:
    log_file.parent.mkdir(parents=True, exist_ok=True)
    log_handle = open(log_file, "a", encoding="utf-8")

    if platform.system().lower() == "windows":
        subprocess.Popen(
            cmd,
            cwd=str(cwd),
            stdout=log_handle,
            stderr=subprocess.STDOUT,
            creationflags=subprocess.CREATE_NEW_CONSOLE,
        )
        return

    subprocess.Popen(
        cmd,
        cwd=str(cwd),
        stdout=log_handle,
        stderr=subprocess.STDOUT,
        start_new_session=True,
    )


def main() -> int:
    print("[1/4] Verificando servicos...")

    if not is_port_open(8080):
        print("[2/4] Iniciando backend Java...")
        spawn_process(["mvn", "spring-boot:run"], PROJECT_ROOT, BACKEND_LOG)
    else:
        print("[2/4] Backend Java ja esta rodando.")

    if not wait_port(8080, 90):
        print(f"[erro] Backend nao subiu na porta 8080. Verifique {BACKEND_LOG}")
        return 1

    if not is_port_open(5173):
        print("[3/4] Iniciando frontend React...")
        spawn_process(["npm", "run", "dev", "--", "--host", "127.0.0.1", "--port", "5173"], FRONTEND_DIR, FRONTEND_LOG)
    else:
        print("[3/4] Frontend React ja esta rodando.")

    if not wait_port(5173, 60):
        print(f"[erro] Frontend nao subiu na porta 5173. Verifique {FRONTEND_LOG}")
        return 1

    print("[4/4] Abrindo aplicacao no navegador...")
    webbrowser.open("http://127.0.0.1:5173")
    print("Aplicacao iniciada com sucesso.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
