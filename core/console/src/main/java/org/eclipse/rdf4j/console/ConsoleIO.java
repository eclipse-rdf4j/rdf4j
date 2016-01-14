/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

import org.eclipse.rdf4j.rio.RDFParseException;

/**
 * @author Dale Visser
 */
class ConsoleIO {

	private static final String PLEASE_OPEN_FIRST = "please open a repository first";

	private final BufferedReader input;

	private final PrintStream out, err;

	private final ConsoleState appInfo;

	private boolean echo = false;

	private boolean quiet = false;

	private boolean force = false;

	private boolean cautious = false;

	private boolean errorWritten;

	ConsoleIO(BufferedReader input, PrintStream out, PrintStream err, ConsoleState info) {
		this.input = input;
		this.out = out;
		this.err = err;
		this.appInfo = info;
	}

	protected String readCommand() throws IOException {
		String repositoryID = appInfo.getRepositoryID();
		if (!quiet) {
			if (repositoryID != null) {
				write(repositoryID);
			}
			write("> ");
		}
		String line = input.readLine().trim();
		if (line.endsWith(".")) {
			line = line.substring(0, line.length() - 1); 
		}
		return line;
	}
	
	/**
	 * Reads multiple lines from the input until a line that with a '.' on its own is
	 * read.
	 */
	protected String readMultiLineInput()
		throws IOException
	{
		String line = input.readLine();
		String result = null;
		if (line != null) {
			final StringBuilder buf = new StringBuilder(256);
			buf.append(line);
			while (line != null && !(line.length() == 1 && line.endsWith("."))) {
				line = input.readLine();
				buf.append('\n');
				buf.append(line);
			}

			// Remove closing dot
			buf.setLength(buf.length() - 1);
			result = buf.toString().trim();
		}
		if (echo) {
			writeln(result);
		}
		return result;
	}

	protected String readln(String... message)
		throws IOException
	{
		if (!quiet && message.length > 0) {
			String prompt = message[0];
			if (prompt != null) {
				write(prompt + " ");
			}
		}
		String result = input.readLine();
		if (echo) {
			writeln(result);
		}
		return result;
	}

	protected String readPassword(final String message)
		throws IOException
	{
		// TODO: Proper password reader
		String result = readln(message);
		if (echo && !result.isEmpty()) {
			writeln("************");
		}
		return result;
	}

	protected void write(final String string) {
		out.print(string);
	}

	protected void writeln() {
		out.println();
	}

	protected void writeln(final String string) {
		out.println(string);
	}

	protected void writeError(final String errMsg) {
		err.println(errMsg);
		errorWritten = true;
	}

	protected void writeUnopenedError() {
		writeError(PLEASE_OPEN_FIRST);
	}

	protected void writeParseError(final String prefix, final long lineNo, final long colNo, final String msg) {
		String locationString = RDFParseException.getLocationString(lineNo, colNo);
		int locSize = locationString.length();
		final StringBuilder builder = new StringBuilder(locSize + prefix.length() + msg.length() + 3);
		builder.append(prefix).append(": ").append(msg);
		if (locSize > 0) {
			builder.append(" ").append(locationString);
		}
		writeError(builder.toString());
	}

	protected boolean askProceed(final String msg, final boolean defaultValue)
		throws IOException
	{
		final String defaultString = defaultValue ? "yes" : "no";
		boolean result = force ? true : (cautious ? false : defaultValue);
		if (!force && !cautious) {
			while (true) {
				writeln(msg);
				write("Proceed? (yes|no) [" + defaultString + "]: ");
				final String reply = readln();
				if ("no".equalsIgnoreCase(reply) || "no.".equalsIgnoreCase(reply)) {
					result = false;
					break;
				}
				else if ("yes".equalsIgnoreCase(reply) || "yes.".equalsIgnoreCase(reply)) {
					result = true;
					break;
				}
				else if (reply.trim().isEmpty()) {
					break;
				}
			}
		}
		return result;
	}

	/**
	 * @param echo
	 *        whether to echo user input to output stream
	 */
	protected void setEcho(boolean echo) {
		this.echo = echo;
	}

	/**
	 * @param quiet
	 *        whether to suppress printing of prompts to output
	 */
	public void setQuiet(boolean quiet) {
		this.quiet = quiet;
	}

	/**
	 */
	public void setForce() {
		this.force = true;
	}

	/**
	 */
	public void setCautious() {
		this.cautious = true;
	}

	public boolean wasErrorWritten() {
		return errorWritten;
	}
}
