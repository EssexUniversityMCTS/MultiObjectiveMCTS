package controllers.singleMCTS;


/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 12/02/13
 * Time: 12:17
 * To change this template use File | Settings | File Templates.
 */
public interface TreePolicy
{
    public SingleTreeNode bestChild(SingleTreeNode node, double[] bounds);
}
