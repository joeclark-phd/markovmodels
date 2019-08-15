package net.joeclark.proceduralgeneration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import static net.joeclark.proceduralgeneration.MultiOrderMarkovChain.DEFAULT_PRIOR;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MultiOrderMarkovChain...")
class MultiOrderMarkovChainTest {

    @Test
    @DisplayName("Can initialize a chain of Characters")
    void CanInitializeAChainOfCharacters() {
        MultiOrderMarkovChain<Character> chain = new MultiOrderMarkovChain<>();
        chain.addSequence(new ArrayList<>(Arrays.asList('V','W','X','Y','Z')));
        chain.addSequence(new ArrayList<>(Arrays.asList('X','Y','Z')));
    }

    @Test
    @DisplayName("Can initialize a chain of Integers")
    void CanInitializeAChainOfIntegers() {
        MultiOrderMarkovChain<Integer> chain = new MultiOrderMarkovChain<>();
        chain.addSequence(new ArrayList<>(Arrays.asList(1,1,2,3,5,8,13)));
    }

    @Test
    @DisplayName("Can initialize and train a chain of strings on string sequences")
    void CanTrainAChainOnStringSequences() {
        MultiOrderMarkovChain<String> chain = new MultiOrderMarkovChain<>();
        chain.addSequence(new ArrayList<>(Arrays.asList("how","much","wood","would","a","woodchuck","chuck")));
        chain.addSequence(new LinkedList<>(Arrays.asList("if","a","woodchuck","could","chuck","wood")));
    }

    @Test
    @DisplayName("Can set maxOrder with a builder-constructor function")
    void CanSetMaxOrderWithBuilderConstructor() {
        MultiOrderMarkovChain<Float> chain = new MultiOrderMarkovChain<Float>().withMaxOrder(2);
        chain.addSequence(Arrays.asList(0.01F,3.14F,1.0F,1.414F,2.718F));
    }


    @Nested
    @DisplayName("When initialized but not trained...")
    class WhenUnTrained {

        private MultiOrderMarkovChain<Object> chain;

        @BeforeEach
        void InitializeButDontTrain() {
            chain = new MultiOrderMarkovChain<Object>();
        }

        @Test
        @DisplayName("hasModel() should return false")
        void HasModelShouldBeFalse() {
            assertEquals(false,chain.hasModel(),"hasModel() returned true despite model being empty/untrained");
        }

        @Test
        @DisplayName("allKnownStates should return empty set")
        void AllKnownStatesShouldReturnNull() {
            assertTrue(chain.allKnownStates().isEmpty());
        }

    }

    @Nested
    @DisplayName("Once trained...")
    class OnceTrained {

        private MultiOrderMarkovChain<String> chain;

        @BeforeEach
        void trainChain() {
            chain = new MultiOrderMarkovChain<>();
            chain.addSequence(new ArrayList<>(Arrays.asList("one","small","step","for","man")));
            chain.addSequence(new LinkedList<>(Arrays.asList("one","giant","leap","for","mankind")));
        }

        @Test
        @DisplayName("hasModel() should return true")
        void HasModelShouldBeTrue() {
            assertEquals(true,chain.hasModel(),"hasModel() returned false despite model being trained");
        }

        @Test
        @DisplayName("allKnownStates should include 'from' states")
        void AllKnownStatesShouldIncludeAllFromStates() {
            assertTrue(chain.allKnownStates().contains("one"));

        }

        @Test
        @DisplayName("allKnownStates shoudl includes states that are both 'from' and 'to' states")
        void AllKnownStatesShoudlIncludeToAndFromStates() {
            assertTrue(chain.allKnownStates().contains("for"));
        }

        @Test
        @DisplayName("allKnownStates should include all 'to' states")
        void AllKnownStatesShouldIncludeAllToStates() {
            assertTrue(chain.allKnownStates().contains("mankind"));
        }

        @Test
        @DisplayName("unweightedRandomNext choice should be one of the trained links")
        void RandomNextShouldBeOneOfTheTrainedLinks() {
            String next = chain.unweightedRandomNext(Arrays.asList("one"));
            assertTrue(next.equals("small") || next.equals("giant"));
        }

        @Test
        @DisplayName("weightedRandomNext choice should be one of the trained links (assuming priors haven't been added)")
        void WeightedRandomNextShouldBeOneOfTheTrainedLinks() {
            String next = chain.weightedRandomNext(Arrays.asList("one"));
            assertTrue(next.equals("small") || next.equals("giant"));
        }

        @Test
        @DisplayName("unweightedRandomNext should throw exception if state doesn't exist")
        void RandomNextShouldThrowExceptionIfStateUnknown() {
            assertThrows(IllegalArgumentException.class,() -> chain.unweightedRandomNext(Arrays.asList("moon")));
        }

        @Test
        @DisplayName("unweightedRandomNext should throw exception if state has no 'to' links")
        void RandomNextShouldReturnNullIfStateHasNoToLinks() {
            assertThrows(IllegalStateException.class, () -> chain.unweightedRandomNext(Arrays.asList("mankind")));
        }

        @Test
        @DisplayName("allPossibleNext should throw exception if state doesn't exist")
        void AllPossibleNextShouldThrowExceptionIfStateUnknown() {
            assertThrows(IllegalArgumentException.class,() -> chain.allPossibleNext(Arrays.asList("moon")));
        }

        @Test
        @DisplayName("allPossibleNext should return empty set if state has no 'to' links")
        void AllPossibleNextShouldReturnEmptyIfStateHasNoToLinks() {
            assertTrue(chain.allPossibleNext(Arrays.asList("mankind")).isEmpty());
        }

        @Test
        @DisplayName("priors can be added for links that were not observed")
        void PriorsCanBeAdded() {
            chain.addPriors(0.005D);
            assertEquals(1.0D,chain.model.get(Arrays.asList("one")).get("small"),"adding priors should not affect already-observed links");
            assertEquals(0.005D,chain.model.get(Arrays.asList("one")).get("step"),"priors should be added for unobserved links from non-terminal states");
            assertFalse(chain.model.containsKey(Arrays.asList("mankind")),"adding priors should not create new models for terminal states");
        }

        @Test
        @DisplayName("priors can be changed by calling removeWeakLinks and then addPriors")
        void PriorsCanBeChanged() {
            chain.addPriors(0.005D);
            assertEquals(1.0D,chain.model.get(Arrays.asList("one")).get("small"),"adding priors should not affect already-observed links");
            assertEquals(0.005D,chain.model.get(Arrays.asList("one")).get("step"),"priors should be added for unobserved links from non-terminal states");
            assertFalse(chain.model.containsKey(Arrays.asList("mankind")),"adding priors should not create new models for terminal states");
            chain.removeWeakLinks(1D);
            assertFalse(chain.model.get(Arrays.asList("one")).containsKey("step"),"removeWeakLinks failed to remove a prior");
            assertTrue(chain.model.get(Arrays.asList("one")).containsKey("small"),"removeWeakLinks removed a link it shouldn't have removed");
            chain.addPriors(0.001D);
            assertEquals(0.001D,chain.model.get(Arrays.asList("one")).get("step"),"addPriors after removeWeakLinks did not successfully change the prior");
        }

        @Test
        @DisplayName("priors can be set and removed with shortcut functions")
        void PriorsCanBeSetAndRemovedWithShortcuts() {
            chain.addPriors();
            assertEquals(DEFAULT_PRIOR,chain.model.get(Arrays.asList("one")).get("step"),"prior was not set to DEFAULT_PRIOR by the shortcut addPriors()");
            chain.removeWeakLinks();
            assertFalse(chain.model.get(Arrays.asList("one")).containsKey("step"),"shortcut removeWeakLinks() failed to remove a prior");
        }

    }



}