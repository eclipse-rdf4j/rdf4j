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

package org.eclipse.rdf4j.common.io;

import java.io.File;
import java.io.IOException;

/**
 * Utility methods for operations on Files.
 */
public class FileUtil {
	/**
	 * Deletes the specified directory and any files and directories in it recursively.
	 *
	 * @param dir The directory to remove.
	 * @throws IOException If the directory could not be removed.
	 */
	public static void deleteDir(File dir) throws IOException {
		if (!dir.isDirectory()) {
			throw new IOException("Not a directory " + dir);
		}

		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				deleteDir(file);
			} else {
				boolean deleted = file.delete();
				if (!deleted) {
					throw new IOException("Unable to delete file" + file);
				}
			}
		}

		dir.delete();
	}
}
