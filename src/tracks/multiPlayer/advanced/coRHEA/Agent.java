package tracks.multiPlayer.advanced.coRHEA;

import core.game.StateObservationMulti;
import core.player.AbstractMultiPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tracks.multiPlayer.tools.heuristics.StateHeuristicMulti;
import tracks.multiPlayer.tools.heuristics.WinScoreHeuristic;

import java.util.*;

public class Agent extends AbstractMultiPlayer {

    // variable
    private int POPULATION_SIZE = 9;
    private int SIMULATION_DEPTH = 10;
    private int CROSSOVER_TYPE = UNIFORM_CROSS;
    private double DISCOUNT = 1; //0.99;

    // set
    //    private boolean REPLACE = false;
    private int MUTATION = 1;
    private int TOURNAMENT_SIZE = 2;
    private int NO_PARENTS = 2;
    private int ELITISM = 1;

    // constants
    private final long BREAK_MS = 10;
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

    private boolean shift_buffer = true;
    private boolean firstIteration = true;
    private  boolean crossOverOn = false;


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
    }

    @Override
    public Types.ACTIONS act(StateObservationMulti stateObs, ElapsedCpuTimer elapsedTimer) {
        this.timer = elapsedTimer;
        avgTimeTaken = 0;
        acumTimeTaken = 0;
        numEvals = 0;
        acumTimeTakenEval = 0;
        numIters = 0;
        remaining = timer.remainingTimeMillis();
        NUM_INDIVIDUALS = 0;
        keepIterating = true;

        // INITIALISE POPULATION
        init_pop(stateObs);

        // RUN EVOLUTION
        remaining = timer.remainingTimeMillis();
        while (remaining > avgTimeTaken && remaining > BREAK_MS && keepIterating) {
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
        ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

        if (NUM_INDIVIDUALS > 1) {
            for (int i = ELITISM; i < NUM_INDIVIDUALS; i++) {
                if (remaining > 2*avgTimeTakenEval && remaining > BREAK_MS) { // if enough time to evaluate one more individual
                    Individual newind;

                    newind = crossover();

                    newind = newind.mutate(MUTATION);

                    // evaluate new individual, insert into population
                    add_individual(newind, nextPop, i, stateObs);

                    remaining = timer.remainingTimeMillis();

                } else {keepIterating = false; break;}
            }
            Arrays.sort(nextPop, new Comparator<Individual>() {
                @Override
                public int compare(Individual o1, Individual o2) {
                    if (o1 == null && o2 == null) {
                        return 0;
                    }
                    if (o1 == null) {
                        return 1;
                    }
                    if (o2 == null) {
                        return -1;
                    }
                    return o1.compareTo(o2);
                }
            });
        } else if (NUM_INDIVIDUALS == 1){
            Individual newind = new Individual(SIMULATION_DEPTH, N_ACTIONS[playerID], randomGenerator).mutate(MUTATION);
            evaluate(newind, opPlan, heuristic, stateObs, playerID);
            if (newind.value > population[0].value)
                nextPop[0] = newind;
        }

        population = nextPop.clone();
        /*TODO: I think this is the right place to put this, basically here we need to mutate the oponents plan,
        Evaulate them both vs the current best plan and choose the best one as the neew op
        */
        opPlanM = opPlan.mutate(MUTATION);

        double planScore =  evaluate(opPlan, population[0], heuristic, stateObs, playerID+1);
        double planMScore =  evaluate(opPlan, population[0], heuristic, stateObs, playerID+1);

        if(planMScore >= planScore) opPlan = opPlanM;

        numIters++;
        acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
        avgTimeTaken = acumTimeTaken / numIters;
    }

    /**
     * Evaluates an individual by rolling the current state with the actions in the individual
     * and returning the value of the resulting state; random action chosen for the opponent
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

        // Apply discount factor
        value *= Math.pow(DISCOUNT,i);

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
    private Individual crossover() {
        //todo: Have a method to turn this off and so something inexpensive instead
        Individual newind = null;
        if(!crossOverOn)
        {
            //Tournament selection for when not using crossover
            Individual[] tournament = new Individual[TOURNAMENT_SIZE];
            //Select a number of random distinct individuals for tournament and sort them based on value
            for (int i = 0; i < TOURNAMENT_SIZE; i++) {
                int index = randomGenerator.nextInt(population.length);
                tournament[i] = population[index];
            }
            Arrays.sort(tournament);
            return tournament[0].copy();
        }
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
    //TODO: SHIFT BUFFER
    private void init_pop(StateObservationMulti stateObs) {
        if (shift_buffer && !firstIteration)
        {
            firstIteration = false;
            for(int i = 0; i < population.length; i++)
            {
                population[i].shift();
            }
            //TODO: Experiment with opponenet shift buffer
            //I think it is a very bad idea to have it
            //opPlan.shift();
            opPlan = new Individual(SIMULATION_DEPTH, N_ACTIONS[playerID+1], randomGenerator);
            return;
        }

        double remaining = timer.remainingTimeMillis();

        N_ACTIONS = new int[noPlayers];
        action_mapping = new HashMap[noPlayers];

        for (int i = 0; i < noPlayers; i++) {
            ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions(i);
            N_ACTIONS[i] = actions.size() + 1;
            action_mapping[i] = new HashMap<>();
            int k = 0;
            for (Types.ACTIONS action : actions) {
                action_mapping[i].put(k, action);
                k++;
            }
            action_mapping[i].put(k, Types.ACTIONS.ACTION_NIL);
        }

        //todo:These N-ACTIONS prob want to be playerID+1 or 1-playerID, not sure which. Simons codes used 1-pid so that is whatI  have atm
        opPlan = new Individual(SIMULATION_DEPTH, N_ACTIONS[1 - playerID], randomGenerator);
        opPlanM = new Individual(SIMULATION_DEPTH, N_ACTIONS[1 - playerID], randomGenerator);
//        opPlan = new Individual(SIMULATION_DEPTH, N_ACTIONS[playerID+1], randomGenerator);
//        opPlanM = new Individual(SIMULATION_DEPTH, N_ACTIONS[playerID+1], randomGenerator);

        population = new Individual[POPULATION_SIZE];
        nextPop = new Individual[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            if (i == 0 || remaining > avgTimeTakenEval && remaining > BREAK_MS) {
                population[i] = new Individual(SIMULATION_DEPTH, N_ACTIONS[playerID], randomGenerator);
                evaluate(population[i], opPlan, heuristic, stateObs, playerID);
                remaining = timer.remainingTimeMillis();
                NUM_INDIVIDUALS = i+1;
            } else {break;}
        }

//        if (NUM_INDIVIDUALS > 1)
//            Arrays.sort(population, new Comparator<Individual>() {
//                @Override
//                public int compare(Individual o1, Individual o2) {
//                    if (o1 == null && o2 == null) {
//                        return 0;
//                    }
//                    if (o1 == null) {
//                        return 1;
//                    }
//                    if (o2 == null) {
//                        return -1;
//                    }
//                    return o1.compareTo(o2);
//                }});
        for (int i = 0; i < NUM_INDIVIDUALS; i++) {
            if (population[i] != null)
                nextPop[i] = population[i].copy();
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
