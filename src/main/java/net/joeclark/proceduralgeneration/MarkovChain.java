package net.joeclark.proceduralgeneration;

import java.util.List;
import java.util.Set;

/**
 * A Markov chain maps current states to possible future states, usually defined with probabilities.  This is useful
 * in procedural generation, for example to model which letters in a language most frequently follow a given letter, or
 * to model which weather conditions most likely follow a given weather state.  For each known state, a net.joeclark.proceduralgeneration.MarkovChain
 * knows or can calculate the possible following states, so a client should be able to traverse the "chain" as many
 * iterations as he likes (e.g. for a simulation).
 * @param <T> the type of a "state"
 */
public interface MarkovChain<T> {
    public boolean hasModel();
    public Set<T> allKnownStates();
    public Set<T> allPossibleNext(List<T> currentSequence);
    public T unweightedRandomNext(List<T> currentSequence);
    public T weightedRandomNext(List<T> currentSequence);
}
