/**
 * Copyright (c) 2017 Eclipse RDF4J contributors, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;


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
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

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
		ValueFactory factory = SimpleValueFactory.getInstance();

		BASIC_CONTAINER = factory.createIRI(NAMESPACE, "BasicContainer");
		CONTAINER = factory.createIRI(NAMESPACE, "Container");
		DIRECT_CONTAINER = factory.createIRI(NAMESPACE, "DirectContainer");
		INDIRECT_CONTAINER = factory.createIRI(NAMESPACE, "IndirectContainer");
		NON_RDF_SOURCE = factory.createIRI(NAMESPACE, "NonRDFSource");
		PAGE = factory.createIRI(NAMESPACE, "Page");
		PAGE_SORT_CRITERION = factory.createIRI(NAMESPACE, "PageSortCriterion");
		RDF_SOURCE = factory.createIRI(NAMESPACE, "RDFSource");
		RESOURCE = factory.createIRI(NAMESPACE, "Resource");

		CONSTRAINED_BY = factory.createIRI(NAMESPACE, "constrainedBy");
		CONTAINS = factory.createIRI(NAMESPACE, "contains");
		HAS_MEMBER_RELATION = factory.createIRI(NAMESPACE, "hasMemberRelation");
		INBOX = factory.createIRI(NAMESPACE, "inbox");
		INSERTED_CONTENT_RELATION = factory.createIRI(NAMESPACE, "insertedContentRelation");
		IS_MEMBER_OF_RELATION = factory.createIRI(NAMESPACE, "isMemberOfRelation");
		MEMBER = factory.createIRI(NAMESPACE, "member");
		MEMBERSHIP_RESOURCE = factory.createIRI(NAMESPACE, "membershipResource");
		PAGE_SEQUENCE = factory.createIRI(NAMESPACE, "pageSequence");
		PAGE_SORT_COLLATION = factory.createIRI(NAMESPACE, "pageSortCollation");
		PAGE_SORT_CRITERIA = factory.createIRI(NAMESPACE, "pageSortCriteria");
		PAGE_SORT_ORDER = factory.createIRI(NAMESPACE, "pageSortOrder");
		PAGE_SORT_PREDICATE = factory.createIRI(NAMESPACE, "pageSortPredicate");

		ASCENDING = factory.createIRI(NAMESPACE, "Ascending");
		DESCENDING = factory.createIRI(NAMESPACE, "Descending");
		MEMBER_SUBJECT = factory.createIRI(NAMESPACE, "MemberSubject");
		PREFER_CONTAINMENT = factory.createIRI(NAMESPACE, "PreferContainment");
		PREFER_EMPTY_CONTAINER = factory.createIRI(NAMESPACE, "PreferEmptyContainer");
		PREFER_MEMBERSHIP = factory.createIRI(NAMESPACE, "PreferMembership");
		PREFER_MINIMAL_CONTAINER = factory.createIRI(NAMESPACE, "PreferMinimalContainer");
	}
}
