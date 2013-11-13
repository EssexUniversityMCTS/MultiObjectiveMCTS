package controllers.singleMCTS;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 07/11/13
 * Time: 17:33
 */
public class SingleEGreedyTreePolicy implements TreePolicy
{
    public double epsilon = 0.1;

    public SingleEGreedyTreePolicy()
    {
    }

    public SingleTreeNode bestChild(SingleTreeNode node, double[] bounds) {

        SingleTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        ArrayList<SingleTreeNode> notBetter = new ArrayList<SingleTreeNode>();
        int i = 0;
        for (SingleTreeNode child : node.children) {

            //If it is not prunned.
            if(!node.m_prunedChildren[i])
            {
                double hvVal = child.totValue;
                double childValue =  hvVal / (child.nVisits + node.epsilon);

                // small random numbers: break ties in unexpanded nodes
                if (childValue > bestValue)
                {
                    if(selected != null)
                    {
                        notBetter.add(selected);
                    }
                    selected = child;
                    bestValue = childValue;
                }else
                {
                    notBetter.add(child);
                }
            }
            ++i;
        }

        if(node.m_rnd.nextDouble() < epsilon)
        {
            int numNonBetter = notBetter.size();
            if(numNonBetter > 0)
                selected = notBetter.get(node.m_rnd.nextInt(numNonBetter));
        }


        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + node.children.length);
        }

        return selected;
    }


}
