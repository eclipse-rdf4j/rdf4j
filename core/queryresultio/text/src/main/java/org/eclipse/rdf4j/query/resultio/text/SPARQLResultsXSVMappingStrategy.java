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
package org.eclipse.rdf4j.query.resultio.text;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;

import com.opencsv.bean.MappingStrategy;

/**
 * This serves as a base class for mapping strategies for character separated inputs. Specifically, it is meant for
 * {@link org.eclipse.rdf4j.query.resultio.text.csv.SPARQLResultsCSVMappingStrategy} and
 * {@link org.eclipse.rdf4j.query.resultio.text.tsv.SPARQLResultsTSVMappingStrategy}.
 *
 * @author Andrew Rucker Jones
 */
abstract public class SPARQLResultsXSVMappingStrategy implements MappingStrategy<BindingSet> {

	protected List<String> bindingNames = null;
	protected final ValueFactory valueFactory;
	protected static final Pattern numberPattern = Pattern.compile("^[-+]?[\\d.].*");
	private static final String WRITING_UNSUPPORTED = "This mapping strategy does not write.";

	public SPARQLResultsXSVMappingStrategy(ValueFactory valueFactory) {
		this.valueFactory = valueFactory;
	}

	public List<String> getBindingNames() {
		return bindingNames;
	}

	@Override
	public String[] generateHeader(BindingSet bean) {
		throw new UnsupportedOperationException(WRITING_UNSUPPORTED);
	}

	@Override
	public boolean isAnnotationDriven() {
		// This is a bald-faced lie, but it determines whether populateNewBean()
		// or populateNewBeanWithIntrospection() is used.
		return true;
	}

	/**
	 * This method parses a number as matched by {@link #numberPattern} into a {@link Value}.
	 *
	 * @param valueString The string to be parsed into a number
	 * @return The parsed value
	 */
	protected Value parseNumberPatternMatch(String valueString) {
		IRI dataType = null;

		if (XMLDatatypeUtil.isValidInteger(valueString)) {
			if (XMLDatatypeUtil.isValidNegativeInteger(valueString)) {
				dataType = XSD.NEGATIVE_INTEGER;
			} else {
				dataType = XSD.INTEGER;
			}
		} else if (XMLDatatypeUtil.isValidDecimal(valueString)) {
			dataType = XSD.DECIMAL;
		} else if (XMLDatatypeUtil.isValidDouble(valueString)) {
			dataType = XSD.DOUBLE;
		}

		return dataType != null ? valueFactory.createLiteral(valueString, dataType)
				: valueFactory.createLiteral(valueString);
	}

	@Override
	public void setErrorLocale(Locale errorLocale) {
	}

	@Override
	public void setType(Class<? extends BindingSet> type) {
	}

	@Override
	public String[] transmuteBean(BindingSet bean) {
		throw new UnsupportedOperationException(WRITING_UNSUPPORTED);
	}
}
