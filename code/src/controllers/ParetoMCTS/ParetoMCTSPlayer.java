package controllers.ParetoMCTS;

import controllers.utils.ParetoArchive;
import framework.core.Game;

import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 12/02/13
 * Time: 11:04
 * To change this template use File | Settings | File Templates.
 */
public class ParetoMCTSPlayer implements Player {

    TreePolicy m_treePolicy;
    ParetoTreeNode m_root;
    Random m_rnd;
    double[] m_targetWeights;
    public ParetoArchive m_globalPA;
    Roller m_randomRoller;
    HeuristicMO m_heuristic;


    public ParetoMCTSPlayer(TreePolicy a_treePolicy, HeuristicMO a_h, Random a_rnd, double[] a_targetWeights)
    {
        m_heuristic = a_h;
        m_treePolicy = a_treePolicy;
        this.m_rnd = a_rnd;
        this.m_targetWeights = a_targetWeights;
        m_globalPA = new ParetoArchive();
        m_randomRoller = new RandomRoller(RandomRoller.RANDOM_ROLLOUT, this.m_rnd);
        m_root = new ParetoTreeNode(null, m_randomRoller,m_treePolicy, m_rnd, this);
    }

    public void init()
    {
        m_root = new ParetoTreeNode(null, m_randomRoller,m_treePolicy,m_rnd, this);
    }

    public int run(Game a_gameState, long a_timeDue)
    {
        m_root.state = a_gameState;

        m_root.mctsSearch(a_timeDue);
        int nextAction = m_root.bestActionIndex(m_targetWeights);

        /*for(int i = 0; i < m_root.pa.m_members.size(); ++i)
        {
            m_globalPA.add(m_root.pa.m_members.get(i));
        }   */


        return nextAction;
    }

    public HeuristicMO getHeuristic(){return m_heuristic;}

    public double getHV(boolean a_normalized)
    {
        return m_root.getHV(a_normalized);
    }

    public void reset(){}
}
