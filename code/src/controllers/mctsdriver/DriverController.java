package controllers.mctsdriver;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import framework.core.Controller;
import framework.core.Game;
import framework.core.PTSPConstants;
import framework.core.Ship;
import framework.core.Waypoint;
import framework.utils.Vector2d;

public abstract class DriverController extends Controller
{
	public static DriverController ActiveMctsController = null;
	
	protected Parameters m_par, m_normalPar, m_panicPar;
	
	protected class MetaAction
	{
		private int m_rotationSteps;
		private int m_rotationDirection;
		private int m_thrustDuty;
		private int m_thrustCycle;
		
		public MetaAction(int rotationSteps, int rotationDirection, boolean applyThrust)
		{
			m_rotationSteps = rotationSteps;
			m_rotationDirection = rotationDirection;
			m_thrustDuty = applyThrust ? 1 : 0;
			m_thrustCycle = 1;
		}
		
		public MetaAction(int rotationSteps, int rotationDirection, int thrustDuty, int thrustCycle)
		{
			m_rotationSteps = rotationSteps;
			m_rotationDirection = rotationDirection;
			m_thrustDuty = thrustDuty;
			m_thrustCycle = thrustCycle;
		}
		
		public int getNumSteps()
		{
			return m_rotationSteps;
		}
		
		public int getDirection()
		{
			return m_rotationDirection;
		}
		
		/*public boolean getApplyThrust()
		{
			return m_applyThrust;
		}*/
		
		private int GetNextCommand(int step)
		{
			int nextAction = ACTION_NO_FRONT;
			
			if (step < getNumSteps())
			{
				if (getDirection() < 0)
					nextAction = ACTION_NO_LEFT;
				else if (getDirection() > 0)
					nextAction = ACTION_NO_RIGHT;
				
				if (step % m_thrustCycle < m_thrustDuty)
					nextAction += 3;
			}
			
			return nextAction;
		}

		@Override
		public String toString()
		{
			return String.format("%d * % 2d, %d:%d", m_rotationSteps, m_rotationDirection, m_thrustDuty, m_thrustCycle);
		}
	}
	
	protected Random rng;
	
	protected List<MetaAction> m_actionList;
	
	private MetaAction m_currentMetaAction;
	private int m_currentMetaActionStep;
	protected double getPositionInCurrentMetaAction()
	{
		if (m_currentMetaAction == null)
			return 0;
		else
			return (double)m_currentMetaActionStep / m_currentMetaAction.m_rotationSteps;
	}
	
	protected Game m_rootGameState;
	private int expectedTurns;
	
	protected RoutePlanner m_routePlanner;
	protected StateEvaluator m_evaluator;
	
	public DriverController(Game a_game, long a_timeDue, Parameters parameters)
	{
		if (parameters == null)
			parameters = new Parameters();
		
		initialise(a_game, a_timeDue, parameters);
	}
	
	protected void initialise(Game a_game, long a_timeDue, Parameters parameters)
	{
		m_par = parameters;
		m_normalPar = parameters;
		
		m_panicPar = parameters.clone();
		m_panicPar.PWM_THRUST = false;
		m_panicPar.PRUNE_STALLING_MOVES = true;
		m_panicPar.MCTS_PLAYOUT_LIMIT = 3;
		m_panicPar.EVAL_NAV_WEIGHT = 0.75;
		m_panicPar.EVAL_WAYPOINT_BONUS = 10.0;
		m_panicPar.EVAL_EARLY_WAYPOINT_WEIGHT = 1.0;
		m_panicPar.EVAL_GREEDY_WEIGHT = 0;
		m_panicPar.EVAL_SPEED_WEIGHT = 0;
		m_panicPar.EVAL_SPEED_PENALTY = 0;
		m_panicPar.EVAL_DAMAGE_PENALTY = 0;
		m_panicPar.EVAL_COLLISION_DAMAGE_PENALTY = 0;
		m_panicPar.EVAL_FUEL_PENALTY = 0;
		m_panicPar.EVAL_FUEL_TANK_BONUS = 10.0;
		m_panicPar.PANIC_MODE_RATIO = Double.POSITIVE_INFINITY;
		
		long startTime = System.currentTimeMillis();
		ActiveMctsController = this;
		rng = new Random(System.currentTimeMillis());
		
		expectedTurns = 0;
		
		resetSearch(a_game);

		m_actionList = GenerateActionList();
		
		try
		{
			Class plannerClass = Class.forName(m_par.PLANNER_CLASS_NAME);
			Constructor plannerCtor = plannerClass.getConstructor(Parameters.class);
			m_routePlanner = (RoutePlanner)plannerCtor.newInstance(m_par);
			
			Class evaluatorClass = Class.forName(m_par.EVALUATOR_CLASS_NAME);
			Constructor evaluatorCtor = evaluatorClass.getConstructor(Parameters.class);
			m_evaluator = (StateEvaluator)evaluatorCtor.newInstance(m_par);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		m_routePlanner.analyseMap(a_game, a_timeDue - 200);
		if (m_par.CONSOLE_OUTPUT) System.out.println(String.format("%dms: analyseMap finished ", System.currentTimeMillis() - startTime));
		
		// startTime = System.currentTimeMillis();
		
		doSearch(a_timeDue - 200);
	}
	
	boolean m_finishedRoutePlanning = false;
	int m_lastWaypointCount = -1;
	
	protected void resetSearch(Game newRootState)
	{
		m_rootGameState = newRootState.getCopy();
		m_currentMetaAction = null;
	}
	
	protected abstract void doSearch(long timeDue);
	protected abstract boolean isSearchTreeEmpty();
	protected abstract MetaAction getBestMetaAction();
	
	@Override
	public int getAction(Game a_game, long a_timeDue)
	{
		long startTime = System.currentTimeMillis();
		
		if (m_lastWaypointCount != a_game.getWaypointsVisited())
		{
			while (m_routePlanner.IsRoutePointCollected(a_game, m_routePlanner.getNextWaypoint(a_game)))
				m_routePlanner.advanceRoute();

			m_lastWaypointCount = a_game.getWaypointsVisited();
			
			// Leave panic mode
			if (m_par == m_panicPar)
			{
				leavePanicMode();
			}
		}
			
		if (!m_finishedRoutePlanning && !m_routePlanner.isFinishedPlanning())
		{
			long timeDue = a_timeDue - m_par.MCTS_TIME_LIMIT_WHILE_ROUTEPLANNING;
			if (m_par.WAIT_FOR_ROUTEPLANNER)
				timeDue = a_timeDue;
			
			m_routePlanner.analyseMap(a_game, timeDue);
			
			if (m_par.WAIT_FOR_ROUTEPLANNER)
				return 0;
		}
		else
		{
			m_finishedRoutePlanning = true;
		}
		
		if (a_game.getTotalTime() != expectedTurns)
		{
			if (m_par.CONSOLE_OUTPUT)
			{
				System.out.println("ACTION COUNT MISMATCH:");
				System.out.print("Actual: ");
				System.out.println(a_game.getTotalTime());
				System.out.print("Expected: ");
				System.out.println(expectedTurns);
			}
			
			expectedTurns = a_game.getTotalTime();
			resetSearch(a_game);
		}
		
		//if (m_currentMetaAction != null)
		//	System.out.println(String.format("WP: %d to %d STEPS: %d of %d", a_game.getWaypointsVisited(),m_rootGameState.getWaypointsVisited(),m_currentMetaActionStep,m_currentMetaAction.getNumSteps()));
		
		if (m_currentMetaAction != null && m_currentMetaActionStep == m_currentMetaAction.getNumSteps())
		{
			
			if (a_game.getShip().s.x != m_rootGameState.getShip().s.x || a_game.getShip().s.y != m_rootGameState.getShip().s.y)
			{
				if (m_par.CONSOLE_OUTPUT)
				{
					System.out.println("POSITION MISMATCH:");
					System.out.print("Actual: ");
					System.out.println(a_game.getShip().s);
					System.out.print("Expected: ");
					System.out.println(m_rootGameState.getShip().s);
				}
				
				resetSearch(a_game);
			}
		}
		
		expectedTurns++;
		
		int returnCommand = -1;
		
		if (m_currentMetaAction != null)
		{
			if (m_currentMetaActionStep < m_currentMetaAction.getNumSteps())
			{
				returnCommand = m_currentMetaAction.GetNextCommand(m_currentMetaActionStep);
				
				m_currentMetaActionStep++;
			}
			else
				m_currentMetaAction = null;
		}
		
		boolean mctsDone = false;
		
		if (m_currentMetaAction == null)
		{
			// Check for panic mode
			if (m_routePlanner.getDistanceToWaypoint(a_game.getShip().s, m_routePlanner.getNextWaypoint(a_game)) / a_game.getStepsLeft()
					> m_par.PANIC_MODE_RATIO)
			{
				enterPanicMode();
			}
			
			if (isSearchTreeEmpty())
			{
				doSearch(a_timeDue - 40 + m_par.MCTS_TIME_LIMIT);
				mctsDone = true;
			}
			
			// If the search timed out before it even started, skip the turn
			if (isSearchTreeEmpty())
				return 0;
			
			MetaAction bestMetaAction = getBestMetaAction();
			
			ApplyMetaAction(m_rootGameState, bestMetaAction, null);
			//System.out.println(String.format("WP: %d STEPS: %d", m_rootGameState.getWaypointsVisited(),bestChild.GetIncomingMetaAction().getNumSteps()));
			resetSearch(m_rootGameState);			
			
			SetNewMetaAction(bestMetaAction);
			
			returnCommand = m_currentMetaAction.GetNextCommand(m_currentMetaActionStep);
			m_currentMetaActionStep++;
		}
		
		if (!mctsDone)
			doSearch(a_timeDue - 40 + m_par.MCTS_TIME_LIMIT);
		
		return returnCommand;
	}
	
	protected void enterPanicMode()
	{
		if (m_par.CONSOLE_OUTPUT) System.out.println("PANIC!!!");
		m_par = m_panicPar;
		m_evaluator.m_par = m_panicPar;
	}
	
	protected void leavePanicMode()
	{
		if (m_par.CONSOLE_OUTPUT) System.out.println("PANIC over!");
		m_par = m_normalPar;
		m_evaluator.m_par = m_normalPar;
	}
	
	private void SetNewMetaAction(MetaAction a)
	{
		m_currentMetaAction = a;
		m_currentMetaActionStep = 0;
	}
		
	protected void ApplyMetaAction(Game gameState, MetaAction a, ExtraPlayoutInfo extraInfo)
	{
		int currentMetaActionStep = 0;

		Vector2d startPos = gameState.getShip().s.copy();
		Vector2d startVel = gameState.getShip().v.copy();
		
		while (currentMetaActionStep < a.getNumSteps() && !gameState.isEnded())
		{
			gameState.tick(a.GetNextCommand(currentMetaActionStep));
			currentMetaActionStep++;
			
			if (extraInfo != null)
			{
				if (gameState.getShip().getCollLastStep() && gameState.getShip().getInvulnerableTime() == 0)
				{
					switch (gameState.getShip().getLastCollisionType())
					{
					case PTSPConstants.DAMAGE_COLLISION_TYPE:
						extraInfo.m_collisionDamage += PTSPConstants.DAMAGE_DAMAGE_COLLISION;
						break;
						
					case PTSPConstants.NORMAL_COLLISION_TYPE:
						extraInfo.m_collisionDamage += PTSPConstants.DAMAGE_NORMAL_COLLISION;
						break;
						
					case PTSPConstants.ELASTIC_COLLISION_TYPE:
						// Do nothing
						break;
					}
				}
				
				if (!Double.isNaN(extraInfo.m_distanceTravelled))
					extraInfo.m_distanceTravelled += gameState.getShip().s.copy().subtract(gameState.getShip().ps).mag();
			}
		}
		
		// Check for stalling
		if (extraInfo != null)
		{
			if (!gameState.isEnded() && startVel.mag() < 1.0 && gameState.getShip().v.mag() < 1.0 && startPos.sqDist(gameState.getShip().s) < 1.0)
				extraInfo.m_lastMoveWasStalling = true;
			else
				extraInfo.m_lastMoveWasStalling = false;
		}
	}
	
	private List<MetaAction> GenerateActionList()
	{
		List<MetaAction> actions = new ArrayList<MetaAction>();
		
		/*for (int r = 0; r <= ROTATION_SUBDIVISIONS; r++)
		{
			for (int d = -1; d< 2; d+= 2)
			{
				if (r == 0 && d == 1 || r == ROTATION_SUBDIVISIONS && d == 1)
					continue;
				
				int direction = d;
				if (r == 0)
					direction = 0;
				
				int numSteps = r * (TURN_STEPS / ROTATION_SUBDIVISIONS);
				if (numSteps == 0)
					numSteps = MCTS_STRAIGHT_MULTIPLIER * TURN_STEPS / ROTATION_SUBDIVISIONS;
				
				actions.add(new MetaAction(numSteps,direction,true));
				if (!ALWAYS_THRUSTING)
					actions.add(new MetaAction(numSteps,direction,false));
			}
		}*/
		
		actions.add(new MetaAction(m_par.ROTATION_SUBDIVISIONS,									-1, true));
		actions.add(new MetaAction(m_par.MCTS_STRAIGHT_MULTIPLIER * m_par.ROTATION_SUBDIVISIONS, 0, true));
		actions.add(new MetaAction(m_par.ROTATION_SUBDIVISIONS,									 1, true));
		
		if (!m_par.ALWAYS_THRUSTING)
		{
			actions.add(new MetaAction(m_par.ROTATION_SUBDIVISIONS,									-1, false));
			actions.add(new MetaAction(m_par.MCTS_STRAIGHT_MULTIPLIER * m_par.ROTATION_SUBDIVISIONS, 0, false));
			actions.add(new MetaAction(m_par.ROTATION_SUBDIVISIONS,									 1, false));
		}
		
		if (m_par.PWM_THRUST)
		{
			actions.add(new MetaAction(m_par.ROTATION_SUBDIVISIONS,									-1, m_par.PWM_THRUST_DUTY, m_par.PWM_THRUST_CYCLE));
			actions.add(new MetaAction(m_par.MCTS_STRAIGHT_MULTIPLIER * m_par.ROTATION_SUBDIVISIONS, 0, m_par.PWM_THRUST_DUTY, m_par.PWM_THRUST_CYCLE));
			actions.add(new MetaAction(m_par.ROTATION_SUBDIVISIONS,									 1, m_par.PWM_THRUST_DUTY, m_par.PWM_THRUST_CYCLE));
		}
		
		return actions;
	}

	private double calculateClosestWaypointDistance(Game a_gameCopy)
    {
        double minDistance = Double.MAX_VALUE;
        for(Waypoint way: a_gameCopy.getWaypoints())
        {
            if(!way.isCollected())     //Only consider those not collected yet.
            {
                double fx = way.s.x-a_gameCopy.getShip().s.x, fy = way.s.y-a_gameCopy.getShip().s.y;
                double dist = Math.sqrt(fx*fx+fy*fy);
                if( dist < minDistance )
                {
                    //Keep the minimum distance.
                    minDistance = dist;
                }
            }
        }
        
        return minDistance;
    }
	
	protected double DoSimulation(Game leafGameState, int initialDepth, ExtraPlayoutInfo extraInfo)
	{	
		int currentDepth = initialDepth;
		while (!leafGameState.isEnded() && currentDepth < m_par.MCTS_PLAYOUT_LIMIT)
		{
			MetaAction action = null;
			
			if (m_par.SIMULATION_EPSILON >= 1.0 || rng.nextDouble() < m_par.SIMULATION_EPSILON)
			{
				action = m_actionList.get(rng.nextInt(m_actionList.size()));
			}
			else
			{
				double bestScore = 0;
				
				for (MetaAction a : m_actionList)
				{
					Game nextState = leafGameState.getCopy();
					ExtraPlayoutInfo nextExtraInfo = extraInfo.getCopy();
					ApplyMetaAction(nextState, a, nextExtraInfo);
					
					double score = m_evaluator.evaluate(nextState, nextExtraInfo, m_rootGameState, m_routePlanner);
					if (action == null || score > bestScore)
					{
						action = a;
						bestScore = score;
					}
				}
			}
			
			ApplyMetaAction(leafGameState, action, extraInfo);
			currentDepth+= 1;
		}
		
		return GetSimulationResult(leafGameState, extraInfo);
	}
	
	protected double GetSimulationResult(Game leafGameState, ExtraPlayoutInfo extraInfo)
	{
		if (leafGameState.getWaypointsLeft() == 0)
		{
			int nw = leafGameState.getNumWaypoints();
			int maxTime = nw * PTSPConstants.getStepsPerWaypoints(nw);
			//return (leafGameState.getWaypoints().size() * m_par.EVAL_WAYPOINT_BONUS * 2)-Math.min(9000,((3* leafGameState.getTotalTime())/1000));
			return nw * m_par.EVAL_WAYPOINT_BONUS + m_par.EVAL_TERMINAL_TIME_BONUS * (maxTime - leafGameState.getTotalTime())
					+ m_par.EVAL_TERMINAL_DAMAGE_BONUS * (PTSPConstants.MAX_DAMAGE - leafGameState.getShip().getDamage())
					+ m_par.EVAL_TERMINAL_FUEL_BONUS * leafGameState.getShip().getRemainingFuel();
		}
		else
			return m_evaluator.evaluate(leafGameState, extraInfo, m_rootGameState, m_routePlanner);
	}
	
	BufferedImage m_rewardImage = null;
	int m_rewardImageLastWaypointCount = -1;
	
	class RewardImageUpdater extends Thread
	{
		Game copy;
		
		public RewardImageUpdater(Game game)
		{
			copy = game;
		}
		
		@Override
		public void run() 
		{
			ExtraPlayoutInfo extraInfo = new ExtraPlayoutInfo();
			
			for (int wh = 8; wh > 0; wh /= 2)
			{			
				double[][] fitnessValues = new double[copy.getMapSize().width / wh + 1][copy.getMapSize().height / wh + 1];
				double minFitness = Double.POSITIVE_INFINITY;
				double maxFitness = Double.NEGATIVE_INFINITY;
				
				for (int x=0; x<copy.getMapSize().width; x+=wh)
				{
					for (int y=0; y<copy.getMapSize().height; y+=wh)
					{
						if (!copy.getMap().isOutsideBounds(x, y) && !copy.getMap().isObstacle(x, y))
						{
							copy.getShip().s.x = x;
							copy.getShip().s.y = y;
							copy.getShip().v.x = copy.getShip().v.y = 0;
							
							double fitnessValue = GetSimulationResult(copy, extraInfo);
							fitnessValues[x/wh][y/wh] = fitnessValue;
							if (fitnessValue < minFitness) minFitness = fitnessValue;
							if (fitnessValue > maxFitness) maxFitness = fitnessValue;
						}
					}
				}
					
				for (int x=0; x<copy.getMapSize().width; x+=wh)
				{
					for (int y=0; y<copy.getMapSize().height; y+=wh)
					{
						if (!copy.getMap().isOutsideBounds(x, y) && !copy.getMap().isObstacle(x, y))
						{
							double fitness = fitnessValues[x/wh][y/wh];
							
							double v = (fitness - minFitness) / (maxFitness - minFitness);
							int rgb = 0x20000000 + Color.HSBtoRGB((float)(0.8*v), 1.0f, 0.5f);
							
							if (Math.abs(v - (int)Math.round(v*20)/20.0) < 0.001)
								rgb = 0x80FFFFFF;

							for (int x2=x; x2<x+wh && x2<copy.getMapSize().width; x2++)
								for (int y2=y; y2<y+wh && y2<copy.getMapSize().height; y2++)
								m_rewardImage.setRGB(x2, y2, rgb);
						}
					}
				}
			}
		}
	}
	
	RewardImageUpdater m_rewardImageUpdater = null;
	
	@Override
	public void paint(Graphics2D g)
	{
		Game copy = m_rootGameState.getCopy();
		
		if (m_rewardImage == null)
			m_rewardImage = new BufferedImage(copy.getMapSize().width, copy.getMapSize().height, BufferedImage.TYPE_INT_ARGB);

		if (copy.getWaypointsVisited() + copy.getFuelTanksCollected() != m_rewardImageLastWaypointCount && copy.getWaypointsLeft() > 0)
		{
			if (m_rewardImageUpdater != null)
				m_rewardImageUpdater.stop();
			
			m_rewardImageUpdater = new RewardImageUpdater(copy);
			m_rewardImageUpdater.start();
			
			m_rewardImageLastWaypointCount = m_rootGameState.getWaypointsVisited() + m_rootGameState.getFuelTanksCollected();
		}
		
		//synchronized(m_rewardImage)
		{
			g.drawImage(m_rewardImage, 0, 0, null);
		}
		
		// Draw route
		m_routePlanner.draw(g, copy);
	}
}
