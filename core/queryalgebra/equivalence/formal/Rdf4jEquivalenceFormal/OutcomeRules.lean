import Rdf4jEquivalenceFormal.ObservationLaws

namespace Rdf4jEquivalenceFormal

def unionOutcome : EvalOutcome → EvalOutcome → EvalOutcome
  | .success left, .success right => .success (left ++ right)
  | _, _ => .failure

theorem unionOutcome_associative
    (first second third : EvalOutcome) :
    unionOutcome (unionOutcome first second) third =
      unionOutcome first (unionOutcome second third) := by
  cases first <;> cases second <;> cases third <;> simp [unionOutcome, List.append_assoc]

theorem observe_union_commutative
    (mode : Observation)
    (notSequence : mode ≠ .sequence)
    (left right : EvalOutcome) :
    observe mode (unionOutcome left right) =
      observe mode (unionOutcome right left) := by
  cases left with
  | failure => cases right <;> rfl
  | success leftRows =>
      cases right with
      | failure => rfl
      | success rightRows =>
          cases mode with
          | bag => simpa [unionOutcome] using observe_bag_append_commutative leftRows rightRows
          | set => simpa [unionOutcome] using observe_set_append_commutative leftRows rightRows
          | sequence => contradiction
          | ask => simpa [unionOutcome] using observe_ask_append_commutative leftRows rightRows

theorem observe_union_empty_left
    (mode : Observation)
    (outcome : EvalOutcome) :
    observe mode (unionOutcome (.success []) outcome) = observe mode outcome := by
  cases outcome <;> simp [unionOutcome, observe]

theorem observe_union_empty_right
    (mode : Observation)
    (outcome : EvalOutcome) :
    observe mode (unionOutcome outcome (.success [])) = observe mode outcome := by
  cases outcome <;> simp [unionOutcome, observe]

theorem observe_union_set_idempotent
    (outcome : EvalOutcome) :
    observe .set (unionOutcome outcome outcome) = observe .set outcome := by
  cases outcome <;> simp [unionOutcome, observe]

theorem observe_union_ask_idempotent
    (outcome : EvalOutcome) :
    observe .ask (unionOutcome outcome outcome) = observe .ask outcome := by
  cases outcome with
  | failure => rfl
  | success rows => simpa [unionOutcome] using observe_ask_append_idempotent rows

def deduplicateMappings : SolutionSequence → SolutionSequence
  | [] => []
  | mapping :: rows =>
      let deduplicated := deduplicateMappings rows
      if mapping ∈ deduplicated then deduplicated else mapping :: deduplicated

theorem deduplicateMappings_nodup
    (rows : SolutionSequence) :
    (deduplicateMappings rows).Nodup := by
  induction rows with
  | nil => simp [deduplicateMappings]
  | cons mapping rows inductionHypothesis =>
      simp only [deduplicateMappings]
      split
      · exact inductionHypothesis
      · exact List.nodup_cons.mpr ⟨by assumption, inductionHypothesis⟩

theorem deduplicateMappings_eq_self
    (rows : SolutionSequence)
    (nodup : rows.Nodup) :
    deduplicateMappings rows = rows := by
  induction rows with
  | nil => rfl
  | cons mapping rows inductionHypothesis =>
      have ⟨notMember, tailNodup⟩ := List.nodup_cons.mp nodup
      simp [deduplicateMappings, inductionHypothesis tailNodup, notMember]

theorem deduplicateMappings_idempotent
    (rows : SolutionSequence) :
    deduplicateMappings (deduplicateMappings rows) = deduplicateMappings rows :=
  deduplicateMappings_eq_self _ (deduplicateMappings_nodup rows)

theorem mem_deduplicateMappings
    (mapping : Mapping)
    (rows : SolutionSequence) :
    mapping ∈ deduplicateMappings rows ↔ mapping ∈ rows := by
  induction rows generalizing mapping with
  | nil => simp [deduplicateMappings]
  | cons head rows inductionHypothesis =>
      simp only [deduplicateMappings]
      split <;> simp_all

def distinctOutcome : EvalOutcome → EvalOutcome
  | .failure => .failure
  | .success rows => .success (deduplicateMappings rows)

def filterOutcome (keep : Bool) : EvalOutcome → EvalOutcome
  | .failure => .failure
  | .success rows => if keep then .success rows else .success []

def filterRowsOutcome (keep : Mapping → Bool) : EvalOutcome → EvalOutcome
  | .failure => .failure
  | .success rows => .success (rows.filter keep)

theorem filterOutcome_true_identity
    (outcome : EvalOutcome) :
    filterOutcome true outcome = outcome := by
  cases outcome <;> simp [filterOutcome]

theorem filterOutcome_false_success
    (rows : SolutionSequence) :
    filterOutcome false (.success rows) = .success [] := by
  simp [filterOutcome]

theorem filterRowsOutcome_true (outcome : EvalOutcome) :
    filterRowsOutcome (fun _ => true) outcome = outcome := by
  cases outcome <;> simp [filterRowsOutcome]

theorem filterRowsOutcome_false_success (rows : SolutionSequence) :
    filterRowsOutcome (fun _ => false) (.success rows) = .success [] := by
  simp [filterRowsOutcome]

theorem filterRowsOutcome_union
    (keep : Mapping → Bool)
    (left right : EvalOutcome) :
    filterRowsOutcome keep (unionOutcome left right) =
      unionOutcome (filterRowsOutcome keep left) (filterRowsOutcome keep right) := by
  cases left <;> cases right <;> simp [filterRowsOutcome, unionOutcome, List.filter_append]

def optionalMatches
    (merge : Mapping → Mapping → Option Mapping)
    (keep : Mapping → Bool)
    (left : Mapping)
    (rightRows : SolutionSequence) : SolutionSequence :=
  rightRows.filterMap fun right =>
    match merge left right with
    | none => none
    | some combined => if keep combined then some combined else none

def leftJoinRow
    (merge : Mapping → Mapping → Option Mapping)
    (keep : Mapping → Bool)
    (left : Mapping)
    (rightRows : SolutionSequence) : SolutionSequence :=
  let matchedRows := optionalMatches merge keep left rightRows
  if matchedRows.isEmpty then [left] else matchedRows

def leftJoinRows
    (merge : Mapping → Mapping → Option Mapping)
    (keep : Mapping → Bool)
    (leftRows rightRows : SolutionSequence) : SolutionSequence :=
  leftRows.flatMap fun left => leftJoinRow merge keep left rightRows

def leftJoinOutcome
    (merge : Mapping → Mapping → Option Mapping)
    (keep : Mapping → Bool) : EvalOutcome → EvalOutcome → EvalOutcome
  | .failure, _ => .failure
  | .success [], _ => .success []
  | .success (_ :: _), .failure => .failure
  | .success leftRows, .success rightRows =>
      .success (leftJoinRows merge keep leftRows rightRows)

@[simp] theorem leftJoinRows_append
    (merge : Mapping → Mapping → Option Mapping)
    (keep : Mapping → Bool)
    (first second right : SolutionSequence) :
    leftJoinRows merge keep (first ++ second) right =
      leftJoinRows merge keep first right ++ leftJoinRows merge keep second right := by
  simp [leftJoinRows, List.flatMap_append]

@[simp] theorem leftJoinOutcome_success
    (merge : Mapping → Mapping → Option Mapping)
    (keep : Mapping → Bool)
    (leftRows rightRows : SolutionSequence) :
    leftJoinOutcome merge keep (.success leftRows) (.success rightRows) =
      .success (leftJoinRows merge keep leftRows rightRows) := by
  cases leftRows <;> rfl

theorem leftJoinOutcome_union_left
    (merge : Mapping → Mapping → Option Mapping)
    (keep : Mapping → Bool)
    (first second right : EvalOutcome) :
    leftJoinOutcome merge keep (unionOutcome first second) right =
      unionOutcome
        (leftJoinOutcome merge keep first right)
        (leftJoinOutcome merge keep second right) := by
  cases first with
  | failure => cases second <;> rfl
  | success firstRows =>
      cases second with
      | failure => cases firstRows <;> cases right <;> rfl
      | success secondRows =>
          cases right with
          | failure => cases firstRows <;> cases secondRows <;> rfl
          | success rightRows =>
              simp [unionOutcome, leftJoinOutcome_success, leftJoinRows_append]

def minusRows
    (harmful : Mapping → Mapping → Bool)
    (leftRows rightRows : SolutionSequence) : SolutionSequence :=
  leftRows.filter fun left => !rightRows.any (harmful left)

def minusOutcome
    (harmful : Mapping → Mapping → Bool) : EvalOutcome → EvalOutcome → EvalOutcome
  | .failure, _ => .failure
  | .success [], _ => .success []
  | .success (_ :: _), .failure => .failure
  | .success leftRows, .success rightRows => .success (minusRows harmful leftRows rightRows)

@[simp] theorem minusRows_append_left
    (harmful : Mapping → Mapping → Bool)
    (first second right : SolutionSequence) :
    minusRows harmful (first ++ second) right =
      minusRows harmful first right ++ minusRows harmful second right := by
  simp [minusRows, List.filter_append]

theorem minusRows_union_right
    (harmful : Mapping → Mapping → Bool)
    (left first second : SolutionSequence) :
    minusRows harmful left (first ++ second) =
      minusRows harmful (minusRows harmful left first) second := by
  induction left with
  | nil => rfl
  | cons head tail inductionHypothesis =>
      simp [minusRows, List.any_append, inductionHypothesis, Bool.not_or, Bool.and_comm]

theorem minusRows_blockers_commute
    (harmful : Mapping → Mapping → Bool)
    (left first second : SolutionSequence) :
    minusRows harmful (minusRows harmful left first) second =
      minusRows harmful (minusRows harmful left second) first := by
  induction left with
  | nil => rfl
  | cons head tail inductionHypothesis =>
      simp [minusRows, inductionHypothesis, Bool.and_comm]

theorem minusRows_blocker_idempotent
    (harmful : Mapping → Mapping → Bool)
    (left blocker : SolutionSequence) :
    minusRows harmful (minusRows harmful left blocker) blocker =
      minusRows harmful left blocker := by
  induction left with
  | nil => rfl
  | cons head tail inductionHypothesis =>
      simp [minusRows, inductionHypothesis]

@[simp] theorem minusOutcome_success
    (harmful : Mapping → Mapping → Bool)
    (leftRows rightRows : SolutionSequence) :
    minusOutcome harmful (.success leftRows) (.success rightRows) =
      .success (minusRows harmful leftRows rightRows) := by
  cases leftRows <;> rfl

theorem minusOutcome_union_left
    (harmful : Mapping → Mapping → Bool)
    (first second right : EvalOutcome) :
    minusOutcome harmful (unionOutcome first second) right =
      unionOutcome
        (minusOutcome harmful first right)
        (minusOutcome harmful second right) := by
  cases first with
  | failure => cases second <;> rfl
  | success firstRows =>
      cases second with
      | failure => cases firstRows <;> cases right <;> rfl
      | success secondRows =>
          cases right with
          | failure => cases firstRows <;> cases secondRows <;> rfl
          | success rightRows =>
              simp [unionOutcome, minusOutcome_success, minusRows_append_left]

theorem distinctOutcome_idempotent
    (outcome : EvalOutcome) :
    distinctOutcome (distinctOutcome outcome) = distinctOutcome outcome := by
  cases outcome <;> simp [distinctOutcome, deduplicateMappings_idempotent]

theorem observe_distinct_set_hidden
    (outcome : EvalOutcome) :
    observe .set (distinctOutcome outcome) = observe .set outcome := by
  cases outcome with
  | failure => rfl
  | success rows =>
      simp only [distinctOutcome, observe]
      congr 1
      funext mapping
      simp [mem_deduplicateMappings]

theorem observe_distinct_ask_hidden
    (outcome : EvalOutcome) :
    observe .ask (distinctOutcome outcome) = observe .ask outcome := by
  cases outcome with
  | failure => rfl
  | success rows =>
      cases rows with
      | nil => rfl
      | cons head tail =>
          have present : head ∈ deduplicateMappings (head :: tail) :=
            (mem_deduplicateMappings head (head :: tail)).mpr (by simp)
          have nonempty : deduplicateMappings (head :: tail) ≠ [] := by
            intro empty
            simp [empty] at present
          simp [distinctOutcome, observe, nonempty]

theorem observe_values_empty
    (mode : Observation) :
    observe mode (.success []) = observe mode (.success ([] : SolutionSequence)) := by
  rfl

theorem observe_values_unit
    (mode : Observation)
    (emptyMapping : Mapping) :
    observe mode (.success [emptyMapping]) = observe mode (.success [emptyMapping]) := by
  rfl

#print axioms unionOutcome_associative
#print axioms observe_union_commutative
#print axioms observe_union_empty_left
#print axioms observe_union_empty_right
#print axioms observe_union_set_idempotent
#print axioms observe_union_ask_idempotent
#print axioms deduplicateMappings_nodup
#print axioms deduplicateMappings_eq_self
#print axioms deduplicateMappings_idempotent
#print axioms mem_deduplicateMappings
#print axioms distinctOutcome_idempotent
#print axioms filterOutcome_true_identity
#print axioms filterOutcome_false_success
#print axioms filterRowsOutcome_true
#print axioms filterRowsOutcome_false_success
#print axioms filterRowsOutcome_union
#print axioms leftJoinRows_append
#print axioms leftJoinOutcome_success
#print axioms leftJoinOutcome_union_left
#print axioms minusRows_append_left
#print axioms minusRows_union_right
#print axioms minusRows_blockers_commute
#print axioms minusRows_blocker_idempotent
#print axioms minusOutcome_success
#print axioms minusOutcome_union_left
#print axioms observe_distinct_set_hidden
#print axioms observe_distinct_ask_hidden
#print axioms observe_values_empty
#print axioms observe_values_unit

end Rdf4jEquivalenceFormal
