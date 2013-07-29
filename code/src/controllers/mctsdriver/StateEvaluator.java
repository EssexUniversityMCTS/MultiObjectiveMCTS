package controllers.mctsdriver;

import framework.core.Game;

public abstract class StateEvaluator
{
	Parameters m_par;
	
	protected StateEvaluator(Parameters parameters)
	{
		m_par = parameters;
	}
	
	public abstract double evaluate(Game state, ExtraPlayoutInfo extraInfo, Game rootState, RoutePlanner planner);
}
