package org.deri.cqels.engine;

import java.util.List;
import org.deri.cqels.data.Mapping;
import org.deri.cqels.data.ProjectMapping;
import org.deri.cqels.engine.iterator.MappingIterOnQueryIter;
import org.deri.cqels.engine.iterator.MappingIterator;
import org.deri.cqels.engine.iterator.QueryIterOnMappingIter;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.iterator.QueryIterGroup;
import org.apache.jena.sparql.expr.ExprAggregator;

/**
 * This class implements the router with group-by operator
 *
 * @author Danh Le Phuoc
 * @author Chan Le Van
 * @organization DERI Galway, NUIG, Ireland www.deri.ie
 * @email danh.lephuoc@deri.org
 * @email chan.levan@deri.org
 * @see OpRouter1
 */
public class GroupRouter extends OpRouter1 {

    private final VarExprList groupVars;
    private final List<ExprAggregator> aggregators;

    public GroupRouter(ExecContext context, OpGroup op, OpRouter sub) {
        super(context, op, sub);
        this.groupVars = ((OpGroup) op).getGroupVars();
        this.aggregators = ((OpGroup) op).getAggregators();
    }

    @Override
    public void route(Mapping mapping) {
        ProjectMapping project = new ProjectMapping(
                context, mapping, groupVars.getVars());
        MappingIterator itr = calc(mapping.from().searchBuff4Match(project));
        while (itr.hasNext()) {
            Mapping _mapping = itr.next();
            _route(_mapping);
        }
        itr.close();
    }

    @Override
    public MappingIterator searchBuff4Match(Mapping mapping) {
        return calc(sub().searchBuff4Match(mapping));
    }

    private MappingIterator calc(MappingIterator itr) {
        QueryIterGroup groupItrGroup = new QueryIterGroup(
                new QueryIterOnMappingIter(context, itr),
                groupVars, aggregators, context.getARQExCtx());
        return new MappingIterOnQueryIter(context, groupItrGroup);
    }

    @Override
    public MappingIterator getBuff() {
        return calc(sub().getBuff());
    }

    @Override
    public void visit(RouterVisitor rv) {
        rv.visit(this);
        sub().visit(rv);
    }

    public void destroy() {
        aggregators.clear();
        context.policy().removeRouter(sub(), this);
    }
}
