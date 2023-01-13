/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.base;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.DualUnionIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Combines two {@link SailDataset} into a single view.
 *
 * @author Håvard M. Ottestad
 */
class UnionSailDataset implements SailDataset {

	private final SailDataset dataset1;
	private final SailDataset dataset2;

	private UnionSailDataset(SailDataset dataset1, SailDataset dataset2) {
		this.dataset1 = dataset1;
		this.dataset2 = dataset2;
	}

	/**
	 * Creates a new {@link SailDataset} that includes both the provided {@link SailDataset}s.
	 */
	public static SailDataset getInstance(SailDataset dataset1, SailDataset dataset2) {
		return new UnionSailDataset(dataset1, dataset2);
	}

	@Override
	public String toString() {
		return "UnionSailDataset{" +
				"dataset1=" + dataset1 +
				", dataset2=" + dataset2 +
				'}';
	}

	@Override
	public void close() {
		try {
			dataset1.close();
		} finally {
			dataset2.close();
		}
	}

	@Override
	public CloseableIteration<? extends Namespace> getNamespaces() {

		CloseableIteration<? extends Namespace> iteration1 = null;
		CloseableIteration<? extends Namespace> iteration2 = null;
		try {
			iteration1 = dataset1.getNamespaces();
			iteration2 = dataset2.getNamespaces();
			return union(iteration1, iteration2);
		} catch (Throwable t) {
			try {
				if (iteration1 != null) {
					iteration1.close();
				}
			} finally {
				if (iteration2 != null) {
					iteration2.close();
				}
			}
			throw t;
		}
	}

	@Override
	public String getNamespace(String prefix) {
		String namespace = dataset1.getNamespace(prefix);
		if (namespace != null) {
			return namespace;
		} else {
			return dataset2.getNamespace(prefix);
		}
	}

	@Override
	public CloseableIteration<? extends Resource> getContextIDs() {
		CloseableIteration<? extends Resource> iteration1 = null;
		CloseableIteration<? extends Resource> iteration2 = null;
		try {
			iteration1 = dataset1.getContextIDs();
			iteration2 = dataset2.getContextIDs();
			return union(iteration1, iteration2);
		} catch (Throwable t) {
			try {
				if (iteration1 != null) {
					iteration1.close();
				}
			} finally {
				if (iteration2 != null) {
					iteration2.close();
				}
			}
			throw t;
		}
	}

	@Override
	public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
			Resource... contexts) {

		CloseableIteration<? extends Statement> iteration1 = null;
		CloseableIteration<? extends Statement> iteration2 = null;
		try {
			iteration1 = dataset1.getStatements(subj, pred, obj, contexts);
			iteration2 = dataset2.getStatements(subj, pred, obj, contexts);
			return union(iteration1, iteration2);
		} catch (Throwable t) {
			try {
				if (iteration1 != null) {
					iteration1.close();
				}
			} finally {
				if (iteration2 != null) {
					iteration2.close();
				}
			}
			throw t;
		}

	}

	@Override
	public CloseableIteration<? extends Triple> getTriples(Resource subj, IRI pred, Value obj) {

		CloseableIteration<? extends Triple> iteration1 = null;
		CloseableIteration<? extends Triple> iteration2 = null;
		try {
			iteration1 = dataset1.getTriples(subj, pred, obj);
			iteration2 = dataset2.getTriples(subj, pred, obj);
			return union(iteration1, iteration2);
		} catch (Throwable t) {
			try {
				if (iteration1 != null) {
					iteration1.close();
				}
			} finally {
				if (iteration2 != null) {
					iteration2.close();
				}
			}
			throw t;
		}

	}

	private <T> CloseableIteration<? extends T> union(
			CloseableIteration<? extends T> iteration1,
			CloseableIteration<? extends T> iteration2) {
		return DualUnionIteration.getWildcardInstance(iteration1, iteration2);
	}

}
