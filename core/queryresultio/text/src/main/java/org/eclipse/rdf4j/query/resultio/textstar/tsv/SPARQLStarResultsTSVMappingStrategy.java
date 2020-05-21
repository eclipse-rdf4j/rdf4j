/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.textstar.tsv;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.resultio.text.tsv.SPARQLResultsTSVMappingStrategy;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

/**
 * Extends {@link SPARQLResultsTSVMappingStrategy} with support for parsing a {@link org.eclipse.rdf4j.model.Triple}.
 *
 * @author Pavel Mihaylov
 */
public class SPARQLStarResultsTSVMappingStrategy extends SPARQLResultsTSVMappingStrategy {
	public SPARQLStarResultsTSVMappingStrategy(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	protected Value parseValue(String valueString) {
		if (valueString.startsWith("<<")) {
			return NTriplesUtil.parseTriple(valueString, valueFactory);
		} else {
			return super.parseValue(valueString);
		}
	}
}
