/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.text.csv;

import com.opencsv.CSVReader;
import com.opencsv.bean.BeanField;
import com.opencsv.bean.MappingStrategy;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.ListBindingSet;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Implements a {@link com.opencsv.bean.MappingStrategy} to allow opencsv to work in parallel.
 * This is where the input is converted into {@link org.eclipse.rdf4j.query.BindingSet}s.
 *
 * @author Andrew Rucker Jones
 */
public class SPARQLResultsCSVMappingStrategy implements MappingStrategy<BindingSet> {

	private List<String> bindingNames = null;
	private final ValueFactory valueFactory;
	private static final Pattern numberPattern = Pattern.compile("^[-+]?[\\d.].*");
	private static final String WRITING_UNSUPPORTED = "This mapping strategy does not write.";

	public SPARQLResultsCSVMappingStrategy(ValueFactory valueFactory) {
		this.valueFactory = valueFactory;
	}

	public List<String> getBindingNames() {
		return bindingNames;
	}

	@Deprecated
	public PropertyDescriptor findDescriptor(int col) {
		return null;
	}

	@Deprecated
	public BeanField<BindingSet> findField(int col) {
		return null;
	}

	@Deprecated
	public int findMaxFieldIndex() {
		return 0;
	}

	@Deprecated
	public BindingSet createBean() {
		return null;
	}

	@Override
	public void captureHeader(CSVReader reader) throws IOException {
		// header is mandatory in SPARQL CSV
		bindingNames = Arrays.asList(reader.readNext());
	}

	public String[] generateHeader(BindingSet bean) {
		throw new UnsupportedOperationException(WRITING_UNSUPPORTED);
	}

	@Deprecated
	public Integer getColumnIndex(String name) {
		return null;
	}

	public boolean isAnnotationDriven() {
		// This is a bald-faced lie, but it determines whether populateNewBean()
		// or populateNewBeanWithIntrospection() is used.
		return true;
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

					IRI datatype = null;

					if (XMLDatatypeUtil.isValidInteger(valueString)) {
						if (XMLDatatypeUtil.isValidNegativeInteger(valueString)) {
							datatype = XMLSchema.NEGATIVE_INTEGER;
						} else {
							datatype = XMLSchema.INTEGER;
						}
					} else if (XMLDatatypeUtil.isValidDecimal(valueString)) {
						datatype = XMLSchema.DECIMAL;
					} else if (XMLDatatypeUtil.isValidDouble(valueString)) {
						datatype = XMLSchema.DOUBLE;
					}

					if (datatype != null) {
						v = valueFactory.createLiteral(valueString, datatype);
					} else {
						v = valueFactory.createLiteral(valueString);
					}
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

	@Deprecated
	public BindingSet populateNewBeanWithIntrospection(String[] line) {
		throw new UnsupportedOperationException("Please use populateNewBean() instead.");
	}

	@Deprecated
	public void verifyLineLength(int numberOfFields) {
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
