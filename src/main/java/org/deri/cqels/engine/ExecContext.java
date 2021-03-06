package org.deri.cqels.engine;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.base.file.FileFactory;
import org.apache.jena.tdb.base.file.FileSet;
import org.apache.jena.tdb.base.file.Location;
import org.apache.jena.tdb.index.IndexFactory;
import org.apache.jena.tdb.solver.OpExecutorTDB1;
import org.apache.jena.tdb.store.DatasetGraphTDB;
import org.apache.jena.tdb.store.bulkloader.BulkLoader;
import org.apache.jena.tdb.store.nodetable.NodeTable;
import org.apache.jena.tdb.store.nodetable.NodeTableNative;
import org.apache.jena.tdb.sys.SystemTDB;
import org.apache.jena.tdb.transaction.DatasetGraphTransaction;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.deri.cqels.lang.cqels.ParserCQELS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements CQELS execution context
 *
 * @author Danh Le Phuoc
 * @author Chan Le Van
 * @organization DERI Galway, NUIG, Ireland www.deri.ie
 * @email danh.lephuoc@deri.org
 * @email chan.levan@deri.org
 */
public class ExecContext {

    private static final Logger logger = LoggerFactory.getLogger(
            ExecContext.class);
    CQELSEngine engine;
    RoutingPolicy policy;
    Properties config;
    HashMap<String, Object> hashMap;
    HashMap<Integer, OpRouter> routers;
    DatasetGraphTDB dataset;
    NodeTable dictionary;
    Location location;
    Environment env;
    ExecutionContext arqExCtx;

    /**
     * @param path home path containing dataset
     * @param cleanDataset a flag indicates whether the old dataset will be
     * cleaned or not
     */
    public ExecContext(String path, boolean cleanDataset) {
        this.hashMap = new HashMap<String, Object>();
        //combine cache and disk-based dictionary
        this.dictionary = new NodeTableNative(IndexFactory.buildIndex(
                FileSet.mem(), SystemTDB.nodeRecordFactory), 
                FileFactory.createObjectFileDisk(path + "/dict"));
        setEngine(new CQELSEngine(this));
        createCache(path + "/cache");
        if (cleanDataset) {
            cleanNCreate(path + "/datasets");
        }
        createDataSet(path + "/datasets");

        this.routers = new HashMap<Integer, OpRouter>();
        this.policy = new HeuristicRoutingPolicy(this);
    }

    static void cleanNCreate(String path) {
        deleteDir(new File(path));
        if (!(new File(path)).mkdir()) {
            System.out.println("can not create working directory" + path);
        }
    }

    /**
     * to delete a directory
     *
     * @param dir directory will be deleted
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    System.out.println("can not delete" + dir);
                    return false;
                }
            }
        }
        return dir.delete();
    }

    /**
     * get the ARQ context
     */
    public ExecutionContext getARQExCtx() {
        return this.arqExCtx;
    }

    /**
     * create a dataset with a specified location
     *
     * @param location the specified location string
     */
    public void createDataSet(String location) {
        this.dataset = ((DatasetGraphTransaction) TDBFactory.createDatasetGraph(location)).getBaseDatasetGraph();
        this.arqExCtx = new ExecutionContext(this.dataset.getContext(),
                this.dataset.getDefaultGraph(),
                this.dataset, OpExecutorTDB1.OpExecFactoryTDB);
    }

    /**
     * load a dataset with the specified graph uri and data uri
     *
     * @param graphUri
     * @param dataUri
     */
    public void loadDataset(String graphUri, String dataUri) {
        //FIXME Virtuoso SPARQL Store specific. Must be fixed!
        if (!dataUri.endsWith("sparql-graph-crud")) {
            BulkLoader.loadNamedGraph(dataset,
                    NodeFactory.createURI(graphUri),
                    Arrays.asList(dataUri), false,false);
        } else {
            /**
             * Uses SPARQL GRAPH protocol
             * (http://www.w3.org/TR/sparql11-http-rdf-update/) to fetch a dump
             * of given graph.
             */
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(dataUri + "?graph-uri=" + graphUri);
            //FIXME Asks for NTRIPLES by default
            request.setHeader("Accept", "text/ntriples");
            InputStream stream = null;
            try {
                HttpResponse response = client.execute(request);
                int code = response.getStatusLine().getStatusCode();
                if (code == 200) {
                    stream = response.getEntity().getContent();
                    BulkLoader.loadNamedGraph(
                            dataset,
                            NodeFactory.createURI(graphUri), stream, false,false);
                } else if (code == 404) {
                    System.out.println("SPARQL endpoint [" + dataUri + "] doesn't have [" + graphUri + "] graph!");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * load a dataset with the specified data uri
     *
     * @param dataUri
     */
    public void loadDefaultDataset(String dataUri) {
        BulkLoader.loadDefaultGraph(this.dataset, Arrays.asList(dataUri), true,false);
    }

    /**
     * get the dataset
     *
     * @param dataUri
     */
    public DatasetGraphTDB getDataset() {
        return dataset;
    }

    ;
	
	/**
	 * create cache with the specified path
	 * @param cachePath path string 
	 */
	public void createCache(String cachePath) {
        cleanNCreate(cachePath);
        createEnv(cachePath);
    }

    private void createEnv(String path) {
        EnvironmentConfig config = new EnvironmentConfig();
        config.setAllowCreate(true);
        this.env = new Environment(new File(path), config);
    }

    /**
     * get environment
     *
     * @return
     */
    public Environment env() {
        return this.env;
    }

    /**
     * get CQELS engine
     */
    public CQELSEngine engine() {
        return this.engine;
    }

    /**
     * set CQELS engine
     *
     * @param engine
     */
    public void setEngine(CQELSEngine engine) {
        this.engine = engine;
    }

    /**
     * get routing policy
     */
    public RoutingPolicy policy() {
        return this.policy;
    }

    /**
     * set routing policy with the specified policy
     *
     * @param policy specified policy and mostly heuristic policy in this
     * version
     */
    public void setPolicy(RoutingPolicy policy) {
        this.policy = policy;
    }

    ;
	
	/**
	 * put key and value to the map
	 * @param key
	 * @param value
	 */
	public void put(String key, Object value) {
        this.hashMap.put(key, value);
    }

    /**
     * get the value with the specified key
     *
     * @param key
     */
    public Object get(String key) {
        return this.hashMap.get(key);
    }

    /**
     * init TDB graph with the specified directory
     *
     * @param directory
     */
    public void initTDBGraph(String directory) {
        //this.dataset = TDBFactory.createDatasetGraph(directory);
        this.dataset = ((DatasetGraphTransaction) TDBFactory.createDatasetGraph(location)).getBaseDatasetGraph();
    }

    /**
     * load graph pattern
     *
     * @param op operator
     * @return query iterator
     */
    public QueryIterator loadGraphPattern(Op op) {
        return Algebra.exec(op, this.dataset);
    }

    /**
     * load graph pattern with the specified dataset
     *
     * @param op operator
     * @param ds specified dataset
     * @return query iterator
     */
    public QueryIterator loadGraphPattern(Op op, DatasetGraph ds) {
        return Algebra.exec(op, ds);
    }
    
    public QueryIterator loadGraphPattern(Op op, Model model){
        return Algebra.exec(op, model);
    }

    /**
     * get the cache location
     *
     * @return cache location
     */
    public Location cacheLocation() {
        return this.location;
    }

    /**
     * get the dictionary
     *
     * @return dictionary
     */
    public NodeTable dictionary() {
        return this.dictionary;
    }

    /**
     * get cache configuration
     *
     * @return cache configuration
     */
    public Properties cacheConfig() {
        return this.config;
    }

    /**
     * @param idx
     * @return router
     */
    public void router(int idx, OpRouter router) {
        this.routers.put(Integer.valueOf(idx), router);
    }

    /**
     * @param idx
     * @return router
     */
    public OpRouter router(int idx) {
        return this.routers.get(Integer.valueOf(idx));
    }

    /**
     * register a select query
     *
     * @param queryStr query string
     * @return this method return an instance of ContinuousSelect interface
     */
    public ContinuousSelect registerSelect(String queryStr) {
        Query query = new Query();
        ParserCQELS parser = new ParserCQELS();
        parser.parse(query, queryStr);
        return this.policy.registerSelectQuery(query);
    }
    
    public void unregisterSelect(ContinuousSelect cs) {
        cs.visit(new RouterVisitor() {

            @Override
            public void visit(JoinRouter router) {
                router.destroy();
            }

            @Override
            public void visit(IndexedTripleRouter router) {
                router.destroy();
            }

            @Override
            public void visit(ProjectRouter router) {
                router.destroy();
            }

            @Override
            public void visit(ThroughRouter router) {
                router.destroy();
            }

            @Override
            public void visit(BDBGraphPatternRouter router) {
            }

            @Override
            public void visit(ExtendRouter router) {
                router.destroy();
            }

            @Override
            public void visit(FilterExprRouter router) {
                router.destroy();
            }

            @Override
            public void visit(ContinuousConstruct router) {
                router.destroy();
            }

            @Override
            public void visit(ContinuousSelect router) {
                router.destroy();
            }

            @Override
            public void visit(GroupRouter router) {
                router.destroy();
            }

            @Override
            public void visit(OpRouter router) {
            }
        });
    }

    /**
     * register a construct query
     *
     * @param queryStr query string
     * @return this method return an instance of ContinuousConstruct interface
     */
    public ContinuousConstruct registerConstruct(String queryStr) {
        Query query = new Query();
        ParserCQELS parser = new ParserCQELS();
        parser.parse(query, queryStr);
        return this.policy.registerConstructQuery(query);
    }
    
     public void unregisterConstruct(ContinuousConstruct cons){
        cons.visit(new RouterVisitor() {

            @Override
            public void visit(JoinRouter router) {
                router.destroy();
            }

            @Override
            public void visit(IndexedTripleRouter router) {
                router.destroy();
            }

            @Override
            public void visit(ProjectRouter router) {
                router.destroy();
            }

            @Override
            public void visit(ThroughRouter router) {
                router.destroy();
            }

            @Override
            public void visit(BDBGraphPatternRouter router) {
            }

            @Override
            public void visit(ExtendRouter router) {
                router.destroy();
            }

            @Override
            public void visit(FilterExprRouter router) {
                router.destroy();
            }

            @Override
            public void visit(ContinuousConstruct router) {
                router.destroy();
            }

            @Override
            public void visit(ContinuousSelect router) {
                router.destroy();
            }

            @Override
            public void visit(GroupRouter router) {
                router.destroy();
            }

            @Override
            public void visit(OpRouter router) {
            }
        });
    }
}
