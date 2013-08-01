package controllers.ParetoMCTS;

import controllers.utils.*;
import framework.core.Game;

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

    public ParetoTreeNode()
    {
        this(null, null, -1, null, null, null, null);
    }

    public ParetoTreeNode(Game state, Roller roller, TreePolicy treePolicy, Random rnd, Player a_player) {
        this(state, null, -1, roller, treePolicy, rnd, a_player);
    }

    public ParetoTreeNode(Game state, ParetoTreeNode parent, int childIndex, Roller roller,
                          TreePolicy treePolicy, Random rnd, Player a_player) {
        this.m_player = a_player;
        this.state = state;
        this.parent = parent;
        this.m_rnd = rnd;
        children = new ParetoTreeNode[ParetoMCTSController.NUM_ACTIONS];
        totValue = new double[ParetoMCTSController.NUM_TARGETS];
        this.roller = roller;
        this.treePolicy = treePolicy;
        pa = new ParetoArchive();
        this.childIndex = childIndex;
        this.m_prunedChildren = new boolean[ParetoMCTSController.NUM_ACTIONS];
        
        if(parent == null) //This is only for the root:
        {
            this.initValueRoute();
        }
    }


    public void mctsSearch(long a_timeDue) {

        long remaining = a_timeDue - System.currentTimeMillis();
        int its = 0;

        //for (int i = 0; i < 800; i++) {
        while(remaining > 10)   {
            m_runList.clear();
            m_runList.add(this); //root always in.

            ParetoTreeNode selected = treePolicy();
            double delta[] = selected.rollOut();
            Solution deltaSol = new Solution(delta);
            selected.backUp(delta, deltaSol, true, selected.childIndex);

            its++;
            remaining = a_timeDue - System.currentTimeMillis();
        }
        System.out.print(its+",");
    }

    public ParetoTreeNode treePolicy() {

        ParetoTreeNode cur = this;
        while (cur.nonTerminal() && !cur.state.isEnded())
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
                    cur = cur.parent;

                }else
                {
                    //Really, do nothing, next iteration we use UCT1
                }


            } else {
                cur = cur.bestChild();
                m_runList.add(0,cur);
            }
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
                tn = new ParetoTreeNode(nextState, this, bestAction, this.roller, this.treePolicy, this.m_rnd, this.m_player);
                children[bestAction] = tn;
                return tn;
            }

        }

        if(tn == null)
        {
            //All children go pruned... prune myself?
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
            thisDepth++;
        }

        return m_player.getHeuristic().value(rollerState);
    }

    public void advance(Game st, int action)
    {
        boolean gameOver = false;
        for(int singleAction = 0; !gameOver && singleAction < ParetoMCTSController.MACRO_ACTION_LENGTH; ++singleAction)
        {
            ((ParetoMCTSPlayer)m_player).m_heightMap[(int)st.getShip().s.x][(int)st.getShip().s.y]++;
            st.tick(action);
            gameOver = st.isEnded();
        }
    }


    public boolean finishRollout(Game rollerState, int depth, int action)
    {
        if(depth >= ParetoMCTSController.ROLLOUT_DEPTH)      //rollout end condition.
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


    public int bestActionIndex(double[] targets) {
        int selected = -1;
        double[][] bounds = m_player.getHeuristic().getValueBounds();
        double bestValue = -Double.MAX_VALUE;
        OrderedSolutionList myPA = pa.m_members;
        //System.out.println("----------------");
        for(int i = 0; i < myPA.size(); ++i)
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

           // System.out.println("Element in PA " + i + ": " + val);

            if(val > bestValue) {
                bestValue = val;
                selected = i;
            }
        }

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
        double bestValue = Double.MIN_VALUE;
        for (int i=0; i<children.length; i++) {
            double sol[] = children[i].pa.m_members.get(0).m_data;
           // System.out.println("Child " + i + ": " + sol[0] + ", " + sol[1] + ", nVis: " + children[i].nVisits);
            if (children[i] != null && children[i].nVisits + m_rnd.nextDouble() * epsilon > bestValue) {
                bestValue = children[i].nVisits;
                selected = i;
            }
        }
        if (selected == -1) throw new RuntimeException("Unexpected selection!");

        double sol[] = children[selected].pa.m_members.get(0).m_data;
        //System.out.println("SELECTED: " + (int)bestValue + "," + sol[0] + "," + sol[1] + ": " + selected);

        return selected;
    }


    public int bestActionIndex(ParetoArchive globalPA, double[] targets) {
        int selected = -1;
        double[][] bounds = m_player.getHeuristic().getValueBounds();
        OrderedSolutionList paMembers = pa.m_members;

        //if(pa.m_members.size() > 1)
        //    System.out.println("HEY: " + pa.m_members.size());

        double distance = Double.MAX_VALUE;
        for(int i = 0; i < paMembers.size(); ++i)
        {
            double[] thisRes = paMembers.get(i).m_data;

            //if(pa.contains(thisRes))
            {
                double val0 = Utils.normalise(thisRes[0], bounds[0][0], bounds[0][1]);
                double val1 = Utils.normalise(thisRes[1], bounds[1][0], bounds[1][1]);
                double[] thisResNorm = new double[]{val0, val1};
                double thisDist = Utils.distanceEuq(thisResNorm, targets);
                if(thisDist < distance)
                {
                    distance = thisDist;
                    selected = i;
                }
            }
        }

        double selectedTarget[] = paMembers.get(selected).m_data;
        NavigableSet<Integer> navSet = valueRoute.navigableKeySet();
        for(Integer key : navSet)
        {
            LinkedList<Solution> resFromThisChild = valueRoute.get(key);

            for(int i =0; i < resFromThisChild.size(); ++i)
            {
                double[] sol = resFromThisChild.get(i).m_data;
                if(sol[0] == selectedTarget[0] && sol[1] == selectedTarget[1])
                    return key;
            }
        }
        throw new RuntimeException("Unexpected selection: " + selected);

    }

    public void backUp(double[] result) {
        //Nothing to do.
    }

    public double getHV(boolean a_normalized)
    {
        if(a_normalized)
            return pa.computeHV(m_player.getHeuristic().getValueBounds());
        else return
                pa.computeHV();

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
