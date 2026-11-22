"""Fail before upload if the artifact is not the isolated invitation preview."""
from pathlib import Path
import subprocess
import sys

apk, aapt, build_config = map(Path, sys.argv[1:])
config = build_config.read_text(encoding="utf-8")
required = (
    'APPLICATION_ID = "com.nuvio.livetv.trial"',
    'TRIAL_ENABLED = true',
    'VERSION_CODE = 107119',
    'FEATURE_IN_APP_UPDATES_ENABLED = false',
    'VERSION_NAME = "1.0.0-beta.4-trial"',
)
for marker in required:
    if marker not in config:
        raise SystemExit("Trial build configuration check failed: " + marker)
badging = subprocess.check_output([str(aapt), "dump", "badging", str(apk)], text=True)
if not any(line.startswith("package: name='com.nuvio.livetv.trial'") and "versionName='1.0.0-beta.4-trial'" in line and "versionCode='107119'" in line for line in badging.splitlines()):
    raise SystemExit("APK package or version is not the dedicated trial.")
for prefix in ("application-label:", "launchable-activity:"):
    rows = [line for line in badging.splitlines() if line.startswith(prefix)]
    if not rows or not all("epiSLATE Trial" in line for line in rows):
        raise SystemExit("APK application/launcher label is not epiSLATE Trial.")
print("Verified trial package, version, launcher name, activation flag, and disabled beta updates.")

# Verify preset is actually packaged in this dedicated trial artifact.
import json
import zipfile
with zipfile.ZipFile(apk) as archive:
    preset = json.loads(archive.read('assets/trial-preset.json'))
assert preset['version'] == 1 and len(preset['addonUrls']) == 2
assert len(preset['collections']) == 4
discover = next(row for row in preset['collections'] if row['title'] == 'DISCOVER')
assert len(discover['folders']) == 4
assert preset['discoverCatalogsRevision'] == '2026-11-22'
assert preset['qualityCatalogsRevision'] == '2026-11-22-v05'
assert {folder['title']: len(folder['sources']) for folder in discover['folders']} == {
    'New Releases': 2, 'Coming Soon': 2, 'AI Picks': 0, 'Trending': 0,
}
import hashlib
assert hashlib.sha256(json.dumps(preset['collections'], sort_keys=True, separators=(',', ':')).encode()).hexdigest() == 'bbbb17f7e7858a71a85afa3125967a6cb35e86e57d5f9f3e1a462628dec3789b'
assert hashlib.sha256(json.dumps(preset['addonUrls'], separators=(',', ':')).encode()).hexdigest() == '7103b7b3294727166a0f99e12c302665f63c7885e2562adc87ed18e88defb4a3'
print('Verified packaged trial preset and restricted Discover sources.')
