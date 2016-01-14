/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.apache.lucene.spatial.tier.projections;

import org.apache.lucene.spatial.DistanceUtils;

public class FixedSinusoidalProjector implements IProjector {

	@Override
	public String coordsAsString(double latitude, double longitude) {
		double[] coords = coords(latitude, longitude);
		return coords[0] + "," + coords[1];
	}

	@Override
	public double[] coords(double latitude, double longitude) {
		double rlat = latitude * DistanceUtils.DEGREES_TO_RADIANS;
		double rlong = longitude * DistanceUtils.DEGREES_TO_RADIANS;
		double x = rlong * Math.cos(rlat);
		return new double[] { x, rlat };

	}

}
