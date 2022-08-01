/**
 * Copyright (c) 2015 Eclipse RDF4J contributors, and others.
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
 * Constants for the W3C Organization Ontology.
 *
 * @see <a href="https://www.w3.org/TR/vocab-org/">Organization Ontology</a>
 *
 * @author Bart.Hanssens
 */
public class ORG {

	/**
	 * The Organization Ontology namespace: http://www.w3.org/ns/org#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/org#";

	/**
	 * Recommended prefix for the Organization Ontology namespace: "org"
	 */
	public static final String PREFIX = "org";

	/**
	 * An immutable {@link Namespace} constant that represents the Organization namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** void:ChangeEvent */
	public static final IRI CHANGE_EVENT;

	/** void:FormalOrganization */
	public static final IRI FORMAL_ORGANIZATION;

	/** void:Membership */
	public static final IRI MEMBERSHIP;

	/** void:Organization */
	public static final IRI ORGANIZATION;

	/** void:OrganizationalCollaboration */
	public static final IRI ORGANIZATIONAL_COLLABORATION;

	/** void:OrganizationalUnit */
	public static final IRI ORGANIZATIONAL_UNIT;

	/** void:Post */
	public static final IRI POST;

	/** void:Role */
	public static final IRI ROLE;

	/** void:Site */
	public static final IRI SITE;

	// Properties
	/** void:basedAt */
	public static final IRI BASED_AT;

	/** void:changedBy */
	public static final IRI CHANGED_BY;

	/** void:classification */
	public static final IRI CLASSIFICATION;

	/** void:hasMember */
	public static final IRI HAS_MEMBER;

	/** void:hasMembership */
	public static final IRI HAS_MEMBERSHIP;

	/** void:hasPost */
	public static final IRI HAS_POST;

	/** void:hasPrimarySite */
	public static final IRI HAS_PRIMARY_SITE;

	/** void:hasRegisteredSite */
	public static final IRI HAS_REGISTERED_SITE;

	/** void:hasSite */
	public static final IRI HAS_SITE;

	/** void:hasSubOrganization */
	public static final IRI HAS_SUB_ORGANIZATION;

	/** void:hasUnit */
	public static final IRI HAS_UNIT;

	/** void:headOf */
	public static final IRI HEAD_OF;

	/** void:heldBy */
	public static final IRI HELD_BY;

	/** void:holds */
	public static final IRI HOLDS;

	/** void:identifier */
	public static final IRI IDENTIFIER;

	/** void:linkedTo */
	public static final IRI LINKED_TO;

	/** void:location */
	public static final IRI LOCATION;

	/** void:memberDuring */
	public static final IRI MEMBER_DURING;

	/** void:memberOf */
	public static final IRI MEMBER_OF;

	/** void:member */
	public static final IRI MEMBER;

	/** void:organization */
	public static final IRI HAS_ORGANIZATION;

	/** void:originalOrganization */
	public static final IRI ORIGINAL_ORGANIZATION;

	/** void:postIn */
	public static final IRI POST_IN;

	/** void:purpose */
	public static final IRI PURPOSE;

	/** void:remuneration */
	public static final IRI REMUNERATION;

	/** void:reportsTo */
	public static final IRI REPORTS_TO;

	/** void:resultedFrom */
	public static final IRI RESULTED_FROM;

	/** void:resultingOrganization */
	public static final IRI RESULTING_ORGANIZATION;

	/** void:role */
	public static final IRI HAS_ROLE;

	/** void:roleProperty */
	public static final IRI ROLE_PROPERTY;

	/** void:siteAddress */
	public static final IRI SITE_ADDRESS;

	/** void:siteOf */
	public static final IRI SITE_OF;

	/** void:subOrganizationOf */
	public static final IRI SUB_ORGANIZATION_OF;

	/** void:transitiveSubOrganizationOf */
	public static final IRI TRANSITIVE_SUB_ORGANIZATION_OF;

	/** void:unitOf */
	public static final IRI UNIT_OF;

	static {

		CHANGE_EVENT = Vocabularies.createIRI(NAMESPACE, "ChangeEvent");
		FORMAL_ORGANIZATION = Vocabularies.createIRI(NAMESPACE, "FormalOrganization");
		MEMBERSHIP = Vocabularies.createIRI(NAMESPACE, "Membership");
		ORGANIZATION = Vocabularies.createIRI(NAMESPACE, "Organization");
		ORGANIZATIONAL_COLLABORATION = Vocabularies.createIRI(NAMESPACE, "OrganizationalCollaboration");
		ORGANIZATIONAL_UNIT = Vocabularies.createIRI(NAMESPACE, "OrganizationalUnit");
		POST = Vocabularies.createIRI(NAMESPACE, "Post");
		ROLE = Vocabularies.createIRI(NAMESPACE, "Role");
		SITE = Vocabularies.createIRI(NAMESPACE, "Site");

		BASED_AT = Vocabularies.createIRI(NAMESPACE, "basedAt");
		CHANGED_BY = Vocabularies.createIRI(NAMESPACE, "changedBy");
		CLASSIFICATION = Vocabularies.createIRI(NAMESPACE, "classification");
		HAS_MEMBER = Vocabularies.createIRI(NAMESPACE, "hasMember");
		HAS_MEMBERSHIP = Vocabularies.createIRI(NAMESPACE, "hasMembership");
		HAS_POST = Vocabularies.createIRI(NAMESPACE, "hasPost");
		HAS_PRIMARY_SITE = Vocabularies.createIRI(NAMESPACE, "hasPrimarySite");
		HAS_REGISTERED_SITE = Vocabularies.createIRI(NAMESPACE, "hasRegisteredSite");
		HAS_SITE = Vocabularies.createIRI(NAMESPACE, "hasSite");
		HAS_SUB_ORGANIZATION = Vocabularies.createIRI(NAMESPACE, "hasSubOrganization");
		HAS_UNIT = Vocabularies.createIRI(NAMESPACE, "hasUnit");
		HEAD_OF = Vocabularies.createIRI(NAMESPACE, "headOf");
		HELD_BY = Vocabularies.createIRI(NAMESPACE, "heldBy");
		HOLDS = Vocabularies.createIRI(NAMESPACE, "holds");
		IDENTIFIER = Vocabularies.createIRI(NAMESPACE, "identifier");
		LINKED_TO = Vocabularies.createIRI(NAMESPACE, "linkedTo");
		LOCATION = Vocabularies.createIRI(NAMESPACE, "location");
		MEMBER_DURING = Vocabularies.createIRI(NAMESPACE, "memberDuring");
		MEMBER_OF = Vocabularies.createIRI(NAMESPACE, "memberOf");
		MEMBER = Vocabularies.createIRI(NAMESPACE, "member");
		HAS_ORGANIZATION = Vocabularies.createIRI(NAMESPACE, "organization");
		ORIGINAL_ORGANIZATION = Vocabularies.createIRI(NAMESPACE, "originalOrganization");
		POST_IN = Vocabularies.createIRI(NAMESPACE, "postIn");
		PURPOSE = Vocabularies.createIRI(NAMESPACE, "purpose");
		REMUNERATION = Vocabularies.createIRI(NAMESPACE, "remuneration");
		REPORTS_TO = Vocabularies.createIRI(NAMESPACE, "reportsTo");
		RESULTED_FROM = Vocabularies.createIRI(NAMESPACE, "resultedFrom");
		RESULTING_ORGANIZATION = Vocabularies.createIRI(NAMESPACE, "resultingOrganization");
		HAS_ROLE = Vocabularies.createIRI(NAMESPACE, "role");
		ROLE_PROPERTY = Vocabularies.createIRI(NAMESPACE, "roleProperty");
		SITE_ADDRESS = Vocabularies.createIRI(NAMESPACE, "siteAddress");
		SITE_OF = Vocabularies.createIRI(NAMESPACE, "siteOf");
		SUB_ORGANIZATION_OF = Vocabularies.createIRI(NAMESPACE, "subOrganizationOf");
		TRANSITIVE_SUB_ORGANIZATION_OF = Vocabularies.createIRI(NAMESPACE, "transitiveSubOrganizationOf");
		UNIT_OF = Vocabularies.createIRI(NAMESPACE, "unitOf");
	}
}
