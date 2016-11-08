/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.inferencer.fc;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;

import java.util.*;

/**
 * @author Håvard Mikkelsen Ottestad
 */

public class FastRdfsForwardChainingSailConnetion extends AbstractForwardChainingInferencerConnection {


    private final FastRdfsForwardChainingSail fastRdfsForwardChainingSail;
    private final NotifyingSailConnection connection;


    public FastRdfsForwardChainingSailConnetion(FastRdfsForwardChainingSail fastRdfsForwardChainingSail, InferencerConnection e) {
        super(fastRdfsForwardChainingSail, e);
        this.fastRdfsForwardChainingSail = fastRdfsForwardChainingSail;
        this.connection = e;
    }


    void statementCollector(Statement statement) {
        Value object = statement.getObject();
        IRI predicate = statement.getPredicate();
        Resource subject = statement.getSubject();

        if (predicate.equals(RDFS.SUBCLASSOF)) {
            fastRdfsForwardChainingSail.subClassOfStatemenets.add(statement);
        } else if (predicate.equals(RDF.TYPE) && object.equals(RDF.PROPERTY)) {
            fastRdfsForwardChainingSail.propertyStatements.add(statement);
        } else if (predicate.equals(RDFS.SUBPROPERTYOF)) {
            fastRdfsForwardChainingSail.subPropertyOfStatemenets.add(statement);
        } else if (predicate.equals(RDFS.RANGE)) {
            fastRdfsForwardChainingSail.rangeStatemenets.add(statement);
        } else if (predicate.equals(RDFS.DOMAIN)) {
            fastRdfsForwardChainingSail.domainStatemenets.add(statement);
        } else if (predicate.equals(RDF.TYPE) && object.equals(RDFS.CLASS)) {
            fastRdfsForwardChainingSail.subClassOfStatemenets.add(fastRdfsForwardChainingSail.getValueFactory().createStatement(subject, RDFS.SUBCLASSOF, RDFS.RESOURCE));
        } else if (predicate.equals(RDF.TYPE) && object.equals(RDFS.DATATYPE)) {
            fastRdfsForwardChainingSail.subClassOfStatemenets.add(fastRdfsForwardChainingSail.getValueFactory().createStatement(subject, RDFS.SUBCLASSOF, RDFS.LITERAL));
        } else if (predicate.equals(RDF.TYPE) && object.equals(RDFS.CONTAINERMEMBERSHIPPROPERTY)) {
            fastRdfsForwardChainingSail.subPropertyOfStatemenets.add(fastRdfsForwardChainingSail.getValueFactory().createStatement(subject, RDFS.SUBPROPERTYOF, RDFS.MEMBER));
        }

        Statement statement1 = fastRdfsForwardChainingSail.getValueFactory().createStatement(predicate, RDF.TYPE, RDF.PROPERTY, statement.getContext());
        fastRdfsForwardChainingSail.propertyStatements.add(statement1);
        Statement statement2 = fastRdfsForwardChainingSail.getValueFactory().createStatement(predicate, RDFS.SUBPROPERTYOF, predicate, statement.getContext());
        fastRdfsForwardChainingSail.subPropertyOfStatemenets.add(statement2);


    }

    void calculateInferenceMaps() {
        calculateSubClassOf(fastRdfsForwardChainingSail.subClassOfStatemenets);
        findProperties(fastRdfsForwardChainingSail.propertyStatements);
        calculateSubPropertyOf(fastRdfsForwardChainingSail.subPropertyOfStatemenets);

        calculateRangeDomain(fastRdfsForwardChainingSail.rangeStatemenets, fastRdfsForwardChainingSail.calculatedRange);
        calculateRangeDomain(fastRdfsForwardChainingSail.domainStatemenets, fastRdfsForwardChainingSail.calculatedDomain);


        fastRdfsForwardChainingSail.calculatedTypes.forEach((subClass, superClasses) -> {
            addInferredStatement(subClass, RDFS.SUBCLASSOF, subClass);

            superClasses.forEach(superClass -> {
                addInferredStatement(subClass, RDFS.SUBCLASSOF, superClass);
                addInferredStatement(superClass, RDFS.SUBCLASSOF, superClass);

            });
        });

        fastRdfsForwardChainingSail.calculatedProperties.forEach((sub, sups) -> {
            addInferredStatement(sub, RDFS.SUBPROPERTYOF, sub);

            sups.forEach(sup -> {
                addInferredStatement(sub, RDFS.SUBPROPERTYOF, sup);
                addInferredStatement(sup, RDFS.SUBPROPERTYOF, sup);

            });
        });

    }

    private Set<Resource> resolveTypes(Resource value) {
        Set<Resource> iris = fastRdfsForwardChainingSail.calculatedTypes.get(value);

        return iris != null ? iris : Collections.emptySet();
    }

    private Set<IRI> resolveProperties(IRI predicate) {
        Set<IRI> iris = fastRdfsForwardChainingSail.calculatedProperties.get(predicate);

        return iris != null ? iris : Collections.emptySet();
    }


    private Set<Resource> resolveRangeTypes(IRI predicate) {
        Set<Resource> iris = fastRdfsForwardChainingSail.calculatedRange.get(predicate);

        return iris != null ? iris : Collections.emptySet();
    }

    private Set<Resource> resolveDomainTypes(IRI predicate) {
        Set<Resource> iris = fastRdfsForwardChainingSail.calculatedDomain.get(predicate);

        return iris != null ? iris : Collections.emptySet();
    }


    private void calculateSubClassOf(List<Statement> subClassOfStatements) {
        subClassOfStatements.forEach(s -> {
            Resource subClass = s.getSubject();
            if (!fastRdfsForwardChainingSail.calculatedTypes.containsKey(subClass)) {
                fastRdfsForwardChainingSail.calculatedTypes.put(subClass, new HashSet<>());
            }

            fastRdfsForwardChainingSail.calculatedTypes.get(subClass).add((Resource) s.getObject());

        });

        long prevSize = 0;
        final long[] newSize = {-1};
        while (prevSize != newSize[0]) {

            prevSize = newSize[0];

            newSize[0] = 0;

            fastRdfsForwardChainingSail.calculatedTypes.forEach((key, value) -> {
                List<Resource> temp = new ArrayList<Resource>();
                value.forEach(superClass -> {
                    temp
                        .addAll(resolveTypes(superClass));
                });

                value.addAll(temp);
                newSize[0] += value.size();
            });


        }
    }

    private void findProperties(List<Statement> propertyStatements) {
        propertyStatements.forEach(statement -> {
            IRI predicate = (IRI) statement.getSubject();
            if (!fastRdfsForwardChainingSail.calculatedProperties.containsKey(predicate)) {
                addInferredStatement(predicate, RDF.TYPE, RDF.PROPERTY, statement.getContext());
                fastRdfsForwardChainingSail.calculatedProperties.put(predicate, new HashSet<>());
            }

        });
    }


    private void calculateSubPropertyOf(List<Statement> subPropertyOfStatemenets) {

        subPropertyOfStatemenets.forEach(s -> {
            IRI subClass = (IRI) s.getSubject();
            IRI superClass = (IRI) s.getObject();
            if (!fastRdfsForwardChainingSail.calculatedProperties.containsKey(subClass)) {
                fastRdfsForwardChainingSail.calculatedProperties.put(subClass, new HashSet<>());
            }

            if (!fastRdfsForwardChainingSail.calculatedProperties.containsKey(superClass)) {
                fastRdfsForwardChainingSail.calculatedProperties.put(superClass, new HashSet<>());
            }

            fastRdfsForwardChainingSail.calculatedProperties.get(subClass).add((IRI) s.getObject());

        });


        long prevSize = 0;
        final long[] newSize = {-1};
        while (prevSize != newSize[0]) {

            prevSize = newSize[0];

            newSize[0] = 0;

            fastRdfsForwardChainingSail.calculatedProperties.forEach((key, value) -> {
                List<IRI> temp = new ArrayList<IRI>();
                value.forEach(superProperty -> {
                    temp.addAll(resolveProperties(superProperty));
                });

                value.addAll(temp);
                newSize[0] += value.size();
            });


        }
    }

    private void calculateRangeDomain(List<Statement> rangeOrDomainStatements, Map<IRI, Set<Resource>> calculatedRangeOrDomain) {

        rangeOrDomainStatements.forEach(s -> {
            IRI predicate = (IRI) s.getSubject();
            if (!fastRdfsForwardChainingSail.calculatedProperties.containsKey(predicate)) {
                fastRdfsForwardChainingSail.calculatedProperties.put(predicate, new HashSet<>());
            }

            if (!calculatedRangeOrDomain.containsKey(predicate)) {
                calculatedRangeOrDomain.put(predicate, new HashSet<>());
            }

            calculatedRangeOrDomain.get(predicate).add((Resource) s.getObject());

            if (!fastRdfsForwardChainingSail.calculatedTypes.containsKey(s.getObject())) {
                fastRdfsForwardChainingSail.calculatedTypes.put((Resource) s.getObject(), new HashSet<>());
            }

        });


        fastRdfsForwardChainingSail.calculatedProperties
            .keySet()
            .stream()
            .filter(key -> !calculatedRangeOrDomain.containsKey(key))
            .forEach(key -> calculatedRangeOrDomain.put(key, new HashSet<>()));

        long prevSize = 0;
        final long[] newSize = {-1};
        while (prevSize != newSize[0]) {

            prevSize = newSize[0];

            newSize[0] = 0;

            calculatedRangeOrDomain.forEach((key, value) -> {
                List<Resource> resolvedBySubProperty = new ArrayList<>();
                resolveProperties(key).forEach(newPredicate -> {
                    Set<Resource> iris = calculatedRangeOrDomain.get(newPredicate);
                    if (iris != null) {
                        resolvedBySubProperty.addAll(iris);
                    }

                });

                List<Resource> resolvedBySubClass = new ArrayList<>();
                value.addAll(resolvedBySubProperty);


                value.stream().map(this::resolveTypes).forEach(resolvedBySubClass::addAll);

                value.addAll(resolvedBySubClass);

                newSize[0] += value.size();
            });


        }
    }


    boolean inferredCleared = true;

    @Override
    public void clearInferred(Resource... contexts) throws SailException {
        super.clearInferred(contexts);
        inferredCleared = true;
    }

    @Override
    protected void doInferencing() throws SailException {
        prepareIteration();

        if (fastRdfsForwardChainingSail.schema == null) {

            fastRdfsForwardChainingSail.clearInferenceTables();

            try (CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(null, null, null, false)) {
                while (statements.hasNext()) {
                    Statement next = statements.next();
                    statementCollector(next);
                }
            }
            calculateInferenceMaps();
            inferredCleared = true;

        }

        if (!inferredCleared) {
            return;
        }


        try (CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(null, null, null, false)) {
            while (statements.hasNext()) {
                Statement next = statements.next();
                addStatement(false, next.getSubject(), next.getPredicate(), next.getObject(), next.getContext());
            }
        }
        inferredCleared = false;

    }


    @Override
    protected Model createModel() {
        return new Model() {
            @Override
            public Model unmodifiable() {
                return null;
            }

            @Override
            public Set<Namespace> getNamespaces() {
                return null;
            }

            @Override
            public void setNamespace(Namespace namespace) {

            }

            @Override
            public Optional<Namespace> removeNamespace(String s) {
                return null;
            }

            @Override
            public boolean contains(Resource resource, IRI iri, Value value, Resource... resources) {
                return false;
            }

            @Override
            public boolean add(Resource resource, IRI iri, Value value, Resource... resources) {
                return false;
            }

            @Override
            public boolean clear(Resource... resources) {
                return false;
            }

            @Override
            public boolean remove(Resource resource, IRI iri, Value value, Resource... resources) {
                return false;
            }

            @Override
            public Model filter(Resource resource, IRI iri, Value value, Resource... resources) {
                return null;
            }

            @Override
            public Set<Resource> subjects() {
                return null;
            }

            @Override
            public Set<IRI> predicates() {
                return null;
            }

            @Override
            public Set<Value> objects() {
                return null;
            }

            @Override
            public ValueFactory getValueFactory() {
                return null;
            }

            @Override
            public Iterator<Statement> match(Resource resource, IRI iri, Value value, Resource... resources) {
                return null;
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<Statement> iterator() {
                return null;
            }

            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return null;
            }

            @Override
            public boolean add(Statement statement) {
                return false;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(Collection<? extends Statement> c) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {

            }
        };
    }


    protected int applyRules(Model model) throws SailException {


        return 0;
    }


    public void addStatement(Resource subject, IRI predicate, Value object, Resource... resources) throws SailException {
        addStatement(true, subject, predicate, object, resources);
    }


    public void addStatement(boolean actuallyAdd, Resource subject, IRI predicate, Value object, Resource... resources) throws SailException {

        final boolean[] inferRdfTypeSubject = {false};
        final boolean[] inferRdfTypeObject = {false};

        if (fastRdfsForwardChainingSail.useAllRdfsRules) {
            addInferredStatement(subject, RDF.TYPE, RDFS.RESOURCE, resources);

            if (object instanceof Resource) {
                addInferredStatement((Resource) object, RDF.TYPE, RDFS.RESOURCE, resources);
            }
        }


        if (predicate.getNamespace().equals(RDF.NAMESPACE) && predicate.getLocalName().charAt(0) == '_') {

            try {
                int i = Integer.parseInt(predicate.getLocalName().substring(1));
                if (i >= 1) {
                    addInferredStatement(subject, RDFS.MEMBER, object, resources);

                    addInferredStatement(predicate, RDF.TYPE, RDFS.RESOURCE, resources);
                    addInferredStatement(predicate, RDF.TYPE, RDFS.CONTAINERMEMBERSHIPPROPERTY, resources);
                    addInferredStatement(predicate, RDF.TYPE, RDF.PROPERTY, resources);
                    addInferredStatement(predicate, RDFS.SUBPROPERTYOF, predicate, resources);
                    addInferredStatement(predicate, RDFS.SUBPROPERTYOF, RDFS.MEMBER, resources);

                }
            } catch (NumberFormatException e) {
                // Ignore exception.
            }

        }

        if (actuallyAdd) {
            connection.addStatement(subject, predicate, object, resources);

        }

        if (predicate.equals(RDF.TYPE)) {

            resolveTypes((Resource) object)
                .stream()
                .peek(inferredType -> {
                    if (fastRdfsForwardChainingSail.useAllRdfsRules && inferredType.equals(RDFS.CLASS)) {
                        addInferredStatement(subject, RDFS.SUBCLASSOF, RDFS.RESOURCE, resources);
                    }

                    inferRdfTypeSubject[0] = true;
                })
                .filter(inferredType -> !inferredType.equals(object))
                .forEach(inferredType -> addInferredStatement(subject, RDF.TYPE, inferredType, resources));
        }

        resolveProperties(predicate)
            .stream()
            .filter(inferredProperty -> !inferredProperty.equals(predicate))
            .forEach(inferredProperty -> addInferredStatement(subject, inferredProperty, object, resources));


        if (object instanceof Resource) {
            resolveRangeTypes(predicate)
                .stream()
                .peek(inferredType -> {
                    if (fastRdfsForwardChainingSail.useAllRdfsRules && inferredType.equals(RDFS.CLASS)) {
                        addInferredStatement(((Resource) object), RDFS.SUBCLASSOF, RDFS.RESOURCE, resources);
                    }
                    inferRdfTypeObject[0] = true;

                })
                .forEach(inferredType -> addInferredStatement(((Resource) object), RDF.TYPE, inferredType, resources));
        }


        resolveDomainTypes((IRI) predicate)
            .stream()
            .peek(inferredType -> {
                if (fastRdfsForwardChainingSail.useAllRdfsRules && inferredType.equals(RDFS.CLASS)) {
                    addInferredStatement(subject, RDFS.SUBCLASSOF, RDFS.RESOURCE, resources);
                }
                inferRdfTypeSubject[0] = true;

            })
            .forEach(inferredType -> addInferredStatement((subject), RDF.TYPE, inferredType, resources));

        if (inferRdfTypeSubject[0]) {
            addInferredStatement(subject, RDF.TYPE, RDFS.RESOURCE, resources);

        }

        if (inferRdfTypeObject[0]) {
            addInferredStatement(((Resource) object), RDF.TYPE, RDFS.RESOURCE, resources);

        }

    }

    @Override
    public boolean addInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
        return super.addInferredStatement(subj, pred, obj);
    }

    protected void addAxiomStatements() {
//        StringBuilder axioms = new StringBuilder();
//
//        try {
//
//
//            RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
//            parser.setRDFHandler(new RDFHandler() {
//                @Override
//                public void startRDF() throws RDFHandlerException {
//
//                }
//
//                @Override
//                public void endRDF() throws RDFHandlerException {
//
//                }
//
//                @Override
//                public void handleNamespace(String s11, String s1) throws RDFHandlerException {
//
//                }
//
//                @Override
//                public void handleStatement(Statement statement) throws RDFHandlerException {
//
//                    SimpleValueFactory vf = SimpleValueFactory.getInstance();
//
//
//                    axioms.append("statement = vf.createStatement(vf.createIRI(\"" + statement.getSubject() + "\"), vf.createIRI(\"" + statement.getPredicate() + "\"), vf.createIRI(\"" + statement.getObject() + "\"));\n");
//                    axioms.append("statementCollector(statement);").append("\n");
//                    axioms.append(" addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());").append("\n");
//
//                    statementCollector(statement);
//                    addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
//                }
//
//                @Override
//                public void handleComment(String s1) throws RDFHandlerException {
//
//                }
//            });
//
//            parser.parse(new ByteArrayInputStream(baseRDFS.getBytes("UTF-8")), "");
//
//
//        } catch (IOException ignored) {
//        }


        ValueFactory vf = fastRdfsForwardChainingSail.getValueFactory();

        Statement statement = vf.createStatement(RDF.ALT, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.ALT, RDF.TYPE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.ALT, RDFS.SUBCLASSOF, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.ALT, RDFS.SUBCLASSOF, RDFS.CONTAINER);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.ALT, RDFS.SUBCLASSOF, RDF.ALT);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.BAG, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.BAG, RDF.TYPE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.BAG, RDFS.SUBCLASSOF, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.BAG, RDFS.SUBCLASSOF, RDFS.CONTAINER);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.BAG, RDFS.SUBCLASSOF, RDF.BAG);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.LIST, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.LIST, RDF.TYPE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.LIST, RDFS.SUBCLASSOF, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.LIST, RDFS.SUBCLASSOF, RDF.LIST);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.PROPERTY, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.PROPERTY, RDF.TYPE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.PROPERTY, RDFS.SUBCLASSOF, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.PROPERTY, RDFS.SUBCLASSOF, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.SEQ, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.SEQ, RDF.TYPE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.SEQ, RDFS.SUBCLASSOF, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.SEQ, RDFS.SUBCLASSOF, RDFS.CONTAINER);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.SEQ, RDFS.SUBCLASSOF, RDF.SEQ);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.STATEMENT, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.STATEMENT, RDF.TYPE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.STATEMENT, RDFS.SUBCLASSOF, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.STATEMENT, RDFS.SUBCLASSOF, RDF.STATEMENT);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.XMLLITERAL, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.XMLLITERAL, RDF.TYPE, RDFS.DATATYPE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.XMLLITERAL, RDF.TYPE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.XMLLITERAL, RDFS.SUBCLASSOF, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.XMLLITERAL, RDFS.SUBCLASSOF, RDFS.LITERAL);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.XMLLITERAL, RDFS.SUBCLASSOF, RDF.XMLLITERAL);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.FIRST, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.FIRST, RDF.TYPE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.FIRST, RDFS.DOMAIN, RDF.LIST);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.FIRST, RDFS.RANGE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.FIRST, RDFS.SUBPROPERTYOF, RDF.FIRST);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.NIL, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.NIL, RDF.TYPE, RDF.LIST);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.OBJECT, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.OBJECT, RDF.TYPE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.OBJECT, RDFS.DOMAIN, RDF.STATEMENT);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.OBJECT, RDFS.RANGE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.OBJECT, RDFS.SUBPROPERTYOF, RDF.OBJECT);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.PREDICATE, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.PREDICATE, RDF.TYPE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.PREDICATE, RDFS.DOMAIN, RDF.STATEMENT);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.PREDICATE, RDFS.RANGE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.PREDICATE, RDFS.SUBPROPERTYOF, RDF.PREDICATE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.REST, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.REST, RDF.TYPE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.REST, RDFS.DOMAIN, RDF.LIST);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.REST, RDFS.RANGE, RDF.LIST);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.REST, RDFS.SUBPROPERTYOF, RDF.REST);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.SUBJECT, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.SUBJECT, RDF.TYPE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.SUBJECT, RDFS.DOMAIN, RDF.STATEMENT);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.SUBJECT, RDFS.RANGE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.SUBJECT, RDFS.SUBPROPERTYOF, RDF.SUBJECT);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.TYPE, RDFS.DOMAIN, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.TYPE, RDFS.RANGE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.TYPE, RDFS.SUBPROPERTYOF, RDF.TYPE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.VALUE, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.VALUE, RDF.TYPE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.VALUE, RDFS.DOMAIN, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.VALUE, RDFS.RANGE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDF.VALUE, RDFS.SUBPROPERTYOF, RDF.VALUE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.CLASS, RDF.TYPE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.CLASS, RDFS.SUBCLASSOF, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.CLASS, RDFS.SUBCLASSOF, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.CONTAINER, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.CONTAINER, RDF.TYPE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.CONTAINER, RDFS.SUBCLASSOF, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.CONTAINER, RDFS.SUBCLASSOF, RDFS.CONTAINER);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDF.TYPE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDFS.SUBCLASSOF, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDFS.SUBCLASSOF, RDFS.CONTAINERMEMBERSHIPPROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDFS.SUBCLASSOF, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.DATATYPE, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.DATATYPE, RDF.TYPE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.DATATYPE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.DATATYPE, RDFS.SUBCLASSOF, RDFS.DATATYPE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.DATATYPE, RDFS.SUBCLASSOF, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.LITERAL, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.LITERAL, RDF.TYPE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.LITERAL, RDFS.SUBCLASSOF, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.LITERAL, RDFS.SUBCLASSOF, RDFS.LITERAL);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.RESOURCE, RDF.TYPE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.RESOURCE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.COMMENT, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.COMMENT, RDF.TYPE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.COMMENT, RDFS.DOMAIN, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.COMMENT, RDFS.RANGE, RDFS.LITERAL);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.COMMENT, RDFS.SUBPROPERTYOF, RDFS.COMMENT);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.DOMAIN, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.DOMAIN, RDF.TYPE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.DOMAIN, RDFS.DOMAIN, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.DOMAIN, RDFS.RANGE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.DOMAIN, RDFS.SUBPROPERTYOF, RDFS.DOMAIN);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.ISDEFINEDBY, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.ISDEFINEDBY, RDF.TYPE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.ISDEFINEDBY, RDFS.DOMAIN, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.ISDEFINEDBY, RDFS.RANGE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.ISDEFINEDBY, RDFS.SUBPROPERTYOF, RDFS.SEEALSO);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.ISDEFINEDBY, RDFS.SUBPROPERTYOF, RDFS.ISDEFINEDBY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.LABEL, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.LABEL, RDF.TYPE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.LABEL, RDFS.DOMAIN, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.LABEL, RDFS.RANGE, RDFS.LITERAL);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.LABEL, RDFS.SUBPROPERTYOF, RDFS.LABEL);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.MEMBER, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.MEMBER, RDF.TYPE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.MEMBER, RDFS.DOMAIN, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.MEMBER, RDFS.RANGE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.MEMBER, RDFS.SUBPROPERTYOF, RDFS.MEMBER);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.RANGE, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.RANGE, RDF.TYPE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.RANGE, RDFS.DOMAIN, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.RANGE, RDFS.RANGE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.RANGE, RDFS.SUBPROPERTYOF, RDFS.RANGE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.SEEALSO, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.SEEALSO, RDF.TYPE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.SEEALSO, RDFS.DOMAIN, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.SEEALSO, RDFS.RANGE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.SEEALSO, RDFS.SUBPROPERTYOF, RDFS.SEEALSO);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.SUBCLASSOF, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.SUBCLASSOF, RDF.TYPE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.SUBCLASSOF, RDFS.DOMAIN, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.SUBCLASSOF, RDFS.RANGE, RDFS.CLASS);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.SUBCLASSOF, RDFS.SUBPROPERTYOF, RDFS.SUBCLASSOF);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.SUBPROPERTYOF, RDF.TYPE, RDFS.RESOURCE);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.SUBPROPERTYOF, RDF.TYPE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.SUBPROPERTYOF, RDFS.DOMAIN, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.SUBPROPERTYOF, RDFS.RANGE, RDF.PROPERTY);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
        statement = vf.createStatement(RDFS.SUBPROPERTYOF, RDFS.SUBPROPERTYOF, RDFS.SUBPROPERTYOF);
        statementCollector(statement);
        addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());


    }


}
