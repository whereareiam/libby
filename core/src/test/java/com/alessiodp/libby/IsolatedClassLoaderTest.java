package com.alessiodp.libby;

import com.alessiodp.libby.classloader.IsolatedClassLoader;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;

public class IsolatedClassLoaderTest {

    @Test
    void isolation() {
        IsolatedClassLoader isolatedClassLoader = new IsolatedClassLoader();

        assertThrows(ClassNotFoundException.class, () -> isolatedClassLoader.loadClass("com.alessiodp.libby.Library"));
        assertDoesNotThrow(() -> isolatedClassLoader.loadClass(LinkedList.class.getName()));
    }
}
