package net.bytebuddy.agent.builder;

import com.luufery.bytebuddy.api.plugin.listener.MyRedefinitionListener;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.CompoundList;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.JavaType;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import net.bytebuddy.utility.nullability.AlwaysNull;
import net.bytebuddy.utility.nullability.MaybeNull;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static net.bytebuddy.matcher.ElementMatchers.any;

@HashCodeAndEqualsPlugin.Enhance
public class CustomAgentBuilder implements AgentBuilder {

    /**
     * The name of the Byte Buddy {@code net.bytebuddy.agent.Installer} class.
     */
    private static final String INSTALLER_TYPE = "net.bytebuddy.agent.Installer";

    /**
     * The name of the getter for {@code net.bytebuddy.agent.Installer} to read the {@link Instrumentation}.
     */
    private static final String INSTALLER_GETTER = "getInstrumentation";

    /**
     * The value that is to be returned from a {@link java.lang.instrument.ClassFileTransformer} to indicate
     * that no class file transformation is to be applied.
     */
    @AlwaysNull
    private static final byte[] NO_TRANSFORMATION = null;

    /**
     * A type-safe constant to express that a class is not already loaded when applying a class file transformer.
     */
    @AlwaysNull
    private static final Class<?> NOT_PREVIOUSLY_DEFINED = null;

    /**
     * A dipatcher to use for interacting with the instrumentation API.
     */
    private static final Dispatcher DISPATCHER = doPrivileged(JavaDispatcher.of(Dispatcher.class));

    /**
     * The default circularity lock that assures that no agent created by any agent builder within this
     * class loader causes a class loading circularity.
     */
    private static final CircularityLock DEFAULT_LOCK = new CircularityLock.Default();

    /**
     * The {@link net.bytebuddy.ByteBuddy} instance to be used.
     */
    protected final ByteBuddy byteBuddy;

    /**
     * The listener to notify on transformations.
     */
    protected final Listener listener;

    /**
     * The circularity lock to use.
     */
    protected final CircularityLock circularityLock;

    /**
     * The pool strategy to use.
     */
    protected final PoolStrategy poolStrategy;

    /**
     * The definition handler to use.
     */
    protected final TypeStrategy typeStrategy;

    /**
     * The location strategy to use.
     */
    protected final LocationStrategy locationStrategy;

    /**
     * The native method strategy to use.
     */
    protected final NativeMethodStrategy nativeMethodStrategy;

    /**
     * The warmup strategy to use.
     */
    protected final WarmupStrategy warmupStrategy;

    /**
     * A decorator to wrap the created class file transformer.
     */
    protected final TransformerDecorator transformerDecorator;

    /**
     * The initialization strategy to use for creating classes.
     */
    protected final InitializationStrategy initializationStrategy;

    /**
     * The redefinition strategy to apply.
     */
    protected final RedefinitionStrategy redefinitionStrategy;

    /**
     * The discovery strategy for loaded types to be redefined.
     */
    protected final RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy;

    /**
     * The batch allocator for the redefinition strategy to apply.
     */
    protected final RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator;

    /**
     * The redefinition listener for the redefinition strategy to apply.
     */
    protected final RedefinitionStrategy.Listener redefinitionListener;

    /**
     * The resubmission strategy to apply.
     */
    protected final RedefinitionStrategy.ResubmissionStrategy redefinitionResubmissionStrategy;

    /**
     * The injection strategy for injecting classes into a class loader.
     */
    protected final InjectionStrategy injectionStrategy;

    /**
     * A strategy to determine of the {@code LambdaMetafactory} should be instrumented to allow for the instrumentation
     * of classes that represent lambda expressions.
     */
    protected final LambdaInstrumentationStrategy lambdaInstrumentationStrategy;

    /**
     * The description strategy for resolving type descriptions for types.
     */
    protected final DescriptionStrategy descriptionStrategy;

    /**
     * The fallback strategy to apply.
     */
    protected final FallbackStrategy fallbackStrategy;

    /**
     * The class file buffer strategy to use.
     */
    protected final ClassFileBufferStrategy classFileBufferStrategy;

    /**
     * The installation listener to notify.
     */
    protected final InstallationListener installationListener;

    /**
     * Identifies types that should not be instrumented.
     */
    protected final RawMatcher ignoreMatcher;

    /**
     * The transformation object for handling type transformations.
     */
    protected final List<CustomAgentBuilder.Transformation> transformations;


    protected final String module;

    /**
     * Creates a new default agent builder that uses a default {@link net.bytebuddy.ByteBuddy} instance for creating classes.
     */
    public CustomAgentBuilder(String module) {
        this(new ByteBuddy(), module);
    }

    /**
     * Creates a new agent builder with default settings. By default, Byte Buddy ignores any types loaded by the bootstrap class loader, any
     * type within a {@code net.bytebuddy} package and any synthetic type. Self-injection and rebasing is enabled. In order to avoid class format
     * changes, set {@link AgentBuilder#disableClassFormatChanges()}. All types are parsed without their debugging information
     * ({@link PoolStrategy.Default#FAST}).
     *
     * @param byteBuddy The Byte Buddy instance to be used.
     */
    public CustomAgentBuilder(ByteBuddy byteBuddy, String module) {
        this(byteBuddy,
                Listener.NoOp.INSTANCE,
                DEFAULT_LOCK,
                PoolStrategy.Default.FAST,
                TypeStrategy.Default.REBASE,
                LocationStrategy.ForClassLoader.STRONG,
                CustomAgentBuilder.NativeMethodStrategy.Disabled.INSTANCE,
                CustomAgentBuilder.WarmupStrategy.NoOp.INSTANCE,
                TransformerDecorator.NoOp.INSTANCE,
                new InitializationStrategy.SelfInjection.Split(),
                RedefinitionStrategy.DISABLED,
                RedefinitionStrategy.DiscoveryStrategy.SinglePass.INSTANCE,
                RedefinitionStrategy.BatchAllocator.ForTotal.INSTANCE,
                new MyRedefinitionListener(module),
                RedefinitionStrategy.ResubmissionStrategy.Disabled.INSTANCE,
                InjectionStrategy.UsingReflection.INSTANCE,
                LambdaInstrumentationStrategy.DISABLED,
                DescriptionStrategy.Default.HYBRID,
                FallbackStrategy.ByThrowableType.ofOptionalTypes(),
                ClassFileBufferStrategy.Default.RETAINING,
                InstallationListener.NoOp.INSTANCE,
                new RawMatcher.Disjunction(
//                        new RawMatcher.ForElementMatchers(any(), isBootstrapClassLoader().or(isExtensionClassLoader())),
                        new RawMatcher.ForElementMatchers(named("java.lang.Object").or(named("java.io.Serializable"))),
                        new RawMatcher.ForElementMatchers(nameStartsWith("net.bytebuddy.")
                                .and(not(ElementMatchers.nameStartsWith(NamingStrategy.BYTE_BUDDY_RENAME_PACKAGE + ".")))
                                .or(nameStartsWith("sun.reflect.").or(nameStartsWith("jdk.internal.reflect.")))
                                .<TypeDescription>or(isSynthetic()))),
                Collections.<CustomAgentBuilder.Transformation>emptyList(),
                module);
    }

    /**
     * Creates a new default agent builder.
     *
     * @param byteBuddy                        The Byte Buddy instance to be used.
     * @param listener                         The listener to notify on transformations.
     * @param circularityLock                  The circularity lock to use.
     * @param poolStrategy                     The pool strategy to use.
     * @param typeStrategy                     The definition handler to use.
     * @param locationStrategy                 The location strategy to use.
     * @param nativeMethodStrategy             The native method strategy to apply.
     * @param warmupStrategy                   The warmup strategy to use.
     * @param transformerDecorator             A decorator to wrap the created class file transformer.
     * @param initializationStrategy           The initialization strategy to use for transformed types.
     * @param redefinitionStrategy             The redefinition strategy to apply.
     * @param redefinitionDiscoveryStrategy    The discovery strategy for loaded types to be redefined.
     * @param redefinitionBatchAllocator       The batch allocator for the redefinition strategy to apply.
     * @param redefinitionListener             The redefinition listener for the redefinition strategy to apply.
     * @param redefinitionResubmissionStrategy The resubmission strategy to apply.
     * @param injectionStrategy                The injection strategy for injecting classes into a class loader.
     * @param lambdaInstrumentationStrategy    A strategy to determine of the {@code LambdaMetafactory} should be instrumented to allow for the
     *                                         instrumentation of classes that represent lambda expressions.
     * @param descriptionStrategy              The description strategy for resolving type descriptions for types.
     * @param fallbackStrategy                 The fallback strategy to apply.
     * @param classFileBufferStrategy          The class file buffer strategy to use.
     * @param installationListener             The installation listener to notify.
     * @param ignoreMatcher                    Identifies types that should not be instrumented.
     * @param transformations                  The transformations to apply for any non-ignored type.
     */
    protected CustomAgentBuilder(ByteBuddy byteBuddy,
                                 Listener listener,
                                 CircularityLock circularityLock,
                                 PoolStrategy poolStrategy,
                                 TypeStrategy typeStrategy,
                                 LocationStrategy locationStrategy,
                                 NativeMethodStrategy nativeMethodStrategy,
                                 WarmupStrategy warmupStrategy,
                                 TransformerDecorator transformerDecorator,
                                 InitializationStrategy initializationStrategy,
                                 RedefinitionStrategy redefinitionStrategy,
                                 RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy,
                                 RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator,
                                 RedefinitionStrategy.Listener redefinitionListener,
                                 RedefinitionStrategy.ResubmissionStrategy redefinitionResubmissionStrategy,
                                 InjectionStrategy injectionStrategy,
                                 LambdaInstrumentationStrategy lambdaInstrumentationStrategy,
                                 DescriptionStrategy descriptionStrategy,
                                 FallbackStrategy fallbackStrategy,
                                 ClassFileBufferStrategy classFileBufferStrategy,
                                 InstallationListener installationListener,
                                 RawMatcher ignoreMatcher,
                                 List<CustomAgentBuilder.Transformation> transformations, String module) {
        this.byteBuddy = byteBuddy;
        this.listener = listener;
        this.circularityLock = circularityLock;
        this.poolStrategy = poolStrategy;
        this.typeStrategy = typeStrategy;
        this.locationStrategy = locationStrategy;
        this.nativeMethodStrategy = nativeMethodStrategy;
        this.warmupStrategy = warmupStrategy;
        this.transformerDecorator = transformerDecorator;
        this.initializationStrategy = initializationStrategy;
        this.redefinitionStrategy = redefinitionStrategy;
        this.redefinitionDiscoveryStrategy = redefinitionDiscoveryStrategy;
        this.redefinitionBatchAllocator = redefinitionBatchAllocator;
        this.redefinitionListener = redefinitionListener;
        this.redefinitionResubmissionStrategy = redefinitionResubmissionStrategy;
        this.injectionStrategy = injectionStrategy;
        this.lambdaInstrumentationStrategy = lambdaInstrumentationStrategy;
        this.descriptionStrategy = descriptionStrategy;
        this.fallbackStrategy = fallbackStrategy;
        this.classFileBufferStrategy = classFileBufferStrategy;
        this.installationListener = installationListener;
        this.ignoreMatcher = ignoreMatcher;
        this.transformations = transformations;
        this.module = module;
    }

    /**
     * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
     *
     * @param action The action to execute from a privileged context.
     * @param <T>    The type of the action's resolved value.
     * @return The action's resolved value.
     */
    @AccessControllerPlugin.Enhance
    private static <T> T doPrivileged(PrivilegedAction<T> action) {
        return action.run();
    }

    /**
     * Creates an {@link AgentBuilder} that realizes the provided build plugins. As {@link EntryPoint}, {@link EntryPoint.Default#REBASE} is implied.
     *
     * @param plugin The build plugins to apply as a Java agent.
     * @return An appropriate agent builder.
     */
    public static AgentBuilder of(Plugin... plugin) {
        return of(Arrays.asList(plugin));
    }

    /**
     * Creates an {@link AgentBuilder} that realizes the provided build plugins. As {@link EntryPoint}, {@link EntryPoint.Default#REBASE} is implied.
     *
     * @param plugins The build plugins to apply as a Java agent.
     * @return An appropriate agent builder.
     */
    public static AgentBuilder of(List<? extends Plugin> plugins) {
        return of(EntryPoint.Default.REBASE, plugins);
    }

    /**
     * Creates an {@link AgentBuilder} that realizes the provided build plugins.
     *
     * @param entryPoint The build entry point to use.
     * @param plugin     The build plugins to apply as a Java agent.
     * @return An appropriate agent builder.
     */
    public static AgentBuilder of(EntryPoint entryPoint, Plugin... plugin) {
        return of(entryPoint, Arrays.asList(plugin));
    }

    /**
     * Creates an {@link AgentBuilder} that realizes the provided build plugins.
     *
     * @param entryPoint The build entry point to use.
     * @param plugins    The build plugins to apply as a Java agent.
     * @return An appropriate agent builder.
     */
    public static AgentBuilder of(EntryPoint entryPoint, List<? extends Plugin> plugins) {
        return of(entryPoint, ClassFileVersion.ofThisVm(ClassFileVersion.JAVA_V5), plugins);
    }

    /**
     * Creates an {@link AgentBuilder} that realizes the provided build plugins. As {@link EntryPoint}, {@link EntryPoint.Default#REBASE} is implied.
     *
     * @param classFileVersion The class file version to use.
     * @param plugin           The build plugins to apply as a Java agent.
     * @return An appropriate agent builder.
     */
    public static AgentBuilder of(ClassFileVersion classFileVersion, Plugin... plugin) {
        return of(classFileVersion, Arrays.asList(plugin));
    }

    /**
     * Creates an {@link AgentBuilder} that realizes the provided build plugins. As {@link EntryPoint}, {@link EntryPoint.Default#REBASE} is implied.
     *
     * @param classFileVersion The class file version to use.
     * @param plugins          The build plugins to apply as a Java agent.
     * @return An appropriate agent builder.
     */
    public static AgentBuilder of(ClassFileVersion classFileVersion, List<? extends Plugin> plugins) {
        return of(EntryPoint.Default.REBASE, classFileVersion, plugins);
    }

    /**
     * Creates an {@link AgentBuilder} that realizes the provided build plugins.
     *
     * @param entryPoint       The build entry point to use.
     * @param classFileVersion The class file version to use.
     * @param plugin           The build plugins to apply as a Java agent.
     * @return An appropriate agent builder.
     */
    public static AgentBuilder of(EntryPoint entryPoint, ClassFileVersion classFileVersion, Plugin... plugin) {
        return of(entryPoint, classFileVersion, Arrays.asList(plugin));
    }

    /**
     * Creates an {@link AgentBuilder} that realizes the provided build plugins.
     *
     * @param entryPoint       The build entry point to use.
     * @param classFileVersion The class file version to use.
     * @param plugins          The build plugins to apply as a Java agent.
     * @return An appropriate agent builder.
     */
    public static AgentBuilder of(EntryPoint entryPoint, ClassFileVersion classFileVersion, List<? extends Plugin> plugins) {
        AgentBuilder agentBuilder = new AgentBuilder.Default(entryPoint.byteBuddy(classFileVersion)).with(new TypeStrategy.ForBuildEntryPoint(entryPoint));
        for (Plugin plugin : plugins) {
            agentBuilder = agentBuilder.type(plugin).transform(new Transformer.ForBuildPlugin(plugin));
        }
        return agentBuilder;
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder with(ByteBuddy byteBuddy) {
        return new CustomAgentBuilder(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                warmupStrategy,
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder with(Listener listener) {
        return new CustomAgentBuilder(byteBuddy,
                new Listener.Compound(this.listener, listener),
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                warmupStrategy,
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder with(CircularityLock circularityLock) {
        return new CustomAgentBuilder(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                warmupStrategy,
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder with(TypeStrategy typeStrategy) {
        return new CustomAgentBuilder(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                warmupStrategy,
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder with(PoolStrategy poolStrategy) {
        return new CustomAgentBuilder(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                warmupStrategy,
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder with(LocationStrategy locationStrategy) {
        return new CustomAgentBuilder(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                warmupStrategy,
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder enableNativeMethodPrefix(String prefix) {
        return new CustomAgentBuilder(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                CustomAgentBuilder.NativeMethodStrategy.ForPrefix.of(prefix),
                warmupStrategy,
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder disableNativeMethodPrefix() {
        return new CustomAgentBuilder(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                CustomAgentBuilder.NativeMethodStrategy.Disabled.INSTANCE,
                warmupStrategy,
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder warmUp(Class<?>... type) {
        return warmUp(Arrays.asList(type));
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder warmUp(Collection<Class<?>> types) {
        if (types.isEmpty()) {
            return this;
        }
        for (Class<?> type : types) {
            if (type.isPrimitive() || type.isArray()) {
                throw new IllegalArgumentException("Cannot warm up primitive or array type: " + type);
            }
        }
        return new CustomAgentBuilder(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                warmupStrategy.with(types),
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder with(TransformerDecorator transformerDecorator) {
        return new CustomAgentBuilder(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                warmupStrategy,
                new TransformerDecorator.Compound(this.transformerDecorator, transformerDecorator),
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public RedefinitionListenable.WithoutBatchStrategy with(RedefinitionStrategy redefinitionStrategy) {
        return new CustomAgentBuilder.Redefining(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                warmupStrategy,
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                RedefinitionStrategy.DiscoveryStrategy.SinglePass.INSTANCE,
                RedefinitionStrategy.BatchAllocator.ForTotal.INSTANCE,
                new MyRedefinitionListener(module),
                RedefinitionStrategy.ResubmissionStrategy.Disabled.INSTANCE,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder with(InitializationStrategy initializationStrategy) {
        return new CustomAgentBuilder(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                warmupStrategy,
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder with(LambdaInstrumentationStrategy lambdaInstrumentationStrategy) {
        return new CustomAgentBuilder(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                warmupStrategy,
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder with(DescriptionStrategy descriptionStrategy) {
        return new CustomAgentBuilder(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                warmupStrategy,
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder with(FallbackStrategy fallbackStrategy) {
        return new CustomAgentBuilder(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                warmupStrategy,
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder with(ClassFileBufferStrategy classFileBufferStrategy) {
        return new CustomAgentBuilder(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                warmupStrategy,
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder with(InstallationListener installationListener) {
        return new CustomAgentBuilder(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                warmupStrategy,
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                new InstallationListener.Compound(this.installationListener, installationListener),
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder with(InjectionStrategy injectionStrategy) {
        return new CustomAgentBuilder(byteBuddy,
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                warmupStrategy,
                transformerDecorator,
                initializationStrategy,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder disableClassFormatChanges() {
        return new CustomAgentBuilder(byteBuddy.with(Implementation.Context.Disabled.Factory.INSTANCE),
                listener,
                circularityLock,
                poolStrategy,
                typeStrategy == TypeStrategy.Default.DECORATE
                        ? TypeStrategy.Default.DECORATE
                        : TypeStrategy.Default.REDEFINE_FROZEN,
                locationStrategy,
                CustomAgentBuilder.NativeMethodStrategy.Disabled.INSTANCE,
                warmupStrategy,
                transformerDecorator,
                InitializationStrategy.NoOp.INSTANCE,
                redefinitionStrategy,
                redefinitionDiscoveryStrategy,
                redefinitionBatchAllocator,
                redefinitionListener,
                redefinitionResubmissionStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                transformations, module);
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder assureReadEdgeTo(Instrumentation instrumentation, Class<?>... type) {
        return JavaModule.isSupported()
                ? with(Listener.ModuleReadEdgeCompleting.of(instrumentation, false, type))
                : this;
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder assureReadEdgeTo(Instrumentation instrumentation, JavaModule... module) {
        return assureReadEdgeTo(instrumentation, Arrays.asList(module));
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder assureReadEdgeTo(Instrumentation instrumentation, Collection<? extends JavaModule> modules) {
        return with(new Listener.ModuleReadEdgeCompleting(instrumentation, false, new HashSet<JavaModule>(modules)));
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder assureReadEdgeFromAndTo(Instrumentation instrumentation, Class<?>... type) {
        return JavaModule.isSupported()
                ? with(Listener.ModuleReadEdgeCompleting.of(instrumentation, true, type))
                : this;
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder assureReadEdgeFromAndTo(Instrumentation instrumentation, JavaModule... module) {
        return assureReadEdgeFromAndTo(instrumentation, Arrays.asList(module));
    }

    /**
     * {@inheritDoc}
     */
    public AgentBuilder assureReadEdgeFromAndTo(Instrumentation instrumentation, Collection<? extends JavaModule> modules) {
        return with(new Listener.ModuleReadEdgeCompleting(instrumentation, true, new HashSet<JavaModule>(modules)));
    }

    /**
     * {@inheritDoc}
     */
    public Identified.Narrowable type(RawMatcher matcher) {
        return new CustomAgentBuilder.Transforming(matcher, Collections.<Transformer>emptyList(), false);
    }

    /**
     * {@inheritDoc}
     */
    public Identified.Narrowable type(ElementMatcher<? super TypeDescription> typeMatcher) {
        return type(typeMatcher, any());
    }

    /**
     * {@inheritDoc}
     */
    public Identified.Narrowable type(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher) {
        return type(typeMatcher, classLoaderMatcher, any());
    }

    /**
     * {@inheritDoc}
     */
    public Identified.Narrowable type(ElementMatcher<? super TypeDescription> typeMatcher,
                                      ElementMatcher<? super ClassLoader> classLoaderMatcher,
                                      ElementMatcher<? super JavaModule> moduleMatcher) {
        return type(new RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, not(supportsModules()).or(moduleMatcher)));
    }

    /**
     * {@inheritDoc}
     */
    public Ignored ignore(ElementMatcher<? super TypeDescription> typeMatcher) {
        return ignore(typeMatcher, any());
    }

    /**
     * {@inheritDoc}
     */
    public Ignored ignore(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher) {
        return ignore(typeMatcher, classLoaderMatcher, any());
    }

    /**
     * {@inheritDoc}
     */
    public Ignored ignore(ElementMatcher<? super TypeDescription> typeMatcher,
                          ElementMatcher<? super ClassLoader> classLoaderMatcher,
                          ElementMatcher<? super JavaModule> moduleMatcher) {
        return ignore(new RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, not(supportsModules()).or(moduleMatcher)));
    }

    /**
     * {@inheritDoc}
     */
    public Ignored ignore(RawMatcher rawMatcher) {
        return new CustomAgentBuilder.Ignoring(rawMatcher);
    }

    /**
     * {@inheritDoc}
     */
    public ResettableClassFileTransformer makeRaw() {
        return makeRaw(listener, InstallationListener.NoOp.INSTANCE, RedefinitionStrategy.ResubmissionEnforcer.Disabled.INSTANCE);
    }

    /**
     * Creates a new class file transformer with a given listener.
     *
     * @param listener             The listener to supply.
     * @param installationListener The installation listener to notify.
     * @param resubmissionEnforcer The resubmission enforcer to use.
     * @return The resettable class file transformer to use.
     */
    private ResettableClassFileTransformer makeRaw(Listener listener,
                                                   InstallationListener installationListener,
                                                   RedefinitionStrategy.ResubmissionEnforcer resubmissionEnforcer) {
        return CustomAgentBuilder.ExecutingTransformer.FACTORY.make(byteBuddy,
                listener,
                poolStrategy,
                typeStrategy,
                locationStrategy,
                nativeMethodStrategy,
                initializationStrategy,
                injectionStrategy,
                lambdaInstrumentationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                classFileBufferStrategy,
                installationListener,
                ignoreMatcher,
                resubmissionEnforcer,
                transformations,
                circularityLock);
    }

    /**
     * Resolves the instrumentation provided by {@code net.bytebuddy.agent.Installer}.
     *
     * @return The installed instrumentation instance.
     */
    private static Instrumentation resolveByteBuddyAgentInstrumentation() {
        try {
            Class<?> installer = ClassLoader.getSystemClassLoader().loadClass(INSTALLER_TYPE);
            JavaModule source = JavaModule.ofType(AgentBuilder.class), target = JavaModule.ofType(installer);
            if (source != null && !source.canRead(target)) {
                Class<?> module = Class.forName("java.lang.Module");
                module.getMethod("addReads", module).invoke(source.unwrap(), target.unwrap());
            }
            return (Instrumentation) installer.getMethod(INSTALLER_GETTER).invoke(null);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("The Byte Buddy agent is not installed or not accessible", exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ResettableClassFileTransformer installOn(Instrumentation instrumentation) {
        if (!circularityLock.acquire()) {
            throw new IllegalStateException("Could not acquire the circularity lock upon installation.");
        }
        try {
            return doInstall(instrumentation, new CustomAgentBuilder.Transformation.SimpleMatcher(ignoreMatcher, transformations));
        } finally {
            circularityLock.release();
        }
    }

    /**
     * {@inheritDoc}
     */
    public ResettableClassFileTransformer installOnByteBuddyAgent() {
        return installOn(resolveByteBuddyAgentInstrumentation());
    }

    /**
     * {@inheritDoc}
     */
    public ResettableClassFileTransformer patchOn(Instrumentation instrumentation, ResettableClassFileTransformer classFileTransformer) {
        if (!circularityLock.acquire()) {
            throw new IllegalStateException("Could not acquire the circularity lock upon installation.");
        }
        try {
            if (!classFileTransformer.reset(instrumentation, RedefinitionStrategy.DISABLED)) {
                throw new IllegalArgumentException("Cannot patch unregistered class file transformer: " + classFileTransformer);
            }
            return doInstall(instrumentation, new CustomAgentBuilder.Transformation.DifferentialMatcher(ignoreMatcher, transformations, classFileTransformer));
        } finally {
            circularityLock.release();
        }
    }

    /**
     * {@inheritDoc}
     */
    public ResettableClassFileTransformer patchOnByteBuddyAgent(ResettableClassFileTransformer classFileTransformer) {
        return patchOn(resolveByteBuddyAgentInstrumentation(), classFileTransformer);
    }

    /**
     * Installs the class file transformer.
     *
     * @param instrumentation The instrumentation to install the matcher on.
     * @param matcher         The matcher to identify redefined types.
     * @return The created class file transformer.
     */
    private ResettableClassFileTransformer doInstall(Instrumentation instrumentation, RawMatcher matcher) {
        RedefinitionStrategy.ResubmissionStrategy.Installation installation = redefinitionResubmissionStrategy.apply(instrumentation,
                poolStrategy,
                locationStrategy,
                descriptionStrategy,
                fallbackStrategy,
                listener,
                installationListener,
                circularityLock,
                new CustomAgentBuilder.Transformation.SimpleMatcher(ignoreMatcher, transformations),
                redefinitionStrategy,
                redefinitionBatchAllocator,
                redefinitionListener);
        ResettableClassFileTransformer classFileTransformer = transformerDecorator.decorate(makeRaw(installation.getListener(),
                installation.getInstallationListener(),
                installation.getResubmissionEnforcer()));
        installation.getInstallationListener().onBeforeInstall(instrumentation, classFileTransformer);
        try {
            warmupStrategy.apply(classFileTransformer,
                    locationStrategy,
                    redefinitionStrategy,
                    circularityLock,
                    installation.getInstallationListener());
            if (redefinitionStrategy.isRetransforming()) {
                DISPATCHER.addTransformer(instrumentation, classFileTransformer, true);
            } else {
                instrumentation.addTransformer(classFileTransformer);
            }
            nativeMethodStrategy.apply(instrumentation, classFileTransformer);
            lambdaInstrumentationStrategy.apply(byteBuddy, instrumentation, classFileTransformer);
            redefinitionStrategy.apply(instrumentation,
                    poolStrategy,
                    locationStrategy,
                    descriptionStrategy,
                    fallbackStrategy,
                    redefinitionDiscoveryStrategy,
                    lambdaInstrumentationStrategy,
                    installation.getListener(),
                    redefinitionListener,
                    matcher,
                    redefinitionBatchAllocator,
                    circularityLock);
        } catch (Throwable throwable) {
            throwable = installation.getInstallationListener().onError(instrumentation, classFileTransformer, throwable);
            if (throwable != null) {
                instrumentation.removeTransformer(classFileTransformer);
                throw new IllegalStateException("Could not install class file transformer", throwable);
            }
        }
        installation.getInstallationListener().onInstall(instrumentation, classFileTransformer);
        return classFileTransformer;
    }

    /**
     * A dispatcher for interacting with the instrumentation API.
     */
    @JavaDispatcher.Proxied("java.lang.instrument.Instrumentation")
    protected interface Dispatcher {

        /**
         * Returns {@code true} if the supplied instrumentation instance supports setting native method prefixes.
         *
         * @param instrumentation The instrumentation instance to use.
         * @return {@code true} if the supplied instrumentation instance supports native method prefixes.
         */
        @JavaDispatcher.Defaults
        boolean isNativeMethodPrefixSupported(Instrumentation instrumentation);

        /**
         * Sets a native method prefix for the supplied class file transformer.
         *
         * @param instrumentation      The instrumentation instance to use.
         * @param classFileTransformer The class file transformer for which the prefix is set.
         * @param prefix               The prefix to set.
         */
        void setNativeMethodPrefix(Instrumentation instrumentation, ClassFileTransformer classFileTransformer, String prefix);

        /**
         * Adds a class file transformer to an instrumentation instance.
         *
         * @param instrumentation      The instrumentation instance to use for registration.
         * @param classFileTransformer The class file transformer to register.
         * @param canRetransform       {@code true} if the class file transformer is capable of retransformation.
         */
        void addTransformer(Instrumentation instrumentation, ClassFileTransformer classFileTransformer, boolean canRetransform);
    }

    /**
     * A strategy for determining if a native method name prefix should be used when rebasing methods.
     */
    protected interface NativeMethodStrategy {

        /**
         * Resolves the method name transformer for this strategy.
         *
         * @return A method name transformer for this strategy.
         */
        MethodNameTransformer resolve();

        /**
         * Applies this native method strategy.
         *
         * @param instrumentation      The instrumentation to apply this strategy upon.
         * @param classFileTransformer The class file transformer being registered.
         */
        void apply(Instrumentation instrumentation, ClassFileTransformer classFileTransformer);

        /**
         * A native method strategy that suffixes method names with a random suffix and disables native method rebasement.
         */
        enum Disabled implements CustomAgentBuilder.NativeMethodStrategy {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public MethodNameTransformer resolve() {
                return MethodNameTransformer.Suffixing.withRandomSuffix();
            }

            /**
             * {@inheritDoc}
             */
            public void apply(Instrumentation instrumentation, ClassFileTransformer classFileTransformer) {
                /* do nothing */
            }
        }

        /**
         * A native method strategy that prefixes method names with a fixed value for supporting rebasing of native methods.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForPrefix implements CustomAgentBuilder.NativeMethodStrategy {

            /**
             * The method name prefix.
             */
            private final String prefix;

            /**
             * Creates a new name prefixing native method strategy.
             *
             * @param prefix The method name prefix.
             */
            protected ForPrefix(String prefix) {
                this.prefix = prefix;
            }

            /**
             * Creates a new native method strategy for prefixing method names.
             *
             * @param prefix The method name prefix.
             * @return An appropriate native method strategy.
             */
            protected static CustomAgentBuilder.NativeMethodStrategy of(String prefix) {
                if (prefix.length() == 0) {
                    throw new IllegalArgumentException("A method name prefix must not be the empty string");
                }
                return new ForPrefix(prefix);
            }

            /**
             * {@inheritDoc}
             */
            public MethodNameTransformer resolve() {
                return new MethodNameTransformer.Prefixing(prefix);
            }

            /**
             * {@inheritDoc}
             */
            public void apply(Instrumentation instrumentation, ClassFileTransformer classFileTransformer) {
                if (!DISPATCHER.isNativeMethodPrefixSupported(instrumentation)) {
                    throw new IllegalArgumentException("A prefix for native methods is not supported: " + instrumentation);
                }
                DISPATCHER.setNativeMethodPrefix(instrumentation, classFileTransformer, prefix);
            }
        }
    }

    /**
     * A strategy to warm up a {@link ClassFileTransformer} before using it to eagerly load classes and to avoid
     * circularity errors when classes are loaded during actual transformation for the first time.
     */
    protected interface WarmupStrategy {

        /**
         * Applies this warm up strategy.
         *
         * @param classFileTransformer The class file transformer to warm up.
         * @param locationStrategy     The location strategy to use.
         * @param redefinitionStrategy The redefinition strategy being used.
         * @param circularityLock      The circularity lock to use.
         * @param listener             The listener to notify over warmup events.
         */
        void apply(ResettableClassFileTransformer classFileTransformer,
                   LocationStrategy locationStrategy,
                   RedefinitionStrategy redefinitionStrategy,
                   CircularityLock circularityLock,
                   InstallationListener listener);

        /**
         * Adds the provided types to this warmup strategy.
         *
         * @param types The types to add.
         * @return An appropriate warmup strategy.
         */
        CustomAgentBuilder.WarmupStrategy with(Collection<Class<?>> types);

        /**
         * A non-operational warmup strategy.
         */
        enum NoOp implements CustomAgentBuilder.WarmupStrategy {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public void apply(ResettableClassFileTransformer classFileTransformer,
                              LocationStrategy locationStrategy,
                              RedefinitionStrategy redefinitionStrategy,
                              CircularityLock circularityLock,
                              InstallationListener listener) {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public CustomAgentBuilder.WarmupStrategy with(Collection<Class<?>> types) {
                return new Enabled(new LinkedHashSet<Class<?>>(types));
            }
        }

        /**
         * An enabled warmup strategy.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Enabled implements CustomAgentBuilder.WarmupStrategy {

            /**
             * A dispatcher for invoking a {@link ClassFileTransformer} when the module system is available.
             */
            private static final Enabled.Dispatcher DISPATCHER = CustomAgentBuilder.doPrivileged(JavaDispatcher.of(Enabled.Dispatcher.class));

            /**
             * The types to warm up.
             */
            private final Set<Class<?>> types;

            /**
             * Creates a new enabled warmup strategy.
             *
             * @param types The types to warm up.
             */
            protected Enabled(Set<Class<?>> types) {
                this.types = types;
            }

            /**
             * {@inheritDoc}
             */
            public void apply(ResettableClassFileTransformer classFileTransformer,
                              LocationStrategy locationStrategy,
                              RedefinitionStrategy redefinitionStrategy,
                              CircularityLock circularityLock,
                              InstallationListener listener) {
                listener.onBeforeWarmUp(types, classFileTransformer);
                boolean transformed = false;
                Map<Class<?>, byte[]> results = new LinkedHashMap<Class<?>, byte[]>();
                for (Class<?> type : types) {
                    try {
                        JavaModule module = JavaModule.ofType(type);
                        byte[] binaryRepresentation = locationStrategy.classFileLocator(type.getClassLoader(), module)
                                .locate(type.getName())
                                .resolve();
                        circularityLock.release();
                        try {
                            byte[] result;
                            if (module == null) {
                                result = classFileTransformer.transform(type.getClassLoader(),
                                        Type.getInternalName(type),
                                        NOT_PREVIOUSLY_DEFINED,
                                        type.getProtectionDomain(),
                                        binaryRepresentation);
                                transformed |= result != null;
                                if (redefinitionStrategy.isEnabled()) {
                                    result = classFileTransformer.transform(type.getClassLoader(),
                                            Type.getInternalName(type),
                                            type,
                                            type.getProtectionDomain(),
                                            binaryRepresentation);
                                    transformed |= result != null;
                                }
                            } else {
                                result = DISPATCHER.transform(classFileTransformer,
                                        module.unwrap(),
                                        type.getClassLoader(),
                                        Type.getInternalName(type),
                                        NOT_PREVIOUSLY_DEFINED,
                                        type.getProtectionDomain(),
                                        binaryRepresentation);
                                transformed |= result != null;
                                if (redefinitionStrategy.isEnabled()) {
                                    result = DISPATCHER.transform(classFileTransformer,
                                            module.unwrap(),
                                            type.getClassLoader(),
                                            Type.getInternalName(type),
                                            type,
                                            type.getProtectionDomain(),
                                            binaryRepresentation);
                                    transformed |= result != null;
                                }
                            }
                            results.put(type, result);
                        } finally {
                            circularityLock.acquire();
                        }
                    } catch (Throwable throwable) {
                        listener.onWarmUpError(type, classFileTransformer, throwable);
                        results.put(type, NO_TRANSFORMATION);
                    }
                }
                listener.onAfterWarmUp(results, classFileTransformer, transformed);
            }

            /**
             * {@inheritDoc}
             */
            public CustomAgentBuilder.WarmupStrategy with(Collection<Class<?>> types) {
                Set<Class<?>> combined = new LinkedHashSet<Class<?>>(this.types);
                combined.addAll(types);
                return new Enabled(combined);
            }

            /**
             * A dispatcher to interact with a {@link ClassFileTransformer} when the module system is active.
             */
            @JavaDispatcher.Proxied("java.lang.instrument.ClassFileTransformer")
            protected interface Dispatcher {

                /**
                 * Transforms a class.
                 *
                 * @param target               The transformer to use for transformation.
                 * @param module               The Java module of the transformed class.
                 * @param classLoader          The class loader of the transformed class or {@code null} if loaded by the boot loader.
                 * @param name                 The internal name of the transformed class.
                 * @param classBeingRedefined  The class being redefined or {@code null} if not a retransformation.
                 * @param protectionDomain     The class's protection domain.
                 * @param binaryRepresentation The class's binary representation.
                 * @return The transformed class file or {@code null} if untransformed.
                 * @throws IllegalClassFormatException If the class file cannot be generated.
                 */
                @MaybeNull
                byte[] transform(ClassFileTransformer target,
                                 @MaybeNull @JavaDispatcher.Proxied("java.lang.Module") Object module,
                                 @MaybeNull ClassLoader classLoader,
                                 String name,
                                 @MaybeNull Class<?> classBeingRedefined,
                                 ProtectionDomain protectionDomain,
                                 byte[] binaryRepresentation) throws IllegalClassFormatException;
            }
        }
    }

    /**
     * A transformation to apply.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class Transformation {

        /**
         * Indicates that a type should not be ignored.
         */
        @AlwaysNull
        private static final byte[] NONE = null;

        /**
         * The matcher to identify types for transformation.
         */
        private final RawMatcher matcher;

        /**
         * A list of transformers to apply.
         */
        private final List<Transformer> transformers;

        /**
         * {@code true} if this transformation is terminal.
         */
        private final boolean terminal;

        /**
         * Creates a new transformation.
         *
         * @param matcher      The matcher to identify types eligable for transformation.
         * @param transformers A list of transformers to apply.
         * @param terminal     Indicates that this transformation is terminal.
         */
        protected Transformation(RawMatcher matcher, List<Transformer> transformers, boolean terminal) {
            this.matcher = matcher;
            this.transformers = transformers;
            this.terminal = terminal;
        }

        /**
         * Returns the matcher to identify types for transformation.
         *
         * @return The matcher to identify types for transformation.
         */
        protected RawMatcher getMatcher() {
            return matcher;
        }

        /**
         * Returns a list of transformers to apply.
         *
         * @return A list of transformers to apply.
         */
        protected List<Transformer> getTransformers() {
            return transformers;
        }

        /**
         * Returns {@code true} if this transformation is terminal.
         *
         * @return {@code true} if this transformation is terminal.
         */
        protected boolean isTerminal() {
            return terminal;
        }

        /**
         * A matcher that matches any type that is touched by a transformer without being ignored.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class SimpleMatcher implements RawMatcher {

            /**
             * Identifies types that should not be instrumented.
             */
            private final RawMatcher ignoreMatcher;

            /**
             * The transformations to apply on non-ignored types.
             */
            private final List<CustomAgentBuilder.Transformation> transformations;

            /**
             * Creates a new simple matcher.
             *
             * @param ignoreMatcher   Identifies types that should not be instrumented.
             * @param transformations The transformations to apply on non-ignored types.
             */
            protected SimpleMatcher(RawMatcher ignoreMatcher, List<CustomAgentBuilder.Transformation> transformations) {
                this.ignoreMatcher = ignoreMatcher;
                this.transformations = transformations;
            }

            /**
             * {@inheritDoc}
             */
            public boolean matches(TypeDescription typeDescription,
                                   @MaybeNull ClassLoader classLoader,
                                   @MaybeNull JavaModule module,
                                   @MaybeNull Class<?> classBeingRedefined,
                                   ProtectionDomain protectionDomain) {
                if (ignoreMatcher.matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain)) {
                    return false;
                }
                for (CustomAgentBuilder.Transformation transformation : transformations) {
                    if (transformation.getMatcher().matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain)) {
                        return true;
                    }
                }
                return false;
            }
        }

        /**
         * A matcher that considers the differential of two transformers' transformations.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class DifferentialMatcher implements RawMatcher {

            /**
             * Identifies types that should not be instrumented.
             */
            private final RawMatcher ignoreMatcher;

            /**
             * The transformations to apply on non-ignored types.
             */
            private final List<CustomAgentBuilder.Transformation> transformations;

            /**
             * The class file transformer representing the differential.
             */
            private final ResettableClassFileTransformer classFileTransformer;

            /**
             * Creates a new differential matcher.
             *
             * @param ignoreMatcher        Identifies types that should not be instrumented.
             * @param transformations      The transformations to apply on non-ignored types.
             * @param classFileTransformer The class file transformer representing the differential.
             */
            protected DifferentialMatcher(RawMatcher ignoreMatcher,
                                          List<CustomAgentBuilder.Transformation> transformations,
                                          ResettableClassFileTransformer classFileTransformer) {
                this.ignoreMatcher = ignoreMatcher;
                this.transformations = transformations;
                this.classFileTransformer = classFileTransformer;
            }

            /**
             * {@inheritDoc}
             */
            public boolean matches(TypeDescription typeDescription,
                                   @MaybeNull ClassLoader classLoader,
                                   @MaybeNull JavaModule module,
                                   @MaybeNull Class<?> classBeingRedefined,
                                   ProtectionDomain protectionDomain) {
                Iterator<Transformer> iterator = classFileTransformer.iterator(typeDescription, classLoader, module, classBeingRedefined, protectionDomain);
                if (ignoreMatcher.matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain)) {
                    return iterator.hasNext();
                }
                for (CustomAgentBuilder.Transformation transformation : transformations) {
                    if (transformation.getMatcher().matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain)) {
                        for (Transformer transformer : transformation.getTransformers()) {
                            if (!iterator.hasNext() || !iterator.next().equals(transformer)) {
                                return true;
                            }
                        }
                    }
                }
                return iterator.hasNext();
            }
        }

        /**
         * An iterator over a list of transformations that match a raw matcher specification.
         */
        protected static class TransformerIterator implements Iterator<Transformer> {

            /**
             * A description of the matched type.
             */
            private final TypeDescription typeDescription;

            /**
             * The type's class loader.
             */
            @MaybeNull
            private final ClassLoader classLoader;

            /**
             * The type's module.
             */
            @MaybeNull
            private final JavaModule module;

            /**
             * The class being redefined or {@code null} if the type was not previously loaded.
             */
            @MaybeNull
            private final Class<?> classBeingRedefined;

            /**
             * The type's protection domain.
             */
            private final ProtectionDomain protectionDomain;

            /**
             * An iterator over the remaining transformations that were not yet considered.
             */
            private final Iterator<CustomAgentBuilder.Transformation> transformations;

            /**
             * An iterator over the currently matched transformers.
             */
            private Iterator<Transformer> transformers;

            /**
             * Creates a new iterator.
             *
             * @param typeDescription     A description of the matched type.
             * @param classLoader         The type's class loader.
             * @param module              The type's module.
             * @param classBeingRedefined The class being redefined or {@code null} if the type was not previously loaded.
             * @param protectionDomain    The type's protection domain.
             * @param transformations     The matched transformations.
             */
            protected TransformerIterator(TypeDescription typeDescription,
                                          @MaybeNull ClassLoader classLoader,
                                          @MaybeNull JavaModule module,
                                          @MaybeNull Class<?> classBeingRedefined,
                                          ProtectionDomain protectionDomain,
                                          List<CustomAgentBuilder.Transformation> transformations) {
                this.typeDescription = typeDescription;
                this.classLoader = classLoader;
                this.module = module;
                this.classBeingRedefined = classBeingRedefined;
                this.protectionDomain = protectionDomain;
                this.transformations = transformations.iterator();
                transformers = Collections.<Transformer>emptySet().iterator();
                while (!transformers.hasNext() && this.transformations.hasNext()) {
                    CustomAgentBuilder.Transformation transformation = this.transformations.next();
                    if (transformation.getMatcher().matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain)) {
                        transformers = transformation.getTransformers().iterator();
                    }
                }
            }

            /**
             * {@inheritDoc}
             */
            public boolean hasNext() {
                return transformers.hasNext();
            }

            /**
             * {@inheritDoc}
             */
            public Transformer next() {
                try {
                    return transformers.next();
                } finally {
                    while (!transformers.hasNext() && transformations.hasNext()) {
                        CustomAgentBuilder.Transformation transformation = transformations.next();
                        if (transformation.getMatcher().matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain)) {
                            transformers = transformation.getTransformers().iterator();
                        }
                    }
                }
            }

            /**
             * {@inheritDoc}
             */
            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        }
    }

    /**
     * A {@link java.lang.instrument.ClassFileTransformer} that implements the enclosing agent builder's
     * configuration.
     */
    protected static class ExecutingTransformer extends ResettableClassFileTransformer.AbstractBase {

        /**
         * A factory for creating a {@link ClassFileTransformer} that supports the features of the current VM.
         */
        protected static final CustomAgentBuilder.ExecutingTransformer.Factory FACTORY = CustomAgentBuilder.doPrivileged(CustomAgentBuilder.ExecutingTransformer.Factory.CreationAction.INSTANCE);

        /**
         * The Byte Buddy instance to be used.
         */
        private final ByteBuddy byteBuddy;

        /**
         * The pool strategy to use.
         */
        private final PoolStrategy poolStrategy;

        /**
         * The definition handler to use.
         */
        private final TypeStrategy typeStrategy;

        /**
         * The listener to notify on transformations.
         */
        private final Listener listener;

        /**
         * The native method strategy to apply.
         */
        private final CustomAgentBuilder.NativeMethodStrategy nativeMethodStrategy;

        /**
         * The initialization strategy to use for transformed types.
         */
        private final InitializationStrategy initializationStrategy;

        /**
         * The injection strategy to use.
         */
        private final InjectionStrategy injectionStrategy;

        /**
         * The lambda instrumentation strategy to use.
         */
        private final LambdaInstrumentationStrategy lambdaInstrumentationStrategy;

        /**
         * The description strategy for resolving type descriptions for types.
         */
        private final DescriptionStrategy descriptionStrategy;

        /**
         * The location strategy to use.
         */
        private final LocationStrategy locationStrategy;

        /**
         * The fallback strategy to use.
         */
        private final FallbackStrategy fallbackStrategy;

        /**
         * The class file buffer strategy to use.
         */
        private final ClassFileBufferStrategy classFileBufferStrategy;

        /**
         * The installation listener to notify.
         */
        private final InstallationListener installationListener;

        /**
         * Identifies types that should not be instrumented.
         */
        private final RawMatcher ignoreMatcher;

        /**
         * The resubmission enforcer to use.
         */
        private final RedefinitionStrategy.ResubmissionEnforcer resubmissionEnforcer;

        /**
         * The transformations to apply on non-ignored types.
         */
        private final List<CustomAgentBuilder.Transformation> transformations;

        /**
         * A lock that prevents circular class transformations.
         */
        private final CircularityLock circularityLock;

        /**
         * The access control context to use for loading classes or {@code null} if the
         * access controller is not available on the current VM.
         */
        @MaybeNull
        private final Object accessControlContext;

        /**
         * Creates a new class file transformer.
         *
         * @param byteBuddy                     The Byte Buddy instance to be used.
         * @param listener                      The listener to notify on transformations.
         * @param poolStrategy                  The pool strategy to use.
         * @param typeStrategy                  The definition handler to use.
         * @param locationStrategy              The location strategy to use.
         * @param nativeMethodStrategy          The native method strategy to apply.
         * @param initializationStrategy        The initialization strategy to use for transformed types.
         * @param injectionStrategy             The injection strategy to use.
         * @param lambdaInstrumentationStrategy The lambda instrumentation strategy to use.
         * @param descriptionStrategy           The description strategy for resolving type descriptions for types.
         * @param fallbackStrategy              The fallback strategy to use.
         * @param installationListener          The installation listener to notify.
         * @param classFileBufferStrategy       The class file buffer strategy to use.
         * @param ignoreMatcher                 Identifies types that should not be instrumented.
         * @param resubmissionEnforcer          The resubmission enforcer to use.
         * @param transformations               The transformations to apply on non-ignored types.
         * @param circularityLock               The circularity lock to use.
         */
        public ExecutingTransformer(ByteBuddy byteBuddy,
                                    Listener listener,
                                    PoolStrategy poolStrategy,
                                    TypeStrategy typeStrategy,
                                    LocationStrategy locationStrategy,
                                    CustomAgentBuilder.NativeMethodStrategy nativeMethodStrategy,
                                    InitializationStrategy initializationStrategy,
                                    InjectionStrategy injectionStrategy,
                                    LambdaInstrumentationStrategy lambdaInstrumentationStrategy,
                                    DescriptionStrategy descriptionStrategy,
                                    FallbackStrategy fallbackStrategy,
                                    ClassFileBufferStrategy classFileBufferStrategy,
                                    InstallationListener installationListener,
                                    RawMatcher ignoreMatcher,
                                    RedefinitionStrategy.ResubmissionEnforcer resubmissionEnforcer,
                                    List<CustomAgentBuilder.Transformation> transformations,
                                    CircularityLock circularityLock) {
            this.byteBuddy = byteBuddy;
            this.typeStrategy = typeStrategy;
            this.poolStrategy = poolStrategy;
            this.locationStrategy = locationStrategy;
            this.listener = listener;
            this.nativeMethodStrategy = nativeMethodStrategy;
            this.initializationStrategy = initializationStrategy;
            this.injectionStrategy = injectionStrategy;
            this.lambdaInstrumentationStrategy = lambdaInstrumentationStrategy;
            this.descriptionStrategy = descriptionStrategy;
            this.fallbackStrategy = fallbackStrategy;
            this.classFileBufferStrategy = classFileBufferStrategy;
            this.installationListener = installationListener;
            this.ignoreMatcher = ignoreMatcher;
            this.resubmissionEnforcer = resubmissionEnforcer;
            this.transformations = transformations;
            this.circularityLock = circularityLock;
            accessControlContext = getContext();
        }

        /**
         * A proxy for {@code java.security.AccessController#getContext} that is activated if available.
         *
         * @return The current access control context or {@code null} if the current VM does not support it.
         */
        @MaybeNull
        @AccessControllerPlugin.Enhance
        private static Object getContext() {
            return null;
        }

        /**
         * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
         *
         * @param action  The action to execute from a privileged context.
         * @param context The access control context or {@code null} if the current VM does not support it.
         * @param <T>     The type of the action's resolved value.
         * @return The action's resolved value.
         */
        @AccessControllerPlugin.Enhance
        private static <T> T doPrivileged(PrivilegedAction<T> action, @MaybeNull @SuppressWarnings("unused") Object context) {
            return action.run();
        }

        /**
         * {@inheritDoc}
         */
        @MaybeNull
        public byte[] transform(@MaybeNull ClassLoader classLoader,
                                @MaybeNull String internalTypeName,
                                @MaybeNull Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain,
                                byte[] binaryRepresentation) {
            if (circularityLock.acquire()) {
                try {
                    return doPrivileged(new CustomAgentBuilder.ExecutingTransformer.LegacyVmDispatcher(classLoader,
                            internalTypeName,
                            classBeingRedefined,
                            protectionDomain,
                            binaryRepresentation), accessControlContext);
                } finally {
                    circularityLock.release();
                }
            } else {
                return NO_TRANSFORMATION;
            }
        }

        /**
         * Applies a transformation for a class that was captured by this {@link ClassFileTransformer}. Invoking this method
         * allows to process module information which is available since Java 9.
         *
         * @param rawModule            The instrumented class's Java {@code java.lang.Module}.
         * @param classLoader          The type's class loader or {@code null} if the type is loaded by the bootstrap loader.
         * @param internalTypeName     The internal name of the instrumented class.
         * @param classBeingRedefined  The loaded {@link Class} being redefined or {@code null} if no such class exists.
         * @param protectionDomain     The instrumented type's protection domain.
         * @param binaryRepresentation The class file of the instrumented class in its current state.
         * @return The transformed class file or an empty byte array if this transformer does not apply an instrumentation.
         */
        @MaybeNull
        protected byte[] transform(Object rawModule,
                                   @MaybeNull ClassLoader classLoader,
                                   @MaybeNull String internalTypeName,
                                   @MaybeNull Class<?> classBeingRedefined,
                                   ProtectionDomain protectionDomain,
                                   byte[] binaryRepresentation) {
            if (circularityLock.acquire()) {
                try {
                    return doPrivileged(new CustomAgentBuilder.ExecutingTransformer.Java9CapableVmDispatcher(rawModule,
                            classLoader,
                            internalTypeName,
                            classBeingRedefined,
                            protectionDomain,
                            binaryRepresentation), accessControlContext);
                } finally {
                    circularityLock.release();
                }
            } else {
                return NO_TRANSFORMATION;
            }
        }

        /**
         * Applies a transformation for a class that was captured by this {@link ClassFileTransformer}.
         *
         * @param module               The instrumented class's Java module in its wrapped form or {@code null} if the current VM does not support modules.
         * @param classLoader          The instrumented class's class loader.
         * @param internalTypeName     The internal name of the instrumented class.
         * @param classBeingRedefined  The loaded {@link Class} being redefined or {@code null} if no such class exists.
         * @param protectionDomain     The instrumented type's protection domain.
         * @param binaryRepresentation The class file of the instrumented class in its current state.
         * @return The transformed class file or an empty byte array if this transformer does not apply an instrumentation.
         */
        @MaybeNull
        private byte[] transform(@MaybeNull JavaModule module,
                                 @MaybeNull ClassLoader classLoader,
                                 @MaybeNull String internalTypeName,
                                 @MaybeNull Class<?> classBeingRedefined,
                                 ProtectionDomain protectionDomain,
                                 byte[] binaryRepresentation) {
            if (internalTypeName == null || !lambdaInstrumentationStrategy.isInstrumented(classBeingRedefined)) {
                return NO_TRANSFORMATION;
            }
            String name = internalTypeName.replace('/', '.');
            try {
                if (resubmissionEnforcer.isEnforced(name, classLoader, module, classBeingRedefined)) {
                    return NO_TRANSFORMATION;
                }
            } catch (Throwable throwable) {
                try {
                    listener.onDiscovery(name, classLoader, module, classBeingRedefined != null);
                } finally {
                    listener.onError(name, classLoader, module, classBeingRedefined != null, throwable);
                }
                throw new IllegalStateException("Failed transformation of " + name, throwable);
            }
            try {
                listener.onDiscovery(name, classLoader, module, classBeingRedefined != null);
                ClassFileLocator classFileLocator = new ClassFileLocator.Compound(classFileBufferStrategy.resolve(name,
                        binaryRepresentation,
                        classLoader,
                        module,
                        protectionDomain), locationStrategy.classFileLocator(classLoader, module));
                TypePool typePool = classFileBufferStrategy.typePool(poolStrategy, classFileLocator, classLoader, name);
                try {
                    return doTransform(module, classLoader, name, classBeingRedefined, classBeingRedefined != null, protectionDomain, typePool, classFileLocator);
                } catch (Throwable throwable) {
                    if (classBeingRedefined != null && descriptionStrategy.isLoadedFirst() && fallbackStrategy.isFallback(classBeingRedefined, throwable)) {
                        return doTransform(module, classLoader, name, NOT_PREVIOUSLY_DEFINED, Listener.LOADED, protectionDomain, typePool, classFileLocator);
                    } else {
                        throw throwable;
                    }
                }
            } catch (Throwable throwable) {
                listener.onError(name, classLoader, module, classBeingRedefined != null, throwable);
                throw new IllegalStateException("Failed transformation of " + name, throwable);
            } finally {
                listener.onComplete(name, classLoader, module, classBeingRedefined != null);
            }
        }

        /**
         * Applies a transformation for a class that was captured by this {@link ClassFileTransformer}.
         *
         * @param module              The instrumented class's Java module in its wrapped form or {@code null} if the current VM does not support modules.
         * @param classLoader         The instrumented class's class loader.
         * @param name                The binary name of the instrumented class.
         * @param classBeingRedefined The loaded {@link Class} being redefined or {@code null} if no such class exists.
         * @param loaded              {@code true} if the instrumented type is loaded.
         * @param protectionDomain    The instrumented type's protection domain.
         * @param typePool            The type pool to use.
         * @param classFileLocator    The class file locator to use.
         * @return The transformed class file or an empty byte array if this transformer does not apply an instrumentation.
         */
        @MaybeNull
        private byte[] doTransform(@MaybeNull JavaModule module,
                                   @MaybeNull ClassLoader classLoader,
                                   String name,
                                   @MaybeNull Class<?> classBeingRedefined,
                                   boolean loaded,
                                   ProtectionDomain protectionDomain,
                                   TypePool typePool,
                                   ClassFileLocator classFileLocator) {
            TypeDescription typeDescription = descriptionStrategy.apply(name, classBeingRedefined, typePool, circularityLock, classLoader, module);
            List<Transformer> transformers = new ArrayList<Transformer>();
            if (!ignoreMatcher.matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain)) {
                for (CustomAgentBuilder.Transformation transformation : transformations) {
                    if (transformation.getMatcher().matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain)) {
                        transformers.addAll(transformation.getTransformers());
                        if (transformation.isTerminal()) {
                            break;
                        }
                    }
                }
            }
            if (transformers.isEmpty()) {
                listener.onIgnored(typeDescription, classLoader, module, loaded);
                return CustomAgentBuilder.Transformation.NONE;
            }
            DynamicType.Builder<?> builder = typeStrategy.builder(typeDescription,
                    byteBuddy,
                    classFileLocator,
                    nativeMethodStrategy.resolve(),
                    classLoader,
                    module,
                    protectionDomain);
            InitializationStrategy.Dispatcher dispatcher = initializationStrategy.dispatcher();
            for (Transformer transformer : transformers) {
                builder = transformer.transform(builder, typeDescription, classLoader, module);
            }
            DynamicType.Unloaded<?> dynamicType = dispatcher.apply(builder).make(TypeResolutionStrategy.Disabled.INSTANCE, typePool);
            dispatcher.register(dynamicType, classLoader, protectionDomain, injectionStrategy);
            listener.onTransformation(typeDescription, classLoader, module, loaded, dynamicType);
            return dynamicType.getBytes();
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<Transformer> iterator(TypeDescription typeDescription,
                                              @MaybeNull ClassLoader classLoader,
                                              @MaybeNull JavaModule module,
                                              @MaybeNull Class<?> classBeingRedefined,
                                              ProtectionDomain protectionDomain) {
            return ignoreMatcher.matches(typeDescription, classLoader, module, classBeingRedefined, protectionDomain)
                    ? Collections.<Transformer>emptySet().iterator()
                    : new CustomAgentBuilder.Transformation.TransformerIterator(typeDescription, classLoader, module, classBeingRedefined, protectionDomain, transformations);
        }

        /**
         * {@inheritDoc}
         */
        public synchronized boolean reset(Instrumentation instrumentation,
                                          ResettableClassFileTransformer classFileTransformer,
                                          RedefinitionStrategy redefinitionStrategy,
                                          RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy,
                                          RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator,
                                          RedefinitionStrategy.Listener redefinitionListener) {
            if (instrumentation.removeTransformer(classFileTransformer)) {
                redefinitionStrategy.apply(instrumentation,
                        poolStrategy,
                        locationStrategy,
                        descriptionStrategy,
                        fallbackStrategy,
                        redefinitionDiscoveryStrategy,
                        lambdaInstrumentationStrategy,
                        Listener.NoOp.INSTANCE,
                        redefinitionListener,
                        new CustomAgentBuilder.Transformation.SimpleMatcher(ignoreMatcher, transformations),
                        redefinitionBatchAllocator,
                        CircularityLock.Inactive.INSTANCE);
                installationListener.onReset(instrumentation, classFileTransformer);
                return true;
            } else {
                return false;
            }
        }

        /* does not implement hashCode and equals in order to align with identity treatment of the JVM */

        /**
         * A factory for creating a {@link ClassFileTransformer} for the current VM.
         */
        protected interface Factory {

            /**
             * Creates a new class file transformer for the current VM.
             *
             * @param byteBuddy                     The Byte Buddy instance to be used.
             * @param listener                      The listener to notify on transformations.
             * @param poolStrategy                  The pool strategy to use.
             * @param typeStrategy                  The definition handler to use.
             * @param locationStrategy              The location strategy to use.
             * @param nativeMethodStrategy          The native method strategy to apply.
             * @param initializationStrategy        The initialization strategy to use for transformed types.
             * @param injectionStrategy             The injection strategy to use.
             * @param lambdaInstrumentationStrategy The lambda instrumentation strategy to use.
             * @param descriptionStrategy           The description strategy for resolving type descriptions for types.
             * @param fallbackStrategy              The fallback strategy to use.
             * @param classFileBufferStrategy       The class file buffer strategy to use.
             * @param installationListener          The installation listener to notify.
             * @param ignoreMatcher                 Identifies types that should not be instrumented.
             * @param resubmissionEnforcer          The resubmission enforcer to use.
             * @param transformations               The transformations to apply on non-ignored types.
             * @param circularityLock               The circularity lock to use.
             * @return A class file transformer for the current VM that supports the API of the current VM.
             */
            ResettableClassFileTransformer make(ByteBuddy byteBuddy,
                                                Listener listener,
                                                PoolStrategy poolStrategy,
                                                TypeStrategy typeStrategy,
                                                LocationStrategy locationStrategy,
                                                CustomAgentBuilder.NativeMethodStrategy nativeMethodStrategy,
                                                InitializationStrategy initializationStrategy,
                                                InjectionStrategy injectionStrategy,
                                                LambdaInstrumentationStrategy lambdaInstrumentationStrategy,
                                                DescriptionStrategy descriptionStrategy,
                                                FallbackStrategy fallbackStrategy,
                                                ClassFileBufferStrategy classFileBufferStrategy,
                                                InstallationListener installationListener,
                                                RawMatcher ignoreMatcher,
                                                RedefinitionStrategy.ResubmissionEnforcer resubmissionEnforcer,
                                                List<CustomAgentBuilder.Transformation> transformations,
                                                CircularityLock circularityLock);

            /**
             * An action to create an implementation of {@link CustomAgentBuilder.ExecutingTransformer} that support Java 9 modules.
             */
            enum CreationAction implements PrivilegedAction<CustomAgentBuilder.ExecutingTransformer.Factory> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
//                @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback.")
                public Factory run() {
                    try {
                        return new Factory.ForJava9CapableVm(new ByteBuddy()
                                .with(TypeValidation.DISABLED)
                                .subclass(CustomAgentBuilder.ExecutingTransformer.class)
                                .name(CustomAgentBuilder.ExecutingTransformer.class.getName() + "$ByteBuddy$ModuleSupport")
                                .method(named("transform").and(takesArgument(0, JavaType.MODULE.load())))
                                .intercept(MethodCall.invoke(CustomAgentBuilder.ExecutingTransformer.class.getDeclaredMethod("transform",
                                        Object.class,
                                        ClassLoader.class,
                                        String.class,
                                        Class.class,
                                        ProtectionDomain.class,
                                        byte[].class)).onSuper().withAllArguments())
                                .make()
                                .load(CustomAgentBuilder.ExecutingTransformer.class.getClassLoader(),
                                        ClassLoadingStrategy.Default.WRAPPER_PERSISTENT.with(CustomAgentBuilder.ExecutingTransformer.class.getProtectionDomain()))
                                .getLoaded()
                                .getDeclaredConstructor(ByteBuddy.class,
                                        Listener.class,
                                        PoolStrategy.class,
                                        TypeStrategy.class,
                                        LocationStrategy.class,
                                        CustomAgentBuilder.NativeMethodStrategy.class,
                                        InitializationStrategy.class,
                                        InjectionStrategy.class,
                                        LambdaInstrumentationStrategy.class,
                                        DescriptionStrategy.class,
                                        FallbackStrategy.class,
                                        ClassFileBufferStrategy.class,
                                        InstallationListener.class,
                                        RawMatcher.class,
                                        RedefinitionStrategy.ResubmissionEnforcer.class,
                                        List.class,
                                        CircularityLock.class));
                    } catch (Exception ignored) {
                        return CustomAgentBuilder.ExecutingTransformer.Factory.ForLegacyVm.INSTANCE;
                    }
                }
            }

            /**
             * A factory for a class file transformer on a JVM that supports the {@code java.lang.Module} API to override
             * the newly added method of the {@link ClassFileTransformer} to capture an instrumented class's module.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForJava9CapableVm implements CustomAgentBuilder.ExecutingTransformer.Factory {

                /**
                 * A constructor for creating a {@link ClassFileTransformer} that overrides the newly added method for extracting
                 * the {@code java.lang.Module} of an instrumented class.
                 */
                private final Constructor<? extends ResettableClassFileTransformer> executingTransformer;

                /**
                 * Creates a class file transformer factory for a Java 9 capable VM.
                 *
                 * @param executingTransformer A constructor for creating a {@link ClassFileTransformer} that overrides the newly added
                 *                             method for extracting the {@code java.lang.Module} of an instrumented class.
                 */
                protected ForJava9CapableVm(Constructor<? extends ResettableClassFileTransformer> executingTransformer) {
                    this.executingTransformer = executingTransformer;
                }

                /**
                 * {@inheritDoc}
                 */
                public ResettableClassFileTransformer make(ByteBuddy byteBuddy,
                                                           Listener listener,
                                                           PoolStrategy poolStrategy,
                                                           TypeStrategy typeStrategy,
                                                           LocationStrategy locationStrategy,
                                                           CustomAgentBuilder.NativeMethodStrategy nativeMethodStrategy,
                                                           InitializationStrategy initializationStrategy,
                                                           InjectionStrategy injectionStrategy,
                                                           LambdaInstrumentationStrategy lambdaInstrumentationStrategy,
                                                           DescriptionStrategy descriptionStrategy,
                                                           FallbackStrategy fallbackStrategy,
                                                           ClassFileBufferStrategy classFileBufferStrategy,
                                                           InstallationListener installationListener,
                                                           RawMatcher ignoreMatcher,
                                                           RedefinitionStrategy.ResubmissionEnforcer resubmissionEnforcer,
                                                           List<CustomAgentBuilder.Transformation> transformations,
                                                           CircularityLock circularityLock) {
                    try {
                        return executingTransformer.newInstance(byteBuddy,
                                listener,
                                poolStrategy,
                                typeStrategy,
                                locationStrategy,
                                nativeMethodStrategy,
                                initializationStrategy,
                                injectionStrategy,
                                lambdaInstrumentationStrategy,
                                descriptionStrategy,
                                fallbackStrategy,
                                classFileBufferStrategy,
                                installationListener,
                                ignoreMatcher,
                                resubmissionEnforcer,
                                transformations,
                                circularityLock);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access " + executingTransformer, exception);
                    } catch (InstantiationException exception) {
                        throw new IllegalStateException("Cannot instantiate " + executingTransformer.getDeclaringClass(), exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Cannot invoke " + executingTransformer, exception.getTargetException());
                    }
                }
            }

            /**
             * A factory for a {@link ClassFileTransformer} on a VM that does not support the {@code java.lang.Module} API.
             */
            enum ForLegacyVm implements CustomAgentBuilder.ExecutingTransformer.Factory {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public ResettableClassFileTransformer make(ByteBuddy byteBuddy,
                                                           Listener listener,
                                                           PoolStrategy poolStrategy,
                                                           TypeStrategy typeStrategy,
                                                           LocationStrategy locationStrategy,
                                                           CustomAgentBuilder.NativeMethodStrategy nativeMethodStrategy,
                                                           InitializationStrategy initializationStrategy,
                                                           InjectionStrategy injectionStrategy,
                                                           LambdaInstrumentationStrategy lambdaInstrumentationStrategy,
                                                           DescriptionStrategy descriptionStrategy,
                                                           FallbackStrategy fallbackStrategy,
                                                           ClassFileBufferStrategy classFileBufferStrategy,
                                                           InstallationListener installationListener,
                                                           RawMatcher ignoreMatcher,
                                                           RedefinitionStrategy.ResubmissionEnforcer resubmissionEnforcer,
                                                           List<CustomAgentBuilder.Transformation> transformations,
                                                           CircularityLock circularityLock) {
                    return new CustomAgentBuilder.ExecutingTransformer(byteBuddy,
                            listener,
                            poolStrategy,
                            typeStrategy,
                            locationStrategy,
                            nativeMethodStrategy,
                            initializationStrategy,
                            injectionStrategy,
                            lambdaInstrumentationStrategy,
                            descriptionStrategy,
                            fallbackStrategy,
                            classFileBufferStrategy,
                            installationListener,
                            ignoreMatcher,
                            resubmissionEnforcer,
                            transformations,
                            circularityLock);
                }
            }
        }

        /**
         * A privileged action for transforming a class on a JVM prior to Java 9.
         */
        @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
        protected class LegacyVmDispatcher implements PrivilegedAction<byte[]> {

            /**
             * The type's class loader or {@code null} if the bootstrap class loader is represented.
             */
            @MaybeNull
            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
            private final ClassLoader classLoader;

            /**
             * The type's internal name or {@code null} if no such name exists.
             */
            @MaybeNull
            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
            private final String internalTypeName;

            /**
             * The class being redefined or {@code null} if no such class exists.
             */
            @MaybeNull
            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
            private final Class<?> classBeingRedefined;

            /**
             * The type's protection domain.
             */
            private final ProtectionDomain protectionDomain;

            /**
             * The type's binary representation.
             */
            private final byte[] binaryRepresentation;

            /**
             * Creates a new type transformation dispatcher.
             *
             * @param classLoader          The type's class loader or {@code null} if the bootstrap class loader is represented.
             * @param internalTypeName     The type's internal name or {@code null} if no such name exists.
             * @param classBeingRedefined  The class being redefined or {@code null} if no such class exists.
             * @param protectionDomain     The type's protection domain.
             * @param binaryRepresentation The type's binary representation.
             */
            protected LegacyVmDispatcher(@MaybeNull ClassLoader classLoader,
                                         @MaybeNull String internalTypeName,
                                         @MaybeNull Class<?> classBeingRedefined,
                                         ProtectionDomain protectionDomain,
                                         byte[] binaryRepresentation) {
                this.classLoader = classLoader;
                this.internalTypeName = internalTypeName;
                this.classBeingRedefined = classBeingRedefined;
                this.protectionDomain = protectionDomain;
                this.binaryRepresentation = binaryRepresentation;
            }

            /**
             * {@inheritDoc}
             */
            @MaybeNull
            public byte[] run() {
                return transform(JavaModule.UNSUPPORTED,
                        classLoader,
                        internalTypeName,
                        classBeingRedefined,
                        protectionDomain,
                        binaryRepresentation);
            }
        }

        /**
         * A privileged action for transforming a class on a JVM that supports modules.
         */
        @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
        protected class Java9CapableVmDispatcher implements PrivilegedAction<byte[]> {

            /**
             * The type's {@code java.lang.Module}.
             */
            private final Object rawModule;

            /**
             * The type's class loader or {@code null} if the type is loaded by the bootstrap loader.
             */
            @MaybeNull
            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
            private final ClassLoader classLoader;

            /**
             * The type's internal name or {@code null} if no such name exists.
             */
            @MaybeNull
            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
            private final String internalTypeName;

            /**
             * The class being redefined or {@code null} if no such class exists.
             */
            @MaybeNull
            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
            private final Class<?> classBeingRedefined;

            /**
             * The type's protection domain.
             */
            private final ProtectionDomain protectionDomain;

            /**
             * The type's binary representation.
             */
            private final byte[] binaryRepresentation;

            /**
             * Creates a new legacy dispatcher.
             *
             * @param rawModule            The type's {@code java.lang.Module}.
             * @param classLoader          The type's class loader or {@code null} if the type is loaded by the bootstrap loader.
             * @param internalTypeName     The type's internal name or {@code null} if no such name exists.
             * @param classBeingRedefined  The class being redefined or {@code null} if no such class exists.
             * @param protectionDomain     The type's protection domain.
             * @param binaryRepresentation The type's binary representation.
             */
            protected Java9CapableVmDispatcher(Object rawModule,
                                               @MaybeNull ClassLoader classLoader,
                                               @MaybeNull String internalTypeName,
                                               @MaybeNull Class<?> classBeingRedefined,
                                               ProtectionDomain protectionDomain,
                                               byte[] binaryRepresentation) {
                this.rawModule = rawModule;
                this.classLoader = classLoader;
                this.internalTypeName = internalTypeName;
                this.classBeingRedefined = classBeingRedefined;
                this.protectionDomain = protectionDomain;
                this.binaryRepresentation = binaryRepresentation;
            }

            /**
             * {@inheritDoc}
             */
            @MaybeNull
            public byte[] run() {
                return transform(JavaModule.of(rawModule),
                        classLoader,
                        internalTypeName,
                        classBeingRedefined,
                        protectionDomain,
                        binaryRepresentation);
            }
        }
    }

    /**
     * An abstract implementation of an agent builder that delegates all invocation to another instance.
     */
    protected abstract static class Delegator implements AgentBuilder {

        /**
         * Materializes the currently described {@link net.bytebuddy.agent.builder.AgentBuilder}.
         *
         * @return An agent builder that represents the currently described entry of this instance.
         */
        protected abstract AgentBuilder materialize();

        /**
         * {@inheritDoc}
         */
        public AgentBuilder with(ByteBuddy byteBuddy) {
            return materialize().with(byteBuddy);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder with(Listener listener) {
            return materialize().with(listener);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder with(CircularityLock circularityLock) {
            return materialize().with(circularityLock);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder with(TypeStrategy typeStrategy) {
            return materialize().with(typeStrategy);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder with(PoolStrategy poolStrategy) {
            return materialize().with(poolStrategy);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder with(LocationStrategy locationStrategy) {
            return materialize().with(locationStrategy);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder with(InitializationStrategy initializationStrategy) {
            return materialize().with(initializationStrategy);
        }

        /**
         * {@inheritDoc}
         */
        public RedefinitionListenable.WithoutBatchStrategy with(RedefinitionStrategy redefinitionStrategy) {
            return materialize().with(redefinitionStrategy);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder with(LambdaInstrumentationStrategy lambdaInstrumentationStrategy) {
            return materialize().with(lambdaInstrumentationStrategy);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder with(DescriptionStrategy descriptionStrategy) {
            return materialize().with(descriptionStrategy);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder with(FallbackStrategy fallbackStrategy) {
            return materialize().with(fallbackStrategy);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder with(ClassFileBufferStrategy classFileBufferStrategy) {
            return materialize().with(classFileBufferStrategy);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder with(InstallationListener installationListener) {
            return materialize().with(installationListener);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder with(InjectionStrategy injectionStrategy) {
            return materialize().with(injectionStrategy);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder with(TransformerDecorator transformerDecorator) {
            return materialize().with(transformerDecorator);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder enableNativeMethodPrefix(String prefix) {
            return materialize().enableNativeMethodPrefix(prefix);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder disableNativeMethodPrefix() {
            return materialize().disableNativeMethodPrefix();
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder disableClassFormatChanges() {
            return materialize().disableClassFormatChanges();
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder warmUp(Class<?>... type) {
            return materialize().warmUp(type);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder warmUp(Collection<Class<?>> types) {
            return materialize().warmUp(types);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder assureReadEdgeTo(Instrumentation instrumentation, Class<?>... type) {
            return materialize().assureReadEdgeTo(instrumentation, type);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder assureReadEdgeTo(Instrumentation instrumentation, JavaModule... module) {
            return materialize().assureReadEdgeTo(instrumentation, module);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder assureReadEdgeTo(Instrumentation instrumentation, Collection<? extends JavaModule> modules) {
            return materialize().assureReadEdgeTo(instrumentation, modules);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder assureReadEdgeFromAndTo(Instrumentation instrumentation, Class<?>... type) {
            return materialize().assureReadEdgeFromAndTo(instrumentation, type);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder assureReadEdgeFromAndTo(Instrumentation instrumentation, JavaModule... module) {
            return materialize().assureReadEdgeFromAndTo(instrumentation, module);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder assureReadEdgeFromAndTo(Instrumentation instrumentation, Collection<? extends JavaModule> modules) {
            return materialize().assureReadEdgeFromAndTo(instrumentation, modules);
        }

        /**
         * {@inheritDoc}
         */
        public Identified.Narrowable type(ElementMatcher<? super TypeDescription> typeMatcher) {
            return materialize().type(typeMatcher);
        }

        /**
         * {@inheritDoc}
         */
        public Identified.Narrowable type(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher) {
            return materialize().type(typeMatcher, classLoaderMatcher);
        }

        /**
         * {@inheritDoc}
         */
        public Identified.Narrowable type(ElementMatcher<? super TypeDescription> typeMatcher,
                                          ElementMatcher<? super ClassLoader> classLoaderMatcher,
                                          ElementMatcher<? super JavaModule> moduleMatcher) {
            return materialize().type(typeMatcher, classLoaderMatcher, moduleMatcher);
        }

        /**
         * {@inheritDoc}
         */
        public Identified.Narrowable type(RawMatcher matcher) {
            return materialize().type(matcher);
        }

        /**
         * {@inheritDoc}
         */
        public Ignored ignore(ElementMatcher<? super TypeDescription> ignoredTypes) {
            return materialize().ignore(ignoredTypes);
        }

        /**
         * {@inheritDoc}
         */
        public Ignored ignore(ElementMatcher<? super TypeDescription> ignoredTypes, ElementMatcher<? super ClassLoader> ignoredClassLoaders) {
            return materialize().ignore(ignoredTypes, ignoredClassLoaders);
        }

        /**
         * {@inheritDoc}
         */
        public Ignored ignore(ElementMatcher<? super TypeDescription> typeMatcher,
                              ElementMatcher<? super ClassLoader> classLoaderMatcher,
                              ElementMatcher<? super JavaModule> moduleMatcher) {
            return materialize().ignore(typeMatcher, classLoaderMatcher, moduleMatcher);
        }

        /**
         * {@inheritDoc}
         */
        public Ignored ignore(RawMatcher rawMatcher) {
            return materialize().ignore(rawMatcher);
        }

        /**
         * {@inheritDoc}
         */
        public ClassFileTransformer makeRaw() {
            return materialize().makeRaw();
        }

        /**
         * {@inheritDoc}
         */
        public ResettableClassFileTransformer installOn(Instrumentation instrumentation) {
            return materialize().installOn(instrumentation);
        }

        /**
         * {@inheritDoc}
         */
        public ResettableClassFileTransformer installOnByteBuddyAgent() {
            return materialize().installOnByteBuddyAgent();
        }

        /**
         * {@inheritDoc}
         */
        public ResettableClassFileTransformer patchOn(Instrumentation instrumentation, ResettableClassFileTransformer classFileTransformer) {
            return materialize().patchOn(instrumentation, classFileTransformer);
        }

        /**
         * {@inheritDoc}
         */
        public ResettableClassFileTransformer patchOnByteBuddyAgent(ResettableClassFileTransformer classFileTransformer) {
            return materialize().patchOnByteBuddyAgent(classFileTransformer);
        }

        /**
         * An abstract base implementation of a matchable.
         *
         * @param <S> The type that is produced by chaining a matcher.
         */
        protected abstract static class Matchable<S extends AgentBuilder.Matchable<S>> extends Default.Delegator implements AgentBuilder.Matchable<S> {

            /**
             * {@inheritDoc}
             */
            public S and(ElementMatcher<? super TypeDescription> typeMatcher) {
                return and(typeMatcher, any());
            }

            /**
             * {@inheritDoc}
             */
            public S and(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher) {
                return and(typeMatcher, classLoaderMatcher, any());
            }

            /**
             * {@inheritDoc}
             */
            public S and(ElementMatcher<? super TypeDescription> typeMatcher,
                         ElementMatcher<? super ClassLoader> classLoaderMatcher,
                         ElementMatcher<? super JavaModule> moduleMatcher) {
                return and(new RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, moduleMatcher));
            }

            /**
             * {@inheritDoc}
             */
            public S or(ElementMatcher<? super TypeDescription> typeMatcher) {
                return or(typeMatcher, any());
            }

            /**
             * {@inheritDoc}
             */
            public S or(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher) {
                return or(typeMatcher, classLoaderMatcher, any());
            }

            /**
             * {@inheritDoc}
             */
            public S or(ElementMatcher<? super TypeDescription> typeMatcher,
                        ElementMatcher<? super ClassLoader> classLoaderMatcher,
                        ElementMatcher<? super JavaModule> moduleMatcher) {
                return or(new RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, moduleMatcher));
            }
        }
    }

    /**
     * A delegator transformer for further precising what types to ignore.
     */
    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
    protected class Ignoring extends Default.Delegator.Matchable<Ignored> implements Ignored {

        /**
         * A matcher for identifying types that should not be instrumented.
         */
        private final RawMatcher rawMatcher;

        /**
         * Creates a new agent builder for further specifying what types to ignore.
         *
         * @param rawMatcher A matcher for identifying types that should not be instrumented.
         */
        protected Ignoring(RawMatcher rawMatcher) {
            this.rawMatcher = rawMatcher;
        }

        @Override
        protected AgentBuilder materialize() {
            return new CustomAgentBuilder(byteBuddy,
                    listener,
                    circularityLock,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    warmupStrategy,
                    transformerDecorator,
                    initializationStrategy,
                    redefinitionStrategy,
                    redefinitionDiscoveryStrategy,
                    redefinitionBatchAllocator,
                    redefinitionListener,
                    redefinitionResubmissionStrategy,
                    injectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    fallbackStrategy,
                    classFileBufferStrategy,
                    installationListener,
                    rawMatcher,
                    transformations, module);
        }

        /**
         * {@inheritDoc}
         */
        public Ignored and(RawMatcher rawMatcher) {
            return new CustomAgentBuilder.Ignoring(new RawMatcher.Conjunction(this.rawMatcher, rawMatcher));
        }

        /**
         * {@inheritDoc}
         */
        public Ignored or(RawMatcher rawMatcher) {
            return new CustomAgentBuilder.Ignoring(new RawMatcher.Disjunction(this.rawMatcher, rawMatcher));
        }
    }

    /**
     * A helper class that describes a {@link net.bytebuddy.agent.builder.AgentBuilder.Default} after supplying
     * a {@link net.bytebuddy.agent.builder.AgentBuilder.RawMatcher} such that one or several
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s can be supplied.
     */
    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
    protected class Transforming extends Default.Delegator.Matchable<Identified.Narrowable> implements Identified.Extendable, Identified.Narrowable {

        /**
         * The supplied raw matcher.
         */
        private final RawMatcher rawMatcher;

        /**
         * The supplied transformer.
         */
        private final List<Transformer> transformers;

        /**
         * {@code true} if this transformer is a terminal transformation.
         */
        private final boolean terminal;

        /**
         * Creates a new matched default agent builder.
         *
         * @param rawMatcher   The supplied raw matcher.
         * @param transformers The transformers to apply.
         * @param terminal     {@code true} if this transformer is a terminal transformation.
         */
        protected Transforming(RawMatcher rawMatcher, List<Transformer> transformers, boolean terminal) {
            this.rawMatcher = rawMatcher;
            this.transformers = transformers;
            this.terminal = terminal;
        }

        @Override
        protected AgentBuilder materialize() {
            return new CustomAgentBuilder(byteBuddy,
                    listener,
                    circularityLock,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    warmupStrategy,
                    transformerDecorator,
                    initializationStrategy,
                    redefinitionStrategy,
                    redefinitionDiscoveryStrategy,
                    redefinitionBatchAllocator,
                    redefinitionListener,
                    redefinitionResubmissionStrategy,
                    injectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    fallbackStrategy,
                    classFileBufferStrategy,
                    installationListener,
                    ignoreMatcher,
                    CompoundList.of(transformations, new CustomAgentBuilder.Transformation(rawMatcher, transformers, terminal)), module);
        }

        /**
         * {@inheritDoc}
         */
        public Identified.Extendable transform(Transformer transformer) {
            return new CustomAgentBuilder.Transforming(rawMatcher, CompoundList.of(this.transformers, transformer), terminal);
        }

        /**
         * {@inheritDoc}
         */
        public AgentBuilder asTerminalTransformation() {
            return new CustomAgentBuilder.Transforming(rawMatcher, transformers, true);
        }

        /**
         * {@inheritDoc}
         */
        public Narrowable and(RawMatcher rawMatcher) {
            return new CustomAgentBuilder.Transforming(new RawMatcher.Conjunction(this.rawMatcher, rawMatcher), transformers, terminal);
        }

        /**
         * {@inheritDoc}
         */
        public Narrowable or(RawMatcher rawMatcher) {
            return new CustomAgentBuilder.Transforming(new RawMatcher.Disjunction(this.rawMatcher, rawMatcher), transformers, terminal);
        }
    }

    /**
     * An implementation of a default agent builder that allows for refinement of the redefinition strategy.
     */
    protected static class Redefining extends CustomAgentBuilder implements RedefinitionListenable.WithoutBatchStrategy {

        /**
         * Creates a new default agent builder that allows for refinement of the redefinition strategy.
         *
         * @param byteBuddy                        The Byte Buddy instance to be used.
         * @param listener                         The listener to notify on transformations.
         * @param circularityLock                  The circularity lock to use.
         * @param poolStrategy                     The pool strategy to use.
         * @param typeStrategy                     The definition handler to use.
         * @param locationStrategy                 The location strategy to use.
         * @param nativeMethodStrategy             The native method strategy to apply.
         * @param warmupStrategy                   The warmup strategy to use.
         * @param transformerDecorator             A decorator to wrap the created class file transformer.
         * @param initializationStrategy           The initialization strategy to use for transformed types.
         * @param redefinitionStrategy             The redefinition strategy to apply.
         * @param redefinitionDiscoveryStrategy    The discovery strategy for loaded types to be redefined.
         * @param redefinitionBatchAllocator       The batch allocator for the redefinition strategy to apply.
         * @param redefinitionListener             The redefinition listener for the redefinition strategy to apply.
         * @param redefinitionResubmissionStrategy The resubmission strategy to apply.
         * @param injectionStrategy                The injection strategy to use.
         * @param lambdaInstrumentationStrategy    A strategy to determine of the {@code LambdaMetafactory} should be instrumented to allow for the
         *                                         instrumentation of classes that represent lambda expressions.
         * @param descriptionStrategy              The description strategy for resolving type descriptions for types.
         * @param fallbackStrategy                 The fallback strategy to apply.
         * @param classFileBufferStrategy          The class file buffer strategy to use.
         * @param installationListener             The installation listener to notify.
         * @param ignoreMatcher                    Identifies types that should not be instrumented.
         * @param transformations                  The transformations to apply on non-ignored types.
         */
        protected Redefining(ByteBuddy byteBuddy,
                             Listener listener,
                             CircularityLock circularityLock,
                             PoolStrategy poolStrategy,
                             TypeStrategy typeStrategy,
                             LocationStrategy locationStrategy,
                             NativeMethodStrategy nativeMethodStrategy,
                             WarmupStrategy warmupStrategy,
                             TransformerDecorator transformerDecorator,
                             InitializationStrategy initializationStrategy,
                             RedefinitionStrategy redefinitionStrategy,
                             RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy,
                             RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator,
                             RedefinitionStrategy.Listener redefinitionListener,
                             RedefinitionStrategy.ResubmissionStrategy redefinitionResubmissionStrategy,
                             InjectionStrategy injectionStrategy,
                             LambdaInstrumentationStrategy lambdaInstrumentationStrategy,
                             DescriptionStrategy descriptionStrategy,
                             FallbackStrategy fallbackStrategy,
                             ClassFileBufferStrategy classFileBufferStrategy,
                             InstallationListener installationListener,
                             RawMatcher ignoreMatcher,
                             List<Transformation> transformations, String module) {
            super(byteBuddy,
                    listener,
                    circularityLock,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    warmupStrategy,
                    transformerDecorator,
                    initializationStrategy,
                    redefinitionStrategy,
                    redefinitionDiscoveryStrategy,
                    redefinitionBatchAllocator,
                    redefinitionListener,
                    redefinitionResubmissionStrategy,
                    injectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    fallbackStrategy,
                    classFileBufferStrategy,
                    installationListener,
                    ignoreMatcher,
                    transformations, module);
        }

        /**
         * {@inheritDoc}
         */
        public WithImplicitDiscoveryStrategy with(RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator) {
            if (!redefinitionStrategy.isEnabled()) {
                throw new IllegalStateException("Cannot set redefinition batch allocator when redefinition is disabled");
            }
            return new Redefining(byteBuddy,
                    listener,
                    circularityLock,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    warmupStrategy,
                    transformerDecorator,
                    initializationStrategy,
                    redefinitionStrategy,
                    redefinitionDiscoveryStrategy,
                    redefinitionBatchAllocator,
                    redefinitionListener,
                    redefinitionResubmissionStrategy,
                    injectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    fallbackStrategy,
                    classFileBufferStrategy,
                    installationListener,
                    ignoreMatcher,
                    transformations, module);
        }

        /**
         * {@inheritDoc}
         */
        public RedefinitionListenable redefineOnly(Class<?>... type) {
            return with(new RedefinitionStrategy.DiscoveryStrategy.Explicit(type));
        }

        /**
         * {@inheritDoc}
         */
        public RedefinitionListenable with(RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy) {
            if (!redefinitionStrategy.isEnabled()) {
                throw new IllegalStateException("Cannot set redefinition discovery strategy when redefinition is disabled");
            }
            return new Redefining(byteBuddy,
                    listener,
                    circularityLock,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    warmupStrategy,
                    transformerDecorator,
                    initializationStrategy,
                    redefinitionStrategy,
                    redefinitionDiscoveryStrategy,
                    redefinitionBatchAllocator,
                    redefinitionListener,
                    redefinitionResubmissionStrategy,
                    injectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    fallbackStrategy,
                    classFileBufferStrategy,
                    installationListener,
                    ignoreMatcher,
                    transformations, module);
        }

        /**
         * {@inheritDoc}
         */
        public RedefinitionListenable with(RedefinitionStrategy.Listener redefinitionListener) {
            if (!redefinitionStrategy.isEnabled()) {
                throw new IllegalStateException("Cannot set redefinition listener when redefinition is disabled");
            }
            return new Redefining(byteBuddy,
                    listener,
                    circularityLock,
                    poolStrategy,
                    typeStrategy,
                    locationStrategy,
                    nativeMethodStrategy,
                    warmupStrategy,
                    transformerDecorator,
                    initializationStrategy,
                    redefinitionStrategy,
                    redefinitionDiscoveryStrategy,
                    redefinitionBatchAllocator,
                    new RedefinitionStrategy.Listener.Compound(this.redefinitionListener, redefinitionListener),
                    redefinitionResubmissionStrategy,
                    injectionStrategy,
                    lambdaInstrumentationStrategy,
                    descriptionStrategy,
                    fallbackStrategy,
                    classFileBufferStrategy,
                    installationListener,
                    ignoreMatcher,
                    transformations, module);
        }

        /**
         * {@inheritDoc}
         */
        public WithoutResubmissionSpecification withResubmission(RedefinitionStrategy.ResubmissionScheduler resubmissionScheduler) {
            if (!redefinitionStrategy.isEnabled()) {
                throw new IllegalStateException("Cannot enable resubmission when redefinition is disabled");
            }
            return new Redefining.WithResubmission(resubmissionScheduler,
                    ResubmissionOnErrorMatcher.Trivial.NON_MATCHING,
                    ResubmissionImmediateMatcher.Trivial.NON_MATCHING);
        }

        /**
         * A delegator that applies a resubmission.
         */
        protected class WithResubmission extends Delegator implements WithResubmissionSpecification {

            /**
             * The resubmission scheduler to use.
             */
            private final RedefinitionStrategy.ResubmissionScheduler resubmissionScheduler;

            /**
             * A matcher to determine resubmissions on errors.
             */
            private final ResubmissionOnErrorMatcher resubmissionOnErrorMatcher;

            /**
             * A matcher to determine resubmissions without errors.
             */
            private final ResubmissionImmediateMatcher resubmissionImmediateMatcher;

            /**
             * Creates a new delegator that applies resubmissions.
             *
             * @param resubmissionScheduler        The resubmission scheduler to use.
             * @param resubmissionOnErrorMatcher   A matcher to determine resubmissions on errors.
             * @param resubmissionImmediateMatcher A matcher to determine resubmissions without errors.
             */
            protected WithResubmission(RedefinitionStrategy.ResubmissionScheduler resubmissionScheduler,
                                       ResubmissionOnErrorMatcher resubmissionOnErrorMatcher,
                                       ResubmissionImmediateMatcher resubmissionImmediateMatcher) {
                this.resubmissionScheduler = resubmissionScheduler;
                this.resubmissionOnErrorMatcher = resubmissionOnErrorMatcher;
                this.resubmissionImmediateMatcher = resubmissionImmediateMatcher;
            }

            @Override
            protected AgentBuilder materialize() {
                return new CustomAgentBuilder(byteBuddy,
                        listener,
                        circularityLock,
                        poolStrategy,
                        typeStrategy,
                        locationStrategy,
                        nativeMethodStrategy,
                        warmupStrategy,
                        transformerDecorator,
                        initializationStrategy,
                        redefinitionStrategy,
                        redefinitionDiscoveryStrategy,
                        redefinitionBatchAllocator,
                        redefinitionListener,
                        new RedefinitionStrategy.ResubmissionStrategy.Enabled(resubmissionScheduler, resubmissionOnErrorMatcher, resubmissionImmediateMatcher),
                        injectionStrategy,
                        lambdaInstrumentationStrategy,
                        descriptionStrategy,
                        fallbackStrategy,
                        classFileBufferStrategy,
                        installationListener,
                        ignoreMatcher,
                        transformations,
                        module);
            }

            /**
             * {@inheritDoc}
             */
            public WithResubmissionSpecification resubmitOnError() {
                return resubmitOnError(ElementMatchers.<Throwable>any());
            }

            /**
             * {@inheritDoc}
             */
            public WithResubmissionSpecification resubmitOnError(ElementMatcher<? super Throwable> exceptionMatcher) {
                return resubmitOnError(exceptionMatcher, ElementMatchers.<String>any());
            }

            /**
             * {@inheritDoc}
             */
            public WithResubmissionSpecification resubmitOnError(ElementMatcher<? super Throwable> exceptionMatcher,
                                                                 ElementMatcher<String> typeNameMatcher) {
                return resubmitOnError(exceptionMatcher, typeNameMatcher, ElementMatchers.<ClassLoader>any());
            }

            /**
             * {@inheritDoc}
             */
            public WithResubmissionSpecification resubmitOnError(ElementMatcher<? super Throwable> exceptionMatcher,
                                                                 ElementMatcher<String> typeNameMatcher,
                                                                 ElementMatcher<? super ClassLoader> classLoaderMatcher) {
                return resubmitOnError(exceptionMatcher, typeNameMatcher, classLoaderMatcher, ElementMatchers.<JavaModule>any());
            }

            /**
             * {@inheritDoc}
             */
            public WithResubmissionSpecification resubmitOnError(ElementMatcher<? super Throwable> exceptionMatcher,
                                                                 ElementMatcher<String> typeNameMatcher,
                                                                 ElementMatcher<? super ClassLoader> classLoaderMatcher,
                                                                 ElementMatcher<? super JavaModule> moduleMatcher) {
                return resubmitOnError(new ResubmissionOnErrorMatcher.ForElementMatchers(exceptionMatcher, typeNameMatcher, classLoaderMatcher, moduleMatcher));
            }

            /**
             * {@inheritDoc}
             */
            public WithResubmissionSpecification resubmitOnError(ResubmissionOnErrorMatcher matcher) {
                return new Redefining.WithResubmission(resubmissionScheduler,
                        new ResubmissionOnErrorMatcher.Disjunction(resubmissionOnErrorMatcher, matcher),
                        resubmissionImmediateMatcher);
            }

            /**
             * {@inheritDoc}
             */
            public WithResubmissionSpecification resubmitImmediate() {
                return resubmitImmediate(ElementMatchers.<String>any());
            }

            /**
             * {@inheritDoc}
             */
            public WithResubmissionSpecification resubmitImmediate(ElementMatcher<String> typeNameMatcher) {
                return resubmitImmediate(typeNameMatcher, ElementMatchers.<ClassLoader>any());
            }

            /**
             * {@inheritDoc}
             */
            public WithResubmissionSpecification resubmitImmediate(ElementMatcher<String> typeNameMatcher,
                                                                   ElementMatcher<? super ClassLoader> classLoaderMatcher) {
                return resubmitImmediate(typeNameMatcher, classLoaderMatcher, ElementMatchers.<JavaModule>any());
            }

            /**
             * {@inheritDoc}
             */
            public WithResubmissionSpecification resubmitImmediate(ElementMatcher<String> typeNameMatcher,
                                                                   ElementMatcher<? super ClassLoader> classLoaderMatcher,
                                                                   ElementMatcher<? super JavaModule> moduleMatcher) {
                return resubmitImmediate(new ResubmissionImmediateMatcher.ForElementMatchers(typeNameMatcher, classLoaderMatcher, moduleMatcher));
            }

            /**
             * {@inheritDoc}
             */
            public WithResubmissionSpecification resubmitImmediate(ResubmissionImmediateMatcher matcher) {
                return new Redefining.WithResubmission(resubmissionScheduler,
                        resubmissionOnErrorMatcher,
                        new ResubmissionImmediateMatcher.Disjunction(resubmissionImmediateMatcher, matcher));
            }
        }
    }
}
