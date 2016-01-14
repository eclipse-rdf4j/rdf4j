/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.openrdf.sail.rdbms.iteration.base;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import info.aduna.iteration.AbstractCloseableIteration;

/**
 * Base class for Iteration of a {@link ResultSet}.
 * 
 * @author James Leigh
 * 
 */
public abstract class RdbmIterationBase<T, X extends Exception> extends AbstractCloseableIteration<T, X> {

	private PreparedStatement stmt;

	private ResultSet rs;

	private boolean advanced;

	private boolean hasNext;

	public RdbmIterationBase(PreparedStatement stmt)
		throws SQLException
	{
		super();
		this.stmt = stmt;
		if (stmt != null) {
			this.rs = stmt.executeQuery();
		}
	}

	@Override
	protected void handleClose()
		throws X
	{
		super.handleClose();
		try {
			rs.close();
			stmt.close();
		}
		catch (SQLException e) {
			throw convertSQLException(e);
		}
	}

	public boolean hasNext()
		throws X
	{
		if (advanced)
			return hasNext;
		advanced = true;
		try {
			return hasNext = rs.next();
		}
		catch (SQLException e) {
			throw convertSQLException(e);
		}
	}

	public T next()
		throws X
	{
		try {
			if (!advanced) {
				hasNext = rs.next();
			}
			advanced = false;
			return convert(rs);
		}
		catch (SQLException e) {
			throw convertSQLException(e);
		}
	}

	public void remove()
		throws X
	{
		try {
			rs.rowDeleted();
		}
		catch (SQLException e) {
			throw convertSQLException(e);
		}
	}

	protected abstract T convert(ResultSet rs)
		throws SQLException;

	protected abstract X convertSQLException(SQLException e);
	
}
