package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.paths;

import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNodeWrapper;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValidationTuple;

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
				s -> new ValidationTuple(s.getSubject(), this, s.getObject()));
		if (planNodeWrapper != null) {
			unorderedSelect = planNodeWrapper.apply(unorderedSelect);
		}

		return connectionsGroup.getCachedNodeFor(new Sort(unorderedSelect));
	}

	@Override
	public String toString() {
		return "SimplePath{ <" + predicate + "> }";
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
	}

	@Override
	public Stream<StatementPattern> getStatementPatterns(Var subject, Var object) {
		return Stream.of(new StatementPattern(subject, new Var(predicate), object));
	}

	@Override
	public String getQueryFragment(Var subject, Var object) {

		return "?" + subject.getName() + " <" + predicate + "> ?" + object.getName() + " .";
	}
}
