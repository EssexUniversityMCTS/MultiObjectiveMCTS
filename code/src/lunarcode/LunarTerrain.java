package lunarcode;

import framework.utils.Vector2d;

import java.awt.*;

/**
 * Created by Samuel Roberts, 2013
 */
public class LunarTerrain {
    public double[] moonSurface;
    public int[] landingPads;
    private int numPoints;


    // values held purely for visualisation purposes
    // These values have zero basis on the physics of the simulation.
    // Adjust the values in LunarParams.java for simulation physics adjustment.
    public double gravity = 10;
    public double friction = 1.0;
    public double survivableVelocity = 15;


    public LunarTerrain(int numPoints, int numLandingPads, int seed, boolean flat, double gravity, double friction, double survivableVelocity) {
        this(numPoints, numLandingPads, seed, flat);
        this.gravity = gravity;
        this.friction = friction;
        this.survivableVelocity = survivableVelocity;
    }

    // Generate some terrain for Lunar Lander.
    public LunarTerrain(int numPoints, int numLandingPads, int seed, boolean flat) {
        LunarParams.rand.setSeed(seed);

        this.numPoints = numPoints;

        // create surface
        moonSurface = new double[numPoints];
        moonSurface[0] = 7 * LunarParams.worldHeight/8;

        // if the surface is not flat, make it jagged
        if(!flat) {
            for(int i = 1; i < moonSurface.length; i++) {
                moonSurface[i] = moonSurface[i-1] + (LunarParams.rand.nextGaussian() * 30);

                moonSurface[i] = Math.max(LunarParams.worldHeight / 4, moonSurface[i]);
                moonSurface[i] = Math.min(7 * LunarParams.worldHeight / 8, moonSurface[i]);
            }
        } else {
            // else make it flat and low
            for(int i=0; i<moonSurface.length; i++) {
                moonSurface[i] = 7 * LunarParams.worldHeight/8;
            }
        }

        // create landing pads
        landingPads = new int[numLandingPads * 2];
        for(int i = 0; i < landingPads.length; i += 2) {
            // pick a random starting point and length
            int startIndex = LunarParams.rand.nextInt(moonSurface.length);
            int length = LunarParams.landingPadSize;
            double height = moonSurface[startIndex];
            for(int j = startIndex; j < moonSurface.length && j < (startIndex + length); j++) {
                moonSurface[j] = height;
            }
            if(startIndex + length > moonSurface.length) {
                length = (moonSurface.length - startIndex) - 1;
            }
            landingPads[i] = startIndex;
            landingPads[i+1] = length;
        }
    }

    // Given an x co-ordinate, get the corresponding y or height co-ordinate.
    public double getHeightAtX(double x) {
        double xCheck = x/(LunarParams.worldWidth/numPoints);
        int xIndex = (int)xCheck;
        xIndex = Math.min(xIndex, moonSurface.length - 1);
        xIndex = Math.max(xIndex, 0);
        double interpolation = xCheck - xIndex;
        int nextIndex = xIndex + 1;
        if(nextIndex >= moonSurface.length) nextIndex = 0;
        return moonSurface[xIndex] + interpolation * (moonSurface[nextIndex] - moonSurface[xIndex]);
    }

    // Find the centremost point of the closest landing pad.
    public Vector2d getNearestSafeLandingPoint(Vector2d point) {
        Vector2d landingPoint = new Vector2d();
        Vector2d bestPoint = new Vector2d();
        double bestDist = Double.MAX_VALUE;

        double xCheck = point.x/(LunarParams.worldWidth/numPoints);
        int xIndex = (int)xCheck;

        for(int i = 0; i < landingPads.length; i += 2) {
            int lowerBound = landingPads[i];
            int upperBound = landingPads[i] + (landingPads[i+1] - 1);
            landingPoint.x = (lowerBound + (landingPads[i+1]/2)) * (LunarParams.worldWidth/moonSurface.length);
            landingPoint.y = moonSurface[lowerBound];
            if(point.dist(landingPoint) < bestDist) {
                bestPoint.set(landingPoint);
                bestDist = point.dist(landingPoint);
            }
        }

        return bestPoint;
    }

    // Is the ship on a landing pad?
    public boolean onLandingPad(LunarShip ship) {
        boolean landed = false;
        if(isShipColliding(ship)) {
            Vector2d point = ship.s;

            double xCheck = point.x/(LunarParams.worldWidth/numPoints);
            int xIndex = (int)xCheck;

            for(int i = 0; i < landingPads.length; i += 2) {
                int lowerBound = landingPads[i];
                int upperBound = landingPads[i] + (landingPads[i+1] - 1);
                if(xIndex >= lowerBound && xIndex < upperBound) {
                    landed = true;
                    break;
                }
            }
        }
        return landed;
    }

    // Is the ship colliding with the terrain in general?
    public boolean isShipColliding(LunarShip ship) {
        // check ship is within bounds
        if(ship.s.x < 0 || ship.s.x >= LunarParams.worldWidth) return false;
        // check for collision
        Vector2d point = ship.s;
        double yValueToTest = getHeightAtX(point.x);
        double shipBottomY = ship.s.y + ship.radius;
        return shipBottomY >= yValueToTest;
    }

    // Just draw the terrain with no fancy bells or whistles.
    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        double xInterval = LunarParams.worldWidth / (double)moonSurface.length;
        // Draw the actual terrain as a jagged line
        for(int i = 0; i < moonSurface.length; i++) {

            g2d.drawLine((int)(i * xInterval), (int)moonSurface[i], (int)((i+1) * xInterval), (int)moonSurface[((i+1)%moonSurface.length)]);
        }
        // Draw the landing pads again over the line but in a different colour
        g2d.setColor(Color.BLUE);
        for(int i = 0; i < landingPads.length; i += 2) {
            g2d.drawLine((int)(landingPads[i] * xInterval), (int)(moonSurface[landingPads[i]]),
                    (int)((landingPads[i] + (landingPads[i+1]-1)) * xInterval), (int)(moonSurface[landingPads[i]]));
        }
    }

    // Presentation drawing: Draw some extra statistics about the physics of the environment.
    // Probably not needed for this project, but kept around just in case.
    public void draw(Graphics2D g2d, float alpha) {
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));


        double xInterval = LunarParams.worldWidth / (double)moonSurface.length;
        for(int i = 0; i < moonSurface.length; i++) {
            g2d.setColor(Color.WHITE);
            g2d.drawLine((int)(i * xInterval), (int)moonSurface[i], (int)((i+1) * xInterval), (int)moonSurface[((i+1)%moonSurface.length)]);

            // draw strength of gravity
            g2d.setColor(Color.GREEN);
            g2d.drawLine((int)(i * xInterval), (int)moonSurface[i], (int)(i * xInterval), (int)(moonSurface[i] + gravity*2));
        }
        for(int i = 0; i < landingPads.length; i += 2) {
            g2d.setColor(Color.BLUE);
            g2d.drawLine((int)(landingPads[i] * xInterval), (int)(moonSurface[landingPads[i]]),
                    (int)((landingPads[i] + (landingPads[i+1]-1)) * xInterval), (int)(moonSurface[landingPads[i]]));

            // draw survivable velocity
            g2d.setColor(Color.RED);
            g2d.drawLine((int)(landingPads[i] * xInterval), (int)(moonSurface[landingPads[i]]), (int)(landingPads[i] * xInterval), (int)(moonSurface[landingPads[i]] - (survivableVelocity*2)));
        }

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }
}
