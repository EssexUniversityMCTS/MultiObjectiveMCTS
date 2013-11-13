package controllers.singleMCTS;

import framework.core.Game;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 07/11/13
 * Time: 17:13
 */
public class SingleMCTSPlayer implements Player
{
    TreePolicy m_treePolicy;
    public SingleTreeNode m_root;
    Random m_rnd;
    Roller m_randomRoller;
    Heuristic m_heuristic;
    PlayoutInfo m_playoutInfo;

    /**
     * Debug height map
     */
    //public int[][] m_heightMap;
    public int m_numCalls;
    public int m_numIters;


    public int bestChildFoundIndex = -1;
    public double bestValueFound = -1;

    public SingleMCTSPlayer(TreePolicy a_treePolicy, Heuristic a_h, Random a_rnd, Game a_game, PlayoutInfo pInfo)
    {
        m_playoutInfo = pInfo;
        //m_heightMap = new int[a_game.getMap().getMapWidth()][a_game.getMap().getMapHeight()];
        m_heuristic = a_h;
        m_heuristic.setPlayoutInfo(m_playoutInfo);
        m_treePolicy = a_treePolicy;
        this.m_rnd = a_rnd;
        m_randomRoller = new RandomRoller(RandomRoller.RANDOM_ROLLOUT, this.m_rnd, SingleMCTSParameters.NUM_ACTIONS);
        m_root = new SingleTreeNode(null, m_randomRoller,m_treePolicy, m_rnd, this,m_playoutInfo);
        this.m_numCalls = 0;
        this.m_numIters = 0;
    }

    public void init()
    {
        m_root = new SingleTreeNode(null, m_randomRoller,m_treePolicy,m_rnd, this,m_playoutInfo);
        //m_heightMap = new int[m_heightMap.length][m_heightMap[0].length];
    }

    public int run(Game a_gameState, long a_timeDue, boolean a)
    {
        m_root.state = a_gameState;
        m_root.m_numIters = 0;
        bestChildFoundIndex = -1;
        bestValueFound = -1;

        m_root.mctsSearch(a_timeDue);
        int nextAction = 0;
        if(a)
        {
            //nextAction = m_root.bestActionIndexValue();
            //nextAction = m_root.bestActionIndex();
            nextAction = m_root.maxValueActionIndex();

            /*    System.out.print("(");
                for(int i = 0; i < m_root.children.length; ++i)
                {
                    if(!m_root.m_prunedChildren[i])
                    {
                        System.out.print(i + ":" + m_root.children[i].nVisits + ",");
                    }
                }
                System.out.println(")"); */

        }

        this.m_numCalls++;
        this.m_numIters += m_root.m_numIters;
        return nextAction;
    }

    public Heuristic getHeuristic(){return m_heuristic;}

    public void reset(){}
}
