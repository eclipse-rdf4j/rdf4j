package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.tempPlanNodes.TupleValidationPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.tempPlanNodes.ValidationTupleMapper;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnorderedSelect;

public class TargetClass extends Target {
	private final Set<Resource> targetClass;

	public TargetClass(Set<Resource> targetClass) {
		this.targetClass = targetClass;
		assert !this.targetClass.isEmpty();

	}

	@Override
	public IRI getPredicate() {
		return SHACL.TARGET_CLASS;
	}

	@Override
	public TupleValidationPlanNode getAdded(ConnectionsGroup connectionsGroup) {
		PlanNode planNode;
		if (targetClass.size() == 1) {
			Resource clazz = targetClass.stream().findAny().get();
			planNode = connectionsGroup
					.getCachedNodeFor(new Sort(new UnorderedSelect(connectionsGroup.getAddedStatements(), null,
							RDF.TYPE, clazz, UnorderedSelect.OutputPattern.SubjectPredicateObject)));
		} else {
			planNode = connectionsGroup.getCachedNodeFor(
					new Select(connectionsGroup.getAddedStatements(), getQueryFragment("?a", "?c", null), "?a", "?c"));
		}

		Unique targets = new Unique(new TrimTuple(planNode, 0, 1));

		return new ValidationTupleMapper(targets, (t) -> {
			List<Value> line = t.getLine();
			return new ArrayDeque<>(line);
		}, null, null);
	}

	@Override
	public String getQueryFragment(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		Set<Resource> targets = targetClass;

		if (rdfsSubClassOfReasoner != null) {
			targets = new HashSet<>(targets);

			targets = targets.stream()
					.flatMap(target -> rdfsSubClassOfReasoner.backwardsChain(target).stream())
					.collect(Collectors.toSet());
		}

		assert targets.size() >= 1;

		return targets.stream()
				.map(r -> "{ BIND(rdf:type as ?b1) \n BIND(<" + r + "> as " + objectVariable + ") \n " + subjectVariable
						+ " ?b1 " + objectVariable + ". } \n")
				.reduce((l, r) -> l + " UNION " + r)
				.get();

	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		targetClass.forEach(t -> {
			model.add(subject, getPredicate(), t);
		});
	}

	@Override
	public Stream<StatementPattern> getStatementPatterns(Var subject, Var object) {
		assert (subject == null);

		return targetClass.stream().map(t -> new StatementPattern(object, new Var(RDF.TYPE), new Var(t)));
	}

	@Override
	public String getQueryFragment(Var subject, Var object) {
		assert (subject == null);

		return targetClass.stream()
				.map(t -> "?" + object.getName() + " a <" + t + "> .")
				.reduce((a, b) -> a + "\n" + b)
				.orElse("");

	}
}
