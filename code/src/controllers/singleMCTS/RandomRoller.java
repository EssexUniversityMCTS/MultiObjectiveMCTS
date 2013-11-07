package controllers.singleMCTS;

import framework.core.Game;

import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 12/02/13
 * Time: 11:08
 * To change this template use File | Settings | File Templates.
 */
public class RandomRoller implements Roller
{
    public static int RANDOM_ROLLOUT = 0;

    //Rollout type.
    private int m_rolloutType;
    
    private Random m_r;

    private int m_numActions;

    public RandomRoller(int a_rT, Random a_r, int a_numActions)
    {
        m_rolloutType = a_rT;
        m_r = a_r;
        m_numActions = a_numActions;
    }

    public int roll(Game a_gameState)
    {
        if(this.m_rolloutType == RANDOM_ROLLOUT)
            return m_r.nextInt(m_numActions);
        else throw new RuntimeException("Unknown rollout mode: " + this.m_rolloutType);
    }
    
}
