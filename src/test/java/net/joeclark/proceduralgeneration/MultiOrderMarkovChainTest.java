package net.joeclark.proceduralgeneration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

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
    @DisplayName("Can define a model by direct specification rather than training")
    void CanDefineAModelByDirectSpecification() {
        MultiOrderMarkovChain<Character> chain = new MultiOrderMarkovChain<>();
        chain.specifyLink(Arrays.asList('A'),'B',1.5D);
        chain.specifyLink(Arrays.asList('A'),'B',1.25D);
        chain.specifyLink(Arrays.asList('B'),'C',1.75D);
        assertEquals(1.75D,chain.model.get(Arrays.asList('B')).get('C'),"specifyLink did not correctly specify a link");
        assertEquals(1.25D,chain.model.get(Arrays.asList('A')).get('B'),"specifyLink failed to correctly overwrite a prior specification");
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

    @Test
    @DisplayName("Can be trained on a Stream<List<T>>")
    void CanBeTrainedOnAStream() {
        List<List<Character>> trainingData = Arrays.asList(
                Arrays.asList('h','e','l','l','o'),
                Arrays.asList('w','o','r','l','d')
        );
        MultiOrderMarkovChain<Character> chain = new MultiOrderMarkovChain<Character>().andTrain(trainingData.stream());
        assertTrue(chain.knownStates.contains('h'),"didn't train states in first sequence");
        assertTrue(chain.knownStates.contains('d'),"didn't train states in last sequence");
        assertTrue(chain.model.get(Arrays.asList('l')).keySet().containsAll(Arrays.asList('l','o','d')),"missed some transitions in the training data stream");
    }

    @Test
    @DisplayName("Short sequences in a training stream don't blow up a batch of training")
    void ShortSequencesDontBlowUpTraining() {
        List<List<Character>> trainingData = Arrays.asList(
                Arrays.asList('i'),
                Arrays.asList('l','i','k','e'),
                Arrays.asList('s','p','a','m')
        );
        MultiOrderMarkovChain<Character> chain = new MultiOrderMarkovChain<Character>().andTrain(trainingData.stream());
        assertEquals(2,chain.getNumTrainedSequences(),"two sequences should have been ingested, one ignored");
    }

    @Nested
    @DisplayName("When initialized but not trained...")
    class WhenUnTrained {

        private MultiOrderMarkovChain<Float> chain;

        @BeforeEach
        void InitializeButDontTrain() {
            chain = new MultiOrderMarkovChain<Float>();
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
        @DisplayName("allKnownStates should includes states that are both 'from' and 'to' states")
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

        @Test
        @DisplayName("Can be serialized and deserialized")
        void CanBeSerializedAndDeserialized() throws IOException, ClassNotFoundException {
            chain.setMaxOrder(2);

            FileOutputStream fileOutputStream = new FileOutputStream("target/mychain.ser");
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(chain);
            objectOutputStream.flush();
            objectOutputStream.close();

            FileInputStream fileInputStream = new FileInputStream("target/mychain.ser");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            @SuppressWarnings("unchecked")
            MultiOrderMarkovChain<String> loadedChain = (MultiOrderMarkovChain<String>) objectInputStream.readObject();
            objectInputStream.close();

            System.out.println(chain.model);
            System.out.println(loadedChain.model);

            assertEquals(chain.getMaxOrder(),loadedChain.getMaxOrder(),"an int field was not preserved through serialization-deserialization");
            assertEquals(chain.model,loadedChain.model,"the markov model was not preserved through serialization-deserialization");
            assertEquals(chain.numTrainedSequences,loadedChain.numTrainedSequences,"numTrainedSequences was not preserved through serialization-deserialization");
            assertEquals(chain.knownStates,loadedChain.knownStates,"knownStates was not preserved through serialization-deserialization");
            assertEquals(chain.random.nextInt(),loadedChain.random.nextInt(),"random was not preserved through serialization-deserialization");

            assertEquals(chain,loadedChain,"the serialized-deserialzed chain is not .equals() to the original chain");
        }

    }


    @Nested
    @DisplayName("With a complex object type")
    class WithAComplexObjectType {

        class WeatherPattern implements Serializable {
            public String condition;
            public Integer temperature;
            public Character windDirection;

            public String getCondition() { return condition; }
            public Integer getTemperature() { return temperature; }
            public Character getWindDirection() { return windDirection; }

            public WeatherPattern(String condition, Integer temperature, Character windDirection) {
                this.condition = condition;
                this.temperature = temperature;
                this.windDirection = windDirection;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                WeatherPattern that = (WeatherPattern) o;
                return Objects.equals(condition, that.condition) && Objects.equals(temperature, that.temperature) && Objects.equals(windDirection, that.windDirection);
            }

            @Override
            public int hashCode() {
                return Objects.hash(condition, temperature, windDirection);
            }

            @Override
            public String toString() {
                return "{" + condition + "}";
            }
        }

        @Test
        @DisplayName("MultiOrderMarkovChain should behave the same as with primitive types")
        void ShouldBehaveTheSame() {
            MultiOrderMarkovChain<WeatherPattern> weatherchain = new MultiOrderMarkovChain<>();
            WeatherPattern sunny = new WeatherPattern("sunny",75,'W');
            WeatherPattern cloudy = new WeatherPattern("cloudy",55,'N');
            WeatherPattern partlycloudy = new WeatherPattern("partly cloudy",65,'S');
            WeatherPattern stormy = new WeatherPattern("stormy",50,'E');
            weatherchain.addSequence(Arrays.asList(sunny,partlycloudy,cloudy,stormy,partlycloudy,sunny));
            System.out.println(weatherchain.getModel());

            WeatherPattern next = weatherchain.weightedRandomNext(Arrays.asList(partlycloudy));
            assertTrue( next.equals(cloudy) || next.equals(sunny) );
            assertTrue( weatherchain.weightedRandomNext(Arrays.asList(sunny,partlycloudy)).equals(cloudy) );
        }

    }


}