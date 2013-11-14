package controllers.nsga2Controller;

import framework.core.*;
import framework.graph.Node;
import framework.graph.Path;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.NavigableSet;
import java.util.TreeMap;

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
    public static int MACRO_ACTION_LENGTH = 15;
    public static int ROLLOUT_DEPTH = 8;

    /** STATE VARIABLES **/
    public double[] targetWeights;
    public TreeMap<Integer, Node> m_nodeLookup;
    public int[] m_bestRoute;
    public int[] m_nextPickups;
    public double m_preRolloutDistance1;
    public double m_preRolloutDistance2;
    public double m_preRolloutFuel;
    public double m_preRolloutDamage;
    public int m_preTotalTime;
    public int m_lastVisitTick;
    public int m_numTargets;
    public double maxDist = 0;

    public double[][] m_bounds;
    public double[] segmentsCost;
    public double distanceToFinal1, distanceToFinal2, distanceToFinal3;
    public double m_currentShipSpeed;
    public int nlastCyclesThrusting;

    public HeuristicPTSP(Game a_game, int []bestRoute)
    {
        this.targetWeights = NSGAIIParameters.targetWeights;
        m_numTargets = targetWeights.length;
        MACRO_ACTION_LENGTH = NSGAIIParameters.MACRO_ACTION_LENGTH;
        ROLLOUT_DEPTH = NSGAIIParameters.ROLLOUT_DEPTH;
        m_nodeLookup = new TreeMap<Integer, Node>();
        m_bestRoute = bestRoute;

        initBounds();

        segmentsCost = new double[m_bestRoute.length+1];

        segmentsCost[m_bestRoute.length] = 0;
        for(int i = m_bestRoute.length-2, j = segmentsCost.length-2; i >= 0; --i, --j)
        {
            int from = m_bestRoute[i];
            int to =  m_bestRoute[i+1];

            GameObject fromObj = from<10? a_game.getWaypoints().get(from) : a_game.getFuelTanks().get(from-10);
            GameObject toObj = to<10? a_game.getWaypoints().get(to) : a_game.getFuelTanks().get(to-10);

            Path p = getPathBetweenGameObjects(fromObj, from, toObj, to);
            segmentsCost[j] = p.m_cost + segmentsCost[j+1];
        }

        GameObject firstObj = m_bestRoute[0]<10? a_game.getWaypoints().get(m_bestRoute[0]) : a_game.getFuelTanks().get(m_bestRoute[0]-10);

        Path p = getPathToGameObject(a_game,firstObj,m_bestRoute[0]);

        segmentsCost[0] = p.m_cost + segmentsCost[1];

    }

    private void initBounds()
    {
        m_bounds = new double[m_numTargets][2];
        m_bounds[0][0] = 0;
        m_bounds[0][1] = 1;
        if(m_numTargets >= 2)
        {
            m_bounds[1][0] = 0;
            m_bounds[1][1] = 1;
            if(m_numTargets == 3)
            {
                m_bounds[2][0] = 0;
                m_bounds[2][1] = 1;
            }
        }
    }

    public double[] value(Game a_gameState)
    {
        VALUE_CALLS++;

        int playoutLength = MACRO_ACTION_LENGTH * ROLLOUT_DEPTH;
        if(m_nextPickups == null)
        {
            //In this case, the game as ended for sure: IT ENDS DURING THE
            //MACRO-ACTION BEING EXECUTED NOW.
            double superReward[] = new double[targetWeights.length];
            for(int i = 0; i < targetWeights.length; ++i)
                superReward[i]=5;  //just whatever.

            return superReward;
        }

        boolean matching = match(a_gameState.getVisitOrder(),m_bestRoute);

        if((!matching) || (a_gameState.isEnded() && a_gameState.getWaypointsLeft()>0))
        {
            double superPunishment[] = new double[targetWeights.length];
            for(int i = 0; i < targetWeights.length; ++i)
                superPunishment[i] = -2;
            //System.out.println("SUPER PUNISHMENT! "+matching+" "+a_gameState.getTotalTime());
            return superPunishment; // Game finished - game over.
        }

        //All my waypoints
        LinkedList<Waypoint> waypoints = a_gameState.getWaypoints();

        //All my fuel tanks
        LinkedList<FuelTank> fuelTanks = a_gameState.getFuelTanks();

        double consumedFuelInterval = (PTSPConstants.INITIAL_FUEL-a_gameState.getShip().getRemainingFuel()) - m_preRolloutFuel ;
        double damageTakenInterval = a_gameState.getShip().getDamage() - m_preRolloutDamage;

        //First object. Is it collected?
        double distanceToEnd = -1;
        double distanceScore1 = GAMMA; //1*GAMMA;
        double distance1 = 0;
        boolean is1Collected = (m_nextPickups[0] < 10) ? (waypoints.get(m_nextPickups[0])).isCollected() : (fuelTanks.get(m_nextPickups[0]-10)).isCollected();
        GameObject firstObj = (m_nextPickups[0] < 10) ? waypoints.get(m_nextPickups[0]) : fuelTanks.get(m_nextPickups[0]-10);
        if(!is1Collected)
        {
            Path pathToFirst = getPathToGameObject(a_gameState, firstObj, m_nextPickups[0]);
            distance1 = pathToFirst.m_cost;
            distanceToEnd = distance1+distanceToFinal1;
            distanceScore1 = 1 - (distance1/m_preRolloutDistance1);

        }

        double alpha = ALPHA;
        double beta = BETA;
        double distanceScore2 = GAMMA;
        double distance2 = 0;
        double distance3 = 0;
        boolean is2Collected = false;
        if(m_nextPickups.length>1)
        {
            is2Collected = (m_nextPickups[1] < 10) ? (waypoints.get(m_nextPickups[1])).isCollected() : (fuelTanks.get(m_nextPickups[1]-10)).isCollected();
            GameObject secondObject = (m_nextPickups[1] < 10) ? waypoints.get(m_nextPickups[1]) : fuelTanks.get(m_nextPickups[1]-10);
            if(!is2Collected)
            {
                if(!is1Collected)
                {
                    distance2 = distance1; //Plus distance between objects.
                    Path pathToSecond = getPathBetweenGameObjects(firstObj, m_nextPickups[0], secondObject, m_nextPickups[1]);
                    distance2 += pathToSecond.m_cost;

                }else
                {
                    Path pathToSecond = getPathToGameObject(a_gameState, secondObject, m_nextPickups[1]);
                    distance2 = pathToSecond.m_cost;
                    distanceToEnd = distance2+distanceToFinal2;
                }
                distanceScore2 = 1 - (distance2/m_preRolloutDistance2);
            }
            else
            {
                GameObject thirdObject = (m_nextPickups[2] < 10) ? waypoints.get(m_nextPickups[2]) : fuelTanks.get(m_nextPickups[2]-10);
                Path pathToThird = getPathToGameObject(a_gameState, thirdObject, m_nextPickups[2]);
                distance3 = pathToThird.m_cost;
                distanceToEnd = distance3+distanceToFinal3;
            }
        }else
        {
            alpha = 10;
            beta = 0;
        }


        //Distance points:
        //double distancePoints = alpha * distanceScore1 + beta * distanceScore2;
        double distancePoints = 1 - (distanceToEnd*2 / segmentsCost[0]);
        //double distancePoints = 1 - (distanceToEnd / segmentsCost[0]);
        if(a_gameState.getWaypointsLeft() == 0)
        {
            //Give more or less reward depending on depth in the tree.
            int ticksMoved = a_gameState.getTotalTime() - m_preTotalTime;
            double ticksScore = 2 - (ticksMoved/(double)playoutLength);
            distancePoints *= ticksScore;
        }

        //distancePoints += 1; //To set it in the range (0,1)
        distancePoints += 2; //To set it in the range (and avoid <0 HVs)


        //double distancePoints = 1 - (distanceToEnd / segmentsCost[0]);
        double speedPoints = a_gameState.getShip().v.mag();

        //THIS WORKED OK
        double fuelPoints = 1 - ((PTSPConstants.INITIAL_FUEL-a_gameState.getShip().getRemainingFuel()) / (double) PTSPConstants.INITIAL_FUEL);
        double fuelPower = fuelPoints*NSGAIIController.FUEL_POWER_MULT + distancePoints*(1.0-NSGAIIController.FUEL_POWER_MULT);

        double damagePoints =  1 - (a_gameState.getShip().getDamage() / (double) PTSPConstants.MAX_DAMAGE);
        //double damagePoints = 1 - (damageTakenInterval/playoutLength);

        double damagePower = 0.0;
        if(m_currentShipSpeed > NSGAIIController.THRESHOLD_HIGH_SPEED)
            damagePower = damagePoints*NSGAIIController.DAMAGE_POWER_MULT +
                    distancePoints*(1.0-NSGAIIController.DAMAGE_POWER_MULT);
        else
            damagePower = damagePoints*NSGAIIController.DAMAGE_POWER_MULT_SLOW +
                    distancePoints*(1.0-NSGAIIController.DAMAGE_POWER_MULT_SLOW);

        double[] tw = NSGAIIParameters.targetWeights;
        double allInOne = //distancePoints*0.33 + fuelPower*0.33 + damagePower*0.33;
                        distancePoints*0.1 + fuelPower*0.3 + damagePower*0.6;
                    //distancePoints*tw[0] + fuelPower*tw[1] + damagePower*tw[2];

        //double[] moScore = new double[]{distancePoints, damagePower};
        //double[] moScore = new double[]{allInOne, allInOne, allInOne};

        //double[] moScore = new double[]{damagePower, damagePower};

        double[] moScore = new double[]{distancePoints, fuelPower, damagePower};
        //double[] moScore = new double[]{allInOne};

        return moScore;
    }

    public double[][] getValueBounds()
    {
        return m_bounds;
    }

    /**
     * Updates m_nextPickups, that indicates the next a_howMany objects to follow.
     * @param a_howMany number of objects to include in the search.
     */
    public void updateNextPickups(Game a_gameState, int a_howMany)
    {
        GameObject[] pickups = null;
        m_nextPickups = null;

        nlastCyclesThrusting = 0;
        distanceToFinal1 = -1;
        distanceToFinal2 = -1;
        distanceToFinal3 = -1;
        int indexInRoute[] = null;

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
                indexInRoute = new int[pLength];

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
                            indexInRoute[j] = i+1;
                            pickups[j++] = waypoints.get(key);
                        }
                    }else
                    {
                        //Fuel tank
                        if(!fuelTanks.get(key-10).isCollected())
                        {
                            //The first pLength elements not visited are selected.
                            m_nextPickups[j] = key;
                            indexInRoute[j] = i+1;
                            pickups[j++] = fuelTanks.get(key-10);
                        }
                    }


                }

                distanceToFinal1 = segmentsCost[indexInRoute[0]];
                if(indexInRoute[1]<segmentsCost.length)
                    distanceToFinal2 = segmentsCost[indexInRoute[1]];
                if(indexInRoute[2]<segmentsCost.length)
                    distanceToFinal3 = segmentsCost[indexInRoute[2]];

            }


            if(nVisited == a_gameState.getNumWaypoints()-1 && m_nextPickups[0]<10)
            {
                int k = m_nextPickups[0];
                GameObject go = pickups[0];

                m_nextPickups = new int[1];
                m_nextPickups[0] = k;
                pickups = new GameObject[1];
                pickups[0] = go;
                distanceToFinal2 = -1;
                distanceToFinal3 = -1;
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        ArrayList pastActions = a_gameState.getShip().getActionList();
        int nActions = pastActions.size();
        int which = nActions-1;
        boolean stop = false;
        while(which >= 0 && !stop)
        {
            int action = (Integer) pastActions.get(which);
            if(Controller.getThrust(action))
                nlastCyclesThrusting++;
            else
                stop = true;
            which--;
        }

        nlastCyclesThrusting /= MACRO_ACTION_LENGTH;

        //Initial values for all objectives.
        m_preTotalTime = a_gameState.getTotalTime();
        m_preRolloutFuel = PTSPConstants.INITIAL_FUEL-a_gameState.getShip().getRemainingFuel(); //Fuel Consumed!
        m_preRolloutDamage = a_gameState.getShip().getDamage();

        m_preRolloutDistance1 = m_preRolloutDistance2 = 1;//default value.
        if(pickups != null)
        {
            Path pathToFirst = getPathToGameObject(a_gameState, pickups[0], m_nextPickups[0]);
            m_preRolloutDistance1 = pathToFirst.m_cost;

            if(pickups.length > 1)
            {
                Path pathToSecond = getPathBetweenGameObjects( pickups[0], m_nextPickups[0], pickups[1], m_nextPickups[1]);
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

        m_currentShipSpeed = a_gameState.getShip().v.mag();
        //System.out.println( (m_currentShipSpeed>NSGAIIController.THRESHOLD_HIGH_SPEED)?
        //        "High speed "+m_currentShipSpeed : "Low Speed "+m_currentShipSpeed );

        //System.out.println(indexInRoute[0] + " " + indexInRoute[1] + " " + indexInRoute[2]);
    }


    public boolean mustBePruned(Game a_newGameState, Game a_previousGameState)
    {
        //Previous speed:
        //double prevSpeed = a_previousGameState.getShip().v.mag();
        double newSpeed = a_newGameState.getShip().v.mag();
        //if(newSpeed < 0.001)
        if(newSpeed < 0.01)
        {
            return true;
        }

        //Collision with high damage.
        if(a_newGameState.getShip().getCollLastStep())
        {
            int collType = a_newGameState.getShip().getLastCollisionType();
            if(collType == PTSPConstants.DAMAGE_COLLISION_TYPE)
            {
                return true;
            }
        }


        return false;
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
        Node shipNode = NSGAIIController.m_graph.getClosestNodeTo(a_game.getShip().s.x, a_game.getShip().s.y);

        //Node objectNode = NSGAIIController.m_graph.getClosestNodeTo(a_gObj.s.x, a_gObj.s.y);
        //The closest node to the target's location (checking the cache).
        Node objectNode = null;
        if(m_nodeLookup.containsKey(a_objKey))
            objectNode = m_nodeLookup.get(a_objKey);
        else{
            objectNode = NSGAIIController.m_graph.getClosestNodeTo(a_gObj.s.x, a_gObj.s.y);
            m_nodeLookup.put(a_objKey, objectNode);
        }

        //Get the parh between the nodes.
        return NSGAIIController.m_graph.getPath(shipNode.id(), objectNode.id());
    }

    /**
     * Gets the path from the current location of the ship to the object passed as parameter.
     * @param a_gObj object ot get the path to.
     * @param a_objKey index of the object to look for.
     * @return the path from the current ship position to  a_gObj.
     */
    private Path getPathBetweenGameObjects(GameObject a_gObj, int a_objKey, GameObject a_gObj2, int a_objKey2)
    {
        //The closest node to the first object
        Node object1Node = null;
        if(m_nodeLookup.containsKey(a_objKey))
            object1Node = m_nodeLookup.get(a_objKey);
        else{
            object1Node = NSGAIIController.m_graph.getClosestNodeTo(a_gObj.s.x, a_gObj.s.y);
            m_nodeLookup.put(a_objKey, object1Node);
        }

        //The closest node to the second object
        Node object2Node = null;
        if(m_nodeLookup.containsKey(a_objKey2))
            object2Node = m_nodeLookup.get(a_objKey2);
        else{
            object2Node = NSGAIIController.m_graph.getClosestNodeTo(a_gObj2.s.x, a_gObj2.s.y);
            m_nodeLookup.put(a_objKey2, object2Node);
        }
        //Get the parh between the nodes.
        return NSGAIIController.m_graph.getPath(object1Node.id(), object2Node.id());
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
            if(idx >= a_pathDesired.length)
                return true;                 //Route is completed.

            if(i != a_pathDesired[idx])      //Mismatch.
                return false;

            idx++;
        }
        return true;
    }
}
