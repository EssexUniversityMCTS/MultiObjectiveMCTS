package controllers.mctsdriver;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import framework.core.Game;
import framework.utils.Vector2d;

public class NearestWaypointPlanner extends DistanceMapBasedPlanner
{

	private List<Integer> m_route;
	private int m_positionInRoute = -1;

	public NearestWaypointPlanner(Parameters parameters)
	{
		super(parameters);
	}
	
	@Override
	public void analyseMap(Game game, long dueTime)
	{
		m_map = game.getMap();
		
		if (m_distanceMap == null)
			computeDistanceMaps(game.getMap());
		
		boolean[] waypointUsed = new boolean[game.getNumWaypoints()];
		for (int i=0; i<game.getNumWaypoints(); i++)
			waypointUsed[i] = false;
		
		Vector2d currentPos = game.getShip().s;
		
		m_route = new ArrayList<Integer>();
		
		while (m_route.size() < game.getNumWaypoints())
		{
			int nearestIndex = 0;
			double nearestDistance = Double.POSITIVE_INFINITY;
			
			for (int i=0; i<game.getNumWaypoints(); i++)
			{
				if (!waypointUsed[i])
				{
					double d = m_distanceMap[i].getDistance(currentPos);
					if (d <nearestDistance)
					{
						nearestIndex = i;
						nearestDistance = d;
					}
				}
			}
			
			m_route.add(nearestIndex);
			waypointUsed[nearestIndex] = true;
			currentPos = game.getWaypoints().get(nearestIndex).s;
		}
		
		m_positionInRoute = 0;
	}

	@Override
	public boolean isFinishedPlanning()
	{
		return true;
	}

	@Override
	public int getNextWaypoint(Game game)
	{
		return m_route.get(m_positionInRoute);
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
			if (game.getWaypoints().get(wp).isCollected())
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
		
		for (int wp : m_route)
		{
			int nextx = (int)m_map.getWaypointPositions().get(wp).x;
			int nexty = (int)m_map.getWaypointPositions().get(wp).y;
			
			g.setColor(Color.white);
			g.drawString("  " + m_route.indexOf(wp), nextx, nexty);
		}
	}

}
