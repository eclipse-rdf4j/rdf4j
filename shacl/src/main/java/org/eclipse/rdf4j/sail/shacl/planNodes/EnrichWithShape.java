/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.AST.PropertyShape;

/**
 * @author Håvard Mikkelsen Ottestad
 */
public class EnrichWithShape implements PlanNode {

	private final PropertyShape propertyShape;
	private final PlanNode parent;
	private boolean printed = false;
	boolean closed;

	public EnrichWithShape(PlanNode parent, PropertyShape propertyShape) {
		this.parent = parent;
		this.propertyShape = propertyShape;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		if(closed){
			throw new IllegalStateException();
		}
		return new CloseableIteration<Tuple, SailException>() {

			CloseableIteration<Tuple, SailException> parentIterator = parent.iterator();

			@Override
			public boolean hasNext() throws SailException {
				return parentIterator.hasNext();
			}

			@Override
			public Tuple next() throws SailException {
				Tuple next = parentIterator.next();
				next.addCausedByPropertyShape(propertyShape);
				return next;
			}

			@Override
			public void remove() throws SailException {
				parentIterator.remove();
			}

			@Override
			public void close() throws SailException {
				closed = true;
				if(parentIterator != null){
					parentIterator.close();
					parentIterator = null;
				}
			}
		};
	}

	@Override
	public int depth() {
		return parent.depth();
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if(printed) return;
		printed = true;
		stringBuilder.append(getId()).append(" [label=\"").append(StringEscapeUtils.escapeJava(this.toString())).append("\"];").append("\n");
		stringBuilder.append(parent.getId()).append(" -> ").append(getId()).append("\n");
		parent.getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String getId() {
		return System.identityHashCode(this)+"";
	}


	@Override
	public IteratorData getIteratorDataType() {
		return parent.getIteratorDataType();
	}

	@Override
	public String toString() {
		return "EnrichWithShape";
	}

	public PropertyShape getPropertyShape() {
		return propertyShape;
	}
}
