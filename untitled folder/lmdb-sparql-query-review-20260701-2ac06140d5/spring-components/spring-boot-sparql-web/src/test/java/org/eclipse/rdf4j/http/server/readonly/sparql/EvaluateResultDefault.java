/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.readonly.sparql;

import java.io.OutputStream;

public class EvaluateResultDefault implements EvaluateResult {
	private String contentType;

	private OutputStream outputstream;

	public EvaluateResultDefault(OutputStream outputstream) {
		this.outputstream = outputstream;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@Override
	public OutputStream getOutputstream() {
		return outputstream;
	}

	public void setOutputstream(OutputStream outputstream) {
		this.outputstream = outputstream;
	}
}
