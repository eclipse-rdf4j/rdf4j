/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.util;

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.ExtendedVariable;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class QueryResultUtils {

	public static Optional<Value> getValueOptional(BindingSet resultRow, String varName) {
		return Optional.ofNullable(resultRow.getValue(varName));
	}

	public static Optional<Value> getValueOptional(BindingSet resultRow, ExtendedVariable var) {
		return getValueOptional(resultRow, var.getVarName());
	}

	public static Value getValueMaybe(BindingSet resultRow, String varName) {
		return getValueOptional(resultRow, varName).orElse(null);
	}

	public static Value getValueMaybe(BindingSet resultRow, ExtendedVariable var) {
		return getValueMaybe(resultRow, var.getVarName());
	}

	public static Value getValue(BindingSet resultRow, String varName) {
		return getValueOptional(resultRow, varName)
				.orElseThrow(
						() -> new IllegalStateException(
								String.format(
										"BindingSet does not contain binding for variable %s",
										varName)));
	}

	public static Value getValue(BindingSet resultRow, ExtendedVariable var) {
		return getValue(resultRow, var.getVarName());
	}

	public static IRI getIRI(BindingSet resultRow, ExtendedVariable var) {
		return TypeMappingUtils.toIRI(getValue(resultRow, var));
	}

	public static IRI getIRI(BindingSet resultRow, String varName) {
		return TypeMappingUtils.toIRI(getValue(resultRow, varName));
	}

	public static Optional<IRI> getIRIOptional(BindingSet resultRow, String varName) {
		return getValueOptional(resultRow, varName).map(TypeMappingUtils::toIRI);
	}

	public static Optional<IRI> getIRIOptional(BindingSet resultRow, ExtendedVariable var) {
		return getValueOptional(resultRow, var.getVarName()).map(TypeMappingUtils::toIRI);
	}

	public static IRI getIRIMaybe(BindingSet resultRow, String varName) {
		return getIRIOptional(resultRow, varName).orElse(null);
	}

	public static IRI getIRIMaybe(BindingSet resultRow, ExtendedVariable var) {
		return getIRIMaybe(resultRow, var.getVarName());
	}

	public static String getString(BindingSet resultRow, ExtendedVariable var) {
		return getValue(resultRow, var).stringValue();
	}

	public static String getString(BindingSet resultRow, String varName) {
		return getValue(resultRow, varName).stringValue();
	}

	public static Optional<String> getStringOptional(
			BindingSet resultRow, ExtendedVariable var) {
		return getValueOptional(resultRow, var).map(Value::stringValue);
	}

	public static Optional<String> getStringOptional(BindingSet resultRow, String varName) {
		return getValueOptional(resultRow, varName).map(Value::stringValue);
	}

	public static String getStringMaybe(BindingSet resultRow, String varName) {
		return getStringOptional(resultRow, varName).orElse(null);
	}

	public static String getStringMaybe(BindingSet resultRow, ExtendedVariable var) {
		return getStringMaybe(resultRow, var.getVarName());
	}

	public static Boolean getBoolean(BindingSet resultRow, ExtendedVariable var) {
		return TypeMappingUtils.toBoolean(getValue(resultRow, var));
	}

	public static Boolean getBoolean(BindingSet resultRow, String varName) {
		return TypeMappingUtils.toBoolean(getValue(resultRow, varName));
	}

	public static Optional<Boolean> getBooleanOptional(
			BindingSet resultRow, ExtendedVariable var) {
		return getValueOptional(resultRow, var).map(TypeMappingUtils::toBoolean);
	}

	public static Optional<Boolean> getBooleanOptional(BindingSet resultRow, String varName) {
		return getValueOptional(resultRow, varName).map(TypeMappingUtils::toBoolean);
	}

	public static Boolean getBooleanMaybe(BindingSet resultRow, String varName) {
		return getBooleanOptional(resultRow, varName).orElse(null);
	}

	public static Boolean getBooleanMaybe(BindingSet resultRow, ExtendedVariable var) {
		return getBooleanMaybe(resultRow, var.getVarName());
	}
}
