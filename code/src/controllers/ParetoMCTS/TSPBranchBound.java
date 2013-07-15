package controllers.ParetoMCTS;

import framework.core.FuelTank;
import framework.core.Game;
import framework.core.Waypoint;
import framework.graph.Graph;
import framework.graph.Node;
import framework.graph.Path;
import framework.utils.Vector2d;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * PTSP-Competition
 * Branch and bound algorithm to get a TSP ordering.
 */
public class TSPBranchBound
{
    /**
     * Number of nodes in the map (cities in the TSP).
     */
    public static int MAX_NODES;

    /**
     * Best TSP path found so far.
     */
    public TSPPath m_tspBestPath;

    /**
     * Game graph
     */
    public Graph m_graph;

    /**
     * Game reference
     */
    public Game m_game;

    /**
     * Node positions.
     */
    public TreeMap<Integer,Vector2d> m_nodes;

    /**
     * Paths using A*.
     */
    public Path[][] m_paths;

    /**
     * Distances using A*.
     */
    public double[][] m_dists;

    /**
     * Distances from Origin
     */
    public double[] m_distOrigin;

    /**
     * Minimum cost from orders found.
     */
    private double m_minCost = Double.MAX_VALUE;

    /** CACHE OF ROUTES TO SPEED UP EXPERIMENTS **/
    private TreeMap<String,ArrayList<Integer>> m_PreRoutes;


    /**
     * Creates the TSP Graph.
     * @param a_game Game to take the waypoints from.
     * @param a_graph Graph to take the costs
     */
    public TSPBranchBound(Game a_game, Graph a_graph)
    {
        MAX_NODES =  a_game.getWaypoints().size() + a_game.getFuelTanks().size();
        m_graph = a_graph;
        m_nodes = new TreeMap<Integer, Vector2d>();
        m_dists = new double[MAX_NODES][MAX_NODES];
        m_distOrigin = new double[MAX_NODES];
        m_paths = new Path[MAX_NODES][MAX_NODES];

        int index = 0;
        for(Waypoint way: a_game.getWaypoints())        //Add all waypoints to the path.
        {
            m_nodes.put(index++, way.s.copy());
        }

        for(FuelTank ft: a_game.getFuelTanks())        //Add all fuel tanks to the path.
        {
            m_nodes.put(index++, ft.s.copy());
        }


        //Precompute distances between all waypoints.
        for(int i = 0; i < m_nodes.size(); ++i)
        {
            Vector2d a1 = m_nodes.get(i);
            for(int j = 0; j < m_nodes.size(); ++j)
            {
                if(i > j)
                {
                    Vector2d a2 = m_nodes.get(j);
                    m_paths[i][j] = getPath(a1, a2);
                    double distance = m_paths[i][j].m_cost;
                    m_paths[j][i] = getPath(a2, a1);  //we need both directions, but it's symmetric.

                    m_dists[i][j] = distance;
                    m_dists[j][i] = distance;

                }else if(i == j){
                    m_dists[i][i] = Double.MAX_VALUE;
                }
            }
        }

        //Precompute distances from starting position to all waypoints.
        Vector2d startingPoint = a_game.getMap().getStartingPoint();
        for(int i = 0; i < m_nodes.size(); ++i)
        {
            Vector2d a1 = m_nodes.get(i);
            double distance = getDistance(startingPoint, a1);//a1.dist(startingPoint);
            m_distOrigin[i] = distance;
        }


        m_PreRoutes = new TreeMap<String, ArrayList<Integer>>();
        m_PreRoutes.put("maps/ptsp_map01.map",  getArrayObject(new int[]{7,8,11,2,6,13,4,1,10,0,3,5,12,9}));
        m_PreRoutes.put("maps/ptsp_map02.map",  getArrayObject(new int[]{12,4,1,2,10,0,3,11,6,9,7,13,8,5}));
        m_PreRoutes.put("maps/ptsp_map08.map",  getArrayObject(new int[]{7,13,9,8,11,4,5,12,6,3,0,1,10,2}));
        m_PreRoutes.put("maps/ptsp_map19.map",  getArrayObject(new int[]{9,4,1,10,3,12,6,8,13,7,5,11,2,0}));
        m_PreRoutes.put("maps/ptsp_map24.map",  getArrayObject(new int[]{4,2,10,3,11,0,1,5,13,8,9,6,12,7}));
        m_PreRoutes.put("maps/ptsp_map35.map",  getArrayObject(new int[]{12,4,6,8,9,13,7,11,2,0,3,5,10,1}));
        m_PreRoutes.put("maps/ptsp_map40.map",  getArrayObject(new int[]{2,3,0,10,1,12,11,4,6,8,9,13,5,7}));
        m_PreRoutes.put("maps/ptsp_map45.map",  getArrayObject(new int[]{5,9,13,7,12,4,11,2,0,10,1,3,6,8}));
        m_PreRoutes.put("maps/ptsp_map56.map",  getArrayObject(new int[]{8,12,5,10,0,3,1,2,4,11,13,6,7,9}));
        m_PreRoutes.put("maps/ptsp_map61.map",  getArrayObject(new int[]{13,8,7,4,11,6,1,3,0,2,10,5,12,9}));

        /*
        {,"maps/ptsp_map02.map","maps/ptsp_map08.map",
                "maps/ptsp_map19.map","maps/ptsp_map24.map","maps/ptsp_map35.map","maps/ptsp_map40.map",
                "maps/ptsp_map45.map","maps/ptsp_map56.map","maps/ptsp_map61.map"};
         */

    }

    /**
     * Gets the path from position a_org to a_dest
     * @param a_org  Origin
     * @param a_dest Destination
     * @return The path .
     */
    private Path getPath(Vector2d a_org, Vector2d a_dest)
    {
        Node org = m_graph.getClosestNodeTo(a_org.x, a_org.y);
        Node dest = m_graph.getClosestNodeTo(a_dest.x, a_dest.y);
        return m_graph.getPath(org.id(), dest.id());
    }


    /**
     * Gets the path distance from position a_org to a_dest
     * @param a_org  Origin
     * @param a_dest Destination
     * @return The distance.
     */
    private double getDistance(Vector2d a_org, Vector2d a_dest)
    {
        Path p  = getPath(a_org, a_dest);
        return p.m_cost;
    }

    /**
     * Solves the TSP (Branch and Bound algorithm).
     */
    public void solve()
    {
         //Create a default one, to be the best so far.
        int[] defaultBestPath = new int[MAX_NODES];
        for(int i =0; i < MAX_NODES; ++i)
            defaultBestPath[i] = i;
        double cost = getPathCost(defaultBestPath);
        m_tspBestPath = new TSPPath(MAX_NODES, defaultBestPath, cost);

        //Create an empty path to start with.
        int[] empty = new int[MAX_NODES];
        cost = 0;
        TSPPath emptyPath = new TSPPath(0, empty, cost);

        //And do the search (it updates m_tspBestPath)
        _search(emptyPath);

    }

    /**
     * Gets the cost of a given path
     * @param a_path Path to get the cost.
     * @return the total cost.
     */
    private double getPathCost(int[] a_path)
    {
        int index = 0;
        double cost = 0;

        //Cost from the origin to the first waypoint.
        if(a_path[index] == -1)
            return -1;
        else cost = m_distOrigin[a_path[index]];
        index++;

        //Add the cost between waypoints from start to end of the path.
        while(index < a_path.length && a_path[index] != -1)
        {
            double thisCost = m_dists[a_path[index-1]][a_path[index]];
            cost += thisCost;
            index++;
        }
        return cost;
    }

    /**
     * Recursive search of TSP paths.
     * @param a_currentPath  current path being built.
     */
    private void _search(TSPPath a_currentPath)
    {
        if(a_currentPath.m_nNodes == m_tspBestPath.m_nNodes)
        {
            //We have a path with all nodes in it. Check if m_tspBestPath needs to be updated.
            if(a_currentPath.m_totalCost < m_tspBestPath.m_totalCost)
            {
                if(a_currentPath.m_totalCost < m_minCost) m_minCost = a_currentPath.m_totalCost;
                m_tspBestPath = a_currentPath;
            }
        }else
        {
            //Take all nodes...
            for(int i = 0; i < MAX_NODES; ++i)
            {
                //..  that are not included in a_currentPath.
                if(!a_currentPath.includes(i))
                {
                     //Get the cost to this new link.
                    double linkCost;
                    if(a_currentPath.m_nNodes == 0)
                    {
                        linkCost = m_distOrigin[i];
                    }else{
                        int lastNode = a_currentPath.m_path[a_currentPath.m_nNodes-1];
                        linkCost =  m_dists[lastNode][i];
                    }

                    //Build the new path
                    double newCost = a_currentPath.m_totalCost + linkCost;
                    if(newCost < m_tspBestPath.m_totalCost)
                    {
                        //search!
                        TSPPath nextPath = new TSPPath(a_currentPath, i, newCost);
                        _search(nextPath);
                    }
                }
            }
        }
    }

    /**
     * Returns the best path found by this solver.
     * @return the path.
     */
    public int[] getBestPath()
    {
        return m_tspBestPath.m_path;
    }

    private ArrayList<Integer> getArrayObject(int []values)
    {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for(int i = 0; i < values.length; ++i)
            list.add(values[i]);
        return list;
    }

    public int[] getPreRouteArray(String a_key)
    {
        ArrayList<Integer> list = m_PreRoutes.get(a_key);
        int[] array = new int[list.size()];
        int i = 0;
        for(int v : list)
        {
            array[i++] = v;
        }
        return array;
    }


    /**
     * PTSP-Competition
     * Helper class for the TSP solver. Manages nodes and costs in a TSP path.
     */
    private class TSPPath
    {
        /**
         * Number of nodes present in this path.
         */
        public int m_nNodes;

        /**
         * Cost of this path.
         */
        public double m_totalCost;

        /**
         * The path itself.
         */
        public int[] m_path;

        /**
         * Constructor for TSP path.
         * @param a_nNodes  Number of nodes in the path.
         * @param a_nodes Nodes.
         * @param a_totCost  cost of the path so far.
         */
        public TSPPath(int a_nNodes, int[] a_nodes, double a_totCost)
        {
            m_path = new int[a_nodes.length];
            m_nNodes = a_nNodes;
            m_totalCost =a_totCost;
            System.arraycopy(a_nodes, 0, m_path, 0, a_nNodes);
        }


        /**
         *   Constructs a TSP path from and old one, adding a new node and the associated cost.
         * @param a_base TSP path used to build the new path.
         * @param a_newNode new node to add.
         * @param a_newCost cost to add to the base TSP path.
         */
        public TSPPath(TSPPath a_base, int a_newNode, double a_newCost)
        {
            m_path = new int[MAX_NODES];
            m_nNodes = a_base.m_nNodes+1;
            m_totalCost =a_newCost;
            System.arraycopy(a_base.m_path, 0, m_path, 0, a_base.m_nNodes);
            m_path[m_nNodes-1] = a_newNode;
        }

        /**
         * Checks if a given node is present in this path.
         * @param a_nodeId id of the node to look for.
         * @return true if the node is already in the path.
         */
        public boolean includes(int a_nodeId)
        {
            for(int i =0; i < m_nNodes; ++i)
                if(m_path[i] == a_nodeId)
                    return true;
            return false;
        }

    }

}
