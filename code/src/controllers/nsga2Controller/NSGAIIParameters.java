package controllers.nsga2Controller;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 07/11/13
 * Time: 17:09
 */
public class NSGAIIParameters
{

    public static int MACRO_ACTION_LENGTH = 15;
    public static int ROLLOUT_DEPTH = 8;
    public static int NUM_ACTIONS = 6;
    public static double K = Math.sqrt(2);
    public static boolean EXPLORATION_VIEW_ON;

    public static double[] targetWeights=
            new double[]{0.33,0.33,0.33};
            //new double[]{0.6,0.3,0.1};
            //new double[]{0.3,0.6,0.1};
           // new double[]{0.1,0.3,0.6};
}
