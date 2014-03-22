package lunarcode;

import fr.inria.optimization.cmaes.fitness.IObjectiveFunction;
import framework.utils.Vector2d;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 17/03/14
 * Time: 13:08
 */
public class LunarLanderObjective implements IObjectiveFunction {

    private final LunarGame game;
    public static int MACRO_LENGTH = 8;
    Vector2d vertical = new Vector2d(1, 0);
    Vector2d horizontal = new Vector2d(0, -1);
    public static final int WIN_BASE = 999999999;


    public LunarLanderObjective(LunarGame game) {
        this.game = game;
    }

    @Override
    public double valueOf(double[] x) {
        x = x.clone();
        LunarGame cGame = (LunarGame) game.getCopy();

        Vector2d nearestPad = cGame.terrain.getNearestSafeLandingPoint(game.getShip().s);
        double distFromPad = cGame.getShip().s.dist(nearestPad);
        double xdist_oring =   Math.abs(game.getShip().s.x - nearestPad.x);
        double y_maxspeed = 2;
        double x_ignorspeed = 1;
        if (distFromPad > 40) {
            y_maxspeed = 30;
            x_ignorspeed = 10;
        }


        int phase = 0;
        if ( xdist_oring < 50) {
            phase = 1;
        }

        int i = 0;
        double score = 0;

        forward:
        {
            for (i = 0; i < x.length - 1; i += 2) {

                //System.out.println(thrust + " " + spin);
                double[] realVulues = convertInputs(x[i], x[i + 1]);
                for (int j = 0; j < MACRO_LENGTH; j++) {
                    cGame.tickCont(realVulues[0], realVulues[1]);
                    if (cGame.isEnded()) {

                        break forward;
                    }


//
                    else {
                        if (phase == 1) {
                            Vector2d nv = cGame.getShip().d.copy();
                            //nv.normalise();
                            double anglePoints = nv.dot(vertical) / (nv.mag() * vertical.mag());
                            anglePoints = Math.acos(anglePoints);

                            if (anglePoints < 0.5) {
                                anglePoints = 0.0;
                            }
                            score -= anglePoints;
                        }

//
                    }


                }


            }


        }


        if(phase == 1) {
            score += getScorePhaseB(game, cGame, nearestPad, x_ignorspeed, y_maxspeed);
        }

        if(phase == 0) {
            double xdist = Math.abs(cGame.getShip().s.x - nearestPad.x);
            double ydist = Math.abs(cGame.getShip().s.y - nearestPad.y);
            Vector2d speed = cGame.getShip().v;
            score = -Math.abs(speed.mag() - 100) - xdist;
            //score += getScorePhaseA(game, cGame, nearestPad, 2, 50);
            //System.out.println("score = " + score);
        }



        return -score;


    }

    double getScorePhaseB(LunarGame startingGame, LunarGame cGame, Vector2d nearestPad, double x_ignorspeed, double y_maxspeed) {

        double score = 0;
        Vector2d speed = cGame.getShip().v;
        double xdist = Math.abs(cGame.getShip().s.x - nearestPad.x);
        //score -= Math.abs(Math.abs(speed.y)-2) + Math.abs(speed.x);

        if (Math.abs(speed.x) > x_ignorspeed) {

            score -= Math.abs(speed.x);
        }

        score -= Math.abs(speed.y - y_maxspeed);
        score -= xdist;

        return score;
    }


    double getScorePhaseA(LunarGame startingGame, LunarGame cGame, Vector2d nearestPad, double x_ignorspeed, double y_maxspeed) {

        double score = 0;
        Vector2d speed = cGame.getShip().v;
        double xdist = Math.abs(cGame.getShip().s.x - nearestPad.x);
        //score -= Math.abs(Math.abs(speed.y)-2) + Math.abs(speed.x);

        if (Math.abs(speed.y) > x_ignorspeed) {

            score -= Math.abs(speed.y);
        }

        score -= Math.abs(speed.x - y_maxspeed);
        //score -= xdist;


        return score;
    }


    public double[] convertInputs(double thrust, double spin) {
        thrust = thrust * 300;
        spin = (spin - 0.5) * 5;

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
