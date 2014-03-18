package lunarcode;

import controllers.utils.StatSummary;
import framework.core.Controller;
import framework.core.Game;
import framework.core.PTSPConstants;
import framework.utils.ElapsedCpuTimer;
import framework.utils.JEasyFrame;

/**
 * Created by Samuel Roberts, 2013
 */
public class LunarRun {


    public static boolean m_verbose = false;

    public static Game game;
    public static LunarLanderEvoCont cont;

    public static void main(String[] args) {
        runGame();
    }

    public static void prepareGame() {
        game = new LunarGame();
        cont = new LunarLanderEvoCont();
    }

    public static void runGame() {
        prepareGame();
        LunarView view = new LunarView((LunarGame)game, (LunarShip)game.getShip());
        JEasyFrame frame = new JEasyFrame(view, "Lunar Lander Cont");

        while(!game.isEnded())
        {

            ElapsedCpuTimer ect = new ElapsedCpuTimer(ElapsedCpuTimer.TimerType.CPU_TIME);
            ect.setMaxTimeMillis(PTSPConstants.ACTION_TIME_MS);


            double[] actionToExecute = cont.getAction(game, ect);

            ((LunarGame) game).tickCont(actionToExecute[0], actionToExecute[1]);
            System.out.println("speed = " + ((LunarGame) game).getShip().v.mag() + " " + ((LunarGame) game).landedSuccessfully);
            view.repaint();
            try {
                Thread.sleep(LunarParams.delay);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        }
        System.out.println("Ship landed with velocity " + ((LunarShip) game.getShip()).v.mag() + "Victory?" + ((LunarGame) game).landedSuccessfully);
        System.exit(0);
    }






//    public static void runGames(int trials) {
//        //Prepare the average results.
//
//
//        StatSummary ssWp = new StatSummary();
//        StatSummary ssTime = new StatSummary();
//        StatSummary ssFuel = new StatSummary();
//
//
//        int numGamesPlayed = 0;
//
//
//        //For each trial...
//        for(int i=0;i<trials;i++)
//        {
//            // ... create a new game.
//            prepareGame();
//
//            numGamesPlayed++; //another game
//
//            //PLay the game until the end.
//            while(!game.isEnded())
//            {
//                //When the result is expected:
//                long due = System.currentTimeMillis()+ PTSPConstants.ACTION_TIME_MS;
//
//                //Advance the game.
//                int actionToExecute = cont.getAction(game.getCopy(), due);
//
//                //Exceeded time
//                long exceeded = System.currentTimeMillis() - due;
//                /*if(exceeded > PTSPConstants.TIME_ACTION_DISQ)
//                {
//                    actionToExecute = 0;
//                    numGamesPlayed--;
//                    m_game.abort();
//
//                }else{  */
//
//                // if(exceeded > PTSPConstants.ACTION_TIME_MS)
//                //     actionToExecute = 0;
//
//                game.tick(actionToExecute);
//                //}
//
//
//
//            }
//
//            //Update the averages with the results of this trial.
//            trials++;
//        }
//    }
}
