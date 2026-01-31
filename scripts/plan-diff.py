#!/usr/bin/env python3
"""Run and/or diff learned join plans from plan-evolution logs."""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Dict, List, Tuple

RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"

JAVA_TEMPLATE = """import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.benchmark.common.ThemeQueryCatalog;
import org.eclipse.rdf4j.benchmark.rio.util.ThemeDataSetGenerator;
import org.eclipse.rdf4j.benchmark.rio.util.ThemeDataSetGenerator.Theme;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.LearningEvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.JoinStatsProvider;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.MemoryJoinStats;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.learned.LearnedJoinConfig;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;

public class PlanEvolution {
	private static final String THEMES_PROP = "plan.themes";
	private static final String QUERY_INDEXES_PROP = "plan.queryIndexes";
	private static final String ITERATIONS_PROP = "plan.iterations";

	public static void main(String[] args) throws Exception {
		List<String> themes = splitValues(System.getProperty(THEMES_PROP, "ELECTRICAL_GRID,PHARMA"));
		List<Integer> queryIndexes = splitIndexes(System.getProperty(QUERY_INDEXES_PROP, "4,6"));
		int iterations = Integer.getInteger(ITERATIONS_PROP, 5);
		for (String themeName : themes) {
			for (int queryIndex : queryIndexes) {
				runScenario(themeName, queryIndex, false, iterations);
				runScenario(themeName, queryIndex, true, iterations);
			}
		}
	}

	private static void runScenario(String themeName, int queryIndex, boolean dpEnabled, int iterations)
			throws Exception {
		System.setProperty(LearnedJoinConfig.DP_ENABLED_PROPERTY, Boolean.toString(dpEnabled));
		JoinStatsProvider statsProvider = new MemoryJoinStats();
		LearnedJoinConfig joinConfig = new LearnedJoinConfig();
		LearningEvaluationStrategyFactory factory = new LearningEvaluationStrategyFactory(statsProvider, null, joinConfig);

		File dataDir = Files.newTemporaryFolder();
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,ospc,psoc");
		config.setForceSync(false);
		config.setValueDBSize(1_073_741_824L);
		config.setTripleDBSize(config.getValueDBSize());
		LmdbStore store = new LmdbStore(dataDir, config);
		store.setEvaluationStrategyFactory(factory);

		SailRepository repository = new SailRepository(store);
		Theme theme = Theme.valueOf(themeName);
		String query = ThemeQueryCatalog.queryFor(theme, queryIndex);
		loadData(repository, theme);

		System.out.println("=== theme=" + themeName + " queryIndex=" + queryIndex + " dpEnabled=" + dpEnabled
				+ " iterations=" + iterations + " ===");
		for (int iteration = 0; iteration < iterations; iteration++) {
			try (SailRepositoryConnection connection = repository.getConnection()) {
				String explanation = connection
						.prepareTupleQuery(query)
						.explain(Explanation.Level.Executed)
						.toString();
				System.out.println("--- iteration=" + iteration + " ---");
				System.out.println(explanation);
			}
		}

		repository.shutDown();
		FileUtils.deleteDirectory(dataDir);
	}

	private static void loadData(SailRepository repository, Theme theme) throws IOException {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			RDFInserter inserter = new RDFInserter(connection);
			ThemeDataSetGenerator.generate(theme, inserter);
			connection.commit();
		}
	}

	private static List<String> splitValues(String value) {
		List<String> values = new ArrayList<>();
		for (String part : value.split(",")) {
			String trimmed = part.trim();
			if (!trimmed.isEmpty()) {
				values.add(trimmed);
			}
		}
		return values;
	}

	private static List<Integer> splitIndexes(String value) {
		return splitValues(value).stream()
				.map(Integer::parseInt)
				.collect(Collectors.toList());
	}
}
"""


def compact_uri(uri: str) -> str:
	if uri == RDF_TYPE:
		return "rdf:type"
	if "/theme/grid/" in uri:
		return "grid:" + uri.rsplit("/", 1)[-1]
	if "/theme/pharma/" in uri:
		return "pharma:" + uri.rsplit("/", 1)[-1]
	if "#" in uri:
		return uri.rsplit("#", 1)[-1]
	if "/" in uri:
		return uri.rsplit("/", 1)[-1]
	return uri


def parse_log(text: str) -> Dict[Tuple[str, int, str], Dict[int, str]]:
	sections = re.split(r"(?m)^=== theme=", text)
	data: Dict[Tuple[str, int, str], Dict[int, str]] = {}
	for sec in sections:
		if not sec.strip():
			continue
		header, *rest = sec.split("\n", 1)
		m = re.match(r"(\w+) queryIndex=(\d+) dpEnabled=(\w+) iterations=(\d+)", header.strip())
		if not m:
			continue
		theme, qidx, dp, _ = m.groups()
		body = rest[0] if rest else ""
		key = (theme, int(qidx), dp)
		data[key] = {}
		for m_it in re.finditer(r"(?m)^--- iteration=(\d+) ---", body):
			it = int(m_it.group(1))
			start = m_it.end()
			m_next = re.search(r"(?m)^--- iteration=\d+ ---", body[start:])
			end = start + (m_next.start() if m_next else len(body[start:]))
			data[key][it] = body[start:end]
	return data


def extract_patterns(block: str, max_patterns: int) -> List[str]:
	lines = block.splitlines()
	try:
		idx = next(i for i, line in enumerate(lines) if "LeftJoin" in line)
	except StopIteration:
		return []
	patterns: List[str] = []
	pending_actual = None
	for line in lines[idx + 1 :]:
		if "StatementPattern" in line:
			match = re.search(r"resultSizeActual=([^),]+)", line)
			pending_actual = match.group(1) if match else "?"
			continue
		if pending_actual is not None and "p: Var" in line:
			match = re.search(r"value=([^,]+)", line)
			uri = match.group(1) if match else line.strip()
			patterns.append(f"{compact_uri(uri)}@{pending_actual}")
			pending_actual = None
			if len(patterns) >= max_patterns:
				break
	return patterns


def diff_patterns(left: List[str], right: List[str]) -> str:
	max_len = max(len(left), len(right))
	diffs = []
	for i in range(max_len):
		lhs = left[i] if i < len(left) else "<none>"
		rhs = right[i] if i < len(right) else "<none>"
		if lhs != rhs:
			diffs.append(f"{i+1}:{lhs} != {rhs}")
	return "same" if not diffs else "; ".join(diffs)


def run_plan_evolution(
		log: Path,
		iterations: int,
		themes: str,
		query_indexes: str,
		classpath_file: Path,
		java_bin: str,
		javac_bin: str,
) -> None:
	classpath_file.parent.mkdir(parents=True, exist_ok=True)
	if not classpath_file.exists():
		cmd = [
			"mvn",
			"-o",
			"-Dmaven.repo.local=.m2_repo",
			"-pl",
			"core/sail/lmdb",
			"-DincludeScope=test",
			f"-Dmdep.outputFile={classpath_file}",
			"dependency:build-classpath",
		]
		subprocess.run(cmd, check=True)

	java_source = Path(tempfile.gettempdir()) / "PlanEvolution.java"
	java_source.write_text(JAVA_TEMPLATE)
	compile_cp = f"{classpath_file.read_text().strip()}:core/sail/lmdb/target/classes"
	subprocess.run([javac_bin, "-cp", compile_cp, str(java_source)], check=True)

	java_cp = f"{java_source.parent}:{classpath_file.read_text().strip()}:core/sail/lmdb/target/classes"
	java_cmd = [
		java_bin,
		f"-Dplan.iterations={iterations}",
		f"-Dplan.themes={themes}",
		f"-Dplan.queryIndexes={query_indexes}",
		"-cp",
		java_cp,
		"PlanEvolution",
	]

	log.parent.mkdir(parents=True, exist_ok=True)
	with log.open("w", encoding="utf-8") as handle:
		proc = subprocess.Popen(java_cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
		assert proc.stdout is not None
		for line in proc.stdout:
			handle.write(line)
			sys.stdout.write(line)
			sys.stdout.flush()
		ret = proc.wait()
		if ret != 0:
			raise subprocess.CalledProcessError(ret, java_cmd)

	try:
		java_source.unlink()
		(java_source.parent / "PlanEvolution.class").unlink()
	except FileNotFoundError:
		pass


def main() -> int:
	parser = argparse.ArgumentParser(description="Run/diff plans from plan-evolution logs")
	parser.add_argument("log", nargs="?", type=Path, help="path to plan-evolution log")
	parser.add_argument("--run", action="store_true", help="run plan evolution before diffing")
	parser.add_argument("--output", type=Path, default=Path("/tmp/plan-evolution.log"),
			help="output path for plan-evolution log")
	parser.add_argument("--iterations", type=int, default=5, help="iterations per query")
	parser.add_argument("--themes", default="ELECTRICAL_GRID,PHARMA", help="comma-separated themes")
	parser.add_argument("--query-indexes", default="4,6", help="comma-separated query indexes (0-based)")
	parser.add_argument("--classpath-file", type=Path, default=Path("/tmp/lmdb-test-cp.txt"),
			help="path to store Maven test classpath")
	parser.add_argument("--java", default=os.environ.get("JAVA_BIN", "java"), help="java binary")
	parser.add_argument("--javac", default=os.environ.get("JAVAC_BIN", "javac"), help="javac binary")
	parser.add_argument("--max-patterns", type=int, default=5, help="max patterns to show per iteration")
	args = parser.parse_args()

	log_path = args.log
	if args.run:
		log_path = args.output
		run_plan_evolution(
			log=log_path,
			iterations=args.iterations,
			themes=args.themes,
			query_indexes=args.query_indexes,
			classpath_file=args.classpath_file,
			java_bin=args.java,
			javac_bin=args.javac,
		)
	if log_path is None:
		raise SystemExit("log path required (or use --run)")

	text = log_path.read_text()
	data = parse_log(text)

	pairs = {}
	for (theme, qidx, dp), iters in data.items():
		pairs.setdefault((theme, qidx), {})[dp] = iters

	for (theme, qidx), dp_iters in sorted(pairs.items()):
		if "false" not in dp_iters or "true" not in dp_iters:
			continue
		false_iters = dp_iters["false"]
		true_iters = dp_iters["true"]
		all_iters = sorted(set(false_iters.keys()) | set(true_iters.keys()))
		print(f"=== {theme} #{qidx} ===")
		for it in all_iters:
			f_block = false_iters.get(it, "")
			t_block = true_iters.get(it, "")
			f_patterns = extract_patterns(f_block, args.max_patterns)
			t_patterns = extract_patterns(t_block, args.max_patterns)
			print(f"iter {it} | dp=false: " + " -> ".join(f_patterns))
			print(f"iter {it} | dp=true : " + " -> ".join(t_patterns))
			print(f"iter {it} | diff    : {diff_patterns(f_patterns, t_patterns)}")
		print()

	return 0


if __name__ == "__main__":
	raise SystemExit(main())
