#!/usr/bin/env python3
"""Fail fast when benchmark output exposes a clearly dangerous query plan."""

from __future__ import annotations

import argparse
import os
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


EXIT_PLAN_RISK = 42
DEFAULT_MAX_CARTESIAN_WORK_ROWS = 100_000_000.0

ROW_RE = re.compile(r"([0-9]+(?:\.[0-9]+)?)([KMB])?")
CARTESIAN_WORK_RE = re.compile(r"plannedCostCartesianWorkRows=([0-9]+(?:\.[0-9]+)?)([KMB])?")
SPO_DIRECT_LOOKUP_RE = re.compile(
		r"StatementPattern.*plannedIndexAccessMode=directLookup.*plannedLookupComponents=\[S, P, O\]")
STATEMENT_PATTERN_RE = re.compile(r"StatementPattern")
SUBJECT_RE = re.compile(r"\bs:\s+")
UNBOUND_SUBJECT_RE = re.compile(r"\bs:\s+Var .*bindingState=unbound")
UNBOUND_SUBJECT_NAME_RE = re.compile(r"\bs:\s+Var \(name=([^,\)]+).*bindingState=unbound")
PLANNED_BOUND_VARS_RE = re.compile(r"plannedBoundVars=([^)]*?)(?:, [A-Za-z][A-Za-z0-9.]*=|\))")


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
			if rows > self.max_cartesian_work_rows and not bounded_connected_work(line, self.max_cartesian_work_rows):
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
			if SUBJECT_RE.search(line):
				if UNBOUND_SUBJECT_RE.search(line) and not planned_bound_subject(
						self.pending_direct_spo_header, line):
					risks.append(PlanRisk(
						"unbound-spo-direct-lookup",
						"direct [S, P, O] lookup is planned while the subject binding is still unbound",
						self.pending_direct_spo_header.rstrip("\n") + "\n" + line.rstrip("\n"),
					))
				self.pending_direct_spo_header = None
				self.pending_direct_spo_lines = 0
			elif STATEMENT_PATTERN_RE.search(line) or self.pending_direct_spo_lines <= 0:
				self.pending_direct_spo_header = None
				self.pending_direct_spo_lines = 0
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


def metric_rows(line: str, metric_name: str) -> float:
	match = re.search(rf"{re.escape(metric_name)}=([0-9]+(?:\.[0-9]+)?)([KMB])?", line)
	if not match:
		return float("nan")
	return parse_rows(match.group(0).split("=", 1)[1])


def bounded_connected_work(line: str, max_work_rows: float) -> bool:
	connected_components = metric_rows(line, "optimizer.connectedComponentCount")
	planned_cost_work = metric_rows(line, "plannedCostWorkRows")
	planned_work = metric_rows(line, "plannedWorkRows")
	return (
		connected_components == 1.0
		and is_finite(planned_cost_work)
		and planned_cost_work <= max_work_rows
		and (not is_finite(planned_work) or planned_work <= max_work_rows)
	)


def planned_bound_subject(header: str, subject_line: str) -> bool:
	subject_match = UNBOUND_SUBJECT_NAME_RE.search(subject_line)
	bound_vars_match = PLANNED_BOUND_VARS_RE.search(header)
	if not subject_match or not bound_vars_match:
		return False
	subject_name = subject_match.group(1).strip()
	bound_vars = {name.strip() for name in bound_vars_match.group(1).split(",")}
	return subject_name in bound_vars


def is_finite(value: float) -> bool:
	return value == value and value not in (float("inf"), float("-inf"))


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


def compact_evidence_line(line: str) -> bool:
	stripped = line.strip()
	if not stripped:
		return False
	if stripped.startswith("#") or stripped.startswith("Result"):
		return True
	if "Benchmark" in stripped:
		return True
	return any(unit in stripped for unit in (" ops/", " ns/op", " us/op", " ms/op", " s/op", " B/op"))


def stream_stdin(max_cartesian_work_rows: float, compact: bool, log_path: str | None) -> int:
	detector = PlanRiskDetector(max_cartesian_work_rows)
	if log_path:
		Path(log_path).parent.mkdir(parents=True, exist_ok=True)
	log_file = open(log_path, "w", encoding="utf-8", errors="replace") if log_path else None
	try:
		for line in sys.stdin:
			if log_file is not None:
				log_file.write(line)
			if not compact or compact_evidence_line(line):
				sys.stdout.write(line)
				sys.stdout.flush()
			risks = detector.feed(line)
			if risks:
				report_risks(risks)
				return EXIT_PLAN_RISK
	finally:
		if log_file is not None:
			log_file.close()
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
	connected_filter_diagnostic_cartesian = [
		"Filter (plannedCostCartesianWorkRows=146130.4M, plannedCostWorkRows=13.1K, "
		"plannedWorkRows=13.1K, optimizer.connectedComponentCount=1.00)\n"
	]
	bound_direct_lookup = [
		"StatementPattern (plannedIndexAccessMode=directLookup, plannedLookupComponents=[S, P, O])\n",
		"  s: Var (name=rel) (bindingState=bound)\n",
	]
	bound_direct_lookup_with_unbound_sibling = [
		"StatementPattern (plannedIndexAccessMode=directLookup, plannedLookupComponents=[S, P, O]) [right]\n",
		"  s: Var (name=line) (bindingState=bound)\n",
		"  p: Var (name=_const_type) (bindingState=bound)\n",
		"  o: Var (name=_const_line) (bindingState=bound)\n",
		"StatementPattern (plannedIndexAccessMode=directLookup, plannedLookupComponents=[P, O]) [right]\n",
		"  s: Var (name=section) (bindingState=unbound)\n",
	]
	connected_dp_direct_lookup_with_planned_bound_subject = [
		"StatementPattern (plannedIndexAccessMode=directLookup, plannedLookupComponents=[S, P, O], "
		"plannedBoundVars=a,optName, plannerId=lmdb-cascades)\n",
		"  s: Var (name=a) (bindingState=unbound)\n",
	]

	checks = [
		("dangerous cartesian", dangerous_cartesian, True),
		("dangerous direct lookup", dangerous_direct_lookup, True),
		("bounded cartesian", bounded_cartesian, False),
		("connected filter diagnostic cartesian", connected_filter_diagnostic_cartesian, False),
		("bound direct lookup", bound_direct_lookup, False),
		("bound direct lookup with unbound sibling", bound_direct_lookup_with_unbound_sibling, False),
		("connected dp direct lookup with planned bound subject",
			connected_dp_direct_lookup_with_planned_bound_subject, False),
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
	parser.add_argument("--compact", action="store_true", help="Print only JMH evidence and risk lines to stdout.")
	parser.add_argument("--log", help="Write the full benchmark stream to this file.")
	args = parser.parse_args(argv)
	if args.self_test:
		return self_test()
	return stream_stdin(max_cartesian_from_env(), args.compact, args.log)


if __name__ == "__main__":
	sys.exit(main(sys.argv[1:]))
