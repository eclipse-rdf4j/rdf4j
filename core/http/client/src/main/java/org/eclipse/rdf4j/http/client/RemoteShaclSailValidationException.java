/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.http.client;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.StringReader;

public class RemoteShaclSailValidationException extends Exception {

	private final RemoteValidation remoteValidation;

	public RemoteShaclSailValidationException(StringReader stringReader, String s, RDFFormat format) {
		remoteValidation = new RemoteValidation(stringReader, s, format);
	}

	/**
	 * @return A Model containing the validation report as specified by the SHACL Recommendation
	 */
	@SuppressWarnings("WeakerAccess")
	public Model validationReportAsModel() {
		Model model = remoteValidation.asModel();
		model.setNamespace(SHACL.PREFIX, SHACL.NAMESPACE);
		return model;

	}

}
