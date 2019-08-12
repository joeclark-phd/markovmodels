package net.joeclark.proceduralgeneration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * A very simple implementation of net.joeclark.proceduralgeneration.MarkovChain that stores the possible outcome states for each given state,
 * but doesn't learn relative probabilities.
 */
public class MultiOrderMarkovChain<T> implements MarkovChain<T> {


    // for each observed state or sequence of states (of length at most maxOrder),
    // store a set of possible following states (as a list for slower writes, faster reads)
    private final Map<List<T>, List<T>> stateSet = new HashMap<>();
    private final Set<T> knownStates = new HashSet<>();

    private Random random = new Random();
    private int maxOrder = 3;

    public void setRandom(Random random) { this.random = random; }
    //TODO: set/get maxOrder

    @Override
    public Set<T> allKnownStates() {
        return knownStates;
    }

    @Override
    public Set<T> allPossibleNext(T currentState) {
        List<T> currentList = new ArrayList<>(Collections.singletonList(currentState));
        if(!stateSet.containsKey(currentList)) {
            throw new IllegalArgumentException("the given state is unknown to this model");
        } else {
            return new HashSet<>(stateSet.get(currentList));
        }
    }

    @Override
    public T randomNext(T currentState) {
        List<T> currentList = new ArrayList<>(Collections.singletonList(currentState));
        if( !stateSet.containsKey(currentList) ) {
            throw new IllegalArgumentException("the given state is unknown to this model");
        } else {
            List<T> possibles = stateSet.get(currentList);
            if( possibles.isEmpty() ) {
                throw new IllegalStateException("there are no known links possible from this state");
            } else {
                return possibles.get(random.nextInt(possibles.size()));
            }
        }
    }

    public boolean hasModel() {
        return !stateSet.isEmpty();
    }

    public void addSequence(List<T> sequence) {
        // All tokens/states in the sequence should be in the set of all known states.
        // That set acts like an "alphabet" of tokens (possible states) that can form sequences based on this model.
        knownStates.addAll(sequence);

        if(sequence.size()<2) {
            throw new IllegalArgumentException("input sequence must include at least two tokens");
        } else {
            for(int j=sequence.size();j>1;j--) {
                for (int i = 0; ((i < maxOrder) && (i + 1 < j)); i++) {
                    ArrayList<T> multiplePredecessors = new ArrayList<>(sequence.subList(j - 2 - i, j - 1));
                    //System.out.println("i=" + i + ": multiplePredecessors: " + multiplePredecessors);
                    implementLink(multiplePredecessors, sequence.get(j - 1));
                }
            }
        }
    }

    private void implementLink(List<T> fromState, T toState) {
        System.out.println("implementing link: " + fromState + " -> " + toState);
        List<T> knownFollowers = stateSet.computeIfAbsent(fromState, k -> new ArrayList<>());
        // The set of possible outcomes is stored as a List, for faster random draws, but we want it to act like a Set.
        if(!knownFollowers.contains(toState)) { knownFollowers.add(toState); }
    }

}
