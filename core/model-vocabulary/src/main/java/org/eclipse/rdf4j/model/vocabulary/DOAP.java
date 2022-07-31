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
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for the Description of a Project.
 *
 * @see <a href="https://github.com/ewilderj/doap/wiki">Description of a Project</a>
 */
public class DOAP {
	/**
	 * The DOAP namespace: http://usefulinc.com/ns/doap#
	 */
	public static final String NAMESPACE = "http://usefulinc.com/ns/doap#";

	/**
	 * Recommended prefix for the namespace: "doap"
	 */
	public static final String PREFIX = "doap";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** <var>doap:ArchRepository</var> */
	public static final IRI ARCH_REPOSITORY = create("ArchRepository");

	/** <var>doap:BKRepository</var> */
	public static final IRI BKREPOSITORY = create("BKRepository");

	/** <var>doap:BazaarBranch</var> */
	public static final IRI BAZAAR_BRANCH = create("BazaarBranch");

	/** <var>doap:CVSRepository</var> */
	public static final IRI CVSREPOSITORY = create("CVSRepository");

	/** <var>doap:DarcsRepository</var> */
	public static final IRI DARCS_REPOSITORY = create("DarcsRepository");

	/** <var>doap:GitBranch</var> */
	public static final IRI GIT_BRANCH = create("GitBranch");

	/** <var>doap:GitRepository</var> */
	public static final IRI GIT_REPOSITORY = create("GitRepository");

	/** <var>doap:HgRepository</var> */
	public static final IRI HG_REPOSITORY = create("HgRepository");

	/** <var>doap:Project</var> */
	public static final IRI PROJECT = create("Project");

	/** <var>doap:Repository</var> */
	public static final IRI REPOSITORY = create("Repository");

	/** <var>doap:SVNRepository</var> */
	public static final IRI SVNREPOSITORY = create("SVNRepository");

	/** <var>doap:Specification</var> */
	public static final IRI SPECIFICATION = create("Specification");

	/** <var>doap:Version</var> */
	public static final IRI VERSION = create("Version");

	// Properties
	/** <var>doap:anon-root</var> */
	public static final IRI ANON_ROOT = create("anon-root");

	/** <var>doap:audience</var> */
	public static final IRI AUDIENCE = create("audience");

	/** <var>doap:blog</var> */
	public static final IRI BLOG = create("blog");

	/** <var>doap:browse</var> */
	public static final IRI BROWSE = create("browse");

	/** <var>doap:bug-database</var> */
	public static final IRI BUG_DATABASE = create("bug-database");

	/** <var>doap:category</var> */
	public static final IRI CATEGORY = create("category");

	/** <var>doap:created</var> */
	public static final IRI CREATED = create("created");

	/** <var>doap:description</var> */
	public static final IRI DESCRIPTION = create("description");

	/** <var>doap:developer</var> */
	public static final IRI DEVELOPER = create("developer");

	/** <var>doap:developer-forum</var> */
	public static final IRI DEVELOPER_FORUM = create("developer-forum");

	/** <var>doap:documenter</var> */
	public static final IRI DOCUMENTER = create("documenter");

	/** <var>doap:download-mirror</var> */
	public static final IRI DOWNLOAD_MIRROR = create("download-mirror");

	/** <var>doap:download-page</var> */
	public static final IRI DOWNLOAD_PAGE = create("download-page");

	/** <var>doap:file-release</var> */
	public static final IRI FILE_RELEASE = create("file-release");

	/** <var>doap:helper</var> */
	public static final IRI HELPER = create("helper");

	/** <var>doap:homepage</var> */
	public static final IRI HOMEPAGE = create("homepage");

	/** <var>doap:implements</var> */
	public static final IRI IMPLEMENTS = create("implements");

	/** <var>doap:language</var> */
	public static final IRI LANGUAGE = create("language");

	/** <var>doap:license</var> */
	public static final IRI LICENSE = create("license");

	/** <var>doap:location</var> */
	public static final IRI LOCATION = create("location");

	/** <var>doap:mailing-list</var> */
	public static final IRI MAILING_LIST = create("mailing-list");

	/** <var>doap:maintainer</var> */
	public static final IRI MAINTAINER = create("maintainer");

	/** <var>doap:module</var> */
	public static final IRI MODULE = create("module");

	/** <var>doap:name</var> */
	public static final IRI NAME = create("name");

	/** <var>doap:old-homepage</var> */
	public static final IRI OLD_HOMEPAGE = create("old-homepage");

	/** <var>doap:os</var> */
	public static final IRI OS = create("os");

	/** <var>doap:platform</var> */
	public static final IRI PLATFORM = create("platform");

	/** <var>doap:programming-language</var> */
	public static final IRI PROGRAMMING_LANGUAGE = create("programming-language");

	/** <var>doap:release</var> */
	public static final IRI RELEASE = create("release");

	/** <var>doap:repository</var> */
	public static final IRI REPOSITORY_PROP = create("repository");

	/** <var>doap:repositoryOf</var> */
	public static final IRI REPOSITORY_OF = create("repositoryOf");

	/** <var>doap:revision</var> */
	public static final IRI REVISION = create("revision");

	/** <var>doap:screenshots</var> */
	public static final IRI SCREENSHOTS = create("screenshots");

	/** <var>doap:service-endpoint</var> */
	public static final IRI SERVICE_ENDPOINT = create("service-endpoint");

	/** <var>doap:shortdesc</var> */
	public static final IRI SHORTDESC = create("shortdesc");

	/** <var>doap:support-forum</var> */
	public static final IRI SUPPORT_FORUM = create("support-forum");

	/** <var>doap:tester</var> */
	public static final IRI TESTER = create("tester");

	/** <var>doap:translator</var> */
	public static final IRI TRANSLATOR = create("translator");

	/** <var>doap:vendor</var> */
	public static final IRI VENDOR = create("vendor");

	/** <var>doap:wiki</var> */
	public static final IRI WIKI = create("wiki");

	private static IRI create(String localName) {
		return Vocabularies.createIRI(DOAP.NAMESPACE, localName);
	}
}
