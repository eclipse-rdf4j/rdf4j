#!/usr/bin/env python3

from __future__ import annotations

import re
import unittest
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
TEST_SERVER = (
    REPOSITORY_ROOT
    / "tools/server/src/test/java/org/eclipse/rdf4j/http/server/TestServer.java"
)
LMDB_INVALID_INDEX_IT = (
    REPOSITORY_ROOT
    / "tools/server/src/test/java/org/eclipse/rdf4j/http/server/LmdbInvalidIndexDeletionIT.java"
)
HTTP_MEM_SERVER = (
    REPOSITORY_ROOT
    / "compliance/repository/src/test/java/org/eclipse/rdf4j/repository/http/HTTPMemServer.java"
)
SPARQL_EMBEDDED_SERVER = (
    REPOSITORY_ROOT
    / "compliance/sparql/src/test/java/org/eclipse/rdf4j/query/parser/sparql/SPARQLEmbeddedServer.java"
)
SERVER_BOOT_APPLICATION = (
    REPOSITORY_ROOT
    / "tools/server-boot/src/main/java/org/eclipse/rdf4j/tools/serverboot/Rdf4jServerWorkbenchApplication.java"
)
SERVER_BOOT_SIGNAL_IT = (
    REPOSITORY_ROOT
    / "tools/server-boot/src/test/java/org/eclipse/rdf4j/tools/serverboot/ServerBootSignalIT.java"
)


class ServerAppDataPathContractTest(unittest.TestCase):

    def assert_fork_local_default_preserves_explicit_appdata(
        self, source: str, module_suffix: str
    ) -> None:
        self.assertIsNotNone(
            re.compile(
                r"if \(System\.getProperty\(Platform\.APPDATA_BASEDIR_PROPERTY\) != null\) \{\s*"
                r"return;\s*}",
                re.MULTILINE,
            ).search(source),
            "an explicit RDF4J appdata property must win over the test default",
        )
        self.assertTrue(
            'System.getProperty("rdf4j.test.tmpDirectory"' in source,
            "the default appdata directory must use the dedicated RDF4J test temp property",
        )
        self.assertTrue(
            'System.getProperty("java.io.tmpdir")' in source,
            "the default appdata directory must retain java.io.tmpdir as a non-Maven fallback",
        )
        self.assertTrue(
            module_suffix in source,
            f"the default appdata directory must retain the module suffix {module_suffix}",
        )
        self.assertNotIn('System.getProperty("user.dir") + "/target/datadir"', source)

    def test_tools_server_uses_pom_webapp_and_fork_local_appdata(self) -> None:
        source = TEST_SERVER.read_text(encoding="utf-8")

        self.assertTrue(
            "webapp.setWar(webappDir);" in source,
            "TestServer must use the POM-derived webapp directory",
        )
        self.assertNotIn('webapp.setWar("./target/rdf4j-server");', source)
        self.assert_fork_local_default_preserves_explicit_appdata(
            source, '"rdf4j-http-server", "datadir"'
        )

    def test_lmdb_assertion_uses_the_active_appdata_property(self) -> None:
        source = LMDB_INVALID_INDEX_IT.read_text(encoding="utf-8")

        self.assertTrue(
            "System.getProperty(Platform.APPDATA_BASEDIR_PROPERTY)" in source,
            "the LMDB assertion must derive the repository from active RDF4J appdata",
        )
        self.assertNotIn('System.getProperty("user.dir")', source)

    def test_compliance_servers_use_distinct_fork_local_defaults(self) -> None:
        cases = (
            (HTTP_MEM_SERVER, '"rdf4j-repository-compliance", "datadir"'),
            (SPARQL_EMBEDDED_SERVER, '"rdf4j-sparql-compliance", "datadir"'),
        )

        for path, module_suffix in cases:
            with self.subTest(file=path.name):
                source = path.read_text(encoding="utf-8")
                self.assert_fork_local_default_preserves_explicit_appdata(
                    source, module_suffix
                )

    def test_server_boot_fallback_prefers_test_output_then_legacy_target(self) -> None:
        source = SERVER_BOOT_APPLICATION.read_text(encoding="utf-8")

        self.assertTrue(
            'TEST_OUTPUT_DIRECTORY_PROPERTY = "rdf4j.test.outputDirectory"' in source,
            "server-boot must name the propagated test-output property",
        )
        self.assertTrue(
            "resolveFallbackAppDataBase(" in source,
            "server-boot must expose a directly testable fallback resolver",
        )
        self.assertTrue(
            "System.getProperty(TEST_OUTPUT_DIRECTORY_PROPERTY)" in source,
            "the runtime fallback must read the propagated test-output directory",
        )
        self.assertTrue(
            'userDirectory.resolve("target")' in source,
            "normal launches must retain the user.dir/target fallback",
        )
        self.assertTrue(
            'outputDirectory.resolve("rdf4j-appdata")' in source,
            "all fallback roots must retain the rdf4j-appdata suffix",
        )

        output_lookup = (
            "String testOutputDirectory = "
            "System.getProperty(TEST_OUTPUT_DIRECTORY_PROPERTY);"
        )
        self.assertIn(
            output_lookup,
            source,
            "server-boot must inspect workspace output before probing platform appdata",
        )
        if output_lookup in source and "boolean defaultWritable" in source:
            workspace_branch = source[
                source.index(output_lookup) : source.index("boolean defaultWritable")
            ]
            self.assertIn(
                "if (testOutputDirectory != null && !testOutputDirectory.isBlank())",
                workspace_branch,
                "a nonblank workspace output must select isolated appdata immediately",
            )
            self.assertIn("resolveFallbackAppDataBase(", workspace_branch)
            self.assertIn("return;", workspace_branch)

    def test_server_boot_signal_it_consumes_workspace_build_directory(self) -> None:
        source = SERVER_BOOT_SIGNAL_IT.read_text(encoding="utf-8")

        self.assertIn(
            'TEST_OUTPUT_DIRECTORY_PROPERTY = "rdf4j.test.outputDirectory"',
            source,
            "the signal IT must name the propagated module output property",
        )
        self.assertIn(
            "Path targetDir = resolveBuildDirectory(projectRoot);",
            source,
            "the executable JAR lookup must use the workspace-aware build directory",
        )
        self.assertRegex(
            source,
            re.compile(
                r"String configuredOutputDirectory\s*=\s*System\.getProperty\("
                r"TEST_OUTPUT_DIRECTORY_PROPERTY\);\s*"
                r"return configuredOutputDirectory == null\s*"
                r"\|\| configuredOutputDirectory\.isBlank\(\)\s*"
                r"\? projectRoot\.resolve\(\"target\"\)\s*"
                r": Path\.of\(configuredOutputDirectory\);",
                re.MULTILINE,
            ),
            "normal runs retain target while workspace runs consume their full-GAV build root",
        )


if __name__ == "__main__":
    unittest.main()
