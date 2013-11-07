package controllers.singleMCTS;

import framework.core.Game;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 07/11/13
 * Time: 17:12
 */
public class PlayoutPTSPInfo implements PlayoutInfo
{
    public int m_thurstCount;
    public int[] m_playoutHistory;
    public int m_numMoves;
    public int m_visitedWaypoints;
    public int m_actionFirstPickup;

    public PlayoutPTSPInfo()
    {
        m_thurstCount = 0;
        m_playoutHistory = new int[HeuristicPTSP.ROLLOUT_DEPTH];
        m_numMoves = 0;
        m_visitedWaypoints = 0;
        m_actionFirstPickup = -1;
    }

    public void reset(Game a_gameState)
    {
        m_thurstCount = 0;
        m_playoutHistory = new int[HeuristicPTSP.ROLLOUT_DEPTH];
        m_numMoves = 0;
        m_visitedWaypoints = a_gameState.getWaypointsVisited();
        m_actionFirstPickup = -1;
    }
}
