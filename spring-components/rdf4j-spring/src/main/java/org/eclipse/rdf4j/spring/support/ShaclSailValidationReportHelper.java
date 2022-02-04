/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.support;

import java.io.StringWriter;
import java.lang.invoke.MethodHandles;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.shacl.ShaclSailValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class ShaclSailValidationReportHelper {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static void logValidationReportFromThrowableCause(Throwable t) {
		Throwable thowable;
		for (thowable = t; thowable != null; thowable = thowable.getCause()) {
			if (thowable instanceof ShaclSailValidationException) {
				break;
			}
		}
		if (thowable != null && thowable instanceof ShaclSailValidationException) {
			Model model = ((ShaclSailValidationException) thowable).validationReportAsModel();
			StringWriter w = new StringWriter();
			Rio.write(model, System.err, RDFFormat.TURTLE);
		}
	}
}
