import Rdf4jEquivalenceFormal.OutcomeRules

namespace Rdf4jEquivalenceFormal

structure ReferenceEnvironment where
  evalOpaque : SemanticsTarget → Expr → Dataset → Mapping → FunctionEnvironment → EvalOutcome
  evalCondition : SemanticsTarget → String → Mapping → FunctionEnvironment → Bool

def emptyMapping : Mapping := ∅

def referenceValues
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (rows : List Mapping)
    (dataset : Dataset)
    (incoming : Mapping)
    (functions : FunctionEnvironment) : EvalOutcome :=
  match rows with
  | [] => .success []
  | [row] =>
      if row = emptyMapping then
        match target with
        | .rdf4jRuntime => .success [incoming]
        | _ => .success [row]
      else
        environment.evalOpaque target (.values [row]) dataset incoming functions
  | _ => environment.evalOpaque target (.values rows) dataset incoming functions

def referenceCondition
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (condition : String)
    (mapping : Mapping)
    (functions : FunctionEnvironment) : Bool :=
  if condition = "true" then true
  else if condition = "false" then false
  else environment.evalCondition target condition mapping functions

def referenceEval
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget) :
    Expr → Dataset → Mapping → FunctionEnvironment → EvalOutcome
  | .empty, _, _, _ => .success []
  | .unit, _, incoming, _ => .success [incoming]
  | .values rows, dataset, incoming, functions =>
      referenceValues environment target rows dataset incoming functions
  | .union left right, dataset, incoming, functions =>
      unionOutcome
        (referenceEval environment target left dataset incoming functions)
        (referenceEval environment target right dataset incoming functions)
  | .join .empty _, _, _, _ => .success []
  | .join left .empty, dataset, incoming, functions =>
      match referenceEval environment target left dataset incoming functions with
      | .failure => .failure
      | .success _ => .success []
  | .join .unit right, dataset, incoming, functions =>
      referenceEval environment target right dataset incoming functions
  | .join left .unit, dataset, incoming, functions =>
      referenceEval environment target left dataset incoming functions
  | .join left right, dataset, incoming, functions =>
      environment.evalOpaque target (.join left right) dataset incoming functions
  | .leftJoin .empty _ _, _, _, _ => .success []
  | .leftJoin left .empty _, dataset, incoming, functions =>
      referenceEval environment target left dataset incoming functions
  | .leftJoin left .unit _, dataset, incoming, functions =>
      referenceEval environment target left dataset incoming functions
  | .leftJoin left right condition, dataset, incoming, functions =>
      environment.evalOpaque target (.leftJoin left right condition) dataset incoming functions
  | .minus .empty _, _, _, _ => .success []
  | .minus left .empty, dataset, incoming, functions =>
      referenceEval environment target left dataset incoming functions
  | .minus left right, dataset, incoming, functions =>
      environment.evalOpaque target (.minus left right) dataset incoming functions
  | .distinct arg, dataset, incoming, functions =>
      distinctOutcome (referenceEval environment target arg dataset incoming functions)
  | .reduced arg, dataset, incoming, functions =>
      match target with
      | .rdf4jRuntime =>
          environment.evalOpaque target (.reduced arg) dataset incoming functions
      | _ =>
          distinctOutcome (referenceEval environment target arg dataset incoming functions)
  | .filter condition arg, dataset, incoming, functions =>
      filterRowsOutcome
        (fun mapping => referenceCondition environment target condition mapping functions)
        (referenceEval environment target arg dataset incoming functions)
  | expression, dataset, incoming, functions =>
      environment.evalOpaque target expression dataset incoming functions

def referenceModel (environment : ReferenceEnvironment) : SemanticModel where
  rdf4jRuntime := referenceEval environment .rdf4jRuntime
  sparql11 := referenceEval environment .sparql11
  sparql12 := referenceEval environment .sparql12Draft20260605

def ReferenceSucceeds
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (expression : Expr) : Prop :=
  match target with
  | .rdf4jRuntime =>
      ∀ dataset incoming functions,
        ∃ rows, referenceEval environment .rdf4jRuntime expression dataset incoming functions = .success rows
  | .sparql11 =>
      ∀ dataset incoming functions,
        ∃ rows, referenceEval environment .sparql11 expression dataset incoming functions = .success rows
  | .sparql12Draft20260605 =>
      ∀ dataset incoming functions,
        ∃ rows,
          referenceEval environment .sparql12Draft20260605 expression dataset incoming functions = .success rows
  | .both11And12 =>
      (∀ dataset incoming functions,
        ∃ rows, referenceEval environment .sparql11 expression dataset incoming functions = .success rows) ∧
      (∀ dataset incoming functions,
        ∃ rows,
          referenceEval environment .sparql12Draft20260605 expression dataset incoming functions = .success rows)

theorem referenceEval_join_empty_right
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (expression : Expr)
    (dataset : Dataset)
    (incoming : Mapping)
    (functions : FunctionEnvironment) :
    referenceEval environment target (.join expression .empty) dataset incoming functions =
      match referenceEval environment target expression dataset incoming functions with
      | .failure => .failure
      | .success _ => .success [] := by
  cases expression <;> rfl

theorem reference_union_associative
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (first second third : Expr) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.union (.union first second) third)
      (.union first (.union second third)) := by
  intro dataset incoming functions
  cases target <;> simp [referenceModel, referenceEval, unionOutcome_associative]

theorem reference_filter_distributes_over_union
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (condition : String)
    (first second : Expr) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.filter condition (.union first second))
      (.union (.filter condition first) (.filter condition second)) := by
  intro dataset incoming functions
  cases target <;>
    simp [referenceModel, referenceEval, filterRowsOutcome_union]

theorem reference_join_unit_left
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (expression : Expr) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.join .unit expression)
      expression := by
  intro dataset incoming functions
  cases expression <;> cases target <;> simp [referenceModel, referenceEval]

theorem reference_join_unit_right
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (expression : Expr) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.join expression .unit)
      expression := by
  intro dataset incoming functions
  cases expression <;> cases target <;> simp [referenceModel, referenceEval]

theorem reference_join_empty_left
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (expression : Expr) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.join .empty expression)
      .empty := by
  intro dataset incoming functions
  cases target <;> simp [referenceModel, referenceEval]

theorem reference_join_empty_right_of_success
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (expression : Expr)
    (succeeds : ReferenceSucceeds environment target expression) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.join expression .empty)
      .empty := by
  intro dataset incoming functions
  cases target with
  | rdf4jRuntime =>
      obtain ⟨rows, evaluated⟩ := succeeds dataset incoming functions
      change observe observation
          (referenceEval environment .rdf4jRuntime (.join expression .empty) dataset incoming functions) =
        observe observation (.success [])
      rw [referenceEval_join_empty_right, evaluated]
  | sparql11 =>
      obtain ⟨rows, evaluated⟩ := succeeds dataset incoming functions
      change observe observation
          (referenceEval environment .sparql11 (.join expression .empty) dataset incoming functions) =
        observe observation (.success [])
      rw [referenceEval_join_empty_right, evaluated]
  | sparql12Draft20260605 =>
      obtain ⟨rows, evaluated⟩ := succeeds dataset incoming functions
      change observe observation
          (referenceEval environment .sparql12Draft20260605 (.join expression .empty) dataset incoming functions) =
        observe observation (.success [])
      rw [referenceEval_join_empty_right, evaluated]
  | both11And12 =>
      obtain ⟨sparql11Succeeds, sparql12Succeeds⟩ := succeeds
      obtain ⟨rows11, evaluated11⟩ := sparql11Succeeds dataset incoming functions
      obtain ⟨rows12, evaluated12⟩ := sparql12Succeeds dataset incoming functions
      constructor
      · change observe observation
            (referenceEval environment .sparql11 (.join expression .empty) dataset incoming functions) =
          observe observation (.success [])
        rw [referenceEval_join_empty_right, evaluated11]
      · change observe observation
            (referenceEval environment .sparql12Draft20260605 (.join expression .empty) dataset incoming functions) =
          observe observation (.success [])
        rw [referenceEval_join_empty_right, evaluated12]

theorem reference_leftJoin_empty_left
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (right : Expr)
    (condition : Option String) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.leftJoin .empty right condition)
      .empty := by
  intro dataset incoming functions
  cases target <;> simp [referenceModel, referenceEval]

theorem reference_leftJoin_empty_right
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (left : Expr)
    (condition : Option String) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.leftJoin left .empty condition)
      left := by
  intro dataset incoming functions
  cases left <;> cases target <;> simp [referenceModel, referenceEval]

theorem reference_leftJoin_unit_right
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (left : Expr)
    (condition : Option String) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.leftJoin left .unit condition)
      left := by
  intro dataset incoming functions
  cases left <;> cases target <;> simp [referenceModel, referenceEval]

theorem reference_minus_empty_left
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (right : Expr) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.minus .empty right)
      .empty := by
  intro dataset incoming functions
  cases target <;> simp [referenceModel, referenceEval]

theorem reference_minus_empty_right
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (left : Expr) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.minus left .empty)
      left := by
  intro dataset incoming functions
  cases left <;> cases target <;> simp [referenceModel, referenceEval]

theorem reference_union_commutative
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (notSequence : observation ≠ .sequence)
    (left right : Expr) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.union left right)
      (.union right left) := by
  intro dataset incoming functions
  cases target <;> simp [referenceModel, referenceEval, observe_union_commutative, notSequence]

theorem reference_union_empty_left
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (expression : Expr) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.union .empty expression)
      expression := by
  intro dataset incoming functions
  cases target <;> simp [referenceModel, referenceEval, observe_union_empty_left]

theorem reference_union_empty_right
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (expression : Expr) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.union expression .empty)
      expression := by
  intro dataset incoming functions
  cases target <;> simp [referenceModel, referenceEval, observe_union_empty_right]

theorem reference_union_set_idempotent
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (expression : Expr) :
    Equivalent
      (referenceModel environment)
      target
      .set
      (.union expression expression)
      expression := by
  intro dataset incoming functions
  cases target <;> simp [referenceModel, referenceEval, observe_union_set_idempotent]

theorem reference_union_ask_idempotent
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (expression : Expr) :
    Equivalent
      (referenceModel environment)
      target
      .ask
      (.union expression expression)
      expression := by
  intro dataset incoming functions
  cases target <;> simp [referenceModel, referenceEval, observe_union_ask_idempotent]

theorem reference_distinct_set_hidden
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (expression : Expr) :
    Equivalent
      (referenceModel environment)
      target
      .set
      (.distinct expression)
      expression := by
  intro dataset incoming functions
  cases target <;> simp [referenceModel, referenceEval, observe_distinct_set_hidden]

theorem reference_filter_true_identity
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (expression : Expr) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.filter "true" expression)
      expression := by
  intro dataset incoming functions
  cases target <;>
    simp [referenceModel, referenceEval, referenceCondition, filterRowsOutcome_true]

theorem reference_filter_false_empty
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (expression : Expr)
    (succeeds : ReferenceSucceeds environment target expression) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.filter "false" expression)
      .empty := by
  intro dataset incoming functions
  cases target with
  | rdf4jRuntime =>
      obtain ⟨rows, evaluated⟩ := succeeds dataset incoming functions
      change observe observation
          (filterRowsOutcome (fun _ => false)
            (referenceEval environment .rdf4jRuntime expression dataset incoming functions)) =
        observe observation (.success [])
      rw [evaluated, filterRowsOutcome_false_success]
  | sparql11 =>
      obtain ⟨rows, evaluated⟩ := succeeds dataset incoming functions
      change observe observation
          (filterRowsOutcome (fun _ => false)
            (referenceEval environment .sparql11 expression dataset incoming functions)) =
        observe observation (.success [])
      rw [evaluated, filterRowsOutcome_false_success]
  | sparql12Draft20260605 =>
      obtain ⟨rows, evaluated⟩ := succeeds dataset incoming functions
      change observe observation
          (filterRowsOutcome (fun _ => false)
            (referenceEval environment .sparql12Draft20260605 expression dataset incoming functions)) =
        observe observation (.success [])
      rw [evaluated, filterRowsOutcome_false_success]
  | both11And12 =>
      obtain ⟨sparql11Succeeds, sparql12Succeeds⟩ := succeeds
      obtain ⟨rows11, evaluated11⟩ := sparql11Succeeds dataset incoming functions
      obtain ⟨rows12, evaluated12⟩ := sparql12Succeeds dataset incoming functions
      constructor
      · change observe observation
            (filterRowsOutcome (fun _ => false)
              (referenceEval environment .sparql11 expression dataset incoming functions)) =
          observe observation (.success [])
        rw [evaluated11, filterRowsOutcome_false_success]
      · change observe observation
            (filterRowsOutcome (fun _ => false)
              (referenceEval environment .sparql12Draft20260605 expression dataset incoming functions)) =
          observe observation (.success [])
        rw [evaluated12, filterRowsOutcome_false_success]

theorem reference_distinct_idempotent
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (expression : Expr) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.distinct (.distinct expression))
      (.distinct expression) := by
  intro dataset incoming functions
  cases target <;> simp [referenceModel, referenceEval, distinctOutcome_idempotent]

theorem reference_distinct_ask_hidden
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (expression : Expr) :
    Equivalent
      (referenceModel environment)
      target
      .ask
      (.distinct expression)
      expression := by
  intro dataset incoming functions
  cases target <;> simp [referenceModel, referenceEval, observe_distinct_ask_hidden]

theorem reference_reduced_set_hidden
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (specificationTarget : target ≠ .rdf4jRuntime)
    (expression : Expr) :
    Equivalent
      (referenceModel environment)
      target
      .set
      (.reduced expression)
      expression := by
  intro dataset incoming functions
  cases target <;> simp_all [referenceModel, referenceEval, observe_distinct_set_hidden]

theorem reference_reduced_ask_hidden
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (specificationTarget : target ≠ .rdf4jRuntime)
    (expression : Expr) :
    Equivalent
      (referenceModel environment)
      target
      .ask
      (.reduced expression)
      expression := by
  intro dataset incoming functions
  cases target <;> simp_all [referenceModel, referenceEval, observe_distinct_ask_hidden]

theorem reference_values_empty
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.values [])
      .empty := by
  intro dataset incoming functions
  cases target <;> simp [referenceModel, referenceEval, referenceValues]

theorem reference_runtime_values_unit_all_bindings
    (environment : ReferenceEnvironment)
    (observation : Observation) :
    EquivalentInContext
      (referenceModel environment)
      .rdf4jRuntime
      observation
      .allBindings
      (.values [emptyMapping])
      .unit := by
  intro dataset incoming functions permitted
  simp [referenceModel, referenceEval, referenceValues]

theorem reference_values_unit_top_level
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation) :
    EquivalentInContext
      (referenceModel environment)
      target
      observation
      .topLevelEmpty
      (.values [emptyMapping])
      .unit := by
  intro dataset incoming functions permitted
  change incoming = emptyMapping at permitted
  subst incoming
  cases target <;> simp [referenceModel, referenceEval, referenceValues]

#print axioms referenceEval_join_empty_right
#print axioms reference_union_associative
#print axioms reference_filter_distributes_over_union
#print axioms reference_join_unit_left
#print axioms reference_join_unit_right
#print axioms reference_join_empty_left
#print axioms reference_join_empty_right_of_success
#print axioms reference_leftJoin_empty_left
#print axioms reference_leftJoin_empty_right
#print axioms reference_leftJoin_unit_right
#print axioms reference_minus_empty_left
#print axioms reference_minus_empty_right
#print axioms reference_union_commutative
#print axioms reference_union_empty_left
#print axioms reference_union_empty_right
#print axioms reference_union_set_idempotent
#print axioms reference_union_ask_idempotent
#print axioms reference_distinct_idempotent
#print axioms reference_distinct_set_hidden
#print axioms reference_filter_true_identity
#print axioms reference_filter_false_empty
#print axioms reference_distinct_ask_hidden
#print axioms reference_reduced_set_hidden
#print axioms reference_reduced_ask_hidden
#print axioms reference_values_empty
#print axioms reference_runtime_values_unit_all_bindings
#print axioms reference_values_unit_top_level

end Rdf4jEquivalenceFormal
