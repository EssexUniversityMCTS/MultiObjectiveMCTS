package framework;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.problem.AbstractProblem;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 11/12/13
 * Time: 12:02
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class MOPTSPWeight extends AbstractProblem
{
    public static int trials = 5;
    public static int individualCounter = 0;

    public MOPTSPWeight()
    {
        super(EvoExec.genomeLength, 3);
    }

    public void evaluate(Solution solution)
    {
        int genes[] = EncodingUtils.getInt(solution);
        for(int i = 0; i < genes.length; ++i)
        {
            for(int j = 0; j < numberOfObjectives; ++j)
            {
                EvoExec.currentWeights[i][j] = EvoExec.genes[genes[i]][j];
            }

        }

        //System.out.println(genes[0]+","+genes[1]+","+genes[2]+","+genes[3]+","+genes[4]+","+genes[5]+",...");
        double result[] = EvoExec.evaluate(trials, null, -1);
        //double result[] = new double[]{new Random().nextDouble(), new Random().nextDouble(), new Random().nextDouble()};

        solution.setObjectives(result);
    }

    public Solution newSolution()
    {
        Solution solution = new Solution(numberOfVariables, numberOfObjectives);

        if(individualCounter == 0)
        {
            for(int i = 0; i < numberOfVariables; ++i)
            {
                solution.setVariable(i, new RealVariable(0,0,6));
            }
        }else if(individualCounter == 1)
        {
            for(int i = 0; i < numberOfVariables; ++i)
            {
                solution.setVariable(i, new RealVariable(1,0,6));
            }
        }else if(individualCounter == 2)
        {
            for(int i = 0; i < numberOfVariables; ++i)
            {
                solution.setVariable(i, new RealVariable(3,0,6));
            }
        }else
        {
            for(int i = 0; i < numberOfVariables; ++i)
            {
                solution.setVariable(i, EncodingUtils.newInt(0, 6));
            }
        }

        individualCounter++;
        return solution;
    }
}
