package tracks.singlePlayer;

import java.util.Random;

import core.logging.Logger;
import tools.Utils;
import tracks.ArcadeMachine;
import java.io.*;

/**
 * Created with IntelliJ IDEA. User: Diego Date: 04/10/13 Time: 16:29 This is a
 * Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class SpotTheBot {

    public static void main(String[] args) {
		//Load available games
		String spGamesCollection =  "examples/all_games_sp.csv";
		String[][] games = Utils.readGames(spGamesCollection);

		//Game settings
		boolean visuals = true;
		int seed = new Random().nextInt();

		// Game and level to play
		int gameIdx = 0;
		int levelIdx = 0; // level names from 0 to 4 (game_lvlN.txt).
		int userID = 0;
		boolean aiplayer = false;

		File file = new File("config.txt");
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String st;
			while ((st = br.readLine()) != null)
				if (st.contains("id"))
					userID = getValueFromString(st);
				else if(st.contains("game"))
					gameIdx = getValueFromString(st);
				else if(st.contains("level"))
					levelIdx = getValueFromString(st);
				else if(st.contains("ai"))
					aiplayer = true;

		} catch (java.io.FileNotFoundException e) {

		} catch (java.io.IOException e) {

		}

		String aiString = "";
		if(aiplayer){
			if(userID == 0)  aiString = "tracks.singlePlayer.simple.sampleRandom.Agent";
			else if(userID == 1) aiString = "tracks.singlePlayer.simple.sampleonesteplookahead.Agent";
			else if(userID == 2) aiString = "tracks.singlePlayer.simple.greedyTreeSearch.Agent";
			else if(userID == 3) aiString = "tracks.singlePlayer.advanced.sampleMCTS.Agent";
			else if(userID == 4) aiString = "tracks.singlePlayer.advanced.sampleRS.Agent";
			else if(userID == 5) aiString = "tracks.singlePlayer.advanced.sampleRHEA.Agent";
			else if(userID == 6) aiString = "tracks.singlePlayer.advanced.olets.Agent";
		}

		String gameName = games[gameIdx][1];
		String game = games[gameIdx][0];
		String level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);

		String recordActionsFile =  aiplayer == false ? "game_logs/human_"+ gameIdx + "_" + levelIdx + "_" + userID + "_" + seed + ".txt"
				: "game_logs/ai_"+ gameIdx + "_" + levelIdx + "_" + userID + "_" + seed  + ".txt" ;

		if(!aiplayer) ArcadeMachine.playOneGame(game, level1, recordActionsFile, seed);
		else ArcadeMachine.runOneGame(game, level1, visuals, aiString, recordActionsFile, seed, 0);
    }

	public static int getValueFromString(String str)
	{
		String substring = "";
		boolean recordSubstring = false;
		for(int i = 0; i < str.length(); i++)
		{
			if(recordSubstring) substring += str.charAt(i);
			if (str.charAt(i) == ' ') recordSubstring = true;
		}
		return Integer.parseInt(substring);
	}
}
