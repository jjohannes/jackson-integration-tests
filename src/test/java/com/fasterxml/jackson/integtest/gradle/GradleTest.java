package com.fasterxml.jackson.integtest.gradle;

import java.io.*;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static java.util.Objects.requireNonNull;

import static org.junit.jupiter.api.Assertions.*;

public class GradleTest
{
    @TempDir
    public File testFolder;

    /**
     * This test calls the Gradle build in 'src/test/resources/com/fasterxml/jackson/integtest/gradle' which:
     * - Collects all entries from the latest Jackson BOM
     * - Checks the metadata of all the entries if they point back to the BOM
     * - If not, it fails which can mean one of the following:
     *   - .module file is missing completely (not configured in corresponding pom.xml?)
     *   - 'do_not_remove: published-with-gradle-metadata' comment missing in published POM
     *   - .module file is there but something is misconfigured
     * Gradle Metadata is published for most Jackson components for reasons described here:
     * <a href="https://blog.gradle.org/alignment-with-gradle-module-metadata">blog.gradle.org/alignment-with-gradle-module-metadata</a>
     */
    @Test
    public void testJacksonBomDependency() throws Exception {
        copyToTestFolder("settings.gradle.kts");
        copyToTestFolder("build.gradle.kts");
        build(":checkMetadata");
    }

    private void copyToTestFolder(String fileName) throws IOException {
        Files.copy(new File(requireNonNull(getClass().getResource(fileName)).getFile()).toPath(),
                new File(testFolder, fileName).toPath());
    }

    private void build(String task) throws Exception {
        String gradlew = requireNonNull(getClass().getResource("gradlew")).getFile();
        Runtime.getRuntime().exec("chmod a+x " + gradlew).waitFor();
        ProcessBuilder bp = new ProcessBuilder(gradlew, task, "-q",
                "--project-dir", testFolder.getAbsolutePath());
        bp.redirectErrorStream(true);
        Process process = bp.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder output = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        reader.close();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            fail(output.toString().trim());
        }
    }
}
