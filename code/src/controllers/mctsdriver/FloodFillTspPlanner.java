package controllers.mctsdriver;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import javax.imageio.ImageIO;

import framework.core.Game;
import framework.core.Waypoint;
import framework.utils.Vector2d;

public class FloodFillTspPlanner extends DistanceMapBasedPlanner
{
	private boolean m_startedPlanning = false;
	private boolean m_finishedPlanning = false;
	private List<Integer> m_route;
	private int m_positionInRoute = -1;
	
	private class UndirectedEdge implements Comparable<UndirectedEdge>
	{
		private int m_va, m_vb;
		public int va() { return m_va; }
		public int vb() { return m_vb; }
		
		private Vector2d m_dirA, m_dirB;
		
		public Vector2d dirA()
		{
			if (m_dirA == null) m_dirA = getEdgeDirectionAt(m_va, m_vb);
			return m_dirA;
		}
		
		public Vector2d dirB() 
		{
			if (m_dirB == null) m_dirB = getEdgeDirectionAt(m_vb, m_va);
			return m_dirB;
		}
		
		public double m_weight;
		
		public UndirectedEdge(int a, int b, double weight)
		{
			m_va = Math.min(a, b);
			m_vb = Math.max(a, b);
			m_weight = weight;
			//m_dirA = getEdgeDirectionAt(m_va, m_vb);
			//m_dirB = getEdgeDirectionAt(m_vb, m_va);
			m_dirA = m_dirB = null;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof UndirectedEdge)
			{
				UndirectedEdge other = (UndirectedEdge)obj;
				return this.va() == other.va() && this.vb() == other.vb();
			}
			else
				return false;
		}
		
		@Override
		public int compareTo(UndirectedEdge o)
		{
			if (this.equals(o))
			{
				return 0;
			}
			else
			{
				int result = Double.compare(this.m_weight, o.m_weight);
				if (result == 0) result = o.m_va - this.m_va;
				if (result == 0) result = o.m_vb - this.m_vb;
				return result;
			}
		}
		
		@Override
		public String toString()
		{
			return String.format("%d -- %d", va(), vb());
		}
	}
	
	private List<Integer> traversePath(int startPoint, List<UndirectedEdge> pathEdges)
	{
		int currentPoint = startPoint;
		
		LinkedList<UndirectedEdge> tempEdges = new LinkedList<UndirectedEdge>(pathEdges);
		while (tempEdges.remove(null)) {}
		List<Integer> result = new ArrayList<Integer>();
		
		while (!tempEdges.isEmpty())
		{
			UndirectedEdge nextEdge = null;
			for (UndirectedEdge e : tempEdges)
			{
				if (e.va() == currentPoint || e.vb() == currentPoint)
				{
					nextEdge = e;
					break;
				}
			}
			
			if (nextEdge == null) break;
			
			tempEdges.remove(nextEdge);
			if (nextEdge.va() == currentPoint)
				currentPoint = nextEdge.vb();
			else
				currentPoint = nextEdge.va();
			
			result.add(currentPoint);
		}
		
		return result;
	}
	
	private double calcEdgeWeight(int fromPoint, int toPoint)
	{
		double result;
		
		if (m_par.ROUTE_EDGE_LAVA_WEIGHT == 1)
		{
			// Look up the distance in the distance map
			result = m_distanceMap[fromPoint].getDistance(m_points.get(toPoint));
		}
		else
		{
			// Traverse the gradient descent path, adding extra weight for steps through lava
			double h = 0;
			
			int x = (int)m_points.get(fromPoint).x;
			int y = (int)m_points.get(fromPoint).y;
			int destX = (int)m_points.get(toPoint).x;
			int destY = (int)m_points.get(toPoint).y;
			
			while (x != destX || y != destY)
			{
				int bestDX = 0, bestDY = 0;
				double bestScore = Double.POSITIVE_INFINITY;
				
				for (int dx=-1; dx<=1; dx++)
				{
					for (int dy=-1; dy<=1; dy++)
					{
						if (dx==0 && dy==0)
							continue;
						
						double d = m_distanceMap[toPoint].getDistance(x+dx, y+dy);
						if (d == DistanceMapFloodFiller.c_wall)
							continue;
						
						if (d < bestScore)
						{
							bestScore = d;
							bestDX = dx;
							bestDY = dy;
						}
					}
				}
				
				assert(bestDX != 0 || bestDY != 0);
				double currentScore = m_distanceMap[toPoint].getDistance(x, y); 
				assert(bestScore < currentScore);
				
				double dist = (bestDX==0 || bestDY==0) ? 1 : 1.414213562373095;
				if (m_map.isLava(x+bestDX, y+bestDY))
					dist *= m_par.ROUTE_EDGE_LAVA_WEIGHT;
				
				h += dist;
				x += bestDX;
				y += bestDY;
			}
			
			result = h;
		}
		
		if (m_pointTypes.get(fromPoint) == PointType.STARTPOINT && m_pointTypes.get(toPoint) == PointType.FUELTANK)
			result += m_par.EVAL_ROUTE_FIRST_FUEL_TANK_PENALTY;
		else if (m_pointTypes.get(fromPoint) == PointType.FUELTANK && m_pointTypes.get(toPoint) == PointType.STARTPOINT)
			result += m_par.EVAL_ROUTE_FIRST_FUEL_TANK_PENALTY;
		else if (m_pointTypes.get(fromPoint) == PointType.FUELTANK && m_pointTypes.get(toPoint) == PointType.FUELTANK)
			result += m_par.EVAL_ROUTE_CONSECUTIVE_FUEL_TANKS_PENALTY;
		
		return Math.pow(result, m_par.EVAL_ROUTE_EDGE_WEIGHT_EXPONENT);
	}
	
	UndirectedEdge[][] allEdges;

	private void planWaypointOrder(Game game)
	{
		long startTime = System.currentTimeMillis();
		
		// 2-D array of all edges indexed by node pairs, for 3-opt
		allEdges = new UndirectedEdge[m_points.size()+1][m_points.size()+1];
		
		// Queue of edges in ascending order of weight, for multiple fragment
		PriorityQueue<UndirectedEdge> edgeQueue = new PriorityQueue<UndirectedEdge>();
		
		if (m_par.CONSOLE_OUTPUT) System.out.println("  Starting to populate edges: " + (System.currentTimeMillis() - startTime));
		
		// Populate edges
		for (int i=0; i<m_points.size(); i++)
		{
			for (int j=0; j<i; j++)
			{
				double weight = calcEdgeWeight(i, j);
				UndirectedEdge e = new UndirectedEdge(i, j, weight);
				edgeQueue.add(e);
				allEdges[i][j] = allEdges[j][i] = e;
			}
			
			/*// Edges from starting point
			double weight = m_distanceMap[i].getDistance(game.getShip().s);
			int j = startPointIndex;
			UndirectedEdge e = new UndirectedEdge(i, j, weight);
			edgeQueue.add(e);
			allEdges[i][j] = allEdges[j][i] = e;*/
		}
		
		if (m_par.CONSOLE_OUTPUT) System.out.println("  Starting multiple fragment: " + (System.currentTimeMillis() - startTime));

		// Multiple fragment
		int[] nodeDegree = new int[m_points.size()];
		for (int i=0; i<m_points.size(); i++) nodeDegree[i] = 0;
		nodeDegree[startPointIndex] = 1;
		
		List<UndirectedEdge> pathEdges = new ArrayList<UndirectedEdge>();
		
		while (pathEdges.size() < m_points.size() - 1)
		{
			UndirectedEdge e = edgeQueue.poll();
			
			if (nodeDegree[e.va()] < 2 && nodeDegree[e.vb()] < 2
					&& !traversePath(e.va(), pathEdges).contains(e.vb()))
			{
				pathEdges.add(e);
				nodeDegree[e.va()]++;
				nodeDegree[e.vb()]++;
			}
		}
		
		boolean foundNodeWithDegree1 = false;
		for (int d : nodeDegree)
			if (d == 1 && !foundNodeWithDegree1)
				foundNodeWithDegree1 = true;
			else
				assert d==2;
		assert foundNodeWithDegree1;
		
		assert pathEdges.size() == m_points.size() - 1;
		
		// Traverse the path to determine the route
		m_route = new ArrayList<Integer>(traversePath(startPointIndex, pathEdges));
		assert m_route.size() == m_points.size() - 1;
	}

	class PassStateFor3Opt
	{
		public int b,c,e;
		public boolean routeChanged;
	}
	
	PassStateFor3Opt m_suspended3OptPassState = null;
	
	private boolean do3OptPass(Game game, long timeDue)
	{
		// 3-opt
		double currentRouteFitness = getRouteFitness(game, m_route);
		if (m_par.CONSOLE_OUTPUT) System.out.println("current route: " + m_route.toString());
		if (m_par.CONSOLE_OUTPUT) System.out.println("currentRouteFitness = " + currentRouteFitness);

		PassStateFor3Opt s = new PassStateFor3Opt();
		if (m_par.CONSOLE_OUTPUT) System.out.println("  Starting a 3-opt pass: " + (System.currentTimeMillis() - timeDue));
		s.routeChanged = false;
		
		for (s.e=m_positionInRoute+1; s.e<=m_route.size(); s.e++)
		{
			for (s.c=m_positionInRoute+1; s.c<s.e; s.c++)
			{
				for (s.b=m_positionInRoute+1; s.b<s.c; s.b++)
				{   
					if (m_suspended3OptPassState != null)
					{
						if (m_par.CONSOLE_OUTPUT) System.out.println("    resuming 3-opt pass");
						s = m_suspended3OptPassState;
						m_suspended3OptPassState = null;
					}
					
					if (System.currentTimeMillis() >= timeDue)
					{
						if (m_par.CONSOLE_OUTPUT) System.out.println("    3-opt pass timed out");
						m_suspended3OptPassState = s;
						return true;
					}
					
					int d = s.c+1;
					// abcdef
					Collections.reverse(m_route.subList(s.b, s.c));	// acbdef
					Collections.reverse(m_route.subList(  d, s.e));	// acbedf
					Collections.reverse(m_route.subList(s.b, s.e));	// adebcf
					
					double fitness = getRouteFitness(game, m_route);
					if (fitness < currentRouteFitness)
					{
						s.routeChanged = true;
						currentRouteFitness = fitness;
						if (m_par.CONSOLE_OUTPUT) System.out.println("current route: " + m_route.toString());
						if (m_par.CONSOLE_OUTPUT) System.out.println("currentRouteFitness = " + currentRouteFitness);
						continue;
					}
					else
					{
						Collections.reverse(m_route.subList(s.b, s.e));
						Collections.reverse(m_route.subList(  d, s.e));
						Collections.reverse(m_route.subList(s.b, s.c));
					
						// abcdef
						Collections.reverse(m_route.subList(  d, s.e));	// abcedf
						Collections.reverse(m_route.subList(s.b, s.e));	// adecbf
						
						fitness = getRouteFitness(game, m_route);
						if (fitness < currentRouteFitness)
						{
							s.routeChanged = true;
							currentRouteFitness = fitness;
							if (m_par.CONSOLE_OUTPUT) System.out.println("current route: " + m_route.toString());
							if (m_par.CONSOLE_OUTPUT) System.out.println("currentRouteFitness = " + currentRouteFitness);
							continue;
						}
						else
						{
							Collections.reverse(m_route.subList(s.b, s.e));
							Collections.reverse(m_route.subList(  d, s.e));
						}
					}
					
				}
			}
		}
		
		return s.routeChanged;
	}
	
	double getRouteFitness(Game game, List<Integer> route)
	{
		double fitness = 0;
		
		Vector2d currentDirection = new Vector2d(0, -1);
		int waypointsLeft = m_map.getWaypointPositions().size();
		
		// Break out once waypointsLeft hits 0 -- any fuel tanks after the last waypoint are considered optional
		for (int i=0; i<route.size() && waypointsLeft > 0; i++)
		{
			int wpA, wpB;
			Vector2d pointA, pointB;
			
			if (i == 0)
			{
				wpA = startPointIndex;
				pointA = game.getMap().getStartingPoint();
			}
			else
			{
				wpA = route.get(i-1);
				pointA = m_points.get(wpA);
			}
			
			wpB = route.get(i);
			pointB = m_points.get(wpB);

			double straightLineDistance = pointB.copy().subtract(pointA).mag();

			UndirectedEdge edge = allEdges[wpA][wpB];
			
			fitness += edge.m_weight;
			
			Vector2d outgoingDirection = (wpA == edge.va()) ? edge.dirA() : edge.dirB();
			double cosAngle = -currentDirection.dot(outgoingDirection);
			double angleCost;
			if (cosAngle > 0.7)
				angleCost = 0.0 * (1 - cosAngle);
			else if (cosAngle < 0 && cosAngle > -0.7)
				angleCost = 0.5 * (1 - cosAngle);
			else if (cosAngle < -0.7)
				angleCost = 0.75 * (1 - cosAngle);
			else
				angleCost = 0.25 * (1 - cosAngle);
			
			fitness += m_par.ROUTE_ANGLE_WEIGHT * angleCost;
			fitness += m_par.ROUTE_DIRECTNESS_WEIGHT * allEdges[wpA][wpB].m_weight / straightLineDistance;
			
			// Direction at end of this edge
			currentDirection = (wpB == edge.vb()) ? edge.dirB() : edge.dirA();
			
			switch (m_pointTypes.get(wpB))
			{
			case WAYPOINT:
				waypointsLeft--;
				break;
				
			case FUELTANK:
				fitness -= m_par.EVAL_ROUTE_FUEL_TANK_BONUS;
				break;
				
			case STARTPOINT:
				// do nothing
				break;
			}
		}
		
		return fitness;
	}
	
	public FloodFillTspPlanner(Parameters parameters)
	{
		super(parameters);
	}

	@Override
	public void analyseMap(Game game, long dueTime)
	{
		if (!m_startedPlanning)
		{
			m_startedPlanning = true;
			
			m_map = game.getMap();
			
			if (m_distanceMap == null)
				computeDistanceMaps(m_map);
			
			planWaypointOrder(game);

			m_positionInRoute = -1;
			
			while (!m_finishedPlanning && System.currentTimeMillis() < dueTime)
				if (do3OptPass(game, dueTime) == false)
					m_finishedPlanning = true;

			m_positionInRoute = 0;
			
			//m_distanceMap[getNextWaypoint()].dump("floodfill.png");
		}
		else
		{
			while (!m_finishedPlanning && System.currentTimeMillis() < dueTime)
				if (do3OptPass(game, dueTime) == false)
					m_finishedPlanning = true;
		}
	}

	@Override
	public boolean isFinishedPlanning()
	{
		return m_finishedPlanning;
	}

	@Override
	public int getNextWaypoint(Game game)
	{
		int i = countCollectedWaypointsOnRoute(game); 
		return m_route.get(i);
	}

	@Override
	public void advanceRoute()
	{
		m_positionInRoute++;
	}
	
	@Override
	public List<Integer> getRoute()
	{
		return Collections.unmodifiableList(m_route);
	}

	@Override
	public int countCollectedWaypointsOnRoute(Game game)
	{
		int routeWaypoints = 0;
		for (int wp : m_route)
		{
			if (IsRoutePointCollected(game, wp))
				routeWaypoints++;
			else
				break;
		}
		
		return routeWaypoints;
	}
	
	@Override
	public void draw(Graphics2D g, Game game) 
	{
		g.setFont(new Font("Arial", Font.BOLD, 10));
		
		int lastx = (int)m_map.getStartingPoint().x;
		int lasty = (int)m_map.getStartingPoint().y;
		int lastwp = startPointIndex;
		
		for (int wp : m_route)
		{
			int nextx = (int)m_points.get(wp).x;
			int nexty = (int)m_points.get(wp).y;
			
			g.setColor(Color.white);
			g.drawString("  " + m_route.indexOf(wp), nextx, nexty);
			
			//g.setColor(Color.orange);
			//g.drawLine(lastx, lasty, nextx, nexty);
			
			UndirectedEdge edge = allEdges[lastwp][wp];
			Vector2d lastDir = (edge.va() == lastwp) ? edge.dirA() : edge.dirB();
			Vector2d nextDir = (edge.va() == wp) ? edge.dirA() : edge.dirB();

			double directionSize = 30;
			g.setColor(Color.green);
			g.drawLine(lastx, lasty, lastx + (int)(lastDir.x * directionSize), lasty + (int)(lastDir.y * directionSize));
			g.setColor(Color.orange);
			g.drawLine(nextx, nexty, nextx + (int)(nextDir.x * directionSize), nexty + (int)(nextDir.y * directionSize));
			
			lastx=nextx; lasty=nexty; lastwp = wp;
		}
	}
}
