package controllers.singleMCTS;

import framework.core.Game;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 07/11/13
 * Time: 17:16
 */
public interface Heuristic
{
    public double value(Game a_gameState);
    public double[] getValueBounds();
    public boolean mustBePruned(Game a_newGameState, Game a_previousGameState);
    public void setPlayoutInfo(PlayoutInfo a_pi);
    public void addPlayoutInfo(int a_lastAction, Game a_gameState);
}
