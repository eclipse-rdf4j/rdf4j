/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailSink;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.testsuite.model.ModelTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 3000, unit = MILLISECONDS)
public class SailSourceModelTest extends ModelTest {
	List<LmdbSailStore> stores = new ArrayList<>();
	List<File> storeDirs = new ArrayList<>();

	@Test
	public void testRemove() {
		SailSourceModel sailSourceModel = getNewModel();
		sailSourceModel.add(RDF.TYPE, RDF.TYPE, RDF.TYPE);
		sailSourceModel.remove(RDF.TYPE, RDF.TYPE, RDF.TYPE);
		assertThat(sailSourceModel.contains(RDF.TYPE, RDF.TYPE, RDF.TYPE)).isFalse();
	}

	@Test
	public void testRemoveTermIteration() {
		SailSourceModel sailSourceModel = getNewModel();
		sailSourceModel.add(RDF.TYPE, RDF.TYPE, RDF.TYPE);
		sailSourceModel.removeTermIteration((Iterator<Statement>) mock(Iterator.class), RDF.TYPE, RDF.TYPE, RDF.TYPE);
		assertThat(sailSourceModel.contains(RDF.TYPE, RDF.TYPE, RDF.TYPE)).isFalse();
	}

	@Test
	@Timeout(value = 10_000, unit = MILLISECONDS)
	void abandonedValueIteratorDoesNotBlockDatasetRollover() throws Exception {
		SailSourceModel model = getNewModel();
		Value first = SimpleValueFactory.getInstance().createLiteral("first");
		Value second = SimpleValueFactory.getInstance().createLiteral("second");
		model.add(RDF.TYPE, RDF.VALUE, first);
		model.add(RDF.TYPE, RDF.VALUE, second);
		Set<Value> objects = model.objects();
		assertThat(objects).hasSize(2);

		Iterator<Value> held = objects.iterator();
		Value removed = held.next();
		assertThat(objects.remove(removed)).isTrue();
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Integer> size = executor.submit(objects::size);
		try {
			assertThat(size.get(1_000, MILLISECONDS)).isEqualTo(1);
		} finally {
			// The pre-fix path is waiting for this iterator's native read stamp. Release it so the deliberately red
			// run cannot strand Surefire, then wait for orderly worker termination.
			model.closeIterator(held);
			try {
				size.get(5_000, MILLISECONDS);
			} finally {
				executor.shutdownNow();
			}
		}
	}

	@Test
	void datasetRolloverCompletesCleanupAfterRuntimeFailures() {
		SailSource source = mock(SailSource.class);
		SailDataset dataset = mock(SailDataset.class);
		SailSink sink = mock(SailSink.class);
		@SuppressWarnings("unchecked")
		CloseableIteration<? extends Statement> firstIterator = mock(CloseableIteration.class);
		@SuppressWarnings("unchecked")
		CloseableIteration<? extends Statement> secondIterator = mock(CloseableIteration.class);
		when(source.dataset(IsolationLevels.NONE)).thenReturn(dataset);
		when(source.sink(IsolationLevels.NONE)).thenReturn(sink);
		doReturn(firstIterator).doReturn(secondIterator).when(dataset).getStatements(null, null, null);
		doReturn(new EmptyIteration<>()).when(dataset).getStatements(RDF.TYPE, RDF.VALUE, RDF.TYPE);
		RuntimeException firstCloseFailure = new IllegalStateException("first iterator close");
		RuntimeException secondCloseFailure = new IllegalArgumentException("second iterator close");
		RuntimeException flushFailure = new UnsupportedOperationException("sink flush");
		RuntimeException sinkCloseFailure = new IllegalStateException("sink close");
		RuntimeException datasetCloseFailure = new IllegalArgumentException("dataset close");
		doThrow(firstCloseFailure).when(firstIterator).close();
		doThrow(secondCloseFailure).when(secondIterator).close();
		doThrow(flushFailure).when(sink).flush();
		doThrow(sinkCloseFailure).when(sink).close();
		doThrow(datasetCloseFailure).when(dataset).close();
		SailSourceModel model = new SailSourceModel(source);

		model.iterator();
		model.iterator();
		model.add(RDF.TYPE, RDF.VALUE, RDF.TYPE);
		Throwable thrown = catchThrowable(() -> model.contains(RDF.TYPE, RDF.VALUE, RDF.TYPE));

		List<Throwable> observed = new ArrayList<>();
		observed.add(thrown);
		observed.addAll(List.of(thrown.getSuppressed()));
		assertThat(observed).containsExactlyInAnyOrder(firstCloseFailure, secondCloseFailure, flushFailure,
				sinkCloseFailure, datasetCloseFailure);
		verify(firstIterator).close();
		verify(secondIterator).close();
		verify(sink).flush();
		verify(sink).close();
		verify(dataset).close();
	}

	@Test
	@Override
	@Disabled
	public void testGetStatements_ConcurrentModificationOfModel() {
	}

	@Override
	protected SailSourceModel getNewModel() {
		try {
			File dataDir = Files.createTempDirectory("SailSourceModelTest-").toFile();
			LmdbSailStore store = new LmdbSailStore(dataDir,
					new StoreProperties(),
					new LmdbStoreConfig("spoc"));
			stores.add(store);
			storeDirs.add(dataDir);
			return new SailSourceModel(store);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@AfterEach
	public void tearDown() throws Exception {
		super.tearDown();
		for (int i = 0; i < stores.size(); i++) {
			stores.get(i).close();
			LmdbTestUtil.deleteDir(storeDirs.get(i));
		}
		stores.clear();
		storeDirs.clear();
	}
}
