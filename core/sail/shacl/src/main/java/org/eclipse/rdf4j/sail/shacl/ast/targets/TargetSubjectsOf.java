package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ExternalFilterByPredicate;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;

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
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		targetSubjectsOf.forEach(t -> {
			model.add(subject, getPredicate(), t);
		});
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope) {
		return getAddedRemovedInner(connectionsGroup, scope, connectionsGroup.getAddedStatements());
	}

	private PlanNode getAddedRemovedInner(ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope,
			SailConnection connection) {

		PlanNode planNode = targetSubjectsOf.stream()
				.map(predicate -> (PlanNode) new UnorderedSelect(connection, null,
						predicate, null, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(scope)))
				.reduce(UnionNode::new)
				.orElse(new EmptyNode());

		return new Unique(planNode, false);
	}

	@Override
	public String getQueryFragment(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		String tempVar = "?" + UUID.randomUUID().toString().replace("-", "");

		return targetSubjectsOf.stream()
				.map(target -> "\n{ BIND(<" + target + "> as " + tempVar + ") \n " + subjectVariable + " "
						+ tempVar + " " + objectVariable
						+ ". } \n")
				.reduce((a, b) -> a + " UNION " + b)
				.get();
	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, PlanNode parent) {
		return new ExternalFilterByPredicate(connectionsGroup.getBaseConnection(), targetSubjectsOf, parent,
				ExternalFilterByPredicate.On.Subject);
	}

	@Override
	public Stream<StatementMatcher> getStatementMatcher(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		assert (subject == null);

		return targetSubjectsOf.stream()
				.map(t -> new StatementMatcher(
						object,
						new StatementMatcher.Variable(t),
						null
				)
				);
	}

	@Override
	public String getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		assert (subject == null);

		String tempVar = "?" + UUID.randomUUID().toString().replace("-", "");

		if (targetSubjectsOf.size() == 1) {

			return targetSubjectsOf.stream()
					.map(t -> "?" + object.getName() + " <" + t + "> " + tempVar + " .")
					.reduce((a, b) -> a + "\n" + b)
					.orElse("");

		} else {

			String in = targetSubjectsOf.stream()
					.map(t -> "<" + t + ">")
					.reduce((a, b) -> a + " , " + b)
					.orElse("");

			return "?" + object.getName() + " ?predicatefjhfuewhw " + tempVar + " .\n" +
					"FILTER(?predicatefjhfuewhw in (" + in + ")) \n";
		}

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TargetSubjectsOf that = (TargetSubjectsOf) o;
		return targetSubjectsOf.equals(that.targetSubjectsOf);
	}

	@Override
	public int hashCode() {
		return Objects.hash(targetSubjectsOf);
	}
}
