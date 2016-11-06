package org.eclipse.rdf4j.sail.inferencer.fc;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Håvard Mikkelsen Ottestad
 */

public class FastRdfsForwardChainingSail extends AbstractForwardChainingInferencer {

    final NotifyingSail data;
    final Repository schema;

    boolean sesameCompliant = false;

    List<Statement> subClassOfStatemenets = new ArrayList<>();
    List<Statement> propertyStatements = new ArrayList<>();
    List<Statement> subPropertyOfStatemenets = new ArrayList<>();
    List<Statement> rangeStatemenets = new ArrayList<>();
    List<Statement> domainStatemenets = new ArrayList<>();


    Map<Resource, HashSet<Resource>> calculatedTypes = new HashMap<>();
    Map<IRI, HashSet<IRI>> calculatedProperties = new HashMap<>();
    Map<IRI, HashSet<Resource>> calculatedRange = new HashMap<>();
    Map<IRI, HashSet<Resource>> calculatedDomain = new HashMap<>();


    void clearInferenceTables() {
        subClassOfStatemenets = new ArrayList<>();
        propertyStatements = new ArrayList<>();
        subPropertyOfStatemenets = new ArrayList<>();
        rangeStatemenets = new ArrayList<>();
        domainStatemenets = new ArrayList<>();
        calculatedTypes = new HashMap<>();
        calculatedProperties = new HashMap<>();
        calculatedRange = new HashMap<>();
        calculatedDomain = new HashMap<>();


    }


    public FastRdfsForwardChainingSail(NotifyingSail data) {
        super(data);
        schema = null;
        this.data = data;

    }

    public FastRdfsForwardChainingSail(NotifyingSail data, Repository schema) {
        super(data);

        this.data = data;
        this.schema = schema;

    }

    public FastRdfsForwardChainingSail(NotifyingSail data, boolean sesameCompliant) {
        super(data);
        schema = null;

        this.data = data;
        this.sesameCompliant = sesameCompliant;

    }

    public FastRdfsForwardChainingSail(NotifyingSail data, Repository schema, boolean sesameCompliant) {
        super(data);

        this.data = data;
        this.schema = schema;
        this.sesameCompliant = sesameCompliant;

    }


    public void initialize() throws SailException {
        super.initialize();

        FastRdfsForwardChainingSailConnetion connection = null;

        try {
            connection = getConnection();
            final FastRdfsForwardChainingSailConnetion finalConnection = connection;
            finalConnection.begin();

            finalConnection.addAxiomStatements();

            List<Statement> schemaStatements = new ArrayList<>();

            if (schema != null) {

                try (RepositoryConnection schemaConnection = schema.getConnection()) {
                    schemaConnection.begin();
                    RepositoryResult<Statement> statements = schemaConnection.getStatements(null, null, null);

                    schemaStatements = Iterations.stream(statements)
                        .peek(finalConnection::statementCollector)
                        .collect(Collectors.toList());

                    schemaConnection.commit();
                }
            }

            finalConnection.calculateInferenceMaps();

            schemaStatements.forEach(s -> finalConnection.addStatement(s.getSubject(), s.getPredicate(), s.getObject(), s.getContext()));

            finalConnection.commit();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

    }


    public void setDataDir(File file) {
        throw new UnsupportedOperationException();
    }

    public File getDataDir() {
        throw new UnsupportedOperationException();
    }


    public FastRdfsForwardChainingSailConnetion getConnection() throws SailException {
        InferencerConnection e = (InferencerConnection) super.getConnection();
        return new FastRdfsForwardChainingSailConnetion(this, e);
    }

    public ValueFactory getValueFactory() {

        return SimpleValueFactory.getInstance();
    }


}
