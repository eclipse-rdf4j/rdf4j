#!/usr/bin/env python3
"""Fail fast when benchmark output exposes a clearly dangerous query plan."""

from __future__ import annotations

import argparse
import os
import re
import sys
from dataclasses import dataclass
from typing import Iterable


EXIT_PLAN_RISK = 42
DEFAULT_MAX_CARTESIAN_WORK_ROWS = 100_000_000.0

ROW_RE = re.compile(r"([0-9]+(?:\.[0-9]+)?)([KMB])?")
CARTESIAN_WORK_RE = re.compile(r"plannedCostCartesianWorkRows=([0-9]+(?:\.[0-9]+)?)([KMB])?")
SPO_DIRECT_LOOKUP_RE = re.compile(
		r"StatementPattern.*plannedIndexAccessMode=directLookup.*plannedLookupComponents=\[S, P, O\]")
UNBOUND_SUBJECT_RE = re.compile(r"\bs:\s+Var .*bindingState=unbound")


@dataclass(frozen=True)
class PlanRisk:
	kind: str
	message: str
	line: str


class PlanRiskDetector:
	def __init__(self, max_cartesian_work_rows: float) -> None:
		self.max_cartesian_work_rows = max_cartesian_work_rows
		self.pending_direct_spo_header: str | None = None
		self.pending_direct_spo_lines = 0

	def feed(self, line: str) -> list[PlanRisk]:
		risks: list[PlanRisk] = []
		risks.extend(self._cartesian_work_risks(line))
		risks.extend(self._direct_spo_risks(line))
		return risks

	def _cartesian_work_risks(self, line: str) -> list[PlanRisk]:
		risks: list[PlanRisk] = []
		for match in CARTESIAN_WORK_RE.finditer(line):
			rows = parse_rows(match.group(0).split("=", 1)[1])
			if rows > self.max_cartesian_work_rows:
				risks.append(PlanRisk(
					"cartesian-work",
					f"plannedCostCartesianWorkRows={format_rows(rows)} exceeds "
					f"{format_rows(self.max_cartesian_work_rows)}",
					line.rstrip("\n"),
				))
		return risks

	def _direct_spo_risks(self, line: str) -> list[PlanRisk]:
		risks: list[PlanRisk] = []
		if self.pending_direct_spo_header is not None:
			self.pending_direct_spo_lines -= 1
			if UNBOUND_SUBJECT_RE.search(line):
				risks.append(PlanRisk(
					"unbound-spo-direct-lookup",
					"direct [S, P, O] lookup is planned while the subject binding is still unbound",
					self.pending_direct_spo_header.rstrip("\n") + "\n" + line.rstrip("\n"),
				))
				self.pending_direct_spo_header = None
				self.pending_direct_spo_lines = 0
			elif self.pending_direct_spo_lines <= 0:
				self.pending_direct_spo_header = None
		if SPO_DIRECT_LOOKUP_RE.search(line):
			self.pending_direct_spo_header = line
			self.pending_direct_spo_lines = 8
		return risks


def parse_rows(value: str) -> float:
	match = ROW_RE.fullmatch(value.strip())
	if not match:
		return float("nan")
	rows = float(match.group(1))
	suffix = match.group(2)
	if suffix == "K":
		rows *= 1_000.0
	elif suffix == "M":
		rows *= 1_000_000.0
	elif suffix == "B":
		rows *= 1_000_000_000.0
	return rows


def format_rows(rows: float) -> str:
	if rows >= 1_000_000_000:
		return f"{rows / 1_000_000_000:.1f}B"
	if rows >= 1_000_000:
		return f"{rows / 1_000_000:.1f}M"
	if rows >= 1_000:
		return f"{rows / 1_000:.1f}K"
	return f"{rows:.0f}"


def scan_lines(lines: Iterable[str], max_cartesian_work_rows: float) -> list[PlanRisk]:
	detector = PlanRiskDetector(max_cartesian_work_rows)
	risks: list[PlanRisk] = []
	for line in lines:
		risks.extend(detector.feed(line))
	return risks


def stream_stdin(max_cartesian_work_rows: float) -> int:
	detector = PlanRiskDetector(max_cartesian_work_rows)
	for line in sys.stdin:
		sys.stdout.write(line)
		sys.stdout.flush()
		risks = detector.feed(line)
		if risks:
			report_risks(risks)
			return EXIT_PLAN_RISK
	return 0


def report_risks(risks: list[PlanRisk]) -> None:
	print("Error: benchmark plan risk detected before timed execution.", file=sys.stderr)
	for risk in risks:
		print(f"- {risk.kind}: {risk.message}", file=sys.stderr)
		print(risk.line, file=sys.stderr)


def max_cartesian_from_env() -> float:
	raw = os.environ.get("RDF4J_BENCHMARK_PLAN_GUARD_MAX_CARTESIAN_WORK_ROWS")
	if raw is None or raw.strip() == "":
		return DEFAULT_MAX_CARTESIAN_WORK_ROWS
	return parse_rows(raw)


def self_test() -> int:
	dangerous_cartesian = [
		"Join (plannedCostCartesianWorkRows=1206.1M, plannedCostWorkRows=1206.2M)\n"
	]
	dangerous_direct_lookup = [
		"StatementPattern (plannedIndexAccessMode=directLookup, plannedLookupComponents=[S, P, O])\n",
		"  s: Var (name=rel) (bindingState=unbound)\n",
	]
	bounded_cartesian = [
		"Join (plannedCostCartesianWorkRows=25.0K, plannedCostWorkRows=30.0K)\n"
	]
	bound_direct_lookup = [
		"StatementPattern (plannedIndexAccessMode=directLookup, plannedLookupComponents=[S, P, O])\n",
		"  s: Var (name=rel) (bindingState=bound)\n",
	]

	checks = [
		("dangerous cartesian", dangerous_cartesian, True),
		("dangerous direct lookup", dangerous_direct_lookup, True),
		("bounded cartesian", bounded_cartesian, False),
		("bound direct lookup", bound_direct_lookup, False),
	]
	for name, sample, expected_risk in checks:
		risks = scan_lines(sample, DEFAULT_MAX_CARTESIAN_WORK_ROWS)
		if bool(risks) != expected_risk:
			print(f"self-test failed for {name}: risks={risks}", file=sys.stderr)
			return 2
	print("query-plan-risk-guard self-test passed")
	return 0


def main(argv: list[str]) -> int:
	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument("--self-test", action="store_true")
	args = parser.parse_args(argv)
	if args.self_test:
		return self_test()
	return stream_stdin(max_cartesian_from_env())


if __name__ == "__main__":
	sys.exit(main(sys.argv[1:]))
