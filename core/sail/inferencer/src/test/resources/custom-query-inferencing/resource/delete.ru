PREFIX : <http://foo.org/bar#>

DELETE 
{ 
	?s :sendsMessageTo ?o
} 
WHERE 
{ 
	?s :sendsMessageTo ?o
}