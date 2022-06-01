/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.limited;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;

/**
 * @author Jerven Bolleman, SIB Swiss Institute of Bioinformatics
 */
public class LimitedSizeEvaluationStrategy extends StrictEvaluationStrategy {

	private final AtomicLong used = new AtomicLong();

	private final long maxSize;

	/**
	 * @param tripleSource
	 */
	public LimitedSizeEvaluationStrategy(TripleSource tripleSource, long maxSize,
			FederatedServiceResolver serviceManager) {
		super(tripleSource, serviceManager);
		this.maxSize = maxSize;
	}

	/**
	 * @param tripleSource
	 * @param dataset
	 * @param maxCollectionsSize
	 */
	public LimitedSizeEvaluationStrategy(TripleSource tripleSource, Dataset dataset, int maxCollectionsSize,
			FederatedServiceResolver serviceManager) {
		super(tripleSource, dataset, serviceManager);
		this.maxSize = maxCollectionsSize;
	}

	@Deprecated
	@Override
	public <B> Set<B> makeSet() {
		Set<B> wrapped = super.makeSet();
		return new LimitedSizeSet<B>(wrapped, used, maxSize);
	}

	@Override
	public CollectionFactory getCollectionFactory() {
		return new LimitedSizeCollectionFactory(super.getCollectionFactory(), used, maxSize);
	}

}
