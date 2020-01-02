package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.apache.commons.lang3.StringUtils;
import org.apache.druid.hll.HyperLogLogCollector;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class ExtensibleDynamicEvaluationStatistics extends ExtensibleEvaluationStatistics implements DynamicStatistics {
	private static final Logger logger = LoggerFactory.getLogger(ExtensibleDynamicEvaluationStatistics.class);
	private static final int QUEUE_LIMIT = 128;
	private static final int ONE_DIMENSION_INDEX_SIZE = 1024;

	ConcurrentLinkedQueue<StatemetQueueItem> queue = new ConcurrentLinkedQueue<StatemetQueueItem>();

	AtomicInteger queueSize = new AtomicInteger();

	private final HashFunction hashFunction = Hashing.murmur3_128();

	private final HyperLogLogCollector EMPTY_HLL = HyperLogLogCollector.makeLatestCollector();

	private final HyperLogLogCollector size = HyperLogLogCollector.makeLatestCollector();
	private final HyperLogLogCollector size_removed = HyperLogLogCollector.makeLatestCollector();

	private final Map<Integer, HyperLogLogCollector> subjectIndex = new HashMap<>();
	private final Map<Integer, HyperLogLogCollector> predicateIndex = new HashMap<>();
	private final Map<Integer, HyperLogLogCollector> objectIndex = new HashMap<>();
	private final Map<Integer, HyperLogLogCollector> contextIndex = new HashMap<>();
	private final HyperLogLogCollector defaultContext = HyperLogLogCollector.makeLatestCollector();

	private final HyperLogLogCollector[][] subjectPredicateIndex = new HyperLogLogCollector[64][64];
	private final HyperLogLogCollector[][] predicateObjectIndex = new HyperLogLogCollector[64][64];

	private final Map<Integer, HyperLogLogCollector> subjectIndex_removed = new HashMap<>();
	private final Map<Integer, HyperLogLogCollector> predicateIndex_removed = new HashMap<>();
	private final Map<Integer, HyperLogLogCollector> objectIndex_removed = new HashMap<>();
	private final Map<Integer, HyperLogLogCollector> contextIndex_removed = new HashMap<>();
	private final HyperLogLogCollector defaultContext_removed = HyperLogLogCollector.makeLatestCollector();

	private final HyperLogLogCollector[][] subjectPredicateIndex_removed = new HyperLogLogCollector[64][64];
	private final HyperLogLogCollector[][] predicateObjectIndex_removed = new HyperLogLogCollector[64][64];
	volatile private Thread queueThread;

	public ExtensibleDynamicEvaluationStatistics(ExtensibleSailStore extensibleSailStore) {
		super(extensibleSailStore);

//		Stream.of(subjectIndex, predicateIndex, objectIndex, contextIndex,
//			subjectIndex_removed, predicateIndex_removed, objectIndex_removed, contextIndex_removed)
//			.forEach(index -> {
//				for (int i = 0; i < index.length; i++) {
//					index[i] = HyperLogLogCollector.makeLatestCollector();
//				}
//			});

		Stream.of(subjectPredicateIndex, predicateObjectIndex, subjectPredicateIndex_removed,
				predicateObjectIndex_removed).forEach(index -> {
					for (int i = 0; i < index.length; i++) {
						for (int j = 0; j < index[i].length; j++) {
							index[i][j] = HyperLogLogCollector.makeLatestCollector();
						}
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
			double min = size.estimateCardinality() - size_removed.estimateCardinality();

			min = Math.min(min, getSubjectCardinality(sp.getSubjectVar()));
			min = Math.min(min, getPredicateCardinality(sp.getPredicateVar()));
			min = Math.min(min, getObjectCardinality(sp.getObjectVar()));

			if (sp.getSubjectVar().getValue() != null && sp.getPredicateVar().getValue() != null) {
				min = Math.min(min,
						getHllCardinality(subjectPredicateIndex, subjectPredicateIndex_removed,
								sp.getSubjectVar().getValue(),
								sp.getPredicateVar().getValue()));
			}

			if (sp.getPredicateVar().getValue() != null && sp.getObjectVar().getValue() != null) {
				min = Math.min(min,
						getHllCardinality(predicateObjectIndex, predicateObjectIndex_removed,
								sp.getPredicateVar().getValue(),
								sp.getObjectVar().getValue()));
			}

			return min;

		}

		@Override
		protected double getSubjectCardinality(Var var) {
			if (var.getValue() == null) {
				return size.estimateCardinality();
			} else {
				return getHllCardinality(subjectIndex, subjectIndex_removed, var.getValue());
			}

		}

		@Override
		protected double getPredicateCardinality(Var var) {
			if (var.getValue() == null) {
				return size.estimateCardinality();
			} else {
				return getHllCardinality(predicateIndex, predicateIndex_removed, var.getValue());
			}
		}

		@Override
		protected double getObjectCardinality(Var var) {
			if (var.getValue() == null) {
				return size.estimateCardinality();
			} else {
				return getHllCardinality(objectIndex, objectIndex_removed, var.getValue());
			}
		}

		@Override
		protected double getContextCardinality(Var var) {
			if (var.getValue() == null) {
				return defaultContext.estimateCardinality() - defaultContext_removed.estimateCardinality();
			} else {
				return getHllCardinality(contextIndex, contextIndex_removed, var.getValue());
			}
		}
	};

	private double getHllCardinality(HyperLogLogCollector[][] index, HyperLogLogCollector[][] index_removed,
			Value value1, Value value2) {
		return index[Math.abs(value1.hashCode() % index.length)][Math.abs(value2.hashCode() % index.length)]
				.estimateCardinality()
				- index_removed[Math.abs(value1.hashCode() % index_removed.length)][Math
						.abs(value2.hashCode() % index_removed.length)]
								.estimateCardinality();
	}

	private double getHllCardinality(Map<Integer, HyperLogLogCollector> index,
			Map<Integer, HyperLogLogCollector> index_removed, Value value) {

		return index.getOrDefault(Math.abs(value.hashCode() % ONE_DIMENSION_INDEX_SIZE), EMPTY_HLL)
				.estimateCardinality()
				- index_removed.getOrDefault(Math.abs(value.hashCode() % ONE_DIMENSION_INDEX_SIZE), EMPTY_HLL)
						.estimateCardinality();
	}

	@Override
	public void add(Statement statement, boolean inferred) {

		queue.add(new StatemetQueueItem(statement, inferred, true));

		int size = queueSize.incrementAndGet();
		if (size > QUEUE_LIMIT && queueThread == null) {
			startQueueThread();
		}

		/*
		 * byte[] statementHash = hashFunction.hashString(statement.toString(), StandardCharsets.UTF_8).asBytes();
		 * 
		 * size.add(statementHash); int subjectHash = statement.getSubject().hashCode(); int predicateHash =
		 * statement.getPredicate().hashCode(); int objectHash = statement.getObject().hashCode();
		 * 
		 * indexOneValue(statementHash, subjectIndex, subjectHash); indexOneValue(statementHash, predicateIndex,
		 * predicateHash); indexOneValue(statementHash, objectIndex, objectHash);
		 * 
		 * indexTwoValues(statementHash, subjectPredicateIndex, subjectHash, predicateHash);
		 * indexTwoValues(statementHash, predicateObjectIndex, predicateHash, objectHash);
		 * 
		 * if (statement.getContext() == null) { defaultContext.add(statementHash); } else {
		 * indexOneValue(statementHash, contextIndex, statement.getContext().hashCode()); }
		 * 
		 */

		// logger.info("added: {} : {} ", statement, inferred ? "INFERRED" : "REAL");
	}

	synchronized private void startQueueThread() {
		if (queueThread == null) {
			queueThread = new Thread(() -> {
				try {
					while (!queue.isEmpty()) {
						StatemetQueueItem poll = queue.poll();
						queueSize.decrementAndGet();
						Statement statement = poll.statement;
						byte[] statementHash = hashFunction.hashString(statement.toString(), StandardCharsets.UTF_8)
								.asBytes();

						if (poll.added) {

							size.add(statementHash);
							int subjectHash = statement.getSubject().hashCode();
							int predicateHash = statement.getPredicate().hashCode();
							int objectHash = statement.getObject().hashCode();

							indexOneValue(statementHash, subjectIndex, subjectHash);
							indexOneValue(statementHash, predicateIndex, predicateHash);
							indexOneValue(statementHash, objectIndex, objectHash);

							indexTwoValues(statementHash, subjectPredicateIndex, subjectHash, predicateHash);
							indexTwoValues(statementHash, predicateObjectIndex, predicateHash, objectHash);

							if (statement.getContext() == null) {
								defaultContext.add(statementHash);
							} else {
								indexOneValue(statementHash, contextIndex, statement.getContext().hashCode());
							}

						}
					}
				} finally {
					queueThread = null;
				}

			});

			queueThread.start();

		}
	}

	class StatemetQueueItem {
		Statement statement;
		boolean inferred;
		boolean added;

		public StatemetQueueItem(Statement statement, boolean inferred, boolean added) {
			this.statement = statement;
			this.inferred = inferred;
			this.added = added;
		}
	}

	private void indexTwoValues(byte[] statementHash, HyperLogLogCollector[][] index, int indexHash, int indexHash2) {
		index[Math.abs(indexHash % index.length)][Math.abs(indexHash2 % index.length)].add(statementHash);

	}

	private void indexOneValue(byte[] statementHash, Map<Integer, HyperLogLogCollector> index, int indexHash) {
		index.compute(Math.abs(indexHash % ONE_DIMENSION_INDEX_SIZE), (key, val) -> {
			if (val == null)
				val = HyperLogLogCollector.makeLatestCollector();
			val.add(statementHash);
			return val;
		});
	}

	@Override
	public void remove(Statement statement, boolean inferred) {
		byte[] statementHash = hashFunction.hashString(statement.toString(), StandardCharsets.UTF_8).asBytes();

		size_removed.add(statementHash);
		int subjectHash = statement.getSubject().hashCode();
		int predicateHash = statement.getPredicate().hashCode();
		int objectHash = statement.getObject().hashCode();

		indexOneValue(statementHash, subjectIndex_removed, subjectHash);
		indexOneValue(statementHash, predicateIndex_removed, predicateHash);
		indexOneValue(statementHash, objectIndex_removed, objectHash);

		indexTwoValues(statementHash, subjectPredicateIndex_removed, subjectHash, predicateHash);
		indexTwoValues(statementHash, predicateObjectIndex_removed, predicateHash, objectHash);

		if (statement.getContext() == null) {
			defaultContext_removed.add(statementHash);
		} else {
			indexOneValue(statementHash, contextIndex_removed, statement.getContext().hashCode());
		}
	}

	@Override
	public void removeByQuery(Resource subj, IRI pred, Value obj, boolean inferred, Resource... contexts) {
		// logger.info("removed by query: ?????");

	}

	public String getDistribution() {

		StringBuilder stringBuilder = new StringBuilder();
//		HashMap<String, HyperLogLogCollector[]> stringHashMap = new LinkedHashMap<>();
//
//		stringHashMap.put("subjectIndex", subjectIndex);
//		stringHashMap.put("predicateIndex", predicateIndex);
//		stringHashMap.put("objectIndex", objectIndex);
//		stringHashMap.put("contextIndex", contextIndex);
//
//		stringHashMap.forEach((key, val) -> {
//			stringBuilder
//				.append(StringUtils.rightPad(key, 20, " ")).append(": \t")
//				.append(Arrays
//					.stream(val)
//					.map(HyperLogLogCollector::estimateCardinality)
//					.map(count -> ((int) (1000 / size.estimateCardinality() * count)) / 10.0)
//					.map(percentage -> StringUtils.leftPad(percentage + "", 4, " "))
//					.map(percentage -> "[" + percentage + "%]")
//					.reduce((a, b) -> a + ", " + b)
//					.orElse(" - "))
//				.append("\n");
//		});

		return stringBuilder.toString();

	}

	public void waitForQueue() throws InterruptedException {
		while (!queue.isEmpty()) {
			Thread.yield();
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
		}
	}
}
