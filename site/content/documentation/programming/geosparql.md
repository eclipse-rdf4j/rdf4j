---
title: "GeoSPARQL"
toc: true
weight: 8
---

RDF4J offers an extended algebra for partial GeoSPARQL support. When enabled, this offers additional geospatial functionality as part of the SPARQL engine, on top of any RDF4J repository, using the well-known Spatial4J and JTS libraries for geospatial reasoning.
<!--more-->

To enable GeoSPARQL support, all you need to do is include the `rdf4j-queryalgebra-geosparql` Maven module in your project:

    <dependency>
      <groupId>org.eclipse.rdf4j</groupId>
      <artifactId>rdf4j-queryalgebra-geosparql</artifactId>
    </dependency>

# Adding geospatial data to the Repository

By default, RDF4J only supports GeoSPARQL functions on top of geospatial data represented as so-called Well-Known Text (WKT) strings.

For example, to model the coordinates of landmarks like the Eiffel Tower in Paris or the Tower Bridge in London, you could do something like this:

    @prefix geo: <http://www.opengis.net/ont/geosparql#> .
    @prefix sf: <http://www.opengis.net/ont/sf> .
    @prefix ex: <http://example.org/> .

    ex:eiffelTower a ex:Landmark ;
              geo:hasGeometry ex:coordinates-et.
    ex:coordinates-et a sf:Point;
            geo:asWKT "POINT(2.2945 48.8584)"^^geo:wktLiteral .
    ex:towerBridge a ex:Landmark ;
              geo:hasGeometry ex:coordinates-tb.
    ex:coordinates-tb a sf:Point;
            geo:asWKT "POINT(-0.0754 51.5055)"^^geo:wktLiteral .

After adding this data to a repository, we can use a GeoSPARQL query to get, for example, the distance between the two landmarks, like so:

    PREFIX geo: <http://www.opengis.net/ont/geosparql#>
    PREFIX geof: <http://www.opengis.net/def/function/geosparql/>
    PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/>
    PREFIX ex: <http://example.org/>
    SELECT *
    WHERE {
      ?lmA a ex:Landmark ;
           geo:hasGeometry [ geo:asWKT ?coord1 ].
      ?lmB a ex:Landmark ;
           geo:hasGeometry [ geo:asWKT ?coord2 ].
      BIND((geof:distance(?coord1, ?coord2, uom:metre)/1000) as ?dist) .
      FILTER (str(?lmA) < str(?lmB))
    }

# Supported GeoSPARQL functions

In this section we briefly enumerate the GeoSPARQL extension functions supported by RDF4J. For more information on the precise meaning and use of these functions we refer you to to the GeoSPARQL specification.

## Non-topological and common query functions

RDF4J supports the following non-topological geospatial functions as defined in the GeoSPARQL specs: `geof:distance`, `geof:boundary`, `geof:buffer`, `geof:convexHull`, `geof:difference`, `geof:envelope`, `geof:intersection`, `geof:getSRID`, `geof:symDifference`, `geof:union`, and `geof:relate`.

## Simple Feature functions

RDF4J supports the following Simple Feature (sf) functions: `geof:sfEquals`, `geof:sfDisjoint`, `geof:sfIntersects`, `geof:sfTouches`, `geof:sfCrosses`, `geof:sfWithin`, `geof:sfContains`, and `geof:sfOverlaps`.

## Egenhofer functions

RDF4J supports the following Egenhofer (eh) functions: `geof:ehEquals`, `geof:ehDisjoint`, `geof:ehMeet`, `geof:ehOverlap`, `geof:ehCovers`, `geof:ehCoveredBy` , `geof:ehInside`, and `geof:ehContains`.

## RCC8 functions

RDF4J supports the following RCC8 functions: `geof:rcc8eq`, `geof:rcc8dc`, `geof:rcc8ec`, `geof:rcc8po`, `geof:rcc8tppi`, `geof:rcc8tpp`, `geof:rcc8ntpp`, and `geof:rcc8ntppi`.

## Improved performance through Lucene

Although RDF4J supports GeoSPARQL querying on any type of store, the Lucene SAIL and its derivates (the SolrSail and the ElasticSearchSail) have built-in optimizations that make geospatial querying on large datasets more efficient. By default, the Lucene SAIL only spatially indexes `http://www.opengis.net/ont/geosparql#asWKT fields`. This can be changed using the `LuceneSail.WKT_FIELDS` parameter.

# Reading and writing WKT Literals

RDF4J 2.4 uses Spatial4J 0.7, which has full support for converting between Shape objects and WKT strings. See the [Spatial4J Format documentation](https://github.com/locationtech/spatial4j/blob/master/FORMATS.md) for more details.

# Further reading

Here are some useful links:

- [Spatial4J website](https://projects.eclipse.org/projects/locationtech.spatial4j)
- [OGC GeoSPARQL specification](http://www.opengeospatial.org/standards/geosparql)
- [Wikipedia article on WKT](https://en.wikipedia.org/wiki/Well-known_text)

