package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.apache.druid.hll.HyperLogLogCollector;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleSailStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public class ExtensibleDynamicEvaluationStatistics extends ExtensibleEvaluationStatistics implements DynamicStatistics {
	private static final Logger logger = LoggerFactory.getLogger(ExtensibleDynamicEvaluationStatistics.class);

	HashFunction hashFunction = Hashing.murmur3_128();

	HyperLogLogCollector size = HyperLogLogCollector.makeLatestCollector();

	HyperLogLogCollector[] subjectIndex = new HyperLogLogCollector[1024];
	HyperLogLogCollector[] predicateIndex = new HyperLogLogCollector[1024];
	HyperLogLogCollector[] objectIndex = new HyperLogLogCollector[1024];
	HyperLogLogCollector[] contextIndex = new HyperLogLogCollector[1024];
	HyperLogLogCollector defaultContext = HyperLogLogCollector.makeLatestCollector();

	public ExtensibleDynamicEvaluationStatistics(ExtensibleSailStore extensibleSailStore) {
		super(extensibleSailStore);

		Stream.of(subjectIndex, predicateIndex, objectIndex, contextIndex).parallel().forEach(index -> {
			for (int i = 0; i < index.length; i++) {
				index[i] = HyperLogLogCollector.makeLatestCollector();
			}
		});

	}

	@Override
	protected CardinalityCalculator createCardinalityCalculator() {
		return cardinalityCalculator;
	}

	CardinalityCalculator cardinalityCalculator = new CardinalityCalculator() {

		@Override
		protected double getCardinality(StatementPattern sp) {
			double min = size.estimateCardinality();

			min = Math.min(min, getSubjectCardinality(sp.getSubjectVar()));
			min = Math.min(min, getPredicateCardinality(sp.getPredicateVar()));
			min = Math.min(min, getObjectCardinality(sp.getObjectVar()));

			return min;

		}

		@Override
		protected double getSubjectCardinality(Var var) {
			if (var.getValue() == null) {
				return size.estimateCardinality();
			} else {
				return getHllCardinality(subjectIndex, var.getValue());
			}

		}

		@Override
		protected double getPredicateCardinality(Var var) {
			if (var.getValue() == null) {
				return size.estimateCardinality();
			} else {
				return getHllCardinality(predicateIndex, var.getValue());
			}
		}

		@Override
		protected double getObjectCardinality(Var var) {
			if (var.getValue() == null) {
				return size.estimateCardinality();
			} else {
				return getHllCardinality(objectIndex, var.getValue());
			}
		}

		@Override
		protected double getContextCardinality(Var var) {
			if (var.getValue() == null) {
				return defaultContext.estimateCardinality();
			} else {
				return getHllCardinality(contextIndex, var.getValue());
			}
		}
	};

	private double getHllCardinality(HyperLogLogCollector[] index, Value value) {
		return index[Math.abs(value.hashCode() % index.length)].estimateCardinality();
	}

	@Override
	public void add(Statement statement, boolean inferred) {

		byte[] statementHash = hashFunction.hashString(statement.toString(), StandardCharsets.UTF_8).asBytes();

		size.add(statementHash);
		int subjectHash = statement.getSubject().hashCode();
		int predicateHash = statement.getPredicate().hashCode();
		int objectHash = statement.getObject().hashCode();

		indexSingleValue(statementHash, subjectIndex, subjectHash);
		indexSingleValue(statementHash, predicateIndex, predicateHash);
		indexSingleValue(statementHash, objectIndex, objectHash);

		if (statement.getContext() == null) {
			defaultContext.add(statementHash);
		} else {
			indexSingleValue(statementHash, contextIndex, statement.getContext().hashCode());
		}

		// logger.info("added: {} : {} ", statement, inferred ? "INFERRED" : "REAL");
	}

	private void indexSingleValue(byte[] statementHash, HyperLogLogCollector[] index, int indexHash) {
		index[Math.abs(indexHash % index.length)].add(statementHash);
	}

	@Override
	public void remove(Statement statement, boolean inferred) {
		// logger.info("removed: {} : {} ", statement, inferred ? "INFERRED" : "REAL");

	}

	@Override
	public void removeByQuery(Resource subj, IRI pred, Value obj, boolean inferred, Resource... contexts) {
		// logger.info("removed by query: ?????");

	}
}
