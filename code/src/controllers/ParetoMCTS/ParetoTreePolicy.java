package controllers.ParetoMCTS;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 28/02/13
 * Time: 10:27
 * To change this template use File | Settings | File Templates.
 */
public class ParetoTreePolicy implements TreePolicy{

    public double K;

    public ParetoTreePolicy(double a_kValue)
    {
        K = a_kValue;
    }

    public ParetoTreeNode bestChild(ParetoTreeNode node, double[][] bounds) {

        ParetoTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (ParetoTreeNode child : node.children) {

            //double childValue = child.getHV(true) / (child.nVisits + node.epsilon);

            /*double childValue1 = child.totValue[0]/(child.nVisits + node.epsilon);
            childValue1 = Utils.normalise(childValue1, bounds[0][0], bounds[0][1]);
            double childValue2 = child.totValue[1]/(child.nVisits + node.epsilon);
            childValue2 = Utils.normalise(childValue2, bounds[1][0], bounds[1][1]);
            double estimatedValue = childValue1 * childValue2;
            double optParetoHV = child.getHV(true);
            double childValue = 0;
            if(optParetoHV != 0)
                childValue = estimatedValue / (optParetoHV * (child.nVisits + node.epsilon));    */

            /*double childValue1 = child.totValue[0]/(child.nVisits + node.epsilon);
            childValue1 = Utils.normalise(childValue1, bounds[0][0], bounds[0][1]);
            double childValue2 = child.totValue[1]/(child.nVisits + node.epsilon);
            childValue2 = Utils.normalise(childValue2, bounds[1][0], bounds[1][1]);
            double childValue = (childValue1 * childValue2) / (child.nVisits + node.epsilon);
                        */

            //double hvProp = child.getHV(true) / Play.optimalHValue;
            //double childValue = hvProp / (child.nVisits + node.epsilon);

            double hvVal = child.getHV(false);
            double childValue =  hvVal / (child.nVisits + node.epsilon);

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
        return null;  //N/A
    }

}
