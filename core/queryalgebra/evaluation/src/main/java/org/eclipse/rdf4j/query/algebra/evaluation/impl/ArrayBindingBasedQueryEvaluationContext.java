/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.evaluation.ArrayBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.ZeroLengthPathIteration;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;

public final class ArrayBindingBasedQueryEvaluationContext implements QueryEvaluationContext {
	private final QueryEvaluationContext context;
	private final String[] allVariables;
	private final LinkedHashSet<String> allVariablesSet;
	private final ArrayBindingSet defaultArrayBindingSet;

	ArrayBindingBasedQueryEvaluationContext(QueryEvaluationContext context, String[] allVariables) {
		this.context = context;
		this.allVariables = allVariables;
		this.allVariablesSet = new LinkedHashSet<>();
		this.allVariablesSet.addAll(Arrays.asList(allVariables));
		this.defaultArrayBindingSet = new ArrayBindingSet(allVariables);
	}

	@Override
	public Literal getNow() {
		return context.getNow();
	}

	@Override
	public Dataset getDataset() {
		return context.getDataset();
	}

	@Override
	public ArrayBindingSet createBindingSet() {
		return new ArrayBindingSet(allVariables);
	}

	@Override
	public Predicate<BindingSet> hasBinding(String variableName) {
		assert variableName != null && !variableName.isEmpty();
		Function<ArrayBindingSet, Boolean> directHasVariable = defaultArrayBindingSet
				.getDirectHasBinding(variableName);
		return new HasBinding(variableName, directHasVariable);
	}

	static private class HasBinding implements Predicate<BindingSet> {

		private final String variableName;
		private final Function<ArrayBindingSet, Boolean> directHasVariable;

		public HasBinding(String variableName, Function<ArrayBindingSet, Boolean> directHasVariable) {
			this.variableName = variableName;
			this.directHasVariable = directHasVariable;
		}

		@Override
		public Predicate<BindingSet> negate() {
			return (bs) -> {
				if (!bs.isEmpty()) {
					if (bs instanceof ArrayBindingSet) {
						return !directHasVariable.apply((ArrayBindingSet) bs);
					} else {
						return !bs.hasBinding(variableName);
					}
				}
				return true;
			};
		}

		@Override
		public boolean test(BindingSet bs) {
			if (bs.isEmpty()) {
				return false;
			}
			if (bs instanceof ArrayBindingSet) {
				return directHasVariable.apply((ArrayBindingSet) bs);
			} else {
				return bs.hasBinding(variableName);
			}
		}
	}

	@Override
	public Function<BindingSet, Binding> getBinding(String variableName) {
		Function<ArrayBindingSet, Binding> directAccessForVariable = defaultArrayBindingSet
				.getDirectGetBinding(variableName);
		return (bs) -> {
			if (bs.isEmpty()) {
				return null;
			}
			if (bs instanceof ArrayBindingSet) {
				return directAccessForVariable.apply((ArrayBindingSet) bs);
			} else {
				return bs.getBinding(variableName);
			}
		};
	}

	@Override
	public Function<BindingSet, Value> getValue(String variableName) {
		Function<ArrayBindingSet, Value> directAccessForVariable = defaultArrayBindingSet
				.getDirectGetValue(variableName);
		return new ValueGetter(variableName, directAccessForVariable);
	}

	private static class ValueGetter implements Function<BindingSet, Value> {

		private final String variableName;
		private final Function<ArrayBindingSet, Value> directAccessForVariable;

		public ValueGetter(String variableName, Function<ArrayBindingSet, Value> directAccessForVariable) {

			this.variableName = variableName;
			this.directAccessForVariable = directAccessForVariable;
		}

		@Override
		public Value apply(BindingSet bs) {
			if (bs.isEmpty()) {
				return null;
			}
			if (bs instanceof ArrayBindingSet) {
				return directAccessForVariable.apply((ArrayBindingSet) bs);
			} else {
				return bs.getValue(variableName);
			}
		}
	}

	@Override
	public BiConsumer<Value, MutableBindingSet> setBinding(String variableName) {
		BiConsumer<Value, ArrayBindingSet> directAccessForVariable = defaultArrayBindingSet
				.getDirectSetBinding(variableName);
		return (val, bs) -> {
			if (bs instanceof ArrayBindingSet) {
				directAccessForVariable.accept(val, (ArrayBindingSet) bs);
			} else {
				bs.setBinding(variableName, val);
			}
		};
	}

	@Override
	public BiConsumer<Value, MutableBindingSet> addBinding(String variableName) {
		BiConsumer<Value, ArrayBindingSet> wrapped = defaultArrayBindingSet
				.getDirectAddBinding(variableName);
		return (val, bs) -> {
			if (bs instanceof ArrayBindingSet) {
				wrapped.accept(val, (ArrayBindingSet) bs);
			} else {
				bs.addBinding(variableName, val);
			}
		};
	}

	@Override
	public ArrayBindingSet createBindingSet(BindingSet bindings) {
		if (bindings instanceof ArrayBindingSet) {
			return new ArrayBindingSet(((ArrayBindingSet) bindings), allVariables);
		} else if (bindings == EmptyBindingSet.getInstance()) {
			return createBindingSet();
		} else {
			return new ArrayBindingSet(bindings, allVariablesSet, allVariables);
		}
	}

	public static String[] findAllVariablesUsedInQuery(QueryRoot node) {
		HashMap<String, String> varNames = new LinkedHashMap<>();
		AbstractSimpleQueryModelVisitor<QueryEvaluationException> queryModelVisitorBase = new AbstractSimpleQueryModelVisitor<>(
				true) {

			@Override
			public void meet(Var node) throws QueryEvaluationException {
				super.meet(node);
				if (!node.isConstant()) {
					node.setName(varNames.computeIfAbsent(node.getName(), k -> k));
				}
			}

			@Override
			public void meet(ProjectionElem node) throws QueryEvaluationException {
				super.meet(node);
				node.setSourceName(varNames.computeIfAbsent(node.getSourceName(), k -> k));
				node.setTargetName(varNames.computeIfAbsent(node.getTargetName(), k -> k));
			}

			@Override
			protected void meetUnaryTupleOperator(UnaryTupleOperator node) throws QueryEvaluationException {
				if (node instanceof Projection) {
					node.getArg().visit(this);
					((Projection) node).getProjectionElemList().visit(this);
				} else {
					node.visitChildren(this);
				}
			}

			@Override
			public void meet(MultiProjection node) throws QueryEvaluationException {
				for (String bindingName : node.getBindingNames()) {
					varNames.computeIfAbsent(bindingName, k -> k);
				}
				super.meet(node);
			}

			@Override
			public void meet(ZeroLengthPath node) throws QueryEvaluationException {
				varNames.computeIfAbsent(ZeroLengthPathIteration.ANON_SUBJECT_VAR, k -> k);
				varNames.computeIfAbsent(ZeroLengthPathIteration.ANON_PREDICATE_VAR, k -> k);
				varNames.computeIfAbsent(ZeroLengthPathIteration.ANON_OBJECT_VAR, k -> k);
				varNames.computeIfAbsent(ZeroLengthPathIteration.ANON_SEQUENCE_VAR, k -> k);
				super.meet(node);
			}

			@Override
			public void meet(ExtensionElem node) throws QueryEvaluationException {
				node.setName(varNames.computeIfAbsent(node.getName(), k -> k));
				super.meet(node);
			}
		};
		node.visit(queryModelVisitorBase);
		String[] varNamesArr = varNames.keySet().toArray(new String[0]);
		return varNamesArr;
	}
}