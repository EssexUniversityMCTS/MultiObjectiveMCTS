package lunarcode;

import framework.utils.Vector2d;

import java.util.Random;

/**
 * Created by Samuel Roberts, 2013
 */
public class LunarParams {
    public static int screenWidth = 800;
    public static int screenHeight = 600;

    public static int worldWidth = 800;
    public static int worldHeight = 600;

    public static int delay = 20;
    // approximately how many seconds a tick should take if everything is running at full capacity
    // fixed frame calculations introduce less error and ambiguity
    // (this is academic code anyway so lag probably not an issue)
    public static double dt = delay / 1000.0;
    public static Random rand = new Random();


    // Bounding circle radius of ship
    public static int shipRadius = 10;

    // These are in units PER SECOND (approximately), not per step/tick
    public static double thrustAmount = 200;
    public static double turnAmount = Math.PI/2;

    // The tolerance for the ship landing "upright"
    public static double uprightAngularTolerance = Math.PI/2;

    // physical settings of environment
    public static double lunarGravity = 10;
    public static double friction = 1.0;

    // terrain generation values
    public static int randomSeed = 104353;
    public static int numPoints = 100;
    public static int numLandingPads = 1;
    public static int landingPadSize = 5;
    // how fast is too fast when landing?
    public static double survivableVelocity = 5;

    public static double startingFuel = 10000;

    // make the landscape totally flat?
    public static boolean flatLandscape = false;

    public static Vector2d startingPoint = new Vector2d(200, 200);
    public static Vector2d landingFacing = new Vector2d(0, 1);
}
