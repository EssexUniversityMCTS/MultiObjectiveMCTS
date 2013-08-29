package controllers.ParetoMCTS;

import framework.core.Game;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 02/08/13
 * Time: 13:06
 * To change this template use File | Settings | File Templates.
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
