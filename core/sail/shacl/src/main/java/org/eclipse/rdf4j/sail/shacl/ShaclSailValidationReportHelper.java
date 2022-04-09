/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Optional;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
@InternalUseOnly
public class ShaclSailValidationReportHelper {
	private static final WriterConfig WRITER_CONFIG = new WriterConfig();

	static {
		WRITER_CONFIG
				.set(BasicWriterSettings.PRETTY_PRINT, true)
				.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
	}

	/**
	 * Finds a validation report using {@link #getValidationReport(Throwable)} and returns a {@link String} containing
	 * the pretty-printed report.
	 *
	 * @param t the {@link Throwable} to start searching for a validation report at
	 * @return an Optional with the pretty-printed report if one is found, empty otherwise.
	 */
	public static Optional<String> getValidationReportAsString(Throwable t) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		printValidationReport(t, baos);
		String reportAsString = baos.toString();
		if (reportAsString == null || reportAsString.isBlank()) {
			return Optional.empty();
		}
		return Optional.of(reportAsString);
	}

	/**
	 * Finds a validation report using {@link #getValidationReport(Throwable)} and pretty-prints it to the specified
	 * output stream.
	 *
	 * @param t   the {@link Throwable} to start searching for a validation report at
	 * @param out the output stream to print to
	 */
	public static void printValidationReport(Throwable t, OutputStream out) {
		Optional<Model> reportOpt = getValidationReport(t);
		if (reportOpt.isPresent()) {
			Rio.write(reportOpt.get(), out, RDFFormat.TURTLE, WRITER_CONFIG);
		}
	}

	/**
	 * Looks for a {@link ValidationException} starting with the specified throwable and working back through the cause
	 * references, and returns the validation report as a {@link Model} if one is found.
	 *
	 * @param t the {@link Throwable} to start the search at
	 * @return an optional with the validation report, or empty.
	 */
	public static Optional<Model> getValidationReport(Throwable t) {
		Throwable throwable;
		for (throwable = t; throwable != null; throwable = throwable.getCause()) {
			if (throwable instanceof ValidationException) {
				return Optional.ofNullable(((ValidationException) throwable).validationReportAsModel());
			}
		}
		return Optional.empty();
	}

}
