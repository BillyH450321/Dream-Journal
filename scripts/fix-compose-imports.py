#!/usr/bin/env python3
"""Add missing Compose and common Android imports detected by check-compose-imports.py."""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CHECK = ROOT / "scripts/check-compose-imports.py"

# Usage pattern -> required import (checked as substring match in import lines)
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
    r"Icons\.AutoMirrored\.": "androidx.compose.material.icons.automirrored",
    r"\bIcon\s*\(": "androidx.compose.material3.Icon",
    r"\bCard\s*\(": "androidx.compose.material3.Card",
    r"CardDefaults\.": "androidx.compose.material3.CardDefaults",
    r"ButtonDefaults\.": "androidx.compose.material3.ButtonDefaults",
    r"OutlinedTextFieldDefaults\.": "androidx.compose.material3.OutlinedTextFieldDefaults",
    r"ExperimentalMaterial3Api": "androidx.compose.material3.ExperimentalMaterial3Api",
    r"\bBox\s*\(": "androidx.compose.foundation.layout.Box",
    r"\bAlignment\.": "androidx.compose.ui.Alignment",
    r"\bColor\b": "androidx.compose.ui.graphics.Color",
    r"ContentScale\.": "androidx.compose.ui.layout.ContentScale",
    r"AsyncImage\s*\(": "coil.compose.AsyncImage",
    r"SimpleDateFormat\s*\(": "java.text.SimpleDateFormat",
    r"Locale\.": "java.util.Locale",
    r"\bDate\s*\(": "java.util.Date",
    r"PackageManager\.": "android.content.pm.PackageManager",
    r"Manifest\.permission": "android.Manifest",
    r"ContextCompat\.": "androidx.core.content.ContextCompat",
    r"LocalContext\.": "androidx.compose.ui.platform.LocalContext",
    r"LocalSoftwareKeyboardController": "androidx.compose.ui.platform.LocalSoftwareKeyboardController",
    r"collectAsStateWithLifecycle": "androidx.lifecycle.compose.collectAsStateWithLifecycle",
    r"PasswordVisualTransformation": "androidx.compose.ui.text.input.PasswordVisualTransformation",
    r"VisualTransformation": "androidx.compose.ui.text.input.VisualTransformation",
    r"KeyboardOptions\s*\(": "androidx.compose.foundation.text.KeyboardOptions",
    r"KeyboardActions\s*\(": "androidx.compose.foundation.text.KeyboardActions",
    r"ImeAction\.": "androidx.compose.ui.text.input.ImeAction",
    r"ActivityResultContracts\.": "androidx.activity.result.contract.ActivityResultContracts",
    r"rememberLauncherForActivityResult": "androidx.activity.compose.rememberLauncherForActivityResult",
    r"Intent\s*\(": "android.content.Intent",
    r"\bUri\.": "android.net.Uri",
    r"DisposableEffect\s*\(": "androidx.compose.runtime.DisposableEffect",
    r"LaunchedEffect\s*\(": "androidx.compose.runtime.LaunchedEffect",
    r"collectAsStateWithLifecycle": "androidx.lifecycle.compose.collectAsStateWithLifecycle",
}


def fix_file(path: Path) -> int:
    text = path.read_text()
    import_lines = [line.strip() for line in text.splitlines() if line.startswith("import ")]

    def has_import(required: str) -> bool:
        if any(required in line for line in import_lines):
            return True
        package = required.rsplit(".", 1)[0]
        return any(line.endswith(f"{package}.*") for line in import_lines)

    to_add: list[str] = []
    for pattern, required_import in SYMBOL_IMPORTS.items():
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