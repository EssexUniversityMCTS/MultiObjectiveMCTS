package lunarcode;

import framework.core.Controller;
import framework.core.Game;
import framework.core.GameObject;
import framework.core.Ship;
import framework.utils.Vector2d;

import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Created by Samuel Roberts, 2013
 */
public class LunarShip extends Ship {


    // rendering details!
    public int xpShip[] = {0,16,0,-16};
    public int ypShip[] = {-20,8,-4,8};

    private final double SHIP_RADIUS = 10;

    boolean isThrusting = false;
    public double rotVel;

    // what last move given to ship was
    public int lastMove;

    // remaining fuel for ship
    // upon depletion, ship no longer thrusts
    public double fuel;

    public Game game;


    public LunarShip(Game a_game) {
        super(a_game, LunarParams.startingPoint);
        game = a_game;
        fuel = LunarParams.startingFuel;
        radius = LunarParams.shipRadius;
    }

    public void update() {
        update(lastMove);
    }

    public void update(int move) {
        // set previous (current) position
        ps = s.copy();

        // adjust thrust and spin
        if(Controller.getThrust(move)) {
            thrust(LunarParams.thrustAmount * LunarParams.dt);
            isThrusting = true;
        } else {
            isThrusting = false;
        }
        int turnDir = Controller.getTurning(move);
        spin(turnDir * LunarParams.turnAmount * LunarParams.dt);

        // adjust position and velocity
        s.add(v, LunarParams.dt);
        d.rotate(rotVel * LunarParams.dt);

        v.mul(LunarParams.friction);
        rotVel *= LunarParams.friction;
    }

    public void setNextMove(int shipMove) {
        lastMove = shipMove;
    }

    public void thrust(double force) {
        // limit thrust
        // it can't exceed the thrust limit or be negative
//        if(force > LunarParams.thrustLimit) force = LunarParams.thrustLimit;
//        else if(force < 0) force = 0;

        Vector2d thrustDirection = new Vector2d(0, -1);
        thrustDirection.rotate(d.theta());

        // reuse existing thrust vector to apply force
        thrustDirection.mul(force);
        v.add(thrustDirection);

        // subtract fuel
        fuel -= force;
    }

    public void spin(double impulse) {
        rotVel += impulse;
    }

    public void draw(Graphics2D g) {
        AffineTransform at = g.getTransform();

        // draw a diagram of fuel usage before rotating
        g.translate(s.x, s.y);
        g.setColor(Color.BLUE);
        // first off, the tank outline
        g.drawRect(20, -20, 6, 40);
        // then, the tank contents
        int fuelContents = (int)(40 * (fuel/LunarParams.startingFuel));
        g.fillRect(20, (40 - fuelContents) - 20, 6, fuelContents);

        // rotate for the ship direction and draw the actual ship polygon
        g.rotate(d.theta());
        g.setColor(Color.WHITE);
        g.fillPolygon(xpShip, ypShip, xpShip.length);
        g.setColor(Color.GRAY);
        g.drawPolygon(xpShip, ypShip, xpShip.length);

        // draw a line to indicate thrust force
        if(isThrusting) {
            g.setColor(Color.RED);
            g.drawLine(0, 0, 0, 20);
        }

        g.setTransform(at);
    }

    public int getRemainingFuel() {
        return (int)fuel;
    }


    public void reset() {
        // apparently unused??
        // defining anyway
        s.set(LunarParams.startingPoint);
        v.zero();
        d.set(0, 1);
        rotVel = 0;
        fuel = LunarParams.startingFuel;
    }

    public LunarShip copyShip() {
        LunarShip copy = new LunarShip(game);
        copy.s = s.copy();
        copy.ps = ps.copy();
        copy.d = d.copy();
        copy.v = v.copy();
        copy.rotVel = rotVel;
        copy.fuel = fuel;
        return copy;
    }
}
