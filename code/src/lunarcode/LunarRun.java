package lunarcode;

import controllers.utils.StatSummary;
import framework.core.Controller;
import framework.core.Game;
import framework.core.PTSPConstants;
import framework.utils.ElapsedCpuTimer;
import framework.utils.JEasyFrame;
import framework.utils.Vector2d;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by Samuel Roberts, 2013
 */
public class LunarRun {


    public static boolean m_verbose = false;

    public static Game game;
    public static LunarLanderEvoCont cont;
    //public static LunarLanderHOOCont cont;

    public static void main(String[] args) {
        runGame();
        //runGames(10);

    }

    public static void prepareGame() {
        game = new LunarGame();
        cont = new LunarLanderEvoCont();
       // cont = new LunarLanderHOOCont();
    }

    public static void runGame() {


        prepareGame();
        LunarView view = new LunarView((LunarGame) game, (LunarShip) game.getShip());
        JEasyFrame frame = new JEasyFrame(view, "Lunar Lander Cont");

        while (!game.isEnded()) {


            ElapsedCpuTimer ect = new ElapsedCpuTimer(ElapsedCpuTimer.TimerType.CPU_TIME);
            ect.setMaxTimeMillis(PTSPConstants.ACTION_TIME_MS);


            double[] actionToExecute = cont.getAction(game, ect);

            ((LunarGame) game).tickCont(actionToExecute[0], actionToExecute[1]);
            //System.out.println("speed = " + ((LunarGame) game).getShip().v.mag() + " " + ((LunarGame) game).landedSuccessfully);
            view.repaint();

//            Vector2d nv = ((LunarGame) game).ship.d.copy();
//            nv.normalise();
//
//            double anglePoints = nv.dot(LunarParams.landingFacing) / (nv.mag() * LunarParams.landingFacing.mag());
//            anglePoints = Math.acos(anglePoints);
//            //System.out.println("anglePoints = " + anglePoints);
//
//            Vector2d nearestPad = ((LunarGame)game).terrain.getNearestSafeLandingPoint(game.getShip().s);
//            Vector2d s = game.getShip().s;
//            Vector2d goalVector = new Vector2d(s.x - nearestPad.x, s.y - nearestPad.y);
//            goalVector.normalise();
//            //System.out.println("nv = " + nv);
//            //System.out.println("goalVector = " + goalVector);

            try {
                Thread.sleep(LunarParams.delay);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        }


        int victory = 0;
        if (((LunarGame) game).landedSuccessfully) {
            victory = 1;
        }

        System.out.println(victory + ", " + ((LunarGame) game).ticks );
        //System.out.println("Ship landed with velocity " + ((LunarShip) game.getShip()).v.mag() + "Victory?" + ((LunarGame) game).landedSuccessfully);
        System.exit(0);

    }
//
//


    public static void runGames(int trials) {
        //Prepare the average results.

        StatSummary victories = new StatSummary();
        StatSummary mean_score = new StatSummary();

//        BufferedWriter writer = null;
//        try {
//
//            File logFile = new File("/home/ssamot/projects/MultiObjectiveMCTS/lpaper/lunarexps/noise_statistics.csv");
//            logFile.createNewFile();
//
//
//
//
//            //Close writer
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//


        int numGamesPlayed = 0;


        //For each trial...
        for (int i = 0; i < trials; i++) {
            // ... create a new game.
            prepareGame();

            numGamesPlayed++; //another game

            //PLay the game until the end.
            while (!game.isEnded()) {

                //Advance the game.
                ElapsedCpuTimer ect = new ElapsedCpuTimer(ElapsedCpuTimer.TimerType.CPU_TIME);
                ect.setMaxTimeMillis(PTSPConstants.ACTION_TIME_MS);


                double[] actionToExecute = cont.getAction(game, ect);

                ((LunarGame) game).tickCont(actionToExecute[0], actionToExecute[1]);


            }

            //System.out.println("i = " + i);
            //System.out.println("trials = " + trials);
            double victory = 0;


            mean_score.add(victory);
            victories.add(((LunarGame) game).ticks);
            System.out.println(mean_score);
            System.out.println(victories);

            //System.out.println(");

            System.out.println("Ship landed with velocity " + ((LunarShip) game.getShip()).v.mag() + "Victory?" + ((LunarGame) game).landedSuccessfully);

            //writer.write()

        }


//        try {
//            writer.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }
}
