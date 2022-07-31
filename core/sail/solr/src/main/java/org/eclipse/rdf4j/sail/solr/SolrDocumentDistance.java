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
package org.eclipse.rdf4j.sail.solr;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sail.lucene.DocumentDistance;
import org.eclipse.rdf4j.sail.lucene.util.GeoUnits;

public class SolrDocumentDistance extends SolrDocumentResult implements DocumentDistance {

	private final IRI units;

	public SolrDocumentDistance(SolrSearchDocument doc, IRI units) {
		super(doc);
		this.units = units;
	}

	@Override
	public double getDistance() {
		Number s = ((Number) doc.getDocument().get(SolrIndex.DISTANCE_FIELD));
		return (s != null) ? GeoUnits.fromKilometres(s.doubleValue(), units) : Double.NaN;
	}
}
