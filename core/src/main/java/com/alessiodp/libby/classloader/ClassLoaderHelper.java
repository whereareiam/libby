package com.alessiodp.libby.classloader;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;
import com.alessiodp.libby.Repositories;
import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.function.Consumer;

import static com.alessiodp.libby.Util.replaceWithDots;
import static java.util.Objects.requireNonNull;

/**
 * An abstract class for reflection-based wrappers around class loaders for adding
 * URLs to the classpath.
 */
public abstract class ClassLoaderHelper {
    /**
     * net.bytebuddy.agent.ByteBuddyAgent class name for reflections
     */
    private static final String BYTE_BUDDY_AGENT_CLASS = replaceWithDots("net{}bytebuddy{}agent{}ByteBuddyAgent");

    /**
     * Unsafe class instance. Used in {@link #getPrivilegedMethodHandle(Method)}.
     */
    protected static final Unsafe theUnsafe;

    static {
        Unsafe unsafe = null; // Used to make theUnsafe field final

        // getDeclaredField("theUnsafe") is not used to avoid breakage on JVMs with changed field name
        for (Field f : Unsafe.class.getDeclaredFields()) {
            try {
                if (f.getType() == Unsafe.class && Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    unsafe = (Unsafe) f.get(null);
                }
            } catch (Exception ignored) {
            }
        }
        theUnsafe = unsafe;
    }

    /**
     * Cached {@link Instrumentation} instance. Used by {@link #initInstrumentation(LibraryManager, Consumer)}.
     */
    private static volatile Instrumentation cachedInstrumentation;

    /**
     * The class loader being managed by this helper.
     */
    protected final ClassLoader classLoader;

    /**
     * Creates a new class loader helper.
     *
     * @param classLoader the class loader to manage
     */
    public ClassLoaderHelper(ClassLoader classLoader) {
        this.classLoader = requireNonNull(classLoader, "classLoader");
    }

    /**
     * Adds a URL to the class loader's classpath.
     *
     * @param url the URL to add
     */
    public abstract void addToClasspath(@NotNull URL url);

    /**
     * Adds a path to the class loader's classpath.
     *
     * @param path the path to add
     */
    public void addToClasspath(@NotNull Path path) {
        try {
            addToClasspath(requireNonNull(path, "path").toUri().toURL());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Try to get a MethodHandle for the given method.
     *
     * @param method the method to get the handle for
     * @return the method handle
     */
    protected MethodHandle getPrivilegedMethodHandle(Method method) {
        // The Unsafe class is used to get a privileged MethodHandles.Lookup instance.

        // Looking for MethodHandles.Lookup#IMPL_LOOKUP private static field
        // getDeclaredField("IMPL_LOOKUP") is not used to avoid breakage on JVMs with changed field name
        for (Field trustedLookup : MethodHandles.Lookup.class.getDeclaredFields()) {
            if (trustedLookup.getType() != MethodHandles.Lookup.class || !Modifier.isStatic(trustedLookup.getModifiers()) || trustedLookup.isSynthetic())
                continue;

            try {
                MethodHandles.Lookup lookup = (MethodHandles.Lookup) theUnsafe.getObject(theUnsafe.staticFieldBase(trustedLookup), theUnsafe.staticFieldOffset(trustedLookup));
                return lookup.unreflect(method);
            } catch (Exception ignored) {
                // Unreflect went wrong, trying the next field
            }
        }

        // Every field has been tried
        throw new RuntimeException("Cannot get privileged method handle.");
    }

    /**
     * Load ByteBuddy agent and pass an Instrumentation instance to the consumer.
     *
     * @param libraryManager the library manager used to download dependencies
     * @param consumer the consumer to pass the Instrumentation instance to
     * @throws Exception if an error occurs
     */
    protected void initInstrumentation(LibraryManager libraryManager, Consumer<Instrumentation> consumer) throws Exception {
        Instrumentation instr = cachedInstrumentation;
        if (instr != null) {
            consumer.accept(instr);
            return;
        }

        // To open the class-loader's module we need permissions.
        // Try to add a java agent at runtime (specifically, ByteBuddy's agent) and use it to open the module,
        // since java agents should have such permission.

        // Download ByteBuddy's agent and load it through an IsolatedClassLoader
        IsolatedClassLoader isolatedClassLoader = new IsolatedClassLoader();
        try {
            isolatedClassLoader.addPath(libraryManager.downloadLibrary(
                    Library.builder()
                            .groupId("net{}bytebuddy")
                            .artifactId("byte-buddy-agent")
                            .version("1.12.1")
                            .checksum("mcCtBT9cljUEniB5ESpPDYZMfVxEs1JRPllOiWTP+bM=")
                            .repository(Repositories.MAVEN_CENTRAL)
                            .build()
            ));

            Class<?> byteBuddyAgent = isolatedClassLoader.loadClass(BYTE_BUDDY_AGENT_CLASS);

            // This is effectively calling:
            //
            // Instrumentation instrumentation = ByteBuddyAgent.install();
            // consumer.accept(instrumentation);
            //
            // For more information see https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html

            Instrumentation instrumentation = (Instrumentation) byteBuddyAgent.getMethod("install").invoke(null);
            cachedInstrumentation = instrumentation;
            consumer.accept(instrumentation);
        } finally {
            try {
                isolatedClassLoader.close();
            } catch (Exception ignored) {}
        }
    }
}
