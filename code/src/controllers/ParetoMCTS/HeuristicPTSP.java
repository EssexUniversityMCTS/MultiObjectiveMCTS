package controllers.ParetoMCTS;

import framework.core.*;
import framework.graph.Node;
import framework.graph.Path;

import java.util.*;

/**
 * Created by Diego Perez, University of Essex.
 * Date: 29/07/13
 */
public class HeuristicPTSP implements HeuristicMO
{
    /** TUNABLE PARAMETERS **/
    public double ALPHA = 0.9;
    public double BETA = 1 - ALPHA;
    public double GAMMA = 2;
    public int F_CHECKS;
    public int VALUE_CALLS;
    public double FUEL_OPTIMUM_PROPORTION = 0.0;
    public int MACRO_ACTION_LENGTH = 15;
    public int ROLLOUT_DEPTH = 8;

    /** STATE VARIABLES **/
    public double[] targetWeights;
    public HashMap<Integer, Node> m_nodeLookup;
    public int[] m_bestRoute;
    public int[] m_nextPickups;
    public double m_preRolloutDistance1;
    public double m_preRolloutDistance2;
    public double m_preRolloutFuel;
    public double m_preRolloutDamage;
    public int m_lastVisitTick;
    public int m_numTargets;
    public double maxDist = 0;

    public HeuristicPTSP(double[] tWeights, int []bestRoute, int maLength, int rollDepth)
    {
        this.targetWeights = tWeights;
        m_numTargets = targetWeights.length;
        MACRO_ACTION_LENGTH = maLength;
        ROLLOUT_DEPTH = rollDepth;
        m_nodeLookup = new HashMap<Integer, Node>();
        m_bestRoute = bestRoute;
    }

    public double[] value(Game a_gameState)
    {
        VALUE_CALLS++;
        if(m_nextPickups == null)
        {
            double superReward[] = new double[targetWeights.length];
            for(int i = 0; i < targetWeights.length; ++i)
                superReward[i] = 5;
            //System.out.println("SUPER-REWARD");
            return superReward; // Game finished successfully.
        }

        if(a_gameState.isEnded() && a_gameState.getWaypointsLeft()>0)
        {
            double superPunishment[] = new double[targetWeights.length];
            for(int i = 0; i < targetWeights.length; ++i)
                superPunishment[i] = -2;
            //System.out.println("SUPER PUNISHMENT! " + a_gameState.getTotalTime());
            return superPunishment; // Game finished - game over.
        }

        //All my waypoints
        LinkedList<Waypoint> waypoints = a_gameState.getWaypoints();

        //All my fuel tanks
        LinkedList<FuelTank> fuelTanks = a_gameState.getFuelTanks();

        double collectionBonus = 0;
        double feasibility = 1;
        double visitHistory = 1;
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
               // System.out.println("FEASIBILITY CHECK: " + pot_dist + " < " + distance1);
                F_CHECKS++;
            }
        }

        double alpha = ALPHA;
        double beta = BETA;
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

                    int whenDidItHappen = a_gameState.getShip().getVisitHistory().get(m_nextPickups[0]);
                    int timeSpent = whenDidItHappen - m_lastVisitTick;
                    //System.out.println("Seen on " + whenDidItHappen);

                    //visitHistory = 2 - (whenDidItHappen/8000); //8000=numWaypoints*timePerWaypoint
                    visitHistory = 2 - (timeSpent/PTSPConstants.getStepsInit(10));
                }
                distanceScore2 = 1 - (distance2/m_preRolloutDistance2);
            }
        }else
        {
            alpha = 10;
            beta = 0;
        }

        //Distance points:
        double distancePoints = alpha * distanceScore1 + beta * distanceScore2;

        //Fuel and damage points:
        //double fuelDifference = Math.abs((consumedFuelInterval/playoutLength) - FUEL_OPTIMUM_PROPORTION);
        //double fuelPoints = 1 - fuelDifference;   //THIS ONE SEEMS TO BE SHIT!
        double fuelPoints = 1 - (consumedFuelInterval/playoutLength);
        double damagePoints = 1 - (damageTakenInterval/playoutLength);
        double speedPoints = a_gameState.getShip().v.mag();

        int stepsPerWp = PTSPConstants.getStepsPerWaypoints(a_gameState.getNumWaypoints());
        //double timePoints = 1 - (a_gameState.getTotalTime()/10000.0);
        double timePoints = 1 - ((stepsPerWp - a_gameState.getStepsLeft()) / stepsPerWp);


        //double[] moScore = new double[]{visitHistory*distancePoints*feasibility,
        //                                visitHistory*fuelPoints*feasibility*speedPoints,
        //                                visitHistory*damagePoints*feasibility*speedPoints};
        double[] moScore = new double[]{distancePoints*timePoints,distancePoints*fuelPoints,distancePoints*damagePoints};
        //double[] moScore = new double[]{distancePoints,fuelPoints,damagePoints};

        //double[] moScore = new double[]{distancePoints*fuelPoints+collectionBonus,distancePoints*damagePoints+collectionBonus};
        //double[] moScore = new double[]{distancePoints*fuelPoints,distancePoints*damagePoints};
        //double[] moScore = new double[]{timePoints*distancePoints*fuelPoints,timePoints*distancePoints*damagePoints};

        return moScore;
    }

    public double[][] getValueBounds()
    {
        double[][] bounds = new double[m_numTargets][2];
        /*bounds[0][0] = -2;
        bounds[0][1] = 2;
        bounds[1][0] = -5;
        bounds[1][1] = 5;
        if(m_numTargets == 3)
        {
            bounds[2][0] = -5;
            bounds[2][1] = 5;
        }            */

        bounds[0][0] = 0;
        bounds[0][1] = 1;
        bounds[1][0] = 0;
        bounds[1][1] = 1;
        if(m_numTargets == 3)
        {
            bounds[2][0] = 0;
            bounds[2][1] = 1;
        }
        return bounds;
    }

    /**
     * Updates m_nextPickups, that indicates the next a_howMany objects to follow.
     * @param a_howMany number of objects to include in the search.
     */
    public void updateNextPickups(Game a_gameState, int a_howMany)
    {
        GameObject[] pickups = null;
        m_nextPickups = null;
        try{

            //All my waypoints
            LinkedList<Waypoint> waypoints = a_gameState.getWaypoints();

            //All my fuel tanks
            LinkedList<FuelTank> fuelTanks = a_gameState.getFuelTanks();

            //Number of waypoints visited.
            int nVisited = a_gameState.getWaypointsVisited();
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

            if(nVisited == a_gameState.getNumWaypoints()-1 && m_nextPickups[0]<10)
            {
                int k = m_nextPickups[0];
                GameObject go = pickups[0];

                m_nextPickups = new int[1];
                m_nextPickups[0] = k;
                pickups = new GameObject[1];
                pickups[0] = go;
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        //Initial values for all objectives.
        m_preRolloutFuel = PTSPConstants.INITIAL_FUEL-a_gameState.getShip().getRemainingFuel(); //Fuel Consumed!
        m_preRolloutDamage = a_gameState.getShip().getDamage();

        m_preRolloutDistance1 = m_preRolloutDistance2 = 1;//default value.
        if(pickups != null)
        {
            Path pathToFirst = getPathToGameObject(a_gameState, pickups[0], m_nextPickups[0]);
            m_preRolloutDistance1 = pathToFirst.m_cost;

            if(pickups.length > 1)
            {
                Path pathToSecond = getPathBetweenGameObjects(a_gameState, pickups[0], m_nextPickups[0], pickups[1], m_nextPickups[1]);
                m_preRolloutDistance2 = m_preRolloutDistance1 + pathToSecond.m_cost;
            }
        }

        m_lastVisitTick = 0;
        TreeMap<Integer, Integer> map = a_gameState.getShip().getVisitHistory();
        NavigableSet<Integer> visited = map.descendingKeySet();
        for(Integer v : visited)
        {
            int time = map.get(v);
            if(time > m_lastVisitTick)
            {
                m_lastVisitTick = time;
            }

        }
    }

    /**
     * Gets the path from the current location of the ship to the object passed as parameter.
     * @param a_game copy of the current game state.
     * @param a_gObj object ot get the path to.
     * @param a_objKey index of the object to look for.
     * @return the path from the current ship position to  a_gObj.
     */
    private Path getPathToGameObject(Game a_game, GameObject a_gObj, int a_objKey)
    {
        //The closest node to the ship's location.
        Node shipNode = ParetoMCTSController.m_graph.getClosestNodeTo(a_game.getShip().s.x, a_game.getShip().s.y);

        //Node objectNode = ParetoMCTSController.m_graph.getClosestNodeTo(a_gObj.s.x, a_gObj.s.y);
        //The closest node to the target's location (checking the cache).
        Node objectNode = null;
        if(m_nodeLookup.containsKey(a_objKey))
            objectNode = m_nodeLookup.get(a_objKey);
        else{
            objectNode = ParetoMCTSController.m_graph.getClosestNodeTo(a_gObj.s.x, a_gObj.s.y);
            m_nodeLookup.put(a_objKey, objectNode);
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
    private Path getPathBetweenGameObjects(Game a_game, GameObject a_gObj, int a_objKey, GameObject a_gObj2, int a_objKey2)
    {
        //The closest node to the first object
        Node object1Node = null;
        if(m_nodeLookup.containsKey(a_objKey))
            object1Node = m_nodeLookup.get(a_objKey);
        else{
            object1Node = ParetoMCTSController.m_graph.getClosestNodeTo(a_gObj.s.x, a_gObj.s.y);
            m_nodeLookup.put(a_objKey, object1Node);
        }

        //The closest node to the second object
        Node object2Node = null;
        if(m_nodeLookup.containsKey(a_objKey2))
            object2Node = m_nodeLookup.get(a_objKey2);
        else{
            object2Node = ParetoMCTSController.m_graph.getClosestNodeTo(a_gObj2.s.x, a_gObj2.s.y);
            m_nodeLookup.put(a_objKey2, object2Node);
        }
        //Get the parh between the nodes.
        return ParetoMCTSController.m_graph.getPath(object1Node.id(), object2Node.id());
    }

    /**
     *  Given a distance, returns a score based on its distance.
     * @param a_dist  distance
     * @return heuristic score.
     */
    public double scoreDist(double a_dist)
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
    public boolean match(ArrayList<Integer> a_followedOrder, int[] a_pathDesired)
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
