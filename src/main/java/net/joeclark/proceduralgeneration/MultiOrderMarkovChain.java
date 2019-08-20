package net.joeclark.proceduralgeneration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.Integer.min;

/**
 * The reference implementation for net.joeclark.proceduralgeneration.MarkovChain.
 *
 * <p>This implementation offers multi-order models with a Katz back-off.  What that means is, if {@code maxOrder}
 * is greater than 1, multiple models may match any given sequence. For example, if you provide the sequence
 * {@code {'J','A','V','A'}}, and {@code maxOrder==3}, we will first check to see if we have a model for states
 * that follow {@code {'A','V','A'}}. If none is found, for example because that sequence was never seen in training
 * data, we check for a model for states that follow {@code {'V','A'}}. Failing that too, we fall back on the model
 * of transitions from the state {@code 'A'}, which is certain to exist if {@code 'A'} is a valid input to the
 * relevant method ({@code weightedRandomNext} or {@code unWeightedRandomNext}).</p>
 *
 * <p>Another feature that may be desired in procedural generation applications is the option to inject some "true
 * randomness" in the form of "prior" relative probabilities, i.e., small weights given to transitions <i>not</i>
 * observed in training data. These can make up for the limitations of a training dataset and enable the generation
 * of sequences not observed in training.  Use the {@code addPriors()} method after training your model to take
 * advantage of this.</p>
 *
 * If multi-order models are not desired, set {@code maxOrder} to 1.  If priors are not desired, do nothing; they will
 * not be added by default.
 *
 * @param <T> the type of a "state". Must be serializable so the model can be serializable.
 */
public class MultiOrderMarkovChain<T extends Serializable> implements MarkovChain<T>, Serializable {
    private static final Logger logger = LoggerFactory.getLogger( MultiOrderMarkovChain.class );
    private static final long serialVersionUID = 1L;

    /** {@value}*/
    public static final int DEFAULT_ORDER = 3;
    /** {@value}*/
    public static final double DEFAULT_PRIOR = 0.005D;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiOrderMarkovChain<?> that = (MultiOrderMarkovChain<?>) o;
        return maxOrder == that.maxOrder && numTrainedSequences == that.numTrainedSequences && Objects.equals(model, that.model) && Objects.equals(knownStates, that.knownStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, knownStates, maxOrder, numTrainedSequences);
    }

    // TODO: edit README
    // TODO: build project to qualitatively test output
    // TODO: prep for maven central
    // for each observed state or sequence of states (of length at most maxOrder), store a set of possible following
    // states mapped to weights (equal to the number of times the sequence was observed, not computed probabilities,
    // as that computation would be unnecessary overhead)
    protected final Map<List<T>, Map<T,Double>> model = new HashMap<>();
    protected final Set<T> knownStates = new HashSet<>();

    protected Random random = new Random();
    protected int maxOrder = DEFAULT_ORDER;
    protected int numTrainedSequences = 0;

    public MultiOrderMarkovChain() {
        logger.debug("Initialized a new MultiOrderMarkovChain instance with maxOrder={}",getMaxOrder());
    }

    public void setRandom(Random random) { this.random = random; }

    public int getMaxOrder() { return maxOrder; }

    public void setMaxOrder(int maxOrder) {
        this.maxOrder = maxOrder;
        logger.debug("maxOrder set to {} for future model training",maxOrder);
    }

    public int getNumTrainedSequences() { return numTrainedSequences; }

    public int getNumKnownState() { return knownStates.size(); }

    /**
     * Reveals the model stored in the instance, in its raw form.
     * @return A map of sequences to a map of states to weights; for each sequence, the inner map contains weights
     * (relative probabilities) for each possible transition from the end of that sequence.
     */
    public Map<List<T>, Map<T,Double>> getModel() { return model; }


    public MultiOrderMarkovChain<T> withRandom(Random random) {
        this.setRandom(random);
        return this;
    }
    public MultiOrderMarkovChain<T> withMaxOrder(int maxOrder) {
        this.setMaxOrder(maxOrder);
        return this;
    }
    public MultiOrderMarkovChain<T> andTrain(Stream<List<T>> stream) {
        this.train(stream);
        return this;
    }
    public MultiOrderMarkovChain<T> andAddPriors(Double prior) {
        this.addPriors(prior);
        return this;
    }
    public MultiOrderMarkovChain<T> andAddPriors() {
        this.addPriors();
        return this;
    }

    @Override
    public Set<T> allKnownStates() {
        return knownStates;
    }

    /**
     * Given a {@code List<T>} representing a sequence of states, this method provides the set of all the states that
     * may occur next in the chain.
     *
     * <p>This implementation offers multi-order models with a Katz back-off.  What that means is, if {@code maxOrder}
     * is greater than 1, multiple models may match your sequence. For example, if you provide the sequence
     * {@code {'J','A','V','A'}}, and {@code maxOrder==3}, we will first check to see if we have a model for states
     * that follow {@code {'A','V','A'}}. If none is found, we check for a model for states that follow
     * {@code {'V','A'}}. Failing that, we fall back on the model of transitions from the state {@code 'A'}.</p>
     * @param currentSequence All states in this List must be known to the model, i.e., members of the set returned by
     *  {@code allKnownStates()}.
     * @return A set of states.  If the last state in the {@code currentSequence} is a "terminal state", that is, a
     *  state known to occur only at the end(s) of sequence(s), the return will be an empty set.
     */
    @Override
    public Set<T> allPossibleNext(List<T> currentSequence) {
        try {
            Map<T, Double> bestModel = bestModel(currentSequence);
            if( bestModel==null ) { throw new NullPointerException(); }
            return bestModel.keySet();
        } catch(IllegalStateException e) {
            logger.trace("{}: returning empty set",e.getMessage());
            return new HashSet<>();
        }
    }

    @Override
    public T weightedRandomNext(List<T> currentSequence) {
        Map<T,Double> bestModel = bestModel(currentSequence);
        if( bestModel==null ) { throw new NullPointerException(); }
        double sumOfWeights = bestModel.values().stream().reduce(0.0D, Double::sum);
        double randomRoll = sumOfWeights * random.nextDouble();
        for(Map.Entry<T,Double> link: bestModel.entrySet()) {
            if(randomRoll > link.getValue()) {
                randomRoll -= link.getValue();
            } else {
                return link.getKey();
            }
        }
        logger.warn("something went wrong in weighted random draw and NULL was returned instead of a known state");
        return null;
    }

    @Override
    public T unweightedRandomNext(List<T> currentSequence) {
        Map<T,Double> bestModel = bestModel(currentSequence);
        if( bestModel==null ) { throw new NullPointerException(); }
        List<T> possibles = new ArrayList<>(bestModel.keySet());
        return possibles.get(random.nextInt(possibles.size()));
    }

    private Map<T,Double> bestModel(List<T> currentSequence) {
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

    /**
     * Given a {@code List<T>}, adds all transitions in the sequence (including transitions from sequences of lengths
     * up to {@code maxOrder}) to the model, adding 1.0 to their weights.  All states in the sequence are added to
     * the set of "known states" which serves as the alphabet or vocabulary of possible sequences. If the input list
     * contains only two states, only a single transition is added to the model.
     * @param sequence a List of 2 or more "states"
     */
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
                    incrementLink(multiplePredecessors, sequence.get(j - 1), 1D);
                }
            }
        }

        numTrainedSequences += 1;
    }

    private void incrementLink(List<T> fromState, T toState, double incrementalWeight) {
        logger.trace("incrementing link {} -> {} by {}",fromState, toState, incrementalWeight);
        model.computeIfAbsent(fromState, k -> new HashMap<>()).compute(toState, (k,v) -> (v==null) ? incrementalWeight : v+incrementalWeight);
    }

    /**
     * Directly specify a transition or link from a sequence of states (which could be a List containing a single state)
     * to another state, with an arbitrary weight.  This allows you to specify your model precisely, as an alternative to
     * using the partially-automated {@code train} or {@code addSequence} methods.
     * @param fromState a sequence of states preceding the transition. for a simple Markov chain this could be a single
     *                  state wrapped in a java List
     * @param toState a state that may follow the sequence given by 'fromState'
     * @param weight the weight or (relative) probability of this transition. Weights are relative to other transitions
     *               from the same 'fromState', so they are not constrained by any need to add up to 1.
     */
    public void specifyLink(List<T> fromState, T toState, double weight) {
        logger.trace("specifying link {} -> {} with weight {}", fromState, toState, weight);
        knownStates.addAll(fromState);
        knownStates.add(toState);
        HashMap<T,Double> link = new HashMap<>();
        link.put(toState,weight);
        model.put(fromState,link);
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
        logger.debug("Adding unobserved state transitions with 'prior' weight {}",prior);
        model.forEach( (k,v) -> knownStates.forEach(state -> v.putIfAbsent(state, prior) ));
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
        logger.debug("Removing all links with weight less than {}",threshold);
        model.forEach( (k,v) -> v.entrySet().removeIf(transition -> transition.getValue() < threshold));
    }

    /**
     * A shortcut to {@code removeWeakLinks(1D)}, this method usually serves to remove "priors" only.
     */
    public void removeWeakLinks() {
        removeWeakLinks(1D);
    }

    /**
     * Trains a model on a stream of input data, essentially by calling {@code addSequence()} repeatedly, with a bit
     * of additional logging.
     * @param stream A stream of {@code List<T>} sequences, each of which must contain at least two states, or it will
     *               be ignored.
     */
    public void train(Stream<List<T>> stream) {
        logger.info("Beginning to ingest a stream of training data...");
        int previouslyTrainedSequences = numTrainedSequences;
        stream.filter(sequence -> sequence.size() >= 2 ) // ignore short sequences so they don't blow up a large batch of training
              .forEach( this::addSequence );
        logger.info("...finished training on a stream of {} sequences. Model includes {} known states.",numTrainedSequences-previouslyTrainedSequences,knownStates.size());
    }

}
