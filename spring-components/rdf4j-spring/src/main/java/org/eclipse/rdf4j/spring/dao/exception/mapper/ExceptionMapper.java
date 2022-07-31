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

package org.eclipse.rdf4j.spring.dao.exception.mapper;

import org.eclipse.rdf4j.spring.dao.exception.RDF4JSpringException;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class ExceptionMapper {

	public static RDF4JSpringException mapException(String message, Exception e) {
		if (e instanceof RDF4JSpringException) {
			return (RDF4JSpringException) e;
		}
		return new RDF4JSpringException(message, e);
	}
}
