import Rdf4jEquivalenceFormal.Model

namespace Rdf4jEquivalenceFormal

theorem observe_append_associative
    (mode : Observation)
    (first second third : SolutionSequence) :
    observe mode (.success ((first ++ second) ++ third)) =
      observe mode (.success (first ++ (second ++ third))) := by
  rw [List.append_assoc]

theorem observe_append_empty_left
    (mode : Observation)
    (rows : SolutionSequence) :
    observe mode (.success ([] ++ rows)) = observe mode (.success rows) := by
  rfl

theorem observe_append_empty_right
    (mode : Observation)
    (rows : SolutionSequence) :
    observe mode (.success (rows ++ [])) = observe mode (.success rows) := by
  rw [List.append_nil]

theorem observe_bag_append_commutative
    (left right : SolutionSequence) :
    observe .bag (.success (left ++ right)) =
      observe .bag (.success (right ++ left)) := by
  simp only [observe]
  congr 1
  funext mapping
  simp [Nat.add_comm]

theorem observe_set_append_commutative
    (left right : SolutionSequence) :
    observe .set (.success (left ++ right)) =
      observe .set (.success (right ++ left)) := by
  simp only [observe]
  congr 1
  funext mapping
  simp [Bool.or_comm]

theorem observe_ask_append_commutative
    (left right : SolutionSequence) :
    observe .ask (.success (left ++ right)) =
      observe .ask (.success (right ++ left)) := by
  cases left <;> cases right <;> simp [observe]

theorem observe_set_append_idempotent
    (rows : SolutionSequence) :
    observe .set (.success (rows ++ rows)) = observe .set (.success rows) := by
  simp only [observe]
  congr 1
  funext mapping
  simp

theorem observe_ask_append_idempotent
    (rows : SolutionSequence) :
    observe .ask (.success (rows ++ rows)) = observe .ask (.success rows) := by
  cases rows <;> simp [observe]

theorem observe_set_eraseDups
    (rows : SolutionSequence) :
    observe .set (.success rows.eraseDups) = observe .set (.success rows) := by
  simp only [observe]
  congr 1
  funext mapping
  simp

theorem observe_ask_eraseDups
    (rows : SolutionSequence) :
    observe .ask (.success rows.eraseDups) = observe .ask (.success rows) := by
  cases rows <;> simp [observe, List.eraseDups_cons]

#print axioms observe_bag_append_commutative
#print axioms observe_append_associative
#print axioms observe_append_empty_left
#print axioms observe_append_empty_right
#print axioms observe_set_append_commutative
#print axioms observe_ask_append_commutative
#print axioms observe_set_append_idempotent
#print axioms observe_ask_append_idempotent
#print axioms observe_set_eraseDups
#print axioms observe_ask_eraseDups

end Rdf4jEquivalenceFormal
