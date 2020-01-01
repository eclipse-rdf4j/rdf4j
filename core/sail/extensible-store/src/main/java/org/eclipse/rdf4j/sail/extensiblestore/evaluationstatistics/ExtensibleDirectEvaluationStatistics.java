package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleSailStore;

import java.util.stream.Stream;

public class ExtensibleDirectEvaluationStatistics extends ExtensibleEvaluationStatistics {
	public ExtensibleDirectEvaluationStatistics(ExtensibleSailStore extensibleSailStore) {
		super(extensibleSailStore);
	}

	@Override
	protected CardinalityCalculator createCardinalityCalculator() {
		return cardinalityCalculator;
	}

	CardinalityCalculator cardinalityCalculator = new CardinalityCalculator() {
		@Override
		protected double getCardinality(StatementPattern sp) {

			SailDataset dataset = extensibleSailStore.getExplicitSailSource().dataset(IsolationLevels.NONE);

			Resource subject = (Resource) sp.getSubjectVar().getValue();
			IRI predicate = (IRI) sp.getPredicateVar().getValue();
			Value object = sp.getObjectVar().getValue();

			if (sp.getScope() == StatementPattern.Scope.DEFAULT_CONTEXTS) {
				try (Stream<? extends Statement> stream = Iterations
						.stream(dataset.getStatements(subject, predicate, object))) {
					return stream.count();
				}
			} else {
				Resource[] context = new Resource[] { (Resource) sp.getContextVar().getValue() };
				try (Stream<? extends Statement> stream = Iterations
						.stream(dataset.getStatements(subject, predicate, object, context))) {
					return stream.count();
				}
			}

		}
	};

}
