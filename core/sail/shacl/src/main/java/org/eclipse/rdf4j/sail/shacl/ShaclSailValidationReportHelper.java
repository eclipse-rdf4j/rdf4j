/*
 * *****************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * *****************************************************************************
 */

package org.eclipse.rdf4j.sail.shacl;

import java.lang.invoke.MethodHandles;

import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class ShaclSailValidationReportHelper {
	private static final WriterConfig WRITER_CONFIG = new WriterConfig();

	static {
		WRITER_CONFIG
				.set(BasicWriterSettings.PRETTY_PRINT, true)
				.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
	}

	public static void logValidationReportFromThrowableCause(Throwable t) {
		Throwable thowable;
		for (thowable = t; thowable != null; thowable = thowable.getCause()) {
			if (thowable instanceof ValidationException) {
				break;
			}
		}
		if (thowable != null && thowable instanceof ValidationException) {
			Model model = ((ValidationException) thowable).validationReportAsModel();
			Rio.write(model, System.err, RDFFormat.TURTLE, WRITER_CONFIG);
		}
	}
}
