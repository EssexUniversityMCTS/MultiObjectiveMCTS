package framework;

import controllers.utils.Utils;
import framework.core.Exec;
import framework.core.PTSPConstants;
import framework.core.PTSPView;
import framework.utils.JEasyFrame;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;

import java.util.Random;


/**
 * This class may be used to execute the game in timed or un-timed modes, with or without
 * visuals. Competitors should implement his controller in a subpackage of 'controllers'.
 * The skeleton classes are already provided. The package
 * structure should not be changed (although you may create sub-packages in these packages).
 */
@SuppressWarnings("unused")
public class EvoExec extends Exec
{
    public static double[][] genes;
    public static int nGenes; //number of different genes.
    public static int genomeLength;
    public static Random rnd;
    public static int generation;

    public static double[][] currentWeights;

    public static double[] evaluateVisual()
    {
        int delay = 5;
        //Get the game ready.
        if(!prepareGame())
            return null;

        //Indicate what are we running
        if(m_verbose) System.out.println("Running " + m_controllerName + " in map " + m_game.getMap().getFilename() + "...");

        m_view = new PTSPView(m_game, m_game.getMapSize(), m_game.getMap(), m_game.getShip(), m_controller);
        JEasyFrame frame = new JEasyFrame(m_view, "PTSP-Game: " + m_controllerName);


        while(!m_game.isEnded())
        {
            //When the result is expected:
            long then = System.currentTimeMillis();
            long due = then + PTSPConstants.ACTION_TIME_MS;

            //Advance the game.
            int actionToExecute = m_controller.getAction(m_game.getCopy(), due);

            //Exceeded time
            long now = System.currentTimeMillis();
            long spent = now - then;

            m_game.tick(actionToExecute);

            int remaining = (int) Math.max(0, delay - (now-then));//To adjust to the proper framerate.
            //Wait until de next cycle.
            waitStep(remaining);

            //And paint everything.
            m_view.repaint();
            if(m_game.getTotalTime() == 1)
                waitStep(m_warmUpTime);

        }

        if(m_verbose)
        {
            m_game.printResults();
        }

        return new double[]{m_game.getTotalTime(),
                            PTSPConstants.INITIAL_FUEL - m_game.getShip().getRemainingFuel(),
                            m_game.getShip().getDamage()};
        }


    public static double[] evaluate(int trials, int[] genes, int indIdx)
    {
        double totalResult[] = new double[3];
        int ntrials = trials;

        //For each trial...
        for(int i=0;i<ntrials;i++)
        {
            // ... create a new game.
            if(!prepareGame())
                continue;

            //PLay the game until the end.
            while(!m_game.isEnded())
            {
                //When the result is expected:
                long due = System.currentTimeMillis()+PTSPConstants.ACTION_TIME_MS;

                //Advance the game.
                int actionToExecute = m_controller.getAction(m_game.getCopy(), due);

                //Exceeded time
                long exceeded = System.currentTimeMillis() - due;

                m_game.tick(actionToExecute);
            }

            if(m_game.getWaypointsVisited() != 10)
            {
                ntrials++;
                System.out.println("Not succeeded.");
                printGenome(currentWeights);

                return new double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};

            }else
            {
                int consumedFuel = PTSPConstants.INITIAL_FUEL - m_game.getShip().getRemainingFuel();
                totalResult[0] += m_game.getTotalTime();
                totalResult[1] += consumedFuel;
                totalResult[2] += m_game.getShip().getDamage();
                System.err.format("Single Game: %d, %d, %d\n", m_game.getTotalTime(),consumedFuel,m_game.getShip().getDamage());
            }

            //And save the route, if requested:
            if(m_writeOutput)
                m_game.saveRoute();
        }

        totalResult[0] /= trials;
        totalResult[1] /= trials;
        totalResult[2] /= trials;
        System.err.print(generation + " " + indIdx + " ");
        for(int i = 0; i < genes.length; ++i)
        {
            System.err.print(genes[i]);
        }

        System.err.format(". Fitness: %.3f, %.3f, %.3f\n", totalResult[0],totalResult[1],totalResult[2]);
        //printGenome(currentWeights);

        return totalResult;
    }

    public static double[][] createNewRandomIndividual()
    {
        double [][] ind = new double[genomeLength][3];
        for(int i = 0; i<genomeLength; ++i)
        {
            int idx = rnd.nextInt(nGenes);
            double targets[] = new double[3];
            for(int j = 0; j < targets.length; ++j)
            {
                targets[j] = genes[idx][j];
            }
            ind[i] = targets;
        }

        return ind;
    }

    public static void printGenome(double [][]genome)
    {
        System.out.print("{");
        for(int i = 0; i<genomeLength; ++i)
        {
            System.out.print("{");
            for(int j = 0; j < genome[0].length; ++j)
            {
                System.out.format("%.3f,", genome[i][j]);
            }
            System.out.print("},");
        }
    }


    public static void evaluateMapRS(int trials, int evaluations)
    {
        double bestResult[] = new double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double bestWeights[][] = new double[genomeLength][3];

        for(int ev = 0; ev < evaluations; ++ev)
        {
            double[][] individual = createNewRandomIndividual();
            currentWeights = individual;
            double[] res = evaluate(trials, null, ev);

            int dominance = Utils.dominates(res, bestResult);
            if(dominance == 1) //bestResult dominates res... but we are MINIMIZING!
            {
                for(int i = 0; i<genomeLength; ++i)
                {
                    for(int j = 0; j < bestResult.length; ++j)
                    {
                        bestWeights[i][j] = individual[i][j];
                    }
                }

                System.arraycopy(res, 0, bestResult, 0, res.length);
            }

            System.out.format("Last: %.3f, %.3f, %.3f; Best: %.3f, %.3f, %.3f\n",
                    res[0],res[1],res[2],bestResult[0],bestResult[1],bestResult[2]);
        }

        System.out.println("------------");
        System.out.format("Final best: %.3f, %.3f, %.3f\n", bestResult[0], bestResult[1], bestResult[2]);
        printGenome(bestWeights);
        System.out.println("}");

    }

    public static void evaluateMapNSGAII(int trials, int evaluations)
    {
        currentWeights = new double[genomeLength][3];
        for(int i = 0; i < genomeLength; ++i)
            currentWeights[i] = new double[3];

        MOPTSPWeight.trials = trials;
        NondominatedPopulation result = new Executor()
                .withProblemClass(MOPTSPWeight.class)
                .withAlgorithm("NSGAII")
                .withMaxEvaluations(evaluations)
                .withProperty("populationSize", 10)
                .run();

        int i = 0;
        for (Solution solution : result) {
            double solutionValue[] = solution.getObjectives();
            System.out.format("Solution %d: %.3f, %.3f, %.3f\n", i++, solutionValue[0], solutionValue[1], solutionValue[2]);
        }

    }

    public static void evaluateMapStochHillCliim(int trials, int iterations, int numValues, int popSize)
    {                                     currentWeights = new double[genomeLength][3];
        for(int i = 0; i < genomeLength; ++i)
            currentWeights[i] = new double[3];

        Population pop = new Population(popSize, genomeLength, numValues);
        pop.initPopulationRndBiased();

        for(int it = 0; it < iterations; ++it)
        {
            generation = it;
            pop.evaluate(trials);
            pop.advance();

            double bestResult[] = pop.bestResult;
            int bestIndividual[] = pop.population[pop.indexBest];

            System.out.format("%d %.3f, %.3f, %.3f ", it, bestResult[0],bestResult[1],bestResult[2]);
            for(int j = 0; j < bestIndividual.length; ++j)
                System.out.print(bestIndividual[j]);
            System.out.println();
        }

    }

    public static void runGamesStats(int trials, int evaluations)
    {
        boolean moreMaps = true;

        for(int m = 0; moreMaps && m < m_mapNames.length; ++m)
        {
            //Run this map.
            //evaluateMapRS(trials, evaluations);
            //evaluateMapNSGAII(trials, evaluations);
            evaluateMapStochHillCliim(trials, evaluations, 3, 10);

            //Needed for advance maps.
            moreMaps = m_game.advanceMap();
        }

    }


    /**
     * The main method. Several options are listed - simply remove comments to use the option you want.
     *
     * @param args the command line arguments. Not needed in this class.
     */
    public static void main(String[] args)
    {
        String[] allMaps = new String[]{"maps/ptsp_map01.map","maps/ptsp_map02.map","maps/ptsp_map08.map",
                        "maps/ptsp_map19.map","maps/ptsp_map24.map","maps/ptsp_map35.map","maps/ptsp_map40.map",
                        "maps/ptsp_map45.map","maps/ptsp_map56.map","maps/ptsp_map61.map"}; //In an array, to play in mutiple maps with runGames().

        if(args.length > 0)
        {
            int mapIdx = Integer.parseInt(args[0]);
            m_mapNames = new String[]{allMaps[mapIdx]};
        }else
        {
            m_mapNames = new String[]{"maps/ptsp_map01.map"}; //Set here the name of the map to play in.
        }

        System.err.println("Running in map " + m_mapNames[0] + "...");

        //m_mapNames = new String[]{"maps/ptsp_map01.map","maps/ptsp_map02.map","maps/ptsp_map08.map",
        //        "maps/ptsp_map19.map","maps/ptsp_map24.map","maps/ptsp_map35.map","maps/ptsp_map40.map",
        //        "maps/ptsp_map45.map","maps/ptsp_map56.map","maps/ptsp_map61.map"}; //In an array, to play in mutiple maps with runGames().

        m_controllerName = "controllers.greedy.GreedyController"; //Set here the controller name.
        m_controllerName = "controllers.MacroRandomSearch.MacroRSController"; //Set here the controller name.
        m_controllerName = "controllers.ParetoMCTS.ParetoMCTSController"; //Set here the controller name.
//        m_controllerName = "controllers.mctsdriver.MctsDriverController"; //Set here the controller name.
        //m_controllerName = "controllers.singleMCTS.SingleMCTSController";
        //m_controllerName = "controllers.nsga2Controller.NSGAIIController";

        m_writeOutput = false; //Indicate if the actions must be saved to a file after the end of the game (the file name will be the current date and time)..
        m_verbose = true;

        nGenes = 7;
        int i = 0;

        genes = new double[nGenes][3];

        genes[i++] = new double[]{.33,.33,.33};
        //genes[i++] = new double[]{0,0,1};
        genes[i++] = new double[]{.1,.3,.6};
        genes[i++] = new double[]{.1,.6,.3};

        genes[i++] = new double[]{.3,.1,.6};
        //genes[i++] = new double[]{0,.5,.5};
        //genes[i++] = new double[]{0,1,0};
        genes[i++] = new double[]{.3,.6,.1};
        //genes[i++] = new double[]{.5,0,.5};
        //genes[i++] = new double[]{.5,.5,0};
        //genes[i++] = new double[]{1,0,0};
        genes[i++] = new double[]{.6,.1,.3};
        genes[i++] = new double[]{.6,.3,.1};

        genomeLength = 14;
        rnd = new Random();


        //m_writeOutput = true;
        m_verbose = false; //hides additional output. runGamesStats prints anyway.
        int numTrials=10;  int numEvaluations = 100;
        runGamesStats(numTrials, numEvaluations);

    }

}
