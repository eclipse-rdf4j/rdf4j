#!/usr/bin/env python3

from __future__ import annotations

import argparse
import os
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path


@dataclass
class Summary:
    tests: int = 0
    failures: int = 0
    errors: int = 0
    skipped: int = 0
    time: float = 0.0
    reports: list[Path] = field(default_factory=list)
    problems: list[str] = field(default_factory=list)


def clip(value: str, limit: int = 160) -> str:
    compact = " ".join(value.split())
    if len(compact) <= limit:
        return compact
    return compact[: limit - 3] + "..."


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def int_attr(element: ET.Element, name: str) -> int:
    try:
        return int(element.attrib.get(name, "0"))
    except ValueError:
        return 0


def float_attr(element: ET.Element, name: str) -> float:
    try:
        return float(element.attrib.get(name, "0"))
    except ValueError:
        return 0.0


def test_suites(root: ET.Element) -> list[ET.Element]:
    if local_name(root.tag) == "testsuite":
        return [root]
    return [element for element in root.iter() if local_name(element.tag) == "testsuite"]


def discover_reports(paths: list[Path]) -> list[Path]:
    reports: list[Path] = []
    for path in paths:
        if path.is_dir():
            reports.extend(path.glob("*.xml"))
            reports.extend(path.glob("*.txt"))
        elif path.is_file() and path.suffix in {".xml", ".txt"}:
            reports.append(path)
    return sorted({report.resolve() for report in reports})


def parse_xml(report: Path, summary: Summary) -> None:
    try:
        root = ET.parse(report).getroot()
    except (ET.ParseError, OSError):
        return

    suites = test_suites(root)
    if not suites:
        return

    summary.reports.append(report)
    for suite in suites:
        summary.tests += int_attr(suite, "tests")
        summary.failures += int_attr(suite, "failures")
        summary.errors += int_attr(suite, "errors")
        summary.skipped += int_attr(suite, "skipped")
        summary.time += float_attr(suite, "time")

        for testcase in suite.iter():
            if local_name(testcase.tag) != "testcase":
                continue
            classname = testcase.attrib.get("classname") or suite.attrib.get("name") or "<unknown>"
            name = testcase.attrib.get("name") or "<unknown>"
            for child in testcase:
                child_name = local_name(child.tag)
                if child_name not in {"failure", "error"}:
                    continue
                message = child.attrib.get("message") or child.text or ""
                summary.problems.append(f"{classname}.{name}: {child_name}: {clip(message)}")


def summarize(reports: list[Path]) -> Summary:
    summary = Summary()
    for report in reports:
        if report.suffix == ".xml":
            parse_xml(report, summary)
        elif report.suffix == ".txt":
            summary.reports.append(report)
    return summary


def display_path(path: Path, root: Path) -> str:
    try:
        return path.resolve().relative_to(root.resolve()).as_posix()
    except ValueError:
        return path.as_posix()


def main() -> int:
    parser = argparse.ArgumentParser(description="Print compact Surefire/Failsafe evidence from report files.")
    parser.add_argument("paths", nargs="+", type=Path, help="Report files or report directories.")
    parser.add_argument("--root", type=Path, default=Path.cwd(), help="Root used to shorten printed paths.")
    parser.add_argument("--command", help="Command that produced these reports.")
    parser.add_argument("--log", type=Path, help="Full Maven/test log path.")
    args = parser.parse_args()

    reports = discover_reports(args.paths)
    summary = summarize(reports)
    printed_reports = summary.reports or reports

    print("Evidence:")
    if args.command:
        print(f"Command: {args.command}")
    if printed_reports:
        report_text = ", ".join(display_path(report, args.root) for report in printed_reports[:3])
        if len(printed_reports) > 3:
            report_text += f", +{len(printed_reports) - 3} more"
        print(f"Reports: {report_text}")
    else:
        print("Reports: none found")
    print(
        "Summary: "
        f"tests={summary.tests}, failures={summary.failures}, errors={summary.errors}, "
        f"skipped={summary.skipped}, time={summary.time:.3f}s"
    )
    if summary.problems:
        print(f"Failure: {summary.problems[0]}")
    if args.log:
        print(f"Log: {display_path(args.log, args.root)}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
