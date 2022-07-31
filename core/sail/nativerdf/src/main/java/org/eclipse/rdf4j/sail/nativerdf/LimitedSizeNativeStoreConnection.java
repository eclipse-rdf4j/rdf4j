/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.IOException;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.limited.LimitedSizeEvaluationStrategy;

/**
 * @author Jerven Bolleman, SIB Swiss Institute of Bioinformatics
 */
public class LimitedSizeNativeStoreConnection extends NativeStoreConnection {

	private int maxCollectionsSize = Integer.MAX_VALUE;

	/**
	 * @param nativeStore
	 * @throws IOException
	 */
	protected LimitedSizeNativeStoreConnection(NativeStore nativeStore) throws IOException {
		super(nativeStore);
	}

	public int getMaxCollectionsSize() {
		return maxCollectionsSize;
	}

	public void setMaxCollectionsSize(int maxCollectionsSize) {
		this.maxCollectionsSize = maxCollectionsSize;
	}

	@Override
	protected EvaluationStrategy getEvaluationStrategy(Dataset dataset, TripleSource tripleSource) {
		return new LimitedSizeEvaluationStrategy(tripleSource, dataset, maxCollectionsSize,
				getFederatedServiceResolver());
	}
}
