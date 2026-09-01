#!/usr/bin/env python3

from __future__ import annotations

import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
POM = REPOSITORY_ROOT / "pom.xml"
ASSEMBLY_POM = REPOSITORY_ROOT / "assembly" / "pom.xml"
WORKBENCH_POM = REPOSITORY_ROOT / "tools" / "workbench" / "pom.xml"
SDK_DESCRIPTOR = REPOSITORY_ROOT / "assembly" / "src" / "main" / "assembly" / "sdk.xml"
MAVEN_NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}
PROPERTY_PATTERN = re.compile(r"\$\{([^}]+)}")


def _text(element: ET.Element, path: str) -> str | None:
    child = element.find(path, MAVEN_NAMESPACE)
    return child.text.strip() if child is not None and child.text else None


def _expand(template: str, properties: dict[str, str]) -> str:
    def replace(match: re.Match[str]) -> str:
        name = match.group(1)
        if name not in properties:
            raise AssertionError(f"unresolved Maven model property: {name}")
        return properties[name]

    return PROPERTY_PATTERN.sub(replace, template)


class MavenWorkspacePomModelTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls) -> None:
        cls.project = ET.parse(POM).getroot()
        cls.assembly_project = ET.parse(ASSEMBLY_POM).getroot()
        cls.workbench_project = ET.parse(WORKBENCH_POM).getroot()
        cls.sdk_descriptor = ET.parse(SDK_DESCRIPTOR).getroot()

    def _workspace_profile(self) -> ET.Element:
        for profile in self.project.findall("m:profiles/m:profile", MAVEN_NAMESPACE):
            if _text(profile, "m:id") == "workspace-build-root":
                return profile
        self.fail("root pom.xml must declare the workspace-build-root profile")

    def test_workspace_build_root_profile_is_inactive_without_user_property(self) -> None:
        profile = self._workspace_profile()

        self.assertEqual(
            _text(profile, "m:activation/m:property/m:name"),
            "rdf4j.build.root",
            "workspace output redirection must require the explicit rdf4j.build.root user property",
        )
        self.assertIsNone(
            _text(profile, "m:activation/m:property/m:value"),
            "any supplied rdf4j.build.root path should activate the profile",
        )
        self.assertIsNone(
            _text(profile, "m:activation/m:activeByDefault"),
            "ordinary Maven builds must not activate workspace output redirection",
        )
        self.assertIsNone(
            _text(self.project, "m:build/m:directory"),
            "the base model must retain Maven's module-local target default",
        )

    def test_normal_and_workspace_models_resolve_disjoint_build_and_test_paths(self) -> None:
        profile = self._workspace_profile()
        build_template = _text(profile, "m:build/m:directory")
        self.assertEqual(
            build_template,
            "${rdf4j.build.root}/${project.groupId}/${project.artifactId}/${project.version}",
        )
        test_output_template = _text(self.project, "m:properties/m:rdf4j.test.outputDirectory")
        self.assertEqual(test_output_template, "${project.build.directory}")

        module_directory = REPOSITORY_ROOT / "core" / "queryalgebra" / "evaluation"
        normal_build = module_directory / "target"
        workspace_root = REPOSITORY_ROOT / ".mvnf" / "workspaces" / "model-test" / "build"
        workspace_build = Path(
            _expand(
                build_template,
                {
                    "rdf4j.build.root": str(workspace_root),
                    "project.groupId": "org.eclipse.rdf4j",
                    "project.artifactId": "rdf4j-queryalgebra-evaluation",
                    "project.version": "6.1.0-SNAPSHOT",
                },
            )
        )
        normal_test_output = Path(
            _expand(test_output_template, {"project.build.directory": str(normal_build)})
        )
        workspace_test_output = Path(
            _expand(test_output_template, {"project.build.directory": str(workspace_build)})
        )

        self.assertEqual(normal_build, module_directory / "target")
        self.assertEqual(normal_test_output, normal_build)
        self.assertEqual(
            workspace_build,
            workspace_root
            / "org.eclipse.rdf4j"
            / "rdf4j-queryalgebra-evaluation"
            / "6.1.0-SNAPSHOT",
        )
        self.assertEqual(workspace_test_output, workspace_build)
        self.assertFalse(workspace_build.is_relative_to(module_directory))

    def test_workspace_stages_mockito_javaagent_under_isolated_build_directory(self) -> None:
        profile = self._workspace_profile()
        expected_agent = (
            "-javaagent:${project.build.directory}/test-agents/"
            "mockito-core-${mockito.version}.jar"
        )

        with self.subTest(contract="workspace agent path"):
            self.assertEqual(
                _text(profile, "m:properties/m:mockito.javaagent"),
                expected_agent,
                "workspace tests must not address either repository layer directly",
            )
        with self.subTest(contract="normal build compatibility"):
            self.assertEqual(
                _text(self.project, "m:properties/m:mockito.javaagent"),
                "-javaagent:${settings.localRepository}/org/mockito/mockito-core/"
                "${mockito.version}/mockito-core-${mockito.version}.jar",
            )
        with self.subTest(contract="quick-build skip default"):
            self.assertEqual(_text(self.project, "m:properties/m:skipTests"), "false")
            quick_profile = next(
                candidate
                for candidate in self.project.findall("m:profiles/m:profile", MAVEN_NAMESPACE)
                if _text(candidate, "m:id") == "quick"
            )
            self.assertEqual(_text(quick_profile, "m:properties/m:skipTests"), "true")

        dependency_plugins = [
            plugin
            for plugin in profile.findall("m:build/m:plugins/m:plugin", MAVEN_NAMESPACE)
            if _text(plugin, "m:artifactId") == "maven-dependency-plugin"
        ]
        with self.subTest(contract="workspace resolver staging"):
            self.assertEqual(
                len(dependency_plugins),
                1,
                "the workspace profile must resolve and stage the agent through Maven",
            )
        if len(dependency_plugins) != 1:
            return

        plugin = dependency_plugins[0]
        execution = next(
            (
                candidate
                for candidate in plugin.findall("m:executions/m:execution", MAVEN_NAMESPACE)
                if _text(candidate, "m:id") == "stage-workspace-mockito-javaagent"
            ),
            None,
        )
        self.assertIsNotNone(execution)
        if execution is None:
            return

        self.assertIsNone(_text(plugin, "m:version"), "reuse the root's managed plugin version")
        self.assertNotEqual(_text(plugin, "m:inherited"), "false")
        self.assertEqual(_text(execution, "m:phase"), "process-test-classes")
        self.assertEqual(
            [goal.text.strip() for goal in execution.findall("m:goals/m:goal", MAVEN_NAMESPACE)],
            ["copy"],
        )
        configuration = execution.find("m:configuration", MAVEN_NAMESPACE)
        self.assertIsNotNone(configuration)
        if configuration is None:
            return
        self.assertEqual(_text(configuration, "m:skip"), "${skipTests}")
        self.assertEqual(
            _text(configuration, "m:outputDirectory"),
            "${project.build.directory}/test-agents",
        )
        artifact = configuration.find("m:artifactItems/m:artifactItem", MAVEN_NAMESPACE)
        self.assertIsNotNone(artifact)
        if artifact is None:
            return
        self.assertEqual(_text(artifact, "m:groupId"), "org.mockito")
        self.assertEqual(_text(artifact, "m:artifactId"), "mockito-core")
        self.assertEqual(_text(artifact, "m:version"), "${mockito.version}")
        self.assertEqual(_text(artifact, "m:type"), "jar")

    def test_workspace_disables_legacy_local_obr_index_writes(self) -> None:
        profile = self._workspace_profile()
        bundle_plugins = [
            plugin
            for plugin in profile.findall("m:build/m:plugins/m:plugin", MAVEN_NAMESPACE)
            if _text(plugin, "m:artifactId") == "maven-bundle-plugin"
        ]

        self.assertEqual(len(bundle_plugins), 1)
        self.assertEqual(
            _text(bundle_plugins[0], "m:configuration/m:obrRepository"),
            "NONE",
            "workspace installs must not rewrite the shared local repository's OBR index",
        )

    def test_shade_uses_unique_dependency_reduced_pom_names(self) -> None:
        shade_plugins = [
            plugin
            for plugin in self.project.findall(
                "m:build/m:pluginManagement/m:plugins/m:plugin", MAVEN_NAMESPACE
            )
            if _text(plugin, "m:artifactId") == "maven-shade-plugin"
        ]

        self.assertEqual(len(shade_plugins), 1)
        self.assertEqual(
            _text(shade_plugins[0], "m:configuration/m:generateUniqueDependencyReducedPom"),
            "true",
            "parallel Shade executions must never overwrite dependency-reduced-pom.xml",
        )

    def test_normal_and_workspace_models_resolve_disjoint_test_temporary_directories(self) -> None:
        profile = self._workspace_profile()
        normal_template = _text(self.project, "m:properties/m:rdf4j.test.tmpDirectory")
        workspace_template = _text(profile, "m:properties/m:rdf4j.test.tmpDirectory")

        self.assertEqual(normal_template, "${project.build.directory}/tmp")
        self.assertIsNone(
            _text(self.project, "m:properties/m:rdf4j.test.tmpRoot"),
            "rdf4j.test.tmpRoot is runner-owned and must only exist as a workspace user property",
        )
        self.assertEqual(
            workspace_template,
            "${rdf4j.test.tmpRoot}/${project.groupId}/${project.artifactId}/${project.version}",
        )

        normal_build_directory = "/checkout/core/queryrender/target"
        workspace_tmp_root = "/tmp/rdf4j-workspaces/model-test/tmp/run-123"
        normal_tmp = _expand(normal_template, {"project.build.directory": normal_build_directory})
        workspace_tmp = _expand(
            workspace_template,
            {
                "rdf4j.test.tmpRoot": workspace_tmp_root,
                "project.groupId": "org.eclipse.rdf4j",
                "project.artifactId": "rdf4j-queryalgebra-evaluation",
                "project.version": "6.1.0-SNAPSHOT",
            },
        )

        self.assertEqual(normal_tmp, normal_build_directory + "/tmp")
        self.assertEqual(
            workspace_tmp,
            workspace_tmp_root
            + "/org.eclipse.rdf4j/rdf4j-queryalgebra-evaluation/6.1.0-SNAPSHOT",
        )

    def test_surefire_and_failsafe_preserve_normal_tmp_and_isolate_workspace_tmp(self) -> None:
        plugins = {
            _text(plugin, "m:artifactId"): plugin
            for plugin in self.project.findall(
                "m:build/m:pluginManagement/m:plugins/m:plugin", MAVEN_NAMESPACE
            )
        }
        workspace_plugins = {
            _text(plugin, "m:artifactId"): plugin
            for plugin in self._workspace_profile().findall(
                "m:build/m:plugins/m:plugin", MAVEN_NAMESPACE
            )
        }

        for artifact_id in ("maven-surefire-plugin", "maven-failsafe-plugin"):
            with self.subTest(plugin=artifact_id):
                plugin = plugins[artifact_id]
                self.assertEqual(
                    _text(
                        plugin,
                        "m:configuration/m:systemPropertyVariables/m:rdf4j.test.outputDirectory",
                    ),
                    "${project.build.directory}",
                )
                self.assertEqual(
                    _text(
                        plugin,
                        "m:configuration/m:systemPropertyVariables/m:rdf4j.test.tmpDirectory",
                    ),
                    "${rdf4j.test.tmpDirectory}",
                )
                self.assertIsNone(
                    _text(plugin, "m:configuration/m:systemPropertyVariables/m:java.io.tmpdir"),
                    "normal Maven test forks must retain their existing java.io.tmpdir",
                )
                self.assertEqual(
                    _text(
                        workspace_plugins[artifact_id],
                        "m:configuration/m:systemPropertyVariables/m:java.io.tmpdir",
                    ),
                    "${rdf4j.test.tmpDirectory}",
                    "workspace test forks must use the run-local temporary directory",
                )

    def test_unique_shade_state_is_ignored(self) -> None:
        ignore_lines = {
            line.strip()
            for line in (REPOSITORY_ROOT / ".gitignore").read_text(encoding="utf-8").splitlines()
        }
        formatting_profile = next(
            profile
            for profile in self.project.findall("m:profiles/m:profile", MAVEN_NAMESPACE)
            if _text(profile, "m:id") == "formatting"
        )
        spotless = next(
            plugin
            for plugin in formatting_profile.findall("m:build/m:plugins/m:plugin", MAVEN_NAMESPACE)
            if _text(plugin, "m:artifactId") == "spotless-maven-plugin"
        )
        spotless_excludes = {
            exclude.text.strip()
            for exclude in spotless.findall(
                "m:configuration/m:formats/m:format/m:excludes/m:exclude", MAVEN_NAMESPACE
            )
            if exclude.text
        }

        self.assertIn(
            "dependency-reduced-pom-*.xml",
            ignore_lines,
            "Shade 3.5.1 unique dependency-reduced POMs must not pollute module source directories",
        )
        self.assertNotIn(
            "drp-*.pom",
            ignore_lines,
            "Shade does not use the historical drp-*.pom filename pattern",
        )
        self.assertIn(
            "**/dependency-reduced-pom-*.xml",
            spotless_excludes,
            "Spotless must not mutate generated per-workspace Shade state",
        )

    def test_assembly_shade_output_follows_project_build_directory(self) -> None:
        assembly_plugins = {
            _text(plugin, "m:artifactId"): plugin
            for plugin in self.assembly_project.findall("m:build/m:plugins/m:plugin", MAVEN_NAMESPACE)
        }
        shade = assembly_plugins["maven-shade-plugin"]
        self.assertEqual(
            _text(shade, "m:configuration/m:outputFile"),
            "${project.build.directory}/eclipse-rdf4j-${project.version}-onejar.jar",
            "the one-jar must follow the assembly module's isolated build directory",
        )

    def test_workbench_consumes_named_server_war_path(self) -> None:
        workbench_plugins = {
            _text(plugin, "m:artifactId"): plugin
            for plugin in self.workbench_project.findall(
                "m:build/m:plugins/m:plugin", MAVEN_NAMESPACE
            )
        }
        jetty = workbench_plugins["jetty-ee11-maven-plugin"]
        self.assertEqual(
            _text(
                jetty,
                "m:configuration/m:contextHandlers/m:contextHandler/m:war",
            ),
            "${rdf4j.server.war}",
            "the workbench must consume the root model's named server-WAR producer path",
        )

    def test_sdk_descriptor_consumes_named_producer_paths(self) -> None:
        file_set_directories = [
            element.text.strip()
            for element in self.sdk_descriptor.findall("fileSets/fileSet/directory")
            if element.text
        ]
        file_sources = [
            element.text.strip()
            for element in self.sdk_descriptor.findall("files/file/source")
            if element.text
        ]

        self.assertEqual(
            (file_set_directories, file_sources[:2]),
            (
                ["..", "${rdf4j.apidocs.directory}"],
                ["${rdf4j.server.war}", "${rdf4j.workbench.war}"],
            ),
        )
        self.assertNotIn("../target/site/apidocs", file_set_directories)
        self.assertNotIn("../tools/server/target/rdf4j-server.war", file_sources)
        self.assertNotIn("../tools/workbench/target/rdf4j-workbench.war", file_sources)

    def test_normal_and_workspace_producer_paths_preserve_targets_and_full_gavs(self) -> None:
        profile = self._workspace_profile()
        property_names = (
            "rdf4j.apidocs.directory",
            "rdf4j.server.war",
            "rdf4j.workbench.war",
        )
        normal_templates = {
            name: _text(self.project, f"m:properties/m:{name}") for name in property_names
        }
        workspace_templates = {
            name: _text(profile, f"m:properties/m:{name}") for name in property_names
        }

        for name in property_names:
            with self.subTest(model="normal", property=name):
                self.assertIsNotNone(normal_templates[name])
            with self.subTest(model="workspace", property=name):
                self.assertIsNotNone(workspace_templates[name])

        if any(template is None for template in (*normal_templates.values(), *workspace_templates.values())):
            return

        normal_values = {
            name: Path(
                _expand(
                    normal_templates[name],
                    {
                        "maven.multiModuleProjectDirectory": str(REPOSITORY_ROOT),
                        "project.groupId": "org.eclipse.rdf4j",
                        "project.version": "6.1.0-SNAPSHOT",
                    },
                )
            )
            for name in property_names
        }
        workspace_root = REPOSITORY_ROOT / ".mvnf" / "workspaces" / "model-test" / "build"
        workspace_values = {
            name: Path(
                _expand(
                    workspace_templates[name],
                    {
                        "rdf4j.build.root": str(workspace_root),
                        "project.groupId": "org.eclipse.rdf4j",
                        "project.version": "6.1.0-SNAPSHOT",
                    },
                )
            )
            for name in property_names
        }

        self.assertEqual(normal_values["rdf4j.apidocs.directory"], REPOSITORY_ROOT / "target/site/apidocs")
        self.assertEqual(
            normal_values["rdf4j.server.war"],
            REPOSITORY_ROOT / "tools/server/target/rdf4j-server.war",
        )
        self.assertEqual(
            normal_values["rdf4j.workbench.war"],
            REPOSITORY_ROOT / "tools/workbench/target/rdf4j-workbench.war",
        )
        self.assertEqual(
            workspace_values["rdf4j.apidocs.directory"],
            workspace_root / "org.eclipse.rdf4j/rdf4j/6.1.0-SNAPSHOT/site/apidocs",
        )
        self.assertEqual(
            workspace_values["rdf4j.server.war"],
            workspace_root
            / "org.eclipse.rdf4j/rdf4j-http-server/6.1.0-SNAPSHOT/rdf4j-server.war",
        )
        self.assertEqual(
            workspace_values["rdf4j.workbench.war"],
            workspace_root
            / "org.eclipse.rdf4j/rdf4j-http-workbench/6.1.0-SNAPSHOT/rdf4j-workbench.war",
        )


if __name__ == "__main__":
    unittest.main()
