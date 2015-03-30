/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jbischoff.taxi.emissions;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * 
 * Use the config file as created by the 
 * {@link org.matsim.contrib.emissions.example.CreateEmissionConfig CreateEmissionConfig} to calculate 
 * emissions based on the link leave events of an events file. Results are written into an emission event file. 
 *
 * @author benjamin, julia
 */
public class TaxiOfflineEmissionTool {
	
	private final static String dir = "C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/scenarios/2014_10_basic_scenario_v4/emissions/";
	private static final String configFile = dir + "config.xml";
	
	private static final String eventsFile = dir + "events.out.xml.gz";
	private static final String emissionEventOutputFile = dir + "emission.events.offline.xml.gz";
	
	// =======================================================================================================		
	
	public static void main (String[] args) throws Exception{
		Config config = ConfigUtils.loadConfig(configFile, new EmissionsConfigGroup());
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		EmissionModule emissionModule = new EmissionModule(scenario);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(emissionModule.getWarmEmissionHandler());
		eventsManager.addHandler(emissionModule.getColdEmissionHandler());
		
		EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
		emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);
		
		emissionEventWriter.closeFile();

		emissionModule.writeEmissionInformation(emissionEventOutputFile);
	}


}