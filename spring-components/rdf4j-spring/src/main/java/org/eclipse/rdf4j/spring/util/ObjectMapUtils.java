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

package org.eclipse.rdf4j.spring.util;

import static org.eclipse.rdf4j.spring.util.TypeMappingUtils.toBoolean;
import static org.eclipse.rdf4j.spring.util.TypeMappingUtils.toBooleanMaybe;
import static org.eclipse.rdf4j.spring.util.TypeMappingUtils.toBooleanOptional;
import static org.eclipse.rdf4j.spring.util.TypeMappingUtils.toIRI;
import static org.eclipse.rdf4j.spring.util.TypeMappingUtils.toIRIMaybe;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class ObjectMapUtils {
	public static IRI getIRI(Map<String, Object> map, String key) {
		return toIRI((String) map.get(key));
	}

	public static IRI getIRIMaybe(Map<String, Object> map, String key) {
		return toIRIMaybe((String) map.get(key));
	}

	public static Boolean getBoolean(Map<String, Object> map, String key) {
		return toBoolean((String) map.get(key));
	}

	public static Boolean getBooleanMaybe(Map<String, Object> map, String key) {
		return toBooleanMaybe((Boolean) map.get(key));
	}

	public static Optional<Boolean> getBooleanOptional(Map<String, Object> map, String key) {
		return toBooleanOptional((Boolean) map.get(key));
	}

	public static String getString(Map<String, Object> map, String key) {
		String value = (String) map.get(key);
		Objects.requireNonNull(value);
		return value;
	}

	public static String getStringMaybe(Map<String, Object> map, String key) {
		return (String) map.get(key);
	}

	public static Optional<String> getStringOptional(Map<String, Object> map, String key) {
		return Optional.ofNullable(getStringMaybe(map, key));
	}
}
