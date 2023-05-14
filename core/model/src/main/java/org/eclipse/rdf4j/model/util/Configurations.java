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

	private static final boolean USE_CONFIG = "true"
			.equalsIgnoreCase(System.getProperty("org.eclipse.rdf4j.model.vocabulary.experimental.enableConfig"));

	private static final Logger logger = LoggerFactory.getLogger(Configurations.class);

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
		if (!USE_CONFIG) {
			var result = Models.objectResource(model.getStatements(subject, legacyProperty, null));
			if (result.isPresent()) {
				return result;
			}
		}
		return Models.objectResource(model.getStatements(subject, property, null));
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
		if (!USE_CONFIG) {
			var result = Models.objectLiteral(model.getStatements(subject, legacyProperty, null));
			if (result.isPresent()) {
				return result;
			}
		}
		return Models.objectLiteral(model.getStatements(subject, property, null));
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

		Set<Value> objects = model.filter(subject, property, null).objects();
		Set<Value> legacyObjects = model.filter(subject, legacyProperty, null).objects();
		if (USE_CONFIG) {
			legacyObjects = Set.of();
		}

		if (!objects.equals(legacyObjects)) {
			logger.warn("Discrepancy between use of the old and config vocabulary.");
			if (objects.containsAll(legacyObjects)) {
				return objects;
			} else if (legacyObjects.containsAll(objects)) {
				return legacyObjects;
			}

			Set<Value> results = new HashSet<>(objects);
			results.addAll(legacyObjects);
			return results;
		}

		return legacyObjects;
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
		if (!USE_CONFIG) {
			var result = Models.objectIRI(model.getStatements(subject, legacyProperty, null));
			if (result.isPresent()) {
				return result;
			}
		}
		return Models.objectIRI(model.getStatements(subject, property, null));
	}
}
