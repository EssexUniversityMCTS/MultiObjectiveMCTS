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
    public ParetoTreeNode m_root;
    Random m_rnd;
    double[] m_targetWeights;
    public ParetoArchive m_globalPA;
    Roller m_randomRoller;
    HeuristicMO m_heuristic;
    PlayoutInfo m_playoutInfo;

    /**
     * Debug height map
     */
    //public int[][] m_heightMap;
    public int m_numCalls;
    public int m_numIters;


    public ParetoMCTSPlayer(TreePolicy a_treePolicy, HeuristicMO a_h, Random a_rnd, Game a_game, PlayoutInfo pInfo)
    {
        m_playoutInfo = pInfo;
        //m_heightMap = new int[a_game.getMap().getMapWidth()][a_game.getMap().getMapHeight()];
        m_heuristic = a_h;
        m_heuristic.setPlayoutInfo(m_playoutInfo);
        m_treePolicy = a_treePolicy;
        this.m_rnd = a_rnd;
        this.m_targetWeights = ParetoMCTSParameters.targetWeights;
        m_globalPA = new ParetoArchive();
        m_randomRoller = new RandomRoller(RandomRoller.RANDOM_ROLLOUT, this.m_rnd, ParetoMCTSParameters.NUM_ACTIONS);
        m_root = new ParetoTreeNode(null, m_randomRoller,m_treePolicy, m_rnd, this,m_playoutInfo);
        this.m_numCalls = 0;
        this.m_numIters = 0;
    }

    public void init()
    {
        m_root = new ParetoTreeNode(null, m_randomRoller,m_treePolicy,m_rnd, this,m_playoutInfo);
        //m_heightMap = new int[m_heightMap.length][m_heightMap[0].length];
    }

    public int run(Game a_gameState, long a_timeDue, boolean a)
    {
        m_root.state = a_gameState;
        m_root.m_numIters = 0;

        m_root.mctsSearch(a_timeDue);
        int nextAction = 0;
        if(a)
        {
            if(m_targetWeights.length == 1)
                nextAction = m_root.bestActionIndexValue();
            else{
                /*System.out.print("(");
                for(int i = 0; i < m_root.children.length; ++i)
                {
                    if(!m_root.m_prunedChildren[i])
                    {
                        System.out.print(i + ":" + m_root.children[i].nVisits + ",");
                    }
                }
                System.out.println(")");        */
                double[] targetWeights = m_heuristic.getTargetWeights();
                //System.out.println(targetWeights[0] + "," + targetWeights[1] + "," + targetWeights[2]);
                nextAction = m_root.bestActionIndex(targetWeights);
            }

        }

        this.m_numCalls++;
        this.m_numIters += m_root.m_numIters;
        return nextAction;
    }

    public HeuristicMO getHeuristic(){return m_heuristic;}

    public double getHV(boolean a_normalized)
    {
        return m_root.getHV(a_normalized);
    }

    public void reset(){}
}
