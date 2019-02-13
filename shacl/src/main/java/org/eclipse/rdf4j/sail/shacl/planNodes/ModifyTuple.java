/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.apache.commons.lang.NotImplementedException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

public class ModifyTuple implements PlanNode {
	PlanNode parent;
	ModifyTupleInterface function;

	public ModifyTuple(PlanNode parent, ModifyTupleInterface function) {
		this.parent = parent;
		this.function = function;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {


			CloseableIteration<Tuple, SailException> parentIterator = parent.iterator();

			@Override
			public void close() throws SailException {
				parentIterator.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				return parentIterator.hasNext();
			}

			@Override
			public Tuple next() throws SailException {
				return function.modify(parentIterator.next());
			}

			@Override
			public void remove() throws SailException {
				throw new NotImplementedException();
			}
		};
	}

	@Override
	public int depth() {
		return parent.depth() + 1;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {

	}

	@Override
	public String getId() {
		return null;
	}

	@Override
	public IteratorData getIteratorDataType() {
		return parent.getIteratorDataType();
	}

	public interface ModifyTupleInterface {
		Tuple modify(Tuple t);

	}
}






