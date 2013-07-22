package controllers.ParetoMCTS;

import framework.core.*;
import framework.graph.Graph;
import framework.graph.Node;
import framework.graph.Path;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
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

    /**
     * Cache  for speeding up looks for nodes in the graph.
     */
    public static HashMap<Integer, Node> m_nodeLookup;

    public static double m_preRolloutDistance1;
    public static double m_preRolloutDistance2;
    public static double m_preRolloutFuel;
    public static double m_preRolloutDamage;

    public static int MULT_PER_WAYPOINT = 2;
    public static int MACRO_ACTION_LENGTH = 15;
    public static int ROLLOUT_DEPTH = 8;
    public static int NUM_ACTIONS = 6;
    public static int K = 1;

    public static double[] targetWeights= new double[]{0,0,1};
            //new double[]{0.33,0.33,0.33};
                                  // new double[]{0.5, 0.5};
                                  // new double[]{0.9, 0.05, 0.05};
                                  // new double[]{1, 0, 0});
                                  // new double[]{0.0, 1.0, 0};
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
        m_nodeLookup = new HashMap<Integer, Node>();
        m_tspGraph = new TSPBranchBound(a_game, m_graph);
        m_player = new ParetoMCTSPlayer(new ParetoTreePolicy(K), m_rnd, targetWeights);
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
    public void paint(Graphics2D a_gr) {}

    /**
     * Updates m_nextPickups, that indicates the next a_howMany objects to follow.
     * @param a_howMany number of objects to include in the search.
     */
    public static void updateNextPickups(int a_howMany)
    {
        GameObject[] pickups = null;
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
                pickups = new GameObject[pLength];

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
                            m_nextPickups[j] = key;
                            pickups[j++] = waypoints.get(key);
                        }
                    }else
                    {
                        //Fuel tank
                        if(!fuelTanks.get(key-10).isCollected())
                        {
                            //The first pLength elements not visited are selected.
                            m_nextPickups[j] = key;
                            pickups[j++] = fuelTanks.get(key-10);
                        }
                    }


                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        //Initial values for all objectives.
        m_preRolloutFuel = PTSPConstants.INITIAL_FUEL-m_currentGameState.getShip().getRemainingFuel(); //Fuel Consumed!
        m_preRolloutDamage = m_currentGameState.getShip().getDamage();

        m_preRolloutDistance1 = m_preRolloutDistance2 = 1;//default value.
        if(pickups != null)
        {
            Path pathToFirst = getPathToGameObject(m_currentGameState, pickups[0], m_nextPickups[0]);
            m_preRolloutDistance1 = pathToFirst.m_cost;

            if(pickups.length > 1)
            {
                Path pathToSecond = getPathBetweenGameObjects(m_currentGameState, pickups[0], m_nextPickups[0], pickups[1], m_nextPickups[1]);
                m_preRolloutDistance2 = m_preRolloutDistance1 + pathToSecond.m_cost;
            }
        }

    }

    public static double ALPHA = 0.9;
    public static double BETA = 1 - ALPHA;
    public static double GAMMA = 2;

    public static double[] value(Game a_gameState)
    {
        if(m_nextPickups == null)
        {
            double superReward[] = new double[targetWeights.length];
            for(int i = 0; i < targetWeights.length; ++i)
                superReward[i] = 2;
            return superReward; // Game finished successfully.
        }

        if(a_gameState.isEnded() && a_gameState.getWaypointsLeft()>0)
        {
            double superPunishment[] = new double[targetWeights.length];
            for(int i = 0; i < targetWeights.length; ++i)
                superPunishment[i] = -2;
            return superPunishment; // Game finished - game over.
        }

        //All my waypoints
        LinkedList<Waypoint> waypoints = a_gameState.getWaypoints();

        //All my fuel tanks
        LinkedList<FuelTank> fuelTanks = a_gameState.getFuelTanks();

        double collectionBonus = 0;
        double feasibility = 1;
        int playoutLength = MACRO_ACTION_LENGTH * ROLLOUT_DEPTH;
        double consumedFuelInterval = (PTSPConstants.INITIAL_FUEL-a_gameState.getShip().getRemainingFuel()) - m_preRolloutFuel ;
        double damageTakenInterval = a_gameState.getShip().getDamage() - m_preRolloutDamage;

        //First object. Is it collected?
        double distanceScore1 = GAMMA; //1*GAMMA;
        double distance1 = 0;
        boolean is1Collected = (m_nextPickups[0] < 10) ? (waypoints.get(m_nextPickups[0])).isCollected() : (fuelTanks.get(m_nextPickups[0]-10)).isCollected();
        GameObject firstObj = (m_nextPickups[0] < 10) ? waypoints.get(m_nextPickups[0]) : fuelTanks.get(m_nextPickups[0]-10);
        if(!is1Collected)
        {
            Path pathToFirst = getPathToGameObject(a_gameState, firstObj, m_nextPickups[0]);
            distance1 = pathToFirst.m_cost;
            distanceScore1 = 1 - (distance1/m_preRolloutDistance1);

            //Feasibility check:
            double v = (m_preRolloutDistance1 - distance1) / playoutLength;
            double pot_dist = v * a_gameState.getStepsLeft();
            if(distanceScore1>0 && pot_dist < distance1)
            {
                feasibility = -1;
            }
        }

        double distanceScore2 = GAMMA;
        double distance2 = 0;
        if(m_nextPickups.length>1)
        {
            boolean is2Collected = (m_nextPickups[1] < 10) ? (waypoints.get(m_nextPickups[1])).isCollected() : (fuelTanks.get(m_nextPickups[1]-10)).isCollected();
            GameObject secondObject = (m_nextPickups[1] < 10) ? waypoints.get(m_nextPickups[1]) : fuelTanks.get(m_nextPickups[1]-10);
            if(!is2Collected)
            {
                if(!is1Collected)
                {
                    distance2 = distance1; //Plus distance between objects.
                    Path pathToSecond = getPathBetweenGameObjects(a_gameState, firstObj, m_nextPickups[0], secondObject, m_nextPickups[1]);
                    distance2 += pathToSecond.m_cost;

                }else
                {
                    Path pathToSecond = getPathToGameObject(a_gameState, secondObject, m_nextPickups[1]);
                    distance2 = pathToSecond.m_cost;
                    collectionBonus = 10;
                }
                distanceScore2 = 1 - (distance2/m_preRolloutDistance2);
            }
        }

        //Distance points:
        double distancePoints = ALPHA * distanceScore1 + BETA * distanceScore2;

        //Fuel and damage points:
        double fuelPoints = 1 - (consumedFuelInterval/playoutLength);
        double damagePoints = 1 - (damageTakenInterval/playoutLength);

        int stepsPerWp = PTSPConstants.getStepsPerWaypoints(a_gameState.getNumWaypoints());
        //double timePoints = 1 - (a_gameState.getTotalTime()/10000.0);
        double timePoints = 1 - ((stepsPerWp - a_gameState.getStepsLeft()) / stepsPerWp);

        double[] moScore = new double[]{distancePoints*feasibility,
                                        fuelPoints*feasibility,
                                        damagePoints*feasibility};
        //double[] moScore = new double[]{distancePoints*timePoints,distancePoints*fuelPoints,distancePoints*damagePoints};

        //double[] moScore = new double[]{distancePoints*fuelPoints+collectionBonus,distancePoints*damagePoints+collectionBonus};
        //double[] moScore = new double[]{distancePoints*fuelPoints,distancePoints*damagePoints};
        //double[] moScore = new double[]{timePoints*distancePoints*fuelPoints,timePoints*distancePoints*damagePoints};

        return moScore;
    }


    public static double[][] getValueBounds()
    {
        double[][] bounds = new double[NUM_TARGETS][2];
        bounds[0][0] = 0;
        bounds[0][1] = 1;
        bounds[1][0] = 0;
        bounds[1][1] = 1;
        if(NUM_TARGETS == 3)
        {
            bounds[2][0] = 0;
            bounds[2][1] = 1;
        }
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

        //Node objectNode = ParetoMCTSController.m_graph.getClosestNodeTo(a_gObj.s.x, a_gObj.s.y);
        //The closest node to the target's location (checking the cache).
        Node objectNode = null;
        if(ParetoMCTSController.m_nodeLookup.containsKey(a_objKey))
            objectNode = ParetoMCTSController.m_nodeLookup.get(a_objKey);
        else{
            objectNode = ParetoMCTSController.m_graph.getClosestNodeTo(a_gObj.s.x, a_gObj.s.y);
            ParetoMCTSController.m_nodeLookup.put(a_objKey, objectNode);
        }

        //Get the parh between the nodes.
        return ParetoMCTSController.m_graph.getPath(shipNode.id(), objectNode.id());
    }

    /**
     * Gets the path from the current location of the ship to the object passed as parameter.
     * @param a_game copy of the current game state.
     * @param a_gObj object ot get the path to.
     * @param a_objKey index of the object to look for.
     * @return the path from the current ship position to  a_gObj.
     */
    private static Path getPathBetweenGameObjects(Game a_game, GameObject a_gObj, int a_objKey, GameObject a_gObj2, int a_objKey2)
    {
        //The closest node to the first object
        Node object1Node = null;
        if(ParetoMCTSController.m_nodeLookup.containsKey(a_objKey))
            object1Node = ParetoMCTSController.m_nodeLookup.get(a_objKey);
        else{
            object1Node = ParetoMCTSController.m_graph.getClosestNodeTo(a_gObj.s.x, a_gObj.s.y);
            ParetoMCTSController.m_nodeLookup.put(a_objKey, object1Node);
        }

        //The closest node to the second object
        Node object2Node = null;
        if(ParetoMCTSController.m_nodeLookup.containsKey(a_objKey2))
            object2Node = ParetoMCTSController.m_nodeLookup.get(a_objKey2);
        else{
            object2Node = ParetoMCTSController.m_graph.getClosestNodeTo(a_gObj2.s.x, a_gObj2.s.y);
            ParetoMCTSController.m_nodeLookup.put(a_objKey2, object2Node);
        }
        //Get the parh between the nodes.
        return ParetoMCTSController.m_graph.getPath(object1Node.id(), object2Node.id());
    }


    public static double maxDist = 0;

    /**
     *  Given a distance, returns a score based on its distance.
     * @param a_dist  distance
     * @return heuristic score.
     */
    public static double scoreDist(double a_dist)
    {
        if(a_dist > maxDist)
        {
            maxDist = a_dist;
            //System.out.println(maxDist);
        }

        double estMaxDistance = 1000;
        double distancePoints = estMaxDistance - a_dist;
        distancePoints = Math.max(distancePoints,0) / 1000.0;

        //System.out.println(a_dist + " -> " + distancePoints);
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
