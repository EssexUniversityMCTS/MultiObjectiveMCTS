package controllers.mctsdriver;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Collections;
import java.util.List;

import framework.core.Game;

public class NearestWaypointNonPlanner extends DistanceMapBasedPlanner {

	public NearestWaypointNonPlanner(Parameters parameters)
	{
		super(parameters);
	}
	
	@Override
	public void analyseMap(Game game, long dueTime)
	{
		if (m_distanceMap == null)
			computeDistanceMaps(game.getMap());
	}

	@Override
	public boolean isFinishedPlanning()
	{
		return true;
	}

	@Override
	public int getNextWaypoint(Game game)
	{
		int nearestIndex = startPointIndex;
		double nearestDistance = Double.POSITIVE_INFINITY;
		
		for (int i=0; i<game.getNumWaypoints(); i++)
		{
			if (!game.getWaypoints().get(i).isCollected())
			{
				double d = m_distanceMap[i].getDistance(game.getShip().s);
				if (d <nearestDistance)
				{
					nearestIndex = i;
					nearestDistance = d;
				}
			}
		}
		
		return nearestIndex;
	}

	@Override
	public void advanceRoute()
	{
	}

	@Override
	public int countCollectedWaypointsOnRoute(Game game)
	{
		return game.getWaypointsVisited();
	}
	
	@Override
	public List<Integer> getRoute()
	{
		return null;
	}

	@Override
	public void draw(Graphics2D g, Game game)
	{
		int wp = getNextWaypoint(game);
		
		g.setColor(Color.white);
		final int radius = 8;
		g.drawOval((int)game.getWaypoints().get(wp).s.x - radius, (int)game.getWaypoints().get(wp).s.y - radius,
				2*radius, 2*radius);
	}

}
