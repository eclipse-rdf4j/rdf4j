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
package org.eclipse.rdf4j.federated.evaluation.concurrent;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.federated.SPARQLBaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TaskWrapperIntegrationTest extends SPARQLBaseTest {

	private final TestTaskWrapper taskWrapper = new TestTaskWrapper();

	@Override
	protected void initFedXConfig() {
		fedxRule.withConfiguration(c -> c.withTaskWrapper(taskWrapper));
	}

	@BeforeEach
	public void resetTaskCount() {
		taskWrapper.taskCount.set(0);
	}

	@Test
	public void testTaskWrapper() throws Exception {
		prepareTest(Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));
		execute("/tests/basic/query01.rq", "/tests/basic/query01.srx", false, true);
		Assertions.assertTrue(taskWrapper.taskCount.get() > 0);
	}

	@Test
	public void testTaskWrapper_Union() throws Exception {
		/* test union query (2 relevant endpoint) */
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));
		execute("/tests/medium/query04.rq", "/tests/medium/query04.srx", false, true);
		Assertions.assertTrue(taskWrapper.taskCount.get() > 0);
	}

	static class TestTaskWrapper implements TaskWrapper {

		AtomicInteger taskCount = new AtomicInteger(0);

		@Override
		public Runnable wrap(Runnable runnable) {
			taskCount.incrementAndGet();
			return runnable;
		}

		@Override
		public <T> Callable<T> wrap(Callable<T> callable) {
			taskCount.incrementAndGet();
			return callable;
		}
	}

}
