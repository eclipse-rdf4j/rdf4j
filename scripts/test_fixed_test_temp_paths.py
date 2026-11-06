#!/usr/bin/env python3

from __future__ import annotations

import unittest
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
ENDPOINT_FACTORY_TEST = (
    REPOSITORY_ROOT
    / "tools"
    / "federation"
    / "src"
    / "test"
    / "java"
    / "org"
    / "eclipse"
    / "rdf4j"
    / "federated"
    / "endpoint"
    / "EndpointFactoryTest.java"
)
EMBEDDED_SERVER = (
    REPOSITORY_ROOT
    / "tools"
    / "federation"
    / "src"
    / "test"
    / "java"
    / "org"
    / "eclipse"
    / "rdf4j"
    / "federated"
    / "server"
    / "EmbeddedServer.java"
)
LUCENE_GEOSPARQL_TEST = (
    REPOSITORY_ROOT
    / "compliance"
    / "lucene"
    / "src"
    / "test"
    / "java"
    / "org"
    / "eclipse"
    / "rdf4j"
    / "sail"
    / "lucene"
    / "LuceneGeoSPARQLTest.java"
)


class FixedTestTempPathContractTest(unittest.TestCase):

    @staticmethod
    def assert_uses_test_temp_property(source: str, owner: str) -> None:
        assert 'System.getProperty("rdf4j.test.tmpDirectory"' in source, (
            f"{owner} must resolve its data below the dedicated RDF4J test temp directory"
        )
        assert 'System.getProperty("java.io.tmpdir")' in source, (
            f"{owner} must retain java.io.tmpdir as a non-Maven fallback"
        )

    def test_endpoint_data_uses_the_fork_temp_directory(self) -> None:
        source = ENDPOINT_FACTORY_TEST.read_text(encoding="utf-8")

        self.assert_uses_test_temp_property(source, "EndpointFactoryTest")
        self.assertTrue(
            '"rdf4j-federation", "fedxTest"' in source,
            "EndpointFactoryTest must preserve a distinct federation/fedxTest suffix",
        )
        self.assertNotIn('new File("target/tmp/fedxTest")', source)
        self.assertNotIn(
            'new File("target/tmp/fedxTest", "repositories/dbmodel")', source
        )
        self.assertTrue(
            'new File(baseDir, "repositories/dbmodel")' in source,
            "the repository assertion must remain relative to the configured base directory",
        )

    def test_embedded_jetty_uses_the_fork_temp_directory(self) -> None:
        source = EMBEDDED_SERVER.read_text(encoding="utf-8")

        self.assert_uses_test_temp_property(source, "EmbeddedServer")
        self.assertTrue(
            '"rdf4j-federation", "webapp"' in source,
            "EmbeddedServer must preserve a distinct federation/webapp suffix",
        )
        self.assertNotIn('new File("temp/webapp/")', source)
        self.assertTrue(
            "webapp.setTempDirectory(WEBAPP_TEMP_DIRECTORY);" in source,
            "Jetty must receive the resolved fork-specific temp directory",
        )

    def test_lucene_cleanup_uses_the_fork_temp_directory(self) -> None:
        source = LUCENE_GEOSPARQL_TEST.read_text(encoding="utf-8")

        self.assert_uses_test_temp_property(source, "LuceneGeoSPARQLTest")
        self.assertTrue(
            '"rdf4j-lucene", "test-data"' in source,
            "LuceneGeoSPARQLTest must preserve a distinct lucene/test-data suffix",
        )
        self.assertNotIn('"target/test-data"', source)
        self.assertTrue(
            "FileUtils.deleteDirectory(DATA_DIR);" in source,
            "Lucene cleanup must use the resolved fork-specific directory",
        )


if __name__ == "__main__":
    unittest.main()
