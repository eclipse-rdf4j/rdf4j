/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.common.iteration;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author akam
 */
@RunWith(Suite.class)
@SuiteClasses({ LimitIterationTest.class, EmptyIterationTest.class, OffsetIterationTest.class,
		ConvertingIterationTest.class, CloseableIteratorIterationTest.class, DelayedIterationTest.class,
		DistinctIterationTest.class, ExceptionConvertingIterationTest.class, FilterIterationTest.class,
		IntersectionIterationTest.class, DistinctIntersectionIterationTest.class, IteratorIterationTest.class,
		LookAheadIterationTest.class, MinusIterationTest.class, DistinctMinusIterationTest.class,
		SingletonIterationTest.class, UnionIterationTest.class, })
public class AllTests {
}
