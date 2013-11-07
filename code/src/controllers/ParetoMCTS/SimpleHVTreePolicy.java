package controllers.ParetoMCTS;

import controllers.utils.Utils;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 13/02/13
 * Time: 10:29
 * To change this template use File | Settings | File Templates.
 */
public class SimpleHVTreePolicy implements TreePolicy
{
    private double K;

    public SimpleHVTreePolicy(double kValue)
    {
        K = kValue;
    }

    public ParetoTreeNode bestChild(ParetoTreeNode node, double[][] bounds) {
        ParetoTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (ParetoTreeNode child : node.children) {

            int numTargets = bounds.length;
            double childValue = 1;
            for(int i = 0; i < numTargets; ++i)
            {
                double val = child.totValue[i]/(child.nVisits + node.epsilon);
                val = Utils.normalise(val, bounds[i][0], bounds[i][1]);
                childValue *= val;
            }

            double uctValue = childValue +
                    K * Math.sqrt(Math.log(node.nVisits + 1) / (child.nVisits + node.epsilon)) +
                    node.m_rnd.nextDouble() * node.epsilon;
            // small random numbers: break ties in unexpanded nodes
            if (uctValue > bestValue) {
                selected = child;
                bestValue = uctValue;
            }
        }
        if (selected == null)
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + node.children.length);
        return selected;
    }

    public SimpleTreeNode bestChild(SimpleTreeNode node, double[][] bounds) {
        throw new RuntimeException("Warning! Not implemented bestChild(SingleTreeNode,double[][])");
    }
}
