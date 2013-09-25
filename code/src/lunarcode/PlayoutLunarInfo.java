package lunarcode;

import controllers.ParetoMCTS.HeuristicPTSP;
import controllers.ParetoMCTS.ParetoMCTSParameters;
import controllers.ParetoMCTS.PlayoutInfo;
import framework.core.Game;

/**
 * Created by Samuel Roberts, 2013
 */
public class PlayoutLunarInfo implements PlayoutInfo {

    public int m_thurstCount;
    public int[] m_playoutHistory;
    public int m_numMoves;
    public boolean m_landed;

    public PlayoutLunarInfo()
    {
        m_thurstCount = 0;
        m_playoutHistory = new int[ParetoMCTSParameters.ROLLOUT_DEPTH];
        m_numMoves = 0;
        m_landed = false;
    }

    public void reset(Game a_gameState)
    {
        m_thurstCount = 0;
        m_playoutHistory = new int[ParetoMCTSParameters.ROLLOUT_DEPTH];
        m_numMoves = 0;
        m_landed = false;
    }
}
