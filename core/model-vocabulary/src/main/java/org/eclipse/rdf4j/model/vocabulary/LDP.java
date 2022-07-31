/**
 * Copyright (c) 2017 Eclipse RDF4J contributors, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for the Linked Data Platform.
 *
 * @see <a href="https://www.w3.org/TR/ldp/">Linked Data Platform</a>
 *
 * @author Bart Hanssens
 */
public class LDP {
	/**
	 * The LDP namespace: http://www.w3.org/ns/ldp#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/ldp#";

	/**
	 * Recommended prefix for the namespace: "ldp"
	 */
	public static final String PREFIX = "ldp";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** ldp:BasicContainer */
	public static final IRI BASIC_CONTAINER;

	/** ldp:Container */
	public static final IRI CONTAINER;

	/** ldp:DirectContainer */
	public static final IRI DIRECT_CONTAINER;

	/** ldp:IndirectContainer */
	public static final IRI INDIRECT_CONTAINER;

	/** ldp:NonRDFSource */
	public static final IRI NON_RDF_SOURCE;

	/** ldp:Page */
	public static final IRI PAGE;

	/** ldp:PageSortCriterion */
	public static final IRI PAGE_SORT_CRITERION;

	/** ldp:RDFSource */
	public static final IRI RDF_SOURCE;

	/** ldp:Resource */
	public static final IRI RESOURCE;

	// Properties
	/** ldp:constrainedBy */
	public static final IRI CONSTRAINED_BY;

	/** ldp:contains */
	public static final IRI CONTAINS;

	/** ldp:hasMemberRelation */
	public static final IRI HAS_MEMBER_RELATION;

	/** ldp:inbox */
	public static final IRI INBOX;

	/** ldp:insertedContentRelation */
	public static final IRI INSERTED_CONTENT_RELATION;

	/** ldp:isMemberOfRelation */
	public static final IRI IS_MEMBER_OF_RELATION;

	/** ldp:member */
	public static final IRI MEMBER;

	/** ldp:membershipResource */
	public static final IRI MEMBERSHIP_RESOURCE;

	/** ldp:pageSequence */
	public static final IRI PAGE_SEQUENCE;

	/** ldp:pageSortCollation */
	public static final IRI PAGE_SORT_COLLATION;

	/** ldp:pageSortCriteria */
	public static final IRI PAGE_SORT_CRITERIA;

	/** ldp:pageSortOrder */
	public static final IRI PAGE_SORT_ORDER;

	/** ldp:pageSortPredicate */
	public static final IRI PAGE_SORT_PREDICATE;

	// Individuals
	/** ldp:Ascending */
	public static final IRI ASCENDING;

	/** ldp:Descending */
	public static final IRI DESCENDING;

	/** ldp:MemberSubject */
	public static final IRI MEMBER_SUBJECT;

	/** ldp:PreferContainment */
	public static final IRI PREFER_CONTAINMENT;

	/** ldp:PreferEmptyContainer */
	@Deprecated
	public static final IRI PREFER_EMPTY_CONTAINER;

	/** ldp:PreferMembership */
	public static final IRI PREFER_MEMBERSHIP;

	/** ldp:PreferMinimalContainer */
	public static final IRI PREFER_MINIMAL_CONTAINER;

	static {

		BASIC_CONTAINER = Vocabularies.createIRI(NAMESPACE, "BasicContainer");
		CONTAINER = Vocabularies.createIRI(NAMESPACE, "Container");
		DIRECT_CONTAINER = Vocabularies.createIRI(NAMESPACE, "DirectContainer");
		INDIRECT_CONTAINER = Vocabularies.createIRI(NAMESPACE, "IndirectContainer");
		NON_RDF_SOURCE = Vocabularies.createIRI(NAMESPACE, "NonRDFSource");
		PAGE = Vocabularies.createIRI(NAMESPACE, "Page");
		PAGE_SORT_CRITERION = Vocabularies.createIRI(NAMESPACE, "PageSortCriterion");
		RDF_SOURCE = Vocabularies.createIRI(NAMESPACE, "RDFSource");
		RESOURCE = Vocabularies.createIRI(NAMESPACE, "Resource");

		CONSTRAINED_BY = Vocabularies.createIRI(NAMESPACE, "constrainedBy");
		CONTAINS = Vocabularies.createIRI(NAMESPACE, "contains");
		HAS_MEMBER_RELATION = Vocabularies.createIRI(NAMESPACE, "hasMemberRelation");
		INBOX = Vocabularies.createIRI(NAMESPACE, "inbox");
		INSERTED_CONTENT_RELATION = Vocabularies.createIRI(NAMESPACE, "insertedContentRelation");
		IS_MEMBER_OF_RELATION = Vocabularies.createIRI(NAMESPACE, "isMemberOfRelation");
		MEMBER = Vocabularies.createIRI(NAMESPACE, "member");
		MEMBERSHIP_RESOURCE = Vocabularies.createIRI(NAMESPACE, "membershipResource");
		PAGE_SEQUENCE = Vocabularies.createIRI(NAMESPACE, "pageSequence");
		PAGE_SORT_COLLATION = Vocabularies.createIRI(NAMESPACE, "pageSortCollation");
		PAGE_SORT_CRITERIA = Vocabularies.createIRI(NAMESPACE, "pageSortCriteria");
		PAGE_SORT_ORDER = Vocabularies.createIRI(NAMESPACE, "pageSortOrder");
		PAGE_SORT_PREDICATE = Vocabularies.createIRI(NAMESPACE, "pageSortPredicate");

		ASCENDING = Vocabularies.createIRI(NAMESPACE, "Ascending");
		DESCENDING = Vocabularies.createIRI(NAMESPACE, "Descending");
		MEMBER_SUBJECT = Vocabularies.createIRI(NAMESPACE, "MemberSubject");
		PREFER_CONTAINMENT = Vocabularies.createIRI(NAMESPACE, "PreferContainment");
		PREFER_EMPTY_CONTAINER = Vocabularies.createIRI(NAMESPACE, "PreferEmptyContainer");
		PREFER_MEMBERSHIP = Vocabularies.createIRI(NAMESPACE, "PreferMembership");
		PREFER_MINIMAL_CONTAINER = Vocabularies.createIRI(NAMESPACE, "PreferMinimalContainer");
	}
}
