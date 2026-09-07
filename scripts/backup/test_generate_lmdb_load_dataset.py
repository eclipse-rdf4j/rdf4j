#!/usr/bin/env python3
"""Regression tests for deterministic LMDB load dataset identities."""

import importlib.util
import random
import sys
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("generate-lmdb-load-dataset.py")
SPEC = importlib.util.spec_from_file_location("generate_lmdb_load_dataset", MODULE_PATH)
GENERATOR = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules[SPEC.name] = GENERATOR
SPEC.loader.exec_module(GENERATOR)


class StatementIdentityTest(unittest.TestCase):
    def test_reused_identifier_buckets_keep_the_same_iris(self):
        parameters = GENERATOR.DatasetParameters(100, 20260903, 48, 96, 100, 15, 5000, "spoc,posc")
        rng = random.Random(parameters.seed)

        first = GENERATOR.statement(rng, parameters, 0)[0].split()
        same_predicate = GENERATOR.statement(rng, parameters, 128)[0].split()
        same_subject = GENERATOR.statement(rng, parameters, 50_000)[0].split()
        same_context = GENERATOR.statement(rng, parameters, 64)[0].split()

        self.assertEqual(first[1], same_predicate[1])
        self.assertEqual(first[0], same_subject[0])
        self.assertEqual(first[3], same_context[3])


if __name__ == "__main__":
    unittest.main()
