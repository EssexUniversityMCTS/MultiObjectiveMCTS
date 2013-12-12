package framework;

import controllers.utils.Utils;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 11/12/13
 * Time: 19:55
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Population
{
    int[][] population;
    int []elite;
    int numIndividuals;
    int numValues;
    int genomeLength;
    Random rnd;
    double mutProb = 0.2;
    double bestResult[];
    int indexBest;

    public Population(int numIndividuals, int genomeLength, int numValues)
    {
        bestResult = new double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        rnd = new Random();
        this.genomeLength = genomeLength;
        this.numValues = numValues;
        this.numIndividuals = numIndividuals;
        population = new int [numIndividuals][genomeLength];
        elite = new int[genomeLength];
        indexBest=-1;
    }

    public void initPopulationRnd()
    {
        for(int i = 0; i < numIndividuals; ++i)
        {
            population[i] = randomIndividual();
        }
    }

    public void initPopulationRndBiased()
    {
        for(int i = 0; i < 3; ++i)
        {
            population[i] = createIndividualFrom(i);
        }

        for(int i = 3; i < 6; ++i)
        {
            population[i] = mutateIndividual(i-3);
        }

        for(int i = 6; i < numIndividuals; ++i)
        {
            population[i] = randomIndividual();
        }
    }

    private int[] randomIndividual()
    {
        int[] newGuy = new int[genomeLength];
        for(int i = 0; i < genomeLength; ++i)
        {
            newGuy[i] = rnd.nextInt(numValues);
        }
        return newGuy;
    }

    private int[] createIndividualFrom(int baseValue)
    {
        int[] newGuy = new int[genomeLength];
        for(int i = 0; i < genomeLength; ++i)
        {
            newGuy[i] = baseValue;
        }
        return newGuy;
    }

    private int[] mutateIndividual(int[] base)
    {
        int[] newGuy = new int[genomeLength];
        for(int i = 0; i < genomeLength; ++i)
        {
            if(rnd.nextDouble() < mutProb)
            {
                int n = rnd.nextInt(numValues);
                while(n ==  base[i])
                    n = rnd.nextInt(numValues);
                newGuy[i] = n;
            }
            else
                newGuy[i] = base[i];
        }
        return newGuy;
    }

    private int[] mutateIndividual(int baseValue)
    {
        int[] newGuy = new int[genomeLength];
        for(int i = 0; i < genomeLength; ++i)
        {
            if(rnd.nextDouble() < mutProb)
            {
                int n = rnd.nextInt(numValues);
                while(n ==  baseValue)
                    n = rnd.nextInt(numValues);
                newGuy[i] = n;
            }
            else
                newGuy[i] = baseValue;
        }
        return newGuy;
    }

    public void evaluate(int trials)
    {
        for(int i = 0; i < numIndividuals; ++i)
        {
            if(i != indexBest)
            {
                int genes[] = population[i];
                for(int k = 0; k < genes.length; ++k)
                {
                    for(int j = 0; j < 3; ++j)
                    {
                        EvoExec.currentWeights[k][j] = EvoExec.genes[genes[k]][j];
                    }
                }

                double res[] = EvoExec.evaluate(trials, genes, i);
                //double res[] = EvoExec.evaluateVisual();
                //double res[] = new double[]{rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble()};

                int dominance = Utils.dominates(res, bestResult);
                if(dominance == 1) //bestResult dominates res... but we are MINIMIZING!
                {
                    System.arraycopy(res, 0, bestResult, 0, bestResult.length);
                    indexBest = i;
                }
            }else{
                System.err.print( EvoExec.generation + " " + indexBest + " ");
                for(int idxBest = 0; idxBest < population[indexBest].length; ++idxBest)
                {
                    System.err.print(population[indexBest][idxBest]);
                }
                System.err.format(". Fitness: %.3f, %.3f, %.3f\n", bestResult[0],bestResult[1],bestResult[2]);
            }
        }
    }

    public void advance()
    {
        for(int i = 0; i < numIndividuals/2; ++i)
        {
            if(i!=indexBest)
            {
                population[i] = mutateIndividual(population[indexBest]);
            }
        }

        for(int i =  numIndividuals/2; i < numIndividuals; ++i)
        {
            if(i!=indexBest)
            {
                population[i] = randomIndividual();
            }
        }
    }


}
