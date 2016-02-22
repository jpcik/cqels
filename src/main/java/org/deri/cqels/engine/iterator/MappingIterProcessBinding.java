package org.deri.cqels.engine.iterator;

import java.util.NoSuchElementException;

import org.deri.cqels.data.Mapping;
import org.deri.cqels.engine.ExecContext;

import org.apache.jena.sparql.ARQInternalErrorException;
//import org.apache.jena.sparql.util.Utils;
import org.apache.jena.atlas.lib.Lib;

public abstract class MappingIterProcessBinding extends MappingIter1 {
	
	abstract public Mapping accept(Mapping mapping);
	
	Mapping nextMapping;
	
	public MappingIterProcessBinding(MappingIterator mIter, ExecContext context){
		super(mIter, context);
		nextMapping = null;
	}
	
	@Override
	protected void requestSubCancel() {}

	@Override
	protected void closeSubIterator() {}

	@Override
	protected boolean hasNextMapping() {
		if (isFinished())
            return false;
        
        if (nextMapping != null)
            return true;

        // Null iterator.
        if (getInput() == null)
            throw new ARQInternalErrorException(Lib.className(this)+": Null iterator");

        while (getInput().hasNext()) {
            // Skip forward until a binding to return is found. 
            Mapping input = getInput().nextMapping();
            Mapping output = accept(input);
            if (output != null) {
                nextMapping = output;
                return true;
            }
        }
        nextMapping = null;
        return false;
	}

	@Override
	protected Mapping moveToNextMapping() {
		 if (hasNext()) {
	            Mapping r = nextMapping;
	            nextMapping = null;
	            return r;
	     }
	     throw new NoSuchElementException();
	}

}
