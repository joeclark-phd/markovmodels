package net.joeclark.proceduralgeneration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MultiOrderMarkovChain...")
class MultiOrderMarkovChainTest {

    @Test
    @DisplayName("Can initialize a chain of Characters")
    void CanInitializeAChainOfCharacters() {
        MultiOrderMarkovChain<Character> chain = new MultiOrderMarkovChain<>();
        chain.addSequence(new ArrayList<>(Arrays.asList('V','W','X','Y','Z')));
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
        @DisplayName("random choice should be one of the trained links")
        void RandomNextShouldBeOneOfTheTrainedLinks() {
            String next = chain.randomNext("one");
            assertTrue(next.equals("small") || next.equals("giant"));
        }

        @Test
        @DisplayName("randomNext should throw exception if state doesn't exist")
        void RandomNextShouldThrowExceptionIfStateUnknown() {
            assertThrows(IllegalArgumentException.class,() -> chain.randomNext("moon"));
        }

        @Test
        @DisplayName("randomNext should throw exception if state has no 'to' links")
        void RandomNextShouldReturnNullIfStateHasNoToLinks() {
            assertThrows(IllegalStateException.class, () -> chain.randomNext("mankind"));
        }

        @Test
        @DisplayName("allPossibleNext should throw exception if state doesn't exist")
        void AllPossibleNextShouldThrowExceptionIfStateUnknown() {
            assertThrows(IllegalArgumentException.class,() -> chain.allPossibleNext("moon"));
        }

        @Test
        @DisplayName("allPossibleNext should return empty set if state has no 'to' links")
        void AllPossibleNextShouldReturnEmptyIfStateHasNoToLinks() {
            assertTrue(chain.allPossibleNext("mankind").isEmpty());
        }

    }



}