#!/usr/bin/env python3
"""Fetch the author's 26.3 NeoForge Controllable release for compile-only use.

The SHA-256 is the digest from the author's GitHub release asset metadata:
https://github.com/MrCrayfish/Controllable/releases/tag/v0.26.2%2B26.3
No artifact is published by this script.
"""
import hashlib
from pathlib import Path
import urllib.request

NAME = "controllable-neoforge-0.26.2+26.3-signed.jar"
URL = ("https://github.com/MrCrayfish/Controllable/releases/download/"
       "v0.26.2%2B26.3/controllable-neoforge-0.26.2%2B26.3-signed.jar")
SHA256 = "0aeca1e79043177950e80d6433f3ffac2d28358dfcb0ef9ee3454e0ae8c6ed0c"
target = Path(__file__).resolve().parent.parent / "local-dependencies" / NAME
target.parent.mkdir(parents=True, exist_ok=True)
if not target.exists() or hashlib.sha256(target.read_bytes()).hexdigest() != SHA256:
    temporary = target.with_suffix(".jar.part")
    with urllib.request.urlopen(URL, timeout=120) as response:
        temporary.write_bytes(response.read())
    if hashlib.sha256(temporary.read_bytes()).hexdigest() != SHA256:
        temporary.unlink()
        raise SystemExit("Controllable checksum mismatch; refusing the artifact")
    temporary.replace(target)
print(f"Verified {target.name}")

# Required only for the optional Controllable client development runtime.
framework = target.parent / "framework-neoforge-0.13.27+26.3-signed.jar"
framework_sha256 = "1d18809e402156b3ff4244baa9a156c6b7e957b3452cc09b7e550753fdcc8183"
if not framework.exists() or hashlib.sha256(framework.read_bytes()).hexdigest() != framework_sha256:
    temporary = framework.with_suffix(".jar.part")
    url = ("https://github.com/MrCrayfish/Framework/releases/download/"
           "v0.13.27%2B26.3/framework-neoforge-0.13.27%2B26.3-signed.jar")
    with urllib.request.urlopen(url, timeout=120) as response:
        temporary.write_bytes(response.read())
    if hashlib.sha256(temporary.read_bytes()).hexdigest() != framework_sha256:
        temporary.unlink()
        raise SystemExit("Framework checksum mismatch; refusing the artifact")
    temporary.replace(framework)
print(f"Verified {framework.name}")
