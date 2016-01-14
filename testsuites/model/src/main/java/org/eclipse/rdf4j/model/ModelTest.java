/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.ApacheSetTestCase;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public abstract class ModelTest extends TestCase {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();
	
	public static Test suite(Class<? extends ModelTest> theClass)
			throws Exception {
		TestSuite suite = new TestSuite(theClass);
		if (suite.testCount() == 1
				&& "warning".equals(((TestCase) suite.tests().nextElement())
						.getName())) {
			suite = new TestSuite(theClass.getName());
		}
		Constructor<? extends Test> constructor = theClass
				.getConstructor(String.class);
		for (String name : getTestNames(ApacheSetTestCase.class)) {
			suite.addTest(constructor.newInstance("model:" + name));
			suite.addTest(constructor.newInstance("filtered:" + name));
			suite.addTest(constructor.newInstance("cfiltered:" + name));
			suite.addTest(constructor.newInstance("subjects:" + name));
			suite.addTest(constructor.newInstance("predicates:" + name));
			suite.addTest(constructor.newInstance("objects:" + name));
			suite.addTest(constructor.newInstance("contexts:" + name));
		}
		return suite;
	}

	private static Set<String> getTestNames(Class<?> superClass) {
		Set<String> names = new HashSet<String>();
		while (Test.class.isAssignableFrom(superClass)) {
			Method[] methods = superClass.getDeclaredMethods();
			for (int i = 0; i < methods.length; i++) {
				String name = methods[i].getName();
				if (name.startsWith("test")) {
					names.add(name);
				}
			}
			superClass = superClass.getSuperclass();
		}
		return names;
	}

	public ModelTest(String name) {
		super(name);
	}

	@Override
	public void runBare() throws Throwable {
		String[] name = getName().split(":", 2);
		if (name.length < 2) {
			super.runBare();
		} else if ("model".equals(name[0])) {
			(new ApacheSetTestCase(name[1]) {

				@Override
				public Set makeEmptySet() {
					return makeEmptyModel();
				}

				@Override
				public Collection makeConfirmedCollection() {
					return new LinkedHashModel();
				}

				@Override
				public Object getOneElement() {
					return vf.createStatement(RDF.VALUE, RDF.TYPE, RDF.PROPERTY);
				}

				@Override
				public Object[] convert(Object[] seeds) {
					Random rand = new Random(0);
					List<Statement> list = new ArrayList<Statement>();
					for (Object seed : seeds) {
						IRI s = createURI(seed);
						IRI p = createURI(seeds[rand.nextInt(seeds.length)]);
						IRI o = createURI(seeds[rand.nextInt(seeds.length)]);
						if (rand.nextInt() % 2 == 0) {
							list.add(vf.createStatement(s, p, o));
						} else {
							IRI c = createURI(seeds[rand.nextInt(seeds.length)]);
							list.add(vf.createStatement(s, p, o, c));
						}
					}
					return list.toArray();
				}
			}).runBare();
		} else if ("filtered".equals(name[0])) {
			(new ApacheSetTestCase(name[1]) {

				@Override
				public Set makeEmptySet() {
					return makeEmptyModel().filter(null, null, null);
				}

				@Override
				public Collection makeConfirmedCollection() {
					return new LinkedHashModel();
				}

				@Override
				public Object getOneElement() {
					return vf.createStatement(RDF.VALUE, RDF.TYPE, RDF.PROPERTY);
				}

				@Override
				public Object[] convert(Object[] seeds) {
					Random rand = new Random(0);
					List<Statement> list = new ArrayList<Statement>();
					for (Object seed : seeds) {
						IRI s = createURI(seed);
						IRI p = createURI(seeds[rand.nextInt(seeds.length)]);
						IRI o = createURI(seeds[rand.nextInt(seeds.length)]);
						if (rand.nextInt() % 2 == 0) {
							list.add(vf.createStatement(s, p, o));
						} else {
							IRI c = createURI(seeds[rand.nextInt(seeds.length)]);
							list.add(vf.createStatement(s, p, o, c));
						}
					}
					return list.toArray();
				}
			}).runBare();
		} else if ("cfiltered".equals(name[0])) {
			(new ApacheSetTestCase(name[1]) {
				private IRI ctx0 = createURI("test0");
				private IRI ctx1 = createURI("test1");

				@Override
				public Set makeEmptySet() {
					Model model = makeEmptyModel();
					model.add(vf.createStatement(RDF.TYPE, RDF.TYPE, RDF.PROPERTY, createURI("hidden")));
					return model.filter(null, null, null, ctx0, ctx1);
				}

				@Override
				public Collection makeConfirmedCollection() {
					return new LinkedHashModel();
				}

				@Override
				public Object getOneElement() {
					return vf.createStatement(RDF.VALUE, RDF.TYPE, RDF.PROPERTY, ctx0);
				}

				@Override
				public Object[] convert(Object[] seeds) {
					Random rand = new Random(0);
					List<Statement> list = new ArrayList<Statement>();
					for (Object seed : seeds) {
						IRI s = createURI(seed);
						IRI p = createURI(seeds[rand.nextInt(seeds.length)]);
						IRI o = createURI(seeds[rand.nextInt(seeds.length)]);
						list.add(vf.createStatement(s, p, o, ctx1));
					}
					return list.toArray();
				}
			}).runBare();
		} else if ("subjects".equals(name[0])) {
			(new ApacheSetTestCase(name[1]) {

				@Override
				public Set makeEmptySet() {
					return makeEmptyModel().filter(null, RDF.VALUE, createURI("test")).subjects();
				}

				@Override
				public Collection makeConfirmedCollection() {
					return new HashSet();
				}

				@Override
				public Object getOneElement() {
					return createLiteral("new value");
				}

				@Override
				public Object[] convert(Object[] seeds) {
					List<IRI> list = new ArrayList<IRI>();
					for (Object seed : seeds) {
						list.add(createURI(seed));
					}
					return list.toArray();
				}
			}).runBare();
		} else if ("predicates".equals(name[0])) {
			(new ApacheSetTestCase(name[1]) {

				@Override
				public Set makeEmptySet() {
					return new LinkedHashModel().filter(createURI("test1"), null, createURI("test2")).predicates();
				}

				@Override
				public Collection makeConfirmedCollection() {
					return new HashSet();
				}

				@Override
				public Object getOneElement() {
					return createLiteral("new value");
				}

				@Override
				public Object[] convert(Object[] seeds) {
					List<IRI> list = new ArrayList<IRI>();
					for (Object seed : seeds) {
						list.add(createURI(seed));
					}
					return list.toArray();
				}
			}).runBare();
		} else if ("objects".equals(name[0])) {
			(new ApacheSetTestCase(name[1]) {

				@Override
				public Set makeEmptySet() {
					return new LinkedHashModel().filter(createURI("test"), RDF.VALUE, null).objects();
				}

				@Override
				public Collection makeConfirmedCollection() {
					return new HashSet();
				}

				@Override
				public Object getOneElement() {
					return createLiteral("new value");
				}

				@Override
				public Object[] convert(Object[] seeds) {
					List<Literal> list = new ArrayList<Literal>();
					for (Object seed : seeds) {
						list.add(createLiteral(seed));
					}
					return list.toArray();
				}
			}).runBare();
		} else if ("contexts".equals(name[0])) {
			(new ApacheSetTestCase(name[1]) {

				@Override
				public Set makeEmptySet() {
					return new LinkedHashModel().filter(createURI("test"), RDF.VALUE, createLiteral("value")).contexts();
				}

				@Override
				public Collection makeConfirmedCollection() {
					return new HashSet();
				}

				@Override
				public Object getOneElement() {
					return createLiteral("new value");
				}

				@Override
				public Object[] convert(Object[] seeds) {
					List<IRI> list = new ArrayList<IRI>();
					for (Object seed : seeds) {
						list.add(createURI(seed));
					}
					return list.toArray();
				}
			}).runBare();
		}
	}

	public abstract Model makeEmptyModel();
}
