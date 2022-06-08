/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base;

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.DualUnionIteration;
import org.eclipse.rdf4j.common.iteration.UnionIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Combines multiple {@link SailDataset} into a single view.
 *
 * @author James Leigh
 */
class UnionSailDataset implements SailDataset {

	/**
	 * Set of {@link SailDataset}s to combine.
	 */
	private final SailDataset dataset1;
	private final SailDataset dataset2;

	/**
	 * Creates a new {@link SailDataset} that unions two other sail datasets.
	 */
	public UnionSailDataset(SailDataset dataset1, SailDataset dataset2) {
		this.dataset1 = dataset1;
		this.dataset2 = dataset2;
	}

	public static SailDataset getInstance(SailDataset dataset1, SailDataset dataset2) {
		if (dataset1.isDefinitelyEmpty()) {
			dataset1.close();
			return dataset2;
		}
		if (dataset2.isDefinitelyEmpty()) {
			dataset2.close();
			return dataset1;
		}
		return new UnionSailDataset(dataset1, dataset2);
	}

	@Override
	public String toString() {
		return List.of(dataset1, dataset2).toString();
	}

	@Override
	public void close() throws SailException {
		try {
			dataset1.close();
		} finally {
			dataset2.close();
		}
	}

	@Override
	public CloseableIteration<? extends Namespace, SailException> getNamespaces() throws SailException {
		return DualUnionIteration.getWildcardInstance(dataset1.getNamespaces(), dataset2.getNamespaces());
	}

	@Override
	public String getNamespace(String prefix) throws SailException {
		String namespace = dataset1.getNamespace(prefix);
		if (namespace == null) {
			namespace = dataset2.getNamespace(prefix);
		}
		return namespace;
	}

	@Override
	public CloseableIteration<? extends Resource, SailException> getContextIDs() throws SailException {
		return DualUnionIteration.getWildcardInstance(dataset1.getContextIDs(), dataset2.getContextIDs());
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj,
			Resource... contexts) throws SailException {
		return DualUnionIteration.getWildcardInstance(dataset1.getStatements(subj, pred, obj, contexts),
				dataset2.getStatements(subj, pred, obj, contexts));
	}

	@Override
	public boolean hasStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		return dataset1.hasStatement(subj, pred, obj, contexts) || dataset2.hasStatement(subj, pred, obj, contexts);
	}

	@Override
	public CloseableIteration<? extends Triple, SailException> getTriples(Resource subj, IRI pred, Value obj)
			throws SailException {
		return DualUnionIteration.getWildcardInstance(dataset1.getTriples(subj, pred, obj),
				dataset2.getTriples(subj, pred, obj));
	}

	@Override
	public boolean isDefinitelyEmpty() {
		return dataset1.isDefinitelyEmpty() && dataset2.isDefinitelyEmpty();
	}

}
