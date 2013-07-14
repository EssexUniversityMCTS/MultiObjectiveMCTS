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

            double childValue1 = child.totValue[0]/(child.nVisits + node.epsilon);
            childValue1 = Utils.normalise(childValue1, bounds[0][0], bounds[0][1]);
            double childValue2 = child.totValue[1]/(child.nVisits + node.epsilon);
            childValue2 = Utils.normalise(childValue2, bounds[1][0], bounds[1][1]);

            double childValue = childValue1 * childValue2;

            double uctValue = childValue +
                    K * Math.sqrt(Math.log(node.nVisits + 1) / (child.nVisits + node.epsilon)) +
                    node.r.nextDouble() * node.epsilon;
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
        SimpleTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (SimpleTreeNode child : node.children) {

            double childValue1 = child.totValue[0]/(child.nVisits + node.epsilon);
            childValue1 = Utils.normalise(childValue1, bounds[0][0], bounds[0][1]);
            double childValue2 = child.totValue[1]/(child.nVisits + node.epsilon);
            childValue2 = Utils.normalise(childValue2, bounds[1][0], bounds[1][1]);

            double childValue = childValue1 * childValue2;

            double uctValue = childValue +
                    K * Math.sqrt(Math.log(node.nVisits + 1) / (child.nVisits + node.epsilon)) +
                    node.r.nextDouble() * node.epsilon;
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
}
