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

import java.io.IOException;
import java.io.StringReader;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorLogger;

@InternalUseOnly
class RemoteValidation {

	StringReader stringReader;
	String baseUri;
	RDFFormat format;

	Model model;

	RemoteValidation(StringReader stringReader, String baseUri, RDFFormat format) {
		this.stringReader = stringReader;
		this.baseUri = baseUri;
		this.format = format;
	}

	Model asModel() {
		if (model == null) {
			try {
				ParserConfig parserConfig = new ParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
				model = Rio.parse(stringReader, baseUri, format, parserConfig, SimpleValueFactory.getInstance(),
						new ParseErrorLogger());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return model;
	}

}
