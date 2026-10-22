#!/usr/bin/env python3
"""Run tiny Maven fixtures and prove the PR test partitions are disjoint."""

import argparse
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
	("unit", "build"),
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


def maven_test_selection(job_id):
	workflow = ROOT / ".github/workflows/pr-verify.yml"
	lines = workflow.read_text(encoding="utf-8").splitlines()
	start = next((i for i, line in enumerate(lines) if line == f"  {job_id}:"), None)
	if start is None:
		raise RuntimeError(f"missing workflow job {job_id}")
	end = next(
		(i for i in range(start + 1, len(lines)) if re.match(r"^  [A-Za-z0-9_-]+:$", lines[i])),
		len(lines),
	)
	commands = re.findall(r"^\s*run:\s*(.*)$", "\n".join(lines[start:end]), flags=re.MULTILINE)
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
			index += 1
		selections.append((goal, ",".join(profiles), system_properties))
	if len(selections) != 1:
		raise RuntimeError(
			f"expected one Maven test command in workflow job {job_id}, found {selections}"
		)
	return selections[0]


def property_name(argument):
	return argument[2:].split("=", maxsplit=1)[0]


def run_selection(name, goal, profiles, system_properties, offline):
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
		]
	)
	if profiles:
		command.append(f"-P{profiles}")
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
	if result.returncode:
		raise RuntimeError(f"{name} Maven selection failed; see {log_file}")
	return collect_cases()


def collect_cases():
	cases = set()
	for module in MODULES:
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
	selections = (
		(name, *maven_test_selection(job_id)) for name, job_id in SELECTION_JOBS
	)
	actual = {}
	prepare_fixture()
	for name, goal, profiles, system_properties in selections:
		actual[name] = run_selection(name, goal, profiles, system_properties, arguments.offline)
		if actual[name] != EXPECTED[name]:
			missing = sorted(EXPECTED[name] - actual[name])
			unexpected = sorted(actual[name] - EXPECTED[name])
			raise AssertionError(
				f"{name} selection mismatch; missing={missing}, unexpected={unexpected}"
			)
		print(f"{name}: {len(actual[name])} expected tests executed")

	for left, right in (("unit", "integration"), ("unit", "slow"), ("integration", "slow")):
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
