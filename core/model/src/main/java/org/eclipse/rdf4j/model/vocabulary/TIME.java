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
 * Constants for the Time Ontology in OWL.
 *
 * @see <a href="https://www.w3.org/TR/owl-time/">Time Ontology in OWL</a>
 *
 * @author Bart Hanssens
 */
public class TIME {
	/**
	 * The Time namespace: http://www.w3.org/2006/time#
	 */
	public static final String NAMESPACE = "http://www.w3.org/2006/time#";

	/**
	 * Recommended prefix for the namespace: "time"
	 */
	public static final String PREFIX = "time";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	// Classes
	/** time:DateTimeDescription */
	public static final IRI DATE_TIME_DESCRIPTION;

	/** time:DateTimeInterval */
	public static final IRI DATE_TIME_INTERVAL;

	/** time:DayOfWeek */
	public static final IRI DAY_OF_WEEK;

	/** time:Duration */
	public static final IRI DURATION;

	/** time:DurationDescription */
	public static final IRI DURATION_DESCRIPTION;

	/** time:GeneralDateTimeDescription */
	public static final IRI GENERAL_DATE_TIME_DESCRIPTION;

	/** time:GeneralDurationDescription */
	public static final IRI GENERAL_DURATION_DESCRIPTION;

	/** time:Instant */
	public static final IRI INSTANT;

	/** time:Interval */
	public static final IRI INTERVAL;

	/** time:January */
	@Deprecated
	public static final IRI JANUARY;

	/** time:MonthOfYear */
	public static final IRI MONTH_OF_YEAR;

	/** time:ProperInterval */
	public static final IRI PROPER_INTERVAL;

	/** time:TRS */
	public static final IRI TRS;

	/** time:TemporalDuration */
	public static final IRI TEMPORAL_DURATION;

	/** time:TemporalEntity */
	public static final IRI TEMPORAL_ENTITY;

	/** time:TemporalPosition */
	public static final IRI TEMPORAL_POSITION;

	/** time:TemporalUnit */
	public static final IRI TEMPORAL_UNIT;

	/** time:TimePosition */
	public static final IRI TIME_POSITION;

	/** time:TimeZone */
	public static final IRI TIME_ZONE;

	/** time:Year */
	@Deprecated
	public static final IRI YEAR;

	// Properties
	/** time:after */
	public static final IRI AFTER;

	/** time:before */
	public static final IRI BEFORE;

	/** time:day */
	public static final IRI DAY;

	/** time:dayOfWeek */
	public static final IRI DAY_OF_WEEK_PROP;

	/** time:dayOfYear */
	public static final IRI DAY_OF_YEAR;

	/** time:days */
	public static final IRI DAYS;

	/** time:hasBeginning */
	public static final IRI HAS_BEGINNING;

	/** time:hasDateTimeDescription */
	public static final IRI HAS_DATE_TIME_DESCRIPTION;

	/** time:hasDuration */
	public static final IRI HAS_DURATION;

	/** time:hasDurationDescription */
	public static final IRI HAS_DURATION_DESCRIPTION;

	/** time:hasEnd */
	public static final IRI HAS_END;

	/** time:hasTRS */
	public static final IRI HAS_TRS;

	/** time:hasTemporalDuration */
	public static final IRI HAS_TEMPORAL_DURATION;

	/** time:hasTime */
	public static final IRI HAS_TIME;

	/** time:hasXSDDuration */
	public static final IRI HAS_XSDDURATION;

	/** time:hour */
	public static final IRI HOUR;

	/** time:hours */
	public static final IRI HOURS;

	/** time:inDateTime */
	public static final IRI IN_DATE_TIME;

	/** time:inTemporalPosition */
	public static final IRI IN_TEMPORAL_POSITION;

	/** time:inTimePosition */
	public static final IRI IN_TIME_POSITION;

	/** time:inXSDDate */
	public static final IRI IN_XSDDATE;

	/** time:inXSDDateTime */
	@Deprecated
	public static final IRI IN_XSDDATE_TIME;

	/** time:inXSDDateTimeStamp */
	public static final IRI IN_XSDDATE_TIME_STAMP;

	/** time:inXSDgYear */
	public static final IRI IN_XSDG_YEAR;

	/** time:inXSDgYearMonth */
	public static final IRI IN_XSDG_YEAR_MONTH;

	/** time:inside */
	public static final IRI INSIDE;

	/** time:intervalAfter */
	public static final IRI INTERVAL_AFTER;

	/** time:intervalBefore */
	public static final IRI INTERVAL_BEFORE;

	/** time:intervalContains */
	public static final IRI INTERVAL_CONTAINS;

	/** time:intervalDisjoint */
	public static final IRI INTERVAL_DISJOINT;

	/** time:intervalDuring */
	public static final IRI INTERVAL_DURING;

	/** time:intervalEquals */
	public static final IRI INTERVAL_EQUALS;

	/** time:intervalFinishedBy */
	public static final IRI INTERVAL_FINISHED_BY;

	/** time:intervalFinishes */
	public static final IRI INTERVAL_FINISHES;

	/** time:intervalIn */
	public static final IRI INTERVAL_IN;

	/** time:intervalMeets */
	public static final IRI INTERVAL_MEETS;

	/** time:intervalMetBy */
	public static final IRI INTERVAL_MET_BY;

	/** time:intervalOverlappedBy */
	public static final IRI INTERVAL_OVERLAPPED_BY;

	/** time:intervalOverlaps */
	public static final IRI INTERVAL_OVERLAPS;

	/** time:intervalStartedBy */
	public static final IRI INTERVAL_STARTED_BY;

	/** time:intervalStarts */
	public static final IRI INTERVAL_STARTS;

	/** time:minute */
	public static final IRI MINUTE;

	/** time:minutes */
	public static final IRI MINUTES;

	/** time:month */
	public static final IRI MONTH;

	/** time:monthOfYear */
	public static final IRI MONTH_OF_YEAR_PROP;

	/** time:months */
	public static final IRI MONTHS;

	/** time:nominalPosition */
	public static final IRI NOMINAL_POSITION;

	/** time:numericDuration */
	public static final IRI NUMERIC_DURATION;

	/** time:numericPosition */
	public static final IRI NUMERIC_POSITION;

	/** time:second */
	public static final IRI SECOND;

	/** time:seconds */
	public static final IRI SECONDS;

	/** time:timeZone */
	public static final IRI TIME_ZONE_PROP;

	/** time:unitType */
	public static final IRI UNIT_TYPE;

	/** time:week */
	public static final IRI WEEK;

	/** time:weeks */
	public static final IRI WEEKS;

	/** time:xsdDateTime */
	@Deprecated
	public static final IRI XSD_DATE_TIME;

	/** time:year */
	public static final IRI YEAR_PROP;

	/** time:years */
	public static final IRI YEARS;

	// Individuals
	/** time:Monday */
	public static final IRI MONDAY;

	/** time:Tuesday */
	public static final IRI TUESDAY;

	/** time:Wednesday */
	public static final IRI WEDNESDAY;

	/** time:Thursday */
	public static final IRI THURSDAY;

	/** time:Friday */
	public static final IRI FRIDAY;

	/** time:Saturday */
	public static final IRI SATURDAY;

	/** time:Sunday */
	public static final IRI SUNDAY;

	/** time:unitSecond */
	public static final IRI UNIT_SECOND;

	/** time:unitMinute */
	public static final IRI UNIT_MINUTE;

	/** time:unitHour */
	public static final IRI UNIT_HOUR;

	/** time:unitDay */
	public static final IRI UNIT_DAY;

	/** time:unitWeek */
	public static final IRI UNIT_WEEK;

	/** time:unitMonth */
	public static final IRI UNIT_MONTH;

	/** time:unitYear */
	public static final IRI UNIT_YEAR;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();

		DATE_TIME_DESCRIPTION = factory.createIRI(NAMESPACE, "DateTimeDescription");
		DATE_TIME_INTERVAL = factory.createIRI(NAMESPACE, "DateTimeInterval");
		DAY_OF_WEEK = factory.createIRI(NAMESPACE, "DayOfWeek");
		DURATION = factory.createIRI(NAMESPACE, "Duration");
		DURATION_DESCRIPTION = factory.createIRI(NAMESPACE, "DurationDescription");
		FRIDAY = factory.createIRI(NAMESPACE, "Friday");
		GENERAL_DATE_TIME_DESCRIPTION = factory.createIRI(NAMESPACE, "GeneralDateTimeDescription");
		GENERAL_DURATION_DESCRIPTION = factory.createIRI(NAMESPACE, "GeneralDurationDescription");
		INSTANT = factory.createIRI(NAMESPACE, "Instant");
		INTERVAL = factory.createIRI(NAMESPACE, "Interval");
		JANUARY = factory.createIRI(NAMESPACE, "January");
		MONDAY = factory.createIRI(NAMESPACE, "Monday");
		MONTH_OF_YEAR = factory.createIRI(NAMESPACE, "MonthOfYear");
		PROPER_INTERVAL = factory.createIRI(NAMESPACE, "ProperInterval");
		SATURDAY = factory.createIRI(NAMESPACE, "Saturday");
		SUNDAY = factory.createIRI(NAMESPACE, "Sunday");
		TRS = factory.createIRI(NAMESPACE, "TRS");
		TEMPORAL_DURATION = factory.createIRI(NAMESPACE, "TemporalDuration");
		TEMPORAL_ENTITY = factory.createIRI(NAMESPACE, "TemporalEntity");
		TEMPORAL_POSITION = factory.createIRI(NAMESPACE, "TemporalPosition");
		TEMPORAL_UNIT = factory.createIRI(NAMESPACE, "TemporalUnit");
		THURSDAY = factory.createIRI(NAMESPACE, "Thursday");
		TIME_POSITION = factory.createIRI(NAMESPACE, "TimePosition");
		TIME_ZONE = factory.createIRI(NAMESPACE, "TimeZone");
		TUESDAY = factory.createIRI(NAMESPACE, "Tuesday");
		WEDNESDAY = factory.createIRI(NAMESPACE, "Wednesday");
		YEAR = factory.createIRI(NAMESPACE, "Year");
		UNIT_DAY = factory.createIRI(NAMESPACE, "unitDay");
		UNIT_HOUR = factory.createIRI(NAMESPACE, "unitHour");
		UNIT_MINUTE = factory.createIRI(NAMESPACE, "unitMinute");
		UNIT_MONTH = factory.createIRI(NAMESPACE, "unitMonth");
		UNIT_SECOND = factory.createIRI(NAMESPACE, "unitSecond");
		UNIT_WEEK = factory.createIRI(NAMESPACE, "unitWeek");
		UNIT_YEAR = factory.createIRI(NAMESPACE, "unitYear");

		AFTER = factory.createIRI(NAMESPACE, "after");
		BEFORE = factory.createIRI(NAMESPACE, "before");
		DAY = factory.createIRI(NAMESPACE, "day");
		DAY_OF_WEEK_PROP = factory.createIRI(NAMESPACE, "dayOfWeek");
		DAY_OF_YEAR = factory.createIRI(NAMESPACE, "dayOfYear");
		DAYS = factory.createIRI(NAMESPACE, "days");
		HAS_BEGINNING = factory.createIRI(NAMESPACE, "hasBeginning");
		HAS_DATE_TIME_DESCRIPTION = factory.createIRI(NAMESPACE, "hasDateTimeDescription");
		HAS_DURATION = factory.createIRI(NAMESPACE, "hasDuration");
		HAS_DURATION_DESCRIPTION = factory.createIRI(NAMESPACE, "hasDurationDescription");
		HAS_END = factory.createIRI(NAMESPACE, "hasEnd");
		HAS_TRS = factory.createIRI(NAMESPACE, "hasTRS");
		HAS_TEMPORAL_DURATION = factory.createIRI(NAMESPACE, "hasTemporalDuration");
		HAS_TIME = factory.createIRI(NAMESPACE, "hasTime");
		HAS_XSDDURATION = factory.createIRI(NAMESPACE, "hasXSDDuration");
		HOUR = factory.createIRI(NAMESPACE, "hour");
		HOURS = factory.createIRI(NAMESPACE, "hours");
		IN_DATE_TIME = factory.createIRI(NAMESPACE, "inDateTime");
		IN_TEMPORAL_POSITION = factory.createIRI(NAMESPACE, "inTemporalPosition");
		IN_TIME_POSITION = factory.createIRI(NAMESPACE, "inTimePosition");
		IN_XSDDATE = factory.createIRI(NAMESPACE, "inXSDDate");
		IN_XSDDATE_TIME = factory.createIRI(NAMESPACE, "inXSDDateTime");
		IN_XSDDATE_TIME_STAMP = factory.createIRI(NAMESPACE, "inXSDDateTimeStamp");
		IN_XSDG_YEAR = factory.createIRI(NAMESPACE, "inXSDgYear");
		IN_XSDG_YEAR_MONTH = factory.createIRI(NAMESPACE, "inXSDgYearMonth");
		INSIDE = factory.createIRI(NAMESPACE, "inside");
		INTERVAL_AFTER = factory.createIRI(NAMESPACE, "intervalAfter");
		INTERVAL_BEFORE = factory.createIRI(NAMESPACE, "intervalBefore");
		INTERVAL_CONTAINS = factory.createIRI(NAMESPACE, "intervalContains");
		INTERVAL_DISJOINT = factory.createIRI(NAMESPACE, "intervalDisjoint");
		INTERVAL_DURING = factory.createIRI(NAMESPACE, "intervalDuring");
		INTERVAL_EQUALS = factory.createIRI(NAMESPACE, "intervalEquals");
		INTERVAL_FINISHED_BY = factory.createIRI(NAMESPACE, "intervalFinishedBy");
		INTERVAL_FINISHES = factory.createIRI(NAMESPACE, "intervalFinishes");
		INTERVAL_IN = factory.createIRI(NAMESPACE, "intervalIn");
		INTERVAL_MEETS = factory.createIRI(NAMESPACE, "intervalMeets");
		INTERVAL_MET_BY = factory.createIRI(NAMESPACE, "intervalMetBy");
		INTERVAL_OVERLAPPED_BY = factory.createIRI(NAMESPACE, "intervalOverlappedBy");
		INTERVAL_OVERLAPS = factory.createIRI(NAMESPACE, "intervalOverlaps");
		INTERVAL_STARTED_BY = factory.createIRI(NAMESPACE, "intervalStartedBy");
		INTERVAL_STARTS = factory.createIRI(NAMESPACE, "intervalStarts");
		MINUTE = factory.createIRI(NAMESPACE, "minute");
		MINUTES = factory.createIRI(NAMESPACE, "minutes");
		MONTH = factory.createIRI(NAMESPACE, "month");
		MONTH_OF_YEAR_PROP = factory.createIRI(NAMESPACE, "monthOfYear");
		MONTHS = factory.createIRI(NAMESPACE, "months");
		NOMINAL_POSITION = factory.createIRI(NAMESPACE, "nominalPosition");
		NUMERIC_DURATION = factory.createIRI(NAMESPACE, "numericDuration");
		NUMERIC_POSITION = factory.createIRI(NAMESPACE, "numericPosition");
		SECOND = factory.createIRI(NAMESPACE, "second");
		SECONDS = factory.createIRI(NAMESPACE, "seconds");
		TIME_ZONE_PROP = factory.createIRI(NAMESPACE, "timeZone");
		UNIT_TYPE = factory.createIRI(NAMESPACE, "unitType");
		WEEK = factory.createIRI(NAMESPACE, "week");
		WEEKS = factory.createIRI(NAMESPACE, "weeks");
		XSD_DATE_TIME = factory.createIRI(NAMESPACE, "xsdDateTime");
		YEAR_PROP = factory.createIRI(NAMESPACE, "year");
		YEARS = factory.createIRI(NAMESPACE, "years");
	}
}
