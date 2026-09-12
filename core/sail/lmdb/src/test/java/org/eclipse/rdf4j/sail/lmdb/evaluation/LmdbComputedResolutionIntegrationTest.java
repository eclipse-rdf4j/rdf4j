/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAggregateCompiler.UNKNOWN;
import static org.eclipse.rdf4j.sail.lmdb.evaluation.NativeValueResolutionContractTest.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.sail.lmdb.ValueStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Full-module gate: actual ValueStore dictionary/read transactions, not run by the isolated fixture harness. */
public class LmdbComputedResolutionIntegrationTest {
	@TempDir
	File dir;

	private static final class StoreSource extends Source {
		final ValueStore store;

		StoreSource(ValueStore store) {
			this.store = store;
		}

		@Override
		public Object idSpace() {
			return store;
		}

		@Override
		public Object valueLookupScope() {
			try {
				return store.valueLookupScope();
			} catch (IOException e) {
				throw new QueryEvaluationException(e);
			}
		}

		@Override
		public long idOf(Value value) {
			calls.incrementAndGet();
			try {
				return store.getId(value);
			} catch (IOException e) {
				throw new QueryEvaluationException(e);
			}
		}
	}

	@Test
	public void repeatedMissingComputedLabelAvoidsRepeatedValueStoreGetId() throws Exception {
		ValueStore store = new ValueStore(new File(dir, "values"), new LmdbStoreConfig().setInlineLiterals(false));
		try {
			StoreSource source = new StoreSource(store);
			SyntheticValueSource q = eval(source);
			Value v = VF.createLiteral("urn:a-long-computed-type-label-absent-from-the-dictionary");
			try {
				long first = q.internComputedValue(v);
				for (int i = 0; i < 10000; i++)
					equal(first, q.internComputedValue(VF.createLiteral(v.stringValue())));
				equal(1, source.calls.get());
				equal(UNKNOWN, q.idOf(v));
				equal(1, source.calls.get());
			} finally {
				q.executionContext().close();
			}
		} finally {
			store.close();
		}
	}

	@Test
	public void concurrentDictionaryCommitInvalidatesCachedMissWithinTheEvaluation() throws Exception {
		ValueStore store = new ValueStore(new File(dir, "values"), new LmdbStoreConfig().setInlineLiterals(false));
		ExecutorService writer = Executors.newSingleThreadExecutor();
		try {
			StoreSource source = new StoreSource(store);
			SyntheticValueSource q = eval(source);
			Value v = VF.createLiteral("long-result-created-after-the-first-read");
			try {
				q.internComputedValue(v);
				Object before = store.valueLookupScope();
				long stored = writer.submit(() -> {
					store.startTransaction(true);
					long id = store.storeValue(v);
					store.commit();
					return id;
				}).get();
				check(before != store.valueLookupScope());
				equal(stored, q.internComputedValue(v));
				equal(2, source.calls.get());
			} finally {
				q.executionContext().close();
			}
		} finally {
			writer.shutdownNow();
			store.close();
		}
	}

	@Test
	public void writeOwnerDoesNotReuseReadOnlyAbsence() throws Exception {
		ValueStore store = new ValueStore(new File(dir, "values"), new LmdbStoreConfig().setInlineLiterals(false));
		try {
			StoreSource source = new StoreSource(store);
			SyntheticValueSource q = eval(source);
			Value v = VF.createLiteral("long-result-created-in-a-private-write-transaction");
			try {
				q.internComputedValue(v);
				store.startTransaction(true);
				check(store.valueLookupScope() == null);
				long id = store.storeValue(v);
				equal(id, q.internComputedValue(v));
				store.commit();
				equal(id, q.internComputedValue(v));
			} finally {
				q.executionContext().close();
			}
		} finally {
			store.close();
		}
	}
}
