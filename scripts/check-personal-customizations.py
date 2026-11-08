#!/usr/bin/env python3
"""Fail an update build when a known personal customization is missing."""
from pathlib import Path
import sys
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
        'buildConfigField("String", "GITHUB_REPO", "\\\"NuvioPlusIPTV-TimmysFork\\\"")')
require("app/src/main/java/com/nuvio/tv/ui/screens/home/ModernHomeContent.kt",
        "val collectionHero = activeCarouselItemState.value?.payload as? ModernPayload.CollectionFolder",
        "!collectionHero.heroBackdropUrl.isNullOrBlank()",
        "!collectionHero.heroVideoUrl.isNullOrBlank()", "if (!rowsOnlyHomepage && showHeroMedia) {")
for workflow in [".github/workflows/pr-full-debug-build.yml", ".github/workflows/livetv-build.yml"]:
    require(workflow, "secrets.NUVIO_BACKEND_URL", "secrets.NUVIO_BACKEND_KEY", "secrets.TMDB_API_KEY",
            "add NUVIO_SUPABASE_URL", "add NUVIO_SUPABASE_ANON_KEY", "add TMDB_API_KEY")
if errors:
    print("\n".join(errors), file=sys.stderr)
    sys.exit(1)
print("Personal customization checks passed. Device testing is still required.")
