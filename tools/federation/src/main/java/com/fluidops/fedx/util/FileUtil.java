/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.util;

import java.io.File;

import com.fluidops.fedx.Config;


/**
 * Utility function for files
 * 
 * @author Andreas Schwarte
 *
 */
public class FileUtil {

	/**
	 * Returns the File representing the location.
	 * 
	 * <p>
	 * If the specified path is absolute, it is returned as is, otherwise a location
	 * relative to {@link Config#getBaseDir()} is returned
	 * </p>
	 * 
	 * @param path
	 * @return the file corresponding to the abstract path
	 */
	public static File getFileLocation(String path) {
		
		// check if path is an absolute path that already exists
		File f = new File(path);
		
		if (f.isAbsolute())
			return f;
		
		return fileInBaseDir(path);
	}
	
	/**
	 * Returns a File relative to the configured {@link Config#getBaseDir()}.
	 * 
	 * @param relPath
	 * @return the file
	 */
	public static File fileInBaseDir(String relPath) {
		String baseDir = Config.getConfig().getBaseDir();
		if (baseDir == null) {
			baseDir = ".";
		}
		return new File(Config.getConfig().getBaseDir(), relPath);
	}

	/**
	 * Uses {@link File#mkdirs()} to create the directory (if necessary)
	 * 
	 * @param dir
	 * @throws IllegalStateException if the directories cannot be created
	 */
	public static void mkdirs(File dir) {
		if (dir.isDirectory()) {
			return;
		}
		if (!dir.mkdirs()) {
			throw new IllegalStateException("Failed to create directories at " + dir);
		}
	}

}
