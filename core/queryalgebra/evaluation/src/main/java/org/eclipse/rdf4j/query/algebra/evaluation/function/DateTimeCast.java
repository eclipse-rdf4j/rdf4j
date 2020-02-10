/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function;

/**
 * A {@link org.eclipse.rdf4j.query.algebra.evaluation.function.Function} that tries to cast its argument to an
 * <tt>xsd:dateTime</tt>.
 * 
 * @author Arjohn Kampman
 * @deprecated use {@link org.eclipse.rdf4j.query.algebra.evaluation.function.xsd.DateTimeCast} instead.
 */
@Deprecated
public class DateTimeCast extends org.eclipse.rdf4j.query.algebra.evaluation.function.xsd.DateTimeCast {

}
