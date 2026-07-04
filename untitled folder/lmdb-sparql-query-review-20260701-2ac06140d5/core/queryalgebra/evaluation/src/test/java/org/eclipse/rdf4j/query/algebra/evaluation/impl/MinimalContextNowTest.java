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
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MinimalContextNowTest {

	@Test
	public void testNow() {
		// Tests that the now value is correctly initialized.
		QueryEvaluationContext.Minimal context = new QueryEvaluationContext.Minimal(null);
		QueryValueEvaluationStep prepared = new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(
				context.getNow());

		Assertions.assertNotNull(prepared);

		Value nowValue = prepared.evaluate(EmptyBindingSet.getInstance());

		Assertions.assertTrue(nowValue.isLiteral());
		Assertions.assertEquals(CoreDatatype.XSD.DATETIME, ((Literal) nowValue).getCoreDatatype());
	}

	@Test
	public void testConcurrentAccessToNow() throws ExecutionException, InterruptedException {
		int numberOfIterations = 100;
		int numberOfThreads = 10;

		ExecutorService executorService = Executors.newCachedThreadPool();
		try {

			for (int i = 0; i < numberOfIterations; i++) {
				QueryEvaluationContext.Minimal context = new QueryEvaluationContext.Minimal(null);

				CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

				List<? extends Future<Literal>> futures = IntStream.range(0, numberOfThreads)
						.mapToObj(j -> executorService.submit(() -> {
							try {
								countDownLatch.countDown();
								countDownLatch.await();
								return context.getNow();
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
						}))
						.collect(Collectors.toList());

				Literal prev = null;
				for (Future<Literal> future : futures) {
					Literal now = future.get();
					Assertions.assertNotNull(now);
					if (prev != null) {
						Assertions.assertSame(prev, now);
					}
					prev = now;
				}

			}

		} finally {
			List<Runnable> runnables = executorService.shutdownNow();
			Assertions.assertTrue(runnables.isEmpty());
		}

	}
}
