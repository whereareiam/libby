package com.alessiodp.libby.classloader;

import com.alessiodp.libby.LibraryManager;
import org.jetbrains.annotations.NotNull;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.jar.JarFile;

import static java.util.Objects.requireNonNull;

/**
 * A reflection-based wrapper around SystemClassLoader for adding URLs to
 * the classpath.
 */
public class SystemClassLoaderHelper extends ClassLoaderHelper {

    /**
     * A reflected method in SystemClassLoader, when invoked adds a URL to the classpath.
     */
    private MethodHandle appendMethodHandle = null;
    private Instrumentation appendInstrumentation = null;

    /**
     * Creates a new SystemClassLoader helper.
     *
     * @param classLoader    the class loader to manage
     * @param libraryManager the library manager used to download dependencies
     */
    public SystemClassLoaderHelper(ClassLoader classLoader, @NotNull LibraryManager libraryManager) {
        super(classLoader);
        requireNonNull(libraryManager, "libraryManager");

        try {
            Method appendMethod = classLoader.getClass().getDeclaredMethod("appendToClassPathForInstrumentation", String.class);

            try {
                appendMethod.setAccessible(true);
            } catch (Exception exception) {
                // InaccessibleObjectException has been added in Java 9
                if (exception.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException")) {
                    // It is Java 9+, try to open jdk.internal.loader package
                    if (theUnsafe != null) {
                        try {
                            appendMethodHandle = getPrivilegedMethodHandle(appendMethod).bindTo(classLoader);
                            return; // We're done
                        } catch (Exception ignored) {}
                    }
                    // Cannot use privileged MethodHandles.Lookup, trying with java agent
                    try {
                        initInstrumentation(libraryManager, (instrumentation) -> {
                            appendInstrumentation = instrumentation;
                        });
                    } catch (Exception e) {
                        // Cannot access at all
                        libraryManager.getLogger().error("Cannot access " + classLoader.getClass().getName() + "#appendToClassPathForInstrumentation(String)");
                        throw new RuntimeException("Cannot access " + classLoader.getClass().getName() + "#appendToClassPathForInstrumentation(String)", e);
                    }
                } else {
                    throw new RuntimeException("Cannot set accessible " + classLoader.getClass().getName() + "#appendToClassPathForInstrumentation(String) method in the class loader", exception);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addToClasspath(@NotNull URL url) {
        try {
            if (appendInstrumentation != null)
                appendInstrumentation.appendToSystemClassLoaderSearch(new JarFile(url.toURI().getPath()));
            else
                appendMethodHandle.invokeWithArguments(url.toURI().getPath());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
