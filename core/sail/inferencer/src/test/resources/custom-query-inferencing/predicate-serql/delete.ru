PREFIX : <http://foo.org/bar#>

DELETE 
{ 
	?s :parentOf ?o
} 
WHERE 
{ 
	?s :parentOf ?o
}