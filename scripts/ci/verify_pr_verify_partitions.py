#!/usr/bin/env python3
"""Run tiny Maven fixtures and prove the PR test partitions are disjoint."""

import argparse
import itertools
import os
import re
import shlex
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
FIXTURE_SOURCE = ROOT / "scripts/ci/tests"
FIXTURE_DIRECTORY = ROOT / "logs/ci/pr-verify-partitions/fixture"
FIXTURE_POM = "logs/ci/pr-verify-partitions/fixture/pom.xml"
MODULES = ("standard", "native", "compliance")
SELECTION_JOBS = (
	("unit", "unit-tests"),
	("lmdb-unit", "lmdb-unit-tests"),
	("integration", "integration-tests"),
	("slow", "slow-tests"),
)
MAVEN_NS = "{http://maven.apache.org/POM/4.0.0}"
COMMON_FLAGS = (
	"-Denforcer.skip=true",
	"-Danimal.sniffer.skip=true",
	"-Dformatter.skip=true",
	"-Dimpsort.skip=true",
	"-Djapicmp.skip=true",
	"-Dmaven.javadoc.skip=true",
)

EXPECTED = {
	"unit": {
		"standard:unitRegular",
		"standard:unitRegularSubclassOfMethodSlowBase",
	},
	"lmdb-unit": {
		"native:nativeUnitRegular",
	},
	"integration": {
		"standard:integrationRegular",
		"native:nativeIntegrationRegular",
		"compliance:complianceRegular",
		"compliance:complianceIntegrationRegular",
	},
	"slow": {
		"standard:unitMethodSlow",
		"standard:unitClassSlow",
		"standard:unitInheritedClassSlow",
		"standard:unitSubclassOfClassSlowBase",
		"standard:unitInheritedMethodSlow",
		"standard:integrationMethodSlow",
		"standard:integrationClassSlow",
		"native:nativeUnitMethodSlow",
		"native:nativeUnitClassSlow",
		"native:nativeIntegrationMethodSlow",
		"native:nativeIntegrationClassSlow",
		"compliance:complianceMethodSlow",
		"compliance:complianceClassSlow",
		"compliance:complianceIntegrationMethodSlow",
		"compliance:complianceIntegrationClassSlow",
	},
}


def prepare_fixture():
	root_pom = ET.parse(ROOT / "pom.xml").getroot()
	version = root_pom.findtext(f"{MAVEN_NS}version")
	if not version:
		raise RuntimeError("could not read the root project version")
	FIXTURE_DIRECTORY.parent.mkdir(parents=True, exist_ok=True)
	shutil.copytree(
		FIXTURE_SOURCE,
		FIXTURE_DIRECTORY,
		dirs_exist_ok=True,
		ignore=shutil.ignore_patterns("target"),
	)
	for pom_path in sorted(FIXTURE_DIRECTORY.rglob("pom.xml")):
		content = pom_path.read_text(encoding="utf-8")
		if "@RDF4J_VERSION@" not in content:
			raise RuntimeError(f"missing project-version placeholder in {pom_path}")
		content = content.replace("@RDF4J_VERSION@", version)
		if pom_path == FIXTURE_DIRECTORY / "pom.xml":
			old_relative_path = "<relativePath>../../../pom.xml</relativePath>"
			new_relative_path = "<relativePath>../../../../pom.xml</relativePath>"
			if old_relative_path not in content:
				raise RuntimeError(f"missing root relativePath in {pom_path}")
			content = content.replace(old_relative_path, new_relative_path, 1)
		pom_path.write_text(content, encoding="utf-8")


def workflow_job(job_id):
	workflow = ROOT / ".github/workflows/pr-verify.yml"
	lines = workflow.read_text(encoding="utf-8").splitlines()
	start = next((i for i, line in enumerate(lines) if line == f"  {job_id}:"), None)
	if start is None:
		raise RuntimeError(f"missing workflow job {job_id}")
	end = next(
		(i for i in range(start + 1, len(lines)) if re.match(r"^  [A-Za-z0-9_-]+:$", lines[i])),
		len(lines),
	)
	return lines[start:end]


def maven_test_selection(job_id):
	commands = []
	step_name = None
	for line in workflow_job(job_id):
		step = re.match(r"^\s*- name:\s*(.*)$", line)
		if step:
			step_name = step.group(1)
		command = re.match(r"^\s*run:\s*(.*)$", line)
		if command and step_name in ("Test", "Verify"):
			commands.append(command.group(1))
	selections = []
	for command in commands:
		tokens = shlex.split(command)
		try:
			maven_index = tokens.index("mvn")
		except ValueError:
			continue
		arguments = tokens[maven_index + 1 :]
		goal = next((token for token in arguments if token in ("test", "verify")), None)
		if goal is None:
			continue
		profiles = []
		system_properties = []
		project_selection = []
		index = 0
		while index < len(arguments):
			token = arguments[index]
			if token == "-P" and index + 1 < len(arguments):
				profiles.extend(arguments[index + 1].split(","))
				index += 1
			elif token.startswith("-P"):
				profiles.extend(token[2:].split(","))
			elif token == "-D" and index + 1 < len(arguments):
				system_properties.append(f"-D{arguments[index + 1]}")
				index += 1
			elif token.startswith("-D"):
				system_properties.append(token)
			elif token == "-pl" and index + 1 < len(arguments):
				for project in arguments[index + 1].split(","):
					excluded = project.startswith("!")
					path = project[1:] if excluded else project
					if path != "core/sail/lmdb":
						raise RuntimeError(f"unmapped workflow project selection: {project}")
					project_selection.append(("!" if excluded else "") + "native")
				index += 1
			index += 1
		selections.append((goal, ",".join(profiles), system_properties, project_selection))
	if len(selections) != 1:
		raise RuntimeError(
			f"expected one Maven test command in workflow job {job_id}, found {selections}"
		)
	return selections[0]


def verify_unit_gate():
	lines = workflow_job("build")
	job = "\n".join(lines)
	for required in (
		"needs: [unit-tests, lmdb-unit-tests]",
		"if: ${{ always() }}",
		"UNIT_RESULT: ${{ needs.unit-tests.result }}",
		"LMDB_RESULT: ${{ needs.lmdb-unit-tests.result }}",
	):
		if required not in job:
			raise AssertionError(f"build gate missing {required}")
	lmdb_job = "\n".join(workflow_job("lmdb-unit-tests"))
	if "-Dtest=LmdbSyncValidationProfileProbe" not in lmdb_job or "lmdb-sync-validation" not in lmdb_job:
		raise AssertionError("LMDB job must run the synchronous-validation profile probe")
	try:
		start = next(i for i, line in enumerate(lines) if line.strip() == "run: |") + 1
	except StopIteration as error:
		raise AssertionError("build gate has no shell command") from error
	command_lines = []
	for line in lines[start:]:
		if not line.startswith("          "):
			break
		command_lines.append(line[10:])
	command = "\n".join(command_lines)
	if not command:
		raise AssertionError("build gate has an empty shell command")
	for unit, lmdb in itertools.product(("success", "failure", "cancelled", "skipped"), repeat=2):
		result = subprocess.run(["bash", "-c", command], env={**os.environ, "UNIT_RESULT": unit,
				"LMDB_RESULT": lmdb}, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=False)
		if (result.returncode == 0) != (unit == lmdb == "success"):
			raise AssertionError(f"build gate returned {result.returncode} for unit={unit}, LMDB={lmdb}")
	print("build gate: both unit-test jobs must succeed")


def property_name(argument):
	return argument[2:].split("=", maxsplit=1)[0]


def run_selection(name, goal, profiles, system_properties, project_selection, offline):
	if os.environ.get("MAVEN_REPO_LOCAL"):
		maven_repository = os.environ["MAVEN_REPO_LOCAL"]
	elif os.environ.get("GITHUB_ACTIONS") == "true":
		maven_repository = str(Path.home() / ".m2/repository")
	else:
		maven_repository = ".m2_repo"
	command = [
		"mvn",
		"-B",
		"-ntp",
	]
	if offline:
		command.append("-o")
	command.extend(
		[
			f"-Dmaven.repo.local={maven_repository}",
			"-f",
			FIXTURE_POM,
			"clean",
			goal,
			"--fail-at-end",
		]
	)
	if profiles:
		command.append(f"-P{profiles}")
	if project_selection:
		command.extend(["-pl", ",".join(project_selection)])
	command.extend(system_properties)
	workflow_property_names = {property_name(argument) for argument in system_properties}
	command.extend(
		argument
		for argument in COMMON_FLAGS
		if property_name(argument) not in workflow_property_names
	)
	log_directory = ROOT / "logs/ci/pr-verify-partitions"
	log_directory.mkdir(parents=True, exist_ok=True)
	log_file = log_directory / f"{name}.log"
	with log_file.open("w", encoding="utf-8") as log:
		log.write(f"$ {shlex.join(command)}\n")
		log.flush()
		result = subprocess.run(
			command,
			cwd=ROOT,
			stdout=log,
			stderr=subprocess.STDOUT,
			check=False,
		)
	for module in selected_modules(project_selection):
		for report_directory in ("surefire-reports", "failsafe-reports"):
			source = FIXTURE_DIRECTORY / module / "target" / report_directory
			if source.is_dir():
				shutil.copytree(source, log_directory / f"{name}-reports" / module / report_directory,
								dirs_exist_ok=True)
	if result.returncode:
		raise RuntimeError(f"{name} Maven selection failed; see {log_file}")
	return collect_cases(project_selection)


def selected_modules(project_selection):
	included = {project for project in project_selection if not project.startswith("!")}
	excluded = {project[1:] for project in project_selection if project.startswith("!")}
	return (included or set(MODULES)) - excluded


def collect_cases(project_selection):
	cases = set()
	selected = selected_modules(project_selection)
	for module in MODULES:
		if module not in selected:
			continue
		module_root = FIXTURE_DIRECTORY / module
		for report_directory in ("surefire-reports", "failsafe-reports"):
			report_root = module_root / "target" / report_directory
			for report_path in sorted(report_root.glob("TEST-*.xml")):
				suite = ET.parse(report_path).getroot()
				for testcase in suite.iter("testcase"):
					if testcase.find("skipped") is not None:
						continue
					method = testcase.attrib["name"].split("(", maxsplit=1)[0]
					case = f"{module}:{method}"
					if case in cases:
						raise AssertionError(
							f"test executed more than once in one selection: {case}"
						)
					cases.add(case)
	return cases


def main():
	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument(
		"--no-offline",
		action="store_false",
		dest="offline",
		help="allow Maven to download dependencies (for cold-cache CI runs)",
	)
	arguments = parser.parse_args()
	verify_unit_gate()
	selections = (
		(name, *maven_test_selection(job_id)) for name, job_id in SELECTION_JOBS
	)
	actual = {}
	prepare_fixture()
	for name, goal, profiles, system_properties, project_selection in selections:
		actual[name] = run_selection(name, goal, profiles, system_properties, project_selection, arguments.offline)
		if actual[name] != EXPECTED[name]:
			missing = sorted(EXPECTED[name] - actual[name])
			unexpected = sorted(actual[name] - EXPECTED[name])
			raise AssertionError(
				f"{name} selection mismatch; missing={missing}, unexpected={unexpected}"
			)
		print(f"{name}: {len(actual[name])} expected tests executed")

	for left, right in itertools.combinations(actual, 2):
		shared = actual[left] & actual[right]
		if shared:
			raise AssertionError(f"{left} and {right} overlap: {sorted(shared)}")

	all_expected = set().union(*EXPECTED.values())
	all_actual = set().union(*actual.values())
	if all_actual != all_expected:
		raise AssertionError(
			f"partition union mismatch; missing={sorted(all_expected - all_actual)}, "
			f"unexpected={sorted(all_actual - all_expected)}"
		)
	print(f"partition union: {len(all_actual)} unique tests; pairwise intersections are empty")


if __name__ == "__main__":
	try:
		main()
	except (AssertionError, RuntimeError) as error:
		print(error, file=sys.stderr)
		sys.exit(1)
