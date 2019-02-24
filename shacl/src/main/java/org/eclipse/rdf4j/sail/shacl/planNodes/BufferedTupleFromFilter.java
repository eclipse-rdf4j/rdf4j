/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;


import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author Håvard Ottestad
 */
public class BufferedTupleFromFilter implements PlanNode, PushBasedPlanNode, SupportsParentProvider {


	private CloseableIteration<Tuple, SailException> parentIterator;

	LinkedList<Tuple> next = new LinkedList<>();
	private ParentProvider parentProvider;
	private boolean printed = false;

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {

			private void calculateNext() {
				if (parentIterator != null) {
					while (next.isEmpty() && parentIterator.hasNext()) {
						parentIterator.next();
					}
				}
			}

			@Override
			public void close() throws SailException {
				if (parentIterator != null) {
					parentIterator.close();
				}
			}

			@Override
			public boolean hasNext() throws SailException {
				calculateNext();
				return !next.isEmpty();
			}

			@Override
			public Tuple next() throws SailException {
				calculateNext();

				return next.removeLast();
			}


			@Override
			public void remove() throws SailException {

			}
		};
	}

	@Override
	public int depth() {
		return parentProvider.parent().stream().mapToInt(PlanNode::depth).max().orElse(0)+1;
	}


	@Override
	public String getId() {
		return System.identityHashCode(this)+"";
	}

	@Override
	public IteratorData getIteratorDataType() {
		List<IteratorData> collect = parentProvider.parent().stream().map(PlanNode::getIteratorDataType).distinct().collect(Collectors.toList());
		if(collect.size() == 1) return collect.get(0);

		throw new IllegalStateException("Not implemented");
	}

	@Override
	public void push(Tuple t) {
		if (t != null) {
			next.addFirst(t);
		}
	}

	@Override
	public void parentIterator(CloseableIteration<Tuple, SailException> iterator) {
		parentIterator = iterator;
	}


	@Override
	public void receiveParentProvider(ParentProvider parentProvider) {
		this.parentProvider = parentProvider;
	}


	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if(printed) return;
		printed = true;

		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];").append("\n");

		if(parentProvider instanceof PlanNode){
			((PlanNode) parentProvider).getPlanAsGraphvizDot(stringBuilder);

		}

		if(parentProvider instanceof FilterPlanNode){
			((FilterPlanNode) parentProvider).getPlanAsGraphvizDot(stringBuilder);

		}
	}

	@Override
	public String toString() {
		return "BufferedTupleFromFilter";
	}
}
