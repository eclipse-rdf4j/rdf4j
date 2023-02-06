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
package org.eclipse.rdf4j.sail.config;

import static org.eclipse.rdf4j.model.util.Values.iri;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;

public class SailConfigUtil {

	public static SailImplConfig parseRepositoryImpl(Model m, Resource implNode) throws SailConfigException {
		try {

			Optional<Literal> typeLit = getPropertyAsLiteral(m, implNode, CONFIG.sailType,
					SailConfigSchema.NAMESPACE_OBSOLETE);

			if (typeLit.isPresent()) {
				Optional<SailFactory> factory = SailRegistry.getInstance().get(typeLit.get().getLabel());

				if (factory.isPresent()) {
					SailImplConfig implConfig = factory.get().getConfig();
					implConfig.parse(m, implNode);
					return implConfig;
				} else {
					throw new SailConfigException("Unsupported Sail type: " + typeLit.get().getLabel());
				}
			}

			return null;
		} catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}

	/**
	 * Retrieve a property value for the supplied subject as a {@link Resource} if present, falling back to a property
	 * with the same local name in the supplied fallbackNamespace.
	 *
	 * This method allows use to query sail config models with a mix of old and new namespaces.
	 *
	 * @param model             the model to retrieve property values from.
	 * @param subject           the subject of the property.
	 * @param property          the property to retrieve the value of.
	 * @param fallbackNamespace namespace to use in combination with the local name of the supplied property if the
	 *                          supplied property has no value in the model.
	 * @return the resource value for supplied subject and property (or the property with the supplied
	 *         fallbackNamespace), if present.
	 */
	@InternalUseOnly
	public static Optional<Resource> getPropertyAsResource(Model model, Resource subject, IRI property,
			String fallbackNamespace) {
		return Optional
				.ofNullable(Models.objectResource(model.getStatements(subject, property, null)))
				.orElseGet(() -> {
					IRI fallback = iri(fallbackNamespace, property.getLocalName());
					return Models.objectResource(model.getStatements(subject, fallback, null));
				});
	}

	/**
	 * Retrieve a property value for the supplied subject as a {@link Literal} if present, falling back to a property
	 * with the same local name in the supplied fallbackNamespace.
	 *
	 * This method allows use to query sail config models with a mix of old and new namespaces.
	 *
	 * @param model             the model to retrieve property values from.
	 * @param subject           the subject of the property.
	 * @param property          the property to retrieve the value of.
	 * @param fallbackNamespace namespace to use in combination with the local name of the supplied property if the
	 *                          supplied property has no value in the model.
	 * @return the literal value for supplied subject and property (or the property with the supplied
	 *         fallbackNamespace), if present.
	 */
	@InternalUseOnly
	public static Optional<Literal> getPropertyAsLiteral(Model model, Resource subject, IRI property,
			String fallbackNamespace) {
		return Optional
				.ofNullable(Models.objectLiteral(model.getStatements(subject, property, null)))
				.orElseGet(() -> {
					IRI fallback = iri(fallbackNamespace, property.getLocalName());
					return Models.objectLiteral(model.getStatements(subject, fallback, null));
				});
	}

	/**
	 * Retrieve all property values for the supplied subject as a Set of values and include all values for any property
	 * with the same local name in the supplied fallbackNamespace.
	 *
	 * This method allows use to query sail config models with a mix of old and new namespaces.
	 *
	 * @param model             the model to retrieve property values from.
	 * @param subject           the subject of the property.
	 * @param property          the property to retrieve the values of.
	 * @param fallbackNamespace namespace to use in combination with the local name of the supplied property.
	 * @return the set of values for supplied subject and property (and/or the property with the supplied
	 *         fallbackNamespace).
	 */
	@InternalUseOnly
	public static Set<Value> getPropertyValues(Model model, Resource subject, IRI property,
			String fallbackNamespace) {
		final Set<Value> results = new HashSet<>();
		results.addAll(model.filter(subject, property, null).objects());
		results.addAll(model.filter(subject, iri(fallbackNamespace, property.getLocalName()), null).objects());
		return results;
	}

	/**
	 * Retrieve a property value for the supplied subject as an {@link IRI} if present, falling back to a property with
	 * the same local name in the supplied fallbackNamespace.
	 *
	 * This method allows use to query sail config models with a mix of old and new namespaces.
	 *
	 * @param model             the model to retrieve property values from.
	 * @param subject           the subject of the property.
	 * @param property          the property to retrieve the value of.
	 * @param fallbackNamespace namespace to use in combination with the local name of the supplied property if the
	 *                          supplied property has no value in the model.
	 * @return the IRI value for supplied subject and property (or the property with the supplied fallbackNamespace), if
	 *         present.
	 */
	@InternalUseOnly
	public static Optional<IRI> getPropertyAsIRI(Model model, Resource subject, IRI property,
			String fallbackNamespace) {
		return Optional
				.ofNullable(Models.objectIRI(model.getStatements(subject, property, null)))
				.orElseGet(() -> {
					IRI fallback = iri(fallbackNamespace, property.getLocalName());
					return Models.objectIRI(model.getStatements(subject, fallback, null));
				});
	}
}
