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

    public double[] getAction(Game copy, ElapsedCpuTimer ect) {

        CMAEvolutionStrategy cma = new CMAEvolutionStrategy();
        cma.setDimension(10); // overwrite some loaded properties
        cma.parameters.setPopulationSize(20);
        cma.setInitialX(0.5); // in each dimension, also setTypicalX can be used
        cma.setInitialStandardDeviation(0.3);
        LunarLanderObjective fitfun = new LunarLanderObjective((LunarGame) copy.getCopy());
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


//        for (int i = 0; i < best.length ; i++) {
//
//            if(best[i] < 0 ) {
//                best[i] = 0.0;
//            }
//
//            if(best[i] > 1 ) {
//                best[i] = 1.0;
//            }
//        }

        System.out.println("best" + Arrays.toString(best));
        System.out.println(cma.getBestFunctionValue() + " " + cma.getCountEval()) ;
        //System.exit(0);
        double[] tbr = fitfun.convertInputs(best[0],best[1]);
        //LunarGame g = (LunarGame) copy.getCopy();
        //g.tickCont(tbr[0],tbr[1]);
        //System.out.println(g.getShip().v.mag() + " sdfdf")      ;


        System.out.println("tbtr" + Arrays.toString(tbr));

        return tbr;

    }

}
