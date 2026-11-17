#!/usr/bin/env python3
"""Fail an update build when a known personal customization is missing."""
from pathlib import Path
import sys
import struct
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
errors = []
def require(path, *tokens):
    text = (root / path).read_text(encoding="utf-8")
    for token in tokens:
        if token not in text:
            errors.append(f"{path}: missing {token!r}")

strings = ET.parse(root / "app/src/main/res/values/strings.xml").getroot()
values = {item.attrib.get("name"): "".join(item.itertext()) for item in strings}
for key, expected in {"continue_watching": "CONTINUE WATCHING", "upcoming_section_title": "UPCOMING"}.items():
    if values.get(key) != expected:
        errors.append(f"{key} must remain {expected!r}")

require("app/build.gradle.kts",
        'buildConfigField("String", "GITHUB_OWNER", "\\\"TheTooth29\\\"")',
        'buildConfigField("String", "GITHUB_REPO", "\\\"epiSLATE\\\"")')
require("app/src/main/java/com/nuvio/tv/ui/screens/home/ModernHomeContent.kt",
        "val collectionHero = activeCarouselItemState.value?.payload as? ModernPayload.CollectionFolder",
        "!collectionHero.heroBackdropUrl.isNullOrBlank()",
        "!collectionHero.heroVideoUrl.isNullOrBlank()", "if (!rowsOnlyHomepage && showHeroMedia) {")
for workflow in [".github/workflows/pr-full-debug-build.yml", ".github/workflows/livetv-build.yml"]:
    require(workflow, "secrets.NUVIO_BACKEND_URL", "secrets.NUVIO_BACKEND_KEY", "secrets.TMDB_API_KEY",
            "add NUVIO_SUPABASE_URL", "add NUVIO_SUPABASE_ANON_KEY", "add TMDB_API_KEY")
# Android Settings loads the application icon, independently of the TV launcher activity.
# Use density-specific bitmap package icons for System UI compatibility.
android = "{http://schemas.android.com/apk/res/android}"
application = ET.parse(root / "app/src/full/AndroidManifest.xml").getroot().find("application")
for attribute, resource in {"icon": "epislate_app_icon", "roundIcon": "epislate_app_icon_round"}.items():
    if application is None or application.get(android + attribute) != "@mipmap/" + resource:
        errors.append(f"full application {attribute} must use @mipmap/{resource}")
    for density, size in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        icon = root / f"app/src/full/res/mipmap-{density}/{resource}.png"
        data = icon.read_bytes() if icon.exists() else b""
        if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n" or struct.unpack(">II", data[16:24]) != (size, size):
            errors.append(f"{icon.relative_to(root)} must be a valid {size}x{size} PNG")

if errors:
    print("\n".join(errors), file=sys.stderr)
    sys.exit(1)
print("Personal customization checks passed. Device testing is still required.")
