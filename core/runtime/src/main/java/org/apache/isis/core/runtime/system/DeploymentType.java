/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */


package org.apache.isis.core.runtime.system;

import java.util.List;

import org.apache.isis.core.commons.debug.DebuggableWithTitle;
import org.apache.isis.core.commons.debug.DebugString;
import org.apache.isis.core.runtime.context.IsisContext;
import org.apache.isis.core.runtime.context.IsisContextStatic;
import org.apache.isis.core.runtime.session.IsisSessionFactory;

import com.google.common.collect.Lists;

/**
 * Whether running on client or server side etc.
 * 
 * <p>
 * Previously this was an <tt>enum</tt>, but it is now a regular class.  The change has been made to provide 
 * more flexibility in setting up the {@link IsisContext} lookup.
 * 
 * 
 * implementation can be created, specifying in turn a custom {@link ContextCategory} (also now a regular class) can be specified, allowing different was of setting up the
 * .
 * 
 * <p>
 * To use this capability:
 * <ul>
 * <li>Write your new implementation of {@link IsisContext}, along with a static factory method (cf {@link IsisContextStatic#createInstance(IsisSessionFactory)})
 * <li>Create a new subclass of {@link ContextCategory} (also now a regular class rather than an <tt>enum</tt>); this
 * is where your code goes to instantiate your {@link IsisContext} implementation</li>
 * <li>Create a new subclass of {@link DeploymentType}, passing in the custom {@link ContextCategory} in its constructor</li>
 * <li>In your bootstrap code, instantiate your new {@link DeploymentType} subclass</li>
 * <li>When you run your app, don't forget to specify your custom {@link DeploymentType}, eg using the <tt>--type</tt> command line arg</li>
 * </ul>
 */
public class DeploymentType {

	private static List<DeploymentType> deploymentTypes = Lists.newArrayList();

	public static DeploymentType EXPLORATION = new DeploymentType("EXPLORATION", DeploymentCategory.EXPLORING, ContextCategory.STATIC_RELAXED, SystemConstants.VIEWER_DEFAULT, Splash.SHOW);
	public static DeploymentType PROTOTYPE = new DeploymentType("PROTOTYPE", DeploymentCategory.PROTOTYPING, ContextCategory.STATIC_RELAXED, SystemConstants.VIEWER_DEFAULT, Splash.SHOW);
	public static DeploymentType CLIENT = new DeploymentType("CLIENT", DeploymentCategory.PRODUCTION, ContextCategory.STATIC, SystemConstants.VIEWER_DEFAULT, Splash.SHOW);
	public static DeploymentType SERVER = new DeploymentType("SERVER", DeploymentCategory.PRODUCTION, ContextCategory.THREADLOCAL, null, Splash.NO_SHOW);
	public static DeploymentType SERVER_EXPLORATION = new DeploymentType("SERVER_EXPLORATION", DeploymentCategory.EXPLORING, ContextCategory.THREADLOCAL, null, Splash.NO_SHOW);
	public static DeploymentType SERVER_PROTOTYPE = new DeploymentType("SERVER_PROTOTYPE", DeploymentCategory.PROTOTYPING, ContextCategory.THREADLOCAL, null, Splash.NO_SHOW);
	public static DeploymentType SINGLE_USER = new DeploymentType("SINGLE_USER", DeploymentCategory.PRODUCTION, ContextCategory.STATIC, SystemConstants.VIEWER_DEFAULT, Splash.NO_SHOW);
	public static DeploymentType UTILITY = new DeploymentType("UTILITY", DeploymentCategory.EXPLORING, ContextCategory.STATIC, null, Splash.NO_SHOW);

	private final String name;
	private final DeploymentCategory deploymentCategory;
	private final ContextCategory contextCategory;
	private final String defaultViewer;
	private final Splash splash;

	public DeploymentType(String name, final DeploymentCategory category, ContextCategory contextCategory, final String defaultViewer, Splash splash) {
		this.deploymentCategory = category;
		this.contextCategory = contextCategory;
		this.defaultViewer = defaultViewer;
		this.splash = splash;
		this.name = name;
		deploymentTypes.add(this);
	}

	public DebuggableWithTitle getDebug() {
	    return new DebuggableWithTitle() {

            public void debugData(DebugString debug) {
                debug.appendln("Category", deploymentCategory);
                debug.appendln("Context", contextCategory);
                debug.appendln("Default viewer", defaultViewer == null ? "none" : defaultViewer);
                debug.appendln("Show splash", splash);
                debug.appendln();
                debug.appendln("Name", friendlyName());
                debug.appendln("Can specify object store", canSpecifyObjectStore());
                debug.appendln("Can install fixtures", canInstallFixtures());
                debug.appendln("Should monitor", shouldMonitor());
            }

            public String debugTitle() {
                return "Deployment type";
            }
	    };
	}
	
	public void initContext(IsisSessionFactory sessionFactory) {
		contextCategory.initContext(sessionFactory);
	}

    /**
     * Whether the list of viewers names provided is compatible with this {@link DeploymentType}.
     *
     * @see ContextCategory#canSpecifyViewers(List)
     */
	public boolean canSpecifyViewers(List<String> viewers) {
		return contextCategory.canSpecifyViewers(viewers);
	}

	/**
	 * Whether the list of connector names provided is compatible with this {@link DeploymentType}.
	 * 
	 * <p>
	 * Only a {@link #CLIENT} may have connectors.
	 */
	public boolean canSpecifyConnectors(List<String> connectors) {
		return connectors.size() == 0 || this == CLIENT;
	}

    /**
     * Whether specifying an object store is compatible with this {@link DeploymentType}.
     * 
     * <p>
     * Only a {@link #CLIENT} may NOT have an object store.
     */
	public boolean canSpecifyObjectStore() {
		return this != CLIENT;
	}

    /**
     * Whether specifying fixtures is compatible with this {@link DeploymentType}.
     * 
     * <p>
     * Only a {@link #CLIENT} may NOT have an object store.
     */
	public boolean canInstallFixtures() {
		return this != CLIENT;
	}

	public boolean shouldShowSplash() {
		return splash.isShow();
	}
	
	public boolean shouldMonitor() {
		return (this == SERVER) && isProduction();
	}

	public boolean isExploring() {
		return deploymentCategory == DeploymentCategory.EXPLORING;
	}

	public boolean isPrototyping() {
		return deploymentCategory == DeploymentCategory.PROTOTYPING;
	}

	public boolean isProduction() {
		return deploymentCategory == DeploymentCategory.PRODUCTION;
	}

	public void addDefaultViewer(List<String> requestedViewers) {
		if (requestedViewers.size() == 0 && defaultViewer != null) {
			requestedViewers.add(defaultViewer);
		}
	}
	
	/**
	 * Look up {@link DeploymentType} by their {@link #name()}.
	 * 
	 * <p>
	 * Can substitute <tt>'-'</tt> instead of <tt>'_'</tt>; for example <tt>server_exploration</tt>
	 * will lookup the same as <tt>server-exploration</tt>.
	 */
	public static DeploymentType lookup(final String str) {
		String underscoredStr = str.replace('-', '_').toUpperCase();
		for (DeploymentType deploymentType : deploymentTypes) {
			if (underscoredStr.equals(deploymentType.name())) {
				return deploymentType;
			}
		}
		throw new IllegalArgumentException(String.format("Unknown deployment type '%s'", str));
	}
	
	public String friendlyName() {
		return nameLowerCase().replace('_', '-');
	}

	public String nameLowerCase() {
		return name().toLowerCase();
	}

	public String name() {
		return name;
	}

}