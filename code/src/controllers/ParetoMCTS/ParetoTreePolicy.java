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
        int i = 0;
        for (ParetoTreeNode child : node.children) {

            //If it is not prunned.
            if(!node.m_prunedChildren[i])
            {
                double hvVal = child.getHV(false);
                double childValue =  hvVal;// / (child.nVisits + node.epsilon);

                if(hvVal < 0)
                    System.out.println("Negative HyperVolume: " + hvVal);

                double uctValue = childValue +
                        K * Math.sqrt(Math.log(node.nVisits + 1) / (child.nVisits + node.epsilon)) +
                        node.m_rnd.nextDouble() * node.epsilon;

                // small random numbers: break ties in unexpanded nodes
                if (uctValue > bestValue) {
                    selected = child;
                    bestValue = uctValue;
                }
            }
            ++i;
        }
        if (selected == null)          {
            node.children[0].getHV(false);
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + node.children.length);
        }

        return selected;
    }

    public SimpleTreeNode bestChild(SimpleTreeNode node, double[][] bounds) {
        return null;  //N/A
    }

}
