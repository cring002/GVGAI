package tracks.multiPlayer.advanced.RHCP;

import core.game.StateObservationMulti;
import core.player.AbstractMultiPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tracks.multiPlayer.tools.heuristics.StateHeuristicMulti;
import tracks.multiPlayer.tools.heuristics.WinScoreHeuristic;

import java.lang.reflect.Array;
import java.util.*;

public class Agent extends AbstractMultiPlayer {

    // variable
    private int POPULATION_SIZE = 9;
    private int SIMULATION_DEPTH = 10;
    private int CROSSOVER_TYPE = UNIFORM_CROSS;

    // set
    private int MUTATION = 1;
    private int TOURNAMENT_SIZE = 3;
    private int NO_PARENTS = 2;
    private int ELITISM = 1;

    // constants
    private final long BREAK_MS = 12;
    public static final double epsilon = 1e-6;
    static final int POINT1_CROSS = 0;
    static final int UNIFORM_CROSS = 1;

    private Individual[] population, nextPop;
    private Individual opPlan;
    private Individual opPlanM;

    private int NUM_INDIVIDUALS;
    private int[] N_ACTIONS;
    private HashMap<Integer, Types.ACTIONS>[] action_mapping;

    private ElapsedCpuTimer timer;
    private Random randomGenerator;

    private StateHeuristicMulti heuristic;
    private double acumTimeTakenEval = 0,avgTimeTakenEval = 0, avgTimeTaken = 0, acumTimeTaken = 0;
    private int numEvals = 0, numIters = 0;
    private boolean keepIterating = true;
    private long remaining;

    private boolean shift_buffer = false;
    private boolean firstIteration = true;
    private  boolean crossOverOn = true;
    private float opEvalAvg = 0f;
    private int NO_CROSS_MUTATE = 5;


    //Multiplayer game parameters
    int playerID, opponentID, noPlayers;

    /**
     * Public constructor with state observation and time due.
     *
     * @param stateObs     state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservationMulti stateObs, ElapsedCpuTimer elapsedTimer, int playerID) {
        randomGenerator = new Random();
        heuristic = new WinScoreHeuristic(stateObs);
        this.timer = elapsedTimer;

        // Get multiplayer game parameters
        this.playerID = playerID;
        noPlayers = stateObs.getNoPlayers();
        opponentID = (playerID+1)%noPlayers;

        population = new Individual[POPULATION_SIZE];
        nextPop = new Individual[POPULATION_SIZE];

        N_ACTIONS = new int[noPlayers];
        NUM_INDIVIDUALS  = POPULATION_SIZE;
        action_mapping = new HashMap[noPlayers];
    }

    /*
     This is where the *magic* happens.
     Act is called from the game and is required to return a move in a certain time from (0.04) seconds.
     Act does a few things:
        Init Pop:
            Make a new population (if first move/not using shift buffer) OR shift if using shift buffer and not first move
        Runs the evolution loop:
            Whilst there is enough time generate the next population of individuals
        Return best move:
            return the best move

     */
    @Override
    public Types.ACTIONS act(StateObservationMulti stateObs, ElapsedCpuTimer elapsedTimer) {
        timer = elapsedTimer; //Store the start time of this action selection
        avgTimeTaken = 0; //Store avg time taken per iteration
        acumTimeTaken = 0; //And total times takes
        numEvals = 0; //Number of evaluations taken
        acumTimeTakenEval = 0; //Time taken for evals (I think)
        numIters = 0; //Number of iterations taken
        keepIterating = true; //Naturally we have all of the time in the world so keep iterating from the start
        // INITIALISE POPULATION
        init_pop(stateObs);
        // RUN EVOLUTION
        remaining = timer.remainingTimeMillis();
        while (remaining > avgTimeTaken && remaining > BREAK_MS && keepIterating) {
            //While there is time left iterate the EA
            runIteration(stateObs);
            remaining = timer.remainingTimeMillis();
        }
        // RETURN ACTION
        Types.ACTIONS best = get_best_action(population);
        return best;
    }

    /**
     * Run evolutionary process for one generation
     * @param stateObs - current game state
     */
    private void runIteration(StateObservationMulti stateObs) {
        //Find out how much time we have used up
        ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

        //
        for (int i = ELITISM; i < NUM_INDIVIDUALS; i++) {
            if (remaining > 2*avgTimeTakenEval && remaining > BREAK_MS) { // if enough time to evaluate one more individual
                Individual newind;

                if(crossOverOn) {
                    newind = crossover();
                    newind = newind.mutate(MUTATION);
                } else {
                    newind = getHeavyMutatedInd();
                }

                // evaluate new individual, insert into population
                add_individual(newind, nextPop, i, stateObs);

                remaining = timer.remainingTimeMillis();

            } else {keepIterating = false; break;}
        }
        Arrays.sort(nextPop);

        population = nextPop.clone();
        /*TODO: I think this is the right place to put this, basically here we need to mutate the opponents plan,
        Evaulate them both vs the current best plan and choose the best one as the new op
        */
        //TODO: I Keep getting a time out, maytbe this is causing it, adding in a check to only do if we have time
        if (!(remaining < opEvalAvg || remaining < BREAK_MS)) {
            elapsedTimerIteration = new ElapsedCpuTimer();
            opPlanM = opPlan.mutate(NO_CROSS_MUTATE);
            double planScore = evaluate(opPlan, nextPop[0], heuristic, stateObs, 1 - playerID);
            double planMScore = evaluate(opPlan, nextPop[0], heuristic, stateObs, 1 - playerID);
            if (planMScore >= planScore) opPlan = opPlanM;
            opEvalAvg = opEvalAvg*(numIters/(numIters+1))+elapsedTimerIteration.elapsedMillis() / (numIters + 1);
        }

        numIters++;
        acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
        avgTimeTaken = acumTimeTaken / numIters;
    }

    /**
     * Evaluates an individual by rolling the current state with the actions in the individual
     * and returning the value of the resulting state;
     * @param individual - individual to be valued
     * @param heuristic - heuristic to be used for state evaluation
     * @param state - current state, root of rollouts
     * @return - value of last state reached
     */
    private double evaluate(Individual individual, Individual op,  StateHeuristicMulti heuristic, StateObservationMulti state, int playerID) {

        ElapsedCpuTimer elapsedTimerIterationEval = new ElapsedCpuTimer();

        StateObservationMulti st = state.copy();
        int i;
        for (i = 0; i < SIMULATION_DEPTH; i++) {
            double acum = 0, avg;
            if (! st.isGameOver()) {
                ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

                // Multi player advance method
                Types.ACTIONS[] advanceActs = new Types.ACTIONS[noPlayers];
                for (int k = 0; k < noPlayers; k++) {
                    if (k == playerID)
                        advanceActs[k] = action_mapping[k].get(individual.actions[i]);
                    else advanceActs[k] = action_mapping[k].get(op.actions[i]);
                }
                st.advance(advanceActs);

                acum += elapsedTimerIteration.elapsedMillis();
                avg = acum / (i+1);
                remaining = timer.remainingTimeMillis();
                if (remaining < 2*avg || remaining < BREAK_MS) break;
            } else {
                break;
            }
        }

        StateObservationMulti first = st.copy();
        double value = heuristic.evaluateState(first, playerID);

        individual.value = value;

        numEvals++;
        acumTimeTakenEval += (elapsedTimerIterationEval.elapsedMillis());
        avgTimeTakenEval = acumTimeTakenEval / numEvals;
        remaining = timer.remainingTimeMillis();

        return value;
    }

    /**
     * @return - the individual resulting from crossover applied to the specified population
     */
    private Individual getHeavyMutatedInd()
    {
        //Tournament selection for when not using crossover
        Individual[] tournament = new Individual[TOURNAMENT_SIZE];
        //Select a number of random distinct individuals for tournament and sort them based on value
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            int index = randomGenerator.nextInt(population.length);
            tournament[i] = population[index];
        }
        Arrays.sort(tournament);
        return tournament[0].copy().mutate(NO_CROSS_MUTATE);
    }
    private Individual crossover() {
        Individual newind = null;

        if (NUM_INDIVIDUALS > 1) {
            newind = new Individual(SIMULATION_DEPTH, N_ACTIONS[playerID], randomGenerator);
            Individual[] tournament = new Individual[TOURNAMENT_SIZE];
            Individual[] parents = new Individual[NO_PARENTS];

            ArrayList<Individual> list = new ArrayList<>();
            if (NUM_INDIVIDUALS > TOURNAMENT_SIZE) {
                list.addAll(Arrays.asList(population).subList(ELITISM, NUM_INDIVIDUALS));
            } else {
                list.addAll(Arrays.asList(population));
            }

            //Select a number of random distinct individuals for tournament and sort them based on value
            for (int i = 0; i < TOURNAMENT_SIZE; i++) {
                int index = randomGenerator.nextInt(list.size());
                tournament[i] = list.get(index);
                list.remove(index);
            }
            Arrays.sort(tournament);

            //get best individuals in tournament as parents
            if (NO_PARENTS <= TOURNAMENT_SIZE) {
                for (int i = 0; i < NO_PARENTS; i++) {
                    parents[i] = list.get(i);
                }
                newind.crossover(parents, CROSSOVER_TYPE);
            } else {
                System.out.println("WARNING: Number of parents must be LESS than tournament size.");
            }
        }
        return newind;
    }

    /**
     * Insert a new individual into the population at the specified position by replacing the old one.
     * @param newind - individual to be inserted into population
     * @param pop - population
     * @param idx - position where individual should be inserted
     * @param stateObs - current game state
     */
    private void add_individual(Individual newind, Individual[] pop, int idx, StateObservationMulti stateObs) {
        evaluate(newind, opPlan, heuristic, stateObs, playerID);
        pop[idx] = newind.copy();
    }

    /**
     * Initialize population
     * @param stateObs - current game state
     */
    @SuppressWarnings("unchecked")
    private void init_pop(StateObservationMulti stateObs) {
        if (shift_buffer && !firstIteration) //if using shift buffer and we have a population (e.g. not first move of the game)
        {
            firstIteration = false;
            for(int i = 0; i < population.length; i++)  population[i].shift(); //Shift the elements down by one
            //TODO: Experiment with opponent shift buffer, I think it is a very bad idea to have it so right now we don't do it
            //opPlan.shift();
            opPlan = new Individual(SIMULATION_DEPTH, N_ACTIONS[playerID+1], randomGenerator);
            return;
        }

        //Loop through all players
        for (int i = 0; i < noPlayers; i++) {
            //And get the actions they can make
            ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions(i);
            //Storing them here
            N_ACTIONS[i] = actions.size() + 1;
            action_mapping[i] = new HashMap<>();
            int k = 0;
            for (Types.ACTIONS action : actions) {
                action_mapping[i].put(k, action);
                k++;
            }
            //Add a nil action (for do nothing)
            action_mapping[i].put(k, Types.ACTIONS.ACTION_NIL);
        }

        //Make a new plan for opponent and op mutated
        //TODO:These N-ACTIONS want to be playerID+1 or 1-playerID, not sure which. Simon's codes used 1-pid so that is what I  have atm
        opPlan = new Individual(SIMULATION_DEPTH, N_ACTIONS[1 - playerID], randomGenerator);
        opPlanM = new Individual(SIMULATION_DEPTH, N_ACTIONS[1 - playerID], randomGenerator);

        //Go through the inital population and evaluate it
        //TODO:Maybe here we want to ignore the opponent to begin with?
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population[i] = new Individual(SIMULATION_DEPTH, N_ACTIONS[playerID], randomGenerator);
            evaluate(population[i], opPlan, heuristic, stateObs, playerID);
        }
        Arrays.sort(population); //population already implements comparable and is always not null (because I removed the possibility)

        //Load them into the next population
        for (int i = 0; i < NUM_INDIVIDUALS; i++) {
            if (population[i] != null)  nextPop[i] = population[i].copy();
        }
    }

    /**
     * @param pop - last population obtained after evolution
     * @return - first action of best individual in the population (found at index 0)
     */
    private Types.ACTIONS get_best_action(Individual[] pop) {
        int bestAction = pop[0].actions[0];
        return action_mapping[playerID].get(bestAction);
    }
}