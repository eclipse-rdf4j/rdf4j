package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ExternalFilterTargetIsSubject;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;

public class DashAllSubjects extends Target {

	private final Resource id;

	public DashAllSubjects(Resource id) {
		this.id = id;
	}

	@Override
	public IRI getPredicate() {
		return DASH.AllSubjectsTarget;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.TARGET_PROP, id);
		model.add(id, RDF.TYPE, getPredicate());
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope) {
		return getAddedRemovedInner(connectionsGroup, scope, connectionsGroup.getAddedStatements());
	}

	private PlanNode getAddedRemovedInner(ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope,
			SailConnection connection) {

		return new Unique(new UnorderedSelect(connection, null,
				null, null, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(scope)), false);

	}

	@Override
	public String getQueryFragment(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		String tempVar = "?" + UUID.randomUUID().toString().replace("-", "");

//		return targetObjectsOf.stream()
//			.map(target -> "\n{ BIND(<" + target + "> as " + tempVar + ") \n " + objectVariable + " "
//				+ tempVar + " " + subjectVariable
//				+ ". } \n")
//			.reduce((a, b) -> a + " UNION " + b)
//			.get();

		throw new UnsupportedOperationException("Not sure what calls this code!");
	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, PlanNode parent) {
		return new ExternalFilterTargetIsSubject(connectionsGroup.getBaseConnection(), parent)
				.getTrueNode(UnBufferedPlanNode.class);
	}

	@Override
	public Stream<StatementMatcher> getStatementMatcher(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		assert (subject == null);

		return Stream.of(
				new StatementMatcher(
						object,
						null,
						null
				)
		);
	}

	@Override
	public String getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		assert (subject == null);

		String tempVar1 = "?" + UUID.randomUUID().toString().replace("-", "");
		String tempVar2 = "?" + UUID.randomUUID().toString().replace("-", "");

		return " ?" + object.getName() + " " + tempVar1 + " " + tempVar2 + " .";

	}

}
