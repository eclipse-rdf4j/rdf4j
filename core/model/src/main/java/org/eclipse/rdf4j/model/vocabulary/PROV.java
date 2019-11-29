/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

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
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

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
		ValueFactory factory = SimpleValueFactory.getInstance();

		ACCEPT = factory.createIRI(NAMESPACE, "Accept");
		ACTIVITY = factory.createIRI(NAMESPACE, "Activity");
		ACTIVITY_INFLUENCE = factory.createIRI(NAMESPACE, "ActivityInfluence");
		AGENT = factory.createIRI(NAMESPACE, "Agent");
		AGENT_INFLUENCE = factory.createIRI(NAMESPACE, "AgentInfluence");
		ASSOCIATION = factory.createIRI(NAMESPACE, "Association");
		ATTRIBUTION = factory.createIRI(NAMESPACE, "Attribution");
		BUNDLE = factory.createIRI(NAMESPACE, "Bundle");
		COLLECTION = factory.createIRI(NAMESPACE, "Collection");
		COMMUNICATION = factory.createIRI(NAMESPACE, "Communication");
		CONTRIBUTE = factory.createIRI(NAMESPACE, "Contribute");
		CONTRIBUTOR = factory.createIRI(NAMESPACE, "Contributor");
		COPYRIGHT = factory.createIRI(NAMESPACE, "Copyright");
		CREATE = factory.createIRI(NAMESPACE, "Create");
		CREATOR = factory.createIRI(NAMESPACE, "Creator");
		DELEGATION = factory.createIRI(NAMESPACE, "Delegation");
		DERIVATION = factory.createIRI(NAMESPACE, "Derivation");
		DICTIONARY = factory.createIRI(NAMESPACE, "Dictionary");
		DIRECT_QUERY_SERVICE = factory.createIRI(NAMESPACE, "DirectQueryService");
		EMPTY_COLLECTION = factory.createIRI(NAMESPACE, "EmptyCollection");
		EMPTY_DICTIONARY = factory.createIRI(NAMESPACE, "EmptyDictionary");
		END = factory.createIRI(NAMESPACE, "End");
		ENTITY = factory.createIRI(NAMESPACE, "Entity");
		ENTITY_INFLUENCE = factory.createIRI(NAMESPACE, "EntityInfluence");
		GENERATION = factory.createIRI(NAMESPACE, "Generation");
		INFLUENCE = factory.createIRI(NAMESPACE, "Influence");
		INSERTION = factory.createIRI(NAMESPACE, "Insertion");
		INSTANTANEOUS_EVENT = factory.createIRI(NAMESPACE, "InstantaneousEvent");
		INVALIDATION = factory.createIRI(NAMESPACE, "Invalidation");
		KEY_ENTITY_PAIR = factory.createIRI(NAMESPACE, "KeyEntityPair");
		LOCATION = factory.createIRI(NAMESPACE, "Location");
		MODIFY = factory.createIRI(NAMESPACE, "Modify");
		ORGANIZATION = factory.createIRI(NAMESPACE, "Organization");
		PERSON = factory.createIRI(NAMESPACE, "Person");
		PLAN = factory.createIRI(NAMESPACE, "Plan");
		PRIMARY_SOURCE = factory.createIRI(NAMESPACE, "PrimarySource");
		PUBLISH = factory.createIRI(NAMESPACE, "Publish");
		PUBLISHER = factory.createIRI(NAMESPACE, "Publisher");
		QUOTATION = factory.createIRI(NAMESPACE, "Quotation");
		REMOVAL = factory.createIRI(NAMESPACE, "Removal");
		REPLACE = factory.createIRI(NAMESPACE, "Replace");
		REVISION = factory.createIRI(NAMESPACE, "Revision");
		RIGHTS_ASSIGNMENT = factory.createIRI(NAMESPACE, "RightsAssignment");
		RIGHTS_HOLDER = factory.createIRI(NAMESPACE, "RightsHolder");
		ROLE = factory.createIRI(NAMESPACE, "Role");
		SERVICE_DESCRIPTION = factory.createIRI(NAMESPACE, "ServiceDescription");
		SOFTWARE_AGENT = factory.createIRI(NAMESPACE, "SoftwareAgent");
		START = factory.createIRI(NAMESPACE, "Start");
		SUBMIT = factory.createIRI(NAMESPACE, "Submit");
		USAGE = factory.createIRI(NAMESPACE, "Usage");

		ACTED_ON_BEHALF_OF = factory.createIRI(NAMESPACE, "actedOnBehalfOf");
		ACTIVITY_PROP = factory.createIRI(NAMESPACE, "activity");
		AGENT_PROP = factory.createIRI(NAMESPACE, "agent");
		ALTERNATE_OF = factory.createIRI(NAMESPACE, "alternateOf");
		AS_IN_BUNDLE = factory.createIRI(NAMESPACE, "asInBundle");
		AT_LOCATION = factory.createIRI(NAMESPACE, "atLocation");
		AT_TIME = factory.createIRI(NAMESPACE, "atTime");
		DERIVED_BY_INSERTION_FROM = factory.createIRI(NAMESPACE, "derivedByInsertionFrom");
		DERIVED_BY_REMOVAL_FROM = factory.createIRI(NAMESPACE, "derivedByRemovalFrom");
		DESCRIBES_SERVICE = factory.createIRI(NAMESPACE, "describesService");
		DICTIONARY_PROP = factory.createIRI(NAMESPACE, "dictionary");
		ENDED_AT_TIME = factory.createIRI(NAMESPACE, "endedAtTime");
		ENTITY_PROP = factory.createIRI(NAMESPACE, "entity");
		GENERATED = factory.createIRI(NAMESPACE, "generated");
		GENERATED_AT_TIME = factory.createIRI(NAMESPACE, "generatedAtTime");
		HAD_ACTIVITY = factory.createIRI(NAMESPACE, "hadActivity");
		HAD_DICTIONARY_MEMBER = factory.createIRI(NAMESPACE, "hadDictionaryMember");
		HAD_GENERATION = factory.createIRI(NAMESPACE, "hadGeneration");
		HAD_MEMBER = factory.createIRI(NAMESPACE, "hadMember");
		HAD_PLAN = factory.createIRI(NAMESPACE, "hadPlan");
		HAD_PRIMARY_SOURCE = factory.createIRI(NAMESPACE, "hadPrimarySource");
		HAD_ROLE = factory.createIRI(NAMESPACE, "hadRole");
		HAD_USAGE = factory.createIRI(NAMESPACE, "hadUsage");
		HAS_ANCHOR = factory.createIRI(NAMESPACE, "has_anchor");
		HAS_PROVENANCE = factory.createIRI(NAMESPACE, "has_provenance");
		HAS_QUERY_SERVICE = factory.createIRI(NAMESPACE, "has_query_service");
		INFLUENCED = factory.createIRI(NAMESPACE, "influenced");
		INFLUENCER = factory.createIRI(NAMESPACE, "influencer");
		INSERTED_KEY_ENTITY_PAIR = factory.createIRI(NAMESPACE, "insertedKeyEntityPair");
		INVALIDATED = factory.createIRI(NAMESPACE, "invalidated");
		INVALIDATED_AT_TIME = factory.createIRI(NAMESPACE, "invalidatedAtTime");
		MENTION_OF = factory.createIRI(NAMESPACE, "mentionOf");
		PAIR_ENTITY = factory.createIRI(NAMESPACE, "pairEntity");
		PAIR_KEY = factory.createIRI(NAMESPACE, "pairKey");
		PINGBACK = factory.createIRI(NAMESPACE, "pingback");
		PROVENANCE_URI_TEMPLATE = factory.createIRI(NAMESPACE, "provenanceUriTemplate");
		QUALIFIED_ASSOCIATION = factory.createIRI(NAMESPACE, "qualifiedAssociation");
		QUALIFIED_ATTRIBUTION = factory.createIRI(NAMESPACE, "qualifiedAttribution");
		QUALIFIED_COMMUNICATION = factory.createIRI(NAMESPACE, "qualifiedCommunication");
		QUALIFIED_DELEGATION = factory.createIRI(NAMESPACE, "qualifiedDelegation");
		QUALIFIED_DERIVATION = factory.createIRI(NAMESPACE, "qualifiedDerivation");
		QUALIFIED_END = factory.createIRI(NAMESPACE, "qualifiedEnd");
		QUALIFIED_GENERATION = factory.createIRI(NAMESPACE, "qualifiedGeneration");
		QUALIFIED_INFLUENCE = factory.createIRI(NAMESPACE, "qualifiedInfluence");
		QUALIFIED_INSERTION = factory.createIRI(NAMESPACE, "qualifiedInsertion");
		QUALIFIED_INVALIDATION = factory.createIRI(NAMESPACE, "qualifiedInvalidation");
		QUALIFIED_PRIMARY_SOURCE = factory.createIRI(NAMESPACE, "qualifiedPrimarySource");
		QUALIFIED_QUOTATION = factory.createIRI(NAMESPACE, "qualifiedQuotation");
		QUALIFIED_REMOVAL = factory.createIRI(NAMESPACE, "qualifiedRemoval");
		QUALIFIED_REVISION = factory.createIRI(NAMESPACE, "qualifiedRevision");
		QUALIFIED_START = factory.createIRI(NAMESPACE, "qualifiedStart");
		QUALIFIED_USAGE = factory.createIRI(NAMESPACE, "qualifiedUsage");
		REMOVED_KEY = factory.createIRI(NAMESPACE, "removedKey");
		SPECIALIZATION_OF = factory.createIRI(NAMESPACE, "specializationOf");
		STARTED_AT_TIME = factory.createIRI(NAMESPACE, "startedAtTime");
		USED = factory.createIRI(NAMESPACE, "used");
		VALUE = factory.createIRI(NAMESPACE, "value");
		WAS_ASSOCIATED_WITH = factory.createIRI(NAMESPACE, "wasAssociatedWith");
		WAS_ATTRIBUTED_TO = factory.createIRI(NAMESPACE, "wasAttributedTo");
		WAS_DERIVED_FROM = factory.createIRI(NAMESPACE, "wasDerivedFrom");
		WAS_ENDED_BY = factory.createIRI(NAMESPACE, "wasEndedBy");
		WAS_GENERATED_BY = factory.createIRI(NAMESPACE, "wasGeneratedBy");
		WAS_INFLUENCED_BY = factory.createIRI(NAMESPACE, "wasInfluencedBy");
		WAS_INFORMED_BY = factory.createIRI(NAMESPACE, "wasInformedBy");
		WAS_INVALIDATED_BY = factory.createIRI(NAMESPACE, "wasInvalidatedBy");
		WAS_QUOTED_FROM = factory.createIRI(NAMESPACE, "wasQuotedFrom");
		WAS_REVISION_OF = factory.createIRI(NAMESPACE, "wasRevisionOf");
		WAS_STARTED_BY = factory.createIRI(NAMESPACE, "wasStartedBy");
	}
}
