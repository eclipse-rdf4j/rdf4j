/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.logging.file.logback;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.common.logging.LogReader;
import org.eclipse.rdf4j.common.logging.LogRecord;
import org.eclipse.rdf4j.common.logging.base.AbstractLogReader;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.RollingPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

/**
 * Date range-enabled wrapper for FileLogReader. Reads multiple log files chunked by dates as a single log.
 *
 * @author alex
 */
public class MultipleFileLogReader extends AbstractLogReader {

	private Date startDate = null;
	private Date endDate = null;
	private Date minDate = new Date();
	private Date maxDate = new Date();

	private String fileNamePattern = null;
	private Vector<File> logFiles = new Vector<>();
	private Iterator<File> logFilesIterator = null;

	private LogRecord next = null;
	private int count = 0;

	private FileLogReader currentReader = null;

	@Override
	public boolean supportsDateRanges() {
		return true;
	}

	@Override
	public void setAppender(Appender<?> appender) {
		super.setAppender(appender);
		if (appender instanceof RollingFileAppender) {
			RollingPolicy rp = ((RollingFileAppender<?>) appender).getRollingPolicy();
			if (rp instanceof TimeBasedRollingPolicy) {
				fileNamePattern = ((TimeBasedRollingPolicy) rp).getFileNamePattern();
			} else {
				throw new UnsupportedOperationException("Must be TimeBasedRollingPolicy!");
			}
		} else {
			throw new RuntimeException("MultipleFileLogReader appender must be an instance of RollingFileAppender!");
		}
	}

	@Override
	public void init() throws Exception {
		if (this.getAppender() == null) {
			throw new RuntimeException("Appender must be set before initialization!");
		}
		count = 0;
		logFiles = new Vector<>();
		Calendar startCal = null;
		Calendar endCal = null;
		if (startDate != null) {
			startCal = Calendar.getInstance();
			startCal.setTime(startDate);
		}
		if (endDate != null) {
			endCal = Calendar.getInstance();
			endCal.setTime(endDate);
		}
		// Extracts date pattern
		Pattern dfPattern = Pattern.compile("(.+)%d\\{(.+)\\}");
		Matcher dfMatcher = dfPattern.matcher(fileNamePattern);
		if (!dfMatcher.matches()) {
			throw new RuntimeException("Wrong filename pattern: " + fileNamePattern);
		}

		String dfs = dfMatcher.group(2);
		SimpleDateFormat df = new SimpleDateFormat(dfs);
		String spattern = new File(fileNamePattern).getName();
		spattern = spattern.replace(".", "\\.");
		spattern = spattern.replace("%d{" + dfs + "}", "(.*)");
		Pattern pattern = Pattern.compile(spattern);
		File dir = new File(fileNamePattern).getParentFile();

		String[] files = dir.list(new DateRangeFilenameFilter(pattern, df, startCal, endCal));
		Arrays.sort(files);
		for (int i = files.length - 1; i >= 0; i--) {
			File f = new File(dir, files[i]);
			logFiles.add(f);
			System.out.println(f.getAbsolutePath());
		}
		logFilesIterator = logFiles.iterator();
		if (logFilesIterator.hasNext()) {
			currentReader = new FileLogReader(logFilesIterator.next());
			currentReader.init();
			next = getNext();
			if (getOffset() > 0) {
				doSkip(getOffset());
			}
		}
	}

	@Override
	public boolean hasNext() {
		if (getLimit() == 0) {
			return isMoreAvailable();
		}
		return isMoreAvailable() && (count < (getOffset() + getLimit()));
	}

	@Override
	public boolean isMoreAvailable() {
		return next != null;
	}

	@Override
	public LogRecord next() {
		LogRecord result = next;
		try {
			next = getNext();
			count++;
		} catch (Exception ex) {
			throw new RuntimeException("Unable to get next log record.", ex);
		}
		if (!hasNext()) {
			try {
				destroy();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * Get next log record
	 *
	 * @return log record
	 * @throws Exception
	 */
	private LogRecord getNext() throws Exception {
		if (currentReader.hasNext()) {
			return currentReader.next();
		}
		if (logFilesIterator.hasNext()) {
			currentReader = new FileLogReader(logFilesIterator.next());
			currentReader.init();
			return getNext();
		}
		return null;
	}

	/**
	 * Skip for a specific offset
	 *
	 * @param offset offset
	 */
	private void doSkip(int offset) {
		while (this.hasNext() && (count < offset)) {
			this.next();
		}
	}

	@Override
	public void destroy() throws IOException {
		if (currentReader.hasNext()) {
			currentReader.destroy();
		}
	}

	/**
	 * Return the start date
	 *
	 * @return start date.
	 */
	@Override
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * Set start date
	 *
	 * @param startDate The startDate to set.
	 */
	@Override
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * Return the end date
	 *
	 * @return end date
	 */
	@Override
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * Set the end date
	 *
	 * @param endDate The endDate to set.
	 */
	@Override
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	@Override
	public Date getMaxDate() {
		return this.maxDate;
	}

	@Override
	public Date getMinDate() {
		return this.minDate;
	}

	/**
	 * Custom filename filter
	 *
	 * @author alex
	 */
	public class DateRangeFilenameFilter implements FilenameFilter {

		Pattern pattern;

		SimpleDateFormat df;

		Calendar startCal, endCal;

		/**
		 * Constructor
		 *
		 * @param pattern
		 * @param df
		 * @param startCal
		 * @param endCal
		 */
		public DateRangeFilenameFilter(Pattern pattern, SimpleDateFormat df, Calendar startCal, Calendar endCal) {
			this.pattern = pattern;
			this.df = df;
			this.startCal = startCal;
			this.endCal = endCal;
		}

		@Override
		public boolean accept(File dir, String name) {
			Matcher matcher = pattern.matcher(name);
			if (!matcher.matches()) {
				return false;
			}
			String ds = matcher.group(1);
			Date d;
			try {
				d = df.parse(ds);
			} catch (ParseException e) {
				e.printStackTrace();
				return false;
			}
			if ((maxDate == null) || (d.compareTo(maxDate) > 0)) {
				maxDate = d;
			}
			if ((minDate == null) || (d.compareTo(minDate) < 0)) {
				minDate = d;
			}
			Calendar cal = Calendar.getInstance();
			cal.setTime(d);
			return (((startCal == null) || (cal.compareTo(startCal) >= 0))
					&& ((endCal == null) || (cal.compareTo(endCal) <= 0)));
		}

	}
}
