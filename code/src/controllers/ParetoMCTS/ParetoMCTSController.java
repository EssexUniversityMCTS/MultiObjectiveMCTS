package controllers.ParetoMCTS;

import framework.core.*;
import framework.graph.Graph;
import framework.graph.Node;
import framework.graph.Path;

import java.awt.*;
import java.util.*;

/**
 * PTSP-Competition
 * Sample controller based on macro actions and random search.
 * Created by Diego Perez, University of Essex.
 * Date: 17/10/12
 */
public class ParetoMCTSController extends Controller {

    /**
     * Graph to do pathfining.
     */
    public static Graph m_graph;

    /**
     * TSP solver.
     */
    public static TSPBranchBound m_tspGraph;


    /**
     *   Current action in the macro action being execut
     */
    private int m_currentMacroAction;

    /**
     * Pareto MCTS player to find the optimal macro-action to execute.
     */
    private ParetoMCTSPlayer m_player;

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


    public static int MULT_PER_WAYPOINT = 2;
    public static int MACRO_ACTION_LENGTH = 15;
    public static int ROLLOUT_DEPTH = 8;
    public static int NUM_ACTIONS = 6;
    public static int K = 1;

    public static double[] targetWeights= //new double[]{0.33,0.33,0.33};
                                       //  new double[]{0.8,0.1,0.1};
                                  // new double[]{0.5, 0.5};
                                  // new double[]{0.9, 0.05, 0.05};
                                 //  new double[]{1, 0, 0};
                                 // new double[]{0.0, 1.0, 0};
                                  new double[]{0.0, 0.0, 1};
                                //new double[]{0.0, 0.5, 0.5};
    public static int NUM_TARGETS = targetWeights.length;

    /**
     * Constructor of the controller
     * @param a_game Copy of the initial game state.
     * @param a_timeDue Time to reply to this call.
     */
    public ParetoMCTSController(Game a_game, long a_timeDue)
    {
        m_rnd = new Random();
        m_resetRS = true;
        m_graph = new Graph(a_game);
        m_tspGraph = new TSPBranchBound(a_game, m_graph);
        int []bestRoute = m_tspGraph.getPreRouteArray(a_game.getMap().getFilename());
        m_heuristic = new HeuristicPTSP(targetWeights, bestRoute, MACRO_ACTION_LENGTH, ROLLOUT_DEPTH);
        m_player = new ParetoMCTSPlayer(new ParetoTreePolicy(K), m_heuristic, m_rnd, targetWeights);
        //m_player = new ParetoMCTSPlayer(new SimpleHVTreePolicy(K), m_rnd, targetWeights);
        m_currentMacroAction = 10;
        m_lastMacroAction = 0;
        m_currentGameState = null;

        /* //Enable this to recalculate routes:
        m_tspGraph.solve();
        for(int i =0; i < m_tspGraph.getBestPath().length; ++i)
            System.out.print(m_tspGraph.getBestPath()[i] + ",");
        System.out.println();     */

    }


    public static boolean FLAG;
    public static int HV_COUNTS;
    /**
     * Returns an action to execute in the game.
     * @param a_game A copy of the current game
     * @param a_timeDue The time the next move is due
     * @return action to execute
     */
    @Override
    public int getAction(Game a_game, long a_timeDue)
    {
        FLAG = false;
        HV_COUNTS = 0;
        int cycle = a_game.getTotalTime();
        int nextMacroAction;

        if(cycle == 0)
        {
            //First cycle of a match is special, we need to execute any action to start looking for the next one.
            m_lastMacroAction = 0;
            nextMacroAction = m_lastMacroAction;
            m_resetRS = true;
            m_currentMacroAction = ParetoMCTSController.MACRO_ACTION_LENGTH-1;
        }else
        {
            //advance the game until the last action of the macro action
            prepareGameCopy(a_game);
            m_currentGameState = a_game;
            m_heuristic.updateNextPickups(m_currentGameState, 2);
            if(m_currentMacroAction > 0)
            {
                if(m_resetRS)
                {
                    //search needs to be restarted.
                    m_player.init();
                }
                //keep searching, but it is not time to retrieve the best action found
                m_player.run(a_game, a_timeDue);
                //we keep executing the same action decided in the past.
                nextMacroAction = m_lastMacroAction;
                m_currentMacroAction--;
                m_resetRS = false;
            }else if(m_currentMacroAction == 0)
            {
                nextMacroAction = m_lastMacroAction; //default value
                //keep searching and retrieve the action suggested by the random search engine.
                int suggestedAction = m_player.run(a_game, a_timeDue);

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
                    m_currentMacroAction = ParetoMCTSController.MACRO_ACTION_LENGTH-1;

            }else{
                throw new RuntimeException("This should not be happening: " + m_currentMacroAction);
            }
        }

        FLAG = true;
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
            int first = ParetoMCTSController.MACRO_ACTION_LENGTH - m_currentMacroAction - 1;
            for(int i = first; i < ParetoMCTSController.MACRO_ACTION_LENGTH; ++i)
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
    {
        a_gr.setColor(Color.black);
        LinkedList<Waypoint> wps = m_currentGameState.getWaypoints();
        for(int i = 0; i < wps.size(); ++i)
        {
            Waypoint wp = wps.get(i);
            a_gr.drawString(i+"",(int)wp.s.x+10, (int)wp.s.y+10);
        }

        LinkedList<FuelTank> fts = m_currentGameState.getFuelTanks();
        for(int i = 0; i < fts.size(); ++i)
        {
            FuelTank ft = fts.get(i);
            a_gr.drawString(i+"",(int)ft.s.x+10, (int)ft.s.y+10);
        }
    }
}
