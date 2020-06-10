/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.tempPlanNodes;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths.Path;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets.TargetChain;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Tuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.ValidationExecutionLogger;

public class ValidationTupleMapper implements TupleValidationPlanNode {

	private final PlanNode parent;
	private final Function<Tuple, List<Value>> targetMapper;
	private final Supplier<Path> pathSupplier;
	private final Function<Tuple, Value> valueMapper;
	private boolean printed = false;

	public ValidationTupleMapper(PlanNode planNode, Function<Tuple, List<Value>> targetMapper,
			Supplier<Path> pathSupplier,
			Function<Tuple, Value> valueMapper) {

		this.parent = planNode;
		this.targetMapper = targetMapper;
		this.pathSupplier = pathSupplier;
		this.valueMapper = valueMapper;

	}

	@Override
	public CloseableIteration<ValidationTuple, SailException> iterator() {
		return new CloseableIteration<ValidationTuple, SailException>() {

			final CloseableIteration<Tuple, SailException> iterator = parent.iterator();

			@Override
			public void close() throws SailException {
				iterator.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				return iterator.hasNext();
			}

			@Override
			public ValidationTuple next() throws SailException {
				Tuple next = iterator.next();
				List<Value> target = null;
				Path path = null;
				Value value = null;
				if (targetMapper != null) {
					target = targetMapper.apply(next);
				}

				if (pathSupplier != null) {
					path = pathSupplier.get();
				}

				if (valueMapper != null) {
					value = valueMapper.apply(next);
				}

				return new ValidationTuple(target, path, value);
			}

			@Override
			public void remove() throws SailException {
				iterator.remove();
			}
		};
	}

	@Override
	public int depth() {
		return 0;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId())
				.append(" [label=\"")
				.append(StringEscapeUtils.escapeJava(this.toString()))
				.append("\"];")
				.append("\n");
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public String toString() {
		return "Empty";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
	}
}
