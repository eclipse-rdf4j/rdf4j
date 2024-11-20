/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.model.util;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility functions for working with RDF4J Models representing configuration settings.
 *
 * @author Jeen Broekstra
 * @implNote intended for internal use only.
 */
@InternalUseOnly
public class Configurations {

	private static final Logger logger = LoggerFactory.getLogger(Configurations.class);

	/**
	 * Verifies if use of legacy configuration vocabulary is preferred. Defaults to <code>false</code>. Can be set by
	 * having a system property <code>org.eclipse.rdf4j.model.vocabulary.useLegacyConfig</code> set to
	 * <code>true</code>.
	 *
	 * @return <code>true</code> if <code>org.eclipse.rdf4j.model.vocabulary.useLegacyConfig</code> system property is
	 *         set to <code>true</code>, <code>false</code> otherwise.
	 * @since 5.0.0
	 */
	public static boolean useLegacyConfig() {
		return "true".equalsIgnoreCase(System.getProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig"));
	}

	/**
	 * Verifies if the supplied configuration model uses any legacy vocabulary by checking the IRIs of its properties
	 *
	 * @param configModel a configuration model
	 * @return <code>true</code> if any property IRIs start with <code>http://www.openrdf.org/config</code>,
	 *         <code>false</code> otherwise.
	 */
	public static boolean hasLegacyConfiguration(Model configModel) {
		return configModel.predicates()
				.stream()
				.anyMatch(p -> p.stringValue().startsWith("http://www.openrdf.org/config"));
	}

	/**
	 * Retrieve a property value for the supplied subject as a {@link Resource} if present, falling back to a supplied
	 * legacy property .
	 * <p>
	 * This method allows querying repository config models with a mix of old and new namespaces.
	 *
	 * @param model          the model to retrieve property values from.
	 * @param subject        the subject of the property.
	 * @param property       the property to retrieve the value of.
	 * @param legacyProperty legacy property to use if the supplied property has no value in the model.
	 * @return the resource value for supplied subject and property (or the legacy property ), if present.
	 */
	@InternalUseOnly
	public static Optional<Resource> getResourceValue(Model model, Resource subject, IRI property, IRI legacyProperty) {
		var preferredProperty = useLegacyConfig() ? legacyProperty : property;
		var fallbackProperty = useLegacyConfig() ? property : legacyProperty;

		var preferredResult = Models.objectResource(model.getStatements(subject, preferredProperty, null));
		var fallbackResult = Models.objectResource(model.getStatements(subject, fallbackProperty, null));

		logDiscrepancyWarning(preferredResult, fallbackResult);

		if (preferredResult.isPresent()) {
			return preferredResult;
		}
		return fallbackResult;
	}

	/**
	 * Retrieve a property value for the supplied subject as a {@link Literal} if present, falling back to a supplied
	 * legacy property .
	 * <p>
	 * This method allows querying repository config models with a mix of old and new namespaces.
	 *
	 * @param model          the model to retrieve property values from.
	 * @param subject        the subject of the property.
	 * @param property       the property to retrieve the value of.
	 * @param legacyProperty legacy property to use if the supplied property has no value in the model.
	 * @return the literal value for supplied subject and property (or the legacy property ), if present.
	 */
	@InternalUseOnly
	public static Optional<Literal> getLiteralValue(Model model, Resource subject, IRI property, IRI legacyProperty) {
		var preferredProperty = useLegacyConfig() ? legacyProperty : property;
		var fallbackProperty = useLegacyConfig() ? property : legacyProperty;

		var preferredResult = Models.objectLiteral(model.getStatements(subject, preferredProperty, null));
		var fallbackResult = Models.objectLiteral(model.getStatements(subject, fallbackProperty, null));

		logDiscrepancyWarning(preferredResult, fallbackResult);
		if (preferredResult.isPresent()) {
			return preferredResult;
		}
		return fallbackResult;
	}

	/**
	 * Retrieve a property value for the supplied subject as a {@link Value} if present, falling back to a supplied
	 * legacy property .
	 * <p>
	 * This method allows querying repository config models with a mix of old and new namespaces.
	 *
	 * @param model          the model to retrieve property values from.
	 * @param subject        the subject of the property.
	 * @param property       the property to retrieve the value of.
	 * @param legacyProperty legacy property to use if the supplied property has no value in the model.
	 * @return the literal value for supplied subject and property (or the legacy property ), if present.
	 */
	@InternalUseOnly
	public static Optional<Value> getValue(Model model, Resource subject, IRI property, IRI legacyProperty) {
		var preferredProperty = useLegacyConfig() ? legacyProperty : property;
		var fallbackProperty = useLegacyConfig() ? property : legacyProperty;

		var preferredResult = Models.object(model.getStatements(subject, preferredProperty, null));
		var fallbackResult = Models.object(model.getStatements(subject, fallbackProperty, null));

		logDiscrepancyWarning(preferredResult, fallbackResult);
		if (preferredResult.isPresent()) {
			return preferredResult;
		}
		return fallbackResult;
	}

	/**
	 * Retrieve all property values for the supplied subject as a Set of values and include all values for any legacy
	 * property.
	 * <p>
	 * This method allows querying repository config models with a mix of old and new namespaces.
	 *
	 * @param model          the model to retrieve property values from.
	 * @param subject        the subject of the property.
	 * @param property       the property to retrieve the values of.
	 * @param legacyProperty legacy property to retrieve values of.
	 * @return the set of values for supplied subject and property (and/or legacy property).
	 */
	@InternalUseOnly
	public static Set<Value> getPropertyValues(Model model, Resource subject, IRI property, IRI legacyProperty) {
		var preferredProperty = useLegacyConfig() ? legacyProperty : property;
		var fallbackProperty = useLegacyConfig() ? property : legacyProperty;

		var preferredObjects = model.filter(subject, preferredProperty, null).objects();
		var fallbackObjects = model.filter(subject, fallbackProperty, null).objects();

		if (!fallbackObjects.isEmpty() && !preferredObjects.equals(fallbackObjects)) {
			var msg = "Discrepancy between use of the old and new config vocabulary.";
			// depending on whether preferred is set, we log on warn or debug
			if (preferredObjects.isEmpty()) {
				logger.debug(msg);
			} else {
				logger.warn(msg);
			}

			if (preferredObjects.containsAll(fallbackObjects)) {
				return preferredObjects;
			} else if (fallbackObjects.containsAll(preferredObjects)) {
				return fallbackObjects;
			}

			Set<Value> results = new HashSet<>(preferredObjects);
			results.addAll(fallbackObjects);
			return results;
		}

		return preferredObjects;
	}

	/**
	 * Retrieve a property value for the supplied subject as a {@link IRI} if present, falling back to a supplied legacy
	 * property .
	 * <p>
	 * This method allows querying repository config models with a mix of old and new namespaces.
	 *
	 * @param model          the model to retrieve property values from.
	 * @param subject        the subject of the property.
	 * @param property       the property to retrieve the value of.
	 * @param legacyProperty legacy property to use if the supplied property has no value in the model.
	 * @return the IRI value for supplied subject and property (or the legacy property ), if present.
	 */
	@InternalUseOnly
	public static Optional<IRI> getIRIValue(Model model, Resource subject, IRI property, IRI legacyProperty) {
		var preferredProperty = useLegacyConfig() ? legacyProperty : property;
		var fallbackProperty = useLegacyConfig() ? property : legacyProperty;

		var preferredResult = Models.objectIRI(model.getStatements(subject, preferredProperty, null));
		var fallbackResult = Models.objectIRI(model.getStatements(subject, fallbackProperty, null));

		logDiscrepancyWarning(preferredResult, fallbackResult);
		if (preferredResult.isPresent()) {
			return preferredResult;
		}
		return fallbackResult;
	}

	/**
	 * Retrieve the subject of the supplied type, falling back to a supplied legacy type.
	 *
	 * @param model      the model to retrieve property values from.
	 * @param type       the type to retrieve the value of.
	 * @param legacyType legacy type to use if the supplied type has no value in the model.
	 * @return The subject of the supplied type (or the legacy type), if present.
	 */
	@InternalUseOnly
	public static Optional<Resource> getSubjectByType(Model model, IRI type, IRI legacyType) {
		var preferredType = useLegacyConfig() ? legacyType : type;
		var fallbackType = useLegacyConfig() ? type : legacyType;

		var preferredResult = Models.subject(model.getStatements(null, RDF.TYPE, preferredType));
		var fallbackResult = Models.subject(model.getStatements(null, RDF.TYPE, fallbackType));

		logDiscrepancyWarning(preferredResult, fallbackResult);
		if (preferredResult.isPresent()) {
			return preferredResult;
		}

		return fallbackResult;
	}

	private static void logDiscrepancyWarning(Optional<? extends Value> preferred,
			Optional<? extends Value> fallback) {
		if (fallback.isPresent() && !preferred.equals(fallback)) {
			var msg = "Discrepancy between use of the old and new config vocabulary.";
			// depending on whether preferred is set, we log on warn or debug
			if (preferred.isEmpty()) {
				logger.debug(msg);
			} else {
				logger.warn(msg);
			}
		}
	}
}
