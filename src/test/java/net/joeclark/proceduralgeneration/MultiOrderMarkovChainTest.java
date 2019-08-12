package net.joeclark.proceduralgeneration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;

class MultiOrderMarkovChainTest {

    // TODO: copy over tests from EPMC and then delete that class once the MOMC class fulfils its jobs

    @Test
    @DisplayName("Can train a chain of strings on string sequences")
    void CanInitializeAChainOfStrings() {
        MultiOrderMarkovChain<String> chain = new MultiOrderMarkovChain<>();
        chain.addSequence(new LinkedList<String>(Arrays.asList("A","B","C","D","E")));
    }



}