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
package org.eclipse.rdf4j.rio.helpers;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;
import java.util.Optional;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.eclipse.rdf4j.rio.Rio;

/**
 * An implementation of FileTypeDetector which uses the {@link RDFParserRegistry} to find supported file types and their
 * extensions.
 *
 * @author Peter Ansell
 */
public class RioFileTypeDetector extends FileTypeDetector {

	public RioFileTypeDetector() {
		super();
	}

	@Override
	public String probeContentType(Path path) throws IOException {
		Optional<RDFFormat> result = Rio.getParserFormatForFileName(path.getFileName().toString());

		if (result.isPresent()) {
			return result.get().getDefaultMIMEType();
		}

		// Specification says to return null if we could not
		// conclusively determine the file type
		return null;
	}

}
