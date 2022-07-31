/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.text.csv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.resultio.text.SPARQLResultsXSVMappingStrategy;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

/**
 * Implements a {@link com.opencsv.bean.MappingStrategy} to allow opencsv to work in parallel. This is where the input
 * is converted into {@link org.eclipse.rdf4j.query.BindingSet}s.
 *
 * @author Andrew Rucker Jones
 */
public class SPARQLResultsCSVMappingStrategy extends SPARQLResultsXSVMappingStrategy {

	public SPARQLResultsCSVMappingStrategy(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public void captureHeader(CSVReader reader) throws IOException {
		try {
			bindingNames = Arrays.asList(reader.readNext());
		} catch (CsvValidationException ex) {
			throw new IOException(ex);
		}
	}

	@Override
	public BindingSet populateNewBean(String[] line) {
		List<Value> values = new ArrayList<>(line.length);
		for (String valueString : line) {
			Value v = null;
			if (valueString.startsWith("_:")) {
				v = valueFactory.createBNode(valueString.substring(2));
			} else if (!"".equals(valueString)) {
				if (numberPattern.matcher(valueString).matches()) {
					v = parseNumberPatternMatch(valueString);
				} else {
					try {
						v = valueFactory.createIRI(valueString);
					} catch (IllegalArgumentException e) {
						v = valueFactory.createLiteral(valueString);
					}
				}
			}
			values.add(v);
		}
		return new ListBindingSet(bindingNames, values.toArray(new Value[values.size()]));
	}
}
