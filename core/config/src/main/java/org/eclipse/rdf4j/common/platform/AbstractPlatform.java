/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.platform;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility base class for Platform implementations.
 */
public abstract class AbstractPlatform implements Platform {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	protected String whitespaceReplacement = "-";

	protected String separatorReplacement = "-";

	protected String colonReplacement = "";

	public File getUserHome() {
		File result = null;

		String userHome = System.getProperty("user.home");
		result = new File(userHome);

		return result;
	}

	public final File getApplicationDataDir() {
		File result;
		String sysProp;

		if ((sysProp = System.getProperty(APPDATA_BASEDIR_PROPERTY)) != null) {
			result = new File(sysProp);
		}
		else if ((sysProp = System.getProperty(OLD_DATADIR_PROPERTY)) != null) {
			logger.info(
					"Old Aduna datadir property \"{}\" detected. This property has been replaced with \"{}\". "
							+ "Support for the old property may be removed in a future version of this application.",
					OLD_DATADIR_PROPERTY, APPDATA_BASEDIR_PROPERTY);

			result = new File(sysProp);
		}
		else {
			result = getOSApplicationDataDir();
		}

		return result;
	}

	public final File getApplicationDataDir(String applicationName) {
		return new File(getApplicationDataDir(), getRelativeApplicationDataDir(applicationName));
	}

	public final File getOSApplicationDataDir(String applicationName) {
		return new File(getOSApplicationDataDir(), getRelativeApplicationDataDir(applicationName));
	}

	public String getRelativeApplicationDataDir(String applicationName) {
		return getRelativeApplicationDataDir(applicationName, dataDirPreserveCase(),
				dataDirReplaceWhitespace(), dataDirReplaceColon());
	}

	public String getRelativeApplicationDataDir(String applicationName, boolean caseSensitive,
			boolean replaceWhitespace, boolean replaceColon)
	{
		String result = applicationName.replace(File.separator, separatorReplacement);

		if (!caseSensitive) {
			result = result.toLowerCase();
		}
		if (replaceWhitespace) {
			result = result.replaceAll("\\s", whitespaceReplacement);
		}
		if (replaceColon) {
			result = result.replace(":", colonReplacement);
		}

		return result;
	}
}
