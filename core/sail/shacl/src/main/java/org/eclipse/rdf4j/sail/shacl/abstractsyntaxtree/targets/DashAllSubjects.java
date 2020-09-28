package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ExternalFilterIsSubject;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValidationTuple;

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
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.TARGET_PROP, id);
		model.add(id, RDF.TYPE, getPredicate());
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

		return connectionsGroup
				.getCachedNodeFor(new Unique(new UnorderedSelect(connection, null,
						null, null, s -> new ValidationTuple(s.getSubject(), scope, false))));

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
		return new ExternalFilterIsSubject(connectionsGroup.getBaseConnection(), parent)
				.getFalseNode(UnBufferedPlanNode.class);
	}

	@Override
	public Stream<StatementPattern> getStatementPatterns(Var subject, Var object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		assert (subject == null);

		return Stream.of(
				new StatementPattern(
						object,
						new Var(UUID.randomUUID().toString().replace("-", "")),
						new Var(UUID.randomUUID().toString().replace("-", ""))
				)
		);
	}

	@Override
	public String getTargetQueryFragment(Var subject, Var object, RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		assert (subject == null);

		String tempVar1 = "?" + UUID.randomUUID().toString().replace("-", "");
		String tempVar2 = "?" + UUID.randomUUID().toString().replace("-", "");

		return " ?" + object.getName() + " " + tempVar1 + " " + tempVar2 + " .";

	}

}
