package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.tempPlanNodes.TupleValidationPlanNode;

public class TargetSubjectsOf extends Target {

	private final Set<IRI> targetSubjectsOf;

	public TargetSubjectsOf(Set<IRI> targetSubjectsOf) {
		this.targetSubjectsOf = targetSubjectsOf;
		assert !this.targetSubjectsOf.isEmpty();

	}

	@Override
	public IRI getPredicate() {
		return SHACL.TARGET_SUBJECTS_OF;
	}

	@Override
	public TupleValidationPlanNode getAdded(ConnectionsGroup connectionsGroup) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getQueryFragment(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		targetSubjectsOf.forEach(t -> {
			model.add(subject, getPredicate(), t);
		});
	}

	@Override
	public Stream<StatementPattern> getStatementPatterns(Var subject, Var object) {
		assert (subject == null);
		throw new UnsupportedOperationException();
	}

	@Override
	public String getQueryFragment(Var subject, Var object) {
		throw new UnsupportedOperationException();
	}
}
