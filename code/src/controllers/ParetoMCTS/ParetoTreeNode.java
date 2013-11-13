package controllers.ParetoMCTS;

import controllers.utils.*;
import framework.core.Game;
import lunarcode.LunarGame;
import lunarcode.LunarParetoMCTSController;

import javax.management.RuntimeErrorException;
import java.util.LinkedList;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeMap;

public class ParetoTreeNode {

    public Player m_player; //owner of this tree.
    public ParetoArchive pa;
    public TreeMap<Integer, LinkedList<Solution>> valueRoute;
    public static LinkedList<ParetoTreeNode> m_runList = new LinkedList<ParetoTreeNode>();
    public int childIndex;

    public static double epsilon = 1e-6;
    //public static Random r = new Random();
    public Game state;
    public Roller roller;
    public TreePolicy treePolicy;
    public ParetoTreeNode parent;
    public ParetoTreeNode[] children;
    public double[] totValue;
    public int nVisits;
    public static Random m_rnd;
    public boolean[] m_prunedChildren;
    public int m_numIters;
    public PlayoutInfo m_pi;


    public ParetoTreeNode()
    {
        this(null, null, -1, null, null, null, null,null);
    }

    public ParetoTreeNode(Game state, Roller roller, TreePolicy treePolicy, Random rnd,
                          Player a_player, PlayoutInfo pi) {
        this(state, null, -1, roller, treePolicy, rnd, a_player,pi);
    }

    public ParetoTreeNode(Game state, ParetoTreeNode parent, int childIndex, Roller roller,
                          TreePolicy treePolicy, Random rnd, Player a_player, PlayoutInfo pi) {
        this.m_player = a_player;
        this.state = state;
        this.parent = parent;
        this.m_rnd = rnd;
        children = new ParetoTreeNode[ParetoMCTSParameters.NUM_ACTIONS];
        totValue = new double[ParetoMCTSParameters.NUM_TARGETS];
        this.roller = roller;
        this.treePolicy = treePolicy;
        pa = new ParetoArchive();
        this.childIndex = childIndex;
        this.m_prunedChildren = new boolean[ParetoMCTSParameters.NUM_ACTIONS];
        this.m_numIters = 0;
        this.m_pi = pi;
        
        if(parent == null) //This is only for the root:
        {
            this.initValueRoute();
        }
    }

    public static int NEXT_TICKS;
    public void mctsSearch(long a_timeDue) {

        long remaining = a_timeDue - System.currentTimeMillis();

        NEXT_TICKS=0;
        int numIters = 500;
        double invIters = 0.0;

        if(treePolicy instanceof ParetoEGreedyTreePolicy)
        {
            ((ParetoEGreedyTreePolicy) treePolicy).epsilon = 0.1;
            invIters = 0.1/numIters;
        }

        for (int i = 0; i < numIters; i++) {
        //while(remaining > 10)   {
        //while(remaining > 0)   {
            m_runList.clear();
            m_runList.add(this); //root always in.

            m_pi.reset(this.state);
            ParetoTreeNode selected = treePolicy();
            addPlayoutInfoTree();
            double delta[] = selected.rollOut();
            Solution deltaSol = new Solution(delta);
            selected.backUp(delta, deltaSol, true, selected.childIndex);

            m_numIters++;
            remaining = a_timeDue - System.currentTimeMillis();

            if(treePolicy instanceof ParetoEGreedyTreePolicy)
            {
                ((ParetoEGreedyTreePolicy) treePolicy).epsilon -= invIters;
            }
        }
        //System.out.println("TICKS IN 40 ms: " + NEXT_TICKS);
    }

    public ParetoTreeNode treePolicy() {

        ParetoTreeNode cur = this;
        int depth = 0;

        try{
        while (cur.nonTerminal() && !cur.state.isEnded() && depth < ParetoMCTSParameters.ROLLOUT_DEPTH)
        {
            if (cur.notFullyExpanded()) {
                ParetoTreeNode tn = cur.expand();
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
                cur = cur.bestChild();
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


    public ParetoTreeNode expand() {
        // choose a random unused action and add a new node for that

        ParetoTreeNode tn = null;
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
                tn = new ParetoTreeNode(nextState, this, bestAction, this.roller, this.treePolicy,
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

    public ParetoTreeNode bestChild() {
        return treePolicy.bestChild(this, m_player.getHeuristic().getValueBounds());
    }

    public double[] rollOut()
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
        for(int singleAction = 0; !gameOver && singleAction < ParetoMCTSParameters.MACRO_ACTION_LENGTH; ++singleAction)
        {
            //((ParetoMCTSPlayer)m_player).m_heightMap[(int)st.getShip().s.x][(int)st.getShip().s.y]++;
            st.tick(action);
            NEXT_TICKS++;
            gameOver = st.isEnded();
        }
    }


    public boolean finishRollout(Game rollerState, int depth, int action)
    {
        if(depth >= ParetoMCTSParameters.ROLLOUT_DEPTH)      //rollout end condition.
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

    public void backUp(double result[], Solution sol, boolean added, int cI) {

        /*nVisits++;
        added = pa.add(result);
        int comingFrom = cI;

        for(int i = 0; i < result.length; ++i)
            totValue[i] += result[i];      */

        //for(ParetoTreeNode pn : m_runList)
        int comingFrom = -1;
        int numNodes = m_runList.size();
        for(int i = 0; i < numNodes; ++i)
        {
            ParetoTreeNode pn = m_runList.get(i);
            pn.nVisits++;

            if(added)
                added = pn.pa.add(sol);

            for(int j = 0; j < result.length; ++j)
                pn.totValue[j] += result[j];

            if(i+1 < numNodes)
            {
                //ParetoTreeNode parentNode = m_runList.get(i+1);
                //parentNode.m_childCount[pn.childIndex]++; //for Nsa in one of the tree policies (see TransParetoTreePolicy).
                comingFrom = pn.childIndex;
            }
            else if(i+1 == numNodes)
            {
                if(pn.parent != null)
                    throw new RuntimeException("This should be the root... and it's not.");

                if(added)
                {
                    //System.out.println("ADDING (" + result[0] + "," + result[1] + ") to child " + comingFrom + " from " + pn.parent);
                    if(comingFrom != -1)
                    {
                        sol.m_through = comingFrom;
                        pn.valueRoute.get(comingFrom).add(sol);
                    }
                }

            }

        }
    }

    public void addPlayoutInfoTree()
    {
        int numNodes = m_runList.size();
        for(int i = 0; i < numNodes; ++i)
        {
            ParetoTreeNode pn = m_runList.get(i);
            if(pn.childIndex > -1)
                m_player.getHeuristic().addPlayoutInfo(pn.childIndex, pn.state);
        }
    }

    public void printStats()
    {
        System.out.println("************** Root archive **************");
        this.pa.printArchive();
        if(false) for(int i = 0; i < children.length; ++i)
        {
            ParetoTreeNode pnCh = children[i];
            System.out.println("*********** Child " + i + " **************");
            pnCh.pa.printArchive();
            System.out.println("***************************************");
        }
        System.out.println("********************************************");

    }

    public int bestActionIndexEuqDistance(double[] targets) {
        boolean verbose = false;
        int selected = -1;
        double[][] bounds = m_player.getHeuristic().getValueBounds();
        double distance = Double.MAX_VALUE;
        OrderedSolutionList myPA = pa.m_members;
        int numMembers =  myPA.size();
        if(verbose && numMembers>1)
            System.out.println("Choosing among " + myPA.size() + " members.");

        for(int i = 0; i < numMembers; ++i)
        {
            double[] thisRes = myPA.get(i).m_data;

            double val[] = new double[targets.length];
            for(int t = 0; t < targets.length; ++t)
            {
                double v =  Utils.normalise(thisRes[t], bounds[t][0], bounds[t][1]);
                val[t] = v;
            }

            double thisDist = Utils.distanceEuq(val, targets);
            if(thisDist < distance)
            {
                distance = thisDist;
                selected = i;
            }
        }

        if(verbose && numMembers>1)
            System.out.println("   Selected: " + selected);

        double selectedTarget[] = myPA.get(selected).m_data;
        NavigableSet<Integer> navSet = valueRoute.navigableKeySet();
        for(Integer key : navSet)
        {
            LinkedList<Solution> resFromThisChild = valueRoute.get(key);

            for(int i =0; i < resFromThisChild.size(); ++i)
            {
                double[] sol = resFromThisChild.get(i).m_data;

                if(sol.length == 3 && sol[0] == selectedTarget[0] && sol[1] == selectedTarget[1] && sol[2] == selectedTarget[2])
                {
                    return key;
                }else if(sol.length == 2 && sol[0] == selectedTarget[0] && sol[1] == selectedTarget[1])
                {
                    return key;
                }
            }
        }
        //throw new RuntimeException("Unexpected selection: " + selected);
        return selected;

    }


    public int bestActionIndex(double[] targets) {
        boolean verbose = false;
        int selected = -1;
        double[][] bounds = m_player.getHeuristic().getValueBounds();
        double bestValue = -Double.MAX_VALUE;
        OrderedSolutionList myPA = pa.m_members;
        int numMembers =  myPA.size();
        if(verbose && numMembers>1)
            System.out.println("Choosing among " + myPA.size() + " members.");
        for(int i = 0; i < numMembers; ++i)
        {
            double[] thisRes = myPA.get(i).m_data;
            /*
            double val0 = Utils.normalise(thisRes[0], bounds[0][0], bounds[0][1]);
            double val1 = Utils.normalise(thisRes[1], bounds[1][0], bounds[1][1]);
            double val2 = Utils.normalise(thisRes[2], bounds[2][0], bounds[2][1]);
            double val = targets[0] * val0 + targets[1] * val1 + targets[2] * val2;*/

            double val = 0.0;
            for(int t = 0; t < targets.length; ++t)
            {
                double v =  Utils.normalise(thisRes[t], bounds[t][0], bounds[t][1]);
                val += v*targets[t];
            }


            if(verbose && numMembers>1)
            {
                if(thisRes.length==3)
                    System.out.format("   [%.4f, %.4f, %.4f] => %.4f, from %d\n", thisRes[0], thisRes[1], thisRes[2], val, myPA.get(i).m_through);
                if(thisRes.length==2)
                    System.out.format("   [%.4f, %.4f] => %.4f, from %d\n", thisRes[0], thisRes[1], val, myPA.get(i).m_through);
            }
            //System.out.println("Element in PA " + i + ": " + val);

            if(val > bestValue) {
                bestValue = val;
                selected = i;
            }

        }

        if(verbose && numMembers>1)
            System.out.println("   Selected: " + selected);

        if(selected == -1)
        {
            //System.out.println(" ********************* SELECTED -1, myPA.size(): " + myPA.size() + " ***************");
            return 0;
        }

        double selectedTarget[] = myPA.get(selected).m_data;
        NavigableSet<Integer> navSet = valueRoute.navigableKeySet();
        for(Integer key : navSet)
        {
            LinkedList<Solution> resFromThisChild = valueRoute.get(key);
            
            for(int i =0; i < resFromThisChild.size(); ++i)
            {
                double[] sol = resFromThisChild.get(i).m_data;
                //System.out.println("PA point " + key + ":" + i + ": " + sol[0] + ", " + sol[1] + ", nVis: " + children[key].nVisits);

                if(sol.length == 3 && sol[0] == selectedTarget[0] && sol[1] == selectedTarget[1] && sol[2] == selectedTarget[2])
                //if(sol[0] == selectedTarget[0] && sol[1] == selectedTarget[1])
                {
                    //System.out.println("SELECTED-3: " + children[key].nVisits + "," + sol[0] + "," + sol[1] + ": " + key);
                    return key;
                }else if(sol.length == 2 && sol[0] == selectedTarget[0] && sol[1] == selectedTarget[1])
                {
                    //System.out.println("SELECTED-2: " + children[key].nVisits + "," + sol[0] + "," + sol[1] + ": " + key);
                    return key;
                }
            }
        }


        //If we get down here, we've done something wrong.
       /* pa.printArchive();

        System.out.println("Looking for: " + selectedTarget[0] + "," + selectedTarget[1]);
        for(Integer key : navSet)
        {
            LinkedList<double[]> resFromThisChild = valueRoute.get(key);

            for(int i =0; i < resFromThisChild.size(); ++i)
            {
                double[] sol = resFromThisChild.get(i);
                System.out.println(key + ": " + sol[0] + "," + sol[1]);
                if(sol[0] == selectedTarget[0] && sol[1] == selectedTarget[1])
                    System.out.println("FOUND!");
            }
        }
                  */
        //throw new RuntimeException("Unexpected selection: " + selected);
        return selected;
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
        if (selected == -1) throw new RuntimeException("Unexpected selection!");
        return selected;
    }

    public int bestActionIndexValue() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        for (int i=0; i<children.length; i++) {

            if(!this.m_prunedChildren[i])
            {
                if (children[i] != null && children[i].totValue[0] + m_rnd.nextDouble() * epsilon > bestValue) {
                    bestValue = children[i].totValue[0];
                    selected = i;
                }
            }
        }
        if (selected == -1) throw new RuntimeException("Unexpected selection!");
        return selected;
    }


    public int bestActionIndexExpected() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        for (int i=0; i<children.length; i++) {

            if(!this.m_prunedChildren[i])
            {
               // System.out.println("Child " + i + ": " + sol[0] + ", " + sol[1] + ", nVis: " + children[i].nVisits);
                if (children[i] != null)
                {
                    double val = children[i].totValue[0] / children[i].nVisits;
                    if(val + m_rnd.nextDouble() * epsilon > bestValue){
                        bestValue = val;
                        selected = i;
                    }
                }
            }
        }
        if (selected == -1) throw new RuntimeException("Unexpected selection!");

        //double sol[] = children[selected].pa.m_members.get(0).m_data;
        //System.out.println("SELECTED: " + (int)bestValue + "," + sol[0] + "," + sol[1] + ": " + selected);

        return selected;
    }




    public void backUp(double[] result) {
        //Nothing to do.
    }

    public double getHV(boolean a_normalized)
    {
        if(a_normalized)
            return pa.computeHV(m_player.getHeuristic().getValueBounds());
        else
            return pa.computeHV();

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
        for (ParetoTreeNode tn : children) {
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

    public void initValueRoute()
    {
        this.valueRoute = new TreeMap<Integer, LinkedList<Solution>>();
        for(int i = 0; i < ParetoMCTSController.NUM_ACTIONS; ++i)
        {
            this.valueRoute.put(i,new LinkedList<Solution>());
        }
    }

}
