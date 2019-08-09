package net.joeclark.proceduralgeneration;

import java.util.ArrayList;
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
public class EqualProbabilityMarkovChain<T> implements MarkovChain<T> {

    // for each state, store a set of possible following states. storing it as a list for slower writes, faster reads
    private final Map<T, List<T>> stateSet = new HashMap<>();
    private Random random = new Random();

    public void setRandom(Random random) { this.random = random; }

    @Override
    public Set<T> allKnownStates() {
        return stateSet.keySet();
    }

    @Override
    public Set<T> allPossibleNext(T currentState) {
        if(!stateSet.containsKey(currentState)) {
            throw new IllegalArgumentException("the given state is unknown to this model");
        } else {
            // will this return null or throw an exception if currentState is unknown?
            return new HashSet<T>(stateSet.get(currentState));
        }
    }

    @Override
    public T randomNext(T currentState) {
        if( !stateSet.containsKey(currentState) ) {
            throw new IllegalArgumentException("the given state is unknown to this model");
        } else {
            List<T> possibles = stateSet.get(currentState);
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

    public void addLink(T fromState, T toState) {
        List<T> knownStates = stateSet.computeIfAbsent(fromState, k -> new ArrayList<>());
        // knownStates is a List that we want to act like a Set. this may be slower to write, but faster to do random draws, which is likely to be our typical "read"
        if(!knownStates.contains(toState)) { knownStates.add(toState); }

        // the 'TO' state should also exist in the chain
        stateSet.computeIfAbsent(toState, k->new ArrayList<>());
        System.out.println(stateSet);
    }




}
