package org.deri.cqels.engine;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
/** 
 * @author		Danh Le Phuoc
 * @organization DERI Galway, NUIG, Ireland  www.deri.ie
 * @email 	danh.lephuoc@deri.org
 */
public abstract class RDFStream {
	
	Node streamURI;
	ExecContext context;
	public RDFStream(ExecContext context, String uri) {
		streamURI=NodeFactory.createURI(uri);
		this.context=context;
	}
	
	public void stream(Node s, Node p, Node o){
		context.engine().send(streamURI, s, p, o);
	}
	
	public void stream(String s, String p, String o){
		context.engine().send(streamURI, n(s), n(p), n(o));
	}
	
	public void stream(Triple t){ stream(t.getSubject(),t.getPredicate(),t.getObject()); }
	
	public static  Node n(String st){
		return NodeFactory.createURI(st);
	}
	
	public abstract void stop();
	
	public String getURI(){ return streamURI.getURI();}
}
