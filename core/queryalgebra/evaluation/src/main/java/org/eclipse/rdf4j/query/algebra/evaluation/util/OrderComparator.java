/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.OrderElem;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Comparator} on {@link BindingSet}s that imposes a total ordering by
 * examining supplied {@link Order} elements (i.e. the elements of an ORDER BY
 * clause), falling back on a custom predictable ordering for BindingSet
 * elements if no ordering is established on the basis of the Order elements.
 * 
 * @author James Leigh
 * @author Jeen Broekstra
 */
public class OrderComparator implements Comparator<BindingSet>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7002730491398949902L;

	private final transient Logger logger = LoggerFactory.getLogger(OrderComparator.class);

	private transient EvaluationStrategy strategy;

	private UUID strategyKey;

	private final Order order;

	private transient ValueComparator cmp;

	public OrderComparator(EvaluationStrategy strategy, Order order, ValueComparator vcmp) {
		this.strategy = strategy;
		this.order = order;
		this.cmp = vcmp;
	}

	public int compare(BindingSet o1, BindingSet o2) {

		try {

			for (OrderElem element : order.getElements()) {
				Value v1 = evaluate(element.getExpr(), o1);
				Value v2 = evaluate(element.getExpr(), o2);

				int compare = cmp.compare(v1, v2);

				if (compare != 0) {
					return element.isAscending() ? compare : -compare;
				}
			}

			// On the basis of the order clause elements the two binding sets are
			// unordered.
			// We now need to impose a total ordering (as per the
			// contract of java.util.Comparator). We order by
			// size first, then by binding names, then finally by values.

			// null check
			if (o1 == null || o2 == null) {
				if (o1 == null) {
					return o2 == null ? 0 : 1;
				}
				if (o2 == null) {
					return o1 == null ? 0 : -1;
				}
			}

			if (o2.size() != o1.size()) {
				return o1.size() < o2.size() ? 1 : -1;
			}

			// we create an ordered list of binding names (using natural string order) to use for 
			// consistent iteration over binding names and binding values.
			final ArrayList<String> o1bindingNamesOrdered = new ArrayList<String>(o1.getBindingNames());
			Collections.sort(o1bindingNamesOrdered);
		
			// binding set sizes are equal. compare on binding names.
			if (!o1.getBindingNames().equals(o2.getBindingNames())) {

				final ArrayList<String> o2bindingNamesOrdered = new ArrayList<String>(o2.getBindingNames());
				Collections.sort(o2bindingNamesOrdered);

				for (int i = 0; i < o1bindingNamesOrdered.size(); i++) {
					String o1bn = o1bindingNamesOrdered.get(i);
					String o2bn = o2bindingNamesOrdered.get(i);
					int compare = o1bn.compareTo(o2bn);
					if (compare != 0) {
						return compare;
					}
				}
			}

			// binding names equal. compare on all values.
			for (String bindingName: o1bindingNamesOrdered) {
				final Value v1 = o1.getValue(bindingName);
				final Value v2 = o2.getValue(bindingName);

				final int compare = cmp.compare(v1, v2);
				if (compare != 0) {
					return compare;
				}
			}

			return 0;
		}
		catch (QueryEvaluationException e) {
			logger.debug(e.getMessage(), e);
			return 0;
		}
		catch (IllegalArgumentException e) {
			logger.debug(e.getMessage(), e);
			return 0;
		}
	}

	private Value evaluate(ValueExpr valueExpr, BindingSet o)
		throws QueryEvaluationException
	{
		try {
			return strategy.evaluate(valueExpr, o);
		}
		catch (ValueExprEvaluationException exc) {
			return null;
		}
	}

	private void writeObject(ObjectOutputStream out)
		throws IOException
	{
		this.strategyKey = EvaluationStrategies.register(strategy);
		out.defaultWriteObject();
	}

	private void readObject(ObjectInputStream in)
		throws IOException, ClassNotFoundException
	{
		in.defaultReadObject();
		this.strategy = EvaluationStrategies.get(this.strategyKey);
		this.cmp = new ValueComparator();
	}
}
