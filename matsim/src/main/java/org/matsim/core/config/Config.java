/* *********************************************************************** *
 * project: org.matsim.*
 * Config.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.consistency.ConfigConsistencyChecker;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.EvacuationConfigGroup;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.HouseholdsConfigGroup;
import org.matsim.core.config.groups.LocationChoiceConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoadPricingConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.SignalSystemsConfigGroup;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;

/**
 * Stores all configuration settings specified in a configuration file
 * and provides access to the settings at runtime.
 *
 * @author mrieser
 */
public class Config {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	/** Map of all config-groups known to this instance. */
	private final TreeMap<String, Module> modules = new TreeMap<String,Module>();

	/* the following members are for the direct access to the core config groups. */

	private GlobalConfigGroup global = null;
	private ControlerConfigGroup controler = null;
	private SimulationConfigGroup simulation = null;
	private CountsConfigGroup counts = null;
	private CharyparNagelScoringConfigGroup charyparNagelScoring = null;
	private NetworkConfigGroup network = null;
	private PlansConfigGroup plans = null;
	private PlanomatConfigGroup planomat = null;
	private PlansCalcRouteConfigGroup plansCalcRoute = null;
	private FacilitiesConfigGroup facilities = null;
	private RoadPricingConfigGroup roadpricing = null;
	private EvacuationConfigGroup evacuation = null;
	private StrategyConfigGroup strategy = null;
	private LocationChoiceConfigGroup locationchoice = null;
	private SignalSystemsConfigGroup signalSystemConfigGroup = null;
	private ScenarioConfigGroup scenarioConfigGroup = null;
	private VspExperimentalConfigGroup vspExperimentalGroup = null;
	private QSimConfigGroup qSimConfigGroup = null;

	private TravelTimeCalculatorConfigGroup travelTimeCalculatorConfigGroup;

	private final List<ConfigConsistencyChecker> consistencyCheckers = new ArrayList<ConfigConsistencyChecker>();

	private HouseholdsConfigGroup households;

	private OTFVisConfigGroup otfVis;

	/** static Logger-instance. */
	private static final Logger log = Logger.getLogger(Config.class);

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public Config() {
		// nothing to do
	}

	/**
	 * Adds all the commonly used config-groups, also known as "core modules",
	 * to this config-instance. This should be called before reading any
	 * configuration from file.
	 */
	public void addCoreModules() {
		this.global = new GlobalConfigGroup();
		this.modules.put(GlobalConfigGroup.GROUP_NAME, this.global);

		this.controler = new ControlerConfigGroup();
		this.modules.put(ControlerConfigGroup.GROUP_NAME, this.controler);

		this.simulation = new SimulationConfigGroup();
		this.modules.put(SimulationConfigGroup.GROUP_NAME, this.simulation);

		this.counts = new CountsConfigGroup();
		this.modules.put(CountsConfigGroup.GROUP_NAME, this.counts);

		this.charyparNagelScoring = new CharyparNagelScoringConfigGroup();
		this.modules.put(CharyparNagelScoringConfigGroup.GROUP_NAME, this.charyparNagelScoring);

		this.network = new NetworkConfigGroup();
		this.modules.put(NetworkConfigGroup.GROUP_NAME, this.network);

		this.plans = new PlansConfigGroup();
		this.modules.put(PlansConfigGroup.GROUP_NAME, this.plans);

		this.households = new HouseholdsConfigGroup();
		this.modules.put(HouseholdsConfigGroup.GROUP_NAME, this.households);

		this.planomat = new PlanomatConfigGroup();
		this.modules.put(PlanomatConfigGroup.GROUP_NAME, this.planomat);

		this.facilities = new FacilitiesConfigGroup();
		this.modules.put(FacilitiesConfigGroup.GROUP_NAME, this.facilities);

		this.strategy = new StrategyConfigGroup();
		this.modules.put(StrategyConfigGroup.GROUP_NAME, this.strategy);

		this.roadpricing = new RoadPricingConfigGroup();
		this.modules.put(RoadPricingConfigGroup.GROUP_NAME, this.roadpricing);

		this.evacuation = new EvacuationConfigGroup();
		this.modules.put(EvacuationConfigGroup.GROUP_NAME, this.evacuation);

		this.locationchoice = new LocationChoiceConfigGroup();
		this.modules.put(LocationChoiceConfigGroup.GROUP_NAME, this.locationchoice);

		this.signalSystemConfigGroup = new SignalSystemsConfigGroup();
		this.modules.put(SignalSystemsConfigGroup.GROUPNAME, this.signalSystemConfigGroup);

		this.travelTimeCalculatorConfigGroup = new TravelTimeCalculatorConfigGroup();
		this.modules.put(TravelTimeCalculatorConfigGroup.GROUPNAME, this.travelTimeCalculatorConfigGroup);

		this.scenarioConfigGroup = new ScenarioConfigGroup();
		this.modules.put(ScenarioConfigGroup.GROUP_NAME, this.scenarioConfigGroup);

		this.plansCalcRoute = new PlansCalcRouteConfigGroup();
		this.modules.put(PlansCalcRouteConfigGroup.GROUP_NAME, this.plansCalcRoute);

		this.vspExperimentalGroup = new VspExperimentalConfigGroup();
		this.modules.put(VspExperimentalConfigGroup.GROUP_NAME, this.vspExperimentalGroup);

		this.otfVis = new OTFVisConfigGroup() ;
		this.modules.put( OTFVisConfigGroup.GROUP_NAME, this.otfVis ) ;

	}

	/** Checks each module for consistency, e.g. if the parameters that are currently set make sense
	 * in their combination. */
	public void checkConsistency() {
		for (Module m : this.modules.values()) {
			m.checkConsistency();
		}
		for (ConfigConsistencyChecker c : this.consistencyCheckers){
			c.checkConsistency(this);
		}

	}

	//////////////////////////////////////////////////////////////////////
	// add / set methods
	//////////////////////////////////////////////////////////////////////

	/** Creates a new module / config-group with the specified name.
	 * @param name The name of the config-group to be created.
	 *
	 * @return the newly created config group
	 * @throws IllegalArgumentException if a config-group with the specified name already exists.
	 */
	public final Module createModule(final String name) {
		if (this.modules.containsKey(name)) {
			throw new IllegalArgumentException("Module " + name + " exists already.");
		}
		Module m = new Module(name);
		this.modules.put(name, m);
		return m;
	}

	/**
	 * Adds the specified module / config-group with the specified name to the configuration.
	 *
	 * @param name
	 * @param module
	 *
	 * @throws IllegalArgumentException if a config-group with the specified name already exists.
	 */
	public final void addModule(final String name, final Module module) {
		if (this.modules.containsKey(name)) {
			throw new IllegalArgumentException("Module " + name + " exists already.");
		}
		this.modules.put(name, module);
	}

	/**
	 * Removes the specified module / config-group with the specified name from the configuration.
	 * Does nothing if this module was not existing.
	 *
	 * @param name
	 * @param module
	 *
	 */
	public final void removeModule(final String name) {
		if (this.modules.containsKey(name)) {
			this.modules.remove(name);
			log.warn("Module \"" + name + "\" is removed manually from config");

		}
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final TreeMap<String, Module> getModules() {
		return this.modules;
	}

	/**
	 * Returns the requested module, or <code>null</code> if the module does not exist.
	 *
	 * @param moduleName name of the requested module
	 * @return requested module
	 */
	public final Module getModule(final String moduleName) {
		return this.modules.get(moduleName);
	}

	/** Returns the requested parameter. If the module or parameter is not known, an
	 * error is logged and an IllegalArgumentException is thrown.
	 *
	 * @param moduleName
	 * @param paramName
	 * @return the requested parameter
	 *
	 * @throws IllegalArgumentException if the module or parameter does not exist
	 * @see #findParam(String, String)
	 */
	public final String getParam(final String moduleName, final String paramName) {
		Module m = this.modules.get(moduleName);
		if (m == null) {
			log.error("Module \"" + moduleName + "\" is not known.");
			throw new IllegalArgumentException("Module \"" + moduleName + "\" is not known.");
		}
		String str = m.getValue(paramName);
		if (str == null) {
			log.error("Parameter \"" + paramName + "\" of module \"" + moduleName + "\" is not known");
			throw new IllegalArgumentException("Module \"" + moduleName + "\" is not known.");
		}
		return str;
	}

	/**
	 * Returns the value of the specified parameter if it exists, or <code>null</code> otherwise.
	 *
	 * @param moduleName name of the config-module
	 * @param paramName name of parameter in the specified module
	 * @return value of the parameter if it exists, <code>null</code> otherwise
	 *
	 * @see #getParam(String, String)
	 */
	public final String findParam(final String moduleName, final String paramName) {
		Module m = this.modules.get(moduleName);
		if (m == null) {
			return null;
		}
		try {
			String str = m.getValue(paramName);
			if (str == null) {
				return null;
			}
			return str;
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[nof_modules=" + this.modules.size() + "]";
	}

	//////////////////////////////////////////////////////////////////////
	// is used for using Config without a config-file given
	//////////////////////////////////////////////////////////////////////
	/**
	 * Sets the parameter <code>paramName</code> in the module/config-group
	 * <code>moduleName</code> to the specified value.
	 * If there is no config-group with the specified name, a new group will
	 * be created.
	 *
	 * @param moduleName
	 * @param paramName
	 * @param value
	 */
	public final void setParam(final String moduleName, final String paramName, final String value) {
		Module m = this.modules.get(moduleName);
		if (m == null) {
			m = createModule(moduleName);
			log.info("module \"" + moduleName + "\" added.");
		}
		if (m != null) {
			m.addParam(paramName, value);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// direct access to modules / groups
	//////////////////////////////////////////////////////////////////////

	public final GlobalConfigGroup global() {
		return this.global;
	}

	public final ControlerConfigGroup controler() {
		return this.controler;
	}

	public final SimulationConfigGroup simulation() {
		return this.simulation;
	}

	public final CountsConfigGroup counts() {
		return this.counts;
	}

	public final CharyparNagelScoringConfigGroup charyparNagelScoring() {
		return this.charyparNagelScoring;
	}

	public final NetworkConfigGroup network() {
		return this.network;
	}

	public final PlansConfigGroup plans() {
		return this.plans;
	}

	public final HouseholdsConfigGroup households() {
		return this.households;
	}

	public final FacilitiesConfigGroup facilities() {
		return this.facilities;
	}

	public final RoadPricingConfigGroup roadpricing() {
		return this.roadpricing;
	}

	public final EvacuationConfigGroup evacuation() {
		return this.evacuation;
	}

	public final StrategyConfigGroup strategy() {
		return this.strategy;
	}

	public final LocationChoiceConfigGroup locationchoice() {
		return this.locationchoice;
	}

	public final PlanomatConfigGroup planomat() {
		return this.planomat;
	}

	public SignalSystemsConfigGroup signalSystems() {
		return this.signalSystemConfigGroup;
	}

	public TravelTimeCalculatorConfigGroup travelTimeCalculator() {
		return this.travelTimeCalculatorConfigGroup;
	}

	public ScenarioConfigGroup scenario() {
		return this.scenarioConfigGroup;
	}

	public void addConfigConsistencyChecker(
			final ConfigConsistencyChecker checker) {
		this.consistencyCheckers.add(checker);
	}

	public PlansCalcRouteConfigGroup plansCalcRoute() {
		return this.plansCalcRoute;
	}

	public VspExperimentalConfigGroup vspExperimental() {
		return this.vspExperimentalGroup;
	}

	public OTFVisConfigGroup otfVis() {
		return this.otfVis;
	}

  public QSimConfigGroup getQSimConfigGroup() {
    return this.qSimConfigGroup;
  }

  public void setQSimConfigGroup(final QSimConfigGroup qSimConfigGroup) {
    this.qSimConfigGroup = qSimConfigGroup;
    this.modules.put(QSimConfigGroup.GROUP_NAME, qSimConfigGroup);
  }
}
