import Rdf4jEquivalenceFormal.ExecutableCertificate

namespace Rdf4jEquivalenceFormal

private def sameObserved (mode : Observation) : EvalOutcome → EvalOutcome → Bool
  | .failure, .failure => true
  | .success left, .success right =>
      match mode with
      | .sequence => left == right
      | .ask => left.isEmpty == right.isEmpty
      | .set =>
          (left ++ right).all fun mapping =>
            left.contains mapping == right.contains mapping
      | .bag =>
          (left ++ right).all fun mapping =>
            left.count mapping == right.count mapping
  | _, _ => false

private def oracleTerm : RdfTerm := .iri "urn:rdf4j:equivalence:oracle"

private def oracleMapping : Mapping :=
  (∅ : Mapping).insert "x" oracleTerm

private def emptyDataset : Dataset := {
  defaultGraph := []
  namedGraphs := []
}

private def populatedDataset : Dataset := {
  defaultGraph := [(oracleTerm, .iri "urn:rdf4j:equivalence:p", oracleTerm)]
  namedGraphs := [(.iri "urn:rdf4j:equivalence:g", [])]
}

private def pureFunction : FunctionSemantics := {
  purity := .pure
  apply := fun _ => some oracleTerm
}

private def impureFunction : FunctionSemantics := {
  purity := .impure
  apply := fun _ => none
}

private def emptyFunctions : FunctionEnvironment := fun _ => none

private def pureFunctions : FunctionEnvironment := fun name =>
  if name == "urn:rdf4j:equivalence:pure" then some pureFunction else none

private def impureFunctions : FunctionEnvironment := fun name =>
  if name == "urn:rdf4j:equivalence:impure" then some impureFunction else none

private def oracleEnvironment (variant : Nat) : ReferenceEnvironment where
  evalOpaque := fun _ _ _ incoming _ =>
    match variant with
    | 0 => .success []
    | 1 => .success [incoming]
    | 2 => .success [incoming, incoming]
    | _ => .failure
  evalCondition := fun _ condition _ _ => condition.length % 2 == variant % 2

private def constituentTargets : SemanticsTarget → List SemanticsTarget
  | .both11And12 => [.sparql11, .sparql12Draft20260605]
  | target => [target]

private def permittedIncoming : IncomingContext → List Mapping
  | .topLevelEmpty => [emptyMapping]
  | .allBindings => [emptyMapping, oracleMapping]

private def environments : List ReferenceEnvironment :=
  [oracleEnvironment 0, oracleEnvironment 1, oracleEnvironment 2, oracleEnvironment 3]

private def datasets : List Dataset := [emptyDataset, populatedDataset]

private def functionEnvironments : List FunctionEnvironment :=
  [emptyFunctions, pureFunctions, impureFunctions]

def oracleCaseCount (certificate : AcceptedExecutableCertificate) : Nat :=
  (constituentTargets certificate.profile.target).length *
    environments.length *
    datasets.length *
    (permittedIncoming certificate.profile.context).length *
    functionEnvironments.length

def finiteOracleAgreement (certificate : AcceptedExecutableCertificate) : Bool :=
  (constituentTargets certificate.profile.target).all fun target =>
    environments.all fun environment =>
      datasets.all fun dataset =>
        (permittedIncoming certificate.profile.context).all fun incoming =>
          functionEnvironments.all fun functions =>
            sameObserved certificate.profile.observation
              (referenceEval environment target certificate.original dataset incoming functions)
              (referenceEval environment target certificate.candidate dataset incoming functions)

def checkCertificateLineWithOracle
    (line : String) : Except String AcceptedExecutableCertificate := do
  let certificate ← checkCertificateLine line
  if finiteOracleAgreement certificate then
    pure certificate
  else
    throw "finite formal reference evaluator found a distinguishing outcome"

end Rdf4jEquivalenceFormal
