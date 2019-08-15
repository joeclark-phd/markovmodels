package net.joeclark.proceduralgeneration;

import java.util.List;
import java.util.Set;

/**
 * A Markov chain maps current states to possible future states, usually defined with probabilities.  This is useful
 * in procedural generation, for example to model which letters in a language most frequently follow a given letter, or
 * to model which weather conditions most likely follow a given weather state.  For each known state, a MarkovChain
 * knows or can calculate the possible following states, so a client should be able to traverse the "chain" as many
 * iterations as he likes (e.g. for a simulation).  In most cases these transitions will be weighted by a probability
 * distribution or observed frequencies, so not all transitions from any given state will be equally likely.
 * @param <T> the type of a "state"
 */
public interface MarkovChain<T> {
    /**
     * Tests whether the model has been initialized, populated, or trained with data.
     * @return "true" if at least one state transition is known to the model
     */
    public boolean hasModel();

    /**
     * The set of all "states" that the model is aware of.  For example, if a {@code MarkovChain<Character>} is being
     * used to generate random text, this returns the "alphabet".  If a {@code MarkovChain<String>} is being used to
     * simulate sentences, this returns the "vocabulary".
     * @return a Set of known states
     */
    public Set<T> allKnownStates();

    /**
     * Given a {@code List<T>} representing a sequence of states, this method provides the set of all the states that
     * may occur next in the chain.
     * @param currentSequence All states in this List must be known to the model, i.e., members of the set returned by
     *  {@code allKnownStates()}.
     * @return If the last state in the {@code currentSequence} is a "terminal state", that is, a
     *  state known to occur only at the end(s) of sequence(s), the return will be an empty set.
     */
    public Set<T> allPossibleNext(List<T> currentSequence);

    /**
     * Given a {@code List<T>} representing a sequence of states, this method chooses one state at random from the set
     *  of states that may occur next in the chain, without considering relative weights or probabilities.
     * @param currentSequence All states in this List must be known to the model, i.e., members of the set returned by
     *  {@code allKnownStates()}. Furthermore, the last state in the sequence must not be a "terminal state", that is,
     *  one known to occur only at the end(s) of sequence(s).
     * @return A state chosen by unweighted random draw.
     */
    public T unweightedRandomNext(List<T> currentSequence);

    /**
     * Given a {@code List<T>} representing a sequence of states, this method chooses one state at random from the set
     *  of states that may occur next in the chain, based on a set of weights or an absolute or relative probability
     *  distribution of relative or absolute probabilities known to the model.  I.e. if one state has twice the weight
     *  of another state in the transition model from the given state, it will be twice as likely to be drawn.
     * @param currentSequence All states in this List must be known to the model, i.e., members of the set returned by
     *  {@code allKnownStates()}. Furthermore, the last state in the sequence must not be a "terminal state", that is,
     *  one known to occur only at the end(s) of sequence(s).
     * @return A state chosen by weighted random draw.
     */
    public T weightedRandomNext(List<T> currentSequence);
}
