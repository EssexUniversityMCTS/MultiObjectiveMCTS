package controllers.mctsdriver;

import framework.core.Game;
import framework.core.PTSPConstants;

public class SteppingEvaluator extends StateEvaluator 
{
	public SteppingEvaluator(Parameters parameters)
	{
		super(parameters);
	}

	@Override
	public double evaluate(Game state, ExtraPlayoutInfo extraInfo, Game rootState, RoutePlanner planner)
	{
		int routeWaypoints = planner.countCollectedWaypointsOnRoute(state);
		double result = m_par.EVAL_WAYPOINT_BONUS * routeWaypoints;
		result += m_par.EVAL_WAYPOINT_BONUS * m_par.EVAL_EARLY_WAYPOINT_WEIGHT * (state.getWaypointsVisited() - routeWaypoints);
		
		int currentWaypoint = planner.getNextWaypoint(state);
		int previousWaypoint;
		
		if (state.getWaypointsVisited() == 0)
			previousWaypoint = -1;
		else
			previousWaypoint = state.getVisitOrder().get(state.getWaypointsVisited() - 1);
		
		double routeEval = planner.getDistanceEvaluation(state, currentWaypoint, previousWaypoint) * m_par.EVAL_NAV_WEIGHT;
		//routeEval = Math.max(0, Math.min(routeEval, 1)); // Clamp routeEval to [0,1]
		result += routeEval;
		
		result += m_par.EVAL_SPEED_WEIGHT * (state.getShip().v.mag() - m_par.EVAL_SPEED_PENALTY);
		
		if (state.isEnded() && state.getWaypointsLeft() > 0)
		{
			result += m_par.EVAL_TIMEOUT_PENALTY;
		}
		
		result += m_par.EVAL_DAMAGE_PENALTY * state.getShip().getDamage();
		result += m_par.EVAL_FUEL_PENALTY * (PTSPConstants.INITIAL_FUEL - state.getShip().getRemainingFuel());
		result += m_par.EVAL_FUEL_TANK_BONUS * state.getFuelTanksCollected();
		
		if (extraInfo != null)
			result += m_par.EVAL_COLLISION_DAMAGE_PENALTY * extraInfo.m_collisionDamage;
		
		return result;
	}

}
