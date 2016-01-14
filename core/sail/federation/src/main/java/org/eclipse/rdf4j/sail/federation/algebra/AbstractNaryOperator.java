/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.algebra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;

/**
 * An abstract superclass for operators which have (zero or more) arguments.
 */
public abstract class AbstractNaryOperator<Expr extends QueryModelNode> extends AbstractQueryModelNode {

	private static final long serialVersionUID = 2645544440976923085L;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The operator's arguments.
	 */
	private List<Expr> args = new ArrayList<Expr>();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public AbstractNaryOperator() {
		super();
	}

	/**
	 * Creates a new n-ary operator.
	 */
	public AbstractNaryOperator(final Expr... args) {
		this(Arrays.asList(args));
	}

	/**
	 * Creates a new n-ary operator.
	 */
	public AbstractNaryOperator(final List<? extends Expr> args) {
		this();
		setArgs(args);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the arguments of this n-ary operator.
	 * 
	 * @return A copy of the current argument list.
	 */
	public List<? extends Expr> getArgs() {
		return new CopyOnWriteArrayList<Expr>(args);
	}

	/**
	 * Gets the number of arguments of this n-ary operator.
	 * 
	 * @return The number of arguments.
	 */
	public int getNumberOfArguments() {
		return args.size();
	}

	/**
	 * Gets the <tt>idx</tt>-th argument of this n-ary operator.
	 * 
	 * @return The operator's arguments.
	 */
	public Expr getArg(final int idx) {
		return (idx < args.size()) ? args.get(idx) : null; // NOPMD
	}

	/**
	 * Sets the arguments of this n-ary tuple operator.
	 */
	private final void addArgs(final List<? extends Expr> args) {
		assert args != null;
		for (Expr arg : args) {
			addArg(arg);
		}
	}

	/**
	 * Sets the arguments of this n-ary operator.
	 */
	public final void addArg(final Expr arg) {
		setArg(this.args.size(), arg);
	}

	/**
	 * Sets the arguments of this n-ary operator.
	 */
	private final void setArgs(final List<? extends Expr> args) {
		this.args.clear();
		addArgs(args);
	}

	/**
	 * Sets the <tt>idx</tt>-th argument of this n-ary tuple operator.
	 */
	protected final void setArg(final int idx, final Expr arg) {
		if (arg != null) {
			// arg can be null (i.e. Regex)
			arg.setParentNode(this);
		}

		while (args.size() <= idx) {
			args.add(null);
		}

		this.args.set(idx, arg);
	}

	public boolean removeArg(final Expr arg) {
		return args.remove(arg);
	}

	@Override
	public <X extends Exception> void visitChildren(final QueryModelVisitor<X> visitor)
		throws X
	{
		for (Expr arg : args) {
			if (arg != null) {
				arg.visit(visitor);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void replaceChildNode(final QueryModelNode current, final QueryModelNode replacement) {
		final int index = args.indexOf(current);
		if (index >= 0) {
			setArg(index, (Expr)replacement);
		}
		else {
			super.replaceChildNode(current, replacement);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public AbstractNaryOperator<Expr> clone() { // NOPMD
		final AbstractNaryOperator<Expr> clone = (AbstractNaryOperator<Expr>)super.clone();
		clone.args = new ArrayList<Expr>(args.size());
		for (Expr arg : args) {
			final Expr argClone = (arg == null) ? null : (Expr)arg.clone(); // NOPMD
			clone.addArg(argClone);
		}
		return clone;
	}
}
