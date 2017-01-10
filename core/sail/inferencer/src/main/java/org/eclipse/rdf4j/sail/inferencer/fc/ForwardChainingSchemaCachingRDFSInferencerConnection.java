/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.inferencer.fc;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UnknownSailTransactionStateException;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Håvard Mikkelsen Ottestad
 */

public class ForwardChainingSchemaCachingRDFSInferencerConnection extends AbstractForwardChainingInferencerConnection {


    private final ForwardChainingSchemaCachingRDFSInferencer forwardChainingSchemaCachingRDFSInferencer;
    private final NotifyingSailConnection connection;
    long lockStamp = 0;


    ForwardChainingSchemaCachingRDFSInferencerConnection(ForwardChainingSchemaCachingRDFSInferencer forwardChainingSchemaCachingRDFSInferencer, InferencerConnection e) {
        super(forwardChainingSchemaCachingRDFSInferencer, e);
        this.forwardChainingSchemaCachingRDFSInferencer = forwardChainingSchemaCachingRDFSInferencer;
        this.connection = e;
    }

    @Override
    public void rollback() throws SailException {
        super.rollback();
        if (lockStamp != 0) {
            forwardChainingSchemaCachingRDFSInferencer.releaseLock(this);
        }
        //@TODO Do I need to clean up the tbox cache and lookup maps after rolling back? Probably if the connection has a write lock.
    }

    void statementCollector(Statement statement) {
        Value object = statement.getObject();
        IRI predicate = statement.getPredicate();
        Resource subject = statement.getSubject();

        if (predicate.equals(RDFS.SUBCLASSOF)) {
            forwardChainingSchemaCachingRDFSInferencer.upgradeLock(this);
            forwardChainingSchemaCachingRDFSInferencer.subClassOfStatements.add(statement);
            forwardChainingSchemaCachingRDFSInferencer.types.add((Resource) object);
            forwardChainingSchemaCachingRDFSInferencer.types.add(subject);

        } else if (predicate.equals(RDF.TYPE) && object.equals(RDF.PROPERTY)) {
            forwardChainingSchemaCachingRDFSInferencer.upgradeLock(this);
            forwardChainingSchemaCachingRDFSInferencer.properties.add(statement.getSubject());

        } else if (predicate.equals(RDFS.SUBPROPERTYOF)) {
            forwardChainingSchemaCachingRDFSInferencer.upgradeLock(this);
            forwardChainingSchemaCachingRDFSInferencer.subPropertyOfStatements.add(statement);
            forwardChainingSchemaCachingRDFSInferencer.properties.add(subject);
            forwardChainingSchemaCachingRDFSInferencer.properties.add((Resource) object);

        } else if (predicate.equals(RDFS.RANGE)) {
            forwardChainingSchemaCachingRDFSInferencer.upgradeLock(this);
            forwardChainingSchemaCachingRDFSInferencer.rangeStatements.add(statement);
            forwardChainingSchemaCachingRDFSInferencer.properties.add(subject);
            forwardChainingSchemaCachingRDFSInferencer.types.add((Resource) object);

        } else if (predicate.equals(RDFS.DOMAIN)) {
            forwardChainingSchemaCachingRDFSInferencer.upgradeLock(this);
            forwardChainingSchemaCachingRDFSInferencer.domainStatements.add(statement);
            forwardChainingSchemaCachingRDFSInferencer.properties.add(subject);
            forwardChainingSchemaCachingRDFSInferencer.types.add((Resource) object);

        } else if (predicate.equals(RDF.TYPE) && object.equals(RDFS.CLASS)) {
            forwardChainingSchemaCachingRDFSInferencer.upgradeLock(this);
            forwardChainingSchemaCachingRDFSInferencer.subClassOfStatements.add(forwardChainingSchemaCachingRDFSInferencer.getValueFactory().createStatement(subject, RDFS.SUBCLASSOF, RDFS.RESOURCE));
            forwardChainingSchemaCachingRDFSInferencer.types.add(subject);

        } else if (predicate.equals(RDF.TYPE) && object.equals(RDFS.DATATYPE)) {
            forwardChainingSchemaCachingRDFSInferencer.upgradeLock(this);
            forwardChainingSchemaCachingRDFSInferencer.subClassOfStatements.add(forwardChainingSchemaCachingRDFSInferencer.getValueFactory().createStatement(subject, RDFS.SUBCLASSOF, RDFS.LITERAL));

        } else if (predicate.equals(RDF.TYPE) && object.equals(RDFS.CONTAINERMEMBERSHIPPROPERTY)) {
            forwardChainingSchemaCachingRDFSInferencer.upgradeLock(this);
            forwardChainingSchemaCachingRDFSInferencer.subPropertyOfStatements.add(forwardChainingSchemaCachingRDFSInferencer.getValueFactory().createStatement(subject, RDFS.SUBPROPERTYOF, RDFS.MEMBER));
            forwardChainingSchemaCachingRDFSInferencer.properties.add(subject);

        }else if(predicate.equals(RDF.TYPE)){
            if(!forwardChainingSchemaCachingRDFSInferencer.types.contains(((Resource) object))){
                forwardChainingSchemaCachingRDFSInferencer.upgradeLock(this);
                forwardChainingSchemaCachingRDFSInferencer.types.add((Resource) object);
            }
        }

        if (!forwardChainingSchemaCachingRDFSInferencer.properties.contains(predicate)) {
            forwardChainingSchemaCachingRDFSInferencer.upgradeLock(this);
            forwardChainingSchemaCachingRDFSInferencer.properties.add(predicate);
        }

    }

    void calculateInferenceMaps() {
        forwardChainingSchemaCachingRDFSInferencer.upgradeLock(this);

        calculateSubClassOf(forwardChainingSchemaCachingRDFSInferencer.subClassOfStatements);
        findProperties(forwardChainingSchemaCachingRDFSInferencer.properties);
        calculateSubPropertyOf(forwardChainingSchemaCachingRDFSInferencer.subPropertyOfStatements);

        calculateRangeDomain(forwardChainingSchemaCachingRDFSInferencer.rangeStatements, forwardChainingSchemaCachingRDFSInferencer.calculatedRange);
        calculateRangeDomain(forwardChainingSchemaCachingRDFSInferencer.domainStatements, forwardChainingSchemaCachingRDFSInferencer.calculatedDomain);


        forwardChainingSchemaCachingRDFSInferencer.calculatedTypes.forEach((subClass, superClasses) -> {
            addInferredStatement(subClass, RDFS.SUBCLASSOF, subClass);

            superClasses.forEach(superClass -> {
                addInferredStatement(subClass, RDFS.SUBCLASSOF, superClass);
                addInferredStatement(superClass, RDFS.SUBCLASSOF, superClass);

            });
        });

        forwardChainingSchemaCachingRDFSInferencer.calculatedProperties.forEach((sub, sups) -> {
            addInferredStatement(sub, RDFS.SUBPROPERTYOF, sub);

            sups.forEach(sup -> {
                addInferredStatement(sub, RDFS.SUBPROPERTYOF, sup);
                addInferredStatement(sup, RDFS.SUBPROPERTYOF, sup);

            });
        });


    }

    private Set<Resource> resolveTypes(Resource value) {
        Set<Resource> iris = forwardChainingSchemaCachingRDFSInferencer.calculatedTypes.get(value);

        return iris != null ? iris : Collections.emptySet();
    }

    private Set<Resource> resolveProperties(Resource predicate) {
        Set<Resource> iris = forwardChainingSchemaCachingRDFSInferencer.calculatedProperties.get(predicate);

        return iris != null ? iris : Collections.emptySet();
    }


    private Set<Resource> resolveRangeTypes(IRI predicate) {
        Set<Resource> iris = forwardChainingSchemaCachingRDFSInferencer.calculatedRange.get(predicate);

        return iris != null ? iris : Collections.emptySet();
    }

    private Set<Resource> resolveDomainTypes(IRI predicate) {
        Set<Resource> iris = forwardChainingSchemaCachingRDFSInferencer.calculatedDomain.get(predicate);

        return iris != null ? iris : Collections.emptySet();
    }


    private void calculateSubClassOf(Collection<Statement> subClassOfStatements) {
        forwardChainingSchemaCachingRDFSInferencer.types.forEach(type -> {
            if (!forwardChainingSchemaCachingRDFSInferencer.calculatedTypes.containsKey(type)) {
                forwardChainingSchemaCachingRDFSInferencer.calculatedTypes.put(type, new HashSet<>());
            }

            forwardChainingSchemaCachingRDFSInferencer.calculatedTypes.get(type).add(type);

        });


        subClassOfStatements.forEach(s -> {
            Resource subClass = s.getSubject();
            if (!forwardChainingSchemaCachingRDFSInferencer.calculatedTypes.containsKey(subClass)) {
                forwardChainingSchemaCachingRDFSInferencer.calculatedTypes.put(subClass, new HashSet<>());
            }

            forwardChainingSchemaCachingRDFSInferencer.calculatedTypes.get(subClass).add((Resource) s.getObject());

        });


        // Fixed point approach to finding all sub-classes.
        // prevSize is the size of the previous application of the function
        // newSize is the size of the current application of the function
        // Fixed point is reached when they are the same.
        // Eg. Two consecutive applications return the same number of subclasses
        long prevSize = 0;
        final long[] newSize = {-1};
        while (prevSize != newSize[0]) {

            prevSize = newSize[0];

            newSize[0] = 0;

            forwardChainingSchemaCachingRDFSInferencer.calculatedTypes.forEach((key, value) -> {
                List<Resource> temp = new ArrayList<>();
                value.forEach(superClass -> temp
                    .addAll(resolveTypes(superClass)));

                value.addAll(temp);
                newSize[0] += value.size();
            });


        }
    }

    private void findProperties(Collection<Resource> predicates) {
        predicates.forEach(predicate -> {
            addInferredStatement(predicate, RDF.TYPE, RDF.PROPERTY);
            forwardChainingSchemaCachingRDFSInferencer.calculatedProperties.put(predicate, new HashSet<>());
        });
    }


    private void calculateSubPropertyOf(Collection<Statement> subPropertyOfStatemenets) {

        subPropertyOfStatemenets.forEach(s -> {
            Resource subClass = s.getSubject();
            Resource superClass = (Resource) s.getObject();
            if (!forwardChainingSchemaCachingRDFSInferencer.calculatedProperties.containsKey(subClass)) {
                forwardChainingSchemaCachingRDFSInferencer.calculatedProperties.put(subClass, new HashSet<>());
            }

            if (!forwardChainingSchemaCachingRDFSInferencer.calculatedProperties.containsKey(superClass)) {
                forwardChainingSchemaCachingRDFSInferencer.calculatedProperties.put(superClass, new HashSet<>());
            }

            forwardChainingSchemaCachingRDFSInferencer.calculatedProperties.get(subClass).add((Resource) s.getObject());

        });

        // Fixed point approach to finding all sub-properties.
        // prevSize is the size of the previous application of the function
        // newSize is the size of the current application of the function
        // Fixed point is reached when they are the same.
        long prevSize = 0;
        final long[] newSize = {-1};
        while (prevSize != newSize[0]) {

            prevSize = newSize[0];

            newSize[0] = 0;

            forwardChainingSchemaCachingRDFSInferencer.calculatedProperties.forEach((key, value) -> {
                List<Resource> temp = new ArrayList<>();
                value.forEach(superProperty -> temp.addAll(resolveProperties(superProperty)));

                value.addAll(temp);
                newSize[0] += value.size();
            });


        }
    }

    private void calculateRangeDomain(Collection<Statement> rangeOrDomainStatements, Map<Resource, Set<Resource>> calculatedRangeOrDomain) {

        rangeOrDomainStatements.forEach(s -> {
            Resource predicate = s.getSubject();
            if (!forwardChainingSchemaCachingRDFSInferencer.calculatedProperties.containsKey(predicate)) {
                forwardChainingSchemaCachingRDFSInferencer.calculatedProperties.put(predicate, new HashSet<>());
            }

            if (!calculatedRangeOrDomain.containsKey(predicate)) {
                calculatedRangeOrDomain.put(predicate, new HashSet<>());
            }

            calculatedRangeOrDomain.get(predicate).add((Resource) s.getObject());

            if (!forwardChainingSchemaCachingRDFSInferencer.calculatedTypes.containsKey(s.getObject())) {
                forwardChainingSchemaCachingRDFSInferencer.calculatedTypes.put((Resource) s.getObject(), new HashSet<>());
            }

        });


        forwardChainingSchemaCachingRDFSInferencer.calculatedProperties
            .keySet()
            .stream()
            .filter(key -> !calculatedRangeOrDomain.containsKey(key))
            .forEach(key -> calculatedRangeOrDomain.put(key, new HashSet<>()));

        // Fixed point approach to finding all ranges or domains.
        // prevSize is the size of the previous application of the function
        // newSize is the size of the current application of the function
        // Fixed point is reached when they are the same.
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


    private boolean inferredCleared = false;

    @Override
    public void clearInferred(Resource... contexts) throws SailException {
        super.clearInferred(contexts);
        inferredCleared = true;
    }

    private long origianlTboxCount = -1;


    @Override
    public void begin() throws SailException {
        super.begin();


    }

    @Override
    public void begin(IsolationLevel level) throws UnknownSailTransactionStateException {
        super.begin(level);
        forwardChainingSchemaCachingRDFSInferencer.readLock(this);

        origianlTboxCount = getTboxCount();

    }

    @Override
    public void commit() throws SailException {
        super.commit();
        forwardChainingSchemaCachingRDFSInferencer.releaseLock(this);
    }

    private long getTboxCount() {
        return forwardChainingSchemaCachingRDFSInferencer.subClassOfStatements.size()
        + forwardChainingSchemaCachingRDFSInferencer.properties.size()
        + forwardChainingSchemaCachingRDFSInferencer.subPropertyOfStatements.size()
        + forwardChainingSchemaCachingRDFSInferencer.rangeStatements.size()
        + forwardChainingSchemaCachingRDFSInferencer.domainStatements.size()
        + forwardChainingSchemaCachingRDFSInferencer.types.size();
    }

    @Override
    protected void doInferencing() throws SailException {
        prepareIteration();

        if (forwardChainingSchemaCachingRDFSInferencer.schema == null && origianlTboxCount != getTboxCount()) {

            forwardChainingSchemaCachingRDFSInferencer.upgradeLock(this);

            forwardChainingSchemaCachingRDFSInferencer.clearInferenceTables();
            addAxiomStatements();

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
    public void close() throws SailException {
        if(lockStamp != 0){
            forwardChainingSchemaCachingRDFSInferencer.releaseLock(this);
        }
        super.close();

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

    @Override
    protected int applyRules(Model model) throws SailException {

        // Required by extended class

        // Not used here because rules are usually applied while adding data
        // and not at the end of a transaction

        return 0;
    }


    public void addStatement(Resource subject, IRI predicate, Value object, Resource... contexts) throws SailException {
        addStatement(true, subject, predicate, object, contexts);
    }

    @Override
    public boolean addInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
        return super.addInferredStatement(subj, pred, obj, contexts);
    }

    // actuallyAdd
    private void addStatement(boolean actuallyAdd, Resource subject, IRI predicate, Value object, Resource... resources) throws SailException {

        if (forwardChainingSchemaCachingRDFSInferencer.schema == null) {
            statementCollector(forwardChainingSchemaCachingRDFSInferencer.getValueFactory().createStatement(subject, predicate, object));
        }

        if (forwardChainingSchemaCachingRDFSInferencer.useAllRdfsRules) {
            addInferredStatement(subject, RDF.TYPE, RDFS.RESOURCE);

            if (object instanceof Resource) {
                addInferredStatement((Resource) object, RDF.TYPE, RDFS.RESOURCE);
            }
        }


        if (predicate.getNamespace().equals(RDF.NAMESPACE) && predicate.getLocalName().charAt(0) == '_') {

            try {
                int i = Integer.parseInt(predicate.getLocalName().substring(1));
                if (i >= 1) {
                    addInferredStatement(subject, RDFS.MEMBER, object);

                    addInferredStatement(predicate, RDF.TYPE, RDFS.RESOURCE);
                    addInferredStatement(predicate, RDF.TYPE, RDFS.CONTAINERMEMBERSHIPPROPERTY);
                    addInferredStatement(predicate, RDF.TYPE, RDF.PROPERTY);
                    addInferredStatement(predicate, RDFS.SUBPROPERTYOF, predicate);
                    addInferredStatement(predicate, RDFS.SUBPROPERTYOF, RDFS.MEMBER);

                }
            } catch (NumberFormatException e) {
                // Ignore exception.

                // Means that the predicate started with rdf:_ but does not
                // comply with the container membership format of rdf:_nnn
                // and we can safely ignore this exception since it just means
                // that we didn't need to infer anything about container membership
            }

        }

        if (actuallyAdd) {
            connection.addStatement(subject, predicate, object, resources);

        }

        if (predicate.equals(RDF.TYPE)) {
            if(!(object instanceof Resource)){
                throw new SailException("Expected object to a a Resource: "+object.toString());
            }

            resolveTypes((Resource) object)
                .stream()
                .peek(inferredType -> {
                    if (forwardChainingSchemaCachingRDFSInferencer.useAllRdfsRules && inferredType.equals(RDFS.CLASS)) {
                        addInferredStatement(subject, RDFS.SUBCLASSOF, RDFS.RESOURCE);
                    }
                })
                .filter(inferredType -> !inferredType.equals(object))
                .forEach(inferredType -> addInferredStatement(subject, RDF.TYPE, inferredType));
        }

        resolveProperties(predicate)
            .stream()
            .filter(inferredProperty -> !inferredProperty.equals(predicate))
            .filter(inferredPropery -> inferredPropery instanceof IRI)
            .map(inferredPropery -> ((IRI) inferredPropery))
            .forEach(inferredProperty -> addInferredStatement(subject, inferredProperty, object));


        if (object instanceof Resource) {
            resolveRangeTypes(predicate)
                .stream()
                .peek(inferredType -> {
                    if (forwardChainingSchemaCachingRDFSInferencer.useAllRdfsRules && inferredType.equals(RDFS.CLASS)) {
                        addInferredStatement(((Resource) object), RDFS.SUBCLASSOF, RDFS.RESOURCE);
                    }
                })
                .forEach(inferredType -> addInferredStatement(((Resource) object), RDF.TYPE, inferredType));
        }


        resolveDomainTypes(predicate)
            .stream()
            .peek(inferredType -> {
                if (forwardChainingSchemaCachingRDFSInferencer.useAllRdfsRules && inferredType.equals(RDFS.CLASS)) {
                    addInferredStatement(subject, RDFS.SUBCLASSOF, RDFS.RESOURCE);
                }
            })
            .forEach(inferredType -> addInferredStatement((subject), RDF.TYPE, inferredType));

    }


    protected void addAxiomStatements() {
        ValueFactory vf = forwardChainingSchemaCachingRDFSInferencer.getValueFactory();

        // This is http://www.w3.org/2000/01/rdf-schema# forward chained
        // Eg. all axioms in RDFS forward chained w.r.t. RDFS.
        // All those axioms are simply listed here

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
