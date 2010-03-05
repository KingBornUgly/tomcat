/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.util;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.res.StringManager;


/**
 * Base implementation of the {@link Lifecycle} interface that implements the
 * state transition rules for {@link Lifecycle#start()} and
 * {@link Lifecycle#stop()}
 */
public abstract class LifecycleBase implements Lifecycle {

    private static Log log = LogFactory.getLog(LifecycleBase.class);
    
    private static StringManager sm =
        StringManager.getManager("org.apache.catalina.util");


    /**
     * Used to handle firing lifecycle events.
     * TODO: Consider merging LifecycleSupport into this class.
     */
    private LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The current state of the source component.
     */
    private volatile LifecycleState state = LifecycleState.NEW;


    /**
     * {@inheritDoc}
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }

    
    /**
     * Allow sub classes to fire {@link Lifecycle} events.
     * 
     * @param type  Event type
     * @param data  Data associated with event.
     */
    protected void fireLifecycleEvent(String type, Object data) {
        lifecycle.fireLifecycleEvent(type, data);
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public final void start() throws LifecycleException {
        
        synchronized (this) {
            if (LifecycleState.STARTING_PREP.equals(state) ||
                    LifecycleState.STARTING.equals(state) ||
                    LifecycleState.STARTED.equals(state)) {
                
                if (log.isDebugEnabled()) {
                    Exception e = new LifecycleException();
                    log.debug(sm.getString("lifecycleBase.alreadyStarted",
                            toString()), e);
                } else if (log.isInfoEnabled()) {
                    log.info(sm.getString("lifecycleBase.alreadyStarted",
                            toString()));
                }
                
                return;
            }
            
            if (!state.equals(LifecycleState.NEW) &&
                    !state.equals(LifecycleState.STOPPED)) {
                invalidTransition(Lifecycle.BEFORE_START_EVENT);
            }

            // Set state and fire event separately rather than via setState()
            // so event fires outside of sync boundary
            state = LifecycleState.STARTING_PREP;
        }
        
        lifecycle.fireLifecycleEvent(Lifecycle.BEFORE_START_EVENT, null);

        startInternal();

        if (state.equals(LifecycleState.FAILED) ||
                state.equals(LifecycleState.MUST_STOP)) {
            stop();
        } else {
            // Shouldn't be necessary but acts as a check that sub-classes are doing
            // what they are supposed to.
            if (!state.equals(LifecycleState.STARTING)) {
                invalidTransition(Lifecycle.AFTER_START_EVENT);
            }
            
            setState(LifecycleState.STARTED);
        }
    }


    /**
     * Sub-classes must ensure that:
     * <ul>
     * <li>the {@link Lifecycle#START_EVENT} is fired during the execution of
     *     this method</li>
     * <li>the state is changed to {@link LifecycleState#STARTING} when the
     *     {@link Lifecycle#START_EVENT} is fired
     * </ul>
     * 
     * If a component fails to start it may either throw a
     * {@link LifecycleException} which will cause it's parent to fail to start
     * or it can place itself in the error state in which case {@link #stop()}
     * will be called on the failed component but the parent component will
     * continue to start normally.
     * 
     * @throws LifecycleException
     */
    protected abstract void startInternal() throws LifecycleException;


    /**
     * {@inheritDoc}
     */
    @Override
    public final void stop() throws LifecycleException {

        synchronized (this) {
            if (LifecycleState.STOPPING_PREP.equals(state) ||
                    LifecycleState.STOPPING.equals(state) ||
                    LifecycleState.STOPPED.equals(state)) {
    
                if (log.isDebugEnabled()) {
                    Exception e = new LifecycleException();
                    log.debug(sm.getString("lifecycleBase.alreadyStopped",
                            toString()), e);
                } else if (log.isInfoEnabled()) {
                    log.info(sm.getString("lifecycleBase.alreadyStopped",
                            toString()));
                }
                
                return;
            }
            
            if (!state.equals(LifecycleState.STARTED) &&
                    !state.equals(LifecycleState.FAILED)) {
                invalidTransition(Lifecycle.BEFORE_STOP_EVENT);
            }
            
            // Set state and fire event separately rather than via setState()
            // so event fires outside of sync boundary
            state = LifecycleState.STOPPING_PREP;
        }
        
        lifecycle.fireLifecycleEvent(Lifecycle.BEFORE_STOP_EVENT, null);

        stopInternal();

        // Shouldn't be necessary but acts as a check that sub-classes are doing
        // what they are supposed to.
        if (!state.equals(LifecycleState.STOPPING)) {
            invalidTransition(Lifecycle.AFTER_STOP_EVENT);
        }

        setState(LifecycleState.STOPPED);
    }


    /**
     * Sub-classes must ensure that:
     * <ul>
     * <li>the {@link Lifecycle#STOP_EVENT} is fired during the execution of
     *     this method</li>
     * <li>the state is changed to {@link LifecycleState#STOPPING} when the
     *     {@link Lifecycle#STOP_EVENT} is fired
     * </ul>
     * 
     * @throws LifecycleException
     */
    protected abstract void stopInternal() throws LifecycleException;


    /**
     * {@inheritDoc}
     */
    public LifecycleState getState() {
        return state;
    }


    /**
     * Provides a mechanism for sub-classes to update the component state.
     * Calling this method will automatically fire any associated
     * {@link Lifecycle} event.
     * 
     * @param state The new state for this component
     */
    protected void setState(LifecycleState state) {
        setState(state, null);
    }
    
    
    /**
     * Provides a mechanism for sub-classes to update the component state.
     * Calling this method will automatically fire any associated
     * {@link Lifecycle} event.
     * 
     * @param state The new state for this component
     * @param data  The data to pass to the associated {@link Lifecycle} event
     */
    protected void setState(LifecycleState state, Object data) {
        this.state = state;
        String lifecycleEvent = state.getLifecycleEvent();
        if (lifecycleEvent != null) {
            fireLifecycleEvent(lifecycleEvent, data);
        }
    }

    
    private void invalidTransition(String type) throws LifecycleException {
        String msg = sm.getString("lifecycleBase.invalidTransition", type,
                toString(), state);
        throw new LifecycleException(msg);
    }
}
