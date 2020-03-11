/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.apache.druid.hll.HyperLogLogCollector;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleSailStore;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class ExtensibleDynamicEvaluationStatistics extends ExtensibleEvaluationStatistics implements DynamicStatistics {
	private static final Logger logger = LoggerFactory.getLogger(ExtensibleDynamicEvaluationStatistics.class);
	private static final int QUEUE_LIMIT = 128;
	private static final int SINGLE_DIMENSION_INDEX_SIZE = 1024;

	ConcurrentLinkedQueue<StatementQueueItem> queue = new ConcurrentLinkedQueue<StatementQueueItem>();

	AtomicInteger queueSize = new AtomicInteger();

	private final HashFunction HASH_FUNCTION = Hashing.murmur3_128();

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
	volatile private Thread queueConsumingThread;

	public ExtensibleDynamicEvaluationStatistics(ExtensibleSailStore extensibleSailStore) {
		super(extensibleSailStore);

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

	@Override
	synchronized public double staleness(long actualSize) {

		double estimatedSize = size.estimateCardinality() - size_removed.estimateCardinality();

		double diff = Math.abs(estimatedSize - actualSize);

		double staleness = 1.0 / Math.max(estimatedSize, actualSize) * diff;

		logger.debug("Actual size: {}; estimated size: {}; staleness: {}", actualSize, estimatedSize, staleness);

		return staleness;

	}

	CardinalityCalculator cardinalityCalculator = new CardinalityCalculator() {

		@Override
		synchronized protected double getCardinality(StatementPattern sp) {
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
		synchronized protected double getSubjectCardinality(Var var) {
			if (var.getValue() == null) {
				return size.estimateCardinality();
			} else {
				return getHllCardinality(subjectIndex, subjectIndex_removed, var.getValue());
			}

		}

		@Override
		synchronized protected double getPredicateCardinality(Var var) {
			if (var.getValue() == null) {
				return size.estimateCardinality();
			} else {
				return getHllCardinality(predicateIndex, predicateIndex_removed, var.getValue());
			}
		}

		@Override
		synchronized protected double getObjectCardinality(Var var) {
			if (var.getValue() == null) {
				return size.estimateCardinality();
			} else {
				return getHllCardinality(objectIndex, objectIndex_removed, var.getValue());
			}
		}

		@Override
		synchronized protected double getContextCardinality(Var var) {
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

		int indexIntoMap = Math.abs(value.hashCode() % SINGLE_DIMENSION_INDEX_SIZE);

		double cardinalityAdded = index.getOrDefault(indexIntoMap, EMPTY_HLL).estimateCardinality();
		double cardinalityRemoved = index_removed.getOrDefault(indexIntoMap, EMPTY_HLL).estimateCardinality();

		return cardinalityAdded - cardinalityRemoved;
	}

	@Override
	public void add(ExtensibleStatement statement) {

		queue.add(new StatementQueueItem(statement, StatementQueueItem.Type.added));

		int size = queueSize.incrementAndGet();
		if (size > QUEUE_LIMIT && queueConsumingThread == null) {
			startQueueConsumingThread();
		}
	}

	synchronized private void startQueueConsumingThread() {
		if (queueConsumingThread == null) {
			queueConsumingThread = new Thread(() -> {
				try {
					while (!queue.isEmpty()) {
						StatementQueueItem poll = queue.poll();
						queueSize.decrementAndGet();
						Statement statement = poll.statement;
						byte[] statementHash = HASH_FUNCTION
								.hashString(statement.toString(), StandardCharsets.UTF_8)
								.asBytes();

						if (poll.type == StatementQueueItem.Type.added) {

							handleStatement(statement, statementHash, size, subjectIndex, predicateIndex, objectIndex,
									subjectPredicateIndex, predicateObjectIndex, defaultContext, contextIndex);

						} else { // removed

							assert poll.type == StatementQueueItem.Type.removed;

							handleStatement(statement, statementHash, size_removed, subjectIndex_removed,
									predicateIndex_removed, objectIndex_removed, subjectPredicateIndex_removed,
									predicateObjectIndex_removed, defaultContext_removed, contextIndex_removed);

						}

						if (queue.isEmpty()) {
							try {
								Thread.sleep(2);
							} catch (InterruptedException ignored) {

							}
						}
					}
				} finally {
					queueConsumingThread = null;
				}

			});

			queueConsumingThread.setDaemon(true);
			queueConsumingThread.start();

		}
	}

	synchronized private void handleStatement(Statement statement, byte[] statementHash, HyperLogLogCollector size,
			Map<Integer, HyperLogLogCollector> subjectIndex, Map<Integer, HyperLogLogCollector> predicateIndex,
			Map<Integer, HyperLogLogCollector> objectIndex, HyperLogLogCollector[][] subjectPredicateIndex,
			HyperLogLogCollector[][] predicateObjectIndex, HyperLogLogCollector defaultContext,
			Map<Integer, HyperLogLogCollector> contextIndex) {
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

	static class StatementQueueItem {
		ExtensibleStatement statement;
		Type type;

		public StatementQueueItem(ExtensibleStatement statement, Type type) {
			this.statement = statement;
			this.type = type;
		}

		enum Type {
			added,
			removed;
		}
	}

	private void indexTwoValues(byte[] statementHash, HyperLogLogCollector[][] index, int indexHash, int indexHash2) {
		index[Math.abs(indexHash % index.length)][Math.abs(indexHash2 % index.length)].add(statementHash);

	}

	private void indexOneValue(byte[] statementHash, Map<Integer, HyperLogLogCollector> index, int indexHash) {
		index.compute(Math.abs(indexHash % SINGLE_DIMENSION_INDEX_SIZE), (key, val) -> {
			if (val == null) {
				val = HyperLogLogCollector.makeLatestCollector();
			}
			val.add(statementHash);
			return val;
		});
	}

	@Override
	public void remove(ExtensibleStatement statement) {

		queue.add(new StatementQueueItem(statement, StatementQueueItem.Type.removed));

		int size = queueSize.incrementAndGet();
		if (size > QUEUE_LIMIT && queueConsumingThread == null) {
			startQueueConsumingThread();
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
		while (queueConsumingThread != null) {
			try {
				queueConsumingThread.join();
			} catch (NullPointerException ignored) {
			}
		}
	}
}
