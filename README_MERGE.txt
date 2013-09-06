Game.java - State of the game.

 - boolean isEnded() -> game is finished.
 - game getCopy() -> creates a copy of the game.

Heurisic class implements HeuristicMO

 - double[] value(Game a_gameState);
 - double[][] getValueBounds();
 - boolean mustBePruned(Game a_newGameState, Game a_previousGameState);
 - void setPlayoutInfo(PlayoutInfo a_pi);
 - void addPlayoutInfo(int a_lastAction, Game a_gameState);