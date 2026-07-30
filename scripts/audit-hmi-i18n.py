#!/usr/bin/env python3
"""Audit Chinese/English HMI resources and obvious hard-coded UI literals.

The parity check is strict and intended for CI. The hard-coded literal scan is a
heuristic report by default; pass --strict-hardcoded to make findings fatal.
"""

from __future__ import annotations

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

FORMAT_TOKEN_RE = re.compile(
    r"%(?:\d+\$)?[-#+ 0,(<]*\d*(?:\.\d+)?[a-zA-Z%]"
)
KOTLIN_LITERAL_PATTERNS = (
    re.compile(
        r"\b(?:text|title|subtitle|message|label|placeholder|contentDescription|"
        r"actionText|summary|headline|supportingText)\s*=\s*\"([^\"$]*(?:\\.[^\"$]*)*)\""
    ),
    re.compile(r"\bText\(\s*\"([^\"$]*(?:\\.[^\"$]*)*)\""),
)
HUMAN_TEXT_RE = re.compile(r"[A-Za-z\u3400-\u9fff]")
SKIP_DIRS = {
    ".git", ".gradle", ".idea", "build", "generated", "node_modules",
    "vendor", "Design",
}
SOURCE_SET_NAMES = {"commonMain", "androidMain", "desktopMain", "iosMain"}


@dataclass(frozen=True)
class ResourceValue:
    kind: str
    placeholders: tuple[str, ...]


def placeholders(text: str) -> tuple[str, ...]:
    return tuple(sorted(token for token in FORMAT_TOKEN_RE.findall(text) if token != "%%"))


def parse_resources(path: Path) -> dict[str, ResourceValue]:
    try:
        root = ET.parse(path).getroot()
    except (ET.ParseError, OSError) as exc:
        raise ValueError(f"无法解析 {path}: {exc}") from exc

    result: dict[str, ResourceValue] = {}
    for child in root:
        name = child.attrib.get("name")
        if not name or child.attrib.get("translatable") == "false":
            continue
        if child.tag == "string":
            text = "".join(child.itertext())
            result[name] = ResourceValue("string", placeholders(text))
        elif child.tag == "plurals":
            token_set: set[str] = set()
            for item in child.findall("item"):
                token_set.update(placeholders("".join(item.itertext())))
            result[name] = ResourceValue("plurals", tuple(sorted(token_set)))
    return result


def resource_pairs(root: Path) -> Iterable[tuple[Path, Path]]:
    for default in root.rglob("strings.xml"):
        if any(part in SKIP_DIRS for part in default.parts):
            continue
        parent = default.parent
        if parent.name != "values":
            continue
        if parent.parent.name == "composeResources":
            yield default, parent.parent / "values-zh" / "strings.xml"
        elif parent.parent.name == "res":
            yield default, parent.parent / "values-zh-rCN" / "strings.xml"


def audit_pair(default: Path, translated: Path, root: Path) -> list[str]:
    display_default = default.relative_to(root)
    display_translated = translated.relative_to(root)
    if not translated.exists():
        return [f"缺少中文资源文件: {display_translated}（对应 {display_default}）"]

    source = parse_resources(default)
    target = parse_resources(translated)
    errors: list[str] = []

    for name in sorted(source.keys() - target.keys()):
        errors.append(f"{display_translated}: 缺少资源 {name}")
    for name in sorted(target.keys() - source.keys()):
        errors.append(f"{display_translated}: 多余资源 {name}")
    for name in sorted(source.keys() & target.keys()):
        if source[name].kind != target[name].kind:
            errors.append(
                f"{display_translated}: {name} 类型不一致: "
                f"{source[name].kind} != {target[name].kind}"
            )
        if source[name].placeholders != target[name].placeholders:
            errors.append(
                f"{display_translated}: {name} 格式占位符不一致: "
                f"{source[name].placeholders} != {target[name].placeholders}"
            )
    return errors


def is_runtime_kotlin(path: Path) -> bool:
    if path.suffix != ".kt":
        return False
    if any(part in SKIP_DIRS for part in path.parts):
        return False
    if any(part.endswith("Test") or part.endswith("TestFixtures") for part in path.parts):
        return False
    return bool(SOURCE_SET_NAMES.intersection(path.parts))


def scan_hardcoded(root: Path) -> list[str]:
    findings: list[str] = []
    for path in root.rglob("*.kt"):
        if not is_runtime_kotlin(path):
            continue
        try:
            lines = path.read_text(encoding="utf-8").splitlines()
        except UnicodeDecodeError:
            continue
        for line_number, line in enumerate(lines, 1):
            stripped = line.strip()
            if stripped.startswith("//") or "@Preview" in stripped:
                continue
            for pattern in KOTLIN_LITERAL_PATTERNS:
                for match in pattern.finditer(line):
                    literal = match.group(1)
                    if not HUMAN_TEXT_RE.search(literal):
                        continue
                    if literal.startswith(("http://", "https://", "content://", "file://")):
                        continue
                    findings.append(
                        f"{path.relative_to(root)}:{line_number}: {literal}"
                    )
    return sorted(set(findings))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument(
        "--strict-hardcoded",
        action="store_true",
        help="将疑似硬编码 HMI 文本视为错误",
    )
    args = parser.parse_args()
    root = args.root.resolve()

    parity_errors: list[str] = []
    pairs = list(resource_pairs(root))
    for default, translated in pairs:
        parity_errors.extend(audit_pair(default, translated, root))

    hardcoded = scan_hardcoded(root)

    print(f"检查资源组: {len(pairs)}")
    if parity_errors:
        print(f"\n资源一致性错误: {len(parity_errors)}", file=sys.stderr)
        for error in parity_errors:
            print(f"  - {error}", file=sys.stderr)
    else:
        print("中英文资源键、类型和格式占位符一致。")

    if hardcoded:
        heading = "硬编码 HMI 文本错误" if args.strict_hardcoded else "疑似硬编码 HMI 文本（需人工确认）"
        stream = sys.stderr if args.strict_hardcoded else sys.stdout
        print(f"\n{heading}: {len(hardcoded)}", file=stream)
        for finding in hardcoded:
            print(f"  - {finding}", file=stream)
    else:
        print("未发现明显的硬编码 HMI 文本。")

    if parity_errors or (args.strict_hardcoded and hardcoded):
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
