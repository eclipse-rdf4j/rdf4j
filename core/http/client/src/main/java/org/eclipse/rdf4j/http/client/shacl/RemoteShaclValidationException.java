/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.http.client.shacl;

import java.io.StringReader;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.rio.RDFFormat;

/**
 * Experimental support for handling SHACL violations against a remote RDF4J server.
 */
@Experimental
public class RemoteShaclValidationException extends RDF4JException implements ValidationException {

	private static final long serialVersionUID = 1546454692754781492L;

	private final RemoteValidation remoteValidation;

	public RemoteShaclValidationException(StringReader stringReader, String s, RDFFormat format) {
		remoteValidation = new RemoteValidation(stringReader, s, format);
	}

	/**
	 * @return A Model containing the validation report as specified by the SHACL Recommendation
	 */
	@Override
	public Model validationReportAsModel() {
		Model model = remoteValidation.asModel();
		model.setNamespace(SHACL.PREFIX, SHACL.NAMESPACE);
		return model;

	}

}
