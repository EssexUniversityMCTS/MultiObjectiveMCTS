package controllers.nsga2Controller;

import controllers.utils.ParetoArchive;
import controllers.utils.Utils;
import framework.core.Game;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 14/11/13
 * Time: 13:35
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class NSGAIIPlayer
{

    int m_nEvals = 100; // number of evaluations per move
    int m_populationSize = 20;//20;
    Random m_rnd;
    HeuristicMO m_heuristic;
    String m_algorithmName;

    public NSGAIIPlayer(HeuristicMO a_h, Random a_rnd, int a_nEvals, String a_algorithm)
    {
        m_nEvals = a_nEvals;
        m_heuristic = a_h;
        m_rnd = a_rnd;
        m_algorithmName = a_algorithm;
        MOPTSP.m_pa = new ParetoArchive();
    }

    public void init()
    {

    }

    public int run(Game a_gameState, long a_timeDue, boolean a)
    {
        long now = System.currentTimeMillis();
        int numEvalsMacro = m_nEvals ;
        MOPTSP.m_pa = new ParetoArchive();
        MOPTSP.m_currentState = a_gameState;
        MOPTSP.m_heuristic = m_heuristic;

        NondominatedPopulation result = new Executor()
                .withProblemClass(MOPTSP.class)
                .withAlgorithm(m_algorithmName)
                .withMaxEvaluations(numEvalsMacro)
                .withProperty("populationSize", m_populationSize)
                .run();

        //Choose one of the solutions found, to determine which move to make.
        Solution chosen = maxSolutionDist(result, a_gameState);

        int moves[] = EncodingUtils.getInt(chosen);
        int action = moves[0];

        long spent = System.currentTimeMillis() - now;
        System.out.println("Spent: " + spent);
        return action;
    }

    //Selects a single solution, to make a move, according to the weights provided.
    public Solution maxSolutionDist(NondominatedPopulation result, Game a_gameState)
    {
        double[][] bounds =  m_heuristic.getValueBounds();
        double distance = Double.MAX_VALUE;
        Solution chosen = null;
        double[] targets = new double[]{NSGAIIParameters.targetWeights[0], NSGAIIParameters.targetWeights[1], NSGAIIParameters.targetWeights[2]};
        int i = 0;

        for (Solution solution : result) {
            double solutionValue[] = solution.getObjectives();
            double val0 = Utils.normalise(-solutionValue[0], bounds[0][0], bounds[0][1]); //REMEMBER to negate the solutions (MOEA minimizes)!
            double val1 = Utils.normalise(-solutionValue[1], bounds[1][0], bounds[1][1]);
            double val2 = Utils.normalise(-solutionValue[2], bounds[2][0], bounds[2][1]);
            double[] thisResNorm = new double[]{val0, val1, val2};
            double thisDist = Utils.distanceEuq(thisResNorm, targets);
            if(thisDist < distance)
            {
                distance = thisDist;
                chosen = solution;
            }
        }

        if(chosen == null)
            throw new RuntimeException("No solution found :(");

        //System.out.println("SOLUTION " + (i++) + ": " + chosen.getObjective(0) + "," + chosen.getObjective(1) + " moving " + chosen.getVariable(0));
        //System.out.println("######");

        return chosen;
    }

    public HeuristicMO getHeuristic(){return m_heuristic;}

    public void reset(){}

}
