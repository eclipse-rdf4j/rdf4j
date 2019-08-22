/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;

import java.util.Arrays;
import java.util.Set;

/**
 * @author Håvard Ottestad
 */
public class ExternalFilterByPredicate implements PlanNode {

	private final SailConnection connection;
	private final Set<IRI> filterOnPredicates;
	final PlanNode parent;
	final int index;
	private final On on;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public enum On {
		Subject,
		Object
	}

	public ExternalFilterByPredicate(SailConnection connection, Set<IRI> filterOnPredicates, PlanNode parent, int index,
			On on) {
		this.connection = connection;
		this.filterOnPredicates = filterOnPredicates;
		this.parent = parent;
		this.index = index;
		this.on = on;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			Tuple next = null;

			CloseableIteration<Tuple, SailException> parentIterator = parent.iterator();

			void calculateNext() {
				while (next == null && parentIterator.hasNext()) {
					Tuple temp = parentIterator.next();

					Value subject = temp.line.get(index);

					IRI matchedPredicate = matchesFilter(subject);

					if (matchedPredicate != null) {
						next = temp;
						next.addHistory(new Tuple(Arrays.asList(subject, matchedPredicate)));
					}

				}
			}

			private IRI matchesFilter(Value node) {

				if (node instanceof Resource && on == On.Subject) {

					return filterOnPredicates.stream()
							.filter(predicate -> connection.hasStatement((Resource) node, predicate, null, true))
							.findFirst()
							.orElse(null);

				} else if (on == On.Object) {

					return filterOnPredicates.stream()
							.filter(predicate -> connection.hasStatement(null, predicate, node, true))
							.findFirst()
							.orElse(null);

				}
				return null;
			}

			@Override
			public void close() throws SailException {
				parentIterator.close();
			}

			@Override
			boolean localHasNext() throws SailException {
				calculateNext();
				return next != null;
			}

			@Override
			Tuple loggingNext() throws SailException {
				calculateNext();

				Tuple temp = next;
				next = null;

				return temp;
			}

			@Override
			public void remove() throws SailException {

			}
		};
	}

	@Override
	public int depth() {
		return parent.depth() + 1;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
				.append("\n");
		stringBuilder.append(parent.getId() + " -> " + getId()).append("\n");

		if (connection instanceof MemoryStoreConnection) {
			stringBuilder.append(System.identityHashCode(((MemoryStoreConnection) connection).getSail()) + " -> "
					+ getId() + " [label=\"filter source\"]").append("\n");
		} else {
			stringBuilder.append(System.identityHashCode(connection) + " -> " + getId() + " [label=\"filter source\"]")
					.append("\n");
		}

		parent.getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String toString() {
		return "ExternalFilterByPredicate{" + "filterOnPredicates="
				+ Arrays.toString(filterOnPredicates.stream().map(Formatter::prefix).toArray())
				+ '}';
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public IteratorData getIteratorDataType() {
		return parent.getIteratorDataType();
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		parent.receiveLogger(validationExecutionLogger);
	}
}
