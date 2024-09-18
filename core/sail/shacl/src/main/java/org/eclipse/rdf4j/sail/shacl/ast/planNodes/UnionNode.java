/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Stream;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

/**
 * @author Håvard Ottestad
 */
public class UnionNode implements PlanNode {

	private final HashSet<PlanNode> nodesSet;
	private final ConnectionsGroup connectionsGroup;
	private StackTraceElement[] stackTrace;
	private final PlanNode[] nodes;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	private UnionNode(ConnectionsGroup connectionsGroup, PlanNode... nodes) {

		this.nodes = nodes;
		this.nodesSet = new HashSet<>(Arrays.asList(nodes));
		this.connectionsGroup = connectionsGroup;

		// this.stackTrace = Thread.currentThread().getStackTrace();
	}

	public static PlanNode getInstance(ConnectionsGroup connectionsGroup, PlanNode... nodes) {
		PlanNode[] planNodes = Arrays.stream(nodes).filter(n -> !(n.isGuaranteedEmpty())).flatMap(n -> {
			if (n instanceof UnionNode) {
				return Arrays.stream(((UnionNode) n).nodes);
			}
			return Stream.of(n);
		}).map(n -> PlanNodeHelper.handleSorting(true, n, connectionsGroup)).toArray(PlanNode[]::new);

		if (planNodes.length == 1) {
			return planNodes[0];
		}
		if (planNodes.length == 0) {
			return EmptyNode.getInstance();
		}

		return new UnionNode(connectionsGroup, planNodes);

	}

	public static PlanNode getInstanceDedupe(ConnectionsGroup connectionsGroup, PlanNode... nodes) {
		PlanNode[] planNodes = Arrays.stream(nodes).filter(n -> !(n.isGuaranteedEmpty())).distinct().flatMap(n -> {
			if (n instanceof UnionNode) {
				return Arrays.stream(((UnionNode) n).nodes);
			}
			return Stream.of(n);
		}).map(n -> PlanNodeHelper.handleSorting(true, n, connectionsGroup)).toArray(PlanNode[]::new);

		if (planNodes.length == 1) {
			return planNodes[0];
		}
		if (planNodes.length == 0) {
			return EmptyNode.getInstance();
		}

		return new UnionNode(connectionsGroup, planNodes);

	}

	@Override
	public CloseableIteration<? extends ValidationTuple> iterator() {

		if (nodes.length == 1) {
			return new LoggingCloseableIteration(this, validationExecutionLogger) {

				private CloseableIteration<? extends ValidationTuple> iterator;

				@Override
				protected void init() {
					iterator = nodes[0].iterator();
				}

				@Override
				public void localClose() {
					if (iterator != null) {
						iterator.close();
					}
				}

				@Override
				protected ValidationTuple loggingNext() {
					return iterator.next();
				}

				@Override
				protected boolean localHasNext() {
					return iterator.hasNext();
				}
			};
		}

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			private CloseableIteration<? extends ValidationTuple>[] iterators;

			final ValidationTuple[] peekList = new ValidationTuple[nodes.length];

			ValidationTuple next;
			ValidationTuple prev;

			@Override
			protected void init() {
				iterators = Arrays
						.stream(nodes)
						.map(PlanNode::iterator)
						.toArray(CloseableIteration[]::new);
			}

			private void calculateNext() {

				if (next != null) {
					return;
				}

				for (int i = 0; i < peekList.length; i++) {
					if (peekList[i] == null) {
						var iterator = iterators[i];
						if (iterator.hasNext()) {
							peekList[i] = iterator.next();
						}
					}
				}

				ValidationTuple sortedFirst = null;
				int sortedFirstIndex = -1;

				for (int i = 0; i < peekList.length; i++) {
					if (peekList[i] == null) {
						continue;
					}

					if (sortedFirst == null) {
						sortedFirst = peekList[i];
						sortedFirstIndex = i;
					} else {
						if (peekList[i].compareActiveTarget(sortedFirst) < 0) {
							sortedFirst = peekList[i];
							sortedFirstIndex = i;
						}
					}

				}

				if (sortedFirstIndex >= 0) {
					peekList[sortedFirstIndex] = null;
				}

				next = sortedFirst;
			}

			@Override
			public void localClose() {
				if (iterators != null) {
					Throwable thrown = null;
					for (int i = 0; i < iterators.length; i++) {
						try {
							iterators[i].close();
						} catch (Throwable t) {
							if (thrown != null) {
								thrown.addSuppressed(t);
							} else {
								thrown = t;
							}
						} finally {
							iterators[i] = null;
						}
					}

					if (thrown != null) {
						throw new SailException(thrown);
					}
				}
			}

			@Override
			protected boolean localHasNext() {
				calculateNext();
				return next != null;
			}

			@Override
			protected ValidationTuple loggingNext() {
				calculateNext();

				assert !(prev != null && next.compareActiveTarget(prev) < 0);

				ValidationTuple temp = next;
				prev = next;
				next = null;
				return temp;
			}

		};
	}

	@Override
	public int depth() {
		return Arrays.stream(nodes).mapToInt(PlanNode::depth).max().orElse(0) + 1;

	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
				.append("\n");
		for (PlanNode node : nodes) {
			stringBuilder.append(node.getId() + " -> " + getId()).append("\n");
			node.getPlanAsGraphvizDot(stringBuilder);

		}
	}

	@Override
	public String toString() {
		return "UnionNode";
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		for (PlanNode node : nodes) {
			node.receiveLogger(validationExecutionLogger);
		}
	}

	@Override
	public boolean producesSorted() {
		return true;
	}

	@Override
	public boolean requiresSorted() {
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		UnionNode unionNode = (UnionNode) o;
		if (nodes.length != unionNode.nodes.length) {
			return false;
		}

		return nodesSet.equals(unionNode.nodesSet);

	}

	@Override
	public int hashCode() {
		return nodes.length + nodesSet.hashCode();
	}
}
