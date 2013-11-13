package controllers.singleMCTS;

import framework.core.Controller;
import framework.core.FuelTank;
import framework.core.Game;
import framework.core.Waypoint;
import framework.graph.Graph;

import java.awt.*;
import java.util.LinkedList;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 07/11/13
 * Time: 17:05
 */
public class SingleMCTSController extends Controller
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
     *   Current action in the macro action being execut
     */
    private int m_currentMacroAction;

    /**
     * Pareto MCTS player to find the optimal macro-action to execute.
     */
    public SingleMCTSPlayer m_player;

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
    public SingleMCTSController(Game a_game, long a_timeDue)
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
        m_player = new SingleMCTSPlayer(new SingleTreePolicy(SingleMCTSParameters.K), m_heuristic, m_rnd, a_game, new PlayoutPTSPInfo());
        //m_player = new SingleMCTSPlayer(new SingleEGreedyTreePolicy(), m_heuristic, m_rnd, a_game, new PlayoutPTSPInfo());

        m_currentMacroAction = 10;
        m_lastMacroAction = 0;
        m_currentGameState = null;
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
            //First cycle of a match is special, only one macro-action mcts search cycle.
            m_lastMacroAction = 0;
            nextMacroAction = m_lastMacroAction;
            m_resetRS = true;
            m_currentMacroAction = SingleMCTSParameters.MACRO_ACTION_LENGTH-1;

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
                    m_currentMacroAction = SingleMCTSParameters.MACRO_ACTION_LENGTH-1;

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
            int first = SingleMCTSParameters.MACRO_ACTION_LENGTH - m_currentMacroAction - 1;
            for(int i = first; i < SingleMCTSParameters.MACRO_ACTION_LENGTH; ++i)
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
        if(SingleMCTSParameters.EXPLORATION_VIEW_ON)
            paintHeightMap(a_gr);

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

    private void paintHeightMap(Graphics2D a_gr)
    {
        /*for(int i = 0; i < m_player.m_heightMap.length; ++i)
        {
            for(int j = 0; j < m_player.m_heightMap[0].length; ++j)
            {
                int height = m_player.m_heightMap[i][j];

                if(height > 0)
                {
                    Color col = getColorByHeight(height);
                    a_gr.setColor(col);
                    a_gr.fillRect(i,j,1,1);
                    //System.out.print(height + " ");
                }

            }
        } */
    }

    private Color getColorByHeight(int height)
    {
        if(true)
        {
            if(height < 8)
                return new Color(199,247,252);    //PALE CYAN
            else if(height < 20)
                return new Color(12,220,243);   //CYAN
            else if(height < 40)
                return new Color(11,104,244);    //BLUE
            else if(height < 50)
                return new Color(4,33,134);   //DARK BLUE
            else if(height < 60)
                return new Color(218,15,240);  //PINK
            else if(height < 80)
                return new Color(11,14,241);   //PURPLE
            else if(height < 100)
                return new Color(245,104,10);   //ORANGE

            return Color.red;                   //RED         */
        }else{
            if(height < 4)
                return Color.black;
            else if(height < 10)
                return new Color(50,50,50);
            else if(height < 20)
                return new Color(90,90,90);
            else if(height < 30)
                return new Color(140,140,140);
            else if(height < 40)
                return new Color(190,190,190);
            else if(height < 60)
                return new Color(210,210,210);
            else if(height < 80)
                return new Color(225,225,225);

            return new Color(250,250,250); //almost Color.white
        }
    }
}
