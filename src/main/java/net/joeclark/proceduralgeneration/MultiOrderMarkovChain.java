package net.joeclark.proceduralgeneration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
    private static final Logger logger = LoggerFactory.getLogger( MultiOrderMarkovChain.class );


    // TODO: edit README
    // TODO: add weighted and unweighted random draw
    // TODO: make serializable
    // TODO: complete javadocs


    // for each observed state or sequence of states (of length at most maxOrder), store a set of possible following
    // states mapped to weights (equal to the number of times the sequence was observed, not computed probabilities,
    // as that computation would be unnecessary overhead)
    private final Map<List<T>, Map<T,Double>> model = new HashMap<>();
    private final Set<T> knownStates = new HashSet<>();

    private Random random = new Random();
    private int maxOrder = 3;

    public void setRandom(Random random) { this.random = random; }
    public void setMaxOrder(int maxOrder) {
        this.maxOrder = maxOrder;
        logger.debug("maxOrder set to {} for future model training",maxOrder);
    }
    public int getMaxOrder() { return maxOrder; }


    public MultiOrderMarkovChain() {
        logger.debug("Initialized a new MultiOrderMarkovChain instance with maxOrder={}",getMaxOrder());
    }
    public MultiOrderMarkovChain<T> withRandom(Random random) {
        this.setRandom(random);
        return this;
    }
    public MultiOrderMarkovChain<T> withMaxOrder(int maxOrder) {
        this.setMaxOrder(maxOrder);
        return this;
    }



    @Override
    public Set<T> allKnownStates() {
        return knownStates;
    }

    @Override
    public Set<T> allPossibleNext(T currentState) {
        if(!knownStates.contains(currentState)) {
            throw new IllegalArgumentException("the given state is unknown to this model");
        } else {
            List<T> currentList = new ArrayList<>(Collections.singletonList(currentState));
            if (!model.containsKey(currentList)) {
                // the state is known but has no "downstream" connections, i.e. it only appeared at the end(s) of sequence(s)
                return new HashSet<>();
            } else {
                return model.get(currentList).keySet();
            }
        }
    }

    @Override
    public T randomNext(T currentState) {
        if(!knownStates.contains(currentState)) {
            throw new IllegalArgumentException("the given state is unknown to this model");
        } else {
            List<T> currentList = new ArrayList<>(Collections.singletonList(currentState));
            if (!model.containsKey(currentList)) {
                // the state is known but has no "downstream" connections, i.e. it only appeared at the end(s) of sequence(s)
                throw new IllegalStateException("there are no known links possible from this state");
            } else {
                Set<T> possibles = model.get(currentList).keySet();
                List<T> possiblesListed = new ArrayList<>(possibles);
                return possiblesListed.get(random.nextInt(possiblesListed.size()));
            }
        }
    }

    public boolean hasModel() {
        return !model.isEmpty();
    }

    public void addSequence(List<T> sequence) {
        logger.trace("addSequence called with {}",sequence);
        // All tokens/states in the sequence should be in the set of all known states.
        // That set acts like an "alphabet" of tokens (possible states) that can form sequences based on this model.
        knownStates.addAll(sequence);

        if(sequence.size()<2) {
            throw new IllegalArgumentException("input sequence must include at least two tokens");
        } else {
            for(int j=sequence.size();j>1;j--) {
                for (int i = 0; ((i < maxOrder) && (i + 1 < j)); i++) {
                    ArrayList<T> multiplePredecessors = new ArrayList<>(sequence.subList(j - 2 - i, j - 1));
                    implementLink(multiplePredecessors, sequence.get(j - 1));
                }
            }
        }
    }

    private void implementLink(List<T> fromState, T toState) {
        logger.trace("implementing link: {} -> {}",fromState, toState);
        model.computeIfAbsent(fromState, k -> new HashMap<>()).compute(toState, (k,v) -> (v==null) ? 1D : v+1D);
    }

}
