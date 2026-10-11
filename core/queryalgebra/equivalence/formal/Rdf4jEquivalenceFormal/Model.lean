import Std.Data.ExtTreeMap.Lemmas

namespace Rdf4jEquivalenceFormal

inductive RdfTerm where
  | iri (value : String)
  | blank (identifier : String)
  | literal (lexical datatype language direction : String)
  | triple (subject predicate object : RdfTerm)
deriving Repr, DecidableEq, BEq, ReflBEq, LawfulBEq

abbrev Mapping := Std.ExtTreeMap String RdfTerm

abbrev Bag := Mapping → Nat
abbrev SolutionSequence := List Mapping

inductive EvalOutcome where
  | success (rows : SolutionSequence)
  | failure

inductive Observation where
  | bag
  | set
  | sequence
  | ask
deriving Repr, DecidableEq

inductive ObservedOutcome where
  | bag (multiplicity : Bag)
  | set (support : Mapping → Bool)
  | sequence (rows : SolutionSequence)
  | ask (nonempty : Bool)
  | failure

def observe (mode : Observation) : EvalOutcome → ObservedOutcome
  | .failure => .failure
  | .success rows =>
      match mode with
      | .bag => .bag (fun mapping => rows.count mapping)
      | .set => .set (fun mapping => rows.contains mapping)
      | .sequence => .sequence rows
      | .ask => .ask (!rows.isEmpty)

structure Dataset where
  defaultGraph : List (RdfTerm × RdfTerm × RdfTerm)
  namedGraphs : List (RdfTerm × List (RdfTerm × RdfTerm × RdfTerm))
deriving Repr, DecidableEq

inductive FunctionPurity where
  | pure
  | impure
deriving Repr, DecidableEq

structure FunctionSemantics where
  purity : FunctionPurity
  apply : List RdfTerm → Option RdfTerm

abbrev FunctionEnvironment := String → Option FunctionSemantics

inductive Expr where
  | opaque (schema : String)
  | empty
  | unit
  | values (rows : List Mapping)
  | join (left right : Expr)
  | union (left right : Expr)
  | filter (condition : String) (arg : Expr)
  | leftJoin (left right : Expr) (condition : Option String)
  | minus (left right : Expr)
  | distinct (arg : Expr)
  | reduced (arg : Expr)
  | projection (columns : List (String × String)) (arg : Expr)
  | extension (assignments : List (String × String)) (arg : Expr)
  | order (comparators : List String) (arg : Expr)
  | slice (offset limit : Nat) (arg : Expr)
deriving Repr, DecidableEq

inductive SemanticsTarget where
  | rdf4jRuntime
  | sparql11
  | sparql12Draft20260605
  | both11And12
deriving Repr, DecidableEq

inductive IncomingContext where
  | topLevelEmpty
  | allBindings
deriving Repr, DecidableEq

def contextPermits : IncomingContext → Mapping → Prop
  | .topLevelEmpty, incoming => incoming = ∅
  | .allBindings, _ => True

def specificationVersionIri : SemanticsTarget → Option String
  | .sparql11 => some "https://www.w3.org/TR/2013/REC-sparql11-query-20130321/"
  | .sparql12Draft20260605 => some "https://www.w3.org/TR/2026/WD-sparql12-query-20260605/"
  | _ => none

structure SemanticModel where
  rdf4jRuntime : Expr → Dataset → Mapping → FunctionEnvironment → EvalOutcome
  sparql11 : Expr → Dataset → Mapping → FunctionEnvironment → EvalOutcome
  sparql12 : Expr → Dataset → Mapping → FunctionEnvironment → EvalOutcome

def Equivalent
    (model : SemanticModel)
    (target : SemanticsTarget)
    (observation : Observation)
    (left right : Expr) : Prop :=
  ∀ dataset incoming functions,
    match target with
    | .rdf4jRuntime =>
        observe observation (model.rdf4jRuntime left dataset incoming functions) =
          observe observation (model.rdf4jRuntime right dataset incoming functions)
    | .sparql11 =>
        observe observation (model.sparql11 left dataset incoming functions) =
          observe observation (model.sparql11 right dataset incoming functions)
    | .sparql12Draft20260605 =>
        observe observation (model.sparql12 left dataset incoming functions) =
          observe observation (model.sparql12 right dataset incoming functions)
    | .both11And12 =>
        (observe observation (model.sparql11 left dataset incoming functions) =
            observe observation (model.sparql11 right dataset incoming functions)) ∧
          (observe observation (model.sparql12 left dataset incoming functions) =
            observe observation (model.sparql12 right dataset incoming functions))

def EquivalentInContext
    (model : SemanticModel)
    (target : SemanticsTarget)
    (observation : Observation)
    (context : IncomingContext)
    (left right : Expr) : Prop :=
  ∀ dataset incoming functions,
    contextPermits context incoming →
      match target with
      | .rdf4jRuntime =>
          observe observation (model.rdf4jRuntime left dataset incoming functions) =
            observe observation (model.rdf4jRuntime right dataset incoming functions)
      | .sparql11 =>
          observe observation (model.sparql11 left dataset incoming functions) =
            observe observation (model.sparql11 right dataset incoming functions)
      | .sparql12Draft20260605 =>
          observe observation (model.sparql12 left dataset incoming functions) =
            observe observation (model.sparql12 right dataset incoming functions)
      | .both11And12 =>
          (observe observation (model.sparql11 left dataset incoming functions) =
              observe observation (model.sparql11 right dataset incoming functions)) ∧
            (observe observation (model.sparql12 left dataset incoming functions) =
              observe observation (model.sparql12 right dataset incoming functions))

theorem equivalent_refl
    (model : SemanticModel)
    (target : SemanticsTarget)
    (observation : Observation)
    (expression : Expr) :
    Equivalent model target observation expression expression := by
  intro dataset incoming functions
  cases target <;> simp

theorem equivalent_symm
    {model : SemanticModel}
    {target : SemanticsTarget}
    {observation : Observation}
    {left right : Expr}
    (proof : Equivalent model target observation left right) :
    Equivalent model target observation right left := by
  intro dataset incoming functions
  specialize proof dataset incoming functions
  cases target <;> simp_all

theorem equivalent_trans
    {model : SemanticModel}
    {target : SemanticsTarget}
    {observation : Observation}
    {left middle right : Expr}
    (leftProof : Equivalent model target observation left middle)
    (rightProof : Equivalent model target observation middle right) :
    Equivalent model target observation left right := by
  intro dataset incoming functions
  specialize leftProof dataset incoming functions
  specialize rightProof dataset incoming functions
  cases target <;> simp_all

theorem equivalent_in_every_context
    {model : SemanticModel}
    {target : SemanticsTarget}
    {observation : Observation}
    {left right : Expr}
    (proof : Equivalent model target observation left right)
    (context : IncomingContext) :
    EquivalentInContext model target observation context left right := by
  intro dataset incoming functions permitted
  specialize proof dataset incoming functions
  cases target <;> simpa using proof

theorem equivalent_in_context_refl
    (model : SemanticModel)
    (target : SemanticsTarget)
    (observation : Observation)
    (context : IncomingContext)
    (expression : Expr) :
    EquivalentInContext model target observation context expression expression := by
  intro dataset incoming functions permitted
  cases target <;> simp

theorem equivalent_in_context_symm
    {model : SemanticModel}
    {target : SemanticsTarget}
    {observation : Observation}
    {context : IncomingContext}
    {left right : Expr}
    (proof : EquivalentInContext model target observation context left right) :
    EquivalentInContext model target observation context right left := by
  intro dataset incoming functions permitted
  specialize proof dataset incoming functions permitted
  cases target <;> simp_all

theorem equivalent_in_context_trans
    {model : SemanticModel}
    {target : SemanticsTarget}
    {observation : Observation}
    {context : IncomingContext}
    {left middle right : Expr}
    (leftProof : EquivalentInContext model target observation context left middle)
    (rightProof : EquivalentInContext model target observation context middle right) :
    EquivalentInContext model target observation context left right := by
  intro dataset incoming functions permitted
  specialize leftProof dataset incoming functions permitted
  specialize rightProof dataset incoming functions permitted
  cases target <;> simp_all

end Rdf4jEquivalenceFormal
