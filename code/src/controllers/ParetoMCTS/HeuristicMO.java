package controllers.ParetoMCTS;

import framework.core.Game;

/**
 * Created by Diego Perez, University of Essex.
 * Date: 29/07/13
 */
public interface HeuristicMO
{
     public double[] getTargetWeights();
     public double[] value(Game a_gameState);
     public double[][] getValueBounds();
     public boolean mustBePruned(Game a_newGameState, Game a_previousGameState);
     public void setPlayoutInfo(PlayoutInfo a_pi);
     public void addPlayoutInfo(int a_lastAction, Game a_gameState);
}
