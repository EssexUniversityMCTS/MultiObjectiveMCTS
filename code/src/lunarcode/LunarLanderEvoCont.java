package lunarcode;

import fr.inria.optimization.cmaes.CMAEvolutionStrategy;
import fr.inria.optimization.cmaes.fitness.IObjectiveFunction;
import framework.core.Game;
import framework.utils.ElapsedCpuTimer;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 17/03/14
 * Time: 13:57
 */
public class LunarLanderEvoCont {

    private double[] solution;
    private int s_i = 0;
    private int macro_counter = 0;

    public double[] getAction(Game copy, ElapsedCpuTimer ect) {

        LunarLanderObjective fitfun = new LunarLanderObjective((LunarGame) copy.getCopy());

//        if(solution!=null) {
//            double[] tbr = fitfun.convertInputs(solution[s_i],solution[s_i+1]);
//            macro_counter+=1;
//            if(macro_counter >= fitfun.MACRO_LENGTH) {
//
//                s_i+=2;
//                macro_counter = 0;
//            }
//            return tbr;
//        }

        CMAEvolutionStrategy cma = new CMAEvolutionStrategy();
        cma.setDimension(9); // overwrite some loaded properties
        //cma.parameters.setPopulationSize(10);
        cma.setInitialX(0.5); // in each dimension, also setTypicalX can be used
        cma.setInitialStandardDeviation(0.2);
        cma.options.verbosity = -1;

        double[] fitness = cma.init();

        while (!ect.exceededMaxTime()) {

            double[][] pop = cma.samplePopulation(); // get a new population of solutions
            //System.out.println("pop = " + Arrays.toString(pop[0]));
            for(int i = 0; i < pop.length; ++i) {    // for each candidate solution i

                // a simple way to handle constraints that define a convex feasible domain
                // (like box constraints, i.e. variable boundaries) via "blind re-sampling"
                // assumes that the feasible domain is convex, the optimum is
                while (!fitfun.isFeasible(pop[i]))     //   not located on (or very close to) the domain boundary,

                    pop[i] = cma.resampleSingle(i);    //   initialX is feasible and initialStandardDeviations are
                //   sufficiently small to prevent quasi-infinite looping here
                // compute fitness/objective value
                fitness[i] = fitfun.valueOf(pop[i]); // fitfun.valueOf() is to be minimized
            }
            cma.updateDistribution(fitness);         // pass fitness array to update search distribution


        }

        double[] best = cma.getBestX().clone();


        //System.out.println("best" + Arrays.toString(best));
        //System.out.println(cma.getBestFunctionValue() + " " + cma.getCountEval()) ;
        //System.exit(0);
        double[] tbr = fitfun.convertInputs(best[0],best[1]);
        
        if(cma.getBestFunctionValue() < -LunarLanderObjective.WIN_BASE) {
            //LunarLanderObjective.MACRO_LENGTH -=1;

            solution = best;
            s_i = 0;
            macro_counter = 1;

        }
        //System.out.println("LunarLanderObjective.MACRO_LENGTH = " + LunarLanderObjective.MACRO_LENGTH);
        //System.out.println(cma.getBestFunctionValue());
        //LunarGame g = (LunarGame) copy.getCopy();
        //g.tickCont(tbr[0],tbr[1]);
        //System.out.println(g.getShip().v.mag() + " sdfdf")      ;


        //System.out.println("tbtr" + Arrays.toString(tbr));

        return tbr;

    }

}
