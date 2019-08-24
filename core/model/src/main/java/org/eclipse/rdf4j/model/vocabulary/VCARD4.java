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
 * Constants for the vCard Ontology.
 *
 * @see <a href="https://www.w3.org/TR/2014/NOTE-vcard-rdf-20140522/">vCard Ontology</a>
 * @see <a href="https://www.w3.org/2006/vcard/ns.ttl">vCard OWL file</a>
 *
 * @author Bart Hanssens
 */
public class VCARD4 {
	/**
	 * The VCARD4 namespace: http://www.w3.org/2006/vcard/ns#
	 */
	public static final String NAMESPACE = "http://www.w3.org/2006/vcard/ns#";

	/**
	 * Recommended prefix for the namespace: "vcard"
	 */
	public static final String PREFIX = "vcard";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	// Classes
	/** vcard:Acquaintance */
	public static final IRI ACQUAINTANCE;

	/** vcard:Address */
	public static final IRI ADDRESS;

	/** vcard:Agent */
	public static final IRI AGENT;

	/** vcard:BBS */
	@Deprecated
	public static final IRI BBS;

	/** vcard:Car */
	@Deprecated
	public static final IRI CAR;

	/** vcard:Cell */
	public static final IRI CELL;

	/** vcard:Child */
	public static final IRI CHILD;

	/** vcard:Colleague */
	public static final IRI COLLEAGUE;

	/** vcard:Contact */
	public static final IRI CONTACT;

	/** vcard:Coresident */
	public static final IRI CORESIDENT;

	/** vcard:Coworker */
	public static final IRI COWORKER;

	/** vcard:Crush */
	public static final IRI CRUSH;

	/** vcard:Date */
	public static final IRI DATE;

	/** vcard:Dom */
	@Deprecated
	public static final IRI DOM;

	/** vcard:Email */
	@Deprecated
	public static final IRI EMAIL;

	/** vcard:Emergency */
	public static final IRI EMERGENCY;

	/** vcard:Fax */
	public static final IRI FAX;

	/** vcard:Female */
	public static final IRI FEMALE;

	/** vcard:Friend */
	public static final IRI FRIEND;

	/** vcard:Gender */
	public static final IRI GENDER;

	/** vcard:Group */
	public static final IRI GROUP;

	/** vcard:Home */
	public static final IRI HOME;

	/** vcard:ISDN */
	@Deprecated
	public static final IRI ISDN;

	/** vcard:Individual */
	public static final IRI INDIVIDUAL;

	/** vcard:Internet */
	@Deprecated
	public static final IRI INTERNET;

	/** vcard:Intl */
	@Deprecated
	public static final IRI INTL;

	/** vcard:Kin */
	public static final IRI KIN;

	/** vcard:Kind */
	public static final IRI KIND;

	/** vcard:Label */
	@Deprecated
	public static final IRI LABEL;

	/** vcard:Location */
	public static final IRI LOCATION;

	/** vcard:Male */
	public static final IRI MALE;

	/** vcard:Me */
	public static final IRI ME;

	/** vcard:Met */
	public static final IRI MET;

	/** vcard:Modem */
	@Deprecated
	public static final IRI MODEM;

	/** vcard:Msg */
	@Deprecated
	public static final IRI MSG;

	/** vcard:Muse */
	public static final IRI MUSE;

	/** vcard:Name */
	public static final IRI NAME;

	/** vcard:Neighbor */
	public static final IRI NEIGHBOR;

	/** vcard:None */
	public static final IRI NONE;

	/** vcard:Organization */
	public static final IRI ORGANIZATION;

	/** vcard:Other */
	public static final IRI OTHER;

	/** vcard:PCS */
	@Deprecated
	public static final IRI PCS;

	/** vcard:Pager */
	public static final IRI PAGER;

	/** vcard:Parcel */
	@Deprecated
	public static final IRI PARCEL;

	/** vcard:Parent */
	public static final IRI PARENT;

	/** vcard:Postal */
	@Deprecated
	public static final IRI POSTAL;

	/** vcard:Pref */
	@Deprecated
	public static final IRI PREF;

	/** vcard:RelatedType */
	public static final IRI RELATED_TYPE;

	/** vcard:Sibling */
	public static final IRI SIBLING;

	/** vcard:Spouse */
	public static final IRI SPOUSE;

	/** vcard:Sweetheart */
	public static final IRI SWEETHEART;

	/** vcard:Tel */
	@Deprecated
	public static final IRI TEL;

	/** vcard:TelephoneType */
	public static final IRI TELEPHONE_TYPE;

	/** vcard:Text */
	public static final IRI TEXT;

	/** vcard:TextPhone */
	public static final IRI TEXT_PHONE;

	/** vcard:Type */
	public static final IRI TYPE;

	/** vcard:Unknown */
	public static final IRI UNKNOWN;

	/** vcard:VCard */
	public static final IRI VCARD;

	/** vcard:Video */
	public static final IRI VIDEO;

	/** vcard:Voice */
	public static final IRI VOICE;

	/** vcard:Work */
	public static final IRI WORK;

	/** vcard:X400 */
	@Deprecated
	public static final IRI X400;

	// Properties
	/** vcard:additional-name */
	public static final IRI ADDITIONAL_NAME;

	/** vcard:adr */
	public static final IRI ADR;

	/** vcard:agent */
	@Deprecated
	public static final IRI AGENT_PROP;

	/** vcard:anniversary */
	public static final IRI ANNIVERSARY;

	/** vcard:bday */
	public static final IRI BDAY;

	/** vcard:category */
	public static final IRI CATEGORY;

	/** vcard:class */
	@Deprecated
	public static final IRI CLASS;

	/** vcard:country-name */
	public static final IRI COUNTRY_NAME;

	/** vcard:email */
	public static final IRI EMAIL_PROP;

	/** vcard:extended-address */
	@Deprecated
	public static final IRI EXTENDED_ADDRESS;

	/** vcard:family-name */
	public static final IRI FAMILY_NAME;

	/** vcard:fn */
	public static final IRI FN;

	/** vcard:geo */
	public static final IRI GEO;

	/** vcard:given-name */
	public static final IRI GIVEN_NAME;

	/** vcard:hasAdditionalName */
	public static final IRI HAS_ADDITIONAL_NAME;

	/** vcard:hasAddress */
	public static final IRI HAS_ADDRESS;

	/** vcard:hasCalendarBusy */
	public static final IRI HAS_CALENDAR_BUSY;

	/** vcard:hasCalendarLink */
	public static final IRI HAS_CALENDAR_LINK;

	/** vcard:hasCalendarRequest */
	public static final IRI HAS_CALENDAR_REQUEST;

	/** vcard:hasCategory */
	public static final IRI HAS_CATEGORY;

	/** vcard:hasCountryName */
	public static final IRI HAS_COUNTRY_NAME;

	/** vcard:hasEmail */
	public static final IRI HAS_EMAIL;

	/** vcard:hasFN */
	public static final IRI HAS_FN;

	/** vcard:hasFamilyName */
	public static final IRI HAS_FAMILY_NAME;

	/** vcard:hasGender */
	public static final IRI HAS_GENDER;

	/** vcard:hasGeo */
	public static final IRI HAS_GEO;

	/** vcard:hasGivenName */
	public static final IRI HAS_GIVEN_NAME;

	/** vcard:hasHonorificPrefix */
	public static final IRI HAS_HONORIFIC_PREFIX;

	/** vcard:hasHonorificSuffix */
	public static final IRI HAS_HONORIFIC_SUFFIX;

	/** vcard:hasInstantMessage */
	public static final IRI HAS_INSTANT_MESSAGE;

	/** vcard:hasKey */
	public static final IRI HAS_KEY;

	/** vcard:hasLanguage */
	public static final IRI HAS_LANGUAGE;

	/** vcard:hasLocality */
	public static final IRI HAS_LOCALITY;

	/** vcard:hasLogo */
	public static final IRI HAS_LOGO;

	/** vcard:hasMember */
	public static final IRI HAS_MEMBER;

	/** vcard:hasName */
	public static final IRI HAS_NAME;

	/** vcard:hasNickname */
	public static final IRI HAS_NICKNAME;

	/** vcard:hasNote */
	public static final IRI HAS_NOTE;

	/** vcard:hasOrganizationName */
	public static final IRI HAS_ORGANIZATION_NAME;

	/** vcard:hasOrganizationUnit */
	public static final IRI HAS_ORGANIZATION_UNIT;

	/** vcard:hasPhoto */
	public static final IRI HAS_PHOTO;

	/** vcard:hasPostalCode */
	public static final IRI HAS_POSTAL_CODE;

	/** vcard:hasRegion */
	public static final IRI HAS_REGION;

	/** vcard:hasRelated */
	public static final IRI HAS_RELATED;

	/** vcard:hasRole */
	public static final IRI HAS_ROLE;

	/** vcard:hasSound */
	public static final IRI HAS_SOUND;

	/** vcard:hasSource */
	public static final IRI HAS_SOURCE;

	/** vcard:hasStreetAddress */
	public static final IRI HAS_STREET_ADDRESS;

	/** vcard:hasTelephone */
	public static final IRI HAS_TELEPHONE;

	/** vcard:hasTitle */
	public static final IRI HAS_TITLE;

	/** vcard:hasUID */
	public static final IRI HAS_UID;

	/** vcard:hasURL */
	public static final IRI HAS_URL;

	/** vcard:hasValue */
	public static final IRI HAS_VALUE;

	/** vcard:honorific-prefix */
	public static final IRI HONORIFIC_PREFIX;

	/** vcard:honorific-suffix */
	public static final IRI HONORIFIC_SUFFIX;

	/** vcard:key */
	public static final IRI KEY;

	/** vcard:label */
	@Deprecated
	public static final IRI LABEL_PROP;

	/** vcard:language */
	public static final IRI LANGUAGE;

	/** vcard:latitude */
	@Deprecated
	public static final IRI LATITUDE;

	/** vcard:locality */
	public static final IRI LOCALITY;

	/** vcard:logo */
	public static final IRI LOGO;

	/** vcard:longitude */
	@Deprecated
	public static final IRI LONGITUDE;

	/** vcard:mailer */
	@Deprecated
	public static final IRI MAILER;

	/** vcard:n */
	public static final IRI N;

	/** vcard:nickname */
	public static final IRI NICKNAME;

	/** vcard:note */
	public static final IRI NOTE;

	/** vcard:org */
	public static final IRI ORG;

	/** vcard:organization-name */
	public static final IRI ORGANIZATION_NAME;

	/** vcard:organization-unit */
	public static final IRI ORGANIZATION_UNIT;

	/** vcard:photo */
	public static final IRI PHOTO;

	/** vcard:post-office-box */
	@Deprecated
	public static final IRI POST_OFFICE_BOX;

	/** vcard:postal-code */
	public static final IRI POSTAL_CODE;

	/** vcard:prodid */
	public static final IRI PRODID;

	/** vcard:region */
	public static final IRI REGION;

	/** vcard:rev */
	public static final IRI REV;

	/** vcard:role */
	public static final IRI ROLE;

	/** vcard:sort-string */
	public static final IRI SORT_STRING;

	/** vcard:sound */
	public static final IRI SOUND;

	/** vcard:street-address */
	public static final IRI STREET_ADDRESS;

	/** vcard:tel */
	public static final IRI TEL_PROP;

	/** vcard:title */
	public static final IRI TITLE;

	/** vcard:tz */
	public static final IRI TZ;

	/** vcard:url */
	public static final IRI URL;

	/** vcard:value */
	public static final IRI VALUE;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();

		ACQUAINTANCE = factory.createIRI(NAMESPACE, "Acquaintance");
		ADDRESS = factory.createIRI(NAMESPACE, "Address");
		AGENT = factory.createIRI(NAMESPACE, "Agent");
		BBS = factory.createIRI(NAMESPACE, "BBS");
		CAR = factory.createIRI(NAMESPACE, "Car");
		CELL = factory.createIRI(NAMESPACE, "Cell");
		CHILD = factory.createIRI(NAMESPACE, "Child");
		COLLEAGUE = factory.createIRI(NAMESPACE, "Colleague");
		CONTACT = factory.createIRI(NAMESPACE, "Contact");
		CORESIDENT = factory.createIRI(NAMESPACE, "Coresident");
		COWORKER = factory.createIRI(NAMESPACE, "Coworker");
		CRUSH = factory.createIRI(NAMESPACE, "Crush");
		DATE = factory.createIRI(NAMESPACE, "Date");
		DOM = factory.createIRI(NAMESPACE, "Dom");
		EMAIL = factory.createIRI(NAMESPACE, "Email");
		EMERGENCY = factory.createIRI(NAMESPACE, "Emergency");
		FAX = factory.createIRI(NAMESPACE, "Fax");
		FEMALE = factory.createIRI(NAMESPACE, "Female");
		FRIEND = factory.createIRI(NAMESPACE, "Friend");
		GENDER = factory.createIRI(NAMESPACE, "Gender");
		GROUP = factory.createIRI(NAMESPACE, "Group");
		HOME = factory.createIRI(NAMESPACE, "Home");
		ISDN = factory.createIRI(NAMESPACE, "ISDN");
		INDIVIDUAL = factory.createIRI(NAMESPACE, "Individual");
		INTERNET = factory.createIRI(NAMESPACE, "Internet");
		INTL = factory.createIRI(NAMESPACE, "Intl");
		KIN = factory.createIRI(NAMESPACE, "Kin");
		KIND = factory.createIRI(NAMESPACE, "Kind");
		LABEL = factory.createIRI(NAMESPACE, "Label");
		LOCATION = factory.createIRI(NAMESPACE, "Location");
		MALE = factory.createIRI(NAMESPACE, "Male");
		ME = factory.createIRI(NAMESPACE, "Me");
		MET = factory.createIRI(NAMESPACE, "Met");
		MODEM = factory.createIRI(NAMESPACE, "Modem");
		MSG = factory.createIRI(NAMESPACE, "Msg");
		MUSE = factory.createIRI(NAMESPACE, "Muse");
		NAME = factory.createIRI(NAMESPACE, "Name");
		NEIGHBOR = factory.createIRI(NAMESPACE, "Neighbor");
		NONE = factory.createIRI(NAMESPACE, "None");
		ORGANIZATION = factory.createIRI(NAMESPACE, "Organization");
		OTHER = factory.createIRI(NAMESPACE, "Other");
		PCS = factory.createIRI(NAMESPACE, "PCS");
		PAGER = factory.createIRI(NAMESPACE, "Pager");
		PARCEL = factory.createIRI(NAMESPACE, "Parcel");
		PARENT = factory.createIRI(NAMESPACE, "Parent");
		POSTAL = factory.createIRI(NAMESPACE, "Postal");
		PREF = factory.createIRI(NAMESPACE, "Pref");
		RELATED_TYPE = factory.createIRI(NAMESPACE, "RelatedType");
		SIBLING = factory.createIRI(NAMESPACE, "Sibling");
		SPOUSE = factory.createIRI(NAMESPACE, "Spouse");
		SWEETHEART = factory.createIRI(NAMESPACE, "Sweetheart");
		TEL = factory.createIRI(NAMESPACE, "Tel");
		TELEPHONE_TYPE = factory.createIRI(NAMESPACE, "TelephoneType");
		TEXT = factory.createIRI(NAMESPACE, "Text");
		TEXT_PHONE = factory.createIRI(NAMESPACE, "TextPhone");
		TYPE = factory.createIRI(NAMESPACE, "Type");
		UNKNOWN = factory.createIRI(NAMESPACE, "Unknown");
		VCARD = factory.createIRI(NAMESPACE, "VCard");
		VIDEO = factory.createIRI(NAMESPACE, "Video");
		VOICE = factory.createIRI(NAMESPACE, "Voice");
		WORK = factory.createIRI(NAMESPACE, "Work");
		X400 = factory.createIRI(NAMESPACE, "X400");

		ADDITIONAL_NAME = factory.createIRI(NAMESPACE, "additional-name");
		ADR = factory.createIRI(NAMESPACE, "adr");
		AGENT_PROP = factory.createIRI(NAMESPACE, "agent");
		ANNIVERSARY = factory.createIRI(NAMESPACE, "anniversary");
		BDAY = factory.createIRI(NAMESPACE, "bday");
		CATEGORY = factory.createIRI(NAMESPACE, "category");
		CLASS = factory.createIRI(NAMESPACE, "class");
		COUNTRY_NAME = factory.createIRI(NAMESPACE, "country-name");
		EMAIL_PROP = factory.createIRI(NAMESPACE, "email");
		EXTENDED_ADDRESS = factory.createIRI(NAMESPACE, "extended-address");
		FAMILY_NAME = factory.createIRI(NAMESPACE, "family-name");
		FN = factory.createIRI(NAMESPACE, "fn");
		GEO = factory.createIRI(NAMESPACE, "geo");
		GIVEN_NAME = factory.createIRI(NAMESPACE, "given-name");
		HAS_ADDITIONAL_NAME = factory.createIRI(NAMESPACE, "hasAdditionalName");
		HAS_ADDRESS = factory.createIRI(NAMESPACE, "hasAddress");
		HAS_CALENDAR_BUSY = factory.createIRI(NAMESPACE, "hasCalendarBusy");
		HAS_CALENDAR_LINK = factory.createIRI(NAMESPACE, "hasCalendarLink");
		HAS_CALENDAR_REQUEST = factory.createIRI(NAMESPACE, "hasCalendarRequest");
		HAS_CATEGORY = factory.createIRI(NAMESPACE, "hasCategory");
		HAS_COUNTRY_NAME = factory.createIRI(NAMESPACE, "hasCountryName");
		HAS_EMAIL = factory.createIRI(NAMESPACE, "hasEmail");
		HAS_FN = factory.createIRI(NAMESPACE, "hasFN");
		HAS_FAMILY_NAME = factory.createIRI(NAMESPACE, "hasFamilyName");
		HAS_GENDER = factory.createIRI(NAMESPACE, "hasGender");
		HAS_GEO = factory.createIRI(NAMESPACE, "hasGeo");
		HAS_GIVEN_NAME = factory.createIRI(NAMESPACE, "hasGivenName");
		HAS_HONORIFIC_PREFIX = factory.createIRI(NAMESPACE, "hasHonorificPrefix");
		HAS_HONORIFIC_SUFFIX = factory.createIRI(NAMESPACE, "hasHonorificSuffix");
		HAS_INSTANT_MESSAGE = factory.createIRI(NAMESPACE, "hasInstantMessage");
		HAS_KEY = factory.createIRI(NAMESPACE, "hasKey");
		HAS_LANGUAGE = factory.createIRI(NAMESPACE, "hasLanguage");
		HAS_LOCALITY = factory.createIRI(NAMESPACE, "hasLocality");
		HAS_LOGO = factory.createIRI(NAMESPACE, "hasLogo");
		HAS_MEMBER = factory.createIRI(NAMESPACE, "hasMember");
		HAS_NAME = factory.createIRI(NAMESPACE, "hasName");
		HAS_NICKNAME = factory.createIRI(NAMESPACE, "hasNickname");
		HAS_NOTE = factory.createIRI(NAMESPACE, "hasNote");
		HAS_ORGANIZATION_NAME = factory.createIRI(NAMESPACE, "hasOrganizationName");
		HAS_ORGANIZATION_UNIT = factory.createIRI(NAMESPACE, "hasOrganizationUnit");
		HAS_PHOTO = factory.createIRI(NAMESPACE, "hasPhoto");
		HAS_POSTAL_CODE = factory.createIRI(NAMESPACE, "hasPostalCode");
		HAS_REGION = factory.createIRI(NAMESPACE, "hasRegion");
		HAS_RELATED = factory.createIRI(NAMESPACE, "hasRelated");
		HAS_ROLE = factory.createIRI(NAMESPACE, "hasRole");
		HAS_SOUND = factory.createIRI(NAMESPACE, "hasSound");
		HAS_SOURCE = factory.createIRI(NAMESPACE, "hasSource");
		HAS_STREET_ADDRESS = factory.createIRI(NAMESPACE, "hasStreetAddress");
		HAS_TELEPHONE = factory.createIRI(NAMESPACE, "hasTelephone");
		HAS_TITLE = factory.createIRI(NAMESPACE, "hasTitle");
		HAS_UID = factory.createIRI(NAMESPACE, "hasUID");
		HAS_URL = factory.createIRI(NAMESPACE, "hasURL");
		HAS_VALUE = factory.createIRI(NAMESPACE, "hasValue");
		HONORIFIC_PREFIX = factory.createIRI(NAMESPACE, "honorific-prefix");
		HONORIFIC_SUFFIX = factory.createIRI(NAMESPACE, "honorific-suffix");
		KEY = factory.createIRI(NAMESPACE, "key");
		LABEL_PROP = factory.createIRI(NAMESPACE, "label");
		LANGUAGE = factory.createIRI(NAMESPACE, "language");
		LATITUDE = factory.createIRI(NAMESPACE, "latitude");
		LOCALITY = factory.createIRI(NAMESPACE, "locality");
		LOGO = factory.createIRI(NAMESPACE, "logo");
		LONGITUDE = factory.createIRI(NAMESPACE, "longitude");
		MAILER = factory.createIRI(NAMESPACE, "mailer");
		N = factory.createIRI(NAMESPACE, "n");
		NICKNAME = factory.createIRI(NAMESPACE, "nickname");
		NOTE = factory.createIRI(NAMESPACE, "note");
		ORG = factory.createIRI(NAMESPACE, "org");
		ORGANIZATION_NAME = factory.createIRI(NAMESPACE, "organization-name");
		ORGANIZATION_UNIT = factory.createIRI(NAMESPACE, "organization-unit");
		PHOTO = factory.createIRI(NAMESPACE, "photo");
		POST_OFFICE_BOX = factory.createIRI(NAMESPACE, "post-office-box");
		POSTAL_CODE = factory.createIRI(NAMESPACE, "postal-code");
		PRODID = factory.createIRI(NAMESPACE, "prodid");
		REGION = factory.createIRI(NAMESPACE, "region");
		REV = factory.createIRI(NAMESPACE, "rev");
		ROLE = factory.createIRI(NAMESPACE, "role");
		SORT_STRING = factory.createIRI(NAMESPACE, "sort-string");
		SOUND = factory.createIRI(NAMESPACE, "sound");
		STREET_ADDRESS = factory.createIRI(NAMESPACE, "street-address");
		TEL_PROP = factory.createIRI(NAMESPACE, "tel");
		TITLE = factory.createIRI(NAMESPACE, "title");
		TZ = factory.createIRI(NAMESPACE, "tz");
		URL = factory.createIRI(NAMESPACE, "url");
		VALUE = factory.createIRI(NAMESPACE, "value");
	}
}
