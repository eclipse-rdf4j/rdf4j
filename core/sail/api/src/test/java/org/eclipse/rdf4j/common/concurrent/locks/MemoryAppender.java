/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.common.concurrent.locks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.read.ListAppender;

public class MemoryAppender extends ListAppender<ILoggingEvent> {
	public void reset() {
		this.list.clear();
	}

	public void assertContains(String string, Level level) {
		Assertions.assertThat(
				this.list.stream()
						.map(ILoggingEvent::getLevel)
						.collect(Collectors.toList())
		).contains(level);

		Assertions.assertThat(
				this.list.stream()
						.map(this::getString)
						.collect(Collectors.joining("\n\n"))
		).contains(string);
	}

	public void assertNotContains(String string, Level level) {
		Assertions.assertThat(
				this.list.stream()
						.filter(e -> e.getLevel().equals(level))
						.map(this::getString)
						.collect(Collectors.joining("\n\n"))
		).doesNotContain(string);
	}

	private String getString(ILoggingEvent e) {
		if (e.getThrowableProxy() != null && e.getThrowableProxy().getStackTraceElementProxyArray() != null) {
			return e + "\n" +
					Arrays
							.stream(e.getThrowableProxy().getStackTraceElementProxyArray())
							.map(StackTraceElementProxy::toString)
							.map(s -> "\t" + s)
							.collect(Collectors.joining("\n"));
		} else {
			return e.toString();
		}

	}

	public int countEventsForLogger(String loggerName) {
		return (int) this.list.stream()
				.filter(event -> event.getLoggerName().contains(loggerName))
				.count();
	}

	public List<ILoggingEvent> search(String string) {
		return this.list.stream()
				.filter(event -> getString(event).contains(string))
				.collect(Collectors.toList());
	}

	public List<ILoggingEvent> search(String string, Level level) {
		return this.list.stream()
				.filter(event -> getString(event).contains(string)
						&& event.getLevel().equals(level))
				.collect(Collectors.toList());
	}

	public int getSize() {
		return this.list.size();
	}

	public List<ILoggingEvent> getLoggedEvents() {
		return Collections.unmodifiableList(this.list);
	}

	@Override
	public String toString() {
		return this.list.stream().map(this::getString).collect(Collectors.joining("\n\n"));
	}

	public void waitForEvents() {
		while (list.isEmpty()) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
}
