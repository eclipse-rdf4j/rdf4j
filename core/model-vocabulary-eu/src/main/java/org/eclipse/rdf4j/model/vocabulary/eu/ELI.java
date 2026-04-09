/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.model.vocabulary.eu;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for the European Legislation Identifier Ontology.
 *
 * @see <a href=
 *      "https://op.europa.eu/en/web/eu-vocabularies/dataset/-/resource?uri=http://publications.europa.eu/resource/dataset/eli">European
 *      Legislation Identifier Ontology</a>
 *
 * @author Bart Hanssens
 */
public class ELI {
	/**
	 * The ELI namespace: http://data.europa.eu/eli/ontology#
	 */
	public static final String NAMESPACE = "http://data.europa.eu/eli/ontology#";

	/**
	 * Recommended prefix for the namespace: "eli"
	 */
	public static final String PREFIX = "eli";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** eli:AdministrativeArea */
	public static final IRI ADMINISTRATIVE_AREA;

	/** eli:Agent */
	public static final IRI AGENT;

	/** eli:ComplexWork */
	public static final IRI COMPLEX_WORK;

	/** eli:Expression */
	public static final IRI EXPRESSION;

	/** eli:Format */
	public static final IRI FORMAT;

	/** eli:FormatType */
	public static final IRI FORMAT_TYPE;

	/** eli:InForce */
	public static final IRI IN_FORCE;

	/** eli:Language */
	public static final IRI LANGUAGE;

	/** eli:LegalExpression */
	public static final IRI LEGAL_EXPRESSION;

	/** eli:LegalResource */
	public static final IRI LEGAL_RESOURCE;

	/** eli:LegalResourceSubdivision */
	public static final IRI LEGAL_RESOURCE_SUBDIVISION;

	/** eli:LegalValue */
	public static final IRI LEGAL_VALUE;

	/** eli:Manifestation */
	public static final IRI MANIFESTATION;

	/** eli:Organization */
	public static final IRI ORGANIZATION;

	/** eli:Person */
	public static final IRI PERSON;

	/** eli:ResourceType */
	public static final IRI RESOURCE_TYPE;

	/** eli:SubdivisionType */
	public static final IRI SUBDIVISION_TYPE;

	/** eli:Version */
	public static final IRI VERSION;

	/** eli:Work */
	public static final IRI WORK;

	/** eli:WorkSubdivision */
	public static final IRI WORK_SUBDIVISION;

	/** eli:WorkType */
	public static final IRI WORK_TYPE;

	// Properties
	/** eli:amended_by */
	public static final IRI AMENDED_BY;

	/** eli:amends */
	public static final IRI AMENDS;

	/** eli:applied_by */
	public static final IRI APPLIED_BY;

	/** eli:applies */
	public static final IRI APPLIES;

	/** eli:based_on */
	public static final IRI BASED_ON;

	/** eli:basis_for */
	public static final IRI BASIS_FOR;

	/** eli:changed_by */
	public static final IRI CHANGED_BY;

	/** eli:changes */
	public static final IRI CHANGES;

	/** eli:cited_by */
	public static final IRI CITED_BY;

	/** eli:cited_by_case_law */
	public static final IRI CITED_BY_CASE_LAW;

	/** eli:cited_by_case_law_reference */
	public static final IRI CITED_BY_CASE_LAW_REFERENCE;

	/** eli:cites */
	public static final IRI CITES;

	/** eli:commenced_by */
	public static final IRI COMMENCED_BY;

	/** eli:commences */
	public static final IRI COMMENCES;

	/** eli:consolidated_by */
	public static final IRI CONSOLIDATED_BY;

	/** eli:consolidates */
	public static final IRI CONSOLIDATES;

	/** eli:corrected_by */
	public static final IRI CORRECTED_BY;

	/** eli:corrects */
	public static final IRI CORRECTS;

	/** eli:countersigned_by */
	public static final IRI COUNTERSIGNED_BY;

	/** eli:date_applicability */
	public static final IRI DATE_APPLICABILITY;

	/** eli:date_document */
	public static final IRI DATE_DOCUMENT;

	/** eli:date_no_longer_in_force */
	public static final IRI DATE_NO_LONGER_IN_FORCE;

	/** eli:date_publication */
	public static final IRI DATE_PUBLICATION;

	/** eli:description */
	public static final IRI DESCRIPTION;

	/** eli:embodies */
	public static final IRI EMBODIES;

	/** eli:ensures_implementation_of */
	public static final IRI ENSURES_IMPLEMENTATION_OF;

	/** eli:first_date_entry_in_force */
	public static final IRI FIRST_DATE_ENTRY_IN_FORCE;

	/** eli:format */
	public static final IRI FORMAT_PROP;

	/** eli:has_annex */
	public static final IRI HAS_ANNEX;

	/** eli:has_another_publication */
	public static final IRI HAS_ANOTHER_PUBLICATION;

	/** eli:has_derivative */
	public static final IRI HAS_DERIVATIVE;

	/** eli:has_member */
	public static final IRI HAS_MEMBER;

	/** eli:has_part */
	public static final IRI HAS_PART;

	/** eli:has_translation */
	public static final IRI HAS_TRANSLATION;

	/** eli:id_local */
	public static final IRI ID_LOCAL;

	/** eli:implementation_ensured_by */
	public static final IRI IMPLEMENTATION_ENSURED_BY;

	/** eli:implemented_by */
	@Deprecated
	public static final IRI IMPLEMENTED_BY;

	/** eli:implements */
	@Deprecated
	public static final IRI IMPLEMENTS;

	/** eli:in_force */
	public static final IRI IN_FORCE_PROP;

	/** eli:is_about */
	public static final IRI IS_ABOUT;

	/** eli:is_annex_of */
	public static final IRI IS_ANNEX_OF;

	/** eli:is_another_publication_of */
	public static final IRI IS_ANOTHER_PUBLICATION_OF;

	/** eli:is_derivative_of */
	public static final IRI IS_DERIVATIVE_OF;

	/** eli:is_embodied_by */
	public static final IRI IS_EMBODIED_BY;

	/** eli:is_exemplified_by */
	public static final IRI IS_EXEMPLIFIED_BY;

	/** eli:is_member_of */
	public static final IRI IS_MEMBER_OF;

	/** eli:is_part_of */
	public static final IRI IS_PART_OF;

	/** eli:is_realized_by */
	public static final IRI IS_REALIZED_BY;

	/** eli:is_translation_of */
	public static final IRI IS_TRANSLATION_OF;

	/** eli:jurisdiction */
	public static final IRI JURISDICTION;

	/** eli:language */
	public static final IRI LANGUAGE_PROP;

	/** eli:legal_value */
	public static final IRI LEGAL_VALUE_PROP;

	/** eli:licence */
	public static final IRI LICENCE;

	/** eli:media_type */
	public static final IRI MEDIA_TYPE;

	/** eli:number */
	public static final IRI NUMBER;

	/** eli:passed_by */
	public static final IRI PASSED_BY;

	/** eli:published_in */
	public static final IRI PUBLISHED_IN;

	/** eli:published_in_format */
	public static final IRI PUBLISHED_IN_FORMAT;

	/** eli:publisher */
	public static final IRI PUBLISHER;

	/** eli:publisher_agent */
	public static final IRI PUBLISHER_AGENT;

	/** eli:publishes */
	public static final IRI PUBLISHES;

	/** eli:realizes */
	public static final IRI REALIZES;

	/** eli:related_to */
	public static final IRI RELATED_TO;

	/** eli:relevant_for */
	public static final IRI RELEVANT_FOR;

	/** eli:repealed_by */
	public static final IRI REPEALED_BY;

	/** eli:repeals */
	public static final IRI REPEALS;

	/** eli:responsibility_of */
	public static final IRI RESPONSIBILITY_OF;

	/** eli:responsibility_of_agent */
	public static final IRI RESPONSIBILITY_OF_AGENT;

	/** eli:rights */
	public static final IRI RIGHTS;

	/** eli:rightsholder */
	public static final IRI RIGHTSHOLDER;

	/** eli:rightsholder_agent */
	public static final IRI RIGHTSHOLDER_AGENT;

	/** eli:title */
	public static final IRI TITLE;

	/** eli:title_alternative */
	public static final IRI TITLE_ALTERNATIVE;

	/** eli:title_short */
	public static final IRI TITLE_SHORT;

	/** eli:transposed_by */
	public static final IRI TRANSPOSED_BY;

	/** eli:transposes */
	public static final IRI TRANSPOSES;

	/** eli:type_document */
	public static final IRI TYPE_DOCUMENT;

	/** eli:type_subdivision */
	public static final IRI TYPE_SUBDIVISION;

	/** eli:uri_schema */
	public static final IRI URI_SCHEMA;

	/** eli:version */
	public static final IRI VERSION_PROP;

	/** eli:version_date */
	public static final IRI VERSION_DATE;

	/** eli:work_type */
	public static final IRI WORK_TYPE_PROP;

	// Individuals
	/** eli:AdministrativeAreaTable */
	public static final IRI ADMINISTRATIVE_AREA_TABLE;

	/** eli:FormatTypeTable */
	public static final IRI FORMAT_TYPE_TABLE;

	/** eli:InForce-inForce */
	public static final IRI IN_FORCE_IN_FORCE;

	/** eli:InForce-notInForce */
	public static final IRI IN_FORCE_NOT_IN_FORCE;

	/** eli:InForce-partiallyInForce */
	public static final IRI IN_FORCE_PARTIALLY_IN_FORCE;

	/** eli:InForceTable */
	public static final IRI IN_FORCE_TABLE;

	/** eli:LegalValue-authoritative */
	public static final IRI LEGAL_VALUE_AUTHORITATIVE;

	/** eli:LegalValue-definitive */
	public static final IRI LEGAL_VALUE_DEFINITIVE;

	/** eli:LegalValue-official */
	public static final IRI LEGAL_VALUE_OFFICIAL;

	/** eli:LegalValue-unofficial */
	public static final IRI LEGAL_VALUE_UNOFFICIAL;

	/** eli:LegalValueTable */
	public static final IRI LEGAL_VALUE_TABLE;

	/** eli:ResourceTypeTable */
	public static final IRI RESOURCE_TYPE_TABLE;

	/** eli:SubdivisionTypeTable */
	public static final IRI SUBDIVISION_TYPE_TABLE;

	/** eli:VersionTable */
	public static final IRI VERSION_TABLE;

	/** eli:WorkTypeTable */
	public static final IRI WORK_TYPE_TABLE;

	/** eli:print_format */
	public static final IRI PRINT_FORMAT;

	static {
		ADMINISTRATIVE_AREA = Vocabularies.createIRI(NAMESPACE, "AdministrativeArea");
		AGENT = Vocabularies.createIRI(NAMESPACE, "Agent");
		COMPLEX_WORK = Vocabularies.createIRI(NAMESPACE, "ComplexWork");
		EXPRESSION = Vocabularies.createIRI(NAMESPACE, "Expression");
		FORMAT = Vocabularies.createIRI(NAMESPACE, "Format");
		FORMAT_TYPE = Vocabularies.createIRI(NAMESPACE, "FormatType");
		IN_FORCE = Vocabularies.createIRI(NAMESPACE, "InForce");
		LANGUAGE = Vocabularies.createIRI(NAMESPACE, "Language");
		LEGAL_EXPRESSION = Vocabularies.createIRI(NAMESPACE, "LegalExpression");
		LEGAL_RESOURCE = Vocabularies.createIRI(NAMESPACE, "LegalResource");
		LEGAL_RESOURCE_SUBDIVISION = Vocabularies.createIRI(NAMESPACE, "LegalResourceSubdivision");
		LEGAL_VALUE = Vocabularies.createIRI(NAMESPACE, "LegalValue");
		MANIFESTATION = Vocabularies.createIRI(NAMESPACE, "Manifestation");
		ORGANIZATION = Vocabularies.createIRI(NAMESPACE, "Organization");
		PERSON = Vocabularies.createIRI(NAMESPACE, "Person");
		RESOURCE_TYPE = Vocabularies.createIRI(NAMESPACE, "ResourceType");
		SUBDIVISION_TYPE = Vocabularies.createIRI(NAMESPACE, "SubdivisionType");
		VERSION = Vocabularies.createIRI(NAMESPACE, "Version");
		WORK = Vocabularies.createIRI(NAMESPACE, "Work");
		WORK_SUBDIVISION = Vocabularies.createIRI(NAMESPACE, "WorkSubdivision");
		WORK_TYPE = Vocabularies.createIRI(NAMESPACE, "WorkType");

		AMENDED_BY = Vocabularies.createIRI(NAMESPACE, "amended_by");
		AMENDS = Vocabularies.createIRI(NAMESPACE, "amends");
		APPLIED_BY = Vocabularies.createIRI(NAMESPACE, "applied_by");
		APPLIES = Vocabularies.createIRI(NAMESPACE, "applies");
		BASED_ON = Vocabularies.createIRI(NAMESPACE, "based_on");
		BASIS_FOR = Vocabularies.createIRI(NAMESPACE, "basis_for");
		CHANGED_BY = Vocabularies.createIRI(NAMESPACE, "changed_by");
		CHANGES = Vocabularies.createIRI(NAMESPACE, "changes");
		CITED_BY = Vocabularies.createIRI(NAMESPACE, "cited_by");
		CITED_BY_CASE_LAW = Vocabularies.createIRI(NAMESPACE, "cited_by_case_law");
		CITED_BY_CASE_LAW_REFERENCE = Vocabularies.createIRI(NAMESPACE, "cited_by_case_law_reference");
		CITES = Vocabularies.createIRI(NAMESPACE, "cites");
		COMMENCED_BY = Vocabularies.createIRI(NAMESPACE, "commenced_by");
		COMMENCES = Vocabularies.createIRI(NAMESPACE, "commences");
		CONSOLIDATED_BY = Vocabularies.createIRI(NAMESPACE, "consolidated_by");
		CONSOLIDATES = Vocabularies.createIRI(NAMESPACE, "consolidates");
		CORRECTED_BY = Vocabularies.createIRI(NAMESPACE, "corrected_by");
		CORRECTS = Vocabularies.createIRI(NAMESPACE, "corrects");
		COUNTERSIGNED_BY = Vocabularies.createIRI(NAMESPACE, "countersigned_by");
		DATE_APPLICABILITY = Vocabularies.createIRI(NAMESPACE, "date_applicability");
		DATE_DOCUMENT = Vocabularies.createIRI(NAMESPACE, "date_document");
		DATE_NO_LONGER_IN_FORCE = Vocabularies.createIRI(NAMESPACE, "date_no_longer_in_force");
		DATE_PUBLICATION = Vocabularies.createIRI(NAMESPACE, "date_publication");
		DESCRIPTION = Vocabularies.createIRI(NAMESPACE, "description");
		EMBODIES = Vocabularies.createIRI(NAMESPACE, "embodies");
		ENSURES_IMPLEMENTATION_OF = Vocabularies.createIRI(NAMESPACE, "ensures_implementation_of");
		FIRST_DATE_ENTRY_IN_FORCE = Vocabularies.createIRI(NAMESPACE, "first_date_entry_in_force");
		FORMAT_PROP = Vocabularies.createIRI(NAMESPACE, "format");
		HAS_ANNEX = Vocabularies.createIRI(NAMESPACE, "has_annex");
		HAS_ANOTHER_PUBLICATION = Vocabularies.createIRI(NAMESPACE, "has_another_publication");
		HAS_DERIVATIVE = Vocabularies.createIRI(NAMESPACE, "has_derivative");
		HAS_MEMBER = Vocabularies.createIRI(NAMESPACE, "has_member");
		HAS_PART = Vocabularies.createIRI(NAMESPACE, "has_part");
		HAS_TRANSLATION = Vocabularies.createIRI(NAMESPACE, "has_translation");
		ID_LOCAL = Vocabularies.createIRI(NAMESPACE, "id_local");
		IMPLEMENTATION_ENSURED_BY = Vocabularies.createIRI(NAMESPACE, "implementation_ensured_by");
		IMPLEMENTED_BY = Vocabularies.createIRI(NAMESPACE, "implemented_by");
		IMPLEMENTS = Vocabularies.createIRI(NAMESPACE, "implements");
		IN_FORCE_PROP = Vocabularies.createIRI(NAMESPACE, "in_force");
		IS_ABOUT = Vocabularies.createIRI(NAMESPACE, "is_about");
		IS_ANNEX_OF = Vocabularies.createIRI(NAMESPACE, "is_annex_of");
		IS_ANOTHER_PUBLICATION_OF = Vocabularies.createIRI(NAMESPACE, "is_another_publication_of");
		IS_DERIVATIVE_OF = Vocabularies.createIRI(NAMESPACE, "is_derivative_of");
		IS_EMBODIED_BY = Vocabularies.createIRI(NAMESPACE, "is_embodied_by");
		IS_EXEMPLIFIED_BY = Vocabularies.createIRI(NAMESPACE, "is_exemplified_by");
		IS_MEMBER_OF = Vocabularies.createIRI(NAMESPACE, "is_member_of");
		IS_PART_OF = Vocabularies.createIRI(NAMESPACE, "is_part_of");
		IS_REALIZED_BY = Vocabularies.createIRI(NAMESPACE, "is_realized_by");
		IS_TRANSLATION_OF = Vocabularies.createIRI(NAMESPACE, "is_translation_of");
		JURISDICTION = Vocabularies.createIRI(NAMESPACE, "jurisdiction");
		LANGUAGE_PROP = Vocabularies.createIRI(NAMESPACE, "language");
		LEGAL_VALUE_PROP = Vocabularies.createIRI(NAMESPACE, "legal_value");
		LICENCE = Vocabularies.createIRI(NAMESPACE, "licence");
		MEDIA_TYPE = Vocabularies.createIRI(NAMESPACE, "media_type");
		NUMBER = Vocabularies.createIRI(NAMESPACE, "number");
		PASSED_BY = Vocabularies.createIRI(NAMESPACE, "passed_by");
		PUBLISHED_IN = Vocabularies.createIRI(NAMESPACE, "published_in");
		PUBLISHED_IN_FORMAT = Vocabularies.createIRI(NAMESPACE, "published_in_format");
		PUBLISHER = Vocabularies.createIRI(NAMESPACE, "publisher");
		PUBLISHER_AGENT = Vocabularies.createIRI(NAMESPACE, "publisher_agent");
		PUBLISHES = Vocabularies.createIRI(NAMESPACE, "publishes");
		REALIZES = Vocabularies.createIRI(NAMESPACE, "realizes");
		RELATED_TO = Vocabularies.createIRI(NAMESPACE, "related_to");
		RELEVANT_FOR = Vocabularies.createIRI(NAMESPACE, "relevant_for");
		REPEALED_BY = Vocabularies.createIRI(NAMESPACE, "repealed_by");
		REPEALS = Vocabularies.createIRI(NAMESPACE, "repeals");
		RESPONSIBILITY_OF = Vocabularies.createIRI(NAMESPACE, "responsibility_of");
		RESPONSIBILITY_OF_AGENT = Vocabularies.createIRI(NAMESPACE, "responsibility_of_agent");
		RIGHTS = Vocabularies.createIRI(NAMESPACE, "rights");
		RIGHTSHOLDER = Vocabularies.createIRI(NAMESPACE, "rightsholder");
		RIGHTSHOLDER_AGENT = Vocabularies.createIRI(NAMESPACE, "rightsholder_agent");
		TITLE = Vocabularies.createIRI(NAMESPACE, "title");
		TITLE_ALTERNATIVE = Vocabularies.createIRI(NAMESPACE, "title_alternative");
		TITLE_SHORT = Vocabularies.createIRI(NAMESPACE, "title_short");
		TRANSPOSED_BY = Vocabularies.createIRI(NAMESPACE, "transposed_by");
		TRANSPOSES = Vocabularies.createIRI(NAMESPACE, "transposes");
		TYPE_DOCUMENT = Vocabularies.createIRI(NAMESPACE, "type_document");
		TYPE_SUBDIVISION = Vocabularies.createIRI(NAMESPACE, "type_subdivision");
		URI_SCHEMA = Vocabularies.createIRI(NAMESPACE, "uri_schema");
		VERSION_PROP = Vocabularies.createIRI(NAMESPACE, "version");
		VERSION_DATE = Vocabularies.createIRI(NAMESPACE, "version_date");
		WORK_TYPE_PROP = Vocabularies.createIRI(NAMESPACE, "work_type");

		ADMINISTRATIVE_AREA_TABLE = Vocabularies.createIRI(NAMESPACE, "AdministrativeAreaTable");
		FORMAT_TYPE_TABLE = Vocabularies.createIRI(NAMESPACE, "FormatTypeTable");
		IN_FORCE_IN_FORCE = Vocabularies.createIRI(NAMESPACE, "InForce-inForce");
		IN_FORCE_NOT_IN_FORCE = Vocabularies.createIRI(NAMESPACE, "InForce-notInForce");
		IN_FORCE_PARTIALLY_IN_FORCE = Vocabularies.createIRI(NAMESPACE, "InForce-partiallyInForce");
		IN_FORCE_TABLE = Vocabularies.createIRI(NAMESPACE, "InForceTable");
		LEGAL_VALUE_AUTHORITATIVE = Vocabularies.createIRI(NAMESPACE, "LegalValue-authoritative");
		LEGAL_VALUE_DEFINITIVE = Vocabularies.createIRI(NAMESPACE, "LegalValue-definitive");
		LEGAL_VALUE_OFFICIAL = Vocabularies.createIRI(NAMESPACE, "LegalValue-official");
		LEGAL_VALUE_UNOFFICIAL = Vocabularies.createIRI(NAMESPACE, "LegalValue-unofficial");
		LEGAL_VALUE_TABLE = Vocabularies.createIRI(NAMESPACE, "LegalValueTable");
		RESOURCE_TYPE_TABLE = Vocabularies.createIRI(NAMESPACE, "ResourceTypeTable");
		SUBDIVISION_TYPE_TABLE = Vocabularies.createIRI(NAMESPACE, "SubdivisionTypeTable");
		VERSION_TABLE = Vocabularies.createIRI(NAMESPACE, "VersionTable");
		WORK_TYPE_TABLE = Vocabularies.createIRI(NAMESPACE, "WorkTypeTable");
		PRINT_FORMAT = Vocabularies.createIRI(NAMESPACE, "print_format");
	}
}
