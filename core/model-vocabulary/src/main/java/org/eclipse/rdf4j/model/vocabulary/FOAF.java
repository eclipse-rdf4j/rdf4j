/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
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
 * Constants for FOAF primitives and for the FOAF namespace.<br>
 * Resources here are defined according to the FOAF specs on
 * <a href="http://xmlns.com/foaf/spec/">http://xmlns.com/foaf/spec/</a>, version 0.99, 14 January 2014
 */
public class FOAF {

	/**
	 * The FOAF namespace: http://xmlns.com/foaf/0.1/
	 */
	public static final String NAMESPACE = "http://xmlns.com/foaf/0.1/";

	/**
	 * The recommended prefix for the FOAF namespace: "foaf"
	 */
	public static final String PREFIX = "foaf";

	/**
	 * An immutable {@link Namespace} constant that represents the FOAF namespace.
	 */
	public static final Namespace NS = createNamespace(PREFIX, NAMESPACE);

	// ----- Classes ------
	public final static IRI AGENT;

	public final static IRI DOCUMENT;

	public final static IRI GROUP;

	public final static IRI IMAGE;

	public final static IRI LABEL_PROPERTY;

	public final static IRI ONLINE_ACCOUNT;

	public final static IRI ONLINE_CHAT_ACCOUNT;

	public final static IRI ONLINE_ECOMMERCE_ACCOUNT;

	public final static IRI ONLINE_GAMING_ACCOUNT;

	public final static IRI ORGANIZATION;

	public final static IRI PERSON;

	public final static IRI PERSONAL_PROFILE_DOCUMENT;

	public final static IRI PROJECT;

	// ----- Properties ------
	public final static IRI ACCOUNT;

	public final static IRI ACCOUNT_NAME;

	public final static IRI ACCOUNT_SERVICE_HOMEPAGE;

	public final static IRI AGE;

	public final static IRI AIM_CHAT_ID;

	public final static IRI BASED_NEAR;

	public final static IRI BIRTHDAY;

	public final static IRI CURRENT_PROJECT;

	public final static IRI DEPICTION;

	public final static IRI DEPICTS;

	public final static IRI DNA_CHECKSUM;

	public final static IRI FAMILY_NAME;

	/** @deprecated Use FAMILY_NAME instead for new statements */
	@Deprecated
	public final static IRI FAMILYNAME;

	public final static IRI FIRST_NAME;

	public final static IRI FOCUS;

	public final static IRI FUNDED_BY;

	public final static IRI GEEKCODE;

	public final static IRI GENDER;

	public final static IRI GIVEN_NAME;

	/** @deprecated Use GIVEN_NAME instead for new statements */
	@Deprecated
	public final static IRI GIVENNAME;

	public final static IRI HOLDS_ACCOUNT;

	public final static IRI HOMEPAGE;

	public final static IRI ICQ_CHAT_ID;

	public final static IRI IMG;

	public final static IRI INTEREST;

	public final static IRI IS_PRIMARY_TOPIC_OF;

	public final static IRI JABBER_ID;

	public final static IRI KNOWS;

	public final static IRI LAST_NAME;

	public final static IRI LOGO;

	public final static IRI MADE;

	public final static IRI MAKER;

	public static final IRI MBOX;

	public static final IRI MBOX_SHA1SUM;

	public static final IRI MEMBER;

	public static final IRI MEMBERSHIP_CLASS;

	public static final IRI MSN_CHAT_ID;

	public static final IRI MYERS_BRIGGS;

	public final static IRI NAME;

	public final static IRI NICK;

	public final static IRI OPENID;

	public final static IRI PAGE;

	public final static IRI PAST_PROJECT;

	public final static IRI PHONE;

	public final static IRI PLAN;

	public final static IRI PRIMARY_TOPIC;

	public final static IRI PUBLICATIONS;

	public final static IRI SCHOOL_HOMEPAGE;

	public final static IRI SHA1;

	public final static IRI SKYPE_ID;

	public final static IRI STATUS;

	public final static IRI SURNAME;

	public final static IRI THEME;

	public final static IRI THUMBNAIL;

	public final static IRI TIPJAR;

	public final static IRI TITLE;

	public final static IRI TOPIC;

	public final static IRI TOPIC_INTEREST;

	public final static IRI WEBLOG;

	public final static IRI WORK_INFO_HOMEPAGE;

	public final static IRI WORKPLACE_HOMEPAGE;

	public final static IRI YAHOO_CHAT_ID;

	static {

		// ----- Classes ------
		AGENT = createIRI(FOAF.NAMESPACE, "Agent");
		DOCUMENT = createIRI(FOAF.NAMESPACE, "Document");
		GROUP = createIRI(FOAF.NAMESPACE, "Group");
		IMAGE = createIRI(FOAF.NAMESPACE, "Image");
		LABEL_PROPERTY = createIRI(FOAF.NAMESPACE, "LabelProperty");
		ONLINE_ACCOUNT = createIRI(FOAF.NAMESPACE, "OnlineAccount");
		ONLINE_CHAT_ACCOUNT = createIRI(FOAF.NAMESPACE, "OnlineChatAccount");
		ONLINE_ECOMMERCE_ACCOUNT = createIRI(FOAF.NAMESPACE, "OnlineEcommerceAccount");
		ONLINE_GAMING_ACCOUNT = createIRI(FOAF.NAMESPACE, "OnlineGamingAccount");
		ORGANIZATION = createIRI(FOAF.NAMESPACE, "Organization");
		PERSON = createIRI(FOAF.NAMESPACE, "Person");
		PERSONAL_PROFILE_DOCUMENT = createIRI(FOAF.NAMESPACE, "PersonalProfileDocument");
		PROJECT = createIRI(FOAF.NAMESPACE, "Project");

		// ----- Properties ------
		ACCOUNT = createIRI(FOAF.NAMESPACE, "account");
		ACCOUNT_NAME = createIRI(FOAF.NAMESPACE, "accountName");
		ACCOUNT_SERVICE_HOMEPAGE = createIRI(FOAF.NAMESPACE, "accountServiceHomepage");
		AGE = createIRI(FOAF.NAMESPACE, "age");
		AIM_CHAT_ID = createIRI(FOAF.NAMESPACE, "aimChatID");
		BASED_NEAR = createIRI(FOAF.NAMESPACE, "based_near");
		BIRTHDAY = createIRI(FOAF.NAMESPACE, "birthday");
		CURRENT_PROJECT = createIRI(FOAF.NAMESPACE, "currentProject");
		DEPICTION = createIRI(FOAF.NAMESPACE, "depiction");
		DEPICTS = createIRI(FOAF.NAMESPACE, "depicts");
		DNA_CHECKSUM = createIRI(FOAF.NAMESPACE, "dnaChecksum");
		FAMILY_NAME = createIRI(FOAF.NAMESPACE, "familyName");
		FAMILYNAME = createIRI(FOAF.NAMESPACE, "family_name");
		FIRST_NAME = createIRI(FOAF.NAMESPACE, "firstName");
		FOCUS = createIRI(FOAF.NAMESPACE, "focus");
		FUNDED_BY = createIRI(FOAF.NAMESPACE, "fundedBy");
		GEEKCODE = createIRI(FOAF.NAMESPACE, "geekcode");
		GENDER = createIRI(FOAF.NAMESPACE, "gender");
		GIVEN_NAME = createIRI(FOAF.NAMESPACE, "givenName");
		GIVENNAME = createIRI(FOAF.NAMESPACE, "givenname");
		HOLDS_ACCOUNT = createIRI(FOAF.NAMESPACE, "holdsAccount");
		HOMEPAGE = createIRI(FOAF.NAMESPACE, "homepage");
		ICQ_CHAT_ID = createIRI(FOAF.NAMESPACE, "icqChatID");
		IMG = createIRI(FOAF.NAMESPACE, "img");
		INTEREST = createIRI(FOAF.NAMESPACE, "interest");
		IS_PRIMARY_TOPIC_OF = createIRI(FOAF.NAMESPACE, "isPrimaryTopicOf");
		JABBER_ID = createIRI(FOAF.NAMESPACE, "jabberID");
		KNOWS = createIRI(FOAF.NAMESPACE, "knows");
		LAST_NAME = createIRI(FOAF.NAMESPACE, "lastName");
		LOGO = createIRI(FOAF.NAMESPACE, "logo");
		MADE = createIRI(FOAF.NAMESPACE, "made");
		MAKER = createIRI(FOAF.NAMESPACE, "maker");
		MBOX = createIRI(FOAF.NAMESPACE, "mbox");
		MBOX_SHA1SUM = createIRI(FOAF.NAMESPACE, "mbox_sha1sum");
		MEMBER = createIRI(FOAF.NAMESPACE, "member");
		MEMBERSHIP_CLASS = createIRI(FOAF.NAMESPACE, "membershipClass");
		MSN_CHAT_ID = createIRI(FOAF.NAMESPACE, "msnChatID");
		MYERS_BRIGGS = createIRI(FOAF.NAMESPACE, "myersBriggs");
		NAME = createIRI(FOAF.NAMESPACE, "name");
		NICK = createIRI(FOAF.NAMESPACE, "nick");
		OPENID = createIRI(FOAF.NAMESPACE, "openid");
		PAGE = createIRI(FOAF.NAMESPACE, "page");
		PAST_PROJECT = createIRI(FOAF.NAMESPACE, "pastProject");
		PHONE = createIRI(FOAF.NAMESPACE, "phone");
		PLAN = createIRI(FOAF.NAMESPACE, "plan");
		PRIMARY_TOPIC = createIRI(FOAF.NAMESPACE, "primaryTopic");
		PUBLICATIONS = createIRI(FOAF.NAMESPACE, "publications");
		SCHOOL_HOMEPAGE = createIRI(FOAF.NAMESPACE, "schoolHomepage");
		SHA1 = createIRI(FOAF.NAMESPACE, "sha1");
		SKYPE_ID = createIRI(FOAF.NAMESPACE, "skypeID");
		STATUS = createIRI(FOAF.NAMESPACE, "status");
		SURNAME = createIRI(FOAF.NAMESPACE, "surname");
		THEME = createIRI(FOAF.NAMESPACE, "theme");
		THUMBNAIL = createIRI(FOAF.NAMESPACE, "thumbnail");
		TIPJAR = createIRI(FOAF.NAMESPACE, "tipjar");
		TITLE = createIRI(FOAF.NAMESPACE, "title");
		TOPIC = createIRI(FOAF.NAMESPACE, "topic");
		TOPIC_INTEREST = createIRI(FOAF.NAMESPACE, "topic_interest");
		WEBLOG = createIRI(FOAF.NAMESPACE, "weblog");
		WORK_INFO_HOMEPAGE = createIRI(FOAF.NAMESPACE, "workInfoHomepage");
		WORKPLACE_HOMEPAGE = createIRI(FOAF.NAMESPACE, "workplaceHomepage");
		YAHOO_CHAT_ID = createIRI(FOAF.NAMESPACE, "yahooChatID");
	}
}
