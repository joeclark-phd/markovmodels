# markovmodels

[![MIT License](https://img.shields.io/github/license/joeclark-phd/markovmodels.svg)](https://github.com/joeclark-phd/markovmodels/blob/master/LICENSE.md)

API reference: [https://joeclark-phd.github.io/markovmodels/](https://joeclark-phd.github.io/markovmodels/)

I found myself writing code for multi-order Markov chains in [joeclark-phd/random-text-generators](https://github.com/joeclark-phd/random-text-generators) and realized that I and others might need a generic version of that concept in a few different kinds of procedural generation tasks.  This project is a generic and reusable tool for training and using Markov chains, optionally with multiple "orders" and prior probabilities.

## `MarkovChain<T>`

A Markov chain maps current states to possible future states, usually defined with probabilities ([c.f. wikipedia](https://en.wikipedia.org/wiki/Markov_chain)).  This is useful in procedural generation, for example to model which letters in a language most frequently follow a given letter, or to model which weather conditions most likely follow a given weather condition.  For each known state, a MarkovChain knows or can calculate the possible following states, so a client should be able to traverse the "chain" as many iterations as he likes (e.g. for a simulation).  In most cases these transitions will be weighted by a probability distribution or observed frequencies, so not all transitions from any given state will be equally likely.

## `MultiOrderMarkovChain<T>`

`MultiOrderMarkovChain` is the reference implementation for `MarkovChain`.  It is inspired by the algorithm [described by JLund3 at RogueBasin](http://roguebasin.roguelikedevelopment.org/index.php?title=Names_from_a_high_order_Markov_Process_and_a_simplified_Katz_back-off_scheme).

This implementation offers multi-order models with a Katz back-off.  What that means is, if `maxOrder` is greater than 1, multiple models may match any given sequence. For example, if you provide the sequence `{'J','A','V','A'}`, and `maxOrder==3`, we will first check to see if we have a model for states that follow `{'A','V','A'}`. If none is found, for example because that sequence was never seen in training data, we check for a model for states that follow `{'V','A'}`. Failing that too, we fall back on the model of transitions from the state `'A'`, which is certain to exist if `'A'` is a valid input to the relevant method (`weightedRandomNext` or `unWeightedRandomNext`).

Another feature that may be desired in procedural generation applications is the option to inject some "true randomness" in the form of "prior" relative probabilities, i.e., small weights given to transitions *not* observed in training data. These can make up for the limitations of a training dataset and enable the generation of sequences not observed in training.  Use the `addPriors()` method after training your model to take advantage of this.

If multi-order models are not desired, set `maxOrder` to 1.  If priors are not desired, do nothing; they will not be added by default.

## Maven metadata

    <groupId>net.joeclark.proceduralgeneration</groupId>
    <artifactId>markovmodels</artifactId>
    <version>1.0</version>


## Usage

The interface and implementation here are generic, and you can specify any *Serializable* type for your "states".  If you're generating random words, you might use `Character`.  If you're generating random sentences, you might use `String`.  If you're generating random weather or something, you can use any custom class as long as it implements *Serializable*.

    // states are letters
    MarkovChain<Character> charMarkov = MultiOrderMarkovChain<>();

    // states are words
    MarkovChain<String> stringMarkov = MultiOrderMarkovChain<>();

    // states are Foo instances
    MarkovChain<Foo> fooMarkov = MultiOrderMarkovChain<>();
            

### Sequences in the model

There is one *caveat* that might trip you up: the input to some key functions is a `List<T>` rather than simply a `T`. (`T` is the generic type you specified.)

While a theoretical Markov chain maps transitions from states to states, for example:

    'V' -> 'A' with probability 0.15
    'V' -> 'E' with probability 0.30

Our implementation maps transitions from **sequences** of states, to following states.  These sequences may be of length 1:

    {'V'} -> 'A' with probability 0.15
    {'V'} -> 'E' with probability 0.30

But we implemented it this way to make it possible to go beyond the simple Markov model and keep track of transition probabilities based on sequences of two or more predecessor states, e.g.:

    {'V'} -> 'A' with probability 0.15
    {'A','V'} -> 'A' with probability 0.33
    {'J','A','V'} -> 'V' with probability 0.99

So just be aware than when calling the key functions, you are probably passing in a `List<T>` rather than simply a `T`.

### Training

To train your `MultiOrderMarkovChain<T>` with weights based on frequency of occurrence, you can simply pass in whole sequences of states.  Use the `addSequence()` method to add a single state, or `train()` or `andTrain()` to ingest a `Stream<List<T>>` (for example, from an input file) and add them all.  In either case, the instance will extract every sub-sequence it finds in each training sequence (up to length `maxOrder`, which defaults to 3), and learn that transition.

Check out [the included JUnit 5 tests](https://github.com/joeclark-phd/markovmodels/blob/master/src/test/java/net/joeclark/proceduralgeneration/MultiOrderMarkovChainTest.java) for some examples, e.g.:

    List<List<Character>> trainingData = Arrays.asList(
        Arrays.asList('h','e','l','l','o'),
        Arrays.asList('w','o','r','l','d')
    );
    MultiOrderMarkovChain<Character> chain = new MultiOrderMarkovChain<Character>()
        .andTrain(trainingData.stream());
            
or
            
    MultiOrderMarkovChain<String> chain = new MultiOrderMarkovChain<>();
    chain.addSequence( Arrays.asList("one","small","step","for","man") );
    chain.addSequence( Arrays.asList("one","giant","leap","for","mankind") );

### Setting priors

A limitation of a "trained" model is that the training data may not be large enough to include all possible sequences, and sequences not observed in training cannot be generated procedurally from the model so trained.  To mitigate this, you have the option of setting a small "prior" weight to every transition between *known* states that was *not* observed in the model.  Use the `addPriors()` method *after* training to do this.  The default prior is `0.005`, or you may specify one:

    chain.addPriors(0.01D)

### Procedural generation

The expected use case is that you'd like to randomly draw some new states given past or current states.  The method `weightedRandomNext()` takes a `List<T>` (which can be a sequence of just one state, or many) and draws a new state at random based on the transitions from the end of that sequence that it knows about.  In the standard case, the weights are frequencies of past observations, so a transition that occurs twice as often in the training data will occur twice as often here.

    String next = chain.weightedRandomNext(Arrays.asList("one"));

Just remember the potential hangup: you need to pass in a `List<T>` and not just a `T`.

We also offer a method `unweightedRandomNext()` which ignores relative frequencies and simply performs a random draw from all known transitions.