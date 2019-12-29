package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleSailStore;

public class ExtensibleDynamicEvaluationStatistics extends ExtensibleEvaluationStatistics implements  DynamicStatistics{
	public ExtensibleDynamicEvaluationStatistics(ExtensibleSailStore extensibleSailStore) {
		super(extensibleSailStore);
	}

	@Override
	protected CardinalityCalculator createCardinalityCalculator() {
		return cardinalityCalculator;
	}

	CardinalityCalculator cardinalityCalculator = new CardinalityCalculator() {
	};


	@Override
	public void add(Statement statement, boolean inferred) {

	}

	@Override
	public void remove(Statement statement, boolean inferred) {

	}

	@Override
	public void removeByQuery(Resource subj, IRI pred, Value obj, boolean inferred, Resource... contexts) {

	}
}
