package controllers.nsga2Controller;

import framework.core.Controller;
import framework.core.Game;
import framework.graph.Graph;

import java.awt.*;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 14/11/13
 * Time: 13:31
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class NSGAIIController extends Controller
{
    /**
     * Graph to do pathfining.
     */
    public static Graph m_graph;

    /**
     * TSP solver.
     */
    public static TSPBranchBound m_tspGraph;

    /**
     * Pareto MCTS player to find the optimal macro-action to execute.
     */
    public NSGAIIPlayer m_player;

    /**
     *   Current action in the macro action being execut
     */
    private int m_currentMacroAction;

    /**
     * Flag that indicates if the RS engine must be restarted (a new action has been decided).
     */
    boolean m_resetRS;

    /**
     *  Last macro action to be executed.
     */
    private int m_lastMacroAction;

    /**
     * Random number generator.
     */
    public Random m_rnd;

    /**
     * Heuristic to score mid-game states.
     */
    public HeuristicPTSP m_heuristic;

    /**
     * Current game state
     */
    public static Game m_currentGameState;


    public static double FUEL_POWER_MULT = 0.66;
    public static double DAMAGE_POWER_MULT = 0.75;
    public static double DAMAGE_POWER_MULT_SLOW = DAMAGE_POWER_MULT / 3.0;
    public static double THRESHOLD_HIGH_SPEED = 0.8;

    /**
     * Constructor of the controller
     * @param a_game Copy of the initial game state.
     * @param a_timeDue Time to reply to this call.
     */
    public NSGAIIController(Game a_game, long a_timeDue)
    {
        //long seed = 1375315004579L;//System.currentTimeMillis();
        m_rnd = new Random();
        //System.out.println("Seed: " + seed);
        m_resetRS = true;
        m_graph = new Graph(a_game);
        m_tspGraph = new TSPBranchBound(a_game, m_graph);
        int []bestRoute = m_tspGraph.getPreRouteArray(a_game.getMap().getFilename());

        m_tspGraph.getCost(bestRoute, m_graph, a_game);

        m_heuristic = new HeuristicPTSP(a_game, bestRoute);
        m_player = new NSGAIIPlayer(m_heuristic, m_rnd, 500 ,"NSGAII"); //400 ~ 40ms

        m_currentMacroAction = 10;
        m_lastMacroAction = 0;
        m_currentGameState = null;
    }

    /**
     * Returns an action to execute in the game.
     * @param a_game A copy of the current game
     * @param a_timeDue The time the next move is due
     * @return action to execute
     */
    @Override
    public int getAction(Game a_game, long a_timeDue)
    {
        int cycle = a_game.getTotalTime();
        int nextMacroAction;

        if(cycle == 0)
        {
            //First cycle of a match is special, only one macro-action mcts search cycle.
            m_lastMacroAction = 0;
            nextMacroAction = m_lastMacroAction;
            m_resetRS = true;
            m_currentMacroAction = NSGAIIParameters.MACRO_ACTION_LENGTH-1;

            m_currentGameState = a_game; //no need to prepareGameCopy
            m_heuristic.updateNextPickups(m_currentGameState, 3);
            int suggestedAction = m_player.run(a_game, a_timeDue, true);

            m_resetRS = true;
            if(suggestedAction != -1)
                m_lastMacroAction = suggestedAction;

        }else
        {
            //advance the game until the last action of the macro action
            prepareGameCopy(a_game);
            m_currentGameState = a_game;
            m_heuristic.updateNextPickups(m_currentGameState, 3);

            if(m_currentMacroAction > 0)
            {
                if(m_resetRS)
                {
                    //search needs to be restarted.
                    m_player.init();
                }
                //keep searching, but it is not time to retrieve the best action found
                m_player.run(a_game, a_timeDue, false);
                //we keep executing the same action decided in the past.
                nextMacroAction = m_lastMacroAction;
                m_currentMacroAction--;
                m_resetRS = false;
            }else if(m_currentMacroAction == 0)
            {
                nextMacroAction = m_lastMacroAction; //default value
                //keep searching and retrieve the action suggested by the random search engine.
                int suggestedAction = m_player.run(a_game, a_timeDue, true);
                //System.out.println();

                //System.out.println("FEASIBILITY WARNINGS: " + ((double)F_CHECKS/(double)VALUE_CALLS));
                m_heuristic.F_CHECKS = 0;
                m_heuristic.VALUE_CALLS = 0;

                //System.out.println("PA Size: " + m_player.m_root.pa.m_members.size());
                //System.out.println("HV_COUNTS: " + HV_COUNTS);
                //m_player.m_root.printStats();
                //System.out.println("Suggested: " + suggestedAction);

                //now it's time to execute this action. Also, in next cycle, we need to reset the search
                m_resetRS = true;
                if(suggestedAction != -1)
                    m_lastMacroAction = suggestedAction;

                if(m_lastMacroAction != -1)
                    m_currentMacroAction = NSGAIIParameters.MACRO_ACTION_LENGTH-1;

            }else{
                throw new RuntimeException("This should not be happening: " + m_currentMacroAction);
            }
        }

        return nextMacroAction;
    }

    /**
     * Updates the game state using the macro-action that is being executed. It rolls the game up to the point in the
     * future where the current macro-action is finished.
     * @param a_game  State of the game.
     */
    public void prepareGameCopy(Game a_game)
    {
        //If there is a macro action being executed now.
        if(m_lastMacroAction != -1)
        {
            //Find out how long have we executed this macro-action
            int first = NSGAIIParameters.MACRO_ACTION_LENGTH - m_currentMacroAction - 1;
            for(int i = first; i < NSGAIIParameters.MACRO_ACTION_LENGTH; ++i)
            {
                //make the moves to advance the game state.
                a_game.tick(m_lastMacroAction);
            }
        }
    }

    /**
     * We are boring and we don't paint anything here.
     * @param a_gr Graphics device to paint.
     */
    public void paint(Graphics2D a_gr)
    {}



}
