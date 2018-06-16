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
		String RHCP = "tracks.multiPlayer.experiment.RHCP_forwardModel.Agent";
		String RHEA = "tracks.multiPlayer.experiment.RHEA_forwardModel.Agent";
		String MCTS = "tracks.multiPlayer.experiment.MCTS_forwardModel.Agent";
		String Random = "tracks.multiPlayer.experiment.Random.Agent";


		// Set here the controllers used in the games (need 2 separated by space).
		String controllers = RHCP + " " + RHCP;
        String controllers2 = RHCP + " " + RHCP;

		//Load available games
//		String spGamesCollection =  "examples/all_games_2p_test.csv";
		String spGamesCollection =  System.getProperty("user.dir")+"/examples/all_games_2p_test.csv";
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
		runGames (controllers2, games, gameIndx, levels,repeat);
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
