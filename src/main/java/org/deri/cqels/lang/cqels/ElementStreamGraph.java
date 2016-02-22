package org.deri.cqels.lang.cqels;

import org.deri.cqels.engine.Window;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementVisitor;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

public class ElementStreamGraph extends ElementNamedGraph {
	private Window window;
	public ElementStreamGraph(Node n, Window w, Element el) {
		super(n, el);
		window = w;
	}
	
	public Window getWindow() {	return window; }
}
