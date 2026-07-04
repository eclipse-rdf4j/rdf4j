/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;

/**
 * An extension of {@link SimpleLiteral} that stores a calendar value to avoid parsing.
 *
 * @author David Huynh
 * @author Arjohn Kampman
 */
public class CalendarLiteral extends SimpleLiteral {

	private static final long serialVersionUID = -8959671333074894312L;

	private final XMLGregorianCalendar calendar;

	/**
	 * Creates a literal for the specified calendar using a datatype appropriate for the value indicated by
	 * {@link XMLGregorianCalendar#getXMLSchemaType()}.
	 */
	protected CalendarLiteral(XMLGregorianCalendar calendar) {
		super(calendar.toXMLFormat(), XMLDatatypeUtil.qnameToCoreDatatype(calendar.getXMLSchemaType()));
		this.calendar = calendar;
	}

	@Override
	public XMLGregorianCalendar calendarValue() {
		return calendar;
	}
}
