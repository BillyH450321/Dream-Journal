#!/usr/bin/env python3
"""Add missing Compose modifier imports detected by check-compose-imports.py."""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CHECK = ROOT / "scripts/check-compose-imports.py"

MODIFIER_IMPORTS: dict[str, str] = {
    r"\.background\s*\(": "androidx.compose.foundation.background",
    r"\.clickable\s*\(": "androidx.compose.foundation.clickable",
    r"\.border\s*\(": "androidx.compose.foundation.border",
    r"\.clip\s*\(": "androidx.compose.ui.draw.clip",
    r"\.drawBehind\s*\(": "androidx.compose.ui.draw.drawBehind",
    r"\.horizontalScroll\s*\(": "androidx.compose.foundation.horizontalScroll",
    r"\.verticalScroll\s*\(": "androidx.compose.foundation.verticalScroll",
    r"\.scale\s*\(": "androidx.compose.ui.draw.scale",
    r"\.shadow\s*\(": "androidx.compose.ui.draw.shadow",
    r"FontFamily\.": "androidx.compose.ui.text.font.FontFamily",
    r"CircleShape": "androidx.compose.foundation.shape.CircleShape",
    r"testTag\s*\(": "androidx.compose.ui.platform.testTag",
    r"Brush\.": "androidx.compose.ui.graphics.Brush",
    r"Canvas\s*\(": "androidx.compose.foundation.Canvas",
}


def fix_file(path: Path) -> int:
    text = path.read_text()
    import_lines = [line.strip() for line in text.splitlines() if line.startswith("import ")]

    def has_import(required: str) -> bool:
        return any(required in line for line in import_lines)

    to_add: list[str] = []
    for pattern, required_import in MODIFIER_IMPORTS.items():
        if re.search(pattern, text) and not has_import(required_import):
            to_add.append(required_import)

    if not to_add:
        return 0

    unique_imports = sorted(set(to_add))
    new_import_block = "\n".join(f"import {imp}" for imp in unique_imports)

    lines = text.splitlines()
    last_import_idx = max(i for i, line in enumerate(lines) if line.startswith("import "))
    lines.insert(last_import_idx + 1, new_import_block)
    path.write_text("\n".join(lines) + "\n")
    return len(unique_imports)


def main() -> int:
    ui_dir = ROOT / "app/src/main/java/com/example/ui"
    total = 0
    for path in sorted(ui_dir.rglob("*.kt")):
        total += fix_file(path)

    result = subprocess.run([sys.executable, str(CHECK)], cwd=ROOT, check=False)
    print(f"Added imports to cover {total} missing symbol(s).")
    return result.returncode


if __name__ == "__main__":
    raise SystemExit(main())