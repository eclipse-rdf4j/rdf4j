PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl:<http://www.w3.org/2002/07/owl#>
PREFIX skos:<http://www.w3.org/2004/02/skos/core#>
PREFIX sd:<http://www.w3.org/ns/sparql-service-description#>
PREFIX msg:<http://www.openrdf.org/rdf/2011/messaging#>
PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>
PREFIX audit:<http://www.openrdf.org/rdf/2009/auditing#>

DELETE {
	?item calli:link </callimachus/menu>
} INSERT {
	?item calli:link </manifest/menu>
} WHERE {
	?item calli:link </callimachus/menu>
	FILTER strstarts(str(?item), str(</>))
};

DELETE {
	?item calli:link </callimachus/manifest>
} INSERT {
	?item calli:link </manifest>
} WHERE {
	?item calli:link </callimachus/manifest>
	FILTER strstarts(str(?item), str(</>))
};

DELETE {
	</callimachus/menu> a calli:Menu; a ?menu_type; calli:reader ?menu_reader; calli:editor ?menu_editor; calli:administrator ?menu_administrator;
		rdfs:label ?menu_label; calli:link ?menu_link; calli:item ?menu_nav .
	?menu_nav rdfs:label ?menu_nav_label; calli:position ?menu_nav_position; calli:item ?menu_item .
	?menu_item rdfs:label ?menu_item_label; calli:position ?menu_item_position; calli:link ?menu_item_link .
	?menu_nav calli:link ?menu_nav_ink
} INSERT {
	</manifest/menu> a calli:Menu; a ?menu_type; calli:reader ?menu_reader; calli:editor ?menu_editor; calli:administrator ?menu_administrator;
		rdfs:label ?menu_label; calli:link ?menu_link; calli:item ?manifest_menu_nav .
	?manifest_menu_nav rdfs:label ?menu_nav_label; calli:position ?menu_nav_position; calli:item ?manifest_menu_item .
	?manifest_menu_item rdfs:label ?menu_item_label; calli:position ?menu_item_position; calli:link ?menu_item_link .
	?manifest_menu_nav calli:link ?menu_nav_ink
} WHERE {
	</callimachus/menu> a calli:Menu; a ?menu_type; calli:reader ?menu_reader; calli:editor ?menu_editor; calli:administrator ?menu_administrator;
		rdfs:label ?menu_label; calli:link ?menu_link; calli:item ?menu_nav .
	?menu_nav rdfs:label ?menu_nav_label; calli:position ?menu_nav_position; calli:item ?menu_item .
	?menu_item rdfs:label ?menu_item_label; calli:position ?menu_item_position; calli:link ?menu_item_link .
	OPTIONAL {
		?menu_nav calli:link ?menu_nav_ink
	}
	FILTER strstarts(str(?menu_nav), str(</callimachus/>))
	FILTER strstarts(str(?menu_item), str(</callimachus/>))
	BIND (iri(concat(str(</manifest/>), strafter(str(?menu_nav), str(</callimachus/>)))) AS ?manifest_menu_nav)
	BIND (iri(concat(str(</manifest/>), strafter(str(?menu_item), str(</callimachus/>)))) AS ?manifest_menu_item)
};