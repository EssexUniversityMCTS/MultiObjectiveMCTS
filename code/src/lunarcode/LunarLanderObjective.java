package lunarcode;

import fr.inria.optimization.cmaes.fitness.IObjectiveFunction;
import framework.utils.Vector2d;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 17/03/14
 * Time: 13:08
 */
public class LunarLanderObjective implements IObjectiveFunction {

    private final LunarGame game;
    public static int MACRO_LENGTH = 7;
    Vector2d vertical = new Vector2d(1, 0);
    Vector2d left = new Vector2d(1, 2);
    Vector2d right = new Vector2d(1, -2);
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
        double xdist_oring = Math.abs(game.getShip().s.x - nearestPad.x);
        double y_maxspeed = 2;
        double x_ignorspeed = 5;


        if (distFromPad > 60) {
            y_maxspeed = 10;
            x_ignorspeed = 10;
        }

        double distanceToTerrain = getDistance(game);

        int phase = 0;
        if (xdist_oring < 50) {
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
                        // double rotvel = Math.abs(((LunarShip) cGame.getShip()).rotVel);

                        Vector2d correctOrientation = vertical;
                        if (phase == 1) {

                            Vector2d nv = cGame.getShip().d.copy();
                            //nv.normalise();
                            double anglePoints = nv.dot(correctOrientation) / (nv.mag() * correctOrientation.mag());
                            anglePoints = Math.acos(anglePoints);

                            if (anglePoints < 0.3) {
                                anglePoints = 0.0;
                            }
                            score -= anglePoints;

                        } else {

                            double angle_tolerance = 0.5;
                            if (nearestPad.y < game.getShip().s.y || game.getShip().s.y > 200) {
                                correctOrientation = vertical;
                            } else if (nearestPad.x < game.getShip().s.x) {
                                correctOrientation = right;
                            } else {
                                correctOrientation = left;
                            }


                            if (distanceToTerrain < 50) {
                                if (correctOrientation == left) {
                                    correctOrientation = right;
                                }
                                if (correctOrientation == right) {
                                    correctOrientation = left;
                                }
                                angle_tolerance = 0.0;
                            }

                            Vector2d nv = cGame.getShip().d.copy();
                            //nv.normalise();
                            double anglePoints = nv.dot(correctOrientation) / (nv.mag() * correctOrientation.mag());
                            anglePoints = Math.acos(anglePoints);

                            if (anglePoints < angle_tolerance) {
                                anglePoints = 0.0;
                            }

                            score -= anglePoints;


                        }


//
                    }


                }


            }


        }


        Vector2d speed = cGame.getShip().v;
//        score-=speed.mag();
//        return -score;


        if (phase == 1) {
            score += getScorePhaseB(game, cGame, nearestPad, x_ignorspeed, y_maxspeed);
        }


//
        if (phase == 0) {
            double xdist = Math.abs(cGame.getShip().s.x - nearestPad.x);
            double ydist = Math.abs(nearestPad.y - 30);
//
            //double dist = cGame.getShip().s.dist(new Vector2d(nearestPad.x,nearestPad.y -50 ));

            //System.out.println("dist = " + dist);
            //score += -Math.abs(speed.x - 30) - Math.abs(speed.y);


            score -= ydist + xdist;
            score += -Math.abs(speed.mag() - 30);


            //
//            if (rotvel > 0.3) {
//                score -= rotvel;
//                System.out.println("rotvel = " + rotvel);
//            }

            if (distanceToTerrain < 50) {
                //System.out.println(distanceToTerrain);
                score += -getDistance(cGame);
                //System.out.println("distanceToTerrain = " + distanceToTerrain);
                //System.out.println("score = " + score);
            }
            //System.out.println("((LunarShip)cGame.getShip()).rotVel = " + ((LunarShip)cGame.getShip()).rotVel);
            //score += getScorePhaseA(game, cGame, nearestPad, 2, 50);
            //System.out.println("score = " + score);
        }


        double penalty = -100000;
        ;
        if (cGame.landed && !cGame.landedSuccessfully) {
            //System.out.println("failure" );
            score += penalty;
        }

        if ((cGame.getShip().s.x < 0 || cGame.getShip().s.x >= LunarParams.worldWidth)) {
            //System.out.println("failure" );
            score += penalty;
        }

        if ((cGame.getShip().s.y < 0 || cGame.getShip().s.y >= LunarParams.worldHeight)) {
            //System.out.println("failure" );
            score += penalty;
        }


        return -score;


    }

    public double getDistance(LunarGame game) {
        // check ship is within bounds
        LunarShip ship = (LunarShip) game.getShip();

        // check for collision
        Vector2d point = ship.s;
        double yValueToTest = game.terrain.getHeightAtX(point.x);
        double shipBottomY = ship.s.y + ship.radius;
        double dist = shipBottomY - yValueToTest;
        //System.out.println("-shipBottomY + yValueToTest = " + (shipBottomY - yValueToTest));
        return Math.abs(dist);
    }

    double getScorePhaseB(LunarGame startingGame, LunarGame cGame, Vector2d nearestPad, double x_ignorspeed, double y_maxspeed) {

        double score = 0;
        Vector2d speed = cGame.getShip().v;
        double xdist = Math.abs(cGame.getShip().s.x - nearestPad.x);
        //score -= Math.abs(Math.abs(speed.y)-2) + Math.abs(speed.x);

        if (xdist < 5) {
            xdist = 0;
        }

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
