import Rdf4jEquivalenceFormal.OutcomeRules

namespace Rdf4jEquivalenceFormal

structure ReferenceEnvironment where
  evalOpaque : SemanticsTarget → Expr → Dataset → Mapping → FunctionEnvironment → EvalOutcome
  evalCondition : SemanticsTarget → String → Mapping → FunctionEnvironment → Bool
  /-- Whether a condition survives preparation. RDF4J compiles a condition once, before any input is
      evaluated, so preparation depends only on the condition text and the available functions. A
      constant `REGEX` with an invalid pattern fails here even when no row is ever tested. -/
  conditionPrepares : SemanticsTarget → String → FunctionEnvironment → Bool

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

/-- Preparation of an optional condition under a target.

`RDF4J_RUNTIME` precompiles a condition eagerly, before either input of the owning operator is
evaluated, so an unpreparable condition fails the whole expression even when no row reaches it. The
specification targets evaluate a condition per solution instead, so an absent solution set never
triggers the condition at all and preparation cannot fail. An absent condition (`none`) has nothing
to compile and therefore always prepares. -/
def conditionPrepared
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (condition : Option String)
    (functions : FunctionEnvironment) : Bool :=
  match target with
  | .rdf4jRuntime =>
      match condition with
      | none => true
      | some text => environment.conditionPrepares .rdf4jRuntime text functions
  | _ => true

@[simp] theorem conditionPrepared_none
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (functions : FunctionEnvironment) :
    conditionPrepared environment target none functions = true := by
  cases target <;> rfl

@[simp] theorem conditionPrepared_of_not_runtime
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (condition : Option String)
    (functions : FunctionEnvironment)
    (notRuntime : target ≠ .rdf4jRuntime) :
    conditionPrepared environment target condition functions = true := by
  cases target with
  | rdf4jRuntime => exact absurd rfl notRuntime
  | sparql11 => rfl
  | sparql12Draft20260605 => rfl
  | both11And12 => rfl

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
  -- The elided right operand and condition are still prepared and evaluated by RDF4J, so their
  -- failures survive the elision. Compare `.join left .empty` below, which already propagates the
  -- discarded operand's failure.
  | .leftJoin .empty right condition, dataset, incoming, functions =>
      if conditionPrepared environment target condition functions then
        match target with
        | .rdf4jRuntime =>
            match referenceEval environment .rdf4jRuntime right dataset incoming functions with
            | .failure => .failure
            | .success _ => .success []
        | _ => .success []
      else .failure
  | .leftJoin left .empty condition, dataset, incoming, functions =>
      if conditionPrepared environment target condition functions then
        referenceEval environment target left dataset incoming functions
      else .failure
  | .leftJoin left .unit condition, dataset, incoming, functions =>
      if conditionPrepared environment target condition functions then
        referenceEval environment target left dataset incoming functions
      else .failure
  | .leftJoin left right condition, dataset, incoming, functions =>
      environment.evalOpaque target (.leftJoin left right condition) dataset incoming functions
  | .minus .empty right, dataset, incoming, functions =>
      match target with
      | .rdf4jRuntime =>
          match referenceEval environment .rdf4jRuntime right dataset incoming functions with
          | .failure => .failure
          | .success _ => .success []
      | _ => .success []
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

/-- A condition that prepares under every function environment.

This is the condition-level analogue of `ReferenceSucceeds`. It is the premise an elision rule needs
when it discards a condition that `RDF4J_RUNTIME` would otherwise have precompiled. -/
def ConditionPrepares
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (condition : Option String) : Prop :=
  ∀ functions, conditionPrepared environment target condition functions = true

/-- Totality of an operand that only `RDF4J_RUNTIME` still prepares and evaluates after the elision.

The specification targets read `∅ ⟕ Ω = ∅` and `∅ − Ω = ∅` algebraically and never touch `Ω`, so they
carry no obligation. `RDF4J_RUNTIME` prepares and evaluates the elided operand regardless, so its
failure survives the rewrite. -/
def RuntimeOperandTotal
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (expression : Expr) : Prop :=
  match target with
  | .rdf4jRuntime => ReferenceSucceeds environment .rdf4jRuntime expression
  | _ => True

theorem runtimeOperandTotal_of_not_runtime
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (expression : Expr)
    (notRuntime : target ≠ .rdf4jRuntime) :
    RuntimeOperandTotal environment target expression := by
  cases target with
  | rdf4jRuntime => exact absurd rfl notRuntime
  | sparql11 => trivial
  | sparql12Draft20260605 => trivial
  | both11And12 => trivial

/-- Operands whose totality is decidable from the syntax alone.

`.empty` and `.unit` evaluate to a success in every environment, so an elision rule may discard them
even under `RDF4J_RUNTIME`. Anything else — including `.opaque` leaves standing for unmodelled RDF4J
subtrees — carries no such guarantee. -/
def triviallyTotal : Expr → Bool
  | .empty => true
  | .unit => true
  | _ => false

theorem runtimeOperandTotal_of_triviallyTotal
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (expression : Expr)
    (total : triviallyTotal expression = true) :
    RuntimeOperandTotal environment target expression := by
  cases target with
  | rdf4jRuntime =>
      show ReferenceSucceeds environment .rdf4jRuntime expression
      intro dataset incoming functions
      cases expression with
      | empty => exact ⟨[], rfl⟩
      | unit => exact ⟨[incoming], rfl⟩
      | _ => simp [triviallyTotal] at total
  | sparql11 => trivial
  | sparql12Draft20260605 => trivial
  | both11And12 => trivial

theorem conditionPrepares_none
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget) :
    ConditionPrepares environment target none := by
  intro functions
  exact conditionPrepared_none environment target functions

theorem conditionPrepares_of_not_runtime
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (condition : Option String)
    (notRuntime : target ≠ .rdf4jRuntime) :
    ConditionPrepares environment target condition := by
  intro functions
  exact conditionPrepared_of_not_runtime environment target condition functions notRuntime

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

theorem referenceEval_leftJoin_empty_left
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (right : Expr)
    (condition : Option String)
    (dataset : Dataset)
    (incoming : Mapping)
    (functions : FunctionEnvironment) :
    referenceEval environment target (.leftJoin .empty right condition) dataset incoming functions =
      if conditionPrepared environment target condition functions then
        match target with
        | .rdf4jRuntime =>
            match referenceEval environment .rdf4jRuntime right dataset incoming functions with
            | .failure => .failure
            | .success _ => .success []
        | _ => .success []
      else .failure := by
  cases right <;> rfl

theorem referenceEval_minus_empty_left
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (right : Expr)
    (dataset : Dataset)
    (incoming : Mapping)
    (functions : FunctionEnvironment) :
    referenceEval environment target (.minus .empty right) dataset incoming functions =
      match target with
      | .rdf4jRuntime =>
          match referenceEval environment .rdf4jRuntime right dataset incoming functions with
          | .failure => .failure
          | .success _ => .success []
      | _ => .success [] := by
  cases right <;> rfl

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
    (condition : Option String)
    (prepares : ConditionPrepares environment target condition)
    (total : RuntimeOperandTotal environment target right) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.leftJoin .empty right condition)
      .empty := by
  intro dataset incoming functions
  cases target with
  | rdf4jRuntime =>
      obtain ⟨rows, evaluated⟩ := total dataset incoming functions
      change observe observation
          (referenceEval environment .rdf4jRuntime (.leftJoin .empty right condition)
            dataset incoming functions) =
        observe observation (.success [])
      simp [referenceEval_leftJoin_empty_left, prepares functions, evaluated]
  | sparql11 =>
      simp [referenceModel, referenceEval_leftJoin_empty_left, referenceEval, prepares functions]
  | sparql12Draft20260605 =>
      simp [referenceModel, referenceEval_leftJoin_empty_left, referenceEval, prepares functions]
  | both11And12 =>
      constructor <;>
        simp [referenceModel, referenceEval_leftJoin_empty_left, referenceEval, prepares functions]

theorem reference_leftJoin_empty_right
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (left : Expr)
    (condition : Option String)
    (prepares : ConditionPrepares environment target condition) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.leftJoin left .empty condition)
      left := by
  intro dataset incoming functions
  cases left <;> cases target <;>
    simp [referenceModel, referenceEval, prepares functions]

theorem reference_leftJoin_unit_right
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (left : Expr)
    (condition : Option String)
    (prepares : ConditionPrepares environment target condition) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.leftJoin left .unit condition)
      left := by
  intro dataset incoming functions
  cases left <;> cases target <;>
    simp [referenceModel, referenceEval, prepares functions]

theorem reference_minus_empty_left
    (environment : ReferenceEnvironment)
    (target : SemanticsTarget)
    (observation : Observation)
    (right : Expr)
    (total : RuntimeOperandTotal environment target right) :
    Equivalent
      (referenceModel environment)
      target
      observation
      (.minus .empty right)
      .empty := by
  intro dataset incoming functions
  cases target with
  | rdf4jRuntime =>
      obtain ⟨rows, evaluated⟩ := total dataset incoming functions
      change observe observation
          (referenceEval environment .rdf4jRuntime (.minus .empty right) dataset incoming functions) =
        observe observation (.success [])
      rw [referenceEval_minus_empty_left, evaluated]
  | sparql11 =>
      simp [referenceModel, referenceEval_minus_empty_left, referenceEval]
  | sparql12Draft20260605 =>
      simp [referenceModel, referenceEval_minus_empty_left, referenceEval]
  | both11And12 =>
      constructor <;> simp [referenceModel, referenceEval_minus_empty_left, referenceEval]

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
