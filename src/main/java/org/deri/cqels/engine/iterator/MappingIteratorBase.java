package org.deri.cqels.engine.iterator;

import java.util.NoSuchElementException;
import org.deri.cqels.data.Mapping;
import org.apache.jena.query.QueryCancelledException;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFatalException;
//import org.apache.jena.sparql.util.Utils;
import org.apache.jena.atlas.lib.Lib;
import org.apache.jena.atlas.logging.Log;

public abstract class MappingIteratorBase implements MappingIterator {

    private boolean finished = false;
    private boolean requestingCancel = false;
    private volatile boolean abortIterator = false;

    protected abstract void closeIterator();

    protected abstract boolean hasNextMapping();

    protected abstract Mapping moveToNextMapping();

    protected abstract void requestCancel();

    @Override
    public boolean hasNext() {

        return hasNextMapping();
    }

    @Override
    public Mapping next() {

        return nextMapping();
    }

    protected boolean isFinished() {
        return finished;
    }

    @Override
    public final void remove() {
        Log.warn(this, "Call to QueryIterator.remove() : " + Lib.className(this) + ".remove");
        throw new UnsupportedOperationException(Lib.className(this) + ".remove");
    }

    @Override
    public Mapping nextMapping() {
        try {
            if (abortIterator) {
                throw new QueryCancelledException();
            }
            if (finished) {
                // If abortIterator set after finished.
                if (abortIterator) {
                    throw new QueryCancelledException();
                }
                throw new NoSuchElementException(Lib.className(this));
            }

            if (!hasNextMapping()) {
                throw new NoSuchElementException(Lib.className(this));
            }

            Mapping obj = moveToNextMapping();
            if (obj == null) {
                throw new NoSuchElementException(Lib.className(this));
            }
            if (requestingCancel && !finished) {
                // But .cancel sets both requestingCancel and abortIterator
                // This only happens with a continuing iterator.
                close();
            }

            return obj;
        } catch (QueryFatalException ex) {
            Log.fatal(this, "QueryFatalException", ex);
            //abort ? 
            throw ex;
        }
    }

    protected static void performRequestCancel(MappingIterator iter) {
        if (iter == null) {
            return;
        }
        iter.cancel();
    }

    protected static void performClose(MappingIterator iter) {
        if (iter == null) {
            return;
        }
        iter.close();
    }

    @Override
    public void cancel() {
        if (!this.requestingCancel) {
            synchronized (this) {
                this.requestCancel();
                this.requestingCancel = true;
                this.abortIterator = true;
            }
        }
    }

    @Override
    public void close() {
        if (finished) {
            return;
        }
        try {
            closeIterator();
        } catch (QueryException ex) {
            Log.warn(this, "QueryException in close()", ex);
        }
        finished = true;
    }

}
