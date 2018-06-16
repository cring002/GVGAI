package tracks.multiPlayer;

import tools.Utils;
import tracks.ArcadeMachine;

import java.util.Random;

/**
 * Created with IntelliJ IDEA. User: Raluca Date: 12/04/16 This is a Java port
 * from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class TestMultiPlayer_cmd {
	//for now args is:
	// [0] - int, number of levels
	// [1] - int, number of repetitions
	// [2-?] - the rest are ints for game indexs. if none, all 10 games run
    public static void main(String[] args) {

		// Available controllers:
		String doNothingController = "tracks.multiPlayer.simple.doNothing.Agent";
		String randomController = "tracks.multiPlayer.simple.sampleRandom.Agent";
		String oneStepController = "tracks.multiPlayer.simple.sampleOneStepLookAhead.Agent";

		String sampleMCTSController = "tracks.multiPlayer.advanced.sampleMCTS.Agent";
		String sampleRSController = "tracks.multiPlayer.advanced.sampleRS.Agent";
		String sampleRHEAController = "tracks.multiPlayer.advanced.sampleRHEA.Agent";
		String humanController = "tracks.multiPlayer.tools.human.Agent";

		String coRHEA = "tracks.multiPlayer.advanced.RHCP.Agent";

		String betterRHEA = "tracks.multiPlayer.advanced.betterRHEA.Agent";

		// Set here the controllers used in the games (need 2 separated by space).
		String controllers = betterRHEA + " " + coRHEA;
        String controllers2 = coRHEA + " " + betterRHEA;

		//Load available games
//		String spGamesCollection =  "examples/all_games_2p_test.csv";
		String spGamesCollection =  System.getProperty("user.dir")+"\\examples\\all_games_2p_test.csv";
		System.out.println(spGamesCollection);
		// in games[][] - there are 10 games: games[i][j] where i is the game index (0 to 9)
		// and j is the game's details (0 - file path of the game definition; 1 - the game's name)
		String[][] games = Utils.readGames(spGamesCollection);


		// Other settings
		boolean visuals = true;
		int seed = new Random().nextInt();

		// Game and level to play
//		THESE ARE NOT USED (are overwritten below in 5.)
		int gameIdx = 6;
		int levelIdx = 0; // level names from 0 to 4 (game_lvlN.txt).
		String gameName = games[gameIdx][1];
		String game = games[gameIdx][0];
		String level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);

		String recordActionsFile = null;// "actions_" + games[gameIdx] + "_lvl"
						// + levelIdx + "_" + seed + ".txt";
						// //where to record the actions
						// executed. null if not to save.

		int levels = 5;
		int repeat = 5;
		int [] gameIndx = new int [10];
		int[] args_int = new int [args.length];
		//if there are at least 2 arguments
		if(args.length>1){
			for (int i=0;i<args.length;i++){args_int[i]=Integer.valueOf(args[i]);}
			 levels = args_int[0];
			 repeat = args_int [1];
			 //if there are more than 2 arguments the rest are for games
			 if (args.length>2){
			 	//add them all to gameindx array
				 System.arraycopy(args_int, 2, gameIndx, 0, args_int.length-2);

			 }
		}
		runGames (controllers, games, gameIndx, levels,repeat);

		// 1. This starts a game, in a level, played by two humans.
		//ArcadeMachine.playOneGameMulti(game, level1, recordActionsFile, seed);


		// 2. This plays a game in a level by the tracks. If one of the
		// players is human, change the playerID passed
		// to the runOneGame method to be that of the human player (0 or 1).
		//ArcadeMachine.runOneGame(game, level1, visuals, controllers, recordActionsFile, seed, 0);

		// 3. This replays a game from an action file previously recorded
//		 String readActionsFile = recordActionsFile;
//		 ArcadeMachine.replayGame(game, level1, visuals, readActionsFile);

		// 4. This plays a single game, in N levels, M times :
//		String level2 = new String(game).replace(gameName, gameName + "_lvl" + 1);
//		int M = 1;
//		for(int i=0; i<games.length; i++){
//			game = games[i][0];
//			gameName = games[i][1];
//			level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);
//			ArcadeMachine.runGames(game, new String[]{level1}, M, controllers, null);
//		}

		 // 5. This plays N games, in the first L levels, M times each. Actions to file optional (set saveActions to true).
//         System.out.println("Playing: " + controllers);
//		 int N = games.length, L = 5, M = 5;
//		 boolean saveActions = false;
//		 String[] levels = new String[L];
//		 String[] actionFiles = new String[L*M];
//		 for(int i = 1; i < N; ++i)
//		 {
//	         int actionIdx = 0;
//			 game = games[i][0];
//			 gameName = games[i][1];
//	         for(int j = 0; j < L; ++j)
//	         {
//	             levels[j] = game.replace(gameName, gameName + "_lvl" + j);
//	             if(saveActions) for(int k = 0; k < M; ++k)
//	                actionFiles[actionIdx++] = "actions_game_" + i + "_level_" + j + "_"  + k + ".txt";
//	         }
//		    ArcadeMachine.runGames(game, levels, M, controllers, saveActions? actionFiles:null);
//		 }
//
//        System.out.println("Playing: " + controllers2);
//        for(int i = 1; i < N; ++i)
//        {
//            int actionIdx = 0;
//            game = games[i][0];
//            gameName = games[i][1];
//            for(int j = 0; j < L; ++j)
//            {
//                levels[j] = game.replace(gameName, gameName + "_lvl" + j);
//                if(saveActions) for(int k = 0; k < M; ++k)
//                    actionFiles[actionIdx++] = "actions_game_" + i + "_level_" + j + "_"  + k + ".txt";
//            }
//            ArcadeMachine.runGames(game, levels, M, controllers2, saveActions? actionFiles:null);
//        }


        // 6. This plays a round robin style tournament between multiple tracks, in N games, first L levels, M times each.
		 // Controllers are swapped for each match as well. Actions to file optional (set saveActions to true).
//		 int N = games.length, L = 5, M = 2;
//		 boolean saveActions = false;
//		 String[] levels = new String[L];
//		 String[] actionFiles = new String[L*M];
//		 int actionIdx = 0;
//
//	     //add all controllers that should play in this array
//		 String[] cont = new String[]{doNothingController, randomController, oneStepController, sampleRHEAController, sampleMCTSController, sampleMCTSController};
//	     for(int i = 0; i < N; ++i)
//	     {
//	     	game = games[i][0];
//	     	gameName = games[i][1];
//	        for (int k = 0; k < cont.length - 1; k++) {
//	            for (int t = k + 1; t < cont.length; t++) {
//	                // set action files for the first controller order
//	                for(int j = 0; j < L; ++j){
//	                    levels[j] = game.replace(gameName, gameName + "_lvl" + j);
//	                    if(saveActions){
//	                        actionIdx = 0;
//	                        for(int p = 0; p < M; ++p) {
//	                          actionFiles[actionIdx++] = "actions_" + cont[k] + "_" + cont[t] + "_game_" + i + "_level_" + j + "_" + p + ".txt";
//	                        }
//	                    }
//	                }
//
//	                controllers = cont[k] + " " + cont[t];
//
//	                System.out.println(controllers);
//	                ArcadeMachine.runGames(game, levels, M, controllers, saveActions ? actionFiles : null);
//
//	                // reset action files for the swapped tracks
//	                if (saveActions) {
//	                    actionIdx = 0;
//	                    for (int j = 0; j < L; ++j) {
//	                        for (int p = 0; p < M; ++p) {
//	                            actionFiles[actionIdx++] = "actions_" + cont[t] + "_" + cont[k] + "_game_" + i + "_level_" + j + "_" + p + ".txt";
//	                        }
//	                    }
//	                }
//	                controllers = cont[t] + " " + cont[k];
//	                System.out.println(controllers);
//	                ArcadeMachine.runGames(game, levels, M, controllers, saveActions ? actionFiles : null);
//	            }
//	        }
//	     }



    }

    /*
    * runs a give set of games, a given number of levels and a given number of times
    * controllers = string of 2 controllers for 2 players games
    * games[][] = the games library (there are 10 games: games[i][j] where i is the game index (0 to 9) and j is the game's details (0 - file path of the game definition; 1 - the game's name))
    * gamesIndx [] = a list of the games indexes (0 to 9). if empty will run all 10 games
    * levels  = how many levels to run (0-4)
    * repeat  = how many times to run the games and levels */
    private static void runGames(String controllers,String[][] games, int gamesIndx[], int Levels, int Repreat ){
		System.out.println("Playing: " + controllers);

		//if if there are any game indexes given then add all of them from the games variable
		if (gamesIndx.length==0){
			gamesIndx = new int[games.length];
			for (int i =0; i<games.length; i++){
				gamesIndx[i]=i;
			}
		}

		int N = games.length, L = Levels, M = Repreat;
		boolean saveActions = false;
		String[] levels = new String[L];
		String[] actionFiles = new String[L*M];
		for(int i: gamesIndx)
		{
			int actionIdx = 0;
			String game = games[i][0];
			String gameName = games[i][1];
			for(int j = 0; j < L; ++j)
			{
				levels[j] = game.replace(gameName, gameName + "_lvl" + j);
				if(saveActions) for(int k = 0; k < M; ++k)
					actionFiles[actionIdx++] = "actions_game_" + i + "_level_" + j + "_"  + k + ".txt";
			}
			ArcadeMachine.runGames(game, levels, M, controllers, saveActions? actionFiles:null);
		}
	}
}
