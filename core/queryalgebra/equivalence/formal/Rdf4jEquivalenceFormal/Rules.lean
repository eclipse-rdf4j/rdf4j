import Rdf4jEquivalenceFormal.Model

namespace Rdf4jEquivalenceFormal

inductive RuleId where
  | structuralEquality
  | joinAssociative | joinCommutative | joinUnitIdentity | joinEmptyAnnihilator
  | unionAssociative | unionCommutative | unionEmptyIdentity | unionIdempotentUnderSet
  | filterDistributesOverUnion | filterPushedToJoinLeft | filterPushedToJoinRight
  | filterPushedToLeftJoinLeft | filterTrueIdentity | filterFalseEmpty
  | leftJoinEmptyLeft | leftJoinEmptyRight | leftJoinUnitRight | leftJoinDistributesOverLeftUnion
  | minusEmptyLeft | minusEmptyRight | minusUnitRight | minusDisjointDomains
  | minusDistributesOverLeftUnion | minusRightUnionToChain | minusChainReorder
  | distinctIdempotent | distinctOrSetObservation | reducedHiddenBySetObservation
  | projectionElementOrderIgnored | valuesEmpty | valuesUnit
deriving Repr, DecidableEq

structure RuleProfile where
  target : SemanticsTarget
  observation : Observation
  context : IncomingContext
deriving Repr, DecidableEq

structure RuleInstance where
  rule : RuleId
  profile : RuleProfile
  before : Expr
  after : Expr
deriving Repr, DecidableEq

def RuleSound
    (model : SemanticModel)
    (ruleInstance : RuleInstance) : Prop :=
  EquivalentInContext
    model
    ruleInstance.profile.target
    ruleInstance.profile.observation
    ruleInstance.profile.context
    ruleInstance.before
    ruleInstance.after

structure RuleTheoremInventory (model : SemanticModel) where
  theoremFor : RuleInstance → Prop
  theoremForImpliesSound : ∀ ruleInstance, theoremFor ruleInstance → RuleSound model ruleInstance

theorem inventory_entry_sound
    {model : SemanticModel}
    (inventory : RuleTheoremInventory model)
    (ruleInstance : RuleInstance)
    (checked : inventory.theoremFor ruleInstance) :
    RuleSound model ruleInstance :=
  inventory.theoremForImpliesSound ruleInstance checked

end Rdf4jEquivalenceFormal
