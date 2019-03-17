/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Håvard Ottestad
 */
public class LoggingNode implements PlanNode {

	static private final Logger logger = LoggerFactory.getLogger(LoggingNode.class);

	PlanNode parent;
	String message;

	private boolean pullAll = true;

	public static boolean loggingEnabled = false;

	public LoggingNode(PlanNode parent, String message) {
		this.parent = parent;
		this.message = message;
		assert message != null;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		if (!loggingEnabled) {
			return parent.iterator();
		} else {
			return new CloseableIteration<Tuple, SailException>() {

				CloseableIteration<Tuple, SailException> parentIterator = parent.iterator();

				{
					if (pullAll) {
						parentIterator = cachedIterator(parentIterator);
					}
				}

				private CloseableIteration<Tuple, SailException> cachedIterator(
						CloseableIteration<Tuple, SailException> fromIterator) {
					Stream<Tuple> stream = Iterations.stream(fromIterator);
					List<Tuple> collect = stream.collect(Collectors.toList());

					return new CloseableIteration<Tuple, SailException>() {

						Iterator<Tuple> iterator = collect.iterator();

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
				public void close() throws SailException {

					parentIterator.close();

				}

				@Override
				public boolean hasNext() throws SailException {
					boolean hasNext = parentIterator.hasNext();

//					logger.info(leadingSpace()+parent.getClass().getSimpleName()+".hasNext() : "+hasNext);
					return hasNext;
				}

				@Override
				public Tuple next() throws SailException {
					assert parentIterator.hasNext() : parentIterator.getClass().getSimpleName()
							+ " does not have any more items but next was still called!!!";

					Tuple next = parentIterator.next();

					assert next != null;

					logger.info(leadingSpace() + message + parent.getClass().getSimpleName() + ".next(): " + " "
							+ next.toString());

					return next;
				}

				@Override
				public void remove() throws SailException {

				}
			};

		}
	}

	@Override
	public int depth() {
		return parent.depth() + 1;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		parent.getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String getId() {
		return parent.getId();
	}

	@Override
	public IteratorData getIteratorDataType() {
		return parent.getIteratorDataType();
	}

	private String leadingSpace() {
		return StringUtils.leftPad("", depth(), "    ");
	}

}
