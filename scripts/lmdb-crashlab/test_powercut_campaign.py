import unittest

from run_powercut_campaign import (
    SCENARIOS,
    validate_cutpoint_witness,
    validate_recovery_pair,
)


class PowerCutContractTests(unittest.TestCase):
    def witness(self, scenario):
        contract = SCENARIOS[scenario]
        return {
            "scenario": scenario,
            "cutpoint": contract["cutpoint"],
            "A_acknowledged": True,
            "B_attempted": True,
            "B_commit_invoked": contract["commit_invoked"],
            "B_native_commit_returned": contract["native_commit_returned"],
            "B_acknowledged": False,
        }

    def result(self, outcome, digest="same-complete-state"):
        return {"outcome": outcome, "stateSha256": digest}

    def test_replay_cut_is_invoked_before_native_commit_and_requires_exact_old_state(self):
        validate_cutpoint_witness("mixed-replay-before-native-commit",
                                  self.witness("mixed-replay-before-native-commit"),
                                  b_acknowledged=False)
        validate_recovery_pair("mixed-replay-before-native-commit", self.result("A"), self.result("A"))
        with self.assertRaises(ValueError):
            validate_cutpoint_witness("mixed-replay-before-native-commit", {
                **self.witness("mixed-replay-before-native-commit"), "B_commit_invoked": False,
            }, b_acknowledged=False)

    def test_dictionary_cut_is_after_invocation_but_before_native_commit(self):
        validate_cutpoint_witness("dictionary-before-triple-commit",
                                  self.witness("dictionary-before-triple-commit"), b_acknowledged=False)
        validate_recovery_pair("dictionary-before-triple-commit", self.result("A"), self.result("A"))
        with self.assertRaises(ValueError):
            validate_recovery_pair("dictionary-before-triple-commit",
                                   self.result("A_PLUS_B"), self.result("A_PLUS_B"))

    def test_returned_commit_is_unknown_but_repeated_outcome_must_match(self):
        validate_cutpoint_witness("commit-returned-before-ack", self.witness("commit-returned-before-ack"),
                                  b_acknowledged=False)
        validate_recovery_pair("commit-returned-before-ack", self.result("A_PLUS_B"), self.result("A_PLUS_B"))
        with self.assertRaises(ValueError):
            validate_recovery_pair("commit-returned-before-ack", self.result("A"), self.result("A"))
        with self.assertRaises(ValueError):
            validate_recovery_pair("commit-returned-before-ack", self.result("A"), self.result("A_PLUS_B"))

    def test_b_acknowledgment_is_never_permitted_at_power_cut(self):
        with self.assertRaises(ValueError):
            validate_cutpoint_witness("commit-returned-before-ack", self.witness("commit-returned-before-ack"),
                                      b_acknowledged=True)

    def test_two_recoveries_must_match_complete_state_hash(self):
        with self.assertRaises(ValueError):
            validate_recovery_pair("commit-returned-before-ack", self.result("A", "hash-1"),
                                   self.result("A", "hash-2"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
