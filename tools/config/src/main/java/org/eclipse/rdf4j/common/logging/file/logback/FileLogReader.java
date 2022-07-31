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
package org.eclipse.rdf4j.common.logging.file.logback;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

import org.eclipse.rdf4j.common.logging.LogLevel;
import org.eclipse.rdf4j.common.logging.LogRecord;
import org.eclipse.rdf4j.common.logging.base.AbstractLogReader;
import org.eclipse.rdf4j.common.logging.base.SimpleLogRecord;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;

/**
 * File log reader
 */
public class FileLogReader extends AbstractLogReader {

	private File logFile = null;
	private RandomAccessFile log = null;

	private long fileLength;
	private long byteOffset;

	private LogRecord next = null;
	private int count = 0;

	/**
	 * Constructor
	 */
	public FileLogReader() {
	}

	/**
	 * Constructor
	 *
	 * @param logFile
	 */
	public FileLogReader(File logFile) {
		this.logFile = logFile;
	}

	@Override
	public void setAppender(Appender<?> appender) {
		super.setAppender(appender);
		if (appender instanceof FileAppender) {
			this.logFile = new File(((FileAppender<?>) appender).getFile());
		} else {
			throw new RuntimeException("FileLogReader appender must be an instance of FileAppender!");
		}
		this.next = null;
	}

	@Override
	public void init() throws Exception {
		if (logFile == null) {
			throw new RuntimeException("Log file is undefined for this FileLogReader!");
		}
		if (log != null) {
			log.close();
		}
		log = new RandomAccessFile(logFile, "r");
		fileLength = log.length();
		byteOffset = fileLength - 1;
		count = 0;
		next = getNext();
		if (getOffset() > 0) {
			doSkip(getOffset());
		}
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
	public boolean isMoreAvailable() {
		return next != null;
	}

	@Override
	public boolean hasNext() {
		if (getLimit() == 0) {
			return isMoreAvailable();
		}
		return isMoreAvailable() && (count < (getOffset() + getLimit()));
	}

	@Override
	public LogRecord next() {
		LogRecord result = next;
		try {
			next = getNext();
			count++;
		} catch (IOException ioe) {
			throw new RuntimeException("Unable to get next log record.", ioe);
		}
		if (!hasNext()) {
			try {
				destroy();
			} catch (IOException e) {
				// too bad
			}
		}
		return result;
	}

	/**
	 * Get next log record
	 *
	 * @return log record
	 * @throws IOException
	 */
	private LogRecord getNext() throws IOException {
		SimpleLogRecord result = null;

		StringBuilder message = new StringBuilder();

		List<String> stackTrace = new LinkedList<>();

		while (result == null && byteOffset > 0) {
			List<Byte> bytesRead = new LinkedList<>();
			if (byteOffset < 0) {
				System.err.println("Subzero byteOffset with: ");
				System.err.println("\tMessage: " + message);
				System.err.println("\tStacktrace: " + stackTrace.size());
			}
			// find start of previous line
			byte currentByte;
			do {
				log.seek(byteOffset--);
				currentByte = log.readByte();
				if (currentByte != '\n' && currentByte != '\r') {
					bytesRead.add(0, currentByte);
				}
			} while (byteOffset > 0 && currentByte != '\n' && currentByte != '\r');

			// if at start of file, retrieve the byte we just read in the do/while
			if (byteOffset < 1) {
				byteOffset = 0;
				log.seek(0);
			}

			// read the line
			byte[] lineBytes = new byte[bytesRead.size()];
			int index = 0;
			Iterator<Byte> byteIt = bytesRead.iterator();
			while (byteIt.hasNext()) {
				lineBytes[index] = byteIt.next();
				index++;
			}
			String lastLine = new String(lineBytes, StandardCharsets.UTF_8);

			if (lastLine != null) {
				// is this a log line?
				Matcher matcher = StackTracePatternLayout.DEFAULT_PARSER_PATTERN.matcher(lastLine);
				if (matcher.matches()) {
					try {
						LogLevel level = LogLevel.valueOf(matcher.group(1).trim());
						Date timestamp = LogRecord.ISO8601_TIMESTAMP_FORMAT.parse(matcher.group(2).trim());
						String threadName = matcher.group(3);
						message.insert(0, matcher.group(4));

						result = new SimpleLogRecord();
						result.setLevel(level);
						result.setTime(timestamp);
						result.setThreadName(threadName);
						result.setMessage(message.toString());
						result.setStackTrace(stackTrace);

						message = new StringBuilder();
						stackTrace = new ArrayList<>();
					} catch (ParseException pe) {
						throw new IOException("Unable to parse timestamp in log record");
					}
				}
				// it may be a message line or a stacktrace line
				else {
					if (!lastLine.trim().isEmpty()) {
						if (lastLine.startsWith("\t")) {
							stackTrace.add(0, lastLine.trim());
						} else {
							message.insert(0, lastLine);
						}
					}
				}
			}
		}

		return result;
	}

	@Override
	public void destroy() throws IOException {
		if (log != null) {
			log.close();
		}
		log = null;
	}

}
