package lunarcode;

import framework.core.Game;
import ssamot.mcts.ucb.optimisation.ContinuousProblem;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 */
public class HOOClass extends ContinuousProblem {


    private final  LunarGame game;
    double max = Integer.MIN_VALUE;
    double min = Integer.MAX_VALUE;
    double[] best = null;


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
       score = -score;
//       min = Math.min(min,score);

//       max = Math.max(max, score);
//
//       //return -(score-min)/(max-min);

        if(score > max) {
            max = score;
            best = x;
        }

        min = Math.min(min,score);


        return score;
    }

}