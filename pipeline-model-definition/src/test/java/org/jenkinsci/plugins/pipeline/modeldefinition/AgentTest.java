/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.pipeline.modeldefinition;

import hudson.model.Result;
import hudson.model.Slave;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.BeforeClass;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Andrew Bayer
 */
public class AgentTest extends AbstractModelDefTest {

    private static Slave s;

    @BeforeClass
    public static void setUpAgent() throws Exception {
        s = j.createOnlineSlave();
        s.setLabelString("some-label docker");
        s.getNodeProperties().add(new EnvironmentVariablesNodeProperty(new EnvironmentVariablesNodeProperty.Entry("ONSLAVE", "true")));
    }

    @Test
    public void agentLabel() throws Exception {
        expect("agentLabel")
                .logContains("[Pipeline] { (foo)", "ONSLAVE=true")
                .go();
    }

    @Issue("JENKINS-37932")
    @Test
    public void agentAny() throws Exception {
        expect("agentAny")
                .logContains("[Pipeline] { (foo)", "THIS WORKS")
                .go();
    }

    @Test
    public void noCheckoutScmInWrongContext() throws Exception {
        expect("noCheckoutScmInWrongContext")
                .runFromRepo(false)
                .logContains("[Pipeline] { (foo)", "ONSLAVE=true")
                .go();
    }

    @Test
    public void agentDocker() throws Exception {
        agentDocker("agentDocker", "-v /tmp:/tmp -p 80:80");
    }

    @Test
    public void agentDockerWithNullDockerArgs() throws Exception {
        agentDocker("agentDockerWithNullDockerArgs");
    }

    @Test
    public void agentDockerWithEmptyDockerArgs() throws Exception {
        agentDocker("agentDockerWithEmptyDockerArgs");
    }

    @Test
    public void agentNone() throws Exception {
        expect(Result.FAILURE, "agentNone")
                .logContains("Attempted to execute a step that requires a node context while 'agent none' was specified. " +
                        "Be sure to specify your own 'node { ... }' blocks when using 'agent none'.",
                        "Perhaps you forgot to surround the code with a step that provides this, such as: node")
                .go();
    }

    @Test
    public void agentNoneWithNode() throws Exception {
        expect("agentNoneWithNode")
                .logContains("[Pipeline] { (foo)", "ONSLAVE=true")
                .go();
    }

    @Test
    public void perStageConfigAgent() throws Exception {
        expect("perStageConfigAgent")
                .logContains("[Pipeline] { (foo)", "ONSLAVE=true")
                .go();
    }

    @Test
    public void agentTypeOrdering() throws Exception {
        expect("agentTypeOrdering")
                .logContains("[Pipeline] { (foo)", "ONSLAVE=true", "Running in labelAndOtherField with otherField = banana")
                .go();
    }

    @Test
    public void agentAnyInStage() throws Exception {
        expect("agentAnyInStage")
                .logContains("[Pipeline] { (foo)", "THIS WORKS")
                .go();
    }

    @Test
    public void fromDockerfile() throws Exception {
        assumeDocker();
        // Bind mounting /var on OS X doesn't work at the moment
        onAllowedOS(PossibleOS.LINUX);

        sampleRepo.write("Dockerfile", "FROM ubuntu:14.04\n\nRUN echo 'HI THERE' > /hi-there\n\n");
        sampleRepo.git("init");
        sampleRepo.git("add", "Dockerfile");
        sampleRepo.git("commit", "--message=Dockerfile");

        expect("fromDockerfile")
                .logContains("[Pipeline] { (foo)",
                        "The answer is 42",
                        "-v /tmp:/tmp -p 8000:8000",
                        "HI THERE")
                .go();
    }

    @Test
    public void fromAlternateDockerfile() throws Exception {
        assumeDocker();
        // Bind mounting /var on OS X doesn't work at the moment
        onAllowedOS(PossibleOS.LINUX);
        sampleRepo.write("Dockerfile.alternate", "FROM ubuntu:14.04\n\nRUN echo 'HI THERE' > /hi-there\n\n");
        sampleRepo.git("init");
        sampleRepo.git("add", "Dockerfile.alternate");
        sampleRepo.git("commit", "--message=Dockerfile");

        expect("fromAlternateDockerfile")
                .logContains("[Pipeline] { (foo)",
                        "The answer is 42",
                        "-v /tmp:/tmp -p 8000:8000",
                        "HI THERE")
                .go();
    }

    private void agentDocker(final String jenkinsfile, String... additionalLogContains) throws Exception {
        assumeDocker();
        // Bind mounting /var on OS X doesn't work at the moment
        onAllowedOS(PossibleOS.LINUX);

        List<String> logContains = new ArrayList<>();
        logContains.add("[Pipeline] { (foo)");
        logContains.add("The answer is 42");
        logContains.addAll(Arrays.asList(additionalLogContains));

        expect(jenkinsfile)
                .logContains(logContains.toArray(new String[logContains.size()]))
                .go();
    }
}
