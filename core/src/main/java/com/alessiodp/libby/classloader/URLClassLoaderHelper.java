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
     * @param classLoader    the class loader to manage
     * @param libraryManager the library manager used to download dependencies
     */
    public URLClassLoaderHelper(@NotNull URLClassLoader classLoader, @NotNull LibraryManager libraryManager) {
        super(classLoader);
        requireNonNull(libraryManager, "libraryManager");

        try {
            Method addURLMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);

            try {
                openUrlClassLoaderModule();
            } catch (Exception ignored) {}

            try {
                addURLMethod.setAccessible(true);
            } catch (Exception exception) {
                // InaccessibleObjectException has been added in Java 9
                if (exception.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException")) {
                    // It is Java 9+, try to open java.net package
                    if (theUnsafe != null) {
                        try {
                            addURLMethodHandle = getPrivilegedMethodHandle(addURLMethod).bindTo(classLoader);
                            return; // We're done
                        } catch (Exception ignored) {}
                    }
                    // Cannot use privileged MethodHandles.Lookup, trying with java agent
                    try {
                        initInstrumentation(libraryManager, this::addOpensWithAgent);
                        addURLMethod.setAccessible(true);
                    } catch (Exception e) {
                        // Cannot access at all
                        libraryManager.getLogger().error("Cannot access URLClassLoader#addURL(URL), if you are using Java 9+ try to add the following option to your java command: --add-opens java.base/java.net=ALL-UNNAMED");
                        throw new RuntimeException("Cannot access URLClassLoader#addURL(URL)", e);
                    }
                } else {
                    throw new RuntimeException("Cannot set accessible URLClassLoader#addURL(URL)", exception);
                }
            }
            addURLMethodHandle = MethodHandles.lookup().unreflect(addURLMethod).bindTo(classLoader);
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    private static void openUrlClassLoaderModule() throws Exception {
        //
        // Thanks to lucko (Luck) <luck@lucko.me> for this snippet used in his own class loader
        //
        // This is a workaround used to maintain Java 9+ support with reflections
        // Thanks to this you will be able to run this class loader with Java 8+

        // This is effectively calling:
        //
        // URLClassLoader.class.getModule().addOpens(
        //     URLClassLoader.class.getPackageName(),
        //     URLClassLoaderHelper.class.getModule()
        // );
        //
        // We use reflection since we build against Java 8.

        Class<?> moduleClass = Class.forName("java.lang.Module");
        Method getModuleMethod = Class.class.getMethod("getModule");
        Method addOpensMethod = moduleClass.getMethod("addOpens", String.class, moduleClass);

        Object urlClassLoaderModule = getModuleMethod.invoke(URLClassLoader.class);
        Object thisModule = getModuleMethod.invoke(URLClassLoaderHelper.class);

        addOpensMethod.invoke(urlClassLoaderModule, URLClassLoader.class.getPackage().getName(), thisModule);
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
