/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.common.exception.RDF4JException;

/**
 * A RuntimeException indicating that a specific Rio parser/writer configuration setting is not supported. A typical
 * cause of this exception is that a system property is used to specify a default setting, for a setting that does not
 * support this way of default specification.
 *
 * @author Jeen Broekstra
 */
public class RioConfigurationException extends RDF4JException {

	private static final long serialVersionUID = -1644521868096562781L;

	public RioConfigurationException(String message) {
		super(message);
	}

	public RioConfigurationException(Throwable t) {
		super(t);
	}

	public RioConfigurationException(String message, Throwable t) {
		super(message, t);
	}
}
