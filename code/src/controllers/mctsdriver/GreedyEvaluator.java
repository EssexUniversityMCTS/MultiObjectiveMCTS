package controllers.mctsdriver;

import framework.core.Game;

public class GreedyEvaluator extends StateEvaluator {

	public GreedyEvaluator(Parameters parameters)
	{
		super(parameters);
	}

	@Override
	public double evaluate(Game state, ExtraPlayoutInfo extraInfo, Game rootState, RoutePlanner planner)
	{
		int routeWaypoints = planner.countCollectedWaypointsOnRoute(rootState);
		double result = m_par.EVAL_WAYPOINT_BONUS * routeWaypoints;
		result += m_par.EVAL_WAYPOINT_BONUS * m_par.EVAL_EARLY_WAYPOINT_WEIGHT * (rootState.getWaypointsVisited() - routeWaypoints);
		
		int currentWaypoint = planner.getNextWaypoint(rootState);
		
		if (state.getWaypoints().get(currentWaypoint).isCollected())
		{
			result += m_par.EVAL_WAYPOINT_BONUS;
		}
		else
		{
			int previousWaypoint;
			
			if (rootState.getWaypointsVisited() == 0)
				previousWaypoint = -1;
			else
				previousWaypoint = rootState.getVisitOrder().get(rootState.getWaypointsVisited() - 1);
			
			double routeEval = planner.getDistanceEvaluation(state, currentWaypoint, previousWaypoint) * m_par.EVAL_NAV_WEIGHT;
			//routeEval = Math.max(0, Math.min(routeEval, 1)); // Clamp routeEval to [0,1]
			result += routeEval;
		}
		
		result += m_par.EVAL_SPEED_WEIGHT * (state.getShip().v.mag() - m_par.EVAL_SPEED_PENALTY);
		
		if (state.isEnded() && state.getWaypointsLeft() > 0)
		{
			result += m_par.EVAL_TIMEOUT_PENALTY;
		}
		
		return result;
	}

}
