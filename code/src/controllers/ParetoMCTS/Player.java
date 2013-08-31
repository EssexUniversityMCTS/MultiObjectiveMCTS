package controllers.ParetoMCTS;

import framework.core.Game;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 12/02/13
 * Time: 12:53
 * To change this template use File | Settings | File Templates.
 */
public interface Player {
    void init();
    int run(Game a_gameState, long a_timeDue, boolean a);
    double getHV(boolean a_normalized);
    void reset();
    HeuristicMO getHeuristic();
}
