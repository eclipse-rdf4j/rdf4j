package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ExternalFilterByPredicate;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValidationTuple;

public class TargetObjectsOf extends Target {

	private final Set<IRI> targetObjectsOf;

	public TargetObjectsOf(Set<IRI> targetObjectsOf) {
		this.targetObjectsOf = targetObjectsOf;
		assert !this.targetObjectsOf.isEmpty();

	}

	@Override
	public IRI getPredicate() {
		return SHACL.TARGET_OBJECTS_OF;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		targetObjectsOf.forEach(t -> {
			model.add(subject, getPredicate(), t);
		});
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope) {
		return getAddedRemovedInner(connectionsGroup, scope, connectionsGroup.getAddedStatements());
	}

	@Override
	public PlanNode getRemoved(ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope) {
		return getAddedRemovedInner(connectionsGroup, scope, connectionsGroup.getRemovedStatements());
	}

	private PlanNode getAddedRemovedInner(ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope,
			SailConnection connection) {

		PlanNode planNode = targetObjectsOf.stream()
				.map(predicate -> {
					return connectionsGroup
							.getCachedNodeFor(new Sort(new UnorderedSelect(connection, null,
									predicate, null, s -> new ValidationTuple(s.getObject(), scope, false))));
				})
				.reduce(UnionNode::new)
				.orElse(new EmptyNode());

		return new Unique(planNode);
	}

	@Override
	public String getQueryFragment(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		String tempPredicate = "?" + UUID.randomUUID().toString().replace("-", "");

		return targetObjectsOf.stream()
				.map(target -> "\n{ BIND(<" + target + "> as " + tempPredicate + ") \n " + objectVariable + " "
						+ tempPredicate + " " + subjectVariable
						+ ". } \n")
				.reduce((a, b) -> a + " UNION " + b)
				.get();
	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, PlanNode parent) {
		return new ExternalFilterByPredicate(connectionsGroup.getBaseConnection(), targetObjectsOf, parent,
				ExternalFilterByPredicate.On.Object);
	}

	@Override
	public Stream<StatementPattern> getStatementPatterns(Var subject, Var object) {
		assert (subject == null);

		return targetObjectsOf.stream()
				.map(t -> new StatementPattern(
						new Var(UUID.randomUUID().toString().replace("-", "")),
						new Var(t),
						object
				)
				);
	}

	@Override
	public String getTargetQueryFragment(Var subject, Var object) {
		assert (subject == null);

		String tempPredicate = "?" + UUID.randomUUID().toString().replace("-", "");

		return targetObjectsOf.stream()
				.map(t -> tempPredicate + " <" + t + "> ?" + object.getName() + " .")
				.reduce((a, b) -> a + "\n" + b)
				.orElse("");
	}
}
