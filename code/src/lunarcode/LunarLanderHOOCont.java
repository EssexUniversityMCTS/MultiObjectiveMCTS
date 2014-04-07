package lunarcode;

import fr.inria.optimization.cmaes.CMAEvolutionStrategy;
import framework.core.Game;
import framework.utils.ElapsedCpuTimer;
import ssamot.mcts.MCTS;
import ssamot.mcts.ucb.optimisation.EDECBOptimiser;
import ssamot.mcts.ucb.optimisation.HOOOptimiser;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 17/03/14
 * Time: 13:57
 */
public class LunarLanderHOOCont {

    public double[] getAction(Game copy, ElapsedCpuTimer ect) {


        int min = 0;
        int max = 1;
        int gamma = 1;
        MCTS.DEBUG = false;

        HOOClass func = new HOOClass((LunarGame) copy);
        int dimension = 8;
        int iterations = 10000;
//        HOOOptimiser hoo = new HOOOptimiser(func, dimension, iterations, min,
//                max, gamma);

        EDECBOptimiser hoo = new EDECBOptimiser(func, dimension, iterations, min,
                max, gamma);
        LunarLanderObjective fitfun = new LunarLanderObjective((LunarGame) copy.getCopy());


        int i = 0;
        while (!ect.exceededMaxTime()) {
            hoo.runForSim(1);
            i++;
        }

        System.out.println("i = " + i);

        double[] best = hoo.getBestNode().sampleAction();
        //System.out.println("best" + Arrays.toString(best));
        //System.out.println(hoo + " " + i) ;
        //System.exit(0);
        double[] tbr = fitfun.convertInputs(best[0],best[1]);
        //LunarGame g = (LunarGame) copy.getCopy();
        //g.tickCont(tbr[0],tbr[1]);
        //System.out.println(g.getShip().v.mag() + " sdfdf")      ;


        //System.out.println("tbtr" + Arrays.toString(tbr));

        return tbr;

    }

}
