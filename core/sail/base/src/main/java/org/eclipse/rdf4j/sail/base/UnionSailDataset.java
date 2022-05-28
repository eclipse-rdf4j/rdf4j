/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base;

import java.util.Arrays;

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
	private final SailDataset[] datasets;

	/**
	 * Creates a new {@link SailDataset} that includes all the given {@link SailDataset}s.
	 *
	 * @param datasets
	 */
	public UnionSailDataset(SailDataset... datasets) {
		this.datasets = datasets;
	}

	@Override
	public String toString() {
		return Arrays.asList(datasets).toString();
	}

	@Override
	public void close() throws SailException {
		for (SailDataset dataset : datasets) {
			dataset.close();
		}
	}

	@Override
	public CloseableIteration<? extends Namespace, SailException> getNamespaces() throws SailException {

		if (datasets.length == 1) {
			return datasets[0].getNamespaces();
		} else if (datasets.length == 2) {
			return union(datasets[0].getNamespaces(), datasets[1].getNamespaces());
		} else {
			CloseableIteration<? extends Namespace, SailException>[] result = new CloseableIteration[datasets.length];
			for (int i = 0; i < datasets.length; i++) {
				result[i] = datasets[i].getNamespaces();
			}
			return union(result);
		}

	}

	@Override
	public String getNamespace(String prefix) throws SailException {
		for (int i = 0; i < datasets.length; i++) {
			String result = datasets[i].getNamespace(prefix);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	@Override
	public CloseableIteration<? extends Resource, SailException> getContextIDs() throws SailException {

		if (datasets.length == 1) {
			return datasets[0].getContextIDs();
		} else if (datasets.length == 2) {
			return union(datasets[0].getContextIDs(), datasets[1].getContextIDs());
		} else {
			CloseableIteration<? extends Resource, SailException>[] result = new CloseableIteration[datasets.length];
			for (int i = 0; i < datasets.length; i++) {
				result[i] = datasets[i].getContextIDs();
			}
			return union(result);
		}
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj,
			Resource... contexts) throws SailException {
		if (datasets.length == 1) {
			return datasets[0].getStatements(subj, pred, obj, contexts);
		} else if (datasets.length == 2) {
			return union(datasets[0].getStatements(subj, pred, obj, contexts),
					datasets[1].getStatements(subj, pred, obj, contexts));
		} else {
			CloseableIteration<? extends Statement, SailException>[] result = new CloseableIteration[datasets.length];
			for (int i = 0; i < datasets.length; i++) {
				result[i] = datasets[i].getStatements(subj, pred, obj, contexts);
			}
			return union(result);
		}

	}

	@Override
	public CloseableIteration<? extends Triple, SailException> getTriples(Resource subj, IRI pred, Value obj)
			throws SailException {

		if (datasets.length == 1) {
			return datasets[0].getTriples(subj, pred, obj);
		} else if (datasets.length == 2) {
			return union(datasets[0].getTriples(subj, pred, obj), datasets[1].getTriples(subj, pred, obj));
		} else {
			CloseableIteration<? extends Triple, SailException>[] result = new CloseableIteration[datasets.length];
			for (int i = 0; i < datasets.length; i++) {
				result[i] = datasets[i].getTriples(subj, pred, obj);
			}
			return union(result);
		}

	}

	private <T> CloseableIteration<? extends T, SailException> union(
			CloseableIteration<? extends T, SailException>[] items) {
		return new UnionIteration<>(items);
	}

	private <T> CloseableIteration<? extends T, SailException> union(
			CloseableIteration<? extends T, SailException> iteration1,
			CloseableIteration<? extends T, SailException> iteration2) {
		return DualUnionIteration.getWildcardInstance(iteration1, iteration2);
	}

}
