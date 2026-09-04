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

package org.eclipse.rdf4j.model.vocabulary.biblio;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.base.Vocabularies;

/**
 * Constants for the Citation Typing Ontology.
 *
 * @see <a href="http://purl.archive.org/spar/cito.html">Citation Typing Ontology</a>
 *
 * @author Bart Hanssens
 */
public class CITO {
	/**
	 * The CITO namespace: http://purl.org/spar/cito/
	 */
	public static final String NAMESPACE = "http://purl.org/spar/cito/";

	/**
	 * Recommended prefix for the namespace: "cito"
	 */
	public static final String PREFIX = "cito";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** cito:AffilationSelfCitation */
	public static final IRI AffilationSelfCitation;

	/** cito:AuthorNetworkSelfCitation */
	public static final IRI AuthorNetworkSelfCitation;

	/** cito:AuthorSelfCitation */
	public static final IRI AuthorSelfCitation;

	/** cito:Citation */
	public static final IRI Citation;

	/** cito:DistantCitation */
	public static final IRI DistantCitation;

	/** cito:FunderSelfCitation */
	public static final IRI FunderSelfCitation;

	/** cito:JournalCartelCitation */
	public static final IRI JournalCartelCitation;

	/** cito:JournalSelfCitation */
	public static final IRI JournalSelfCitation;

	/** cito:SelfCitation */
	public static final IRI SelfCitation;

	// Properties
	/** cito:agreesWith */
	public static final IRI agreesWith;

	/** cito:cites */
	public static final IRI cites;

	/** cito:citesAsAuthority */
	public static final IRI citesAsAuthority;

	/** cito:citesAsDataSource */
	public static final IRI citesAsDataSource;

	/** cito:citesAsEvidence */
	public static final IRI citesAsEvidence;

	/** cito:citesAsMetadataDocument */
	public static final IRI citesAsMetadataDocument;

	/** cito:citesAsPotentialSolution */
	public static final IRI citesAsPotentialSolution;

	/** cito:citesAsRecommendedReading */
	public static final IRI citesAsRecommendedReading;

	/** cito:citesAsRelated */
	public static final IRI citesAsRelated;

	/** cito:citesAsSourceDocument */
	public static final IRI citesAsSourceDocument;

	/** cito:citesForInformation */
	public static final IRI citesForInformation;

	/** cito:compiles */
	public static final IRI compiles;

	/** cito:confirms */
	public static final IRI confirms;

	/** cito:containsAssertionFrom */
	public static final IRI containsAssertionFrom;

	/** cito:corrects */
	public static final IRI corrects;

	/** cito:credits */
	public static final IRI credits;

	/** cito:critiques */
	public static final IRI critiques;

	/** cito:derides */
	public static final IRI derides;

	/** cito:describes */
	public static final IRI describes;

	/** cito:disagreesWith */
	public static final IRI disagreesWith;

	/** cito:discusses */
	public static final IRI discusses;

	/** cito:disputes */
	public static final IRI disputes;

	/** cito:documents */
	public static final IRI documents;

	/** cito:extends */
	// note that "extends" is a java reserved word */
	public static final IRI extends_prop;

	/** cito:givesBackgroundTo */
	public static final IRI givesBackgroundTo;

	/** cito:givesSupportTo */
	public static final IRI givesSupportTo;

	/** cito:hasCitationCharacterization */
	public static final IRI hasCitationCharacterization;

	/** cito:hasCitationCreationDate */
	public static final IRI hasCitationCreationDate;

	/** cito:hasCitationTimeSpan */
	public static final IRI hasCitationTimeSpan;

	/** cito:hasCitedEntity */
	public static final IRI hasCitedEntity;

	/** cito:hasCitingEntity */
	public static final IRI hasCitingEntity;

	/** cito:hasCoAuthorshipCitationLevel */
	public static final IRI hasCoAuthorshipCitationLevel;

	/** cito:hasReplyFrom */
	public static final IRI hasReplyFrom;

	/** cito:includesExcerptFrom */
	public static final IRI includesExcerptFrom;

	/** cito:includesQuotationFrom */
	public static final IRI includesQuotationFrom;

	/** cito:isAgreedWithBy */
	public static final IRI isAgreedWithBy;

	/** cito:isCitedAsAuthorityBy */
	public static final IRI isCitedAsAuthorityBy;

	/** cito:isCitedAsDataSourceBy */
	public static final IRI isCitedAsDataSourceBy;

	/** cito:isCitedAsEvidenceBy */
	public static final IRI isCitedAsEvidenceBy;

	/** cito:isCitedAsMetadataDocumentBy */
	public static final IRI isCitedAsMetadataDocumentBy;

	/** cito:isCitedAsPontentialSolutionBy */
	public static final IRI isCitedAsPontentialSolutionBy;

	/** cito:isCitedAsRecommendedReadingBy */
	public static final IRI isCitedAsRecommendedReadingBy;

	/** cito:isCitedAsRelatedBy */
	public static final IRI isCitedAsRelatedBy;

	/** cito:isCitedAsSourceDocumentBy */
	public static final IRI isCitedAsSourceDocumentBy;

	/** cito:isCitedBy */
	public static final IRI isCitedBy;

	/** cito:isCitedForInformationBy */
	public static final IRI isCitedForInformationBy;

	/** cito:isCompiledBy */
	public static final IRI isCompiledBy;

	/** cito:isConfirmedBy */
	public static final IRI isConfirmedBy;

	/** cito:isCorrectedBy */
	public static final IRI isCorrectedBy;

	/** cito:isCreditedBy */
	public static final IRI isCreditedBy;

	/** cito:isCritiquedBy */
	public static final IRI isCritiquedBy;

	/** cito:isDeridedBy */
	public static final IRI isDeridedBy;

	/** cito:isDescribedBy */
	public static final IRI isDescribedBy;

	/** cito:isDisagreedWithBy */
	public static final IRI isDisagreedWithBy;

	/** cito:isDiscussedBy */
	public static final IRI isDiscussedBy;

	/** cito:isDisputedBy */
	public static final IRI isDisputedBy;

	/** cito:isDocumentedBy */
	public static final IRI isDocumentedBy;

	/** cito:isExtendedBy */
	public static final IRI isExtendedBy;

	/** cito:isLinkedToBy */
	public static final IRI isLinkedToBy;

	/** cito:isParodiedBy */
	public static final IRI isParodiedBy;

	/** cito:isPlagiarizedBy */
	public static final IRI isPlagiarizedBy;

	/** cito:isQualifiedBy */
	public static final IRI isQualifiedBy;

	/** cito:isRefutedBy */
	public static final IRI isRefutedBy;

	/** cito:isRetractedBy */
	public static final IRI isRetractedBy;

	/** cito:isReviewedBy */
	public static final IRI isReviewedBy;

	/** cito:isRidiculedBy */
	public static final IRI isRidiculedBy;

	/** cito:isSpeculatedOnBy */
	public static final IRI isSpeculatedOnBy;

	/** cito:isSupportedBy */
	public static final IRI isSupportedBy;

	/** cito:isUpdatedBy */
	public static final IRI isUpdatedBy;

	/** cito:likes */
	public static final IRI likes;

	/** cito:linksTo */
	public static final IRI linksTo;

	/** cito:obtainsBackgroundFrom */
	public static final IRI obtainsBackgroundFrom;

	/** cito:obtainsSupportFrom */
	public static final IRI obtainsSupportFrom;

	/** cito:parodies */
	public static final IRI parodies;

	/** cito:plagiarizes */
	public static final IRI plagiarizes;

	/** cito:providesAssertionFor */
	public static final IRI providesAssertionFor;

	/** cito:providesConclusionsFor */
	public static final IRI providesConclusionsFor;

	/** cito:providesDataFor */
	public static final IRI providesDataFor;

	/** cito:providesExcerptFor */
	public static final IRI providesExcerptFor;

	/** cito:providesMethodFor */
	public static final IRI providesMethodFor;

	/** cito:providesQuotationFor */
	public static final IRI providesQuotationFor;

	/** cito:qualifies */
	public static final IRI qualifies;

	/** cito:refutes */
	public static final IRI refutes;

	/** cito:repliesTo */
	public static final IRI repliesTo;

	/** cito:retracts */
	public static final IRI retracts;

	/** cito:reviews */
	public static final IRI reviews;

	/** cito:ridicules */
	public static final IRI ridicules;

	/** cito:sharesAuthorInstitutionWith */
	public static final IRI sharesAuthorInstitutionWith;

	/** cito:sharesAuthorWith */
	public static final IRI sharesAuthorWith;

	/** cito:sharesFundingAgencyWith */
	public static final IRI sharesFundingAgencyWith;

	/** cito:sharesJournalWith */
	public static final IRI sharesJournalWith;

	/** cito:sharesPublicationVenueWith */
	public static final IRI sharesPublicationVenueWith;

	/** cito:speculatesOn */
	public static final IRI speculatesOn;

	/** cito:supports */
	public static final IRI supports;

	/** cito:updates */
	public static final IRI updates;

	/** cito:usesConclusionsFrom */
	public static final IRI usesConclusionsFrom;

	/** cito:usesDataFrom */
	public static final IRI usesDataFrom;

	/** cito:usesMethodIn */
	public static final IRI usesMethodIn;

	// Individuals

	static {
		AffilationSelfCitation = Vocabularies.createIRI(NAMESPACE, "AffilationSelfCitation");
		AuthorNetworkSelfCitation = Vocabularies.createIRI(NAMESPACE, "AuthorNetworkSelfCitation");
		AuthorSelfCitation = Vocabularies.createIRI(NAMESPACE, "AuthorSelfCitation");
		Citation = Vocabularies.createIRI(NAMESPACE, "Citation");
		DistantCitation = Vocabularies.createIRI(NAMESPACE, "DistantCitation");
		FunderSelfCitation = Vocabularies.createIRI(NAMESPACE, "FunderSelfCitation");
		JournalCartelCitation = Vocabularies.createIRI(NAMESPACE, "JournalCartelCitation");
		JournalSelfCitation = Vocabularies.createIRI(NAMESPACE, "JournalSelfCitation");
		SelfCitation = Vocabularies.createIRI(NAMESPACE, "SelfCitation");

		agreesWith = Vocabularies.createIRI(NAMESPACE, "agreesWith");
		cites = Vocabularies.createIRI(NAMESPACE, "cites");
		citesAsAuthority = Vocabularies.createIRI(NAMESPACE, "citesAsAuthority");
		citesAsDataSource = Vocabularies.createIRI(NAMESPACE, "citesAsDataSource");
		citesAsEvidence = Vocabularies.createIRI(NAMESPACE, "citesAsEvidence");
		citesAsMetadataDocument = Vocabularies.createIRI(NAMESPACE, "citesAsMetadataDocument");
		citesAsPotentialSolution = Vocabularies.createIRI(NAMESPACE, "citesAsPotentialSolution");
		citesAsRecommendedReading = Vocabularies.createIRI(NAMESPACE, "citesAsRecommendedReading");
		citesAsRelated = Vocabularies.createIRI(NAMESPACE, "citesAsRelated");
		citesAsSourceDocument = Vocabularies.createIRI(NAMESPACE, "citesAsSourceDocument");
		citesForInformation = Vocabularies.createIRI(NAMESPACE, "citesForInformation");
		compiles = Vocabularies.createIRI(NAMESPACE, "compiles");
		confirms = Vocabularies.createIRI(NAMESPACE, "confirms");
		containsAssertionFrom = Vocabularies.createIRI(NAMESPACE, "containsAssertionFrom");
		corrects = Vocabularies.createIRI(NAMESPACE, "corrects");
		credits = Vocabularies.createIRI(NAMESPACE, "credits");
		critiques = Vocabularies.createIRI(NAMESPACE, "critiques");
		derides = Vocabularies.createIRI(NAMESPACE, "derides");
		describes = Vocabularies.createIRI(NAMESPACE, "describes");
		disagreesWith = Vocabularies.createIRI(NAMESPACE, "disagreesWith");
		discusses = Vocabularies.createIRI(NAMESPACE, "discusses");
		disputes = Vocabularies.createIRI(NAMESPACE, "disputes");
		documents = Vocabularies.createIRI(NAMESPACE, "documents");
		extends_prop = Vocabularies.createIRI(NAMESPACE, "extends");
		givesBackgroundTo = Vocabularies.createIRI(NAMESPACE, "givesBackgroundTo");
		givesSupportTo = Vocabularies.createIRI(NAMESPACE, "givesSupportTo");
		hasCitationCharacterization = Vocabularies.createIRI(NAMESPACE, "hasCitationCharacterization");
		hasCitationCreationDate = Vocabularies.createIRI(NAMESPACE, "hasCitationCreationDate");
		hasCitationTimeSpan = Vocabularies.createIRI(NAMESPACE, "hasCitationTimeSpan");
		hasCitedEntity = Vocabularies.createIRI(NAMESPACE, "hasCitedEntity");
		hasCitingEntity = Vocabularies.createIRI(NAMESPACE, "hasCitingEntity");
		hasCoAuthorshipCitationLevel = Vocabularies.createIRI(NAMESPACE, "hasCoAuthorshipCitationLevel");
		hasReplyFrom = Vocabularies.createIRI(NAMESPACE, "hasReplyFrom");
		includesExcerptFrom = Vocabularies.createIRI(NAMESPACE, "includesExcerptFrom");
		includesQuotationFrom = Vocabularies.createIRI(NAMESPACE, "includesQuotationFrom");
		isAgreedWithBy = Vocabularies.createIRI(NAMESPACE, "isAgreedWithBy");
		isCitedAsAuthorityBy = Vocabularies.createIRI(NAMESPACE, "isCitedAsAuthorityBy");
		isCitedAsDataSourceBy = Vocabularies.createIRI(NAMESPACE, "isCitedAsDataSourceBy");
		isCitedAsEvidenceBy = Vocabularies.createIRI(NAMESPACE, "isCitedAsEvidenceBy");
		isCitedAsMetadataDocumentBy = Vocabularies.createIRI(NAMESPACE, "isCitedAsMetadataDocumentBy");
		isCitedAsPontentialSolutionBy = Vocabularies.createIRI(NAMESPACE, "isCitedAsPontentialSolutionBy");
		isCitedAsRecommendedReadingBy = Vocabularies.createIRI(NAMESPACE, "isCitedAsRecommendedReadingBy");
		isCitedAsRelatedBy = Vocabularies.createIRI(NAMESPACE, "isCitedAsRelatedBy");
		isCitedAsSourceDocumentBy = Vocabularies.createIRI(NAMESPACE, "isCitedAsSourceDocumentBy");
		isCitedBy = Vocabularies.createIRI(NAMESPACE, "isCitedBy");
		isCitedForInformationBy = Vocabularies.createIRI(NAMESPACE, "isCitedForInformationBy");
		isCompiledBy = Vocabularies.createIRI(NAMESPACE, "isCompiledBy");
		isConfirmedBy = Vocabularies.createIRI(NAMESPACE, "isConfirmedBy");
		isCorrectedBy = Vocabularies.createIRI(NAMESPACE, "isCorrectedBy");
		isCreditedBy = Vocabularies.createIRI(NAMESPACE, "isCreditedBy");
		isCritiquedBy = Vocabularies.createIRI(NAMESPACE, "isCritiquedBy");
		isDeridedBy = Vocabularies.createIRI(NAMESPACE, "isDeridedBy");
		isDescribedBy = Vocabularies.createIRI(NAMESPACE, "isDescribedBy");
		isDisagreedWithBy = Vocabularies.createIRI(NAMESPACE, "isDisagreedWithBy");
		isDiscussedBy = Vocabularies.createIRI(NAMESPACE, "isDiscussedBy");
		isDisputedBy = Vocabularies.createIRI(NAMESPACE, "isDisputedBy");
		isDocumentedBy = Vocabularies.createIRI(NAMESPACE, "isDocumentedBy");
		isExtendedBy = Vocabularies.createIRI(NAMESPACE, "isExtendedBy");
		isLinkedToBy = Vocabularies.createIRI(NAMESPACE, "isLinkedToBy");
		isParodiedBy = Vocabularies.createIRI(NAMESPACE, "isParodiedBy");
		isPlagiarizedBy = Vocabularies.createIRI(NAMESPACE, "isPlagiarizedBy");
		isQualifiedBy = Vocabularies.createIRI(NAMESPACE, "isQualifiedBy");
		isRefutedBy = Vocabularies.createIRI(NAMESPACE, "isRefutedBy");
		isRetractedBy = Vocabularies.createIRI(NAMESPACE, "isRetractedBy");
		isReviewedBy = Vocabularies.createIRI(NAMESPACE, "isReviewedBy");
		isRidiculedBy = Vocabularies.createIRI(NAMESPACE, "isRidiculedBy");
		isSpeculatedOnBy = Vocabularies.createIRI(NAMESPACE, "isSpeculatedOnBy");
		isSupportedBy = Vocabularies.createIRI(NAMESPACE, "isSupportedBy");
		isUpdatedBy = Vocabularies.createIRI(NAMESPACE, "isUpdatedBy");
		likes = Vocabularies.createIRI(NAMESPACE, "likes");
		linksTo = Vocabularies.createIRI(NAMESPACE, "linksTo");
		obtainsBackgroundFrom = Vocabularies.createIRI(NAMESPACE, "obtainsBackgroundFrom");
		obtainsSupportFrom = Vocabularies.createIRI(NAMESPACE, "obtainsSupportFrom");
		parodies = Vocabularies.createIRI(NAMESPACE, "parodies");
		plagiarizes = Vocabularies.createIRI(NAMESPACE, "plagiarizes");
		providesAssertionFor = Vocabularies.createIRI(NAMESPACE, "providesAssertionFor");
		providesConclusionsFor = Vocabularies.createIRI(NAMESPACE, "providesConclusionsFor");
		providesDataFor = Vocabularies.createIRI(NAMESPACE, "providesDataFor");
		providesExcerptFor = Vocabularies.createIRI(NAMESPACE, "providesExcerptFor");
		providesMethodFor = Vocabularies.createIRI(NAMESPACE, "providesMethodFor");
		providesQuotationFor = Vocabularies.createIRI(NAMESPACE, "providesQuotationFor");
		qualifies = Vocabularies.createIRI(NAMESPACE, "qualifies");
		refutes = Vocabularies.createIRI(NAMESPACE, "refutes");
		repliesTo = Vocabularies.createIRI(NAMESPACE, "repliesTo");
		retracts = Vocabularies.createIRI(NAMESPACE, "retracts");
		reviews = Vocabularies.createIRI(NAMESPACE, "reviews");
		ridicules = Vocabularies.createIRI(NAMESPACE, "ridicules");
		sharesAuthorInstitutionWith = Vocabularies.createIRI(NAMESPACE, "sharesAuthorInstitutionWith");
		sharesAuthorWith = Vocabularies.createIRI(NAMESPACE, "sharesAuthorWith");
		sharesFundingAgencyWith = Vocabularies.createIRI(NAMESPACE, "sharesFundingAgencyWith");
		sharesJournalWith = Vocabularies.createIRI(NAMESPACE, "sharesJournalWith");
		sharesPublicationVenueWith = Vocabularies.createIRI(NAMESPACE, "sharesPublicationVenueWith");
		speculatesOn = Vocabularies.createIRI(NAMESPACE, "speculatesOn");
		supports = Vocabularies.createIRI(NAMESPACE, "supports");
		updates = Vocabularies.createIRI(NAMESPACE, "updates");
		usesConclusionsFrom = Vocabularies.createIRI(NAMESPACE, "usesConclusionsFrom");
		usesDataFrom = Vocabularies.createIRI(NAMESPACE, "usesDataFrom");
		usesMethodIn = Vocabularies.createIRI(NAMESPACE, "usesMethodIn");
	}
}
