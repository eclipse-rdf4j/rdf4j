/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;

import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.testers.CollectionIteratorTester;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Unit tests for {@link Model} implementations to check conformance with Java Collection Framework.
 *
 * @author Jeen Broekstra
 *
 */
public class ModelCollectionTest {
	public static Test suite() {
		return new ModelCollectionTest().allTests();
	}

	public Test allTests() {
		TestSuite suite = new TestSuite("org.eclipse.rdf4j.model.ModelCollectionTest");
		suite.addTest(testModelImpl("LinkedHashModel", new LinkedHashModelFactory()));
		suite.addTest(testModelImpl("TreeModel", new TreeModelFactory()));
		suite.addTest(testModelImpl("DynamicModel", new DynamicModelFactory()));
		return suite;
	}

	public Test testModelImpl(String name, ModelFactory factory) {
		try {
			return SetTestSuiteBuilder.using(new TestModelGenerator(factory))
					.named(name)
					.withFeatures(CollectionSize.ANY, CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
							CollectionFeature.SERIALIZABLE, CollectionFeature.SUPPORTS_ADD,
							CollectionFeature.SUPPORTS_ITERATOR_REMOVE, CollectionFeature.SUPPORTS_REMOVE,
							CollectionFeature.NON_STANDARD_TOSTRING)
					// FIXME suppressing test on iterator element remove behavior
					.suppressing(CollectionIteratorTester.class
							.getDeclaredMethod("testIterator_unknownOrderRemoveSupported"))
					.createTestSuite();
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

}
