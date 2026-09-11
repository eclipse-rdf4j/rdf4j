import Rdf4jEquivalenceFormal.Rules

namespace Rdf4jEquivalenceFormal

structure CheckedStep (model : SemanticModel) where
  ruleInstance : RuleInstance
  sound : RuleSound model ruleInstance

structure CheckedNormalizationCertificate (model : SemanticModel) where
  profile : RuleProfile
  original : Expr
  candidate : Expr
  canonical : Expr
  originalSound : EquivalentInContext model profile.target profile.observation profile.context original canonical
  candidateSound : EquivalentInContext model profile.target profile.observation profile.context candidate canonical

def KernelAccepts
    (model : SemanticModel)
    (original candidate : Expr)
    (certificate : CheckedNormalizationCertificate model) : Prop :=
  certificate.original = original ∧ certificate.candidate = candidate

theorem accepted_certificate_sound
    {model : SemanticModel}
    {original candidate : Expr}
    (certificate : CheckedNormalizationCertificate model)
    (accepted : KernelAccepts model original candidate certificate) :
    EquivalentInContext
      model
      certificate.profile.target
      certificate.profile.observation
      certificate.profile.context
      original
      candidate := by
  rcases accepted with ⟨rfl, rfl⟩
  exact equivalent_in_context_trans
    certificate.originalSound
    (equivalent_in_context_symm certificate.candidateSound)

#print axioms equivalent_refl
#print axioms equivalent_symm
#print axioms equivalent_trans
#print axioms equivalent_in_every_context
#print axioms equivalent_in_context_refl
#print axioms equivalent_in_context_symm
#print axioms equivalent_in_context_trans
#print axioms inventory_entry_sound
#print axioms accepted_certificate_sound

end Rdf4jEquivalenceFormal
