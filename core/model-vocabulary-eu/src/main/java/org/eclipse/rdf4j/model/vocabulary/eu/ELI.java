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
import org.eclipse.rdf4j.model.base.Vocabularies;

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
	public static final IRI AdministrativeArea;

	/** eli:Agent */
	public static final IRI Agent;

	/** eli:ComplexWork */
	public static final IRI ComplexWork;

	/** eli:Expression */
	public static final IRI Expression;

	/** eli:Format */
	public static final IRI Format;

	/** eli:FormatType */
	public static final IRI FormatType;

	/** eli:InForce */
	public static final IRI InForce;

	/** eli:Language */
	public static final IRI Language;

	/** eli:LegalExpression */
	public static final IRI LegalExpression;

	/** eli:LegalResource */
	public static final IRI LegalResource;

	/** eli:LegalResourceSubdivision */
	public static final IRI LegalResourceSubdivision;

	/** eli:LegalValue */
	public static final IRI LegalValue;

	/** eli:Manifestation */
	public static final IRI Manifestation;

	/** eli:Organization */
	public static final IRI Organization;

	/** eli:Person */
	public static final IRI Person;

	/** eli:ResourceType */
	public static final IRI ResourceType;

	/** eli:SubdivisionType */
	public static final IRI SubdivisionType;

	/** eli:Version */
	public static final IRI Version;

	/** eli:Work */
	public static final IRI Work;

	/** eli:WorkSubdivision */
	public static final IRI WorkSubdivision;

	/** eli:WorkType */
	public static final IRI WorkType;

	// Properties
	/** eli:amended_by */
	public static final IRI amendedBy;

	/** eli:amends */
	public static final IRI amends;

	/** eli:applied_by */
	public static final IRI appliedBy;

	/** eli:applies */
	public static final IRI applies;

	/** eli:based_on */
	public static final IRI basedOn;

	/** eli:basis_for */
	public static final IRI basisFor;

	/** eli:changed_by */
	public static final IRI changedBy;

	/** eli:changes */
	public static final IRI changes;

	/** eli:cited_by */
	public static final IRI citedBy;

	/** eli:cited_by_case_law */
	public static final IRI citedByCaseLaw;

	/** eli:cited_by_case_law_reference */
	public static final IRI citedByCaseLawReference;

	/** eli:cites */
	public static final IRI cites;

	/** eli:commenced_by */
	public static final IRI commencedBy;

	/** eli:commences */
	public static final IRI commences;

	/** eli:consolidated_by */
	public static final IRI consolidatedBy;

	/** eli:consolidates */
	public static final IRI consolidates;

	/** eli:corrected_by */
	public static final IRI correctedBy;

	/** eli:corrects */
	public static final IRI corrects;

	/** eli:countersigned_by */
	public static final IRI countersignedBy;

	/** eli:date_applicability */
	public static final IRI dateApplicability;

	/** eli:date_document */
	public static final IRI dateDocument;

	/** eli:date_no_longer_in_force */
	public static final IRI dateNoLongerInForce;

	/** eli:date_publication */
	public static final IRI datePublication;

	/** eli:description */
	public static final IRI description;

	/** eli:embodies */
	public static final IRI embodies;

	/** eli:ensures_implementation_of */
	public static final IRI ensuresImplementationOf;

	/** eli:first_date_entry_in_force */
	public static final IRI firstDateEntryInForce;

	/** eli:format */
	public static final IRI format;

	/** eli:has_annex */
	public static final IRI hasAnnex;

	/** eli:has_another_publication */
	public static final IRI hasAnotherPublication;

	/** eli:has_derivative */
	public static final IRI hasDerivative;

	/** eli:has_member */
	public static final IRI hasMember;

	/** eli:has_part */
	public static final IRI hasPart;

	/** eli:has_translation */
	public static final IRI hasTranslation;

	/** eli:id_local */
	public static final IRI idLocal;

	/** eli:implementation_ensured_by */
	public static final IRI implementationEnsuredBy;

	/** eli:implemented_by */
	@Deprecated
	public static final IRI implementedBy;

	/** eli:implements */
	@Deprecated
	public static final IRI implements_;

	/** eli:in_force */
	public static final IRI inForce;

	/** eli:is_about */
	public static final IRI isAbout;

	/** eli:is_annex_of */
	public static final IRI isAnnexOf;

	/** eli:is_another_publication_of */
	public static final IRI isAnotherPublicationOf;

	/** eli:is_derivative_of */
	public static final IRI isDerivativeOf;

	/** eli:is_embodied_by */
	public static final IRI isEmbodiedBy;

	/** eli:is_exemplified_by */
	public static final IRI isExemplifiedBy;

	/** eli:is_member_of */
	public static final IRI isMemberOf;

	/** eli:is_part_of */
	public static final IRI isPartOf;

	/** eli:is_realized_by */
	public static final IRI isRealizedBy;

	/** eli:is_translation_of */
	public static final IRI isTranslationOf;

	/** eli:jurisdiction */
	public static final IRI jurisdiction;

	/** eli:language */
	public static final IRI language;

	/** eli:legal_value */
	public static final IRI legalValue;

	/** eli:licence */
	public static final IRI licence;

	/** eli:media_type */
	public static final IRI mediaType;

	/** eli:number */
	public static final IRI number;

	/** eli:passed_by */
	public static final IRI passedBy;

	/** eli:published_in */
	public static final IRI publishedIn;

	/** eli:published_in_format */
	public static final IRI publishedInFormat;

	/** eli:publisher */
	public static final IRI publisher;

	/** eli:publisher_agent */
	public static final IRI publisherAgent;

	/** eli:publishes */
	public static final IRI publishes;

	/** eli:realizes */
	public static final IRI realizes;

	/** eli:related_to */
	public static final IRI relatedTo;

	/** eli:relevant_for */
	public static final IRI relevantFor;

	/** eli:repealed_by */
	public static final IRI repealedBy;

	/** eli:repeals */
	public static final IRI repeals;

	/** eli:responsibility_of */
	public static final IRI responsibilityOf;

	/** eli:responsibility_of_agent */
	public static final IRI responsibilityOfAgent;

	/** eli:rights */
	public static final IRI rights;

	/** eli:rightsholder */
	public static final IRI rightsholder;

	/** eli:rightsholder_agent */
	public static final IRI rightsholderAgent;

	/** eli:title */
	public static final IRI title;

	/** eli:title_alternative */
	public static final IRI titleAlternative;

	/** eli:title_short */
	public static final IRI titleShort;

	/** eli:transposed_by */
	public static final IRI transposedBy;

	/** eli:transposes */
	public static final IRI transposes;

	/** eli:type_document */
	public static final IRI typeDocument;

	/** eli:type_subdivision */
	public static final IRI typeSubdivision;

	/** eli:uri_schema */
	public static final IRI uriSchema;

	/** eli:version */
	public static final IRI version;

	/** eli:version_date */
	public static final IRI versionDate;

	/** eli:work_type */
	public static final IRI workType;

	// Individuals
	/** eli:AdministrativeAreaTable */
	public static final IRI AdministrativeAreaTable;

	/** eli:FormatTypeTable */
	public static final IRI FormatTypeTable;

	/** eli:InForce-inForce */
	public static final IRI InForceInForce;

	/** eli:InForce-notInForce */
	public static final IRI InForceNotInForce;

	/** eli:InForce-partiallyInForce */
	public static final IRI InForcePartiallyInForce;

	/** eli:InForceTable */
	public static final IRI InForceTable;

	/** eli:LegalValue-authoritative */
	public static final IRI LegalValueAuthoritative;

	/** eli:LegalValue-definitive */
	public static final IRI LegalValueDefinitive;

	/** eli:LegalValue-official */
	public static final IRI LegalValueOfficial;

	/** eli:LegalValue-unofficial */
	public static final IRI LegalValueUnofficial;

	/** eli:LegalValueTable */
	public static final IRI LegalValueTable;

	/** eli:ResourceTypeTable */
	public static final IRI ResourceTypeTable;

	/** eli:SubdivisionTypeTable */
	public static final IRI SubdivisionTypeTable;

	/** eli:VersionTable */
	public static final IRI VersionTable;

	/** eli:WorkTypeTable */
	public static final IRI WorkTypeTable;

	/** eli:print_format */
	public static final IRI printFormat;

	static {
		AdministrativeArea = Vocabularies.createIRI(NAMESPACE, "AdministrativeArea");
		Agent = Vocabularies.createIRI(NAMESPACE, "Agent");
		ComplexWork = Vocabularies.createIRI(NAMESPACE, "ComplexWork");
		Expression = Vocabularies.createIRI(NAMESPACE, "Expression");
		Format = Vocabularies.createIRI(NAMESPACE, "Format");
		FormatType = Vocabularies.createIRI(NAMESPACE, "FormatType");
		InForce = Vocabularies.createIRI(NAMESPACE, "InForce");
		Language = Vocabularies.createIRI(NAMESPACE, "Language");
		LegalExpression = Vocabularies.createIRI(NAMESPACE, "LegalExpression");
		LegalResource = Vocabularies.createIRI(NAMESPACE, "LegalResource");
		LegalResourceSubdivision = Vocabularies.createIRI(NAMESPACE, "LegalResourceSubdivision");
		LegalValue = Vocabularies.createIRI(NAMESPACE, "LegalValue");
		Manifestation = Vocabularies.createIRI(NAMESPACE, "Manifestation");
		Organization = Vocabularies.createIRI(NAMESPACE, "Organization");
		Person = Vocabularies.createIRI(NAMESPACE, "Person");
		ResourceType = Vocabularies.createIRI(NAMESPACE, "ResourceType");
		SubdivisionType = Vocabularies.createIRI(NAMESPACE, "SubdivisionType");
		Version = Vocabularies.createIRI(NAMESPACE, "Version");
		Work = Vocabularies.createIRI(NAMESPACE, "Work");
		WorkSubdivision = Vocabularies.createIRI(NAMESPACE, "WorkSubdivision");
		WorkType = Vocabularies.createIRI(NAMESPACE, "WorkType");

		amendedBy = Vocabularies.createIRI(NAMESPACE, "amended_by");
		amends = Vocabularies.createIRI(NAMESPACE, "amends");
		appliedBy = Vocabularies.createIRI(NAMESPACE, "applied_by");
		applies = Vocabularies.createIRI(NAMESPACE, "applies");
		basedOn = Vocabularies.createIRI(NAMESPACE, "based_on");
		basisFor = Vocabularies.createIRI(NAMESPACE, "basis_for");
		changedBy = Vocabularies.createIRI(NAMESPACE, "changed_by");
		changes = Vocabularies.createIRI(NAMESPACE, "changes");
		citedBy = Vocabularies.createIRI(NAMESPACE, "cited_by");
		citedByCaseLaw = Vocabularies.createIRI(NAMESPACE, "cited_by_case_law");
		citedByCaseLawReference = Vocabularies.createIRI(NAMESPACE, "cited_by_case_law_reference");
		cites = Vocabularies.createIRI(NAMESPACE, "cites");
		commencedBy = Vocabularies.createIRI(NAMESPACE, "commenced_by");
		commences = Vocabularies.createIRI(NAMESPACE, "commences");
		consolidatedBy = Vocabularies.createIRI(NAMESPACE, "consolidated_by");
		consolidates = Vocabularies.createIRI(NAMESPACE, "consolidates");
		correctedBy = Vocabularies.createIRI(NAMESPACE, "corrected_by");
		corrects = Vocabularies.createIRI(NAMESPACE, "corrects");
		countersignedBy = Vocabularies.createIRI(NAMESPACE, "countersigned_by");
		dateApplicability = Vocabularies.createIRI(NAMESPACE, "date_applicability");
		dateDocument = Vocabularies.createIRI(NAMESPACE, "date_document");
		dateNoLongerInForce = Vocabularies.createIRI(NAMESPACE, "date_no_longer_in_force");
		datePublication = Vocabularies.createIRI(NAMESPACE, "date_publication");
		description = Vocabularies.createIRI(NAMESPACE, "description");
		embodies = Vocabularies.createIRI(NAMESPACE, "embodies");
		ensuresImplementationOf = Vocabularies.createIRI(NAMESPACE, "ensures_implementation_of");
		firstDateEntryInForce = Vocabularies.createIRI(NAMESPACE, "first_date_entry_in_force");
		format = Vocabularies.createIRI(NAMESPACE, "format");
		hasAnnex = Vocabularies.createIRI(NAMESPACE, "has_annex");
		hasAnotherPublication = Vocabularies.createIRI(NAMESPACE, "has_another_publication");
		hasDerivative = Vocabularies.createIRI(NAMESPACE, "has_derivative");
		hasMember = Vocabularies.createIRI(NAMESPACE, "has_member");
		hasPart = Vocabularies.createIRI(NAMESPACE, "has_part");
		hasTranslation = Vocabularies.createIRI(NAMESPACE, "has_translation");
		idLocal = Vocabularies.createIRI(NAMESPACE, "id_local");
		implementationEnsuredBy = Vocabularies.createIRI(NAMESPACE, "implementation_ensured_by");
		implementedBy = Vocabularies.createIRI(NAMESPACE, "implemented_by");
		implements_ = Vocabularies.createIRI(NAMESPACE, "implements");
		inForce = Vocabularies.createIRI(NAMESPACE, "in_force");
		isAbout = Vocabularies.createIRI(NAMESPACE, "is_about");
		isAnnexOf = Vocabularies.createIRI(NAMESPACE, "is_annex_of");
		isAnotherPublicationOf = Vocabularies.createIRI(NAMESPACE, "is_another_publication_of");
		isDerivativeOf = Vocabularies.createIRI(NAMESPACE, "is_derivative_of");
		isEmbodiedBy = Vocabularies.createIRI(NAMESPACE, "is_embodied_by");
		isExemplifiedBy = Vocabularies.createIRI(NAMESPACE, "is_exemplified_by");
		isMemberOf = Vocabularies.createIRI(NAMESPACE, "is_member_of");
		isPartOf = Vocabularies.createIRI(NAMESPACE, "is_part_of");
		isRealizedBy = Vocabularies.createIRI(NAMESPACE, "is_realized_by");
		isTranslationOf = Vocabularies.createIRI(NAMESPACE, "is_translation_of");
		jurisdiction = Vocabularies.createIRI(NAMESPACE, "jurisdiction");
		language = Vocabularies.createIRI(NAMESPACE, "language");
		legalValue = Vocabularies.createIRI(NAMESPACE, "legal_value");
		licence = Vocabularies.createIRI(NAMESPACE, "licence");
		mediaType = Vocabularies.createIRI(NAMESPACE, "media_type");
		number = Vocabularies.createIRI(NAMESPACE, "number");
		passedBy = Vocabularies.createIRI(NAMESPACE, "passed_by");
		publishedIn = Vocabularies.createIRI(NAMESPACE, "published_in");
		publishedInFormat = Vocabularies.createIRI(NAMESPACE, "published_in_format");
		publisher = Vocabularies.createIRI(NAMESPACE, "publisher");
		publisherAgent = Vocabularies.createIRI(NAMESPACE, "publisher_agent");
		publishes = Vocabularies.createIRI(NAMESPACE, "publishes");
		realizes = Vocabularies.createIRI(NAMESPACE, "realizes");
		relatedTo = Vocabularies.createIRI(NAMESPACE, "related_to");
		relevantFor = Vocabularies.createIRI(NAMESPACE, "relevant_for");
		repealedBy = Vocabularies.createIRI(NAMESPACE, "repealed_by");
		repeals = Vocabularies.createIRI(NAMESPACE, "repeals");
		responsibilityOf = Vocabularies.createIRI(NAMESPACE, "responsibility_of");
		responsibilityOfAgent = Vocabularies.createIRI(NAMESPACE, "responsibility_of_agent");
		rights = Vocabularies.createIRI(NAMESPACE, "rights");
		rightsholder = Vocabularies.createIRI(NAMESPACE, "rightsholder");
		rightsholderAgent = Vocabularies.createIRI(NAMESPACE, "rightsholder_agent");
		title = Vocabularies.createIRI(NAMESPACE, "title");
		titleAlternative = Vocabularies.createIRI(NAMESPACE, "title_alternative");
		titleShort = Vocabularies.createIRI(NAMESPACE, "title_short");
		transposedBy = Vocabularies.createIRI(NAMESPACE, "transposed_by");
		transposes = Vocabularies.createIRI(NAMESPACE, "transposes");
		typeDocument = Vocabularies.createIRI(NAMESPACE, "type_document");
		typeSubdivision = Vocabularies.createIRI(NAMESPACE, "type_subdivision");
		uriSchema = Vocabularies.createIRI(NAMESPACE, "uri_schema");
		version = Vocabularies.createIRI(NAMESPACE, "version");
		versionDate = Vocabularies.createIRI(NAMESPACE, "version_date");
		workType = Vocabularies.createIRI(NAMESPACE, "work_type");

		AdministrativeAreaTable = Vocabularies.createIRI(NAMESPACE, "AdministrativeAreaTable");
		FormatTypeTable = Vocabularies.createIRI(NAMESPACE, "FormatTypeTable");
		InForceInForce = Vocabularies.createIRI(NAMESPACE, "InForce-inForce");
		InForceNotInForce = Vocabularies.createIRI(NAMESPACE, "InForce-notInForce");
		InForcePartiallyInForce = Vocabularies.createIRI(NAMESPACE, "InForce-partiallyInForce");
		InForceTable = Vocabularies.createIRI(NAMESPACE, "InForceTable");
		LegalValueAuthoritative = Vocabularies.createIRI(NAMESPACE, "LegalValue-authoritative");
		LegalValueDefinitive = Vocabularies.createIRI(NAMESPACE, "LegalValue-definitive");
		LegalValueOfficial = Vocabularies.createIRI(NAMESPACE, "LegalValue-official");
		LegalValueUnofficial = Vocabularies.createIRI(NAMESPACE, "LegalValue-unofficial");
		LegalValueTable = Vocabularies.createIRI(NAMESPACE, "LegalValueTable");
		ResourceTypeTable = Vocabularies.createIRI(NAMESPACE, "ResourceTypeTable");
		SubdivisionTypeTable = Vocabularies.createIRI(NAMESPACE, "SubdivisionTypeTable");
		VersionTable = Vocabularies.createIRI(NAMESPACE, "VersionTable");
		WorkTypeTable = Vocabularies.createIRI(NAMESPACE, "WorkTypeTable");
		printFormat = Vocabularies.createIRI(NAMESPACE, "print_format");

	}
}
