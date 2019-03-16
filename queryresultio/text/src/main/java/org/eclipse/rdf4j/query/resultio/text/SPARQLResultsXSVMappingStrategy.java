/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.text;

import com.opencsv.bean.BeanField;
import com.opencsv.bean.MappingStrategy;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.BindingSet;

import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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
				dataType = XMLSchema.NEGATIVE_INTEGER;
			} else {
				dataType = XMLSchema.INTEGER;
			}
		} else if (XMLDatatypeUtil.isValidDecimal(valueString)) {
			dataType = XMLSchema.DECIMAL;
		} else if (XMLDatatypeUtil.isValidDouble(valueString)) {
			dataType = XMLSchema.DOUBLE;
		}

		return dataType != null ? valueFactory.createLiteral(valueString, dataType)
				: valueFactory.createLiteral(valueString);
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
