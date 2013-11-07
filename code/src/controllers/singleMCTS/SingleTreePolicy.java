package controllers.singleMCTS;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 07/11/13
 * Time: 17:33
 */
public class SingleTreePolicy implements TreePolicy
{
    public double K;

    public SingleTreePolicy(double a_kValue)
    {
        K = a_kValue;
    }

    public SingleTreeNode bestChild(SingleTreeNode node, double[] bounds) {

        SingleTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        int i = 0;
        for (SingleTreeNode child : node.children) {

            //If it is not prunned.
            if(!node.m_prunedChildren[i])
            {
                double hvVal = child.totValue;
                double childValue =  hvVal / (child.nVisits + node.epsilon);

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
        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + node.children.length);
        }

        return selected;
    }


}
