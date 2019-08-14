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

import static java.lang.Integer.min;

/**
 * A very simple implementation of net.joeclark.proceduralgeneration.MarkovChain that stores the possible outcome states for each given state,
 * but doesn't learn relative probabilities.
 */
public class MultiOrderMarkovChain<T> implements MarkovChain<T> {
    private static final Logger logger = LoggerFactory.getLogger( MultiOrderMarkovChain.class );


    // TODO: edit README
    // TODO: add weighted and unweighted random draw
    // TODO: implement priors
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
    public T weightedRandomNext(List<T> currentSequence) {
        Map<T,Double> bestModel = bestModel(currentSequence);
        double sumOfWeights = bestModel.values().stream().reduce(0.0D, (a,b)->a+b);
        double randomRoll = sumOfWeights * random.nextDouble();
        for(T t: bestModel.keySet()) {
            if(randomRoll>bestModel.get(t)) {
                randomRoll -= bestModel.get(t);
            } else {
                return t;
            }
        }
        logger.warn("something went wrong in weighted random draw and NULL was returned instead of a known state");
        return null;
    }

    @Override
    public T unweightedRandomNext(List<T> currentSequence) {
        Map<T,Double> bestModel = bestModel(currentSequence);
        List<T> possibles = new ArrayList<>(bestModel.keySet());
        return possibles.get(random.nextInt(possibles.size()));
    }

    public Map<T,Double> bestModel(List<T> currentSequence) {
        if(!knownStates.containsAll(currentSequence)) {
            throw new IllegalArgumentException("at least one of the states in the sequence is unknown to the model");
        } else {
            if (!model.containsKey(currentSequence.subList(currentSequence.size()-1,currentSequence.size()))) {
                // the final state is known but has no "downstream" transitions, i.e. it only appeared at the end(s) of sequence(s)
                throw new IllegalStateException("there are no known links possible from the end of this sequence");
            } else {
                Map<T,Double> bestModel = null;
                int o = min(maxOrder,currentSequence.size());
                // Find the transition model for the longest subsequence (up to length maxOrder) that the model has observed in training.
                // For example, if the sequence is ['m','a','r','k'] and maxOrder is 3, we first look for a model matching ['a','r','k'],
                // and if none is found (because the link was never observed), we look for a model for ['r','k'], and failing that,
                // we are certain to have a model for ['k'] if the tests above have been passed. I have heard this called a "Katz back-off".
                while(bestModel==null && o>0) {
                    if (model.containsKey(currentSequence.subList(currentSequence.size()-o,currentSequence.size()))) {
                        bestModel = model.get(currentSequence.subList(currentSequence.size()-o,currentSequence.size()));
                    } else {
                        o--;
                    }
                }
                return bestModel;
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
