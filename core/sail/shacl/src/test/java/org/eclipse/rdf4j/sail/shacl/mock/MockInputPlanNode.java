/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.mock;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.planNodes.IteratorData;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Tuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.ValidationExecutionLogger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Håvard Ottestad
 */
public class MockInputPlanNode implements PlanNode {

	Collection<Tuple> initialData;
	private ValidationExecutionLogger validationExecutionLogger;

	public MockInputPlanNode(Collection<Tuple> initialData) {
		this.initialData = initialData;
	}

	public MockInputPlanNode(Collection<String>... list) {

		initialData = Arrays.stream(list)
				.map(strings -> strings.stream()
						.map(SimpleValueFactory.getInstance()::createLiteral)
						.map(l -> (Value) l)
						.collect(Collectors.toList()))
				.map(Tuple::new)
				.sorted((a, b) -> new ValueComparator().compare(a.line.get(0), b.line.get(0)))
				.collect(Collectors.toList());

	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {

			Iterator<Tuple> iterator = initialData.iterator();

			@Override
			public void close() throws SailException {
			}

			@Override
			public boolean hasNext() throws SailException {
				return iterator.hasNext();
			}

			@Override
			public Tuple next() throws SailException {
				return iterator.next();
			}

			@Override
			public void remove() throws SailException {

			}
		};
	}

	@Override
	public int depth() {
		return 0;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {

	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public IteratorData getIteratorDataType() {
		return IteratorData.tripleBased;
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		if (this.validationExecutionLogger == null) {
			this.validationExecutionLogger = validationExecutionLogger;
		}
	}

}
