/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.lmdb;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TempTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();
	private File dataDir;

	@Test
	public void test() throws IOException {

		dataDir = tempDir.newFolder();

		Repository repository = new SailRepository(new LmdbStore(dataDir, new LmdbStoreConfig("spoc,posc")));
		try (RepositoryConnection conn = repository.getConnection()) {

			conn.begin();
			conn.add(new StringReader(data), "", RDFFormat.TURTLE);

			String queryBuilder = "PREFIX mms: <https://mms.researchstudio.at/mms#>\n" +
					"PREFIX s: <http://schema.org/>\n" +
					"SELECT DISTINCT ?id ?f_copyOf ?f_standardId ?f_name ?dateCreated ?dateModified ?f_private ?f_description ?creator ?editor ?f_featureKind ?class ?quantityKind ?quantityKindLabel ?unit ?unitLabel ?allowMultiple ?optionId ?optionValue ?optionDescription ( CONCAT( IF( BOUND( ?organizationId ), CONCAT( \"/organizations/\", REPLACE( STR( ?organizationId ), \"urn:uuid:|/|#|:|\\\\\\\\.\", \"\" ) ), CONCAT( \"/projects/\", REPLACE( STR( ?projectId ), \"urn:uuid:|/|#|:|\\\\\\\\.\", \"\" ) ) ), \"/standards/\", REPLACE( STR( ?f_standardId ), \"urn:uuid:|/|#|:|\\\\\\\\.\", \"\" ), \"/features/\", REPLACE( STR( ?id ), \"urn:uuid:|/|#|:|\\\\\\\\.\", \"\" ) ) AS ?path )\n"
					+
					"WHERE { ?id a mms:Feature ;\n" +
					"    s:name ?f_name ;\n" +
					"    mms:private ?f_private ;\n" +
					"    s:dateCreated ?dateCreated ;\n" +
					"    s:dateModified ?dateModified ;\n" +
					"    s:creator ?creator ;\n" +
					"    s:editor ?editor .\n" +
					"?id mms:belongsToStandard ?f_standardId .\n" +
					"OPTIONAL { ?f_standardId mms:belongsToOrganization ?organizationId . }\n" +
					"OPTIONAL { ?f_standardId mms:belongsToProject ?projectId . }\n" +
					"OPTIONAL { ?id s:description ?f_description . }\n" +
					"OPTIONAL { ?id mms:copyOf ?f_copyOf . }\n" +
					"?id mms:dataType ?f_featureKind .\n" +
					"{ ?f_featureKind a ?class .\n" +
					"OPTIONAL { ?f_featureKind a ?class .\n" +
					"FILTER ( ( ?f_featureKind != <http://www.linkedmodel.org/schema/dtype#SimpleCodeList> && ?f_featureKind != mms:DataType ) ) }\n"
					+
					"OPTIONAL { ?f_featureKind mms:quantityKind ?quantityKind ;\n" +
					"    mms:unit ?unit .\n" +
					"?quantityKind <http://www.w3.org/2000/01/rdf-schema#label> ?quantityKindLabel .\n" +
					"?unit <http://www.w3.org/2000/01/rdf-schema#label> ?unitLabel . }\n" +
					"OPTIONAL { ?f_featureKind mms:allowMultiple ?allowMultiple . }\n" +
					"OPTIONAL { ?f_featureKind <http://www.linkedmodel.org/schema/dtype#hasMember> ?optionId .\n" +
					"?optionId <http://www.linkedmodel.org/schema/dtype#value> ?optionValue .\n" +
					"OPTIONAL { ?optionId s:description ?optionDescription . } }\n" +
					"FILTER ( ( ?f_featureKind IN ( mms:StringType, mms:BooleanType, mms:ReferenceType ) || ?class IN ( mms:NumericType, mms:EnumerationType ) ) ) }\n"
					+
					"FILTER ( !( ( BOUND( ?organizationId ) && BOUND( ?projectId ) ) ) ) }";

			conn.commit();
			conn.begin();

			try (TupleQueryResult evaluate = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryBuilder).evaluate()) {
				List<BindingSet> collect = evaluate.stream().collect(Collectors.toList());

				for (BindingSet bindings : collect) {
					System.out.println("bindings: " + bindings);
					System.out.println("bindings.getBinding(\"quantityKind\"): " + bindings.getBinding("quantityKind"));
					System.out.println("bindings.getBindingNames().contains(\"quantityKind\"): "
							+ bindings.getBindingNames().contains("quantityKind"));
					System.out.println();
				}
			}
			conn.commit();
		}
		repository.shutDown();
	}

	public static final String data = "@prefix :             <https://example.com/test#> .\n" +
			"@prefix mms:          <https://mms.researchstudio.at/mms#> .\n" +
			"@prefix rdf:          <http://www.w3.org/1999/02/22_rdf_syntax_ns#> .\n" +
			"@prefix owl:          <http://www.w3.org/2002/07/owl#> .\n" +
			"@prefix rdfs:         <http://www.w3.org/2000/01/rdf_schema#> .\n" +
			"@prefix dtype:        <http://www.linkedmodel.org/schema/dtype#> .\n" +
			"@prefix dc:           <http://purl.org/dc/elements/1.1/> .\n" +
			"@prefix fno:          <https://w3id.org/function/ontology#> .\n" +
			"@prefix xsd:          <http://www.w3.org/2001/XMLSchema#> .\n" +
			"@prefix qudt:         <http://qudt.org/schema/qudt/> .\n" +
			"@prefix dcterms:      <http://purl.org/dc/terms/> .\n" +
			"@prefix vann:         <http://purl.org/vocab/vann/> .\n" +
			"@prefix s:            <http://schema.org/> .\n" +
			"@prefix skos:         <http://www.w3.org/2004/02/skos/core#> .\n" +
			"@prefix unit:         <http://qudt.org/vocab/unit/> .\n" +
			"@prefix quantitykind: <http://qudt.org/vocab/quantitykind/> .\n" +
			"@prefix vaem:         <http://www.linkedmodel.org/schema/vaem#> .\n" +
			"\n" +
			":orgA\n" +
			"    a              mms:Organization ;\n" +
			"    s:name         \"Test Organization A\" ;\n" +
			"    dc:description \"Some test organization\" ;\n" +
			"    s:creator      \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor       \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated  \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":standardCopyTarget\n" +
			"    a                         mms:Standard ;\n" +
			"    mms:public                true ;\n" +
			"    mms:belongsToOrganization :orgA ;\n" +
			"    skos:prefLabel            \"Empty CopyTarget Standard\" ;\n" +
			"    s:description             \"Empty CopyTarget Standard\" ;\n" +
			"    s:creator                 \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor                  \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated             \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified            \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":standardA1\n" +
			"    a                         mms:Standard ;\n" +
			"    mms:public                true ;\n" +
			"    mms:belongsToOrganization :orgA ;\n" +
			"    skos:prefLabel            \"Test Standard A1\" ;\n" +
			"    s:description             \"A test standard.\" ;\n" +
			"    s:creator                 \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor                  \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated             \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified            \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureGroupA1_1\n" +
			"    a                     mms:FeatureGroup ;\n" +
			"    mms:belongsToStandard :standardA1 ;\n" +
			"    skos:prefLabel        \"Feature Group A1\" ;\n" +
			"    skos:broader          :featureGroupA1_3 ;\n" +
			"    s:description         \"A1 features\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureGroupA1_2\n" +
			"    a                     mms:FeatureGroup ;\n" +
			"    mms:belongsToStandard :standardA1 ;\n" +
			"    skos:prefLabel        \"Feature Group A2\" ;\n" +
			"    skos:broader          :featureGroupA1_3 ;\n" +
			"    s:description         \"A2 features\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureGroupA1_3\n" +
			"    a                     mms:FeatureGroup ;\n" +
			"    mms:belongsToStandard :standardA1 ;\n" +
			"    skos:prefLabel        \"Feature Group A3\" ;\n" +
			"    s:description         \"The supergroup of A1 and A2\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":propertySetA1_1\n" +
			"    a                     mms:PropertySet ;\n" +
			"    mms:belongsToStandard :standardA1 ;\n" +
			"    s:name                \"Property Set A1\" ;\n" +
			"    s:description         \"A1 features\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":propertySetA1_2\n" +
			"    a                     mms:PropertySet ;\n" +
			"    mms:belongsToStandard :standardA1 ;\n" +
			"    s:name                \"Property Set A2\" ;\n" +
			"    s:description         \"A1 features\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":propertySetA1_3\n" +
			"    a                     mms:PropertySet ;\n" +
			"    mms:belongsToStandard :standardA1 ;\n" +
			"    s:name                \"Property Set A3\" ;\n" +
			"    s:description         \"A1 features\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":propertySetB1_1\n" +
			"    a                     mms:PropertySet ;\n" +
			"    mms:belongsToStandard :standardA1 ;\n" +
			"    s:name                \"Property Set B1\" ;\n" +
			"    s:description         \"A1 features\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":propertySetB1_2\n" +
			"    a                     mms:PropertySet ;\n" +
			"    mms:belongsToStandard :standardB1 ;\n" +
			"    s:name                \"Property Set B2\" ;\n" +
			"    s:description         \"B2 PropertySet\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureA1_1\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:belongsToStandard :standardA1 ;\n" +
			"    mms:dataType          mms:StringType ;\n" +
			"    mms:featureGroup      :featureGroupA1_1, :featureGroupA1_2 ;\n" +
			"    mms:propertySet       :propertySetA1_1, :propertySetA1_2 ;\n" +
			"    s:name                \"test feature A1\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureA1_2\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:belongsToStandard :standardA1 ;\n" +
			"    mms:dataType          mms:BooleanType ;\n" +
			"    mms:featureGroup      :featureGroupA1_2 ;\n" +
			"    s:name                \"test feature A2\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureA1_3\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:belongsToStandard :standardA1 ;\n" +
			"    mms:dataType          <urn:uuid:cdef4567> ;\n" +
			"    mms:featureGroup      :featureGroupA1_1 ;\n" +
			"    s:name                \"test length in cm feature\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"<urn:uuid:cdef4567>\n" +
			"    a                mms:NumericType ;\n" +
			"    mms:quantityKind quantitykind:Length ;\n" +
			"    mms:unit         unit:CentiM .\n" +
			"\n" +
			":featureA1_4\n" +
			"    a                     mms:Feature ;\n" +
			"    s:description         \"\" ;\n" +
			"    s:name                \"test enum\" ;\n" +
			"    mms:belongsToStandard :standardA1 ;\n" +
			"    mms:featureGroup      :featureGroupA1_3 ;\n" +
			"    mms:private           false ;\n" +
			"    mms:dataType          <urn:uuid:1111aaaa> ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"<urn:uuid:1111aaaa>\n" +
			"    dtype:hasMember   <urn:uuid:tnxa92342>, <urn:uuid:9009009009> ;\n" +
			"    a                 mms:EnumerationType ;\n" +
			"    mms:allowMultiple false .\n" +
			"\n" +
			"<urn:uuid:tnxa92342>\n" +
			"    s:description \"first value\" ;\n" +
			"    dtype:value   <urn:uuid:3333ccccc> ;\n" +
			"    vaem:name     \"one\" .\n" +
			"\n" +
			"<urn:uuid:9009009009>\n" +
			"    s:description \"second value\" ;\n" +
			"    dtype:value   <urn:uuid:2222bbbb> ;\n" +
			"    vaem:name     \"two\" .\n" +
			"\n" +
			"\n" +
			":standardA2\n" +
			"    a                         mms:Standard ;\n" +
			"    mms:belongsToOrganization :orgA ;\n" +
			"    mms:public                false ;\n" +
			"    skos:prefLabel            \"Test Standard A2\" ;\n" +
			"    s:description             \"A test standard.\" ;\n" +
			"    s:creator                 \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor                  \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated             \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified            \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureA2_1\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:belongsToStandard :standardA2 ;\n" +
			"    mms:dataType          mms:StringType ;\n" +
			"    s:name                \"test feature A2_1\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureA2_2\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:belongsToStandard :standardA2 ;\n" +
			"    mms:dataType          mms:BooleanType ;\n" +
			"    s:name                \"test feature A2_2\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"\n" +
			":orgB\n" +
			"    a              mms:Organization ;\n" +
			"    s:name         \"Test Organization B\" ;\n" +
			"    dc:description \"Some test organization\" ;\n" +
			"    s:creator      \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor       \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated  \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":standardB1\n" +
			"    a                         mms:Standard ;\n" +
			"    mms:belongsToOrganization :orgB ;\n" +
			"    mms:public                false ;\n" +
			"    skos:prefLabel            \"Test Standard A1\" ;\n" +
			"    s:description             \"A test standard.\" ;\n" +
			"    s:creator                 \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor                  \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated             \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified            \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureB1_1\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:belongsToStandard :standardB1 ;\n" +
			"    mms:dataType          mms:StringType ;\n" +
			"    s:name                \"test feature B1\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureB1_2\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:belongsToStandard :standardB1 ;\n" +
			"    mms:dataType          mms:BooleanType ;\n" +
			"    s:name                \"test feature B2\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureB1_3\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:belongsToStandard :standardB1 ;\n" +
			"    mms:dataType          <urn:uuid:abc123> ;\n" +
			"    s:name                \"test numeric feature B1_3\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"<urn:uuid:abc123>\n" +
			"    a                mms:NumericType ;\n" +
			"    mms:quantityKind quantitykind:Length ;\n" +
			"    mms:unit         unit:M .\n" +
			"\n" +
			"\n" +
			":featureB1_4\n" +
			"    a                     mms:Feature ;\n" +
			"    s:description         \"\" ;\n" +
			"    s:name                \"test enum\" ;\n" +
			"    mms:belongsToStandard :standardB1 ;\n" +
			"    mms:private           false ;\n" +
			"    mms:dataType          <urn:uuid:1212121abababab> ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"<urn:uuid:1212121abababab>\n" +
			"    dtype:hasMember   <urn:uuid:b1b1b1b1b1>, <urn:uuid:b2b2b2b2b2b> ;\n" +
			"    a                 mms:EnumerationType ;\n" +
			"    mms:allowMultiple false .\n" +
			"\n" +
			"<urn:uuid:b1b1b1b1b1>\n" +
			"    s:description \"erster Wert\" ;\n" +
			"    dtype:value   <urn:uuid:787878dfdfdf> ;\n" +
			"    vaem:name     \"eins\" .\n" +
			"\n" +
			"<urn:uuid:b2b2b2b2b2b>\n" +
			"    s:description \"zweiter Wert\" ;\n" +
			"    dtype:value   <urn:uuid:8899898asdfsdf> ;\n" +
			"    vaem:name     \"zwei\" .\n" +
			"\n" +
			"\n" +
			"# Projects\n" +
			"\n" +
			":project1\n" +
			"    a              mms:Project ;\n" +
			"    s:name         \"Project One\" ;\n" +
			"    s:creator      \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor       \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated  \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":project2\n" +
			"    a              mms:Project ;\n" +
			"    s:name         \"Project Two\" ;\n" +
			"    s:creator      \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor       \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated  \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":project3\n" +
			"    a              mms:Project ;\n" +
			"    s:name         \"Project Three\" ;\n" +
			"    s:creator      \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor       \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated  \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":project4\n" +
			"    a              mms:Project ;\n" +
			"    s:name         \"Project Four\" ;\n" +
			"    s:creator      \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor       \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated  \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":project5\n" +
			"    a              mms:Project ;\n" +
			"    s:name         \"Project Five\" ;\n" +
			"    s:creator      \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor       \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated  \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"# Project Standards\n" +
			"# Project 1 Standard A - 4 Features\n" +
			"\n" +
			":standardP1_A\n" +
			"    a                            mms:Standard ;\n" +
			"    skos:prefLabel               \"Feature Set of Org A\" ;\n" +
			"    mms:public                   false ;\n" +
			"    mms:belongsToProject         :project1 ;\n" +
			"    mms:importedFromOrganization :orgA ;\n" +
			"    s:creator                    \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor                     \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated                \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified               \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"# Project 1 Standard B - 3 Features\n" +
			"\n" +
			":standardP1_B\n" +
			"    a                            mms:Standard ;\n" +
			"    skos:prefLabel               \"Feature Set of Org B\" ;\n" +
			"    mms:public                   false ;\n" +
			"    mms:belongsToProject         :project1 ;\n" +
			"    mms:importedFromOrganization :orgB ;\n" +
			"    s:creator                    \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor                     \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated                \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified               \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":standardP2_S1\n" +
			"    a                    mms:Standard ;\n" +
			"    skos:prefLabel       \"Feature Set 1 of Project 2\" ;\n" +
			"    mms:public           false ;\n" +
			"    mms:belongsToProject :project2 ;\n" +
			"    s:creator            \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated        \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified       \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"# Project 3 Standard 1 - 1 Feature\n" +
			"\n" +
			":standardP3_S1\n" +
			"    a                    mms:Standard ;\n" +
			"    skos:prefLabel       \"Feature Set 1 of Project 3\" ;\n" +
			"    mms:public           false ;\n" +
			"    mms:belongsToProject :project3 ;\n" +
			"    s:creator            \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated        \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified       \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"# Project 3 Standard 2 - 1 Feature\n" +
			"\n" +
			":standardP3_S2\n" +
			"    a                    mms:Standard ;\n" +
			"    skos:prefLabel       \"Feature Set 2 of Project 3\" ;\n" +
			"    mms:public           false ;\n" +
			"    mms:belongsToProject :project3 ;\n" +
			"    s:creator            \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated        \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified       \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"# Action Groups for mapping\n" +
			"\n" +
			":actionGroup_AG1\n" +
			"    a          mms:DeleteActionGroup ;\n" +
			"    mms:action :deleteAction1_AG1 ;\n" +
			"    mms:action :deleteAction2_AG1 .\n" +
			"\n" +
			":deleteAction1_AG1\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP1_A1_1 .\n" +
			"\n" +
			":deleteAction2_AG1\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP1_A1_2 .\n" +
			"\n" +
			"\n" +
			":actionGroup_AG2\n" +
			"    a          mms:DeleteActionGroup ;\n" +
			"    mms:action :deleteAction1_AG2 ;\n" +
			"    mms:action :deleteAction2_AG2 .\n" +
			"\n" +
			":deleteAction1_AG2\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP1_A1_1 .\n" +
			"\n" +
			":deleteAction2_AG2\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP1_A1_2 .\n" +
			"\n" +
			":actionGroup_AG3\n" +
			"    a          mms:DeleteActionGroup ;\n" +
			"    mms:action :deleteAction1_AG3 ;\n" +
			"    mms:action :deleteAction2_AG3 .\n" +
			"\n" +
			":deleteAction1_AG3\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP1_A1_1 .\n" +
			"\n" +
			":deleteAction2_AG3\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP1_A1_2 .\n" +
			"\n" +
			":actionGroup_AG4\n" +
			"    a          mms:DeleteActionGroup ;\n" +
			"    mms:action :deleteAction1_AG4 ;\n" +
			"    mms:action :deleteAction2_AG4 .\n" +
			"\n" +
			":deleteAction1_AG4\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP1_A1_1 .\n" +
			"\n" +
			":deleteAction2_AG4\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP1_A1_2 .\n" +
			"\n" +
			":actionGroup_AG5\n" +
			"    a          mms:DeleteActionGroup ;\n" +
			"    mms:action :deleteAction1_AG5 ;\n" +
			"    mms:action :deleteAction2_AG5 .\n" +
			"\n" +
			":deleteAction1_AG5\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP1_A1_1 .\n" +
			"\n" +
			":deleteAction2_AG5\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP1_A1_2 .\n" +
			"\n" +
			":actionGroup_AG6\n" +
			"    a          mms:DeleteActionGroup ;\n" +
			"    mms:action :deleteAction1_AG6 ;\n" +
			"    mms:action :deleteAction2_AG6 .\n" +
			"\n" +
			":deleteAction1_AG6\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP1_A1_1 .\n" +
			"\n" +
			":deleteAction2_AG6\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP1_A1_2 .\n" +
			"\n" +
			":actionGroup_AG7\n" +
			"    a          mms:DeleteActionGroup ;\n" +
			"    mms:action :deleteAction1_AG7 ;\n" +
			"    mms:action :deleteAction2_AG7 .\n" +
			"\n" +
			":deleteAction1_AG7\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP3_S2_1 .\n" +
			"\n" +
			":deleteAction2_AG7\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP3_S2_2 .\n" +
			"\n" +
			":actionGroup_AG8\n" +
			"    a          mms:DeleteActionGroup ;\n" +
			"    mms:action :deleteAction1_AG8 ;\n" +
			"    mms:action :deleteAction2_AG8 .\n" +
			"\n" +
			":deleteAction1_AG8\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP2_S1_1 .\n" +
			"\n" +
			":deleteAction2_AG8\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP2_S1_2 .\n" +
			"\n" +
			":actionGroup_P3_AG1\n" +
			"    a          mms:DeleteActionGroup ;\n" +
			"    mms:action :deleteAction1_P3_AG1 .\n" +
			"\n" +
			":deleteAction1_P3_AG1\n" +
			"    a           mms:DeleteAction ;\n" +
			"    mms:feature :featureP3_S1_1 .\n" +
			"\n" +
			"\n" +
			"# Project Mappings\n" +
			"\n" +
			":mappingP1_A\n" +
			"    a                    mms:Mapping ;\n" +
			"    mms:belongsToProject :project1 ;\n" +
			"    mms:targetStandard   :standardP1_A ;\n" +
			"    mms:featureSet       :standardP1_A ;\n" +
			"    mms:featureSet       :standardP1_B ;\n" +
			"    s:name               \"mappingP1_A\" ;\n" +
			"    s:creator            \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated        \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified       \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    mms:actionGroup      :actionGroup_AG1 ;\n" +
			"    mms:actionGroup      :actionGroup_AG2 ;\n" +
			"    mms:condition        :condition1 .\n" +
			"\n" +
			":condition1\n" +
			"    a             mms:Condition ;\n" +
			"    a             mms:SingleCondition ;\n" +
			"    mms:feature   :featureP1_A1_1 ;\n" +
			"    mms:predicate mms:present .\n" +
			"\n" +
			":condition1a\n" +
			"    a             mms:Condition ;\n" +
			"    a             mms:SingleCondition ;\n" +
			"    mms:feature   :featureP1_A1_1 ;\n" +
			"    mms:predicate mms:present .\n" +
			"\n" +
			":condition1b\n" +
			"    a             mms:Condition ;\n" +
			"    a             mms:SingleCondition ;\n" +
			"    mms:feature   :featureP1_A1_1 ;\n" +
			"    mms:predicate mms:present .\n" +
			"\n" +
			":condition1c\n" +
			"    a             mms:Condition ;\n" +
			"    a             mms:SingleCondition ;\n" +
			"    mms:feature   :featureP1_A1_1 ;\n" +
			"    mms:predicate mms:present .\n" +
			"\n" +
			"\n" +
			":condition2a\n" +
			"    a             mms:Condition ;\n" +
			"    a             mms:SingleCondition ;\n" +
			"    mms:feature   :featureP1_A1_2 ;\n" +
			"    mms:predicate mms:present .\n" +
			"\n" +
			":condition2b\n" +
			"    a             mms:Condition ;\n" +
			"    a             mms:SingleCondition ;\n" +
			"    mms:feature   :featureP1_A1_2 ;\n" +
			"    mms:predicate mms:present .\n" +
			"\n" +
			":condition2c\n" +
			"    a             mms:Condition ;\n" +
			"    a             mms:SingleCondition ;\n" +
			"    mms:feature   :featureP1_A1_2 ;\n" +
			"    mms:predicate mms:present .\n" +
			"\n" +
			":condition3\n" +
			"    a                  mms:Condition ;\n" +
			"    a                  mms:SingleCondition ;\n" +
			"    mms:feature        :featureP1_A1_2 ;\n" +
			"    mms:predicate      mms:contains ;\n" +
			"    mms:conditionValue \"blabla\" .\n" +
			"\n" +
			":condition5\n" +
			"    a                  mms:Condition ;\n" +
			"    a                  mms:SingleCondition ;\n" +
			"    mms:feature        :featureP1_A1_2 ;\n" +
			"    mms:predicate      mms:contains ;\n" +
			"    mms:conditionValue \"blabla\" .\n" +
			"\n" +
			":conditionGroup1\n" +
			"    a              mms:Condition ;\n" +
			"    a              mms:ConditionGroup ;\n" +
			"    mms:connective mms:and ;\n" +
			"    mms:condition  :condition1a ;\n" +
			"    mms:condition  :condition2a .\n" +
			"\n" +
			":conditionGroup2\n" +
			"    a              mms:Condition ;\n" +
			"    a              mms:ConditionGroup ;\n" +
			"    mms:connective mms:and ;\n" +
			"    mms:condition  :condition1b ;\n" +
			"    mms:condition  :condition2b .\n" +
			"\n" +
			":conditionGroup3\n" +
			"    a              mms:Condition ;\n" +
			"    a              mms:ConditionGroup ;\n" +
			"    mms:connective mms:or ;\n" +
			"    mms:condition  :condition1c ;\n" +
			"    mms:condition  :condition2c ;\n" +
			"    mms:condition  :conditionGroup4 .\n" +
			"\n" +
			":conditionGroup4\n" +
			"    a              mms:Condition ;\n" +
			"    a              mms:ConditionGroup ;\n" +
			"    mms:connective mms:and ;\n" +
			"    mms:condition  :condition3 ;\n" +
			"    mms:condition  :condition5 .\n" +
			"\n" +
			":conditionGroupNested\n" +
			"    a              mms:Condition ;\n" +
			"    a              mms:ConditionGroup ;\n" +
			"    mms:connective mms:or ;\n" +
			"    mms:condition  :conditionGroup1 ;\n" +
			"    mms:condition  :conditionGroup3 .\n" +
			"\n" +
			":mappingP1_B\n" +
			"    a                    mms:Mapping ;\n" +
			"    mms:belongsToProject :project1 ;\n" +
			"    mms:actionGroup      :actionGroup_AG3 ;\n" +
			"    mms:targetStandard   :standardP1_A ;\n" +
			"    mms:featureSet       :standardP1_A ;\n" +
			"    s:name               \"mapping2\" ;\n" +
			"    s:creator            \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated        \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified       \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":mappingP1_D\n" +
			"    a                    mms:Mapping ;\n" +
			"    mms:belongsToProject :project1 ;\n" +
			"    mms:actionGroup      :actionGroup_AG4 ;\n" +
			"    mms:targetStandard   :standardP1_A ;\n" +
			"    mms:featureSet       :standardP1_A ;\n" +
			"    s:name               \"mappingConditionNestedGroup\" ;\n" +
			"    s:creator            \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated        \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified       \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    mms:condition        :conditionGroupNested .\n" +
			"\n" +
			":mappingP1_C\n" +
			"    a                    mms:Mapping ;\n" +
			"    mms:belongsToProject :project1 ;\n" +
			"    mms:actionGroup      :actionGroup_AG5 ;\n" +
			"    mms:actionGroup      :actionGroup_AG6 ;\n" +
			"    mms:targetStandard   :standardP1_A ;\n" +
			"    mms:featureSet       :standardP1_A ;\n" +
			"    s:name               \"mappingConditionGroup\" ;\n" +
			"    s:creator            \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated        \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified       \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    mms:condition        :conditionGroup2 .\n" +
			"\n" +
			":mappingP2_A\n" +
			"    a                    mms:Mapping ;\n" +
			"    mms:belongsToProject :project2 ;\n" +
			"    mms:actionGroup      :actionGroup_AG7 ;\n" +
			"    mms:targetStandard   :standardP2_S1 ;\n" +
			"    mms:featureSet       :standardP2_S1 ;\n" +
			"    s:name               \"mapping1\" ;\n" +
			"    s:creator            \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated        \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified       \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":mappingP2_B\n" +
			"    a                    mms:Mapping ;\n" +
			"    mms:belongsToProject :project2 ;\n" +
			"    mms:actionGroup      :actionGroup_AG8 ;\n" +
			"    mms:targetStandard   :standardP2_S1 ;\n" +
			"    mms:featureSet       :standardP2_S1 ;\n" +
			"    s:name               \"mapping2\" ;\n" +
			"    s:creator            \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated        \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified       \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":mappingP3_A\n" +
			"    a                    mms:Mapping ;\n" +
			"    mms:belongsToProject :project3 ;\n" +
			"    mms:actionGroup      :actionGroup_P3_AG1 ;\n" +
			"    mms:targetStandard   :standardP3_S1 ;\n" +
			"    mms:featureSet       :standardP3_S1 ;\n" +
			"    s:name               \"mapping1\" ;\n" +
			"    s:creator            \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated        \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified       \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"# Project PropertySets\n" +
			"# Project 1\n" +
			":propertySetP1_A1_1\n" +
			"    a                     mms:PropertySet ;\n" +
			"    mms:belongsToStandard :standardP1_A ;\n" +
			"    s:name                \"propertySetP1_A1_1\" ;\n" +
			"    s:description         \"propertySetP1_A1_1\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"# Project Features\n" +
			"# Project 1\n" +
			"\n" +
			":featureP1_A1_1\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:copyOf            :featureA1_1 ;\n" +
			"    mms:belongsToStandard :standardP1_A ;\n" +
			"    mms:dataType          mms:StringType ;\n" +
			"    s:name                \"copy of test feature A1_1\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"\n" +
			":featureP1_A1_2\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:copyOf            :featureA1_1 ;\n" +
			"    mms:belongsToStandard :standardP1_A ;\n" +
			"    mms:dataType          mms:StringType ;\n" +
			"    s:name                \"copy of test feature A1_1\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureP1_A1_3\n" +
			"    a                            mms:Feature ;\n" +
			"    mms:private                  false ;\n" +
			"    mms:copyOf                   :featureA1_3 ;\n" +
			"    mms:belongsToStandard        :standardP1_A ;\n" +
			"    mms:importedFromOrganization :orgA ;\n" +
			"    mms:dataType                 <urn:uuid:aabb1122> ;\n" +
			"    s:name                       \"test length in cm feature\" ;\n" +
			"    s:creator                    \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor                     \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated                \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified               \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureP1_A1_5\n" +
			"    a                            mms:Feature ;\n" +
			"    mms:private                  false ;\n" +
			"    mms:belongsToStandard        :standardP1_A ;\n" +
			"    mms:importedFromOrganization :orgA ;\n" +
			"    mms:dataType                 mms:BooleanType ;\n" +
			"    s:name                       \"test feature A2\" ;\n" +
			"    s:creator                    \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor                     \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated                \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified               \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"\n" +
			"<urn:uuid:aabb1122>\n" +
			"    a                mms:NumericType ;\n" +
			"    mms:quantityKind quantitykind:Length ;\n" +
			"    mms:unit         unit:CentiM .\n" +
			"\n" +
			":featureP1_A1_4\n" +
			"    a                            mms:Feature ;\n" +
			"    s:description                \"\" ;\n" +
			"    s:name                       \"test enum\" ;\n" +
			"    mms:belongsToStandard        :standardP1_A ;\n" +
			"    mms:importedFromOrganization :orgA ;\n" +
			"    mms:copyOf                   :featureA1_4 ;\n" +
			"    mms:private                  false ;\n" +
			"    mms:dataType                 <urn:uuid:2342345wernnnlk> ;\n" +
			"    s:creator                    \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor                     \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated                \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified               \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"<urn:uuid:2342345wernnnlk>\n" +
			"    dtype:hasMember   <urn:uuid:9q9q9q9q9q>, <urn:uuid:6z6z6z6z6>, <urn:uuid:6z6z6z6z7> ;\n" +
			"    a                 mms:EnumerationType ;\n" +
			"    mms:allowMultiple false\n" +
			".\n" +
			"\n" +
			"<urn:uuid:9q9q9q9q9q>\n" +
			"    s:description \"first value\" ;\n" +
			"    dtype:value   <urn:uuid:3333ccccc> ;\n" +
			"    vaem:name     \"one\" .\n" +
			"\n" +
			"<urn:uuid:6z6z6z6z6>\n" +
			"    s:description \"second value\" ;\n" +
			"    dtype:value   <urn:uuid:2222bbbb> ;\n" +
			"    vaem:name     \"two\" .\n" +
			"\n" +
			"<urn:uuid:6z6z6z6z7>\n" +
			"    s:description \"third value\" ;\n" +
			"    dtype:value   <urn:uuid:4444dddd> ;\n" +
			"    vaem:name     \"three\" .\n" +
			"\n" +
			":featureP1_B1_1\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:copyOf            :featureB1_1 ;\n" +
			"    mms:belongsToStandard :standardP1_B ;\n" +
			"    mms:dataType          mms:StringType ;\n" +
			"    s:name                \"copy of test feature B1_1\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureP1_B1_3\n" +
			"    a                            mms:Feature ;\n" +
			"    mms:private                  false ;\n" +
			"    mms:copyOf                   :featureB1_3 ;\n" +
			"    mms:belongsToStandard        :standardP1_B ;\n" +
			"    mms:importedFromOrganization :orgB ;\n" +
			"    mms:dataType                 <urn:uuid:3344xxyy> ;\n" +
			"    s:name                       \"test numeric feature B1_3\" ;\n" +
			"    s:creator                    \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor                     \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated                \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified               \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"<urn:uuid:3344xxyy>\n" +
			"    a                mms:NumericType ;\n" +
			"    mms:quantityKind quantitykind:Length ;\n" +
			"    mms:unit         unit:M .\n" +
			"\n" +
			":featureP1_B1_4\n" +
			"    a                            mms:Feature ;\n" +
			"    s:description                \"\" ;\n" +
			"    s:name                       \"test enum\" ;\n" +
			"    mms:importedFromOrganization :orgB ;\n" +
			"    mms:belongsToStandard        :standardP1_B ;\n" +
			"    mms:copyOf                   :featureB1_4 ;\n" +
			"    mms:private                  false ;\n" +
			"    mms:dataType                 <urn:uuid:454545kjkjkjkj> ;\n" +
			"    s:creator                    \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor                     \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated                \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified               \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"<urn:uuid:454545kjkjkjkj>\n" +
			"    dtype:hasMember   <urn:uuid:xvxvxvxvx7878787>, <urn:uuid:uiuiuiuiuiuiui> ;\n" +
			"    a                 mms:EnumerationType ;\n" +
			"    mms:allowMultiple false .\n" +
			"\n" +
			"<urn:uuid:xvxvxvxvx7878787>\n" +
			"    s:description \"erster Wert\" ;\n" +
			"    dtype:value   <urn:uuid:575757ajajajaj> ;\n" +
			"    vaem:name     \"eins\" .\n" +
			"\n" +
			"<urn:uuid:uiuiuiuiuiuiui>\n" +
			"    s:description \"zweiter Wert\" ;\n" +
			"    dtype:value   <urn:uuid:67676dfdfdf> ;\n" +
			"    vaem:name     \"zwei\" .\n" +
			"\n" +
			"#Project 2\n" +
			"\n" +
			":featureP2_S1_1\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:copyOf            :featureA1_1 ;\n" +
			"    mms:belongsToStandard :standardP2_S1 ;\n" +
			"    mms:dataType          mms:StringType ;\n" +
			"    s:name                \"copy of test feature S1_1\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"\n" +
			":featureP2_S1_2\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:copyOf            :featureA1_1 ;\n" +
			"    mms:belongsToStandard :standardP2_S1 ;\n" +
			"    mms:dataType          mms:StringType ;\n" +
			"    s:name                \"copy of test feature S1_1\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureP2_S1_3\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:copyOf            :featureA1_3 ;\n" +
			"    mms:belongsToStandard :standardP2_S1 ;\n" +
			"    mms:dataType          <urn:uuid:xxyy8899> ;\n" +
			"    s:name                \"test length in cm feature\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			"<urn:uuid:xxyy8899>\n" +
			"    a                mms:NumericType ;\n" +
			"    mms:quantityKind quantitykind:Length ;\n" +
			"    mms:unit         unit:CentiM .\n" +
			"\n" +
			"#Project 3\n" +
			"\n" +
			":featureP3_S1_1\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:belongsToStandard :standardP3_S1 ;\n" +
			"    mms:dataType          mms:StringType ;\n" +
			"    s:name                \"String Feature of Project 3\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureP3_S2_1\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:belongsToStandard :standardP3_S2 ;\n" +
			"    mms:dataType          mms:StringType ;\n" +
			"    s:name                \"String Feature of Project 3\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureP3_S2_2\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:copyOf            :featureP3_S1_1 ;\n" +
			"    mms:belongsToStandard :standardP3_S2 ;\n" +
			"    mms:dataType          mms:StringType ;\n" +
			"    s:name                \"Copy of FeatureP3_S1_1\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":standardC1\n" +
			"    a                         mms:Standard ;\n" +
			"    mms:public                true ;\n" +
			"    mms:belongsToOrganization :orgA ;\n" +
			"    skos:prefLabel            \"Test Standard C1\" ;\n" +
			"    s:description             \"A test standard.\" ;\n" +
			"    s:creator                 \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor                  \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated             \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified            \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":propertySetC1_1\n" +
			"    a                     mms:PropertySet ;\n" +
			"    mms:belongsToStandard :standardC1 ;\n" +
			"    s:name                \"Property Set C1\" ;\n" +
			"    s:description         \"C1 features\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":featureC1_1\n" +
			"    a                     mms:Feature ;\n" +
			"    mms:private           false ;\n" +
			"    mms:belongsToStandard :standardC1 ;\n" +
			"    mms:dataType          mms:StringType ;\n" +
			"    mms:propertySet       :propertySetC1_1 ;\n" +
			"    s:name                \"feature in StandardC1\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":propertySetC1_2\n" +
			"    a                     mms:PropertySet ;\n" +
			"    mms:belongsToStandard :standardC1 ;\n" +
			"    s:name                \"Property Set C2\" ;\n" +
			"    s:description         \"Empty PropertySet\" ;\n" +
			"    s:creator             \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor              \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated         \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified        \"2022-01-21T13:09:32.695864Z\" .\n" +
			"\n" +
			":standardP5_A\n" +
			"    a                            mms:Standard ;\n" +
			"    skos:prefLabel               \"Feature Set of Org A\" ;\n" +
			"    mms:public                   false ;\n" +
			"    mms:belongsToProject         :project5 ;\n" +
			"    mms:importedFromOrganization :orgA ;\n" +
			"    s:creator                    \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:editor                     \"be533d70-0cd5-4425-beda-080c56b8b820\" ;\n" +
			"    s:dateCreated                \"2022-01-21T13:09:32.695864Z\" ;\n" +
			"    s:dateModified               \"2022-01-21T13:09:32.695864Z\" .";

}
