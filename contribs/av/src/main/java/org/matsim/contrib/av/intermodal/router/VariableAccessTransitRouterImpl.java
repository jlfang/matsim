/* *********************************************************************** *
 * project: org.matsim.*
 * TranitRouter.java
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

package org.matsim.contrib.av.intermodal.router;

import java.util.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.InitialNode;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.*;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * This class is based on the default transit router in MATSim. Instead of always using the mode
 * walk to access and egress transit stops, it allows for multiple possible access and egress
 * modes. Apart from this, it is basically a copy of the default transit router
 * {@link org.matsim.pt.router.TransitRouterImpl}. It does not allow to combine multiple pt legs 
 * with legs of other modes in-between, such as 
 * <i>bike leg - train leg - <u>bike leg</u> - bus leg - bike leg</i>.
 * <p>
 * 
 * The access and the egress modes are chosen independently from each other, that means in a trip
 * an access mode different from the egress mode can be used. This leads to a design issue: When
 * the router compares the full path cost (access leg + pt legs + egress leg) with a direct, non-pt
 * trip, it does so assuming mode walk for the non-pt trip. Alternatively it could be changed to
 * use one of the access and egress modes. However, this would implicitly enable mode choice as
 * routes only using the access or egress mode without any pt or transit_walk leg would be 
 * returned. There is no information left which indicates that this was meant to be a pt trip and
 * the MainModeIdentifier would always consider this a trip of the chosen access or egress mode.
 * So during replanning the corresponding router for this access/egress mode would be used instead
 * of any pt router, effectively changing modes without enabling explicitly mode choice as a
 * replanning strategy. Therefore we keep comparing with a direct <u>walk</u> leg. If mode choice
 * is enabled as a replanning strategy this makes up for the flawed (assuming always mode walk 
 * ignoring other possible modes) direct access/egress mode leg cost calculation, as the selection 
 * of that mode will lead to a direct leg with that mode.
 * gleich, Mar'18
 * <p>
 * 
 * TODO:
 * Whereas the default transit router only needs marginal utilities for the modes "pt" and "walk"
 * which are included in the {@link org.matsim.pt.router.TransitRouterConfig},
 * this router needs parameters for all the admitted access and egress modes. So either the
 * TransitRouterConfig would have to be adapted to include marginal utilities for arbitrary access
 * and egress modes (this change would effect all matsim although only this contrib would need it) 
 * or these parameters are directly taken from 
 * {@link org.matsim.core.config.groups.PlanCalcScoreConfigGroup}. 
 * This class implements the latter way. 
 * However, this leads to another issue as the TransitRouterConfig can
 * have other parameters than PlanCalcScoreConfig. By default these parameters are equal, because
 * they are copied from PlanCalcScoreConfig while creating the TransitRouterConfig. Nevertheless,
 * the TransitRouterConfig has public setters for the marginal utilities, so these can
 * be changed later on. In that case the utilities for access and egress legs by mode walk 
 * (based on PlanCalcScoreConfig) become inconsistent with the utility calculation for 
 * direct walk legs (based on the TransitRouterConfig parameters and done in
 * {@link org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility}). 
 * Scoring will always use PlanCalcScoreConfig values, so changing values in TransitRouterConfig 
 * and thereby introducing differences between routing and scoring parameters seems a bad idea 
 * anyway. So this inconsistency issue won't be dealt with right now.
 * gleich, Mar'18
 * <p>
 * 
 * Not thread-safe because MultiNodeDijkstra is not. Does not expect the TransitSchedule to change once constructed! michaz '13
 *
 * @author jbischoff
 */
public class VariableAccessTransitRouterImpl implements TransitRouter {

	private final TransitRouterNetwork transitNetwork;
	private final Network network;
	private final PlanCalcScoreConfigGroup pcsConfig;
	private final TransitRouterConfig trConfig;
	private final TransitTravelDisutility travelDisutility;
	private final TravelTime travelTime;
	private final VariableAccessEgressTravelDisutility variableAccessEgressTravelDisutility;


	private final PreparedTransitSchedule preparedTransitSchedule;

	

	public VariableAccessTransitRouterImpl(
			final PlanCalcScoreConfigGroup planCalcScoreConfig,
			final TransitRouterConfig transitRouterConfig,
			final PreparedTransitSchedule preparedTransitSchedule,
			final TransitRouterNetwork routerNetwork,
			final TravelTime travelTime,
			final TransitTravelDisutility travelDisutility, final VariableAccessEgressTravelDisutility variableAccessEgressTravelDisutility,
			final Network network) {
		this.pcsConfig = planCalcScoreConfig;
		this.trConfig = transitRouterConfig;
		this.transitNetwork = routerNetwork;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.preparedTransitSchedule = preparedTransitSchedule;
		this.variableAccessEgressTravelDisutility = variableAccessEgressTravelDisutility;
		this.network = network;
	}

	private Map<Node, InitialNode> locateWrappedNearestTransitNodes(Person person, Coord coord, double departureTime) {
		Collection<TransitRouterNetworkNode> nearestNodes = this.transitNetwork.getNearestNodes(coord, this.trConfig.getSearchRadius());
		if (nearestNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkNode nearestNode = this.transitNetwork.getNearestNode(coord);
			double distance = CoordUtils.calcEuclideanDistance(coord, nearestNode.stop.getStopFacility().getCoord());
			nearestNodes = this.transitNetwork.getNearestNodes(coord, distance + this.trConfig.getExtensionRadius());
		}
		Map<Node, InitialNode> wrappedNearestNodes = new LinkedHashMap<>();
		for (TransitRouterNetworkNode node : nearestNodes) {
			Coord toCoord = node.stop.getStopFacility().getCoord();
			Leg initialLeg = getAccessEgressLeg(person, coord, toCoord, departureTime);
			double initialTime = initialLeg.getTravelTime();
			double marginalUtilityOfDistance_utl_m = 
					pcsConfig.getModes().get(initialLeg.getMode()).getMonetaryDistanceRate() * pcsConfig.getMarginalUtilityOfMoney() +
					pcsConfig.getModes().get(initialLeg.getMode()).getMarginalUtilityOfDistance();
			double marginalUtilityOfTravelTime_utl_s = 
					pcsConfig.getModes().get(initialLeg.getMode()).getMarginalUtilityOfTraveling()/3600.0 -
					pcsConfig.getPerforming_utils_hr()/3600.;
			//  getMarginalUtilityOfTravelTimeWalk includes the opportunity cost of time.
			double timeCost = - initialLeg.getTravelTime() * marginalUtilityOfTravelTime_utl_s ;
			// (sign: margUtl is negative; overall it should be positive because it is a cost.)
			double distanceCost = - initialLeg.getRoute().getDistance() * marginalUtilityOfDistance_utl_m ;
			// (sign: same as above)
			double initialCost = timeCost + distanceCost;
			/* note that departureTime is always the departure at the trip origin, so for the 
			 * egress leg it will return 
			 * departure time at trip origin + travel time from egress transit stop to the trip destination, 
			 * omitting the travel time for the access leg and for the pt leg to the egress transit stop
			 */
			wrappedNearestNodes.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}
		return wrappedNearestNodes;
	}

	private Leg getAccessEgressLeg(Person person, Coord coord, Coord toCoord, double time) {
		return variableAccessEgressTravelDisutility.getAccessEgressModeAndTraveltime(person, coord, toCoord, time);
	}


	private double getTransferTime(Person person, Coord coord, Coord toCoord) {
		return travelDisutility.getWalkTravelTime(person, coord, toCoord) + this.trConfig.getAdditionalTransferTime();
	}

	private double getAccessEgressDisutility(Person person, Coord coord, Coord toCoord) {
		return travelDisutility.getWalkTravelDisutility(person, coord, toCoord);
	}

	@Override
	public List<Leg> calcRoute(final Facility<?> fromFacility, final Facility<?> toFacility, final double departureTime, final Person person) {
		// find possible start stops
		Map<Node, InitialNode> wrappedFromNodes = this.locateWrappedNearestTransitNodes(person, fromFacility.getCoord(), departureTime);
		// find possible end stops
		Map<Node, InitialNode> wrappedToNodes = this.locateWrappedNearestTransitNodes(person, toFacility.getCoord(), departureTime);

		TransitLeastCostPathTree tree = new TransitLeastCostPathTree(transitNetwork, travelDisutility, travelTime,
				wrappedFromNodes, wrappedToNodes, person);

		// find routes between start and end stop
		Path p = tree.getPath(wrappedToNodes);

		if (p == null) {
			return null;
		}

		double directWalkCost = getAccessEgressDisutility(person, fromFacility.getCoord(), toFacility.getCoord());
		double pathCost = p.travelCost + wrappedFromNodes.get(p.nodes.get(0)).initialCost + wrappedToNodes.get(p.nodes.get(p.nodes.size() - 1)).initialCost;

		if (directWalkCost * trConfig.getDirectWalkFactor() < pathCost) {
			return this.createDirectAccessEgressModeLegList(null, fromFacility.getCoord(), toFacility.getCoord(), departureTime);
		}
		return convertPathToLegList(departureTime, p, fromFacility.getCoord(), toFacility.getCoord(), person);
	}

	private List<Leg> createDirectAccessEgressModeLegList(Person person, Coord fromCoord, Coord toCoord, double time) {
		/* give always mode transit_walk back, otherwise the main mode identifier will not realize 
		 * this was meant to be a pt in the next iteration. getAccessEgressDisutility(...) 
		 * calculates the directWalkCost with transit_walk utilitie parameters anyway. Therefore, 
		 * it would be misleading to return any different mode here. gleich 03-2018
		 */
		List<Leg> legs = new ArrayList<>();
		Leg leg = PopulationUtils.createLeg(TransportMode.transit_walk);
		double walkTime = travelDisutility.getWalkTravelTime(person, fromCoord, toCoord);
		leg.setTravelTime(walkTime);
		Route walkRoute = RouteUtils.createGenericRouteImpl(null, null);
		walkRoute.setTravelTime(walkTime);
		leg.setRoute(walkRoute);
		legs.add(leg);
		return legs;
	}

	protected List<Leg> convertPathToLegList(double departureTime, Path path, Coord fromCoord, Coord toCoord, Person person) {
		// yy would be nice if the following could be documented a bit better.  kai, jul'16
		
		// now convert the path back into a series of legs with correct routes
		double time = departureTime;
		List<Leg> legs = new ArrayList<>();
		Leg leg;
		TransitLine line = null;
		TransitRoute route = null;
		TransitStopFacility accessStop = null;
		TransitRouteStop transitRouteStart = null;
		TransitRouterNetworkLink prevLink = null;
		double currentDistance = 0;
		int transitLegCnt = 0;
		for (Link ll : path.links) {
			TransitRouterNetworkLink link = (TransitRouterNetworkLink) ll;
			if (link.getLine() == null) {
				// (it must be one of the "transfer" links.) finish the pt leg, if there was one before...
				TransitStopFacility egressStop = link.fromNode.stop.getStopFacility();
				if (route != null) {
					leg = PopulationUtils.createLeg(TransportMode.pt);
					ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
					double arrivalOffset = (link.getFromNode().stop.getArrivalOffset() != Time.UNDEFINED_TIME) ? link.fromNode.stop.getArrivalOffset() : link.fromNode.stop.getDepartureOffset();
					double arrivalTime = this.preparedTransitSchedule.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
					ptRoute.setTravelTime(arrivalTime - time);

//					ptRoute.setDistance( currentDistance );
					ptRoute.setDistance( link.getLength() );
					// (see MATSIM-556)

					leg.setRoute(ptRoute);
					leg.setTravelTime(arrivalTime - time);
					time = arrivalTime;
					legs.add(leg);
					transitLegCnt++;
					accessStop = egressStop;
				}
				line = null;
				route = null;
				transitRouteStart = null;
				currentDistance = link.getLength();
			} else {
				// (a real pt link)
				currentDistance += link.getLength();
				if (link.getRoute() != route) {
					// the line changed
					TransitStopFacility egressStop = link.fromNode.stop.getStopFacility();
					if (route == null) {
						// previously, the agent was on a transfer, add the walk leg
						transitRouteStart = ((TransitRouterNetworkLink) ll).getFromNode().stop;
						if (accessStop != egressStop) {
							if (accessStop != null) {
								leg = PopulationUtils.createLeg(TransportMode.transit_walk);
								//							    double walkTime = getWalkTime(person, accessStop.getCoord(), egressStop.getCoord());
								double transferTime = getTransferTime(person, accessStop.getCoord(), egressStop.getCoord());
								Route walkRoute = RouteUtils.createGenericRouteImpl(accessStop.getLinkId(),
										egressStop.getLinkId());
								// (yy I would have expected this from egressStop to accessStop. kai, jul'16)
								
								//							    walkRoute.setTravelTime(walkTime);
								walkRoute.setTravelTime(transferTime);
								
//								walkRoute.setDistance( currentDistance );
								walkRoute.setDistance( trConfig.getBeelineDistanceFactor() * 
										NetworkUtils.getEuclideanDistance(accessStop.getCoord(), egressStop.getCoord()) );
								// (see MATSIM-556)

								leg.setRoute(walkRoute);
								//							    leg.setTravelTime(walkTime);
								leg.setTravelTime(transferTime);
								//							    time += walkTime;
								time += transferTime;
								legs.add(leg);
							} else {
								// accessStop == null, so it must be the first access-leg. If mode is e.g. taxi, we need a transit_walk to get to pt link
								leg = getAccessEgressLeg(person, fromCoord, egressStop.getCoord(),time);
								if (variableAccessEgressTravelDisutility.isTeleportedAccessEgressMode(leg.getMode()))
								{
									leg.getRoute().setEndLinkId(egressStop.getLinkId());
									time += leg.getTravelTime();
									legs.add(leg);


								} else {
									legs.add(leg); //access leg
									time += leg.getTravelTime();
									
									Route walkRoute = RouteUtils.createGenericRouteImpl(
											leg.getRoute().getEndLinkId(), egressStop.getLinkId());
									double walkTime = getTransferTime(person, network.getLinks().get(leg.getRoute().getEndLinkId()).getCoord(), egressStop.getCoord());
									walkRoute.setTravelTime(walkTime);
									walkRoute.setDistance(trConfig.getBeelineDistanceFactor() * 
											NetworkUtils.getEuclideanDistance(network.getLinks().get(leg.getRoute().getEndLinkId()).getCoord(), egressStop.getCoord()) );
								
									Leg walkleg = PopulationUtils.createLeg(TransportMode.transit_walk);
									walkleg.setTravelTime(walkTime);
									walkleg.setRoute(walkRoute);
									time += walkTime;
									legs.add(walkleg);
									
								}
								
//								walkRoute.setDistance( currentDistance );
								// (see MATSIM-556)

							}
						}
						currentDistance = 0;
					}
					line = link.getLine();
					route = link.getRoute();
					accessStop = egressStop;
				}
			}
			prevLink = link;
		}
		if (route != null) {
			// the last part of the path was with a transit route, so add the pt-leg and final walk-leg
			leg = PopulationUtils.createLeg(TransportMode.pt);
			TransitStopFacility egressStop = prevLink.toNode.stop.getStopFacility();
			ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
//			ptRoute.setDistance( currentDistance );
			ptRoute.setDistance( trConfig.getBeelineDistanceFactor() * NetworkUtils.getEuclideanDistance(accessStop.getCoord(), egressStop.getCoord() ) );
			// (see MATSIM-556)
			leg.setRoute(ptRoute);
			double arrivalOffset = ((prevLink).toNode.stop.getArrivalOffset() != Time.UNDEFINED_TIME) ?
					(prevLink).toNode.stop.getArrivalOffset()
					: (prevLink).toNode.stop.getDepartureOffset();
					double arrivalTime = this.preparedTransitSchedule.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
					leg.setTravelTime(arrivalTime - time);
					ptRoute.setTravelTime( arrivalTime - time );
					legs.add(leg);
					transitLegCnt++;
					accessStop = egressStop;
		}
		if (prevLink != null) {
			if (accessStop == null) {
				// no use of pt
				leg = getAccessEgressLeg(person, fromCoord, toCoord, time);
				legs.add(leg);

			} else {
				Leg eleg = getAccessEgressLeg(person, accessStop.getCoord(), toCoord, time);
				
				if (variableAccessEgressTravelDisutility.isTeleportedAccessEgressMode(eleg.getMode())){
					leg = eleg;
					leg.getRoute().setStartLinkId(accessStop.getLinkId());
					legs.add(leg);

				}
				else {
					leg = PopulationUtils.createLeg(TransportMode.transit_walk);
					double walkTime = getTransferTime(person, accessStop.getCoord(), network.getLinks().get(eleg.getRoute().getStartLinkId()).getCoord());
					leg.setTravelTime(walkTime);
					Route walkRoute = RouteUtils.createGenericRouteImpl(accessStop.getLinkId(), eleg.getRoute().getStartLinkId());
					walkRoute.setDistance(trConfig.getBeelineDistanceFactor() * 
											NetworkUtils.getEuclideanDistance(accessStop.getCoord(),network.getLinks().get(eleg.getRoute().getStartLinkId()).getCoord()));
					leg.setRoute(walkRoute);
					legs.add(leg);
					legs.add(eleg);
				}
			}
		}
		if (transitLegCnt == 0) {
			// it seems, the agent only walked
			legs.clear();
			try{
			leg = getAccessEgressLeg(person, fromCoord, toCoord, time);

			legs.add(leg);
			} catch (NullPointerException e){
				throw new RuntimeException(" npe: person"+ person + fromCoord + toCoord + time);
			}
		}
		return legs;
	}

	public TransitRouterNetwork getTransitRouterNetwork() {
		return this.transitNetwork;
	}

	protected TransitRouterNetwork getTransitNetwork() {
		return transitNetwork;
	}

	protected TransitRouterConfig getConfig() {
		return trConfig;
	}

}
