package controllers.nsga2Controller;

import controllers.utils.ParetoArchive;
import framework.core.Game;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;

import java.util.LinkedList;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 14/11/13
 * Time: 13:45
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class MOPTSP extends AbstractProblem
{
    public static int numEvaluations = 0;

    public static final int numObjectives = 3;
    public static final int numVariables = NSGAIIParameters.ROLLOUT_DEPTH;
    public static TreeMap<Integer, LinkedList<controllers.utils.Solution>> valueRoute;

    public static Game m_currentState;
    public static ParetoArchive m_pa;
    public static HeuristicMO m_heuristic;


    public MOPTSP()
    {
        super(numVariables,numObjectives);
    }

    public static void reset()
    {
        m_pa = new ParetoArchive();

        valueRoute = new TreeMap<Integer, LinkedList<controllers.utils.Solution>>();
        for(int i = 0; i < NSGAIIController.NUM_ACTIONS; ++i)
        {
            valueRoute.put(i,new LinkedList<controllers.utils.Solution>());
        }
    }

    public void evaluate(Solution solution) {

        int moves[] = EncodingUtils.getInt(solution);
        Game stateCopy = m_currentState.getCopy();
        int effectiveMoves = 0;
        int startingMove = moves[0];

        while(!stateCopy.isEnded() && effectiveMoves < moves.length)
        {
            int move = moves[effectiveMoves];
            advance(stateCopy,move);
            effectiveMoves++;
        }

        double objectives[] = m_heuristic.value(stateCopy);

        controllers.utils.Solution paSol = new controllers.utils.Solution(objectives);
        m_pa.add(paSol);
        valueRoute.get(startingMove).add(paSol);

        //MOEA MINIMIZES BY DEFAULT: We have to negate the objectives to get it right!
        double negObjectives[] = new double[]{-objectives[0], -objectives[1], -objectives[2]};

        solution.setObjectives(negObjectives);

    }

    public void advance(Game st, int action)
    {
        boolean gameOver = false;
        for(int singleAction = 0; !gameOver && singleAction < NSGAIIParameters.MACRO_ACTION_LENGTH; ++singleAction)
        {
            st.tick(action);
            gameOver = st.isEnded();
        }
    }

    public Solution newSolution() {
        Solution solution = new Solution(numberOfVariables, numberOfObjectives);

        for(int i = 0; i < numberOfVariables; ++i)
        {
            solution.setVariable(i, EncodingUtils.newInt(0,5));
        }

        return solution;
    }
}
