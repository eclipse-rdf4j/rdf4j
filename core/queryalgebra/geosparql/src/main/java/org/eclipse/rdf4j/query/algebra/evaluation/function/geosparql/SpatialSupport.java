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
package org.eclipse.rdf4j.query.algebra.evaluation.function.geosparql;

import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;

/**
 * This class is responsible for creating the {@link org.locationtech.spatial4j.context.SpatialContext},
 * {@link SpatialAlgebra} and {@link WktWriter} that will be used. It will first try to load a subclass of itself called
 * "org.eclipse.rdf4j.query.algebra.evaluation.function.geosparql.SpatialSupportInitializer" . This is not provided, and
 * is primarily intended as a way to inject custom geospatial support. If this fails then the following fall-backs are
 * used:
 * <ul>
 * <li>it uses the JTS GEO SpatialContext implementation, with added support for polygons.</li>
 * {@link org.locationtech.spatial4j.context.SpatialContextFactory} . The prefix is stripped from the system property
 * name to form the SpatialContextFactory argument name.</li>
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
			support = (SpatialSupport) cls.newInstance();
		} catch (Exception e) {
			support = new JtsSpatialSupport();
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

	private static final class JtsSpatialSupport extends SpatialSupport {

		@Override
		protected JtsSpatialContext createSpatialContext() {
			return JtsSpatialContext.GEO;
		}

		@Override
		protected JtsSpatialAlgebra createSpatialAlgebra() {
			return new JtsSpatialAlgebra((JtsSpatialContext) spatialContext);
		}

		@Override
		protected WktWriter createWktWriter() {
			return new DefaultWktWriter(spatialContext);
		}
	}
}
