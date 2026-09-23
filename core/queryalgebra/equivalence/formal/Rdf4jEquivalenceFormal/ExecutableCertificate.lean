import Lean.Data.Json
import Rdf4jEquivalenceFormal.Certificate
import Rdf4jEquivalenceFormal.ReferenceSemantics

namespace Rdf4jEquivalenceFormal

open Lean

abbrev JsonObject := Std.TreeMap.Raw String Json compare

inductive CheckedRewrite (profile : RuleProfile) : Expr → Expr → Type where
  | joinUnitLeft (notSequence : profile.observation ≠ .sequence) (arg : Expr) :
      CheckedRewrite profile (.join .unit arg) arg
  | joinUnitRight (notSequence : profile.observation ≠ .sequence) (arg : Expr) :
      CheckedRewrite profile (.join arg .unit) arg
  | unionEmptyLeft (notSequence : profile.observation ≠ .sequence) (arg : Expr) :
      CheckedRewrite profile (.union .empty arg) arg
  | unionEmptyRight (notSequence : profile.observation ≠ .sequence) (arg : Expr) :
      CheckedRewrite profile (.union arg .empty) arg
  -- `RDF4J_RUNTIME` prepares an elided condition and evaluates an elided operand regardless of the
  -- rewrite, so these rules carry a discharge obligation for whatever they discard. An absent
  -- condition has nothing to precompile and a `.empty`/`.unit` operand cannot fail, which keeps the
  -- rules available at runtime exactly when the elision is observationally free.
  | leftJoinEmptyLeft (notSequence : profile.observation ≠ .sequence)
      (right : Expr) (condition : Option String)
      (elisionFree : profile.target ≠ .rdf4jRuntime ∨
        (condition = none ∧ triviallyTotal right = true)) :
      CheckedRewrite profile (.leftJoin .empty right condition) .empty
  | leftJoinEmptyRight (notSequence : profile.observation ≠ .sequence)
      (left : Expr) (condition : Option String)
      (elisionFree : profile.target ≠ .rdf4jRuntime ∨ condition = none) :
      CheckedRewrite profile (.leftJoin left .empty condition) left
  | leftJoinUnitRight (notSequence : profile.observation ≠ .sequence)
      (left : Expr) (condition : Option String)
      (elisionFree : profile.target ≠ .rdf4jRuntime ∨ condition = none) :
      CheckedRewrite profile (.leftJoin left .unit condition) left
  | minusEmptyLeft (notSequence : profile.observation ≠ .sequence)
      (right : Expr)
      (elisionFree : profile.target ≠ .rdf4jRuntime ∨ triviallyTotal right = true) :
      CheckedRewrite profile (.minus .empty right) .empty
  | minusEmptyRight (notSequence : profile.observation ≠ .sequence) (left : Expr) :
      CheckedRewrite profile (.minus left .empty) left
  | filterTrue (arg : Expr) : CheckedRewrite profile (.filter "true" arg) arg
  | distinctIdempotent (arg : Expr) :
      CheckedRewrite profile (.distinct (.distinct arg)) (.distinct arg)
  | unionIdempotentSet (isSet : profile.observation = .set) (arg : Expr) :
      CheckedRewrite profile (.union arg arg) arg
  | unionIdempotentAsk (isAsk : profile.observation = .ask) (arg : Expr) :
      CheckedRewrite profile (.union arg arg) arg
  | distinctHiddenSet (isSet : profile.observation = .set) (arg : Expr) :
      CheckedRewrite profile (.distinct arg) arg
  | distinctHiddenAsk (isAsk : profile.observation = .ask) (arg : Expr) :
      CheckedRewrite profile (.distinct arg) arg
  | reducedHiddenSet (isSet : profile.observation = .set)
      (notRuntime : profile.target ≠ .rdf4jRuntime) (arg : Expr) :
      CheckedRewrite profile (.reduced arg) arg
  | reducedHiddenAsk (isAsk : profile.observation = .ask)
      (notRuntime : profile.target ≠ .rdf4jRuntime) (arg : Expr) :
      CheckedRewrite profile (.reduced arg) arg
  | valuesEmpty (notSequence : profile.observation ≠ .sequence) :
      CheckedRewrite profile (.values []) .empty
  | valuesUnitRuntime (notSequence : profile.observation ≠ .sequence)
      (isRuntime : profile.target = .rdf4jRuntime) :
      CheckedRewrite profile (.values [emptyMapping]) .unit
  | valuesUnitTopLevel (notSequence : profile.observation ≠ .sequence)
      (isTopLevel : profile.context = .topLevelEmpty) :
      CheckedRewrite profile (.values [emptyMapping]) .unit

theorem checked_rewrite_sound
    (environment : ReferenceEnvironment)
    {profile : RuleProfile}
    {before after : Expr}
    (checked : CheckedRewrite profile before after) :
    EquivalentInContext
      (referenceModel environment)
      profile.target
      profile.observation
      profile.context
      before
      after := by
  cases checked with
  | joinUnitLeft _ =>
      exact equivalent_in_every_context
        (reference_join_unit_left environment profile.target profile.observation _)
        profile.context
  | joinUnitRight _ =>
      exact equivalent_in_every_context
        (reference_join_unit_right environment profile.target profile.observation _)
        profile.context
  | unionEmptyLeft _ =>
      exact equivalent_in_every_context
        (reference_union_empty_left environment profile.target profile.observation _)
        profile.context
  | unionEmptyRight _ =>
      exact equivalent_in_every_context
        (reference_union_empty_right environment profile.target profile.observation _)
        profile.context
  | leftJoinEmptyLeft _ right condition elisionFree =>
      have prepares : ConditionPrepares environment profile.target condition := by
        rcases elisionFree with notRuntime | ⟨isNone, _⟩
        · exact conditionPrepares_of_not_runtime environment profile.target condition notRuntime
        · exact isNone ▸ conditionPrepares_none environment profile.target
      have total : RuntimeOperandTotal environment profile.target right := by
        rcases elisionFree with notRuntime | ⟨_, isTotal⟩
        · exact runtimeOperandTotal_of_not_runtime environment profile.target right notRuntime
        · exact runtimeOperandTotal_of_triviallyTotal environment profile.target right isTotal
      exact equivalent_in_every_context
        (reference_leftJoin_empty_left environment profile.target profile.observation right condition
          prepares total)
        profile.context
  | leftJoinEmptyRight _ _ condition elisionFree =>
      have prepares : ConditionPrepares environment profile.target condition := by
        rcases elisionFree with notRuntime | isNone
        · exact conditionPrepares_of_not_runtime environment profile.target condition notRuntime
        · exact isNone ▸ conditionPrepares_none environment profile.target
      exact equivalent_in_every_context
        (reference_leftJoin_empty_right environment profile.target profile.observation _ condition
          prepares)
        profile.context
  | leftJoinUnitRight _ _ condition elisionFree =>
      have prepares : ConditionPrepares environment profile.target condition := by
        rcases elisionFree with notRuntime | isNone
        · exact conditionPrepares_of_not_runtime environment profile.target condition notRuntime
        · exact isNone ▸ conditionPrepares_none environment profile.target
      exact equivalent_in_every_context
        (reference_leftJoin_unit_right environment profile.target profile.observation _ condition
          prepares)
        profile.context
  | minusEmptyLeft _ right elisionFree =>
      have total : RuntimeOperandTotal environment profile.target right := by
        rcases elisionFree with notRuntime | isTotal
        · exact runtimeOperandTotal_of_not_runtime environment profile.target right notRuntime
        · exact runtimeOperandTotal_of_triviallyTotal environment profile.target right isTotal
      exact equivalent_in_every_context
        (reference_minus_empty_left environment profile.target profile.observation right total)
        profile.context
  | minusEmptyRight _ =>
      exact equivalent_in_every_context
        (reference_minus_empty_right environment profile.target profile.observation _)
        profile.context
  | filterTrue =>
      exact equivalent_in_every_context
        (reference_filter_true_identity environment profile.target profile.observation _)
        profile.context
  | distinctIdempotent =>
      exact equivalent_in_every_context
        (reference_distinct_idempotent environment profile.target profile.observation _)
        profile.context
  | unionIdempotentSet isSet =>
      rw [isSet]
      exact equivalent_in_every_context
        (reference_union_set_idempotent environment profile.target _)
        profile.context
  | unionIdempotentAsk isAsk =>
      rw [isAsk]
      exact equivalent_in_every_context
        (reference_union_ask_idempotent environment profile.target _)
        profile.context
  | distinctHiddenSet isSet =>
      rw [isSet]
      exact equivalent_in_every_context
        (reference_distinct_set_hidden environment profile.target _)
        profile.context
  | distinctHiddenAsk isAsk =>
      rw [isAsk]
      exact equivalent_in_every_context
        (reference_distinct_ask_hidden environment profile.target _)
        profile.context
  | reducedHiddenSet isSet notRuntime =>
      rw [isSet]
      exact equivalent_in_every_context
        (reference_reduced_set_hidden environment profile.target notRuntime _)
        profile.context
  | reducedHiddenAsk isAsk notRuntime =>
      rw [isAsk]
      exact equivalent_in_every_context
        (reference_reduced_ask_hidden environment profile.target notRuntime _)
        profile.context
  | valuesEmpty _ =>
      exact equivalent_in_every_context
        (reference_values_empty environment profile.target profile.observation)
        profile.context
  | valuesUnitRuntime _ isRuntime =>
      rw [isRuntime]
      cases profile.context with
      | topLevelEmpty =>
          exact reference_values_unit_top_level environment .rdf4jRuntime profile.observation
      | allBindings =>
          exact reference_runtime_values_unit_all_bindings environment profile.observation
  | valuesUnitTopLevel _ isTopLevel =>
      rw [isTopLevel]
      exact reference_values_unit_top_level environment profile.target profile.observation

inductive CheckedTrace (profile : RuleProfile) : Expr → Expr → Type where
  | refl (expression : Expr) : CheckedTrace profile expression expression
  | next {before middle after : Expr}
      (head : CheckedRewrite profile before middle)
      (tail : CheckedTrace profile middle after) :
      CheckedTrace profile before after

theorem checked_trace_sound
    (environment : ReferenceEnvironment)
    {profile : RuleProfile}
    {before after : Expr}
    (trace : CheckedTrace profile before after) :
    EquivalentInContext
      (referenceModel environment)
      profile.target
      profile.observation
      profile.context
      before
      after := by
  induction trace with
  | refl expression =>
      exact equivalent_in_context_refl
        (referenceModel environment)
        profile.target
        profile.observation
        profile.context
        expression
  | next head tail inductionHypothesis =>
      exact equivalent_in_context_trans
        (checked_rewrite_sound environment head)
        inductionHypothesis

structure AcceptedExecutableCertificate where
  profile : RuleProfile
  original : Expr
  candidate : Expr
  canonical : Expr
  originalTrace : CheckedTrace profile original canonical
  candidateTrace : CheckedTrace profile candidate canonical

theorem accepted_executable_certificate_sound
    (environment : ReferenceEnvironment)
    (certificate : AcceptedExecutableCertificate) :
    EquivalentInContext
      (referenceModel environment)
      certificate.profile.target
      certificate.profile.observation
      certificate.profile.context
      certificate.original
      certificate.candidate :=
  equivalent_in_context_trans
    (checked_trace_sound environment certificate.originalTrace)
    (equivalent_in_context_symm (checked_trace_sound environment certificate.candidateTrace))

private structure Augmentation where
  path : String
  json : Json

private structure ParsedExpr where
  expression : Expr
  knownPaths : List String
  opaquePrefixes : List String

private structure DecodedStep where
  rule : RuleId
  nodePath : String
  parameters : JsonObject
  resultingSubtree : String

private structure StepResult (profile : RuleProfile) (before : Expr) where
  after : Expr
  proof : CheckedRewrite profile before after

private structure TraceResult (profile : RuleProfile) (before : Expr) where
  after : Expr
  proof : CheckedTrace profile before after

private def required (object : JsonObject) (name : String) : Except String Json :=
  match object.get? name with
  | some value => pure value
  | none => throw s!"missing {name}"

private def requiredString (object : JsonObject) (name : String) : Except String String := do
  (← required object name).getStr?

private def requiredNat (object : JsonObject) (name : String) : Except String Nat := do
  (← required object name).getNat?

private def fieldsExactly (object : JsonObject) (expected : List String) : Bool :=
  object.toList.length == expected.length &&
    object.toList.all (fun field => expected.contains field.1)

private def requireFields (object : JsonObject) (expected : List String) : Except String Unit :=
  if fieldsExactly object expected then pure ()
  else throw s!"unexpected object fields; expected {expected}"

private def parseAugmentations (json : Json) : Except String (List Augmentation) := do
  let array ← json.getArr?
  let mut result := []
  for value in array do
    let object ← value.getObj?
    let path ← requiredString object "path"
    if !path.startsWith "$" then
      throw s!"invalid augmentation path {path}"
    result := { path := path, json := value } :: result
  pure result.reverse

private def isUnder (rootPath path : String) : Bool :=
  path == rootPath || path.startsWith (rootPath ++ "/")

private def localAugmentations (path : String) (augmentations : List Augmentation) : List Augmentation :=
  augmentations.filter (fun augmentation => augmentation.path == path)

private def binaryAugmentationOk (path : String) (augmentations : List Augmentation) : Bool :=
  match localAugmentations path augmentations with
  | [augmentation] =>
      match augmentation.json with
      | .obj object =>
          fieldsExactly object ["path", "type", "algorithmName"] &&
            object.get? "path" == some (.str path) &&
            object.get? "type" == some (.str "BinaryTupleOperator") &&
            object.get? "algorithmName" == some .null
      | _ => false
  | _ => false

private def noLocalAugmentation (path : String) (augmentations : List Augmentation) : Bool :=
  (localAugmentations path augmentations).isEmpty

private def normalizedAugmentations
    (rootPath : String)
    (augmentations : List Augmentation) : List Json :=
  augmentations.filterMap fun augmentation =>
    if isUnder rootPath augmentation.path then
      match augmentation.json with
      | .obj object =>
          let suffix := augmentation.path.drop rootPath.length
          some (.obj (object.insert "path" (.str ("$" ++ suffix))))
      | _ => none
    else none

private def opaqueIdentity
    (json : Json)
    (path : String)
    (augmentations : List Augmentation) : String :=
  Json.compress <| Json.mkObj [
    ("expression", json),
    ("augmentations", .arr (normalizedAugmentations path augmentations).toArray)
  ]

private def opaqueExpr
    (json : Json)
    (path : String)
    (augmentations : List Augmentation) : ParsedExpr :=
  {
    expression := .opaque (opaqueIdentity json path augmentations)
    knownPaths := []
    opaquePrefixes := [path]
  }

private def combineBinary
    (constructor : Expr → Expr → Expr)
    (path : String)
    (left right : ParsedExpr) : ParsedExpr :=
  {
    expression := constructor left.expression right.expression
    knownPaths := path :: (left.knownPaths ++ right.knownPaths)
    opaquePrefixes := left.opaquePrefixes ++ right.opaquePrefixes
  }

private def combineUnary
    (constructor : Expr → Expr)
    (path : String)
    (arg : ParsedExpr) : ParsedExpr :=
  {
    expression := constructor arg.expression
    knownPaths := path :: arg.knownPaths
    opaquePrefixes := arg.opaquePrefixes
  }

private def rowIsEmptyMapping (json : Json) : Bool :=
  match json with
  | .obj object => object.toList.all (fun field => field.2.isNull)
  | _ => false

private def booleanCondition? (json : Json) : Option String :=
  match json with
  | .obj condition =>
      if !fieldsExactly condition ["type", "value"] ||
          condition.get? "type" != some (.str "ValueConstant") then
        none
      else
        match condition.get? "value" with
        | some (.obj value) =>
            if fieldsExactly value ["type", "label", "datatype"] &&
                value.get? "type" == some (.str "Literal") &&
                value.get? "datatype" == some (.str "http://www.w3.org/2001/XMLSchema#boolean") then
              match value.get? "label" with
              | some (.str "true") => some "true"
              | some (.str "false") => some "false"
              | _ => none
            else none
        | _ => none
  | _ => none

private partial def parseExpr
    (json : Json)
    (path : String)
    (augmentations : List Augmentation) : Except String ParsedExpr := do
  let object ← json.getObj?
  let type ← requiredString object "type"
  match type with
  | "EmptySet" =>
      if fieldsExactly object ["type"] && noLocalAugmentation path augmentations then
        pure { expression := .empty, knownPaths := [path], opaquePrefixes := [] }
      else pure (opaqueExpr json path augmentations)
  | "SingletonSet" =>
      if fieldsExactly object ["type"] && noLocalAugmentation path augmentations then
        pure { expression := .unit, knownPaths := [path], opaquePrefixes := [] }
      else pure (opaqueExpr json path augmentations)
  | "BindingSetAssignment" =>
      if fieldsExactly object ["type", "declaredBindingNames", "bindingSets"] &&
          noLocalAugmentation path augmentations then
        let rows ← (← required object "bindingSets").getArr?
        if rows.isEmpty then
          pure { expression := .values [], knownPaths := [path], opaquePrefixes := [] }
        else if rows.size == 1 && rowIsEmptyMapping rows[0]! then
          pure { expression := .values [emptyMapping], knownPaths := [path], opaquePrefixes := [] }
        else pure (opaqueExpr json path augmentations)
      else pure (opaqueExpr json path augmentations)
  | "Join" =>
      if fieldsExactly object ["type", "leftArg", "rightArg"] &&
          binaryAugmentationOk path augmentations then
        let left ← parseExpr (← required object "leftArg") (path ++ "/0") augmentations
        let right ← parseExpr (← required object "rightArg") (path ++ "/1") augmentations
        pure (combineBinary Expr.join path left right)
      else pure (opaqueExpr json path augmentations)
  | "Union" =>
      if fieldsExactly object ["type", "leftArg", "rightArg"] &&
          binaryAugmentationOk path augmentations then
        let left ← parseExpr (← required object "leftArg") (path ++ "/0") augmentations
        let right ← parseExpr (← required object "rightArg") (path ++ "/1") augmentations
        pure (combineBinary Expr.union path left right)
      else pure (opaqueExpr json path augmentations)
  | "Difference" =>
      if fieldsExactly object ["type", "leftArg", "rightArg"] &&
          binaryAugmentationOk path augmentations then
        let left ← parseExpr (← required object "leftArg") (path ++ "/0") augmentations
        let right ← parseExpr (← required object "rightArg") (path ++ "/1") augmentations
        pure (combineBinary Expr.minus path left right)
      else pure (opaqueExpr json path augmentations)
  | "LeftJoin" =>
      if (fieldsExactly object ["type", "leftArg", "rightArg"] ||
          fieldsExactly object ["type", "leftArg", "rightArg", "condition"]) &&
          binaryAugmentationOk path augmentations then
        let condition := object.get? "condition" |>.map Json.compress
        let leftPath := path ++ (if condition.isSome then "/1" else "/0")
        let rightPath := path ++ (if condition.isSome then "/2" else "/1")
        let left ← parseExpr (← required object "leftArg") leftPath augmentations
        let right ← parseExpr (← required object "rightArg") rightPath augmentations
        pure {
          expression := .leftJoin left.expression right.expression condition
          knownPaths := path :: (left.knownPaths ++ right.knownPaths)
          opaquePrefixes := left.opaquePrefixes ++ right.opaquePrefixes ++
            (if condition.isSome then [path ++ "/0"] else [])
        }
      else pure (opaqueExpr json path augmentations)
  | "Filter" =>
      if fieldsExactly object ["type", "arg", "condition"] &&
          noLocalAugmentation path augmentations then
        let conditionJson ← required object "condition"
        match booleanCondition? conditionJson with
        | some condition =>
            -- RDF4J Filter.visitChildren visits the condition before the tuple argument.
            let arg ← parseExpr (← required object "arg") (path ++ "/1") augmentations
            pure {
              expression := .filter condition arg.expression
              knownPaths := path :: arg.knownPaths
              opaquePrefixes := arg.opaquePrefixes
            }
        | none => pure (opaqueExpr json path augmentations)
      else pure (opaqueExpr json path augmentations)
  | "Distinct" =>
      if fieldsExactly object ["type", "arg"] && noLocalAugmentation path augmentations then
        pure (combineUnary Expr.distinct path
          (← parseExpr (← required object "arg") (path ++ "/0") augmentations))
      else pure (opaqueExpr json path augmentations)
  | "Reduced" =>
      if fieldsExactly object ["type", "arg"] && noLocalAugmentation path augmentations then
        pure (combineUnary Expr.reduced path
          (← parseExpr (← required object "arg") (path ++ "/0") augmentations))
      else pure (opaqueExpr json path augmentations)
  | _ => pure (opaqueExpr json path augmentations)

private def covered (parsed : ParsedExpr) (path : String) : Bool :=
  parsed.knownPaths.contains path || parsed.opaquePrefixes.any (fun rootPath => isUnder rootPath path)

private def parseFormalTree (json : Json) : Except String Expr := do
  let tree ← json.getObj?
  requireFields tree ["schema", "version", "algebra", "augmentations"]
  if (← requiredString tree "schema") != "rdf4j-equivalence-certificate-tree" then
    throw "unsupported formal tree schema"
  if (← requiredNat tree "version") != 1 then
    throw "unsupported formal tree version"
  let algebra ← (← required tree "algebra").getObj?
  requireFields algebra ["format", "version", "expression"]
  if (← requiredString algebra "format") != "rdf4j-tuple-expr" then
    throw "unsupported native algebra format"
  if (← requiredNat algebra "version") != 1 then
    throw "unsupported native algebra version"
  let augmentations ← parseAugmentations (← required tree "augmentations")
  let parsed ← parseExpr (← required algebra "expression") "$" augmentations
  for augmentation in augmentations do
    if !covered parsed augmentation.path then
      throw s!"augmentation path {augmentation.path} does not resolve in the parsed source tree"
  pure parsed.expression

private def parseTarget : String → Except String SemanticsTarget
  | "RDF4J_RUNTIME" => pure .rdf4jRuntime
  | "SPARQL_1_1" => pure .sparql11
  | "SPARQL_1_2_DRAFT" => pure .sparql12Draft20260605
  | "BOTH_1_1_AND_1_2" => pure .both11And12
  | value => throw s!"unsupported semantics target {value}"

private def parseObservation : String → Except String Observation
  | "BAG" => pure .bag
  | "SET" => pure .set
  | "SEQUENCE" => pure .sequence
  | "ASK" => pure .ask
  | value => throw s!"unsupported observation mode {value}"

private def parseContext : String → Except String IncomingContext
  | "TOP_LEVEL_EMPTY" => pure .topLevelEmpty
  | "ALL_BINDINGS" => pure .allBindings
  | value => throw s!"unsupported context mode {value}"

private def parseRule : String → Except String RuleId
  | "JOIN_UNIT_IDENTITY" => pure .joinUnitIdentity
  | "UNION_EMPTY_IDENTITY" => pure .unionEmptyIdentity
  | "LEFT_JOIN_EMPTY_LEFT" => pure .leftJoinEmptyLeft
  | "LEFT_JOIN_EMPTY_RIGHT" => pure .leftJoinEmptyRight
  | "LEFT_JOIN_UNIT_RIGHT" => pure .leftJoinUnitRight
  | "MINUS_EMPTY_LEFT" => pure .minusEmptyLeft
  | "MINUS_EMPTY_RIGHT" => pure .minusEmptyRight
  | "FILTER_TRUE_IDENTITY" => pure .filterTrueIdentity
  | "DISTINCT_IDEMPOTENT" => pure .distinctIdempotent
  | "UNION_IDEMPOTENT_UNDER_SET_OBSERVATION" => pure .unionIdempotentUnderSet
  | "DISTINCT_OR_SET_OBSERVATION" => pure .distinctOrSetObservation
  | "REDUCED_HIDDEN_BY_SET_OBSERVATION" => pure .reducedHiddenBySetObservation
  | "VALUES_EMPTY" => pure .valuesEmpty
  | "VALUES_UNIT" => pure .valuesUnit
  | value => throw s!"rule {value} is not in the executable theorem registry"

private def parseStep (json : Json) : Except String DecodedStep := do
  let object ← json.getObj?
  requireFields object ["rule", "nodePath", "parameters", "resultingSubtree"]
  pure {
    rule := ← parseRule (← requiredString object "rule")
    nodePath := ← requiredString object "nodePath"
    parameters := ← (← required object "parameters").getObj?
    resultingSubtree := ← requiredString object "resultingSubtree"
  }

private def parseSteps (json : Json) : Except String (List DecodedStep) := do
  let array ← json.getArr?
  let mut result := []
  for value in array do
    result := (← parseStep value) :: result
  pure result.reverse

private def parametersOk (step : DecodedStep) : Bool :=
  match step.rule with
  | .distinctIdempotent | .unionIdempotentUnderSet =>
      fieldsExactly step.parameters ["multiplicity"] &&
        step.parameters.get? "multiplicity" == some (.str "deduplicate")
  | _ => fieldsExactly step.parameters []

private def checkStep
    (profile : RuleProfile)
    (rule : RuleId)
    (before : Expr) : Option (StepResult profile before) :=
  match rule, before with
  | .joinUnitIdentity, .join .unit arg =>
      if h : profile.observation ≠ .sequence then
        some { after := arg, proof := .joinUnitLeft h arg }
      else none
  | .joinUnitIdentity, .join arg .unit =>
      if h : profile.observation ≠ .sequence then
        some { after := arg, proof := .joinUnitRight h arg }
      else none
  | .unionEmptyIdentity, .union .empty arg =>
      if h : profile.observation ≠ .sequence then
        some { after := arg, proof := .unionEmptyLeft h arg }
      else none
  | .unionEmptyIdentity, .union arg .empty =>
      if h : profile.observation ≠ .sequence then
        some { after := arg, proof := .unionEmptyRight h arg }
      else none
  | .leftJoinEmptyLeft, .leftJoin .empty right condition =>
      if h : profile.observation ≠ .sequence then
        if free : profile.target ≠ .rdf4jRuntime ∨
            (condition = none ∧ triviallyTotal right = true) then
          some { after := .empty, proof := .leftJoinEmptyLeft h right condition free }
        else none
      else none
  | .leftJoinEmptyRight, .leftJoin left .empty condition =>
      if h : profile.observation ≠ .sequence then
        if free : profile.target ≠ .rdf4jRuntime ∨ condition = none then
          some { after := left, proof := .leftJoinEmptyRight h left condition free }
        else none
      else none
  | .leftJoinUnitRight, .leftJoin left .unit condition =>
      if h : profile.observation ≠ .sequence then
        if free : profile.target ≠ .rdf4jRuntime ∨ condition = none then
          some { after := left, proof := .leftJoinUnitRight h left condition free }
        else none
      else none
  | .minusEmptyLeft, .minus .empty right =>
      if h : profile.observation ≠ .sequence then
        if free : profile.target ≠ .rdf4jRuntime ∨ triviallyTotal right = true then
          some { after := .empty, proof := .minusEmptyLeft h right free }
        else none
      else none
  | .minusEmptyRight, .minus left .empty =>
      if h : profile.observation ≠ .sequence then
        some { after := left, proof := .minusEmptyRight h left }
      else none
  | .filterTrueIdentity, .filter "true" arg =>
      some { after := arg, proof := .filterTrue arg }
  | .distinctIdempotent, .distinct (.distinct arg) =>
      some { after := .distinct arg, proof := .distinctIdempotent arg }
  | .unionIdempotentUnderSet, .union left right =>
      if equal : left = right then
        if isSet : profile.observation = .set then
          some { after := left, proof := equal ▸ .unionIdempotentSet isSet left }
        else if isAsk : profile.observation = .ask then
          some { after := left, proof := equal ▸ .unionIdempotentAsk isAsk left }
        else none
      else none
  | .distinctOrSetObservation, .distinct arg =>
      if isSet : profile.observation = .set then
        some { after := arg, proof := .distinctHiddenSet isSet arg }
      else if isAsk : profile.observation = .ask then
        some { after := arg, proof := .distinctHiddenAsk isAsk arg }
      else none
  | .reducedHiddenBySetObservation, .reduced arg =>
      if notRuntime : profile.target ≠ .rdf4jRuntime then
        if isSet : profile.observation = .set then
          some { after := arg, proof := .reducedHiddenSet isSet notRuntime arg }
        else if isAsk : profile.observation = .ask then
          some { after := arg, proof := .reducedHiddenAsk isAsk notRuntime arg }
        else none
      else none
  | .valuesEmpty, .values [] =>
      if h : profile.observation ≠ .sequence then
        some { after := .empty, proof := .valuesEmpty h }
      else none
  | .valuesUnit, .values [row] =>
      if rowEquals : row = emptyMapping then
        if notSequence : profile.observation ≠ .sequence then
          if isRuntime : profile.target = .rdf4jRuntime then
            let proof : CheckedRewrite profile (.values [row]) .unit := by
              rw [rowEquals]
              exact .valuesUnitRuntime notSequence isRuntime
            some { after := .unit, proof := proof }
          else if isTopLevel : profile.context = .topLevelEmpty then
            let proof : CheckedRewrite profile (.values [row]) .unit := by
              rw [rowEquals]
              exact .valuesUnitTopLevel notSequence isTopLevel
            some { after := .unit, proof := proof }
          else none
        else none
      else none
  | _, _ => none

private def checkTrace
    (profile : RuleProfile)
    (before : Expr)
    (steps : List DecodedStep)
    (canonicalEncoding : String) : Except String (TraceResult profile before) := do
  match steps with
  | [] => pure { after := before, proof := .refl before }
  | step :: rest =>
      if step.nodePath != "$" then
        throw s!"only root-local theorem instances are certified; found {step.nodePath}"
      if !parametersOk step then
        throw "rule parameters do not match the executable rule schema"
      if step.resultingSubtree.isEmpty then
        throw "claimed resulting subtree is empty"
      let checked ← match checkStep profile step.rule before with
        | some result => pure result
        | none => throw s!"rule {repr step.rule} does not apply under the claimed profile"
      let tail ← checkTrace profile checked.after rest canonicalEncoding
      pure { after := tail.after, proof := .next checked.proof tail.proof }

def checkCertificateJson (json : Json) : Except String AcceptedExecutableCertificate := do
  let object ← json.getObj?
  requireFields object [
    "formatVersion", "semanticsTarget", "observationMode", "contextMode",
    "originalTree", "candidateTree", "canonicalFingerprint", "canonicalEncoding",
    "originalSteps", "candidateSteps"
  ]
  let version ← requiredNat object "formatVersion"
  if version != 2 then
    throw s!"unsupported certificate format version {version}"
  let profile : RuleProfile := {
    target := ← parseTarget (← requiredString object "semanticsTarget")
    observation := ← parseObservation (← requiredString object "observationMode")
    context := ← parseContext (← requiredString object "contextMode")
  }
  let original ← parseFormalTree (← required object "originalTree")
  let candidate ← parseFormalTree (← required object "candidateTree")
  let canonicalFingerprint ← requiredString object "canonicalFingerprint"
  let canonicalEncoding ← requiredString object "canonicalEncoding"
  if !canonicalFingerprint.startsWith "sha256:" || canonicalFingerprint.length != 71 then
    throw "malformed canonical fingerprint"
  if canonicalEncoding.isEmpty then
    throw "empty canonical encoding"
  let originalSteps ← parseSteps (← required object "originalSteps")
  let candidateSteps ← parseSteps (← required object "candidateSteps")
  let originalTrace ← checkTrace profile original originalSteps canonicalEncoding
  let candidateTrace ← checkTrace profile candidate candidateSteps canonicalEncoding
  if sameCanonical : originalTrace.after = candidateTrace.after then
    let originalProof : CheckedTrace profile original candidateTrace.after := by
      rw [← sameCanonical]
      exact originalTrace.proof
    pure {
      profile := profile
      original := original
      candidate := candidate
      canonical := candidateTrace.after
      originalTrace := originalProof
      candidateTrace := candidateTrace.proof
    }
  else
    throw "independently replayed formal trees do not converge"

def checkCertificateLine (line : String) : Except String AcceptedExecutableCertificate := do
  checkCertificateJson (← Json.parse line)

#print axioms checked_rewrite_sound
#print axioms checked_trace_sound
#print axioms accepted_executable_certificate_sound

end Rdf4jEquivalenceFormal
