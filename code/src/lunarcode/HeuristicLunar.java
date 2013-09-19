package lunarcode;

import controllers.ParetoMCTS.HeuristicMO;
import controllers.ParetoMCTS.ParetoMCTSController;
import controllers.ParetoMCTS.PlayoutInfo;
import framework.core.*;
import framework.graph.Path;
import framework.utils.Vector2d;

import java.util.LinkedList;

/**
 * Created by Samuel Roberts, 2013
 */
public class HeuristicLunar implements HeuristicMO {

    public int VALUE_CALLS;
    public static int MACRO_ACTION_LENGTH = 15;
    public static int ROLLOUT_DEPTH = 8;
    public static int NUM_OBJECTIVES = 2;
    public double targetWeights[];

    // maximum possible distance from target
    public double maxDistance;

    // bounds of the objective parameter values
    public double[][] bounds;
    // playout information store
    public PlayoutLunarInfo playoutInfo;


    public HeuristicLunar(double[] tWeights) {
        targetWeights = tWeights;

        // precalc the maximum distance you can ever be in the environment
        maxDistance = Math.sqrt(LunarParams.worldWidth*LunarParams.worldWidth + LunarParams.worldHeight+LunarParams.worldHeight);

        // initialise bounds
        // pairs of min-max values

        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // !!JUST EFFECTIVE USE OF DISTANCE AND FUEL FOR NOW!!
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        bounds = new double[NUM_OBJECTIVES][2];
        // set the bounds from 0 to 1, the values will be transformed in the actual scoring
        for(int i=0; i<bounds.length; i++) {
            bounds[i][0] = 0;
            bounds[i][1] = 1;
        }
    }


    // Full disclosure: not entirely sure how vital some of the features of the HeuristicPTSP class are,
    // so this might be a tiny bit cargo cult-y.
    public double[] value(LunarGame a_gameState)
    {
        VALUE_CALLS++;

        int playoutLength = MACRO_ACTION_LENGTH * ROLLOUT_DEPTH;
        if(a_gameState.landed && a_gameState.landedSuccessfully)
        {
            //In this case, the game as ended for sure: IT ENDS DURING THE
            //MACRO-ACTION BEING EXECUTED NOW.
            double superReward[] = new double[targetWeights.length];
            for(int i = 0; i < targetWeights.length; ++i)
                superReward[i]=5;  //just whatever.

            return superReward;
        }

        if((a_gameState.isEnded() && !a_gameState.landedSuccessfully))
        {
            double superPunishment[] = new double[targetWeights.length];
            for(int i = 0; i < targetWeights.length; ++i)
                superPunishment[i] = -2;
            //System.out.println("SUPER PUNISHMENT! "+matching+" "+a_gameState.getTotalTime());
            return superPunishment; // Game finished - game over.
        }

        // linear reward for approaching pad
        double distFromPad = a_gameState.getShip().s.dist(a_gameState.terrain.getNearestSafeLandingPoint(a_gameState.getShip().s));
        double distancePoints = (maxDistance - distFromPad)/maxDistance;

        double fuelPoints = 1 - ((LunarParams.startingFuel-a_gameState.getShip().getRemainingFuel()) / LunarParams.startingFuel);
//        double fuelPower = fuelPoints* ParetoMCTSController.FUEL_POWER_MULT + distancePoints*(1.0-ParetoMCTSController.FUEL_POWER_MULT);

        double allInOne = //distancePoints*0.33 + fuelPoints*0.33 + damagePoints*0.33;
                //distancePoints*0.1 + fuelPoints*0.3 + damagePoints*0.6;
                distancePoints*0.25 + fuelPoints*0.75;

        //double[] moScore = new double[]{distancePoints, damagePower};
        //double[] moScore = new double[]{distancePoints, fuelPower, damagePower};
        double[] moScore = new double[]{allInOne, allInOne};

        //double[] moScore = new double[]{damagePower, damagePower};

        return moScore;
    }


    // ignore these, these are just to make this legal although broken code
    public double[] value(Game a_gameState) { return new double[0]; }
    public boolean mustBePruned(Game a_newGameState, Game a_previousGameState) { return false; }



    public double[][] getValueBounds() {
        return bounds;
    }

    public boolean mustBePruned(LunarGame a_newGameState, LunarGame a_previousGameState) {

        boolean prune = false;
        // if the ship crashed, prune state
        if(a_newGameState.landed && !a_newGameState.landedSuccessfully) {
            prune = true;
        }
        // if the ship went too far upwards, prune state
        if(a_newGameState.getShip().s.y <= 0) {
            prune = true;
        }

        return prune;
    }

    public void setPlayoutInfo(PlayoutInfo a_pi) {
        playoutInfo = (PlayoutLunarInfo) a_pi;
    }

    public void addPlayoutInfo(int a_lastAction, Game a_gameState) {
        if(Controller.getThrust(a_lastAction)) playoutInfo.m_thurstCount++;
        playoutInfo.m_playoutHistory[playoutInfo.m_numMoves] = a_lastAction;
        playoutInfo.m_numMoves++;

        playoutInfo.m_landed = a_gameState.isEnded();
    }
}
