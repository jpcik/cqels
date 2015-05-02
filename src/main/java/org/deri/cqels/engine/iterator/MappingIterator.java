package org.deri.cqels.engine.iterator;

import java.util.Iterator;

import org.deri.cqels.data.Mapping;
import org.apache.jena.atlas.lib.Closeable;

public interface MappingIterator extends Closeable, Iterator<Mapping> {

    /**
     * Get next binding
     */
    public Mapping nextMapping();

    /**
     * Cancels the query as soon as is possible for the given iterator
     */
    public void cancel();
}
