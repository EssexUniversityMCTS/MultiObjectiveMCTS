package controllers.mctsdriver;

import framework.core.Waypoint;

public class Parameters implements Cloneable
{
	public Parameters clone()
	{
		try
		{
			return (Parameters)super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			// This should never happen
			e.printStackTrace();
			return null;
		}
	}
	
	private double mapParameter(double min, double max, double val)
	{
		return ((val / 10.0) * (max-min)) + min;
	}
	
	static double[] ParseParameters(String paramString)
	{
    	String[] paramStrings = paramString.split(" ");
    	double[] params = new double[paramStrings.length];
    	for (int i=0;i<paramStrings.length;i++)
    	{
    		params[i] = Double.parseDouble(paramStrings[i]);
    	}
    	
    	return params;
	}
	
	public Parameters()
	{
    	this(ParseParameters("2.5382730205860424 1.868641531357007 3.160396253322351 6.624794572758817 1.5642214565731265 8.725484381981378 0.4863530069908529 4.86867757355858 5.748630300591624 3.5005323614319033 3.8338979443263215 16.197522369053626 3.970907850504408 16.177900635759766 0.3632048006283749 11.72888471543916 3.3020100742735212 2.5741532118714825"));
		
    	// Parameters tuned using the scoring system from the competition:
    	//this(ParseParameters("0.735278603998225 12.576687455122457 2.7742459899787604 7.603812911752163 4.9999050881140015 18.674467283467333 15.432310312781954 4.524176574080812 4.367988042060501 4.388650210031916 4.13169103447325 25.332384763579977 7.211278303628227 22.04686327661708 1.1494721322064336 -1.5592342701770523 5.88527171346778 4.235487403628629"));
	}
	
	public Parameters(double[] params)
	{
		int x = 0;
		
		// gamma_lava
		FLOODFILL_LAVA_WEIGHT = mapParameter(0,2,params[x]);x++;
		if (FLOODFILL_LAVA_WEIGHT < 0)
			FLOODFILL_LAVA_WEIGHT = 0;
		
		// beta_exponent
		EVAL_ROUTE_EDGE_WEIGHT_EXPONENT = mapParameter(1, 2, params[x]);x++;
		// beta_angle
		ROUTE_ANGLE_WEIGHT = mapParameter(0,500,params[x]);x++;
		// beta_directness
		ROUTE_DIRECTNESS_WEIGHT = mapParameter(0,500, params[x]);x++;
		// beta_include-fuel
	    INCLUDE_FUEL_TANKS_IN_ROUTE = params[x] > 5;x++;
	    // beta_fuel
	    EVAL_ROUTE_FUEL_TANK_BONUS = mapParameter(0,500,params[x]);x++;
	    // beta_consequtive-fuel
	    EVAL_ROUTE_CONSECUTIVE_FUEL_TANKS_PENALTY = mapParameter(0,1000,params[x]);x++;
	    // beta_initial-fuel
	    EVAL_ROUTE_FIRST_FUEL_TANK_PENALTY = mapParameter(0, 1000,params[x]);x++;
	    
	    // T
	    ROTATION_SUBDIVISIONS = (int)Math.round(mapParameter(1,20,params[x]));x++;
	    if (ROTATION_SUBDIVISIONS < 1)
	    	ROTATION_SUBDIVISIONS = 1;
	    // d
	    MCTS_PLAYOUT_LIMIT = (int)Math.round(mapParameter(1, 15, params[x]));x++;
	    // C
	    MCTS_EXPLORATION = mapParameter(0, 2, params[x]);x++;
	    if (MCTS_EXPLORATION < 0)
	    	MCTS_EXPLORATION = 0;
	    /*// alpha_panic-threshold
	    PANIC_MODE_LOCAL_MAXIMUM_THRESHOLD = mapParameter(0,1,params[x]);x++;*/
	    
	    // alpha_waypoints
	    EVAL_WAYPOINT_BONUS = mapParameter(0,1,params[x]);x++;
	    // alpha_early-waypoint
	    EVAL_EARLY_WAYPOINT_WEIGHT = mapParameter(-1,1,params[x]);x++;
	    // alpha_distance
	    EVAL_NAV_WEIGHT = mapParameter(0,1,params[x]);x++;
	    /*// alpha_pwm_enable
	    PWM_THRUST = params[x] > 5;x++;
	    // alpha_pwm-duty
	    PWM_THRUST_DUTY = (int)Math.round(mapParameter(1,7,params[x]));x++;
	    if (PWM_THRUST_DUTY < 1)
	    	PWM_THRUST_DUTY = 1;
	    // alpha_pwm-cycle
	    PWM_THRUST_CYCLE = (int)Math.round(mapParameter(2,8,params[x]));x++;
	    if (PWM_THRUST_CYCLE < 2)
	    	PWM_THRUST_CYCLE = 2;*/
	    // alpha_fuel
	    EVAL_FUEL_PENALTY = -1 * mapParameter(0,0.01,params[x]);x++;
	    // alpha_fuel-tank
	    EVAL_FUEL_TANK_BONUS = mapParameter(0,1,params[x]);x++;
	    // alpha_damage
	    EVAL_DAMAGE_PENALTY = -1 * mapParameter(0,0.01,params[x]);x++;
	    // alpha_collision
	    EVAL_COLLISION_DAMAGE_PENALTY = -1 * mapParameter(0,0.5,params[x]);x++;
	}

	public boolean CONSOLE_OUTPUT = false;
	
	public double ROUTE_ANGLE_WEIGHT = 80;
	public double ROUTE_DIRECTNESS_WEIGHT = 150;
	
	public int DISTANCE_MAP_RESOLUTION = 2;
	
	/** The amount of time in ms for which the driver searches per turn. This should not be tuned. */
	public long MCTS_TIME_LIMIT = 38;

	/** If true, the driver waits for route planning to finish before setting off. */
	public boolean WAIT_FOR_ROUTEPLANNER = false;
	
	/** If WAIT_FOR_ROUTEPLANNER is false, the amount of time in ms for which the driver searches per turn
	 *  while the route planner is active. This balances the time budget between the route planner and the driver.
	 *  Unlike MCTS_TIME_LIMIT, this should be tuned. */
	public long MCTS_TIME_LIMIT_WHILE_ROUTEPLANNING = 20;
	
	/** UCB exploration constant for the driver. */ 
	public double MCTS_EXPLORATION = 1.0;
	
	/** Maximum depth of the MCTS tree. */
	public int MCTS_EXPANSION_LIMIT = 999;
	
	/** Maximum depth of the MCTS playout (tree + simulation). */
	public int MCTS_PLAYOUT_LIMIT = 8;
	
	/** Macro-action size. */
	public int ROTATION_SUBDIVISIONS = 15;
	
	/** Multiplier for macro-action size of non-turning actions. */
	public int MCTS_STRAIGHT_MULTIPLIER = 1;

	/** If true, only moves which apply thrust are present in the tree. */
	public boolean ALWAYS_THRUSTING = false;
	
	/** Whether to include partial thrust actions (using pulse width modulation i.e. duty cycles) */
	public boolean PWM_THRUST = false;

	public int PWM_THRUST_DUTY = 1;
	public int PWM_THRUST_CYCLE = 2;
	
	/** Whether to prune moves that do not significantly change the position or velocity of the ship. */
	public boolean PRUNE_STALLING_MOVES = false;
	
	/** If (distance to next waypoint) / (time left) > PANIC_MODE_RATIO, the driver switches to an alternate (greedier) set of parameters */
	public double PANIC_MODE_RATIO = Double.POSITIVE_INFINITY;
	
	/** If (steps into current macro-action) / (macro-action size) > PANIC_MODE_DECISION_TIME_PORTION
	 *  and (average reward at root of MCTS tree) <= (evaluation of root state), 
	 *  the driver switches to an alternate (greedier) set of parameters */
	public double PANIC_MODE_DECISION_TIME_PORTION = 0.75;
	
	public double PANIC_MODE_LOCAL_MAXIMUM_THRESHOLD = 0.1;
	
	/** Weight of the evaluation component rewarding navigation towards the next waypoint. */
	public double EVAL_NAV_WEIGHT = 0.75;

	/** Evaluation bonus for collecting a waypoint on the route. */
	public double EVAL_WAYPOINT_BONUS = 1.00;
	
	/** Multiplier for EVAL_WAYPOINT_BONUS for waypoints collected out of route order.
	 *  1.0 = same reward as waypoints on the route
	 *  0.0 = no reward for early collection
	 *  < 0 = penalty for early collection
	 */
	public double EVAL_EARLY_WAYPOINT_WEIGHT = -1;
	
	/** Evaluation penalty for exceeding the limit of time steps between waypoints. */
	public double EVAL_TIMEOUT_PENALTY = -1;

	/** Discount factor for MCTS backpropagation. */
	public double MCTS_DISCOUNT_FACTOR = 1.0;
	
	/** For epsilon-greedy simulations. Set to >= 1 to use purely random simulations. */
	public double SIMULATION_EPSILON = 1.0;

	/** TODO description */
	public double EVAL_GREEDY_WEIGHT = 0.0000;//1;

	/** TODO description */
	public double EVAL_SPEED_WEIGHT = 0; //0.25;

	/** TODO description */
	public double EVAL_SPEED_PENALTY = 0.0;
	
	/** Weight of each unit of time in the terminal evaluation */
	public double EVAL_TERMINAL_TIME_BONUS = 0.1;

	/** Weight of each unit of damage in the terminal evaluation */
	public double EVAL_TERMINAL_DAMAGE_BONUS = 0.1;

	/** Weight of each unit of fuel in the terminal evaluation */
	public double EVAL_TERMINAL_FUEL_BONUS = 0.1;
	
	public double EVAL_DAMAGE_PENALTY = -0.002;
	public double EVAL_COLLISION_DAMAGE_PENALTY = -0.3;
	public double EVAL_FUEL_PENALTY = -0.001;
	public double EVAL_FUEL_TANK_BONUS = 0.2;
	
	/** Edge weights are raised to this power. Values > 1 penalise especially long edges. */ 
	public double EVAL_ROUTE_EDGE_WEIGHT_EXPONENT = 1.5;
	
	/** Additional weight for distances through lava in the distance maps.
	 *  Warning: setting this to a value other than 0 can cause the flood filler to perform slowly. */
	public double FLOODFILL_LAVA_WEIGHT = 0.5;
	
	/** Portions of edges that pass through lava have their length multiplied by this. */
	public double ROUTE_EDGE_LAVA_WEIGHT = 1;
	
	/** Whether to include fuel tanks as optional waypoints in the route planning stage */
	public boolean INCLUDE_FUEL_TANKS_IN_ROUTE = true;
	
	/** Bonus in route planner for the route collecting a fuel tank */
	public double EVAL_ROUTE_FUEL_TANK_BONUS = 200;
	
	public double EVAL_ROUTE_FIRST_FUEL_TANK_PENALTY = 1000;
	public double EVAL_ROUTE_CONSECUTIVE_FUEL_TANKS_PENALTY = 1000;
	
	/** TODO description */
	public int DFS_SEARCH_DEPTH = 4;
	
	public boolean ASTAR_USE_HASH_MAP = true;
	public double ASTAR_HASH_MAP_POSITION_RESOLUTION = 1;
	public double ASTAR_HASH_MAP_VELOCITY_RESOLUTION = 0.25;
	public double ASTAR_AVERAGE_SPEED_ESTIMATE = 0.4;
	public double ASTAR_OPEN_LIST_EPSILON = 0; //0.01;
	public double ASTAR_OPEN_LIST_EPSILON_WITH_WINNING_LINE = 0.01;
	public double ASTAR_TIME_WEIGHT = 1;
	public double ASTAR_FUEL_WEIGHT = 1;
	public double ASTAR_DAMAGE_WEIGHT = 1;
	
	public static String DEFAULT_CONTROLLER_CLASS_NAME = "controllers.mctsdriver.MctsDriverController";
	
	/** Name of the route planner class */
	public String PLANNER_CLASS_NAME = "controllers.mctsdriver.FloodFillTspPlanner";
	
	/** Name of the state evaluator class */
	public String EVALUATOR_CLASS_NAME = "controllers.mctsdriver.SteppingEvaluator"; 

}
