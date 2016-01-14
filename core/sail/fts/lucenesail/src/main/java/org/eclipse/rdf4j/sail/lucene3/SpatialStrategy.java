/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene3;

import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.spatial.tier.projections.FixedSinusoidalProjector;
import org.apache.lucene.spatial.tier.projections.IProjector;

import com.spatial4j.core.context.SpatialContext;

public class SpatialStrategy {

	public static final int DEFAULT_MIN_TIER = 2;

	public static final int DEFAULT_MAX_TIER = 15;

	private static final CartesianTierPlotter utils = new CartesianTierPlotter(0, null, null);

	private final SpatialContext context;

	private final IProjector projector = new FixedSinusoidalProjector();

	private final String fieldPrefix;

	private final int minTier;

	private final int maxTier;

	private final CartesianTierPlotter[] plotters;

	public static int getTier(double miles) {
		return utils.bestFit(miles);
	}

	public SpatialStrategy(String field) {
		this(field, DEFAULT_MIN_TIER, DEFAULT_MAX_TIER, SpatialContext.GEO);
	}

	public SpatialStrategy(String field, int minTier, int maxTier, SpatialContext context) {
		this.context = context;
		this.fieldPrefix = CartesianTierPlotter.DEFALT_FIELD_PREFIX + field + "_";
		this.minTier = minTier;
		this.maxTier = maxTier;
		plotters = new CartesianTierPlotter[maxTier - minTier + 1];
		for (int tier = minTier; tier <= maxTier; tier++) {
			plotters[tier - minTier] = new CartesianTierPlotter(tier, projector, fieldPrefix);
		}
	}

	public SpatialContext getSpatialContext() {
		return context;
	}

	public String getFieldPrefix() {
		return fieldPrefix;
	}

	public int getMinTier() {
		return minTier;
	}

	public int getMaxTier() {
		return maxTier;
	}

	public CartesianTierPlotter[] getPlotters() {
		return plotters;
	}
}
