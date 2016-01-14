/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.geosparql;

import java.util.HashMap;
import java.util.Map;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.context.SpatialContextFactory;

/**
 * This class is responsible for creating the
 * {@link com.spatial4j.core.context.SpatialContext}, {@link SpatialAlegbra} and
 * {@link WktWriter} that will be used. It will first try to load a subclass of
 * itself called
 * "org.eclipse.rdf4j.query.algebra.evaluation.function.geosparql.SpatialSupportInitializer"
 * . This is not provided, and is primarily intended as a way to inject JTS
 * support. If this fails then the following fall-backs are used:
 * <ul>
 * <li>a SpatialContext created by passing system properties with the prefix
 * "spatialSupport." to {@link com.spatial4j.core.context.SpatialContextFactory}
 * . The prefix is stripped from the system property name to form the
 * SpatialContextFactory argument name.</li>
 * <li>a SpatialAlgebra that does not support any operation.</li>
 * <li>a WktWriter that only supports points</li>.
 * </ul>
 */
abstract class SpatialSupport {

	private static final SpatialContext spatialContext;

	private static final SpatialAlgebra spatialAlgebra;

	private static final WktWriter wktWriter;

	static {
		SpatialSupport support;
		try {
			Class<?> cls = Class.forName(
					"org.eclipse.rdf4j.query.algebra.evaluation.function.geosparql.SpatialSupportInitializer", true,
					Thread.currentThread().getContextClassLoader());
			support = (SpatialSupport)cls.newInstance();
		}
		catch (Exception e) {
			support = new DefaultSpatialSupport();
		}
		spatialContext = support.createSpatialContext();
		spatialAlgebra = support.createSpatialAlgebra();
		wktWriter = support.createWktWriter();
	}

	static SpatialContext getSpatialContext() {
		return spatialContext;
	}

	static SpatialAlgebra getSpatialAlgebra() {
		return spatialAlgebra;
	}

	static WktWriter getWktWriter() {
		return wktWriter;
	}

	protected abstract SpatialContext createSpatialContext();

	protected abstract SpatialAlgebra createSpatialAlgebra();

	protected abstract WktWriter createWktWriter();

	private static final class DefaultSpatialSupport extends SpatialSupport {

		private static final String SYSTEM_PROPERTY_PREFIX = "spatialSupport.";

		@Override
		protected SpatialContext createSpatialContext() {
			Map<String, String> args = new HashMap<String, String>();
			for (String key : System.getProperties().stringPropertyNames()) {
				if (key.startsWith(SYSTEM_PROPERTY_PREFIX)) {
					args.put(key.substring(SYSTEM_PROPERTY_PREFIX.length()), System.getProperty(key));
				}
			}
			return SpatialContextFactory.makeSpatialContext(args, Thread.currentThread().getContextClassLoader());
		}

		@Override
		protected SpatialAlgebra createSpatialAlgebra() {
			return new DefaultSpatialAlgebra();
		}

		@Override
		protected WktWriter createWktWriter() {
			return new DefaultWktWriter();
		}
	}
}
