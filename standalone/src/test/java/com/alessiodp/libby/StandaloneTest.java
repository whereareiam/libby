package com.alessiodp.libby;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class StandaloneTest {

    private static String JAVA_EXECUTABLE;

    @BeforeAll
    public static void beforeAll() throws Exception {
        String javaHome = System.getProperty("java.home");
        File binDir = new File(javaHome, "bin");

        File executable = new File(binDir, "java");
        if (executable.exists()) {
            JAVA_EXECUTABLE = executable.getAbsolutePath();
        } else {
            File windowsExe = new File(binDir, "java.exe");
            if (windowsExe.exists()) {
                JAVA_EXECUTABLE = windowsExe.getAbsolutePath();
            } else {
                throw new FileNotFoundException("Cannot find Java executable in " + binDir);
            }
        }
    }

    @Test
    public void standaloneWithSystemClassLoader() throws Exception {
        Process testProcess = new ProcessBuilder(JAVA_EXECUTABLE, "-jar", LibbyTestProperties.TEST_JAR)
                .start();

        assertTrue(testProcess.waitFor(1, TimeUnit.MINUTES));

        int exitCode = testProcess.exitValue();
        if (exitCode != 0) {
            failFormatted(testProcess, exitCode);
        }
    }

    @Test
    public void standaloneWithURLClassLoader() throws Exception {
        URL testJarURL = Paths.get(LibbyTestProperties.TEST_JAR).toUri().toURL();
        URLClassLoader loader = new URLClassLoader(new URL[]{testJarURL}, ClassLoader.getSystemClassLoader().getParent());

        Class<?> standaloneTestMainClass = loader.loadClass(StandaloneTestMain.class.getName());
        assertNotSame(StandaloneTestMain.class, standaloneTestMainClass);
        standaloneTestMainClass.getMethod("main", String[].class).invoke(null, (Object) new String[0]);

        assertThrows(ClassNotFoundException.class, () -> Class.forName(TestUtils.STRING_UTILS_CLASS));
        assertNotNull(loader.loadClass(TestUtils.STRING_UTILS_CLASS));
    }

    private void failFormatted(Process testProcess, int exitCode) throws IOException {
        fail("Test process exited with code " + exitCode +
                "\n\nProcess stdout:\n" +
                IOUtils.toString(testProcess.getInputStream(), StandardCharsets.UTF_8) +
                "\n\nProcess stderr:\n" +
                IOUtils.toString(testProcess.getErrorStream(), StandardCharsets.UTF_8) +
                "\nFailed assertion stacktrace:");
    }
}
