PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl:<http://www.w3.org/2002/07/owl#>
PREFIX skos:<http://www.w3.org/2004/02/skos/core#>
PREFIX sd:<http://www.w3.org/ns/sparql-service-description#>
PREFIX msg:<http://www.openrdf.org/rdf/2011/messaging#>
PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>
PREFIX audit:<http://www.openrdf.org/rdf/2009/auditing#>

INSERT DATA {

</callimachus/menu> a </callimachus/Menu>, calli:Menu;
	rdfs:label "menu";
	calli:reader </group/users>;
	calli:editor </group/staff>;
	calli:administrator </group/admin>;
	calli:link </>;
	calli:item </callimachus/menu#site>;
	calli:item </callimachus/menu#resources>;
	calli:item </callimachus/menu#administration>;
	calli:item </callimachus/menu#toolbox>.

</callimachus/menu#site>	calli:position 1; rdfs:label "Site";
	calli:item </callimachus/menu#mainarticle>;
	calli:item </callimachus/menu#homefolder>;
	calli:item </callimachus/menu#contents>;
	calli:item </callimachus/menu#recentchanges>.
</callimachus/menu#resources>	calli:position 8; rdfs:label "Resources";
	calli:item </callimachus/menu#articles>;
	calli:item </callimachus/menu#textfiles>;
	calli:item </callimachus/menu#pages>;
	calli:item </callimachus/menu#scripts>;
	calli:item </callimachus/menu#styles>;
	calli:item </callimachus/menu#transforms>;
	calli:item </callimachus/menu#concepts>;
	calli:item </callimachus/menu#classes>.
</callimachus/menu#administration>	calli:position 9; rdfs:label "Administration";
	calli:item </callimachus/menu#manifest>;
	calli:item </callimachus/menu#usergroups>;
	calli:item </callimachus/menu#namedqueries>;
	calli:item </callimachus/menu#namedgraphs>;
	calli:item </callimachus/menu#graphdocuments>;
	calli:item </callimachus/menu#sparqlquery>.
</callimachus/menu#toolbox>	calli:position 10; rdfs:label "Toolbox";
	calli:item </callimachus/menu#whatlinkshere>;
	calli:item </callimachus/menu#relatedchanges>;
	calli:item </callimachus/menu#introspectresource>;
	calli:item </callimachus/menu#permissions>;
	calli:item </callimachus/menu#printthispage>.

</callimachus/menu#mainarticle>	calli:position 1; rdfs:label "Main article"; calli:link </main-article.docbook?view>.
</callimachus/menu#homefolder>	calli:position 2; rdfs:label "Home folder"; calli:link </?view>.
</callimachus/menu#contents>	calli:position 3; rdfs:label "Contents"; calli:link </callimachus/menu>.
</callimachus/menu#recentchanges>	calli:position 4; rdfs:label "Recent changes"; calli:link </callimachus/changes>.

</callimachus/menu#articles>	calli:position 1; rdfs:label "Articles"; calli:link </callimachus/Article>.
</callimachus/menu#textfiles>	calli:position 2; rdfs:label "Text files"; calli:link </callimachus/TextFile>.
</callimachus/menu#pages>	calli:position 3; rdfs:label "Pages"; calli:link </callimachus/Page>.
</callimachus/menu#scripts>	calli:position 4; rdfs:label "Scripts"; calli:link </callimachus/Script>.
</callimachus/menu#styles>	calli:position 5; rdfs:label "Styles"; calli:link </callimachus/Style>.
</callimachus/menu#transforms>	calli:position 6; rdfs:label "Transforms"; calli:link </callimachus/Transform>.
</callimachus/menu#concepts>	calli:position 7; rdfs:label "Concepts"; calli:link </callimachus/Concept>.
</callimachus/menu#classes>	calli:position 8; rdfs:label "Classes"; calli:link </callimachus/Class>.

</callimachus/menu#manifest>	calli:position 1; rdfs:label "Manifest"; calli:link </callimachus/manifest>.
</callimachus/menu#usergroups>	calli:position 2; rdfs:label "User groups"; calli:link </callimachus/Group>.
</callimachus/menu#namedqueries>	calli:position 3; rdfs:label "Named queries"; calli:link </callimachus/NamedQuery>.
</callimachus/menu#namedgraphs>	calli:position 4; rdfs:label "Named graphs"; calli:link </callimachus/NamedGraph>.
</callimachus/menu#graphdocuments>	calli:position 5; rdfs:label "Graph documents"; calli:link </callimachus/GraphDocument>.
</callimachus/menu#sparqlquery>	calli:position 6; rdfs:label "SPARQL query"; calli:link </sparql>.

</callimachus/menu#whatlinkshere>	calli:position 1; rdfs:label "What links here"; calli:link <javascript:location='?whatlinkshere'>.
</callimachus/menu#relatedchanges>	calli:position 2; rdfs:label "Related changes"; calli:link <javascript:location='?relatedchanges'>.
</callimachus/menu#introspectresource>	calli:position 3; rdfs:label "Introspect resource"; calli:link <javascript:location='?introspect'>.
</callimachus/menu#permissions>	calli:position 3; rdfs:label "Permissions"; calli:link <javascript:location='?permissions'>.
</callimachus/menu#printthispage>	calli:position 4; rdfs:label "Print this page"; calli:link <javascript:print()>.
}
