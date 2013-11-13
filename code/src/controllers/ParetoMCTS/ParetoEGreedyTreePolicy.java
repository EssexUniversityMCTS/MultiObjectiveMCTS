package controllers.ParetoMCTS;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 28/02/13
 * Time: 10:27
 * To change this template use File | Settings | File Templates.
 */
public class ParetoEGreedyTreePolicy implements TreePolicy{


    public double epsilon = 0.1;

    public ParetoEGreedyTreePolicy()
    {
    }

    public ParetoTreeNode bestChild(ParetoTreeNode node, double[][] bounds) {

        ParetoTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        ArrayList<ParetoTreeNode> notBetter = new ArrayList<ParetoTreeNode>();
        int i = 0;
        for (ParetoTreeNode child : node.children) {

            //If it is not prunned.
            if(!node.m_prunedChildren[i])
            {
                double hvVal = child.getHV(false);
                double childValue =  hvVal / (child.nVisits + node.epsilon);

                if(hvVal < 0)
                    System.out.println("Negative HyperVolume: " + hvVal);

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
        if (selected == null)          {
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + node.children.length);
        }

        if(node.m_rnd.nextDouble() < epsilon)
        {
            int numNonBetter = notBetter.size();
            if(numNonBetter > 0)
                selected = notBetter.get(node.m_rnd.nextInt(numNonBetter));
        }

        return selected;
    }

    public SimpleTreeNode bestChild(SimpleTreeNode node, double[][] bounds) {
        return null;  //N/A
    }

}
