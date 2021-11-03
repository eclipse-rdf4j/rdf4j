package org.eclipse.rdf4j.spring.demo.support;

import org.eclipse.rdf4j.spring.support.DataInserter;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;

public class InitialDataInserter {
    DataInserter dataInserter;
    Resource ttlFile;

    public InitialDataInserter(DataInserter dataInserter, Resource ttlFile) {
        this.dataInserter = dataInserter;
        this.ttlFile = ttlFile;
    }

    @PostConstruct
    public void insertDemoData() {
        this.dataInserter.insertData(ttlFile);
    }
}
