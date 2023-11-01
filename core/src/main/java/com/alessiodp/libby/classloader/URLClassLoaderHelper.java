package com.alessiodp.libby.classloader;

import com.alessiodp.libby.LibraryManager;
import org.jetbrains.annotations.NotNull;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A reflection-based wrapper around {@link URLClassLoader} for adding URLs to
 * the classpath.
 */
public class URLClassLoaderHelper extends ClassLoaderHelper {
    /**
     * A reflected method in {@link URLClassLoader}, when invoked adds a URL to the classpath.
     */
    private MethodHandle addURLMethodHandle = null;

    /**
     * Creates a new URL class loader helper.
     *
     * @param classLoader the class loader to manage
     * @param libraryManager the library manager used to download dependencies
     */
    public URLClassLoaderHelper(@NotNull URLClassLoader classLoader, @NotNull LibraryManager libraryManager) {
        super(classLoader);
        requireNonNull(libraryManager, "libraryManager");

        try {
            Method addURLMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            setMethodAccessible(libraryManager, addURLMethod, "URLClassLoader#addURL(URL)",
                    methodHandle -> {
                        addURLMethodHandle = methodHandle;
                    },
                    instrumentation -> {
                        addOpensWithAgent(instrumentation);
                        addURLMethod.setAccessible(true);
                    }
            );
            if (addURLMethodHandle == null) {
                addURLMethodHandle = MethodHandles.lookup().unreflect(addURLMethod).bindTo(classLoader);
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't initialize URLClassLoaderHelper", e);
        }
    }

    @Override
    public void addToClasspath(@NotNull URL url) {
        try {
            addURLMethodHandle.invokeWithArguments(requireNonNull(url, "url"));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void addOpensWithAgent(@NotNull Instrumentation instrumentation) {
        // This is effectively calling:
        //
        // instrumentation.redefineModule(
        //     URLClassLoader.class.getModule(),
        //     Collections.emptySet(),
        //     Collections.emptyMap(),
        //     Collections.singletonMap("java.net", Collections.singleton(getClass().getModule())),
        //     Collections.emptySet(),
        //     Collections.emptyMap()
        // );
        //
        // For more information see https://docs.oracle.com/en/java/javase/16/docs/api/java.instrument/java/lang/instrument/Instrumentation.html
        try {
            Method redefineModule = Instrumentation.class.getMethod("redefineModule", Class.forName("java.lang.Module"), Set.class, Map.class, Map.class, Set.class, Map.class);
            Method getModule = Class.class.getMethod("getModule");
            Map<String, Set<?>> toOpen = Collections.singletonMap("java.net", Collections.singleton(getModule.invoke(getClass())));
            redefineModule.invoke(instrumentation, getModule.invoke(URLClassLoader.class), Collections.emptySet(), Collections.emptyMap(), toOpen, Collections.emptySet(), Collections.emptyMap());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
