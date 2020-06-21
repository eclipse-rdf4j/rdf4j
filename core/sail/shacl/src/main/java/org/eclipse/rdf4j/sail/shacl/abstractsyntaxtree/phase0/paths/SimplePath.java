package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths;

import java.util.ArrayDeque;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.shacl.AST.PlaneNodeWrapper;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.tempPlanNodes.ValidationMapper;
import org.eclipse.rdf4j.sail.shacl.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnorderedSelect;

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
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, PlaneNodeWrapper planeNodeWrapper) {
		PlanNode unorderedSelect = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, predicate, null,
				UnorderedSelect.OutputPattern.SubjectObject);
		if (planeNodeWrapper != null) {
			unorderedSelect = planeNodeWrapper.apply(unorderedSelect);
		}
		PlanNode cachedNodeFor = connectionsGroup.getCachedNodeFor(new Sort(unorderedSelect));

		return new ValidationMapper(cachedNodeFor, t -> new ArrayDeque<>(t.getLine().subList(0, 1)), () -> this,
				t -> t.getLine().get(1));

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
