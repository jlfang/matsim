/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.evacuation.run;

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

public class ShelterEvacuationControllerTest extends MatsimTestCase{

  private static final Logger log = Logger.getLogger(ShelterEvacuationControllerTest.class);
  
	public void testShelterEvacuationController() {
		String config = getInputDirectory() + "config.xml";
    String refEventsFileIt0 = getInputDirectory() + "0.events.xml.gz";
    String testEventsFileIt0 = getOutputDirectory() +"ITERS/it.0/0.events.xml.gz";

		String refEventsFile = getInputDirectory() + "10.events.xml.gz";
		String testEventsFile = getOutputDirectory() +"ITERS/it.10/10.events.xml.gz";


		EvacuationQSimControllerII controler = new EvacuationQSimControllerII(new String [] {config});
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(10);
		controler.run();
    //it 0
		log.info("comparing events files: ");
    log.info(refEventsFileIt0);
    log.info(testEventsFileIt0);
    assertEquals("different events-files.", CRCChecksum.getCRCFromFile(refEventsFileIt0),  CRCChecksum.getCRCFromFile(testEventsFileIt0));
    //it 10
		log.info("comparing events files: ");
		log.info(refEventsFile);
		log.info(testEventsFile);
		assertEquals("different events-files.", CRCChecksum.getCRCFromFile(refEventsFile),	CRCChecksum.getCRCFromFile(testEventsFile));
	}

}
