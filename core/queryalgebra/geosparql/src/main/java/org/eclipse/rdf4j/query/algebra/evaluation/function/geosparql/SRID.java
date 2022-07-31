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

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

/**
 * The GeoSPARQL {@link Function} geof:getSRID, as defined in
 * <a href="http://www.opengeospatial.org/standards/geosparql">OGC GeoSPARQL - A Geographic Query Language for RDF
 * Data</a>.
 */
public class SRID implements Function {

	@Override
	public String getURI() {
		return GEOF.GET_SRID.stringValue();
	}

	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 1) {
			throw new ValueExprEvaluationException(getURI() + " requires exactly 1 argument, got " + args.length);
		}

		Literal geom = FunctionArguments.getLiteral(this, args[0], GEO.WKT_LITERAL);
		String wkt = geom.getLabel();
		String srid;
		int sep = wkt.indexOf(' ');
		if (sep != -1 && wkt.charAt(0) == '<' && wkt.charAt(sep - 1) == '>') {
			srid = wkt.substring(1, sep - 1);
		} else {
			srid = GEO.DEFAULT_SRID;
		}

		return valueFactory.createLiteral(srid, XSD.ANYURI);
	}
}
