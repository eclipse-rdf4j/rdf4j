#!/usr/bin/env python3

from __future__ import annotations

import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
MAVEN_NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}


def text(element: ET.Element, path: str) -> str | None:
    child = element.find(path, MAVEN_NAMESPACE)
    return child.text.strip() if child is not None and child.text else None


def plugin(element: ET.Element, path: str, artifact_id: str) -> ET.Element:
    match = next(
        (
            candidate
            for candidate in element.findall(path, MAVEN_NAMESPACE)
            if text(candidate, "m:artifactId") == artifact_id
        ),
        None,
    )
    if match is None:
        raise AssertionError(f"missing Maven plugin {artifact_id}")
    return match


class MavenWorkspacePomTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls) -> None:
        cls.root = ET.parse(REPOSITORY_ROOT / "pom.xml").getroot()
        cls.assembly = ET.parse(REPOSITORY_ROOT / "assembly" / "pom.xml").getroot()
        cls.workbench = ET.parse(REPOSITORY_ROOT / "tools" / "workbench" / "pom.xml").getroot()
        cls.sdk = ET.parse(
            REPOSITORY_ROOT / "assembly" / "src" / "main" / "assembly" / "sdk.xml"
        ).getroot()

    def workspace_profile(self) -> ET.Element:
        profile = next(
            (
                candidate
                for candidate in self.root.findall("m:profiles/m:profile", MAVEN_NAMESPACE)
                if text(candidate, "m:id") == "workspace-build-root"
            ),
            None,
        )
        self.assertIsNotNone(profile, "root pom.xml must declare workspace-build-root")
        assert profile is not None
        return profile

    def test_workspace_profile_routes_full_gav_outputs(self) -> None:
        profile = self.workspace_profile()
        self.assertEqual(text(profile, "m:activation/m:property/m:name"), "rdf4j.build.root")
        self.assertIsNone(text(profile, "m:activation/m:activeByDefault"))
        self.assertIsNone(text(self.root, "m:build/m:directory"))
        self.assertEqual(
            text(profile, "m:build/m:directory"),
            "${rdf4j.build.root}/${project.groupId}/${project.artifactId}/${project.version}",
        )
        self.assertEqual(
            text(self.root, "m:properties/m:rdf4j.test.outputDirectory"),
            "${project.build.directory}",
        )
        self.assertEqual(
            text(self.root, "m:properties/m:rdf4j.test.tmpDirectory"),
            "${project.build.directory}/tmp",
        )
        self.assertEqual(
            text(profile, "m:properties/m:rdf4j.test.tmpDirectory"),
            "${rdf4j.test.tmpRoot}/${project.groupId}/${project.artifactId}/${project.version}",
        )

    def test_workspace_profile_isolates_agent_and_plugin_writes(self) -> None:
        profile = self.workspace_profile()
        self.assertEqual(
            text(profile, "m:properties/m:mockito.javaagent"),
            "-javaagent:${project.build.directory}/test-agents/mockito-core-${mockito.version}.jar",
        )
        self.assertEqual(
            text(
                plugin(profile, "m:build/m:plugins/m:plugin", "maven-bundle-plugin"),
                "m:configuration/m:obrRepository",
            ),
            "NONE",
        )
        dependency = plugin(profile, "m:build/m:plugins/m:plugin", "maven-dependency-plugin")
        execution = next(
            (
                candidate
                for candidate in dependency.findall("m:executions/m:execution", MAVEN_NAMESPACE)
                if text(candidate, "m:id") == "stage-workspace-mockito-javaagent"
            ),
            None,
        )
        self.assertIsNotNone(execution)
        assert execution is not None
        self.assertEqual(text(execution, "m:phase"), "process-test-classes")
        self.assertEqual(text(execution, "m:configuration/m:skip"), "${skipTests}")
        self.assertEqual(
            text(execution, "m:configuration/m:outputDirectory"),
            "${project.build.directory}/test-agents",
        )

    def test_test_plugins_route_reports_and_temporary_files(self) -> None:
        profile = self.workspace_profile()
        for artifact_id in ("maven-surefire-plugin", "maven-failsafe-plugin"):
            with self.subTest(plugin=artifact_id):
                managed = plugin(
                    self.root,
                    "m:build/m:pluginManagement/m:plugins/m:plugin",
                    artifact_id,
                )
                workspace = plugin(profile, "m:build/m:plugins/m:plugin", artifact_id)
                self.assertEqual(
                    text(
                        managed,
                        "m:configuration/m:systemPropertyVariables/m:rdf4j.test.outputDirectory",
                    ),
                    "${project.build.directory}",
                )
                self.assertEqual(
                    text(
                        managed,
                        "m:configuration/m:systemPropertyVariables/m:rdf4j.test.tmpDirectory",
                    ),
                    "${rdf4j.test.tmpDirectory}",
                )
                self.assertIsNone(
                    text(managed, "m:configuration/m:systemPropertyVariables/m:java.io.tmpdir")
                )
                self.assertEqual(
                    text(workspace, "m:configuration/m:systemPropertyVariables/m:java.io.tmpdir"),
                    "${rdf4j.test.tmpDirectory}",
                )

    def test_parallel_shade_state_is_unique_and_ignored(self) -> None:
        shade = plugin(
            self.root,
            "m:build/m:pluginManagement/m:plugins/m:plugin",
            "maven-shade-plugin",
        )
        self.assertEqual(
            text(shade, "m:configuration/m:generateUniqueDependencyReducedPom"), "true"
        )
        ignored = (REPOSITORY_ROOT / ".gitignore").read_text(encoding="utf-8").splitlines()
        self.assertIn("dependency-reduced-pom-*.xml", ignored)
        formatting = next(
            candidate
            for candidate in self.root.findall("m:profiles/m:profile", MAVEN_NAMESPACE)
            if text(candidate, "m:id") == "formatting"
        )
        spotless = plugin(formatting, "m:build/m:plugins/m:plugin", "spotless-maven-plugin")
        excludes = {
            exclude.text.strip()
            for exclude in spotless.findall(
                "m:configuration/m:formats/m:format/m:excludes/m:exclude", MAVEN_NAMESPACE
            )
            if exclude.text
        }
        self.assertIn("**/dependency-reduced-pom-*.xml", excludes)

    def test_cross_module_paths_follow_workspace_producers(self) -> None:
        profile = self.workspace_profile()
        expected = {
            "rdf4j.apidocs.directory": (
                "${maven.multiModuleProjectDirectory}/target/site/apidocs",
                "${rdf4j.build.root}/org.eclipse.rdf4j/rdf4j/${project.version}/site/apidocs",
            ),
            "rdf4j.server.war": (
                "${maven.multiModuleProjectDirectory}/tools/server/target/rdf4j-server.war",
                "${rdf4j.build.root}/org.eclipse.rdf4j/rdf4j-http-server/${project.version}/rdf4j-server.war",
            ),
            "rdf4j.workbench.war": (
                "${maven.multiModuleProjectDirectory}/tools/workbench/target/rdf4j-workbench.war",
                "${rdf4j.build.root}/org.eclipse.rdf4j/rdf4j-http-workbench/${project.version}/rdf4j-workbench.war",
            ),
        }
        for name, (normal, workspace) in expected.items():
            with self.subTest(property=name):
                self.assertEqual(text(self.root, f"m:properties/m:{name}"), normal)
                self.assertEqual(text(profile, f"m:properties/m:{name}"), workspace)

        assembly_shade = plugin(
            self.assembly, "m:build/m:plugins/m:plugin", "maven-shade-plugin"
        )
        self.assertEqual(
            text(assembly_shade, "m:configuration/m:outputFile"),
            "${project.build.directory}/eclipse-rdf4j-${project.version}-onejar.jar",
        )
        workbench_jetty = plugin(
            self.workbench, "m:build/m:plugins/m:plugin", "jetty-ee11-maven-plugin"
        )
        self.assertEqual(
            text(
                workbench_jetty,
                "m:configuration/m:contextHandlers/m:contextHandler/m:war",
            ),
            "${rdf4j.server.war}",
        )
        sdk_directories = [
            item.text.strip()
            for item in self.sdk.findall("fileSets/fileSet/directory")
            if item.text
        ]
        sdk_sources = [
            item.text.strip() for item in self.sdk.findall("files/file/source") if item.text
        ]
        self.assertEqual(sdk_directories, ["..", "${rdf4j.apidocs.directory}"])
        self.assertEqual(
            sdk_sources[:2], ["${rdf4j.server.war}", "${rdf4j.workbench.war}"]
        )

    def test_workspace_lifecycle_runner_is_available(self) -> None:
        self.assertTrue((REPOSITORY_ROOT / "scripts" / "mvn-agent.py").is_file())


if __name__ == "__main__":
    unittest.main()
