package controllers.mctsdriver;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import framework.core.Game;
import framework.core.Map;
import framework.utils.Vector2d;

public abstract class DistanceMapBasedPlanner implements RoutePlanner 
{
	protected DistanceMapFloodFiller[] m_distanceMap = null;
	protected Map m_map;
	protected List<Vector2d> m_points;
	protected int startPointIndex;
	protected Parameters m_par;
	
	protected enum PointType { WAYPOINT, FUELTANK, STARTPOINT };
	protected List<PointType> m_pointTypes;
	
	protected DistanceMapBasedPlanner(Parameters parameters)
	{
		m_par = parameters;
	}
	
	protected void computeDistanceMaps(Map map)
	{
		long startTime = System.currentTimeMillis();
		
		m_points = new ArrayList<Vector2d>();
		m_points.addAll(map.getWaypointPositions());
		if (m_par.INCLUDE_FUEL_TANKS_IN_ROUTE)
			m_points.addAll(map.getFuelTankPositions());
		m_points.add(map.getStartingPoint());
		startPointIndex = m_points.size() - 1;
		
		m_pointTypes = new ArrayList<PointType>();
		for (int i=0; i<map.getWaypointPositions().size(); i++) m_pointTypes.add(PointType.WAYPOINT);
		if (m_par.INCLUDE_FUEL_TANKS_IN_ROUTE)
			for (int i=0; i<map.getFuelTankPositions().size(); i++) m_pointTypes.add(PointType.FUELTANK);
		m_pointTypes.add(PointType.STARTPOINT);
		
		m_distanceMap = new DistanceMapFloodFiller[m_points.size() + 1];
		
		//DistanceMapFloodFiller.WRITE_DEBUG_IMAGES = true;
		for (int i=0; i<m_points.size(); i++)
		{
			m_distanceMap[i] = new DistanceMapFloodFiller(m_par, map, m_points.get(i));
			DistanceMapFloodFiller.WRITE_DEBUG_IMAGES = false;
			
			//m_distanceMap[i].dump("C:\\Users\\Ed\\Desktop\\flood\\map_" + i + ".png", null, true);
			//m_distanceMap[i].dumpInterpolated("C:\\Users\\Ed\\Desktop\\flood\\interp_" + i + ".png");
		}
		
		if (m_par.CONSOLE_OUTPUT) System.out.println("Floodfill maps computed in " + (System.currentTimeMillis()-startTime) + "ms");
	}
	
	public boolean IsRoutePointCollected(Game game, int wp)
	{
		switch (m_pointTypes.get(wp))
		{
		case WAYPOINT:
			return game.getWaypoints().get(wp).isCollected();
			
		case FUELTANK:
			return game.getFuelTanks().get(wp - game.getNumWaypoints()).isCollected();
			
		default:
			return false;
		}
	}
	
	/**
	 * When travelling along the path from thisIndex to otherIndex, determine the direction in which thisIndex is left
	 * @param thisIndex
	 * @param otherIndex
	 * @return Unit vector
	 */
	protected Vector2d getEdgeDirectionAt(int thisIndex, int otherIndex)
	{
		Vector2d thisPos  = m_points.get(thisIndex);
		Vector2d otherPos = m_points.get(otherIndex);

		int thisX = (int)thisPos.x;
		int thisY = (int)thisPos.y;
		
		// If there is line-of-sight between the two points, return the straight line direction
		if (m_map.checkObsFree(thisX, thisY, (int)otherPos.x, (int)otherPos.y))
		{
			Vector2d result = otherPos.copy().subtract(thisPos);
			result.normalise();
			return result;
		}
		
		// Otherwise, we follow the path until we lose line-of-sight to the start point,
		// and return the direction to the point we reach
		int x = thisX;
		int y = thisY;
		double lastD = Double.POSITIVE_INFINITY;
		
		while (m_distanceMap[otherIndex].getDistance(x, y) > 0)
		{
			// Descend the gradient of the distance map
			int bestX = -1, bestY = -1;
			double bestD = Double.POSITIVE_INFINITY;
			
			for (int dx=-1; dx<=1; dx++) for (int dy=-1; dy<=1; dy++)
			{
				if (dx==0 && dy==0) continue;
				double d = m_distanceMap[otherIndex].getDistance(x+dx, y+dy);
				if (d == DistanceMapFloodFiller.c_wall) continue;
				
				if (d < bestD)
				{
					bestX = x+dx; bestY = y+dy; bestD = d;
				}
			}
			
			if (Double.isInfinite(bestD)) break;
			if (bestD >= lastD) break;
			
			x = bestX; y = bestY; lastD = bestD;
			
			if (!m_map.checkObsFree(x, y, thisX, thisY))
				break;
		}
		
		Vector2d result = new Vector2d(x-thisX, y-thisY);
		result.normalise();
		return result;
	}

	@Override
	public double getDistanceToWaypoint(Vector2d point, int waypoint)
	{
		return m_distanceMap[waypoint].getDistance(point);
	}

	@Override
	public double getDistanceEvaluation(Game game, int currentWaypoint, int previousWaypoint)
	{
		if (previousWaypoint == -1) previousWaypoint = startPointIndex;
		
		Vector2d previousPos = m_points.get(previousWaypoint);
		double prevd = m_distanceMap[currentWaypoint].getDistance(previousPos);
		
		double d = m_distanceMap[currentWaypoint].getDistance(game.getShip().s);
		if (Double.isInfinite(d)) d = prevd;

		double result = 1 - d/prevd;
		
		/*Vector2d nextPos = game.getShip().v.copy().mul(PTSPConstants.T * 5).add(game.getShip().s);
		double nextDist = m_distanceMap[currentWaypoint].getDistance(nextPos);
		if (Double.isInfinite(nextDist)) nextDist = 10000; 
		double descentSpeed = nextDist - d;
		result -= 0.001 * descentSpeed;*/
		
		/*Vector2d gradient = m_distanceMap[currentWaypoint].getGradient(game.getShip().s);
		double speedAlongGradient = gradient.dot(game.getShip().v);
		result -= speedAlongGradient;*/
		
		return result;
	}

	@Override
	public Vector2d getPoint(int wp)
	{
		return m_points.get(wp);
	}

	@Override
	public boolean pointIsWaypoint(int wp)
	{
		return m_pointTypes.get(wp) == PointType.WAYPOINT;
	}
	
}
