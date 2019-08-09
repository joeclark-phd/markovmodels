package net.joeclark.proceduralgeneration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EqualProbabilityMarkovChainTest {

    @Test
    @DisplayName("Can initialize a chain of Strings")
    void CanInitializeAChainOfStrings() {
        EqualProbabilityMarkovChain<String> epmc = new EqualProbabilityMarkovChain<>();
        epmc.addLink("A","B");
    }

    @Test
    @DisplayName("Can initialize a chain of Integers")
    void CanInitializeAChainOfIntegers() {
        EqualProbabilityMarkovChain<Integer> epmc = new EqualProbabilityMarkovChain<>();
        epmc.addLink(1,2);
    }

    @Nested
    @DisplayName("When initialized but not trained...")
    class WhenUnTrained {

        EqualProbabilityMarkovChain<Character> epmc;

        @BeforeEach
        void InitializeButDontTrain() {
            epmc = new EqualProbabilityMarkovChain<>();
        }

        @Test
        @DisplayName("hasModel() should return false")
        void HasModelShouldBeFalse() {
            assertEquals(false,epmc.hasModel(),"hasModel() returned true despite model being empty/untrained");
        }

        @Test
        @DisplayName("allKnownStates should return empty set")
        void AllKnownStatesShouldReturnNull() {
            assertTrue(epmc.allKnownStates().isEmpty());
        }

    }

    @Nested
    @DisplayName("Once trained...")
    class OnceTrained {

        EqualProbabilityMarkovChain<Character> epmc;

        @BeforeEach
        void trainChain() {
            epmc = new EqualProbabilityMarkovChain<>();
            epmc.addLink('A','B');
            epmc.addLink('A','C');
            epmc.addLink('B','C');
            epmc.addLink('B','D');
        }

        @Test
        @DisplayName("hasModel() should return true")
        void HasModelShouldBeTrue() {
            assertEquals(true,epmc.hasModel(),"hasModel() returned false despite model being trained");
        }

        @Test
        @DisplayName("allKnownStates should include all 'from' states")
        void AllKnownStatesShouldIncludeAllFromStates() {
            assertTrue(epmc.allKnownStates().contains('A'));

        }

        @Test
        @DisplayName("allKnownStates shoudl includes states that are both 'from' and 'to' states")
        void AllKnownStatesShoudlIncludeToAndFromStates() {
            assertTrue(epmc.allKnownStates().contains('B'));
        }

        @Test
        @DisplayName("allKnownStates should include all 'to' states")
        void AllKnownStatesShouldIncludeAllToStates() {
            assertTrue(epmc.allKnownStates().contains('C'));
        }

        @Test
        @DisplayName("random choice should be one of the trained links")
        void RandomNextShouldBeOneOfTheTrainedLinks() {
            Character next = epmc.randomNext('A');
            assertTrue(next.equals('B')||next.equals('C'));
        }

        @Test
        @DisplayName("randomNext should throw exception if state doesn't exist")
        void RandomNextShouldThrowExceptionIfStateUnknown() {
            assertThrows(IllegalArgumentException.class,() -> epmc.randomNext('Q'));
        }

        @Test
        @DisplayName("randomNext should throw exception if state has no 'to' links")
        void RandomNextShouldReturnNullIfStateHasNoToLinks() {
            assertThrows(IllegalStateException.class, () -> epmc.randomNext('C'));
        }

        @Test
        @DisplayName("allPossibleNext should throw exception if state doesn't exist")
        void AllPossibleNextShouldThrowExceptionIfStateUnknown() {
            assertThrows(IllegalArgumentException.class,() -> epmc.allPossibleNext('Q'));
        }

        @Test
        @DisplayName("allPossibleNext should return empty set if state has no 'to' links")
        void AllPossibleNextShouldReturnEmptyIfStateHasNoToLinks() {
            assertTrue(epmc.allPossibleNext('C').isEmpty());
        }

    }

}