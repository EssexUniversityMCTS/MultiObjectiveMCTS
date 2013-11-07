package controllers.ParetoMCTS;

import controllers.utils.ParetoArchive;
import controllers.utils.Solution;
import framework.core.Game;

import java.util.Random;

public class SimpleTreeNode {

    public Player m_player;
    public ParetoArchive pa;
    public double[] totValue;
    public static double epsilon = 1e-6;
    public static Random r = new Random();
    Game state;
    public int nVisits;
    public Roller roller;
    public TreePolicy treePolicy;
    public SimpleTreeNode parent;
    public SimpleTreeNode[] children;


    public SimpleTreeNode(Game state, Roller roller, TreePolicy treePolicy, Player a_player) {
        this(state, null, roller, treePolicy, a_player);
        this.roller = roller;
        this.treePolicy = treePolicy;
        pa = new ParetoArchive();
    }

    public SimpleTreeNode(Game state, SimpleTreeNode parent, Roller roller, TreePolicy treePolicy, Player a_player) {
        this.m_player = a_player;
        this.parent = parent;
        children = new SimpleTreeNode[ParetoMCTSController.NUM_ACTIONS];
        totValue = new double[ParetoMCTSParameters.NUM_TARGETS];
        this.roller = roller;
        this.treePolicy = treePolicy;
        // System.out.println("Made a TreeNode of depth " + depth() + ", arity " + children.length);
    }

    public void mctsSearch(int its) {
        for (int i = 0; i < its; i++) {
            SimpleTreeNode selected = treePolicy();
            double delta[] = selected.rollOut();
            pa.add(new Solution(delta));  //Add the result of the new tree walk to the pareto front (it checks for dominance)
            selected.backUp(delta);
        }
    }

    public void backUp(double result[]) {
        nVisits++;
        for(int i = 0; i < result.length; ++i)
            totValue[i] += result[i];

        if (parent != null) parent.backUp(result);
    }

    public void backUp(double[] result, boolean flag, int childIndex) {
        //Nothing to do
    }

    public SimpleTreeNode treePolicy() {


        SimpleTreeNode cur = this;
        while (cur.nonTerminal() && !cur.state.isEnded()) {

            if (cur.notFullyExpanded()) {
                return cur.expand();
            } else {
                cur = cur.bestChild();
            }
            // System.out.println("cur = " + cur);
        }
        return cur;
    }

    public SimpleTreeNode bestChild() {
        return treePolicy.bestChild(this, m_player.getHeuristic().getValueBounds());
    }

    public int bestActionIndex() {
        int selected = -1;
        double bestValue = Double.MIN_VALUE;
        for (int i=0; i<children.length; i++) {
            if (children[i] != null && children[i].nVisits + r.nextDouble() * epsilon > bestValue) {
                bestValue = children[i].nVisits;
                selected = i;
            }
        }
        if (selected == -1) throw new RuntimeException("Unexpected selection!");
        return selected;
    }

    public int bestActionIndex(double target0, double target1) {
        throw new RuntimeException("Not implemented in SingleTreeNode!");
    }

    public SimpleTreeNode expand() {
        // choose a random unused action and add a new node for that
        int bestAction = -1;
        double bestValue = -1;
        for (int i = 0; i < children.length; i++) {
            double x = r.nextDouble();
            if (x > bestValue && children[i] == null) {
                bestAction = i;
                bestValue = x;
            }
        }
        Game nextState = state.getCopy();
        nextState.tick(bestAction);
        SimpleTreeNode tn = new SimpleTreeNode(nextState, this, this.roller, this.treePolicy, this.m_player);
        children[bestAction] = tn;
        return tn;
    }

    public double[] rollOut()
    {
        Game rollerState = state.getCopy();
        int action = 0;
        while (!rollerState.isEnded() && action != -1) {
            action = roller.roll(rollerState);
            if(action >= 0)
            {
                rollerState.tick(action);
            }
        }
        return m_player.getHeuristic().value(rollerState);
    }


    int depth() {
        if (parent == null) return 0;
        else return 1 + parent.depth();
    }

    boolean nonTerminal() {
        return children != null;
    }

    public boolean notFullyExpanded() {
        for (SimpleTreeNode tn : children) {
            if (tn == null) {
                return true;
            }
        }
        return false;
    }

    public boolean isLeaf() {
        return children == null;
    }

    public int arity() {
        return children == null ? 0 : children.length;
    }


}
