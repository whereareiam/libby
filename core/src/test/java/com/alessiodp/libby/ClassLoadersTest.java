package com.alessiodp.libby;

import com.alessiodp.libby.classloader.ClassLoaderHelper;
import com.alessiodp.libby.classloader.SystemClassLoaderHelper;
import com.alessiodp.libby.classloader.URLClassLoaderHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URLClassLoader;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ClassLoadersTest {

    @Test
    public void testHelpers() throws IOException {
        LibraryManagerMock libraryManager = new LibraryManagerMock();
        ClassLoader currentClassLoader = getClass().getClassLoader();

        ClassLoaderHelper helper;
        if (currentClassLoader instanceof URLClassLoader) {
            helper = new URLClassLoaderHelper((URLClassLoader) currentClassLoader, libraryManager);
        } else {
            helper = new SystemClassLoaderHelper(currentClassLoader, libraryManager);
        }

        assertNotNull(helper);
    }
}
