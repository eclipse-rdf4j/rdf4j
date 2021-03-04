package org.eclipse.rdf4j.sail.shacl.ast.paths;

import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeWrapper;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;

public class SimplePath extends Path {

	IRI predicate;

	public SimplePath(IRI predicate) {
		super(predicate);
		this.predicate = predicate;
	}

	@Override
	public Resource getId() {
		return predicate;
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, PlanNodeWrapper planNodeWrapper) {
		PlanNode unorderedSelect = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, predicate, null,
				UnorderedSelect.Mapper.SubjectObjectPropertyShapeMapper.getFunction());

		if (planNodeWrapper != null) {
			unorderedSelect = planNodeWrapper.apply(unorderedSelect);
		}

		return connectionsGroup.getCachedNodeFor(unorderedSelect);
	}

	@Override
	public boolean isSupported() {
		return true;
	}

	@Override
	public String toString() {
		return "SimplePath{ <" + predicate + "> }";
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
	}

	@Override
	public Stream<StatementMatcher> getStatementMatcher(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		return Stream.of(new StatementMatcher(subject, new StatementMatcher.Variable(predicate), object));
	}

	@Override
	public String getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {

		return "?" + subject.getName() + " <" + predicate + "> ?" + object.getName() + " .";
	}
}
