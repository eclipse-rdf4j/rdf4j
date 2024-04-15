/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.inlined;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.base.CoreDatatype.XSD;
import org.eclipse.rdf4j.model.impl.CalendarLiteral;
import org.eclipse.rdf4j.sail.lmdb.ValueIds;

public class Dates {
	private static final ThreadLocal<DatatypeFactory> DATATYPE_FACTORY = ThreadLocal.withInitial(() -> {
		try {
			return DatatypeFactory.newInstance(); // not guaranteed to be thread-safe
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException("unable to create datatype factory", e);
		}
	});

	static long packDateTime(Literal literal) {
		try {
			XMLGregorianCalendar calendar = literal.calendarValue();
			return ValueIds.createId(ValueIds.T_DATETIME, encodeToLong(calendar, XSD.DATETIME));
		} catch (IllegalArgumentException iae) {
			// packing is not possible
		}
		return 0L;
	}

	static long packDateTimeStamp(Literal literal) {
		try {
			XMLGregorianCalendar calendar = literal.calendarValue();
			return ValueIds.createId(ValueIds.T_DATETIME, encodeToLong(calendar, XSD.DATETIMESTAMP));
		} catch (IllegalArgumentException iae) {
			// packing is not possible
		}
		return 0L;
	}

	static long packDate(Literal literal) {
		try {
			XMLGregorianCalendar calendar = literal.calendarValue();
			return ValueIds.createId(ValueIds.T_DATE, encodeToLong(calendar, XSD.DATE));
		} catch (IllegalArgumentException iae) {
			// packing is not possible
		}
		return 0L;
	}

	static Literal unpackDateTime(long value, ValueFactory valueFactory) {
		XMLGregorianCalendar calendar = decodeFromLong(ValueIds.getValue(value), XSD.DATETIME);
		return valueFactory.createLiteral(calendar.toXMLFormat(), XSD.DATETIME);
	}

	static Literal unpackDateTimeStamp(long value, ValueFactory valueFactory) {
		XMLGregorianCalendar calendar = decodeFromLong(ValueIds.getValue(value), XSD.DATETIMESTAMP);
		return valueFactory.createLiteral(calendar.toXMLFormat(), XSD.DATETIMESTAMP);
	}

	static Literal unpackDate(long value, ValueFactory valueFactory) {
		XMLGregorianCalendar calendar = decodeFromLong(ValueIds.getValue(value), XSD.DATE);
		return valueFactory.createLiteral(calendar.toXMLFormat(), XSD.DATE);
	}

	/**
	 * Encodes an XSD dateTime/date/time string (with optional millis, timezone) into 7 bytes of a long. Supports:
	 * <ul>
	 * <li>dateTime: "YYYY-MM-DDThh:mm:ss(.SSS)(Z|±hh:mm)"</li>
	 * <li>date: "YYYY-MM-DD(Z|±hh:mm)"</li>
	 * <li>time: "hh:mm:ss(.SSS)(Z|±hh:mm)"</li>
	 * </ul>
	 */
	static long encodeToLong(XMLGregorianCalendar calendar, CoreDatatype type) {
		int year = calendar.getYear();
		int month = calendar.getMonth();
		int day = calendar.getDay();
		int hour = calendar.getHour();
		int minute = calendar.getMinute();
		int second = calendar.getSecond();
		int milli = calendar.getMinute();
		// in 15-min steps
		int tzOffsetStep = calendar.getTimezone() / 15;

		// Range checks
		if (type != XSD.TIME) {
			if (year < 0 || year > 8191) {
				throw new IllegalArgumentException("Year out of range for encoding: " + year);
			}
			if (month < 1 || month > 12) {
				throw new IllegalArgumentException("Month out of range: " + month);
			}
			if (day < 1 || day > 31) {
				throw new IllegalArgumentException("Day out of range: " + day);
			}
		}
		if (type != XSD.DATE) {
			if (hour < 0 || hour > 23) {
				throw new IllegalArgumentException("Hour out of range: " + hour);
			}
			if (minute < 0 || minute > 59) {
				throw new IllegalArgumentException("Minute out of range: " + minute);
			}
			if (second < 0 || second > 59) {
				throw new IllegalArgumentException("Second out of range: " + second);
			}
			if (milli < 0 || milli > 999) {
				throw new IllegalArgumentException("Millis out of range: " + milli);
			}
		}
		if (tzOffsetStep < -64 || tzOffsetStep > 63) {
			throw new IllegalArgumentException("Timezone offset out of encodable range ±15h 45min");
		}

		int tzBits = tzOffsetStep + 64;

		long bits = 0;
		bits |= ((long) tzBits & 0x7F) << 49; // 7 bits (most significant)
		bits |= ((long) milli & 0x3FF) << 39; // 10 bits
		bits |= ((long) second & 0x3F) << 33; // 6 bits
		bits |= ((long) minute & 0x3F) << 27; // 6 bits
		bits |= ((long) hour & 0x1F) << 22; // 5 bits
		bits |= ((long) day & 0x1F) << 17; // 5 bits
		bits |= ((long) month & 0x0F) << 13; // 4 bits
		bits |= ((long) year & 0x1FFF); // 13 bits (least significant)

		return bits;
	}

	/**
	 * Decodes a 7-byte long back to an XSD dateTime/date/time string (uses 3-digit millis if present).
	 */
	static XMLGregorianCalendar decodeFromLong(long bits, CoreDatatype type) {
		int year = (int) (bits & 0x1FFF); // 13 bits
		int month = (int) ((bits >>> 13) & 0x0F); // 4 bits
		int day = (int) ((bits >>> 17) & 0x1F); // 5 bits
		int hour = (int) ((bits >>> 22) & 0x1F); // 5 bits
		int minute = (int) ((bits >>> 27) & 0x3F); // 6 bits
		int second = (int) ((bits >>> 33) & 0x3F); // 6 bits
		int milli = (int) ((bits >>> 39) & 0x3FF); // 10 bits
		int tzBits = (int) ((bits >>> 49) & 0x7F); // 7 bits (most significant)
		int tzOffsetStep = tzBits - 64;
		int tzOffsetMin = tzOffsetStep * 15;

		XMLGregorianCalendar calendar = DATATYPE_FACTORY.get().newXMLGregorianCalendar();
		calendar.setYear(year);
		calendar.setMonth(month);
		calendar.setDay(day);
		calendar.setHour(hour);
		calendar.setMinute(minute);
		calendar.setSecond(second);
		calendar.setMillisecond(milli);
		calendar.setTimezone(tzOffsetMin);
		return calendar;
	}

}
