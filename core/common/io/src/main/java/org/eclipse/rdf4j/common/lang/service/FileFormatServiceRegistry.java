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
package org.eclipse.rdf4j.common.lang.service;

import java.util.Optional;

import org.eclipse.rdf4j.common.lang.FileFormat;

/**
 * A special {@link ServiceRegistry} for {@link FileFormat} related services. This FileFormat-specific subclass offers
 * some utility methods for matching MIME types and file extensions to the file formats of registered services.
 *
 * @author Arjohn Kampman
 */
public abstract class FileFormatServiceRegistry<FF extends FileFormat, S> extends ServiceRegistry<FF, S> {

	protected FileFormatServiceRegistry(Class<S> serviceClass) {
		super(serviceClass);
	}

	/**
	 * Tries to match a MIME type against the list of registered file formats.
	 *
	 * @param mimeType A MIME type, e.g. "text/plain".
	 * @return The matching {@link FileFormat}, or {@link Optional#empty()} if no match was found.
	 */
	public Optional<FF> getFileFormatForMIMEType(String mimeType) {
		return FileFormat.matchMIMEType(mimeType, this.getKeys());
	}

	/**
	 * Tries to match the extension of a file name against the list of registred file formats.
	 *
	 * @param fileName A file name.
	 * @return The matching {@link FileFormat}, or {@link Optional#empty()} if no match was found.
	 */
	public Optional<FF> getFileFormatForFileName(String fileName) {
		return FileFormat.matchFileName(fileName, this.getKeys());
	}
}
