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
    Game m_curState;
    Random m_rnd;
    double[] m_targetWeights;
    public ParetoArchive m_globalPA;

    //Macroactions
    private int m_currentMacroAction;                       //Current action in the macro action being executed.
    private int m_lastAction;                       //Last macro action to be executed.

    public ParetoMCTSPlayer(TreePolicy a_treePolicy, Random a_rnd, double[] a_targetWeights)
    {
        m_treePolicy = a_treePolicy;
        this.m_rnd = a_rnd;
        this.m_targetWeights = a_targetWeights;
        m_globalPA = new ParetoArchive();
        m_currentMacroAction = 0;
        m_lastAction = 0;
    }

    public void init()
    {

    }

    public int run(Game a_gameState, long a_timeDue)
    {
        m_curState = a_gameState;
        Roller randomRoller = new RandomRoller(RandomRoller.RANDOM_ROLLOUT, this.m_rnd);
        m_root = new ParetoTreeNode(m_curState,randomRoller,m_treePolicy);

        m_root.mctsSearch(a_timeDue);
        m_currentMacroAction = ParetoMCTSController.MACRO_ACTION_LENGTH - 1;
        m_lastAction = m_root.bestActionIndex(m_targetWeights);

        for(int i = 0; i < m_root.pa.m_members.size(); ++i)
        {
            m_globalPA.add(m_root.pa.m_members.get(i));
        }


        return m_lastAction; //(Integer) ((DstState)(a_gameState)).getBoard().getMoves().get(m_lastAction);
    }
    
    public double getHV(boolean a_normalized)
    {
        return m_root.getHV(a_normalized);
    }

    public void reset(){}
}
