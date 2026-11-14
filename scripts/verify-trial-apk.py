"""Fail before upload if the artifact is not the isolated invitation preview."""
from pathlib import Path
import subprocess
import sys

apk, aapt, build_config = map(Path, sys.argv[1:])
config = build_config.read_text(encoding="utf-8")
required = (
    'APPLICATION_ID = "com.nuvio.livetv.trial"',
    'TRIAL_ENABLED = true',
    'FEATURE_IN_APP_UPDATES_ENABLED = false',
    'VERSION_NAME = "1.0.0-beta.1-trial"',
)
for marker in required:
    if marker not in config:
        raise SystemExit("Trial build configuration check failed: " + marker)
badging = subprocess.check_output([str(aapt), "dump", "badging", str(apk)], text=True)
if not any(line.startswith("package: name='com.nuvio.livetv.trial'") and "versionName='1.0.0-beta.1-trial'" in line for line in badging.splitlines()):
    raise SystemExit("APK package or version is not the dedicated trial.")
for prefix in ("application-label:", "launchable-activity:"):
    rows = [line for line in badging.splitlines() if line.startswith(prefix)]
    if not rows or not all("epiSLATE Trial" in line for line in rows):
        raise SystemExit("APK application/launcher label is not epiSLATE Trial.")
print("Verified trial package, version, launcher name, activation flag, and disabled beta updates.")
