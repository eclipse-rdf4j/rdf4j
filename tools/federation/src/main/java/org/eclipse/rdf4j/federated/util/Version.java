/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.util;

import java.net.URI;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Version utility: read the version from the Jar's manifest file.
 *
 * @author Andreas Schwarte
 *
 */
@Deprecated(forRemoval = true)
public class Version {

	protected static final Logger log = LoggerFactory.getLogger(Version.class);

	/* fields with default values (i.e. if not started from jar) */
	protected String project = "FedX";
	protected String date = "88.88.8888";
	protected String longVersion = "8.8 (build 8888)";
	protected String build = "8888";
	protected String version = "FedX 8.8";
	protected String contact = "info@fluidops.com";
	protected String companyName = "fluid Operations AG";
	protected String productName = "fluid FedX";

	private static final Version instance = new Version();

	/**
	 * Return the version instance
	 *
	 * @return the {@link Version} instance
	 */
	public static Version getVersionInfo() {
		return instance;
	}

	private Version() {
		String jarPath = getJarPath();
		if (jarPath != null) {
			initializedVersionInfo(jarPath);
		}
	}

	private void initializedVersionInfo(String jarPath) {

		try (JarFile jar = new JarFile(jarPath)) {

			Manifest buildManifest = jar.getManifest();
			if (buildManifest != null) {
				project = buildManifest.getMainAttributes().getValue("project");
				date = buildManifest.getMainAttributes().getValue("date");
				longVersion = buildManifest.getMainAttributes().getValue("version");
				build = buildManifest.getMainAttributes().getValue("build"); // roughly svn version
				version = buildManifest.getMainAttributes().getValue("ProductVersion");
				contact = buildManifest.getMainAttributes().getValue("ProductContact");
				companyName = buildManifest.getMainAttributes().getValue("CompanyName");
				productName = buildManifest.getMainAttributes().getValue("ProductName");
			}
		} catch (Exception e) {
			log.warn("Error while reading version from jar manifest: " + e.getMessage());
			log.debug("Details:", e);
		}
	}

	protected String getJarPath() {

		URL url = Version.class.getResource("/org/eclipse/rdf4j/federated/util/Version.class");
		String urlPath = url.getPath();
		// url is something like file:/[Pfad_der_JarFile]!/[Pfad_der_Klasse]

		// not a jar, e.g. when started from eclipse
		if (!urlPath.contains("!")) {
			return null;
		}

		try {
			URI uri = new URI(url.getPath().split("!")[0]);
			return uri.getPath();
		} catch (Exception e) {
			log.warn("Error while retrieving jar path", e);
			return null;
		}
	}

	/**
	 * @return the version string, i.e. 'FedX 1.0 alpha (build 1)'
	 */
	public String getVersionString() {
		return project + " " + longVersion;
	}

	/**
	 * print information to Stdout
	 */
	public void printVersionInformation() {
		System.out.println("Version Information: " + project + " " + longVersion);
	}

	public String getProject() {
		return project;
	}

	public String getDate() {
		return date;
	}

	public String getLongVersion() {
		return longVersion;
	}

	public String getBuild() {
		return build;
	}

	public String getVersion() {
		return version;
	}

	public String getContact() {
		return contact;
	}

	public String getCompanyName() {
		return companyName;
	}

	public String getProductName() {
		return productName;
	}

	/**
	 * Prints the version info.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		getVersionInfo().printVersionInformation();
	}

}
