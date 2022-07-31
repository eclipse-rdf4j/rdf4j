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
package org.eclipse.rdf4j.http.server.protocol;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.server.ServerInterceptor;

/**
 * Interceptor for protocol requests. Should not be a singleton bean! Configure as inner bean in openrdf-servlet.xml
 *
 * @author Herko ter Horst
 */
public class ProtocolInterceptor extends ServerInterceptor {

	@Override
	protected String getThreadName() {
		return Protocol.PROTOCOL;
	}
}
