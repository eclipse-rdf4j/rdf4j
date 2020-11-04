/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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
	/** <tt>doap:ArchRepository</tt> */
	public static final IRI ARCH_REPOSITORY = create("ArchRepository");

	/** <tt>doap:BKRepository</tt> */
	public static final IRI BKREPOSITORY = create("BKRepository");

	/** <tt>doap:BazaarBranch</tt> */
	public static final IRI BAZAAR_BRANCH = create("BazaarBranch");

	/** <tt>doap:CVSRepository</tt> */
	public static final IRI CVSREPOSITORY = create("CVSRepository");

	/** <tt>doap:DarcsRepository</tt> */
	public static final IRI DARCS_REPOSITORY = create("DarcsRepository");

	/** <tt>doap:GitBranch</tt> */
	public static final IRI GIT_BRANCH = create("GitBranch");

	/** <tt>doap:GitRepository</tt> */
	public static final IRI GIT_REPOSITORY = create("GitRepository");

	/** <tt>doap:HgRepository</tt> */
	public static final IRI HG_REPOSITORY = create("HgRepository");

	/** <tt>doap:Project</tt> */
	public static final IRI PROJECT = create("Project");

	/** <tt>doap:Repository</tt> */
	public static final IRI REPOSITORY = create("Repository");

	/** <tt>doap:SVNRepository</tt> */
	public static final IRI SVNREPOSITORY = create("SVNRepository");

	/** <tt>doap:Specification</tt> */
	public static final IRI SPECIFICATION = create("Specification");

	/** <tt>doap:Version</tt> */
	public static final IRI VERSION = create("Version");

	// Properties
	/** <tt>doap:anon-root</tt> */
	public static final IRI ANON_ROOT = create("anon-root");

	/** <tt>doap:audience</tt> */
	public static final IRI AUDIENCE = create("audience");

	/** <tt>doap:blog</tt> */
	public static final IRI BLOG = create("blog");

	/** <tt>doap:browse</tt> */
	public static final IRI BROWSE = create("browse");

	/** <tt>doap:bug-database</tt> */
	public static final IRI BUG_DATABASE = create("bug-database");

	/** <tt>doap:category</tt> */
	public static final IRI CATEGORY = create("category");

	/** <tt>doap:created</tt> */
	public static final IRI CREATED = create("created");

	/** <tt>doap:description</tt> */
	public static final IRI DESCRIPTION = create("description");

	/** <tt>doap:developer</tt> */
	public static final IRI DEVELOPER = create("developer");

	/** <tt>doap:developer-forum</tt> */
	public static final IRI DEVELOPER_FORUM = create("developer-forum");

	/** <tt>doap:documenter</tt> */
	public static final IRI DOCUMENTER = create("documenter");

	/** <tt>doap:download-mirror</tt> */
	public static final IRI DOWNLOAD_MIRROR = create("download-mirror");

	/** <tt>doap:download-page</tt> */
	public static final IRI DOWNLOAD_PAGE = create("download-page");

	/** <tt>doap:file-release</tt> */
	public static final IRI FILE_RELEASE = create("file-release");

	/** <tt>doap:helper</tt> */
	public static final IRI HELPER = create("helper");

	/** <tt>doap:homepage</tt> */
	public static final IRI HOMEPAGE = create("homepage");

	/** <tt>doap:implements</tt> */
	public static final IRI IMPLEMENTS = create("implements");

	/** <tt>doap:language</tt> */
	public static final IRI LANGUAGE = create("language");

	/** <tt>doap:license</tt> */
	public static final IRI LICENSE = create("license");

	/** <tt>doap:location</tt> */
	public static final IRI LOCATION = create("location");

	/** <tt>doap:mailing-list</tt> */
	public static final IRI MAILING_LIST = create("mailing-list");

	/** <tt>doap:maintainer</tt> */
	public static final IRI MAINTAINER = create("maintainer");

	/** <tt>doap:module</tt> */
	public static final IRI MODULE = create("module");

	/** <tt>doap:name</tt> */
	public static final IRI NAME = create("name");

	/** <tt>doap:old-homepage</tt> */
	public static final IRI OLD_HOMEPAGE = create("old-homepage");

	/** <tt>doap:os</tt> */
	public static final IRI OS = create("os");

	/** <tt>doap:platform</tt> */
	public static final IRI PLATFORM = create("platform");

	/** <tt>doap:programming-language</tt> */
	public static final IRI PROGRAMMING_LANGUAGE = create("programming-language");

	/** <tt>doap:release</tt> */
	public static final IRI RELEASE = create("release");

	/** <tt>doap:repository</tt> */
	public static final IRI REPOSITORY_PROP = create("repository");

	/** <tt>doap:repositoryOf</tt> */
	public static final IRI REPOSITORY_OF = create("repositoryOf");

	/** <tt>doap:revision</tt> */
	public static final IRI REVISION = create("revision");

	/** <tt>doap:screenshots</tt> */
	public static final IRI SCREENSHOTS = create("screenshots");

	/** <tt>doap:service-endpoint</tt> */
	public static final IRI SERVICE_ENDPOINT = create("service-endpoint");

	/** <tt>doap:shortdesc</tt> */
	public static final IRI SHORTDESC = create("shortdesc");

	/** <tt>doap:support-forum</tt> */
	public static final IRI SUPPORT_FORUM = create("support-forum");

	/** <tt>doap:tester</tt> */
	public static final IRI TESTER = create("tester");

	/** <tt>doap:translator</tt> */
	public static final IRI TRANSLATOR = create("translator");

	/** <tt>doap:vendor</tt> */
	public static final IRI VENDOR = create("vendor");

	/** <tt>doap:wiki</tt> */
	public static final IRI WIKI = create("wiki");

	private static IRI create(String localName) {
		return Vocabularies.createIRI(DOAP.NAMESPACE, localName);
	}
}
