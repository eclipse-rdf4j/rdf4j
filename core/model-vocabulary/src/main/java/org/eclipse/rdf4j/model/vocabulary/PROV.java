/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import static org.eclipse.rdf4j.model.base.AbstractIRI.createIRI;
import static org.eclipse.rdf4j.model.base.AbstractNamespace.createNamespace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for the Provenance Ontology.
 *
 * @see <a href="https://www.w3.org/TR/prov-overview/">Provenance Ontology</a>
 *
 * @author Bart Hanssens
 */
public class PROV {
	/**
	 * The PROV-O namespace: http://www.w3.org/ns/prov#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/prov#";

	/**
	 * Recommended prefix for the namespace: "prov"
	 */
	public static final String PREFIX = "prov";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** prov:Accept */
	public static final IRI ACCEPT;

	/** prov:Activity */
	public static final IRI ACTIVITY;

	/** prov:ActivityInfluence */
	public static final IRI ACTIVITY_INFLUENCE;

	/** prov:Agent */
	public static final IRI AGENT;

	/** prov:AgentInfluence */
	public static final IRI AGENT_INFLUENCE;

	/** prov:Association */
	public static final IRI ASSOCIATION;

	/** prov:Attribution */
	public static final IRI ATTRIBUTION;

	/** prov:Bundle */
	public static final IRI BUNDLE;

	/** prov:Collection */
	public static final IRI COLLECTION;

	/** prov:Communication */
	public static final IRI COMMUNICATION;

	/** prov:Contribute */
	public static final IRI CONTRIBUTE;

	/** prov:Contributor */
	public static final IRI CONTRIBUTOR;

	/** prov:Copyright */
	public static final IRI COPYRIGHT;

	/** prov:Create */
	public static final IRI CREATE;

	/** prov:Creator */
	public static final IRI CREATOR;

	/** prov:Delegation */
	public static final IRI DELEGATION;

	/** prov:Derivation */
	public static final IRI DERIVATION;

	/** prov:Dictionary */
	public static final IRI DICTIONARY;

	/** prov:DirectQueryService */
	public static final IRI DIRECT_QUERY_SERVICE;

	/** prov:EmptyCollection */
	public static final IRI EMPTY_COLLECTION;

	/** prov:EmptyDictionary */
	public static final IRI EMPTY_DICTIONARY;

	/** prov:End */
	public static final IRI END;

	/** prov:Entity */
	public static final IRI ENTITY;

	/** prov:EntityInfluence */
	public static final IRI ENTITY_INFLUENCE;

	/** prov:Generation */
	public static final IRI GENERATION;

	/** prov:Influence */
	public static final IRI INFLUENCE;

	/** prov:Insertion */
	public static final IRI INSERTION;

	/** prov:InstantaneousEvent */
	public static final IRI INSTANTANEOUS_EVENT;

	/** prov:Invalidation */
	public static final IRI INVALIDATION;

	/** prov:KeyEntityPair */
	public static final IRI KEY_ENTITY_PAIR;

	/** prov:Location */
	public static final IRI LOCATION;

	/** prov:Modify */
	public static final IRI MODIFY;

	/** prov:Organization */
	public static final IRI ORGANIZATION;

	/** prov:Person */
	public static final IRI PERSON;

	/** prov:Plan */
	public static final IRI PLAN;

	/** prov:PrimarySource */
	public static final IRI PRIMARY_SOURCE;

	/** prov:Publish */
	public static final IRI PUBLISH;

	/** prov:Publisher */
	public static final IRI PUBLISHER;

	/** prov:Quotation */
	public static final IRI QUOTATION;

	/** prov:Removal */
	public static final IRI REMOVAL;

	/** prov:Replace */
	public static final IRI REPLACE;

	/** prov:Revision */
	public static final IRI REVISION;

	/** prov:RightsAssignment */
	public static final IRI RIGHTS_ASSIGNMENT;

	/** prov:RightsHolder */
	public static final IRI RIGHTS_HOLDER;

	/** prov:Role */
	public static final IRI ROLE;

	/** prov:ServiceDescription */
	public static final IRI SERVICE_DESCRIPTION;

	/** prov:SoftwareAgent */
	public static final IRI SOFTWARE_AGENT;

	/** prov:Start */
	public static final IRI START;

	/** prov:Submit */
	public static final IRI SUBMIT;

	/** prov:Usage */
	public static final IRI USAGE;

	// Properties
	/** prov:actedOnBehalfOf */
	public static final IRI ACTED_ON_BEHALF_OF;

	/** prov:activity */
	public static final IRI ACTIVITY_PROP;

	/** prov:agent */
	public static final IRI AGENT_PROP;

	/** prov:alternateOf */
	public static final IRI ALTERNATE_OF;

	/** prov:asInBundle */
	public static final IRI AS_IN_BUNDLE;

	/** prov:atLocation */
	public static final IRI AT_LOCATION;

	/** prov:atTime */
	public static final IRI AT_TIME;

	/** prov:derivedByInsertionFrom */
	public static final IRI DERIVED_BY_INSERTION_FROM;

	/** prov:derivedByRemovalFrom */
	public static final IRI DERIVED_BY_REMOVAL_FROM;

	/** prov:describesService */
	public static final IRI DESCRIBES_SERVICE;

	/** prov:dictionary */
	public static final IRI DICTIONARY_PROP;

	/** prov:endedAtTime */
	public static final IRI ENDED_AT_TIME;

	/** prov:entity */
	public static final IRI ENTITY_PROP;

	/** prov:generated */
	public static final IRI GENERATED;

	/** prov:generatedAtTime */
	public static final IRI GENERATED_AT_TIME;

	/** prov:hadActivity */
	public static final IRI HAD_ACTIVITY;

	/** prov:hadDictionaryMember */
	public static final IRI HAD_DICTIONARY_MEMBER;

	/** prov:hadGeneration */
	public static final IRI HAD_GENERATION;

	/** prov:hadMember */
	public static final IRI HAD_MEMBER;

	/** prov:hadPlan */
	public static final IRI HAD_PLAN;

	/** prov:hadPrimarySource */
	public static final IRI HAD_PRIMARY_SOURCE;

	/** prov:hadRole */
	public static final IRI HAD_ROLE;

	/** prov:hadUsage */
	public static final IRI HAD_USAGE;

	/** prov:has_anchor */
	public static final IRI HAS_ANCHOR;

	/** prov:has_provenance */
	public static final IRI HAS_PROVENANCE;

	/** prov:has_query_service */
	public static final IRI HAS_QUERY_SERVICE;

	/** prov:influenced */
	public static final IRI INFLUENCED;

	/** prov:influencer */
	public static final IRI INFLUENCER;

	/** prov:insertedKeyEntityPair */
	public static final IRI INSERTED_KEY_ENTITY_PAIR;

	/** prov:invalidated */
	public static final IRI INVALIDATED;

	/** prov:invalidatedAtTime */
	public static final IRI INVALIDATED_AT_TIME;

	/** prov:mentionOf */
	public static final IRI MENTION_OF;

	/** prov:pairEntity */
	public static final IRI PAIR_ENTITY;

	/** prov:pairKey */
	public static final IRI PAIR_KEY;

	/** prov:pingback */
	public static final IRI PINGBACK;

	/** prov:provenanceUriTemplate */
	public static final IRI PROVENANCE_URI_TEMPLATE;

	/** prov:qualifiedAssociation */
	public static final IRI QUALIFIED_ASSOCIATION;

	/** prov:qualifiedAttribution */
	public static final IRI QUALIFIED_ATTRIBUTION;

	/** prov:qualifiedCommunication */
	public static final IRI QUALIFIED_COMMUNICATION;

	/** prov:qualifiedDelegation */
	public static final IRI QUALIFIED_DELEGATION;

	/** prov:qualifiedDerivation */
	public static final IRI QUALIFIED_DERIVATION;

	/** prov:qualifiedEnd */
	public static final IRI QUALIFIED_END;

	/** prov:qualifiedGeneration */
	public static final IRI QUALIFIED_GENERATION;

	/** prov:qualifiedInfluence */
	public static final IRI QUALIFIED_INFLUENCE;

	/** prov:qualifiedInsertion */
	public static final IRI QUALIFIED_INSERTION;

	/** prov:qualifiedInvalidation */
	public static final IRI QUALIFIED_INVALIDATION;

	/** prov:qualifiedPrimarySource */
	public static final IRI QUALIFIED_PRIMARY_SOURCE;

	/** prov:qualifiedQuotation */
	public static final IRI QUALIFIED_QUOTATION;

	/** prov:qualifiedRemoval */
	public static final IRI QUALIFIED_REMOVAL;

	/** prov:qualifiedRevision */
	public static final IRI QUALIFIED_REVISION;

	/** prov:qualifiedStart */
	public static final IRI QUALIFIED_START;

	/** prov:qualifiedUsage */
	public static final IRI QUALIFIED_USAGE;

	/** prov:removedKey */
	public static final IRI REMOVED_KEY;

	/** prov:specializationOf */
	public static final IRI SPECIALIZATION_OF;

	/** prov:startedAtTime */
	public static final IRI STARTED_AT_TIME;

	/** prov:used */
	public static final IRI USED;

	/** prov:value */
	public static final IRI VALUE;

	/** prov:wasAssociatedWith */
	public static final IRI WAS_ASSOCIATED_WITH;

	/** prov:wasAttributedTo */
	public static final IRI WAS_ATTRIBUTED_TO;

	/** prov:wasDerivedFrom */
	public static final IRI WAS_DERIVED_FROM;

	/** prov:wasEndedBy */
	public static final IRI WAS_ENDED_BY;

	/** prov:wasGeneratedBy */
	public static final IRI WAS_GENERATED_BY;

	/** prov:wasInfluencedBy */
	public static final IRI WAS_INFLUENCED_BY;

	/** prov:wasInformedBy */
	public static final IRI WAS_INFORMED_BY;

	/** prov:wasInvalidatedBy */
	public static final IRI WAS_INVALIDATED_BY;

	/** prov:wasQuotedFrom */
	public static final IRI WAS_QUOTED_FROM;

	/** prov:wasRevisionOf */
	public static final IRI WAS_REVISION_OF;

	/** prov:wasStartedBy */
	public static final IRI WAS_STARTED_BY;

	static {

		ACCEPT = createIRI(NAMESPACE, "Accept");
		ACTIVITY = createIRI(NAMESPACE, "Activity");
		ACTIVITY_INFLUENCE = createIRI(NAMESPACE, "ActivityInfluence");
		AGENT = createIRI(NAMESPACE, "Agent");
		AGENT_INFLUENCE = createIRI(NAMESPACE, "AgentInfluence");
		ASSOCIATION = createIRI(NAMESPACE, "Association");
		ATTRIBUTION = createIRI(NAMESPACE, "Attribution");
		BUNDLE = createIRI(NAMESPACE, "Bundle");
		COLLECTION = createIRI(NAMESPACE, "Collection");
		COMMUNICATION = createIRI(NAMESPACE, "Communication");
		CONTRIBUTE = createIRI(NAMESPACE, "Contribute");
		CONTRIBUTOR = createIRI(NAMESPACE, "Contributor");
		COPYRIGHT = createIRI(NAMESPACE, "Copyright");
		CREATE = createIRI(NAMESPACE, "Create");
		CREATOR = createIRI(NAMESPACE, "Creator");
		DELEGATION = createIRI(NAMESPACE, "Delegation");
		DERIVATION = createIRI(NAMESPACE, "Derivation");
		DICTIONARY = createIRI(NAMESPACE, "Dictionary");
		DIRECT_QUERY_SERVICE = createIRI(NAMESPACE, "DirectQueryService");
		EMPTY_COLLECTION = createIRI(NAMESPACE, "EmptyCollection");
		EMPTY_DICTIONARY = createIRI(NAMESPACE, "EmptyDictionary");
		END = createIRI(NAMESPACE, "End");
		ENTITY = createIRI(NAMESPACE, "Entity");
		ENTITY_INFLUENCE = createIRI(NAMESPACE, "EntityInfluence");
		GENERATION = createIRI(NAMESPACE, "Generation");
		INFLUENCE = createIRI(NAMESPACE, "Influence");
		INSERTION = createIRI(NAMESPACE, "Insertion");
		INSTANTANEOUS_EVENT = createIRI(NAMESPACE, "InstantaneousEvent");
		INVALIDATION = createIRI(NAMESPACE, "Invalidation");
		KEY_ENTITY_PAIR = createIRI(NAMESPACE, "KeyEntityPair");
		LOCATION = createIRI(NAMESPACE, "Location");
		MODIFY = createIRI(NAMESPACE, "Modify");
		ORGANIZATION = createIRI(NAMESPACE, "Organization");
		PERSON = createIRI(NAMESPACE, "Person");
		PLAN = createIRI(NAMESPACE, "Plan");
		PRIMARY_SOURCE = createIRI(NAMESPACE, "PrimarySource");
		PUBLISH = createIRI(NAMESPACE, "Publish");
		PUBLISHER = createIRI(NAMESPACE, "Publisher");
		QUOTATION = createIRI(NAMESPACE, "Quotation");
		REMOVAL = createIRI(NAMESPACE, "Removal");
		REPLACE = createIRI(NAMESPACE, "Replace");
		REVISION = createIRI(NAMESPACE, "Revision");
		RIGHTS_ASSIGNMENT = createIRI(NAMESPACE, "RightsAssignment");
		RIGHTS_HOLDER = createIRI(NAMESPACE, "RightsHolder");
		ROLE = createIRI(NAMESPACE, "Role");
		SERVICE_DESCRIPTION = createIRI(NAMESPACE, "ServiceDescription");
		SOFTWARE_AGENT = createIRI(NAMESPACE, "SoftwareAgent");
		START = createIRI(NAMESPACE, "Start");
		SUBMIT = createIRI(NAMESPACE, "Submit");
		USAGE = createIRI(NAMESPACE, "Usage");

		ACTED_ON_BEHALF_OF = createIRI(NAMESPACE, "actedOnBehalfOf");
		ACTIVITY_PROP = createIRI(NAMESPACE, "activity");
		AGENT_PROP = createIRI(NAMESPACE, "agent");
		ALTERNATE_OF = createIRI(NAMESPACE, "alternateOf");
		AS_IN_BUNDLE = createIRI(NAMESPACE, "asInBundle");
		AT_LOCATION = createIRI(NAMESPACE, "atLocation");
		AT_TIME = createIRI(NAMESPACE, "atTime");
		DERIVED_BY_INSERTION_FROM = createIRI(NAMESPACE, "derivedByInsertionFrom");
		DERIVED_BY_REMOVAL_FROM = createIRI(NAMESPACE, "derivedByRemovalFrom");
		DESCRIBES_SERVICE = createIRI(NAMESPACE, "describesService");
		DICTIONARY_PROP = createIRI(NAMESPACE, "dictionary");
		ENDED_AT_TIME = createIRI(NAMESPACE, "endedAtTime");
		ENTITY_PROP = createIRI(NAMESPACE, "entity");
		GENERATED = createIRI(NAMESPACE, "generated");
		GENERATED_AT_TIME = createIRI(NAMESPACE, "generatedAtTime");
		HAD_ACTIVITY = createIRI(NAMESPACE, "hadActivity");
		HAD_DICTIONARY_MEMBER = createIRI(NAMESPACE, "hadDictionaryMember");
		HAD_GENERATION = createIRI(NAMESPACE, "hadGeneration");
		HAD_MEMBER = createIRI(NAMESPACE, "hadMember");
		HAD_PLAN = createIRI(NAMESPACE, "hadPlan");
		HAD_PRIMARY_SOURCE = createIRI(NAMESPACE, "hadPrimarySource");
		HAD_ROLE = createIRI(NAMESPACE, "hadRole");
		HAD_USAGE = createIRI(NAMESPACE, "hadUsage");
		HAS_ANCHOR = createIRI(NAMESPACE, "has_anchor");
		HAS_PROVENANCE = createIRI(NAMESPACE, "has_provenance");
		HAS_QUERY_SERVICE = createIRI(NAMESPACE, "has_query_service");
		INFLUENCED = createIRI(NAMESPACE, "influenced");
		INFLUENCER = createIRI(NAMESPACE, "influencer");
		INSERTED_KEY_ENTITY_PAIR = createIRI(NAMESPACE, "insertedKeyEntityPair");
		INVALIDATED = createIRI(NAMESPACE, "invalidated");
		INVALIDATED_AT_TIME = createIRI(NAMESPACE, "invalidatedAtTime");
		MENTION_OF = createIRI(NAMESPACE, "mentionOf");
		PAIR_ENTITY = createIRI(NAMESPACE, "pairEntity");
		PAIR_KEY = createIRI(NAMESPACE, "pairKey");
		PINGBACK = createIRI(NAMESPACE, "pingback");
		PROVENANCE_URI_TEMPLATE = createIRI(NAMESPACE, "provenanceUriTemplate");
		QUALIFIED_ASSOCIATION = createIRI(NAMESPACE, "qualifiedAssociation");
		QUALIFIED_ATTRIBUTION = createIRI(NAMESPACE, "qualifiedAttribution");
		QUALIFIED_COMMUNICATION = createIRI(NAMESPACE, "qualifiedCommunication");
		QUALIFIED_DELEGATION = createIRI(NAMESPACE, "qualifiedDelegation");
		QUALIFIED_DERIVATION = createIRI(NAMESPACE, "qualifiedDerivation");
		QUALIFIED_END = createIRI(NAMESPACE, "qualifiedEnd");
		QUALIFIED_GENERATION = createIRI(NAMESPACE, "qualifiedGeneration");
		QUALIFIED_INFLUENCE = createIRI(NAMESPACE, "qualifiedInfluence");
		QUALIFIED_INSERTION = createIRI(NAMESPACE, "qualifiedInsertion");
		QUALIFIED_INVALIDATION = createIRI(NAMESPACE, "qualifiedInvalidation");
		QUALIFIED_PRIMARY_SOURCE = createIRI(NAMESPACE, "qualifiedPrimarySource");
		QUALIFIED_QUOTATION = createIRI(NAMESPACE, "qualifiedQuotation");
		QUALIFIED_REMOVAL = createIRI(NAMESPACE, "qualifiedRemoval");
		QUALIFIED_REVISION = createIRI(NAMESPACE, "qualifiedRevision");
		QUALIFIED_START = createIRI(NAMESPACE, "qualifiedStart");
		QUALIFIED_USAGE = createIRI(NAMESPACE, "qualifiedUsage");
		REMOVED_KEY = createIRI(NAMESPACE, "removedKey");
		SPECIALIZATION_OF = createIRI(NAMESPACE, "specializationOf");
		STARTED_AT_TIME = createIRI(NAMESPACE, "startedAtTime");
		USED = createIRI(NAMESPACE, "used");
		VALUE = createIRI(NAMESPACE, "value");
		WAS_ASSOCIATED_WITH = createIRI(NAMESPACE, "wasAssociatedWith");
		WAS_ATTRIBUTED_TO = createIRI(NAMESPACE, "wasAttributedTo");
		WAS_DERIVED_FROM = createIRI(NAMESPACE, "wasDerivedFrom");
		WAS_ENDED_BY = createIRI(NAMESPACE, "wasEndedBy");
		WAS_GENERATED_BY = createIRI(NAMESPACE, "wasGeneratedBy");
		WAS_INFLUENCED_BY = createIRI(NAMESPACE, "wasInfluencedBy");
		WAS_INFORMED_BY = createIRI(NAMESPACE, "wasInformedBy");
		WAS_INVALIDATED_BY = createIRI(NAMESPACE, "wasInvalidatedBy");
		WAS_QUOTED_FROM = createIRI(NAMESPACE, "wasQuotedFrom");
		WAS_REVISION_OF = createIRI(NAMESPACE, "wasRevisionOf");
		WAS_STARTED_BY = createIRI(NAMESPACE, "wasStartedBy");
	}
}
