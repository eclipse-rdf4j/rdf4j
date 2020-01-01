/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics.DynamicStatistics;
import org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics.EvaluationStatisticsEnum;
import org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics.EvaluationStisticsWrapper;
import org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics.ExtensibleDynamicEvaluationStatistics;
import org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics.ExtensibleEvaluationStatistics;

/**
 * @author Håvard Mikkelsen Ottestad
 */
public class ExtensibleSailStore implements SailStore {

	private ExtensibleSailSource sailSource;
	private ExtensibleSailSource sailSourceInferred;
	private ExtensibleEvaluationStatistics evaluationStatistics;

	public ExtensibleSailStore(DataStructureInterface dataStructure, DataStructureInterface dataStructureInferred,
			NamespaceStoreInterface namespaceStore, EvaluationStatisticsEnum evaluationStatisticsType) {
		evaluationStatistics = evaluationStatisticsType.getInstance(this);

		if (evaluationStatistics instanceof DynamicStatistics) {
			dataStructure = new EvaluationStisticsWrapper(dataStructure, (DynamicStatistics) evaluationStatistics,
					false);
			dataStructureInferred = new EvaluationStisticsWrapper(dataStructureInferred,
					(DynamicStatistics) evaluationStatistics, true);
		}

		sailSource = new ExtensibleSailSource(dataStructure, namespaceStore);
		sailSourceInferred = new ExtensibleSailSource(dataStructureInferred, namespaceStore);
	}

	@Override
	public void close() throws SailException {

		sailSource.close();
		sailSourceInferred.close();
	}

	@Override
	public ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	@Override
	public EvaluationStatistics getEvaluationStatistics() {
		return evaluationStatistics;
	}

	@Override
	public SailSource getExplicitSailSource() {
		return sailSource;
	}

	@Override
	public SailSource getInferredSailSource() {
		return sailSourceInferred;
	}

	public void init() {
		sailSource.init();
		sailSourceInferred.init();
	}

}
