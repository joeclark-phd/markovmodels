package net.joeclark.proceduralgeneration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static java.lang.Integer.min;

/**
 * The reference implementation for net.joeclark.proceduralgeneration.MarkovChain.
 */
public class MultiOrderMarkovChain<T> implements MarkovChain<T> {
    /** {@value}*/
    public static final int DEFAULT_ORDER = 3;
    /** {@value}*/
    public static final double DEFAULT_PRIOR = 0.005D;
    private static final Logger logger = LoggerFactory.getLogger( MultiOrderMarkovChain.class );

    // TODO: edit README
    // TODO: add routine to train on a stream
    // TODO: make serializable and test serialization
    // TODO: complete javadocs
    // TODO: build project to qualitatively test output

    // for each observed state or sequence of states (of length at most maxOrder), store a set of possible following
    // states mapped to weights (equal to the number of times the sequence was observed, not computed probabilities,
    // as that computation would be unnecessary overhead)
    protected final Map<List<T>, Map<T,Double>> model = new HashMap<>();
    protected final Set<T> knownStates = new HashSet<>();

    protected Random random = new Random();
    protected int maxOrder = DEFAULT_ORDER;

    public MultiOrderMarkovChain() {
        logger.debug("Initialized a new MultiOrderMarkovChain instance with maxOrder={}",getMaxOrder());
    }

    public void setRandom(Random random) { this.random = random; }

    public int getMaxOrder() { return maxOrder; }

    public void setMaxOrder(int maxOrder) {
        this.maxOrder = maxOrder;
        logger.debug("maxOrder set to {} for future model training",maxOrder);
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
    public Set<T> allPossibleNext(List<T> currentSequence) {
        try {
            Map<T, Double> bestModel = bestModel(currentSequence);
            return bestModel.keySet();
        } catch(IllegalStateException e) {
            logger.trace(e.getMessage() + ": returning empty set");
            return new HashSet<>();
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

    @Override
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

    protected void implementLink(List<T> fromState, T toState) {
        logger.trace("implementing link: {} -> {}",fromState, toState);
        model.computeIfAbsent(fromState, k -> new HashMap<>()).compute(toState, (k,v) -> (v==null) ? 1D : v+1D);
    }

    /**
     * Adds a transition from every known model to every known state for which a link was not previously observed,
     * with the specified weight.  Since each real observation adds a weight of 1.0D, assigning a fraction here gives
     * each of these links a chance to occur that is less than a really-observed link.  Values of 0.001 to 0.01
     * have worked well for the author's procedural generation applications.
     *
     * <p>Note that calling this method twice has no effect unless new states have become known.  To change the
     * priors, first call {@code removeWeakLinks()} to cull the ones previously set.</p>
     *
     * @param prior the weight to be given to each new link
     */
    public void addPriors(Double prior) {
        model.forEach( (k,v) -> {
            knownStates.forEach( state -> v.putIfAbsent(state, prior) );
        });
        System.out.println(model);
    }

    /**
     * A shortcut to {@code addPriors(DEFAULT_PRIOR)}.
     */
    public void addPriors() {
        addPriors(DEFAULT_PRIOR);
    }

    /**
     * This method removes transitions with frequencies below a given threshold. Its main expected use is to remove
     * a "prior" that was previously added. Assuming the prior was less than 1, {@code removeWeakLinks(1D)} will
     * remove all transitions created by {@code addPriors()}.
     * @param threshold the minimum weight/frequency of a transition to keep in the model
     */
    public void removeWeakLinks(Double threshold) {
        model.forEach( (k,v) -> {
            v.entrySet().removeIf( transition -> transition.getValue() < threshold);
        });
    }

    /**
     * A shortcut to {@code removeWeakLinks(1D)}, this method usually serves to remove "priors" only.
     */
    public void removeWeakLinks() {
        removeWeakLinks(1D);
    }

}
