/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.dao.support.bindingsBuilder;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
@Experimental
public interface MutableBindings {
	BindingsBuilder add(Variable key, Value value);

	BindingsBuilder add(String key, Value value);

	BindingsBuilder add(Variable key, IRI value);

	BindingsBuilder add(String key, IRI value);

	BindingsBuilder add(Variable key, String value);

	BindingsBuilder add(String key, String value);

	BindingsBuilder add(Variable key, Integer value);

	BindingsBuilder add(String key, Integer value);

	BindingsBuilder add(Variable key, Boolean value);

	BindingsBuilder add(String key, Boolean value);

	BindingsBuilder addMaybe(Variable key, Boolean value);

	BindingsBuilder addMaybe(String key, Boolean value);

	BindingsBuilder add(Variable key, Float value);

	BindingsBuilder add(String key, Float value);

	BindingsBuilder add(Variable key, Double value);

	BindingsBuilder add(String key, Double value);

	BindingsBuilder addMaybe(Variable key, Value value);

	BindingsBuilder addMaybe(String key, Value value);

	BindingsBuilder addMaybe(Variable key, IRI value);

	BindingsBuilder addMaybe(String key, IRI value);

	BindingsBuilder addMaybe(Variable key, String value);

	BindingsBuilder addMaybe(String key, String value);

	BindingsBuilder addMaybe(Variable key, Integer value);

	BindingsBuilder addMaybe(String key, Integer value);

	BindingsBuilder addMaybe(Variable key, Float value);

	BindingsBuilder addMaybe(String key, Float value);

	BindingsBuilder addMaybe(Variable key, Double value);

	BindingsBuilder addMaybe(String key, Double value);
}
