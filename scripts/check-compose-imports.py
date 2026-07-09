#!/usr/bin/env python3
"""Verify Compose UI files import symbols they use.

Run after splitting monolithic Compose files:
  python3 scripts/check-compose-imports.py
  python3 scripts/fix-compose-imports.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UI_DIR = ROOT / "app/src/main/java/com/example/ui"

SYMBOL_IMPORTS: dict[str, str] = {
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
    r"RoundedCornerShape": "androidx.compose.foundation.shape.RoundedCornerShape",
    r"testTag\s*\(": "androidx.compose.ui.platform.testTag",
    r"Brush\.": "androidx.compose.ui.graphics.Brush",
    r"Canvas\s*\(": "androidx.compose.foundation.Canvas",
    r"\bLazyColumn\s*\(": "androidx.compose.foundation.lazy.LazyColumn",
    r"\bLazyListScope\b": "androidx.compose.foundation.lazy.LazyListScope",
    r"rememberScrollState\s*\(": "androidx.compose.foundation.rememberScrollState",
    r"rememberLazyListState\s*\(": "androidx.compose.foundation.lazy.rememberLazyListState",
    r"\bBorderStroke\s*\(": "androidx.compose.foundation.BorderStroke",
    r"\bIcons\.": "androidx.compose.material.icons.Icons",
    r"Icons\.Default\.": "androidx.compose.material.icons.filled",
    r"\bIcon\s*\(": "androidx.compose.material3.Icon",
    r"\bCard\s*\(": "androidx.compose.material3.Card",
    r"CardDefaults\.": "androidx.compose.material3.CardDefaults",
    r"\bBox\s*\(": "androidx.compose.foundation.layout.Box",
    r"\bAlignment\.": "androidx.compose.ui.Alignment",
    r"SimpleDateFormat\s*\(": "java.text.SimpleDateFormat",
    r"Locale\.": "java.util.Locale",
    r"\bDate\s*\(": "java.util.Date",
    r"PackageManager\.": "android.content.pm.PackageManager",
    r"Manifest\.permission": "android.Manifest",
    r"ContextCompat\.": "androidx.core.content.ContextCompat",
    r"LocalContext\.": "androidx.compose.ui.platform.LocalContext",
    r"collectAsStateWithLifecycle": "androidx.lifecycle.compose.collectAsStateWithLifecycle",
    r"Intent\s*\(": "android.content.Intent",
    r"\bUri\.": "android.net.Uri",
    r"DisposableEffect\s*\(": "androidx.compose.runtime.DisposableEffect",
    r"LaunchedEffect\s*\(": "androidx.compose.runtime.LaunchedEffect",
}


def check_file(path: Path) -> list[str]:
    text = path.read_text()
    import_lines = [
        line.strip()
        for line in text.splitlines()
        if line.startswith("import ")
    ]

    def has_import(required: str) -> bool:
        if any(required in line for line in import_lines):
            return True
        package = required.rsplit(".", 1)[0]
        return any(line.endswith(f"{package}.*") for line in import_lines)

    issues: list[str] = []
    for pattern, required_import in SYMBOL_IMPORTS.items():
        if re.search(pattern, text) and not has_import(required_import):
            rel = path.relative_to(ROOT)
            issues.append(f"{rel}: uses {pattern.strip('()')} but missing import {required_import}")
    return issues


def main() -> int:
    if not UI_DIR.exists():
        print(f"UI directory not found: {UI_DIR}", file=sys.stderr)
        return 1

    all_issues: list[str] = []
    for path in sorted(UI_DIR.rglob("*.kt")):
        all_issues.extend(check_file(path))

    if all_issues:
        print("Compose import check failed:\n")
        for issue in all_issues:
            print(f"  - {issue}")
        print(f"\n{len(all_issues)} issue(s). Run: python3 scripts/fix-compose-imports.py")
        return 1

    print("Compose import check passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())