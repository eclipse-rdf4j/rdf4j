/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.rio.helpers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.rio.RioSetting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A {@link RioSetting} with a {@link Set} value. The given default for the setting can be overridden by means of a
 * System property with a name equal to the setting key, and a string value of a JSON array of the desired values.
 *
 * @param <T> the type of the elements in the set
 */
public final class SetRioSetting<T> extends AbstractRioSetting<Set<T>> {

	private static final long serialVersionUID = 142127221198985291L;

	public SetRioSetting(String key, String description, Set<T> defaultValue) {
		super(key, description, defaultValue);
	}

	@Override
	public Set<T> convert(String stringRepresentation) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return new HashSet<>(objectMapper.readValue(stringRepresentation, new TypeReference<List<T>>() {
			}));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

}
