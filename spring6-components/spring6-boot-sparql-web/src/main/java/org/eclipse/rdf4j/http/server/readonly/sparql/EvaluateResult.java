/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.readonly.sparql;

import java.io.IOException;
import java.io.OutputStream;

/**
 * In/Out Parameter for {@link SparqlQueryEvaluator} to make it independend from things like the serlvet api.
 */
public interface EvaluateResult {
	void setContentType(String contentType);

	String getContentType();

	OutputStream getOutputstream() throws IOException;
}
