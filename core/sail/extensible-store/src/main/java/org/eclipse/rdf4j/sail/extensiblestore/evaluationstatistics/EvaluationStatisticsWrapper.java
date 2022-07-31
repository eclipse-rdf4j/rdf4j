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
package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import java.util.Collection;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.DataStructureInterface;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;

/**
 * A wrapper around a data structure to support evaluation statistics that need to be notified of added or removed
 * statements.
 */
@Experimental
public class EvaluationStatisticsWrapper implements DataStructureInterface {

	private DynamicStatistics dynamicStatistics;
	private final DataStructureInterface delegate;

	public EvaluationStatisticsWrapper(DataStructureInterface delegate, DynamicStatistics dynamicStatistics) {
		this.delegate = delegate;
		this.dynamicStatistics = dynamicStatistics;
	}

	@Override
	public void addStatement(ExtensibleStatement statement) {
		delegate.addStatement(statement);
		dynamicStatistics.add(statement);
	}

	@Override
	public void removeStatement(ExtensibleStatement statement) {
		delegate.removeStatement(statement);
		dynamicStatistics.remove(statement);

	}

	@Override
	public CloseableIteration<? extends ExtensibleStatement, SailException> getStatements(Resource subject,
			IRI predicate,
			Value object, boolean inferred, Resource... context) {
		return delegate.getStatements(subject, predicate, object, inferred, context);
	}

	@Override
	public void flushForReading() {
		delegate.flushForReading();
	}

	@Override
	public void init() {
		delegate.init();
	}

	@Override
	public void clear(boolean inferred, Resource[] contexts) {
		delegate.clear(inferred, contexts);
	}

	@Override
	public void flushForCommit() {
		delegate.flushForCommit();
	}

	@Override
	public boolean removeStatementsByQuery(Resource subj, IRI pred, Value obj, boolean inferred, Resource[] contexts) {
		dynamicStatistics.removeByQuery(subj, pred, obj, inferred, contexts);
		return delegate.removeStatementsByQuery(subj, pred, obj, inferred, contexts);
	}

	@Override
	public void addStatement(Collection<ExtensibleStatement> statements) {
		delegate.addStatement(statements);
		statements.forEach(dynamicStatistics::add);
	}

	@Override
	public void removeStatement(Collection<ExtensibleStatement> statements) {
		delegate.addStatement(statements);
		statements.forEach(dynamicStatistics::remove);
	}

	@Override
	public long getEstimatedSize() {
		return delegate.getEstimatedSize();
	}

	public void setEvaluationStatistics(DynamicStatistics dynamicStatistics) {
		this.dynamicStatistics = dynamicStatistics;
	}
}
