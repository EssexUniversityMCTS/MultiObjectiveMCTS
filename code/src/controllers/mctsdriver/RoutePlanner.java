package controllers.mctsdriver;

import java.awt.Graphics2D;
import java.util.List;

import framework.core.Game;
import framework.utils.Vector2d;

public interface RoutePlanner
{
	public void analyseMap(Game game, long dueTime);
	public boolean isFinishedPlanning();
	
	public int getNextWaypoint(Game game);
	public void advanceRoute();
	public boolean IsRoutePointCollected(Game game, int wp);
	public double getDistanceToWaypoint(Vector2d point, int waypoint);
	public double getDistanceEvaluation(Game game, int currentWaypoint, int previousWaypoint);
	
	public Vector2d getPoint(int wp);
	public boolean pointIsWaypoint(int wp);
	public List<Integer> getRoute();
	
	public int countCollectedWaypointsOnRoute(Game game);
	
	public void draw(Graphics2D g, Game game);
}
