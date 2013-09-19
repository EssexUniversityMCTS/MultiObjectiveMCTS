package lunarcode;

import framework.core.GameObject;

/**
 * Created by Samuel Roberts, 2013
 */
public interface IGame {

    // has the game ended? return true if it has
    public boolean isEnded();

    // return a copy of the game state
    public IGame getCopy();

    // advance game state
    public void tick(int action);

    // get the game ship
    //public GameObject getShip();
}
