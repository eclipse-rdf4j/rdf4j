package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.Targetable;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BindSelect;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ExternalFilterByQuery;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TupleMapper;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;

public class EffectiveTarget {

	private final ArrayDeque<EffectiveTargetObject> chain;
	private final EffectiveTargetObject optional;
	private final RdfsSubClassOfReasoner rdfsSubClassOfReasoner;

	public EffectiveTarget(ArrayDeque<Targetable> chain, Targetable optional, String targetVarPrefix,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		int index = 0;

		this.chain = new ArrayDeque<>();

		EffectiveTargetObject previous = null;

		for (Targetable targetable : chain) {
			EffectiveTargetObject effectiveTargetObject = new EffectiveTargetObject(
					new StatementMatcher.Variable(targetVarPrefix + String.format("%010d", index++)),
					targetable,
					previous,
					rdfsSubClassOfReasoner
			);
			previous = effectiveTargetObject;
			this.chain.addLast(effectiveTargetObject);
		}

		if (optional != null) {
			this.optional = new EffectiveTargetObject(
					new StatementMatcher.Variable(targetVarPrefix + String.format("%010d", index)),
					optional,
					previous,
					rdfsSubClassOfReasoner
			);
		} else {
			this.optional = null;
		}

		this.rdfsSubClassOfReasoner = rdfsSubClassOfReasoner;
	}

	public StatementMatcher.Variable getTargetVar() {
		return chain.getLast().var;
	}

	// Takes a source plan node and for every entry it extends the target chain with all targets that follow.
	// If the target chain is [type foaf:Person / foaf:knows ] and [ex:Peter] is in the source, this will effectively
	// retrieve all "ex:Peter foaf:knows ?extension"
	public PlanNode extend(PlanNode source, ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope,
			Extend direction, boolean includePropertyShapeValues) {

		String query = getQuery(includePropertyShapeValues);
		List<StatementMatcher.Variable> vars = getVars();
		if (includePropertyShapeValues) {
			vars = new ArrayList<>(vars);
			vars.add(optional.var);
		}

		List<String> varNames = vars.stream().map(StatementMatcher.Variable::getName).collect(Collectors.toList());

		if (varNames.size() == 1) {
			return connectionsGroup.getCachedNodeFor(getTargetFilter(connectionsGroup,
					new Unique(new TupleMapper(source, new ActiveTargetTupleMapper(scope, includePropertyShapeValues)),
							false)));
		}

		return connectionsGroup.getCachedNodeFor(
				new Unique(new BindSelect(connectionsGroup.getBaseConnection(), query, vars, source, varNames, scope,
						100, direction, includePropertyShapeValues), true));
	}

	private List<StatementMatcher.Variable> getVars() {
		return chain.stream().map(t -> t.var).collect(Collectors.toList());
	}

	/**
	 *
	 * @return false if it is 100% sure that this will not match, else returns true
	 */
	public boolean couldMatch(ConnectionsGroup connectionsGroup) {

		boolean hasTargetNode = Stream.concat(chain.stream(), getOptionalAsStream())
				.anyMatch(e -> e.target instanceof TargetNode);
		if (hasTargetNode) {
			return true;
		}

		return Stream.concat(chain.stream(), getOptionalAsStream())
				.flatMap(EffectiveTargetObject::getStatementMatcher)
				.anyMatch(currentStatementPattern ->

				connectionsGroup.getAddedStatements()
						.hasStatement(
								currentStatementPattern.getSubjectValue(),
								currentStatementPattern.getPredicateValue(),
								currentStatementPattern.getObjectValue(), false)
						||
						connectionsGroup.getRemovedStatements()
								.hasStatement(
										currentStatementPattern.getSubjectValue(),
										currentStatementPattern.getPredicateValue(),
										currentStatementPattern.getObjectValue(), false)

				);

	}

	private Stream<EffectiveTargetObject> getOptionalAsStream() {
		Stream<EffectiveTargetObject> optional;
		if (this.optional != null) {
			optional = Stream.of(this.optional);
		} else {
			optional = Stream.empty();
		}
		return optional;
	}

	public PlanNode getAllTargets(ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope) {
		String query = chain.stream()
				.map(EffectiveTargetObject::getQueryFragment)
				.reduce((a, b) -> a + "\n" + b)
				.orElse("");

		List<String> varNames = getVars().stream().map(StatementMatcher.Variable::getName).collect(Collectors.toList());

		return new Select(connectionsGroup.getBaseConnection(), query, null,
				new AllTargetsBindingSetMapper(varNames, scope, false));
	}

	public PlanNode getPlanNode(ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope,
			boolean includeTargetsAffectedByRemoval) {
		assert !chain.isEmpty();

		if (chain.size() == 1 && !(includeTargetsAffectedByRemoval && optional != null)) {
			// simple chain

			EffectiveTargetObject last = chain.getLast();
			if (last.target instanceof Target) {
				return connectionsGroup.getCachedNodeFor(((Target) last.target).getAdded(connectionsGroup, scope));
			} else {
				throw new ShaclUnsupportedException(
						"Unknown target in chain is type: " + last.getClass().getSimpleName());
			}

		} else {
			// complex chain

			List<StatementMatcher> statementMatchers = chain.stream()
					.flatMap(EffectiveTargetObject::getStatementMatcher)
					.collect(Collectors.toList());

			String query = chain.stream()
					.map(EffectiveTargetObject::getQueryFragment)
					.reduce((a, b) -> a + "\n" + b)
					.orElse("");

			List<StatementMatcher> statementMatchersRemoval = optional != null
					? optional.getStatementMatcher().collect(Collectors.toCollection(ArrayList::new))
					: new ArrayList<>();

			if (chain.getFirst().target instanceof RSXTargetShape) {
				statementMatchersRemoval.addAll(chain.getFirst().getStatementMatcher().collect(Collectors.toList()));
				includeTargetsAffectedByRemoval = true;
			}

			TargetChainRetriever targetChainRetriever;
			if (includeTargetsAffectedByRemoval) {
				targetChainRetriever = new TargetChainRetriever(
						connectionsGroup,
						statementMatchers,
						statementMatchersRemoval,
						query,
						getVars(),
						scope
				);
			} else {
				targetChainRetriever = new TargetChainRetriever(
						connectionsGroup,
						statementMatchers,
						null,
						query,
						getVars(),
						scope
				);
			}

			return connectionsGroup.getCachedNodeFor(new Unique(targetChainRetriever, true));

		}

	}

	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, PlanNode parent) {

		EffectiveTargetObject last = chain.getLast();

		if (chain.size() == 1) {
			// simple chain

			if (last.target instanceof Target) {
				return ((Target) last.target).getTargetFilter(connectionsGroup, parent);
			} else {
				throw new ShaclUnsupportedException(
						"Unknown target in chain is type: " + last.getClass().getSimpleName());
			}
		}

		String query = chain.stream()
				.map(EffectiveTargetObject::getQueryFragment)
				.reduce((a, b) -> a + "\n" + b)
				.orElse("");

		// TODO: this is a slow way to solve this problem! We should use bulk operations.
		return new ExternalFilterByQuery(connectionsGroup.getBaseConnection(), parent, query, last.var,
				ValidationTuple::getActiveTarget)
						.getTrueNode(UnBufferedPlanNode.class);
	}

	public String getQuery(boolean includeOptional) {

		ArrayDeque<EffectiveTargetObject> chain;

		if (includeOptional) {
			chain = new ArrayDeque<>(this.chain);
			chain.addLast(optional);
		} else {
			chain = this.chain;
		}

		return chain.stream()
				.map(EffectiveTargetObject::getQueryFragment)
				.reduce((a, b) -> a + "\n" + b)
				.orElse("");

	}

	public enum Extend {
		left,
		right
	}

	static class EffectiveTargetObject {

		final StatementMatcher.Variable var;
		final Targetable target;
		final EffectiveTargetObject prev;
		final RdfsSubClassOfReasoner rdfsSubClassOfReasoner;

		public EffectiveTargetObject(StatementMatcher.Variable var, Targetable target, EffectiveTargetObject prev,
				RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
			this.var = var;
			this.target = target;
			this.prev = prev;
			this.rdfsSubClassOfReasoner = rdfsSubClassOfReasoner;
		}

		public Stream<StatementMatcher> getStatementMatcher() {
			if (prev == null) {
				return target.getStatementMatcher(null, var, rdfsSubClassOfReasoner);
			} else {
				return target.getStatementMatcher(prev.var, var, rdfsSubClassOfReasoner);
			}
		}

		public String getQueryFragment() {
			if (prev == null) {
				return target.getTargetQueryFragment(null, var, rdfsSubClassOfReasoner);
			} else {
				return target.getTargetQueryFragment(prev.var, var, rdfsSubClassOfReasoner);
			}
		}
	}

	static class AllTargetsBindingSetMapper implements Function<BindingSet, ValidationTuple> {
		List<String> varNames;
		ConstraintComponent.Scope scope;
		boolean hasValue;

		public AllTargetsBindingSetMapper(List<String> varNames, ConstraintComponent.Scope scope, boolean hasValue) {
			this.varNames = varNames;
			this.scope = scope;
			this.hasValue = hasValue;
		}

		@Override
		public ValidationTuple apply(BindingSet b) {
			return new ValidationTuple(b, varNames, scope, false);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			AllTargetsBindingSetMapper that = (AllTargetsBindingSetMapper) o;
			return hasValue == that.hasValue &&
					varNames.equals(that.varNames) &&
					scope == that.scope;
		}

		@Override
		public int hashCode() {
			return Objects.hash(varNames, scope, hasValue, AllTargetsBindingSetMapper.class);
		}
	}

	static class ActiveTargetTupleMapper implements Function<ValidationTuple, ValidationTuple> {
		ConstraintComponent.Scope scope;
		boolean includePropertyShapeValues;

		public ActiveTargetTupleMapper(ConstraintComponent.Scope scope, boolean includePropertyShapeValues) {
			this.scope = scope;
			this.includePropertyShapeValues = includePropertyShapeValues;
		}

		@Override
		public ValidationTuple apply(ValidationTuple validationTuple) {
			return new ValidationTuple(validationTuple.getActiveTarget(), scope, includePropertyShapeValues);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ActiveTargetTupleMapper that = (ActiveTargetTupleMapper) o;
			return includePropertyShapeValues == that.includePropertyShapeValues && scope == that.scope;
		}

		@Override
		public int hashCode() {
			return Objects.hash(scope, includePropertyShapeValues);
		}
	}

}
