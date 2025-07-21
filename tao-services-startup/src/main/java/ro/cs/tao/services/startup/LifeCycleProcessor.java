/*
 * Copyright (C) 2018 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package ro.cs.tao.services.startup;

import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ro.cs.tao.lifecycle.ComponentLifeCycle;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.utils.executors.NamedThreadPoolExecutor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * This class is responsible with the invocation of (possible) component activator invocations.
 *
 * @author Cosmin Cara
 */
@Component
public class LifeCycleProcessor {

    @Autowired
    private PersistenceManager persistenceManager;

    private final ExecutorService executor;
    private final List<ComponentLifeCycle> detectedComponents;
    private LifeCycleProcessorListener processorListener;

    public LifeCycleProcessor() {
        detectedComponents = new ArrayList<>();
        executor = new NamedThreadPoolExecutor("lifecycle-thread", 1);//Executors.newSingleThreadExecutor();
        Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));
    }

    /**
     * Activates the life cycle processor and initializes all detected components of type {@code BaseLifeCycle}.
     * The method scans for all subclasses of {@code BaseLifeCycle}, creates instances of them, assigns
     * the persistence manager, and sorts them based on their priority. After setup, the startup lifecycle
     * actions are executed for each detected component.
     *
     * @param listener the listener to be notified when all activation processes are completed
     */
    public void activate(LifeCycleProcessorListener listener) {
        processorListener = listener;
        Reflections reflections = new Reflections("ro.cs.tao");
        final Set<Class<? extends BaseLifeCycle>> types = reflections.getSubTypesOf(BaseLifeCycle.class);
        for (Class<? extends BaseLifeCycle> type : types) {
            try {
                final BaseLifeCycle instance = type.newInstance();
                instance.setPersistenceManager(persistenceManager);
                detectedComponents.add(instance);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        detectedComponents.sort(Comparator.comparingInt(ComponentLifeCycle::priority));
        onStartUp();
    }

    /**
     * Executes the startup lifecycle actions for all detected components of type {@code ComponentLifeCycle}.
     * Each component's {@code onStartUp()} method is invoked as part of its startup process. The execution
     * of these lifecycle actions is done using an {@code ExecutorService} to potentially run tasks
     * asynchronously. If any exceptions occur during the startup of individual components, they are caught
     * and logged to prevent interruption to the rest of the startup process.
     *
     * After all component lifecycle startup methods are processed, the {@code LifeCycleProcessorListener}'s
     * {@code activationCompleted} method is invoked to notify that the activation process is finished.
     */
    private void onStartUp() {
        final List<Callable<Void>> tasks = detectedComponents.stream().map(c -> (Callable<Void>) () -> {
            try {
                c.onStartUp();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
        try {
            executor.invokeAll(tasks);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (processorListener != null) {
            processorListener.activationCompleted();
        }
    }

    /**
     * Executes the shutdown lifecycle actions for all detected components of type {@code ComponentLifeCycle}.
     * For each component, the {@code onShutdown()} method is invoked asynchronously using an {@code ExecutorService}.
     * This ensures that the shutdown tasks for individual components can run in parallel, improving overall execution
     * efficiency.
     *
     * During the execution of the {@code onShutdown()} method, any exceptions thrown by individual components are
     * caught and logged, allowing the shutdown process to continue for the remaining components without interruption.
     *
     * This method is intended to be called when the system is being shut down to ensure that all components
     * perform necessary cleanup and finalization tasks.
     */
    private void onShutdown() {
        detectedComponents.forEach(c -> {
            executor.submit(() -> {
                try {
                    c.onShutdown();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
        });
    }
}
