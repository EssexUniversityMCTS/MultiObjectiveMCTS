package controllers.ParetoMCTS;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 12/02/13
 * Time: 12:17
 * To change this template use File | Settings | File Templates.
 */
public interface TreePolicy
{
    public ParetoTreeNode bestChild(ParetoTreeNode node, double[][] bounds);
    public SimpleTreeNode bestChild(SimpleTreeNode node, double[][] bounds);
}
