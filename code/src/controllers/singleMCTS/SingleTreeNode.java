package controllers.singleMCTS;

import framework.core.Game;

import java.util.LinkedList;
import java.util.Random;

public class SingleTreeNode
{
    public SingleMCTSPlayer m_player; //owner of this tree.
    public static LinkedList<SingleTreeNode> m_runList = new LinkedList<SingleTreeNode>();
    public int childIndex;

    public static double epsilon = 1e-6;
    //public static Random r = new Random();
    public Game state;
    public Roller roller;
    public TreePolicy treePolicy;
    public SingleTreeNode parent;
    public SingleTreeNode[] children;
    public double totValue;
    public int nVisits;
    public static Random m_rnd;
    public boolean[] m_prunedChildren;
    public int m_numIters;
    public PlayoutInfo m_pi;

    public int comingFrom = -1;


    public SingleTreeNode()
    {
        this(null, null, -1, null, null, null, null,null);
    }

    public SingleTreeNode(Game state, Roller roller, TreePolicy treePolicy, Random rnd,
                          Player a_player, PlayoutInfo pi) {
        this(state, null, -1, roller, treePolicy, rnd, a_player,pi);
    }

    public SingleTreeNode(Game state, SingleTreeNode parent, int childIndex, Roller roller,
                          TreePolicy treePolicy, Random rnd, Player a_player, PlayoutInfo pi) {
        this.m_player = (SingleMCTSPlayer) a_player;
        this.state = state;
        this.parent = parent;
        this.m_rnd = rnd;
        children = new SingleTreeNode[SingleMCTSParameters.NUM_ACTIONS];
        totValue = 0.0;
        this.roller = roller;
        this.treePolicy = treePolicy;
        this.childIndex = childIndex;
        this.m_prunedChildren = new boolean[SingleMCTSParameters.NUM_ACTIONS];
        this.m_numIters = 0;
        this.m_pi = pi;
    }

    public static int NEXT_TICKS;
    public void mctsSearch(long a_timeDue) {

        long remaining = a_timeDue - System.currentTimeMillis();
        NEXT_TICKS=0;
        int numIters = 500;
        double invIters = 0.0;

        if(treePolicy instanceof SingleEGreedyTreePolicy)
        {
            ((SingleEGreedyTreePolicy) treePolicy).epsilon = 0.1;
            invIters = 0.1/numIters;
        }

        for (int i = 0; i < numIters; i++) {
            //while(remaining > 10)   {
            //while(remaining > 0)   {
            m_runList.clear();
            m_runList.add(this); //root always in.

            m_pi.reset(this.state);
            SingleTreeNode selected = treePolicy();
            addPlayoutInfoTree();
            double delta = selected.rollOut();
            selected.backUp(delta, true);

            m_numIters++;
            remaining = a_timeDue - System.currentTimeMillis();

            if(treePolicy instanceof SingleEGreedyTreePolicy)
            {
                ((SingleEGreedyTreePolicy) treePolicy).epsilon -= invIters;
            }

                //System.out.println("BEST VALUE SO FAR: " + m_player.bestValueFound + " from " + m_player.bestChildFoundIndex);

        }
        //System.out.println("TICKS IN 40 ms: " + NEXT_TICKS);
    }

    public SingleTreeNode treePolicy() {

        SingleTreeNode cur = this;
        int depth = 0;

        try{
            while (cur.nonTerminal() && !cur.state.isEnded() && depth < SingleMCTSParameters.ROLLOUT_DEPTH)
            {
                if (cur.notFullyExpanded()) {
                    SingleTreeNode tn = cur.expand();
                    if(tn != null) //Can happen: if all remaining nodes to be expanded must be pruned.
                    {
                        m_runList.add(0,tn);
                        return tn;
                    }

                    if(cur.allChildrenPruned())
                    {
                        if(cur.parent != null)
                        {
                            cur.parent.m_prunedChildren[cur.childIndex] = true;
                        }
                        m_runList.remove(0);
                        cur = cur.parent;

                        depth--;

                    }else
                    {
                        //Really, do nothing, next iteration we use UCB1
                    }


                } else {
                    SingleTreeNode next = cur.bestChild();
                    cur.comingFrom = next.childIndex;
                    /*if(cur.parent == null)
                    {
                        System.out.println("Starting rollout from " + cur.comingFrom);
                    }*/
                    cur = next;
                    depth++;
                    m_runList.add(0,cur);
                }
           /* if(cur == null)
                System.out.println("CUR is null");
            if(cur.state == null)
                System.out.println("CUR.STATE is null");   */
            }
        }catch(Exception e)
        {
            System.out.println(e);
        }

        return cur;
    }


    public SingleTreeNode expand() {
        // choose a random unused action and add a new node for that

        SingleTreeNode tn = null;
        int prunedN = 0;

        while(tn == null && prunedN < m_prunedChildren.length)
        {
            int bestAction = -1;
            double bestValue = -1;
            for (int i = 0; i < children.length; i++) {
                double x = m_rnd.nextDouble();
                if (x > bestValue && children[i] == null && !m_prunedChildren[i]) {
                    bestAction = i;
                    bestValue = x;
                }
            }

            if(bestValue==-1)
            {
                //No options (because of pruning!)
                return null;
            }

            Game nextState = state.getCopy();
            //nextState.next(bestAction);
            advance(nextState, bestAction);

            if(m_player.getHeuristic().mustBePruned(nextState, state))
            {
                m_prunedChildren[bestAction] = true;
                prunedN++;
            }else{
                tn = new SingleTreeNode(nextState, this, bestAction, this.roller, this.treePolicy,
                        this.m_rnd, this.m_player, this.m_pi);
                children[bestAction] = tn;
                return tn;
            }

        }

        if(tn == null)
        {
            //All children go pruned... prune myself?
            if(parent.m_prunedChildren == null)
                System.out.println("parent.m_prunedChildren is Null");
            parent.m_prunedChildren[childIndex] = true;
        }
        return tn;
    }

    public SingleTreeNode bestChild() {
        return treePolicy.bestChild(this, m_player.getHeuristic().getValueBounds());
    }

    public double rollOut()
    {
        Game rollerState = state.getCopy();
        int thisDepth = this.depth();
        int action = 0;
        // while (!rollerState.isTerminal() && action != -1) {
        while (!finishRollout(rollerState,thisDepth,action)) {
            action = roller.roll(rollerState);
            //rollerState.next(action);
            advance(rollerState, action);
            m_player.getHeuristic().addPlayoutInfo(action, rollerState);
            thisDepth++;
        }

        return m_player.getHeuristic().value(rollerState);
    }

    public void advance(Game st, int action)
    {
        boolean gameOver = false;
        for(int singleAction = 0; !gameOver && singleAction < SingleMCTSParameters.MACRO_ACTION_LENGTH; ++singleAction)
        {
            //((ParetoMCTSPlayer)m_player).m_heightMap[(int)st.getShip().s.x][(int)st.getShip().s.y]++;
            st.tick(action);
            NEXT_TICKS++;
            gameOver = st.isEnded();
        }
    }


    public boolean finishRollout(Game rollerState, int depth, int action)
    {
        if(depth >= SingleMCTSParameters.ROLLOUT_DEPTH)      //rollout end condition.
            return true;

        if(action == -1)                           //end
            return true;

        if(rollerState.isEnded())               //end of game
        {
            //System.out.println("End Reached!");
            return true;
        }

        return false;
    }

    public void backUp(double result, boolean added)
    {
        int numNodes = m_runList.size();
        for(int i = 0; i < numNodes; ++i)
        {
            SingleTreeNode pn = m_runList.get(i);
            pn.nVisits++;
            pn.totValue += result;

            if(i+1 == numNodes)
            {
                if(pn.parent != null)
                    throw new RuntimeException("This should be the root... and it's not.");

                if(result > m_player.bestValueFound)
                {
                    m_player.bestValueFound = result;
                    m_player.bestChildFoundIndex = pn.comingFrom;
                    //System.out.println("new best value found (" + m_player.bestValueFound + "), from " + m_player.bestChildFoundIndex);
                }
            }

        }
    }

    public void addPlayoutInfoTree()
    {
        int numNodes = m_runList.size();
        for(int i = 0; i < numNodes; ++i)
        {
            SingleTreeNode pn = m_runList.get(i);
            if(pn.childIndex > -1)
                m_player.getHeuristic().addPlayoutInfo(pn.childIndex, pn.state);
        }
    }

    public int bestActionIndex() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        for (int i=0; i<children.length; i++) {

            if(!this.m_prunedChildren[i])
            {
                if (children[i] != null && children[i].nVisits + m_rnd.nextDouble() * epsilon > bestValue) {
                    bestValue = children[i].nVisits;
                    selected = i;
                }
            }
        }
        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }
        return selected;
    }

    public int bestActionIndexValue() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        for (int i=0; i<children.length; i++) {

            if(!this.m_prunedChildren[i])
            {
                if (children[i] != null && children[i].totValue + m_rnd.nextDouble() * epsilon > bestValue) {
                    bestValue = children[i].totValue;
                    selected = i;
                }
            }
        }
        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }
        return selected;
    }


    public int bestActionIndexExpected() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        for (int i=0; i<children.length; i++) {

            if(!this.m_prunedChildren[i])
            {
                if (children[i] != null)
                {
                    double val = children[i].totValue / children[i].nVisits;
                    if(val + m_rnd.nextDouble() * epsilon > bestValue){
                        bestValue = val;
                        selected = i;
                    }
                }
            }
        }
        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }

        return selected;
    }

    public int maxValueActionIndex() {
        return m_player.bestChildFoundIndex;
    }



    public void backUp(double[] result) {
        //Nothing to do.
    }


    int depth() {
        if (parent == null) return 0;
        else return 1 + parent.depth();
    }

    boolean nonTerminal() {
        return children != null;
    }

    public boolean notFullyExpanded() {
        int i = 0;
        boolean allPruned = true;
        for (SingleTreeNode tn : children) {
            allPruned &= m_prunedChildren[i];
            if (tn == null && !m_prunedChildren[i]) {
                return true;
            }
            ++i;
        }

        if(allPruned)
            return true;

        return false;
    }

    public boolean allChildrenPruned()
    {
        for (int i = 0; i < m_prunedChildren.length; ++i) {
            if (!m_prunedChildren[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean isLeaf() {
        return children == null;
    }

    public int arity() {
        return children == null ? 0 : children.length;
    }
}
