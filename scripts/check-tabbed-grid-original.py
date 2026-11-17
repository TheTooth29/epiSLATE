"""Protect the original Tabbed Grid implementation during the Test-only comparison."""
import hashlib
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / "app/src/main/java/com/nuvio/tv/ui/screens/collection/FolderDetailScreen.kt").read_text(encoding="utf-8")
function = source.index("private fun OriginalTabbedGridContent(")
start = source.rfind("@OptIn", 0, function)
end = source.index("@OptIn", function)
original = source[start:end].replace("private fun OriginalTabbedGridContent(", "private fun TabbedGridContent(", 1)
expected = "6f7a546b93682de9ba37c83fc27b631adf73a5ab34bc705f686c2484b53af6e0"
assert hashlib.sha256(original.encode()).hexdigest() == expected, "Original Tabbed Grid differs from checkpoint 0592e9e"
print("Original Tabbed Grid matches the frozen checkpoint (only its name changed).")
