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
package org.eclipse.rdf4j.workbench.util;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.workbench.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decodes strings into values for {@link WorkbenchRequst}.
 */
class ValueDecoder {

	private static final Logger LOGGER = LoggerFactory.getLogger(ValueDecoder.class);

	private final ValueFactory factory;

	private final Repository repository;

	/**
	 * Creates an instance of ValueDecoder.
	 *
	 * @param repository to get namespaces from
	 * @param factory    to generate values
	 */
	protected ValueDecoder(Repository repository, ValueFactory factory) {
		this.repository = repository;
		this.factory = factory;
	}

	/**
	 * Decode the given string into a {@link org.eclipse.rdf4j.model.Value}.
	 *
	 * @param string representation of an RDF value
	 * @return the parsed value, or null if the string is null, empty, only whitespace, or
	 *         {@link java.lang.String#equals(Object)} "null".
	 * @throws BadRequestException if a problem occurs during parsing
	 */
	protected Value decodeValue(String string) throws BadRequestException {
		Value result = null;
		try {
			if (string != null) {
				String value = string.trim();
				if (!value.isEmpty() && !"null".equals(value)) {
					if (value.startsWith("_:")) {
						String label = value.substring("_:".length());
						result = factory.createBNode(label);
					} else {
						if (value.charAt(0) == '<' && value.endsWith(">")) {
							result = factory.createIRI(value.substring(1, value.length() - 1));
						} else {
							if (value.charAt(0) == '"') {
								result = parseLiteral(value);
							} else {
								result = parseURI(value);
							}
						}
					}
				}
			}
		} catch (Exception exc) {
			LOGGER.warn(exc.toString(), exc);
			throw new BadRequestException("Malformed value: " + string, exc);
		}
		return result;
	}

	private Value parseURI(String value) throws RepositoryException, BadRequestException {
		String prefix = value.substring(0, value.indexOf(':'));
		String localPart = value.substring(prefix.length() + 1);
		String namespace = getNamespace(prefix);
		if (namespace == null) {
			throw new BadRequestException("Undefined prefix: " + value);
		}
		return factory.createIRI(namespace, localPart);
	}

	private Value parseLiteral(String value) throws BadRequestException {
		String label = value.substring(1, value.lastIndexOf('"'));
		Value result;
		if (value.length() == (label.length() + 2)) {
			result = factory.createLiteral(label);
		} else {
			String rest = value.substring(label.length() + 2);
			if (rest.startsWith("^^")) {
				Value datatype = decodeValue(rest.substring(2));
				if (datatype instanceof IRI) {
					result = factory.createLiteral(label, (IRI) datatype);
				} else {
					throw new BadRequestException("Malformed datatype: " + value);
				}
			} else if (rest.charAt(0) == '@') {
				result = factory.createLiteral(label, rest.substring(1));
			} else {
				throw new BadRequestException("Malformed language tag or datatype: " + value);
			}
		}
		return result;
	}

	private String getNamespace(String prefix) throws RepositoryException {
		try (RepositoryConnection con = repository.getConnection()) {
			return con.getNamespace(prefix);
		}
	}

}
