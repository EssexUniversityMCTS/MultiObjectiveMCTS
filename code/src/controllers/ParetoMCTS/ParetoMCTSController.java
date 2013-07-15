package controllers.ParetoMCTS;

import framework.core.*;
import framework.graph.Graph;
import framework.graph.Node;
import framework.graph.Path;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

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
     * Best route (order of waypoints) found to follow.
     */
    public static int[] m_bestRoute;

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
    private Random m_rnd;

    /**
     * Next two waypoints in the route to pick up.
     */
    public static int[] m_nextPickups;

    /**
     * Current game state
     */
    public static Game m_currentGameState;


    public static int SCORE_PER_WAYPOINT = 1000;
    public static int MACRO_ACTION_LENGTH = 15;
    public static int ROLLOUT_DEPTH = 8;
    public static int NUM_ACTIONS = 6;
    public static int NUM_TARGETS = 3;
    public static int K = 1;

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
        m_player = new ParetoMCTSPlayer(new ParetoTreePolicy(K), m_rnd, new double[]{0.33, 0.33, 0.33});
        m_currentMacroAction = 10;
        m_lastMacroAction = 0;
        m_currentGameState = null;

        /* //Enable this to recalculate routes:
        m_tspGraph.solve();
        for(int i =0; i < m_tspGraph.getBestPath().length; ++i)
            System.out.print(m_tspGraph.getBestPath()[i] + ",");
        System.out.println();     */

        m_bestRoute = m_tspGraph.getPreRouteArray(a_game.getMap().getFilename());
    }


    /**
     * Returns an action to execute in the game.
     * @param a_game A copy of the current game
     * @param a_timeDue The time the next move is due
     * @return
     */
    @Override
    public int getAction(Game a_game, long a_timeDue)
    {
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
            updateNextPickups(2);
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
    public void paint(Graphics2D a_gr) {}

    /**
     * Updates m_nextPickups, that indicates the next a_howMany objects to follow.
     * @param a_howMany number of objects to include in the search.
     */
    public static void updateNextPickups(int a_howMany)
    {
        m_nextPickups = null;
        try{

            //All my waypoints
            LinkedList<Waypoint> waypoints = m_currentGameState.getWaypoints();

            //All my fuel tanks
            LinkedList<FuelTank> fuelTanks = m_currentGameState.getFuelTanks();

            //Number of waypoints visited.
            int nVisited = m_currentGameState.getWaypointsVisited();
            if(nVisited != waypoints.size())
            {
                //Array with the next pickups to visit, considering the case where there are less available.
                m_nextPickups = new int[Math.min(a_howMany, waypoints.size()+fuelTanks.size() - nVisited)];
                int pLength =  m_nextPickups.length; //number of elements to pick up.

                //Go through the best path and check for what is collected.
                for(int i = 0, j = 0; j < pLength && i < m_bestRoute.length; ++i)
                {
                    int key = m_bestRoute[i];

                    if(key < 10)
                    {
                        //It's a waypoint.
                        if(!waypoints.get(key).isCollected())
                        {
                            //The first pLength elements not visited are selected.
                            m_nextPickups[j++] = key;
                        }
                    }else
                    {
                        //Fuel tank
                        if(!fuelTanks.get(key-10).isCollected())
                        {
                            //The first pLength elements not visited are selected.
                            m_nextPickups[j++] = key;
                        }
                    }


                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static double[] value(Game a_gameState)
    {
        int timeSpent = 0;
        double score = 0;
        if(m_nextPickups == null)
        {
            //All waypoints visited, reward for finished game.
            timeSpent = 10000 - a_gameState.getTotalTime();
            score = 10 * (a_gameState.getWaypointsVisited() * SCORE_PER_WAYPOINT + timeSpent);
        }else
        {
            //This is the normal case

            GameObject obj0 = null, obj1 = null;
            boolean obj0Collected = false, obj1Collected = false;
            //This is the path to the object we ned to collect
            Path pathToFirst = null;

            //Next object supposed to be collected, that might have been collected since we started the random path.
            if(m_nextPickups[0] < 10)
            {
                obj0 = a_gameState.getWaypoints().get(m_nextPickups[0]);
                obj0Collected = ((Waypoint)obj0).isCollected();
                pathToFirst = getPathToGameObject(a_gameState, obj0, m_nextPickups[0]);
            }else{
                obj0 = a_gameState.getFuelTanks().get(m_nextPickups[0]-10);
                obj0Collected = ((FuelTank)obj0).isCollected();
             pathToFirst = getPathToGameObject(a_gameState, obj0, m_nextPickups[0]-10);
            }


            //Let's give some points for the distance to it
            double distancePoints = 0;
            if(m_nextPickups.length == 1)
            {
                //If it is the last waypoint, we just give scores for it.
                distancePoints = scoreDist(pathToFirst.m_cost);
            }else
            {
                //There are more waypoints after this one. Get that one.
                if(m_nextPickups[1] < 10)
                {
                    obj1 = a_gameState.getWaypoints().get(m_nextPickups[1]);
                    obj1Collected = ((Waypoint)obj1).isCollected();
                }else{
                    obj1 = a_gameState.getFuelTanks().get(m_nextPickups[1]-10);
                    obj1Collected = ((FuelTank)obj1).isCollected();
                }

                //And give points to these distances.
                if(obj0Collected)
                {
                    double dist = a_gameState.getShip().s.dist(obj1.s);
                    distancePoints = scoreDist(dist) + SCORE_PER_WAYPOINT*10;

                }else
                    distancePoints = scoreDist(pathToFirst.m_cost);

            }

            //Reward points for collecting waypoints.
            double waypointsPoints = 0;
            if(match(a_gameState.getVisitOrder(), m_bestRoute))
            {
                if(obj0Collected)
                    waypointsPoints = SCORE_PER_WAYPOINT;

                if(obj1 != null && obj1Collected)
                    waypointsPoints = SCORE_PER_WAYPOINT * 2;
            }

             score = waypointsPoints + distancePoints;
        }

        double[] moScore = new double[]{score*(2000 - a_gameState.getTotalTime()),
                                        score*(1000 - a_gameState.getShip().getRemainingFuel()),
                                        score*(1000 - a_gameState.getShip().getDamage())};

        return moScore;
    }

    public static double[][] getValueBounds()
    {
        double[][] bounds = new double[NUM_TARGETS][2];
        bounds[0][0] = 0;
        bounds[0][1] = 2000;
        bounds[1][0] = 0;
        bounds[1][1] = 1000;
        bounds[2][0] = 0;
        bounds[2][1] = 1000;

        return bounds;
    }
        /**
     * Gets the path from the current location of the ship to the object passed as parameter.
     * @param a_game copy of the current game state.
     * @param a_gObj object ot get the path to.
     * @param a_objKey index of the object to look for.
     * @return the path from the current ship position to  a_gObj.
     */
    private static Path getPathToGameObject(Game a_game, GameObject a_gObj, int a_objKey)
    {
        //The closest node to the ship's location.
        Node shipNode = ParetoMCTSController.m_graph.getClosestNodeTo(a_game.getShip().s.x, a_game.getShip().s.y);

        Node objectNode = ParetoMCTSController.m_graph.getClosestNodeTo(a_gObj.s.x, a_gObj.s.y);
        //The closest node to the target's location (checking the cache).
        /*Node objectNode = null;
        if(ParetoMCTSController.m_nodeLookup.containsKey(a_objKey))
            objectNode = ParetoMCTSController.m_nodeLookup.get(a_objKey);
        else{

            ParetoMCTSController.m_nodeLookup.put(a_objKey, objectNode);
        }          */

        //Get the parh between the nodes.
        return ParetoMCTSController.m_graph.getPath(shipNode.id(), objectNode.id());
    }

    /**
     *  Given a distance, returns a score based on its distance.
     * @param a_dist  distance
     * @return heuristic score.
     */
    public static double scoreDist(double a_dist)
    {
        double estMaxDistance = 10000;
        double distancePoints = estMaxDistance - a_dist;
        distancePoints = Math.max(distancePoints,0);
        return distancePoints;
    }

     /**
     * Checks if the waypoint order followed so far matches the predifined route.
     * @param a_followedOrder Order followed so far.
     * @param a_pathDesired order of waypoints decided by the TSP solver.
     * @return true if the order followed matches a_pathDesired
     */
    public static boolean match(ArrayList<Integer> a_followedOrder, int[] a_pathDesired)
    {
        int idx = 0;
        for (Integer i : a_followedOrder)
        {
            if(i < 10 && i != a_pathDesired[idx])
                return false;
            idx++;
        }
        return true;
    }

}
