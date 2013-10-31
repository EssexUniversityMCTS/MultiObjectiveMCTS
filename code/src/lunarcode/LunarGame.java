package lunarcode;

import framework.core.Game;
import framework.core.Ship;
import framework.utils.Vector2d;

import java.awt.*;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Samuel Roberts, 2013
 */
public class LunarGame extends Game {


    public LunarTerrain terrain;
    public LunarShip ship;
    public int ticks;

    // did the ship contact the landscape?
    public boolean landed;
    // did the ship contact the landscape while obeying the minimum velocity for safe landing?
    public boolean landedSuccessfully;

    public LunarGame() {
        terrain = new LunarTerrain(LunarParams.numPoints, LunarParams.numLandingPads, LunarParams.randomSeed, LunarParams.flatLandscape);
        ship = new LunarShip(this);
        ticks = 0;
    }

    // Update everything within the game logic, including the ship.
    public void tick(int shipMove) {
        ship.setNextMove(shipMove);

        // add gravity to the ship
        ship.v.add(0, LunarParams.lunarGravity * LunarParams.dt);
        ship.update();

        // wrap ship around screen based on centre x (basic but works)
        ship.s.x %= LunarParams.worldWidth;
        if(ship.s.x < 0) ship.s.x += LunarParams.worldWidth;

        // check for collision
        if(terrain.isShipColliding(ship)) {
            landed = true;
            if(terrain.onLandingPad(ship) && ship.v.mag() <= LunarParams.survivableVelocity) landedSuccessfully = true;
        }

        ticks++;
    }

    public int getTicks() {
        return ticks;
    }

    // pretty basic
    public boolean isEnded() {
        return landed;
    }

    public Game getCopy() {
        LunarGame copy = new LunarGame();
        // no need to copy terrain, an identical terrain object will have
        // already been made in the game copy as parameters are identical
        copy.ship = ship.copyShip();
        copy.ticks = ticks;
        copy.landed = landed;
        copy.landedSuccessfully = landedSuccessfully;
        return copy;
    }

    public Ship getShip() {
        return ship;
    }
}
