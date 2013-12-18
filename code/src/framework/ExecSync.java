package framework;

import controllers.ParetoMCTS.ParetoMCTSController;
import controllers.keycontroller.KeyController;
import controllers.utils.StatSummary;
import framework.core.*;
import framework.utils.JEasyFrame;

import java.awt.*;


/**
 * This class may be used to execute the game in timed or un-timed modes, with or without
 * visuals. Competitors should implement his controller in a subpackage of 'controllers'.
 * The skeleton classes are already provided. The package
 * structure should not be changed (although you may create sub-packages in these packages).
 */
@SuppressWarnings("unused")
public class ExecSync extends Exec
{

    /**
     * Run a game in ONE map. In order to slow thing down in case
     * the controllers return very quickly, a time limit can be used.
     * Use this mode to play the game with the KeyController.
     *
     * @param delay The delay between time-steps
     */
    public static void playGame(int delay)
    {
        m_controllerName = "controllers.keycontroller.KeyController";

        //Get the game ready.
        if(!prepareGame())
            return;


        //Indicate what are we running
        if(m_verbose) System.out.println("Running " + m_controllerName + " in map " + m_game.getMap().getFilename() + "...");

        JEasyFrame frame;

        //View of the game, if applicable.
        m_view = new PTSPView(m_game, m_game.getMapSize(), m_game.getMap(), m_game.getShip(), m_controller);
        frame = new JEasyFrame(m_view, "PTSP-Game: " + m_controllerName);

        //If we are going to play the game with the cursor keys, add the listener for that.
        if(m_controller instanceof KeyController)
        {
            frame.addKeyListener(((KeyController)m_controller).getInput());
        }


        while(!m_game.isEnded())
        {

            //When the result is expected:
            long then = System.currentTimeMillis();
            long due = then+PTSPConstants.ACTION_TIME_MS;

            //Advance the game.
            m_game.tick(m_controller.getAction(m_game.getCopy(), due));

            long now = System.currentTimeMillis();
            int remaining = (int) Math.max(0, delay - (now-then));     //To adjust to the proper framerate.

            //Wait until de next cycle.
            waitStep(remaining);

            //And paint everything.
            m_view.repaint();
        }

        if(m_verbose)
            m_game.printResults();

        //And save the route, if requested:
        if(m_writeOutput)
            m_game.saveRoute();

    }

    /**
     * Runs a game in ONE map.
     *
     * @param visual Indicates whether or not to use visuals
     * @param delay Includes delay between game steps.
     */
    public static void runGame(boolean visual, int delay)
    {
        //Get the game ready.
        if(!prepareGame())
            return;


        //Indicate what are we running
        if(m_verbose) System.out.println("Running " + m_controllerName + " in map " + m_game.getMap().getFilename() + "...");

        JEasyFrame frame;
        if(visual)
        {
            //View of the game, if applicable.
            m_view = new PTSPView(m_game, m_game.getMapSize(), m_game.getMap(), m_game.getShip(), m_controller);
            frame = new JEasyFrame(m_view, "PTSP-Game: " + m_controllerName);
        }


        while(!m_game.isEnded())
        {
            //When the result is expected:
            long then = System.currentTimeMillis();
            long due = then + PTSPConstants.ACTION_TIME_MS;

            //Advance the game.
            int actionToExecute = m_controller.getAction(m_game.getCopy(), due);

            //Exceeded time
            long now = System.currentTimeMillis();
            long spent = now - then;

            /*if(spent > PTSPConstants.TIME_ACTION_DISQ)
            {
                actionToExecute = 0;
                System.out.println("Controller disqualified. Time exceeded: " + (spent - PTSPConstants.TIME_ACTION_DISQ));
                m_game.abort();

            }else{
              */
                //if(spent > PTSPConstants.ACTION_TIME_MS)
                //    actionToExecute = 0;
                m_game.tick(actionToExecute);
                //    System.out.printf("%.3f\n", m_game.getShip().v.mag());
            //}

            int remaining = (int) Math.max(0, delay - (now-then));//To adjust to the proper framerate.
            //Wait until de next cycle.
            waitStep(remaining);

            //And paint everything.
            if(m_visibility)
            {
                m_view.repaint();
                if(m_game.getTotalTime() == 1)
                    waitStep(m_warmUpTime);
            }
        }

        if(m_verbose)
        {
            m_game.printResults();
            if(m_controller instanceof ParetoMCTSController)
                System.out.format("Average iterations: %.3f\n", ((double)  ((ParetoMCTSController)m_controller).m_player.m_numIters /
                ((ParetoMCTSController)m_controller).m_player.m_numCalls));
        }
        //And save the route, if requested:
        if(m_writeOutput)
            m_game.saveRoute();

    }

    /**
     * For running multiple games without visuals, in several maps (m_mapNames).
     *
     * @param trials The number of trials to be executed
     */
    public static void runGames(int trials)
    {
        //Prepare the average results.
        double avgTotalWaypoints=0;
        double avgTotalTimeSpent=0;
        int totalDisqualifications=0;
        int totalNumGamesPlayed=0;
        boolean moreMaps = true;

        StatSummary ssWp, ssTime, ssFuel, ssDamage;

        for(int m = 0; moreMaps && m < m_mapNames.length; ++m)
        {
            ssWp = new StatSummary();
            ssTime = new StatSummary();
            ssFuel = new StatSummary();
            ssDamage = new StatSummary();

            String mapName = m_mapNames[m];
            double avgWaypoints=0;
            double avgTimeSpent=0;
            int numGamesPlayed = 0;

            if(m_verbose)
            {
                System.out.println("--------");
                System.out.println("Running " + m_controllerName + " in map " + mapName + "...");
            }

            //For each trial...
            for(int i=0;i<trials;i++)
            {
                // ... create a new game.
                if(!prepareGame())
                    continue;

                numGamesPlayed++; //another game

                //PLay the game until the end.
                while(!m_game.isEnded())
                {
                    //When the result is expected:
                    long due = System.currentTimeMillis()+PTSPConstants.ACTION_TIME_MS;

                    //Advance the game.
                    int actionToExecute = m_controller.getAction(m_game.getCopy(), due);

                    //Exceeded time
                    long exceeded = System.currentTimeMillis() - due;
                    /*if(exceeded > PTSPConstants.TIME_ACTION_DISQ)
                    {
                        actionToExecute = 0;
                        numGamesPlayed--;
                        m_game.abort();

                    }else{  */

                       // if(exceeded > PTSPConstants.ACTION_TIME_MS)
                       //     actionToExecute = 0;

                        m_game.tick(actionToExecute);
                    //}

                }

                //Update the averages with the results of this trial.
                avgWaypoints += m_game.getWaypointsVisited();
                avgTimeSpent += m_game.getTotalTime();

                if(m_game.getWaypointsVisited() == 10)
                {
                    ssWp.add(m_game.getWaypointsVisited());
                    ssTime.add(m_game.getTotalTime());
                    ssFuel.add(PTSPConstants.INITIAL_FUEL - m_game.getShip().getRemainingFuel());
                    ssDamage.add(m_game.getShip().getDamage());
                }  else trials++;

                //Print the results.
                if(m_verbose)
                {
                    System.out.print(i+"\t");
                    m_game.printResults();
                    if(m_controller instanceof ParetoMCTSController)
                        System.out.format("Average iterations: %.3f\n", ((double)  ((ParetoMCTSController)m_controller).m_player.m_numIters /
                            ((ParetoMCTSController)m_controller).m_player.m_numCalls));
                }

                //And save the route, if requested:
                if(m_writeOutput)
                    m_game.saveRoute();
            }

            moreMaps = m_game.advanceMap();

            avgTotalWaypoints += (avgWaypoints / numGamesPlayed);
            avgTotalTimeSpent += (avgTimeSpent / numGamesPlayed);
            totalDisqualifications += (trials - numGamesPlayed);
            totalNumGamesPlayed += numGamesPlayed;

            //Print the average score.
            if(m_verbose)
            {
                System.out.println("--------");
                //System.out.format("Average waypoints: %.3f, average time spent: %.3f\n",
                //        (avgWaypoints / numGamesPlayed), (avgTimeSpent / numGamesPlayed));
                //System.out.println("Disqualifications: " + (trials - numGamesPlayed) + "/" + trials);

                System.out.format("Average waypoints: %.3f +- %.3f, average time spent: %.3f +- %.3f" +
                        ", average fuel consumed: %.3f +- %.3f, average damage taken: %.3f +- %.3f\n",
                        ssWp.mean(), ssWp.stdErr(), ssTime.mean(), ssTime.stdErr(), ssFuel.mean(), ssFuel.stdErr(), ssDamage.mean(), ssDamage.stdErr());
            }
        }

        //Print the average score.
        /*if(m_verbose)
        {
            System.out.println("-------- Final score --------");
            //System.out.format("Average waypoints: %.3f, average time spent: %.3f\n", (avgTotalWaypoints / m_mapNames.length), (avgTotalTimeSpent / m_mapNames.length));
            //System.out.println("Disqualifications: " + (trials*m_mapNames.length - totalNumGamesPlayed) + "/" + trials*m_mapNames.length);
        }        */
    }

    /**
     * For running multiple games without visuals, in several maps (m_mapNames).
     *
     * @param trials The number of trials to be executed
     */
    public static void runGamesStats(int trials)
    {
        boolean moreMaps = true;

        for(int m = 0; moreMaps && m < m_mapNames.length; ++m)
        {
            String mapName = m_mapNames[m];
            int numGamesPlayed = 0;

            int ntrials = trials;

            //For each trial...
            for(int i=0;i<ntrials;i++)
            {
                // ... create a new game.
                if(!prepareGame())
                    continue;

                numGamesPlayed++; //another game

                //PLay the game until the end.
                while(!m_game.isEnded())
                {
                    //When the result is expected:
                    long due = System.currentTimeMillis()+PTSPConstants.ACTION_TIME_MS;

                    //Advance the game.
                    int actionToExecute = m_controller.getAction(m_game.getCopy(), due);

                    //Exceeded time
                    long exceeded = System.currentTimeMillis() - due;

                    m_game.tick(actionToExecute);
                }

                if(m_game.getWaypointsVisited() != 10)
                    ntrials++;
                else
                {
                    int consumedFuel = PTSPConstants.INITIAL_FUEL - m_game.getShip().getRemainingFuel();
                    System.out.println(mapName + " " + m_game.getWaypointsVisited() + " " + m_game.getTotalTime()
                            + " " + consumedFuel + " " + m_game.getShip().getDamage());
                }

                //And save the route, if requested:
                if(m_writeOutput)
                    m_game.saveRoute();
            }

            moreMaps = m_game.advanceMap();

        }

    }


    /**
     * The main method. Several options are listed - simply remove comments to use the option you want.
     *
     * @param args the command line arguments. Not needed in this class.
     */
    public static void main(String[] args)
    {
        m_mapNames = new String[]{"maps/ptsp_map02.map"}; //Set here the name of the map to play in.
        //m_mapNames = new String[]{"maps/ptsp_map01.map","maps/ptsp_map02.map","maps/ptsp_map08.map",
        //        "maps/ptsp_map19.map","maps/ptsp_map24.map","maps/ptsp_map35.map","maps/ptsp_map40.map",
        //        "maps/ptsp_map45.map","maps/ptsp_map56.map","maps/ptsp_map61.map"}; //In an array, to play in mutiple maps with runGames().

        m_controllerName = "controllers.greedy.GreedyController"; //Set here the controller name.
        m_controllerName = "controllers.MacroRandomSearch.MacroRSController"; //Set here the controller name.
        m_controllerName = "controllers.ParetoMCTS.ParetoMCTSController"; //Set here the controller name.
//        m_controllerName = "controllers.mctsdriver.MctsDriverController"; //Set here the controller name.
        //m_controllerName = "controllers.singleMCTS.SingleMCTSController";
        //m_controllerName = "controllers.nsga2Controller.NSGAIIController";

        //m_controllerName = "controllers.lineofsight.LineOfSight";
        //m_controllerName = "controllers.random.RandomController";
        //m_controllerName = "controllers.WoxController.WoxController"; //Set here the controller name. Leave it to null to play with KeyController.
        m_visibility = true; //Set here if the graphics must be displayed or not (for those modes where graphics are allowed).
        m_writeOutput = false; //Indicate if the actions must be saved to a file after the end of the game (the file name will be the current date and time)..
        m_verbose = true;
        //m_warmUpTime = 750; //Change this to modify the wait time (in milliseconds) before starting the game in a visual mode


        /////// 1. To play the game with the key controller.
        int delay = PTSPConstants.DELAY;  //PTSPConstants.DELAY: best human play speed
        playGame(delay);

        /////// 2. Executes one game.
        //ParetoMCTSController.EXPLORATION_VIEW_ON = true;
        //ParetoMCTSController.PARETO_VIEW_ON = true;
        //int delay = 5;  //1: quickest; PTSPConstants.DELAY: human play speed, PTSPConstants.ACTION_TIME_MS: max. controller delay
        //runGame(m_visibility, delay);

        ////// 3. Executes N games (numMaps x numTrials), graphics disabled.
        //m_writeOutput = true;
        //int numTrials=10;
        //runGames(numTrials);


        ////// 3. Executes N games (numMaps x numTrials), graphics disabled.
        //m_writeOutput = true;
        //m_verbose = false; //hides additional output. runGamesStats prints anyway.
        //int numTrials=30;
        //runGamesStats(numTrials);

    }

}

