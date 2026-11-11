#!/usr/bin/env python3
"""Verify the packaged standalone epiSLATE test app before publishing it."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import zipfile

apk = Path(sys.argv[1])
sdk = Path(os.environ.get("ANDROID_HOME") or os.environ["ANDROID_SDK_ROOT"])
aapts = sorted((sdk / "build-tools").glob("*/aapt"))
if not aapts:
    raise SystemExit("Android aapt is unavailable")
aapt = str(aapts[-1])
badging = subprocess.check_output([aapt, "dump", "badging", str(apk)], text=True)
package = re.search(r"^package: name='([^']+)'", badging, re.M)
labels = re.findall(r"^application-label(?:-[^:]+)?:'([^']*)'", badging, re.M)
if not package or package.group(1) != "com.nuvio.livetv.epislate.collectionreview20261111":
    raise SystemExit("APK is not the standalone test application")
if not labels or set(labels) != {"epiSLATE Collection Test"}:
    raise SystemExit(f"Wrong packaged app labels: {sorted(set(labels))}")
resources = subprocess.check_output([aapt, "dump", "resources", str(apk)], text=True)
if "drawable/epislate_wordmark" not in resources:
    raise SystemExit("Packaged epiSLATE wordmark is missing")
root = Path(__file__).resolve().parents[1]
for relative in [
    "app/src/main/java/com/nuvio/tv/ui/screens/account/AuthQrSignInScreen.kt",
    "app/src/main/java/com/nuvio/tv/ui/components/StartupSplashScreen.kt",
]:
    if "epislate_wordmark" not in (root / relative).read_text(encoding="utf-8"):
        raise SystemExit(f"epiSLATE wordmark not used in {relative}")
with zipfile.ZipFile(apk) as archive:
    if archive.testzip() is not None:
        raise SystemExit("APK archive integrity check failed")
receipt = {
    "source_commit": os.environ.get("GITHUB_SHA"),
    "run_url": f"https://github.com/{os.environ.get('GITHUB_REPOSITORY')}/actions/runs/{os.environ.get('GITHUB_RUN_ID')}",
    "application_id": package.group(1),
    "application_labels": sorted(set(labels)),
    "apk": apk.name,
    "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
    "size_bytes": apk.stat().st_size,
}
Path("epiSLATE-build-identity.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
print(json.dumps(receipt, indent=2))
