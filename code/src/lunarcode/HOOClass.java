package lunarcode;

import framework.core.Game;
import ssamot.mcts.ucb.optimisation.ContinuousProblem;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 */
public class HOOClass extends ContinuousProblem {


    private final  LunarGame game;
    double max = Integer.MAX_VALUE;
    double min = Integer.MIN_VALUE;


    public HOOClass(LunarGame cGame) {

        game = cGame;
    }



    @Override
    public double getFtarget() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double evaluate(double[] x) {
       LunarLanderObjective obj = new LunarLanderObjective(game);

       double score = obj.valueOf(x);
//       min = Math.min(min,score);
//       max = Math.max(max, score);
//
//       //return -(score-min)/(max-min);
        return -score;
    }

}