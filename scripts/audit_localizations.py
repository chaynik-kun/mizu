#!/usr/bin/env python3
"""Audit direct/effective localization coverage and format placeholders."""

from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / "composeApp/src/commonMain/composeResources"
FORMAT = re.compile(r"%(?:(\d+)\$)?([a-zA-Z%])")
PARENTS = {"de-rCH": "de", "pt-rBR": "pt"}


def resources(locale: str) -> dict[tuple[str, str], ET.Element]:
    directory = "values" if locale == "en" else f"values-{locale}"
    root = ET.parse(ROOT / directory / "strings.xml").getroot()
    return {(node.tag, node.attrib["name"]): node for node in root if node.tag in {"string", "plurals"}}


def placeholders(text: str) -> list[tuple[str, str]]:
    return sorted((position or "", kind) for position, kind in FORMAT.findall(text) if kind != "%")


def validate(base: dict, translated: dict) -> list[str]:
    issues = []
    for key, node in translated.items():
        if key not in base:
            continue
        source = base[key]
        if key[0] == "string":
            if placeholders("".join(source.itertext())) != placeholders("".join(node.itertext())):
                issues.append(key[1])
            continue
        source_items = {item.attrib["quantity"]: "".join(item.itertext()) for item in source}
        for item in node:
            reference = source_items.get(item.attrib["quantity"], source_items["other"])
            if placeholders(reference) != placeholders("".join(item.itertext())):
                issues.append(f"{key[1]}:{item.attrib['quantity']}")
    return issues


def main() -> int:
    base = resources("en")
    translatable = {key for key, node in base.items() if node.attrib.get("translatable") != "false"}
    failed = False
    for path in sorted(ROOT.glob("values-*/strings.xml")):
        locale = path.parent.name.removeprefix("values-")
        direct = resources(locale)
        parent = resources(PARENTS[locale]) if locale in PARENTS else {}
        direct_keys = translatable & set(direct)
        parent_keys = translatable & set(parent)
        # Android always falls back to the default resources. "effective" here means
        # locale or regional-parent coverage before that final English fallback.
        effective = direct_keys | parent_keys
        issues = validate(base, direct)
        failed |= bool(issues)
        print(
            f"{locale:8} direct={len(direct_keys):3}/{len(translatable)} "
            f"effective={len(effective):3}/{len(translatable)} missing={len(translatable)-len(effective):3} "
            f"plurals={sum(key[0] == 'plurals' for key in direct):1} placeholders={len(issues)}"
        )
        for issue in issues:
            print(f"  placeholder mismatch: {issue}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
