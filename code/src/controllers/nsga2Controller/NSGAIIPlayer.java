package controllers.nsga2Controller;

import controllers.utils.OrderedSolutionList;
import controllers.utils.ParetoArchive;
import controllers.utils.Utils;
import framework.core.Game;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;

import java.util.LinkedList;
import java.util.NavigableSet;
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
    int m_populationSize = 50;//20;
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
        MOPTSP.reset();
        MOPTSP.m_currentState = a_gameState;
        MOPTSP.m_heuristic = m_heuristic;

        NondominatedPopulation result = new Executor()
                .withProblemClass(MOPTSP.class)
                .withAlgorithm(m_algorithmName)
                .withMaxEvaluations(numEvalsMacro)
                .withProperty("populationSize", m_populationSize)
                .run();

        //Choose one of the solutions found, to determine which move to make.

        /*Solution chosen = maxSolutionDist(result, a_gameState);
        int moves[] = EncodingUtils.getInt(chosen);
        int action = moves[0];       */

        int action = maxSolution(NSGAIIParameters.targetWeights);


        long spent = System.currentTimeMillis() - now;
        //System.out.println("Spent: " + spent);
        return action;
    }

    public int maxSolution(double[] weights)
    {
        int selected = -1;
        double[][] bounds =  m_heuristic.getValueBounds();

        double bestValue = -Double.MAX_VALUE;
        OrderedSolutionList myPA = MOPTSP.m_pa.m_members;
        int numMembers =  myPA.size();

        for(int i = 0; i < numMembers; ++i)
        {
            double[] thisRes = myPA.get(i).m_data;
            double val = 0.0;
            for(int t = 0; t < weights.length; ++t)
            {
                double v =  Utils.normalise(thisRes[t], bounds[t][0], bounds[t][1]);
                val += v*weights[t];
            }

            if(val > bestValue) {
                bestValue = val;
                selected = i;
            }
        }

        if(selected == -1)
        {
            //System.out.println(" ********************* SELECTED -1, myPA.size(): " + myPA.size() + " ***************");
            return -1;
        }

        double selectedTarget[] = myPA.get(selected).m_data;
        NavigableSet<Integer> navSet = MOPTSP.valueRoute.navigableKeySet();
        for(Integer key : navSet)
        {
            LinkedList<controllers.utils.Solution> resFromThisChild = MOPTSP.valueRoute.get(key);

            for(int i =0; i < resFromThisChild.size(); ++i)
            {
                double[] sol = resFromThisChild.get(i).m_data;
                //System.out.println("PA point " + key + ":" + i + ": " + sol[0] + ", " + sol[1] + ", nVis: " + children[key].nVisits);

                if(sol.length == 3 && sol[0] == selectedTarget[0] && sol[1] == selectedTarget[1] && sol[2] == selectedTarget[2])
                //if(sol[0] == selectedTarget[0] && sol[1] == selectedTarget[1])
                {
                    //System.out.println("SELECTED-3: " + children[key].nVisits + "," + sol[0] + "," + sol[1] + ": " + key);
                    return key;
                }else if(sol.length == 2 && sol[0] == selectedTarget[0] && sol[1] == selectedTarget[1])
                {
                    //System.out.println("SELECTED-2: " + children[key].nVisits + "," + sol[0] + "," + sol[1] + ": " + key);
                    return key;
                }
            }
        }
        return selected;
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
