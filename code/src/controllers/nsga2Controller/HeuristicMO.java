package controllers.nsga2Controller;

import controllers.ParetoMCTS.PlayoutInfo;
import framework.core.Game;

/**
 * Created by Diego Perez, University of Essex.
 * Date: 29/07/13
 */
public interface HeuristicMO
{
     public double[] value(Game a_gameState);
     public double[][] getValueBounds();
     public boolean mustBePruned(Game a_newGameState, Game a_previousGameState);
}
