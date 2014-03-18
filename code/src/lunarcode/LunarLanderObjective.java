package lunarcode;

import fr.inria.optimization.cmaes.fitness.IObjectiveFunction;
import framework.utils.Vector2d;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 17/03/14
 * Time: 13:08
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class LunarLanderObjective implements IObjectiveFunction {

    private final LunarGame game;
    private static final int MACRO_LENGHT = 4;
    Vector2d desiredDirection = new Vector2d(1, 0);


    public LunarLanderObjective(LunarGame game) {
        this.game = game;
    }

    @Override
    public double valueOf(double[] x) {
        x = x.clone();
        LunarGame cGame = (LunarGame) game.getCopy();

//        for (int i = 0; i < x.length; i++) {
//
//            if (x[i] < 0) {
//                x[i] = 0.0;
//            }
//
//            if (x[i] > 1) {
//                x[i] = 1.0;
//            }
//        }
        int i = 0;
        forward: {
        for (i = 0; i < x.length - 1; i += 2) {

            //System.out.println(thrust + " " + spin);
            double[] realVulues = convertInputs(x[i], x[i + 1]);
            for (int j = 0; j < MACRO_LENGHT; j++) {
                cGame.tickCont(realVulues[0], realVulues[1]);
                if (cGame.isEnded()) {

                    break forward;
                }
            }




        }



        }

        double score = 0;
//        if (cGame.landedSuccessfully) {
//            System.exit(0);
//           // score += 10;
//            //score += cGame.getShip().getRemainingFuel();
//            //score -= cGame.ticks;
//            score = 1000;
        if (!cGame.landedSuccessfully && cGame.isEnded()) {
            score -= 100000000;
        } //else


        Vector2d nv = cGame.getShip().d.copy();
        nv.normalise();
        double anglePoints = nv.dot(desiredDirection) / (nv.mag() * desiredDirection.mag());
        anglePoints = Math.acos(anglePoints) * 180 / Math.PI;
        //System.out.println(anglePoints);

        double speed = cGame.getShip().v.mag();
        Vector2d nearestPad = cGame.terrain.getNearestSafeLandingPoint(cGame.getShip().s);
        double distFromPad = cGame.getShip().s.dist(nearestPad);
        //distFromPad += Math.abs(cGame.getShip().s.x -   nearestPad.x);
        //System.out.println(x[0]);
        if(speed < LunarParams.survivableVelocity) {
            speed = 0;
        }
        //System.out.println(distFromPad*10 +  " " + anglePoints + " " + speed);

        if(distFromPad < 50) {
            score += -distFromPad - anglePoints - speed + game.ticks;
        }

        else {
            score += -distFromPad  + game.ticks;
        }


        if (cGame.landedSuccessfully) {
            //System.out.println(speed + " , " + i);
            score += 1000000;
        }

//        if(speed > 25) {
//            score -=speed*speed-10000;
//        }

        //System.out.println(score);
        return -score;

    }

    public double[] convertInputs(double thrust, double spin) {
        thrust = thrust * 300;
        spin = (spin - 0.5) * 3;

        return new double[]{thrust, spin};

    }


    @Override
    public boolean isFeasible(double[] x) {
        for (int i = 0; i < x.length; i++) {

            if (x[i] < 0) {
                return false;
            }

            if (x[i] > 1) {
                return false;
            }
        }
        return true;
    }
}
