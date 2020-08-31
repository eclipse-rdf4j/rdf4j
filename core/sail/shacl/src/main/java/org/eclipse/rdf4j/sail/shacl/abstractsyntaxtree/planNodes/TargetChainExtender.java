/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.EffectiveTarget;

/**
 * Takes a plan node and an expected target chain and extends the validation tuples from the plan node to match the
 * expected target chain by retrieving the required data from the database.
 *
 * @author Håvard Ottestad
 */
public class TargetChainExtender implements PlanNode {

	private final PlanNode parent;
	private final EffectiveTarget effectiveTarget;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public TargetChainExtender(PlanNode parent, EffectiveTarget effectiveTarget) {
		this.parent = parent;
		this.effectiveTarget = effectiveTarget;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final private CloseableIteration<? extends ValidationTuple, SailException> iterator = parent.iterator();

			@Override
			public void close() throws SailException {
				iterator.close();
			}

			@Override
			boolean localHasNext() throws SailException {
				return iterator.hasNext();
			}

			@Override
			ValidationTuple loggingNext() throws SailException {

				ValidationTuple next = iterator.next();

				// TODO: We need to enrich the next tuple with all targets that match next and the effective target. One
				// ValidationTuple could turn into multiple, so we should maybe have "calculateNext" method and some
				// sort of buffer.

				System.out.println(next);

				return next;
			}

			@Override
			public void remove() throws SailException {
				iterator.remove();
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
		parent.getPlanAsGraphvizDot(stringBuilder);

	}

	@Override
	public String toString() {
		return "TargetChainPopper";
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		parent.receiveLogger(validationExecutionLogger);
	}
}
