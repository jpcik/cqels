package org.deri.cqels.lang.cqels;

import org.apache.jena.sparql.lang.SPARQLParserBase;

public class CQELSParserBase extends SPARQLParserBase
    implements CQELSParserConstants
{
	public Duration getDuration(String str){
		return new Duration(str);
	}
}
