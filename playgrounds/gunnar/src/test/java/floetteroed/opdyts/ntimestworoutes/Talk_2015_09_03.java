package floetteroed.opdyts.ntimestworoutes;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.VectorBasedObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.ObjectiveFunctionChangeConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.TrajectorySamplingSelfTuner;
import floetteroed.utilities.latex.PSTricksDiagramWriter;
import floetteroed.utilities.math.Discretizer;
import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class Talk_2015_09_03 {

	private static Vector minMaxExternalities(final double maxExternality,
			final int linkCnt) {
		final Vector externalities = new Vector(linkCnt);
		for (int i = 0; i < linkCnt; i++) {
			final double externality = (i % 2 == 0) ? maxExternality : 0.0;
			externalities.set(i, externality);
			System.out.println("externality on link " + i + " is "
					+ externality);
		}
		return externalities;
	}

	private static Vector randomExternalities(final double maxExternality,
			final int linkCnt, final Random rnd) {
		final Vector externalities = new Vector(linkCnt);
		for (int i = 0; i < linkCnt; i++) {
			final double externality = rnd.nextDouble() * maxExternality;
			externalities.set(i, externality);
			System.out.println("externality on link " + i + " is "
					+ externality);
		}
		return externalities;
	}

	private static void write2file(final List<Double> cumulativeTransitions,
			final List<Double> rawData, final int transitionsBinSize,
			final int transitionsBinCnt, final PrintWriter writer) {
		final List<Double> data = Discretizer.interpolateOrder1(
				cumulativeTransitions, rawData, 0, transitionsBinSize,
				transitionsBinCnt);
		for (int i = 0; i < data.size(); i++) {
			writer.print(data.get(i));
			if (i < data.size() - 1) {
				writer.print(",");
			}
		}
		writer.println();
		writer.flush();
	}

	public static void main(String[] args) throws FileNotFoundException {

		System.out.println("STARTED ...");

		final Random rnd = new Random();
		final double demand = 500;
		final double capacity = 1000;
		final int linkCnt = 2 * 20;

		final int maxIterations = Integer.MAX_VALUE;
		final int transitionBinCnt = 200;
		final int transitionBinSize = 100;
		final int maxTransitions = transitionBinCnt * transitionBinSize;

		final int replications = 1;
		final boolean keepBestSolution = true;
		final int maxDeltaBin = 3;
		final double replanningProbability = 0.1;

		for (Integer populationSize : new Integer[] { 32 }) { // 2, 4, 8, 16,
																// 32, 64, 128,
																// 256}) {

			final List<List<Double>> naiveTransitionList = new ArrayList<List<Double>>();
			final List<List<Double>> naiveObjectiveFunctionValueList = new ArrayList<List<Double>>();

			final List<List<Double>> transitionList = new ArrayList<List<Double>>();
			final List<List<Double>> objectiveFunctionValueList = new ArrayList<List<Double>>();
			final List<List<Double>> equilibriumWeightList = new ArrayList<List<Double>>();
			final List<List<Double>> uniformityWeightList = new ArrayList<List<Double>>();

			final double maxExternality = 3.0;
			final Vector externalities = minMaxExternalities(maxExternality,
					linkCnt);
			// final Vector externalities = randomExternalities(
			// maxExternality, linkCnt, rnd);

			for (Boolean interpolate : new Boolean[] { false, true }) {

				final String prefix = "./testdata/hEART2015/";
				final String postfix = "_pop-" + populationSize + "_interpol-"
						+ interpolate + "_keepbest-" + keepBestSolution
						+ "_binsize-" + transitionBinSize + "_maxdeltabin-"
						+ maxDeltaBin + "_replanproba-" + replanningProbability
						+ "_linkcnt-" + linkCnt + ".csv";

				final PrintWriter objFctWriter = new PrintWriter(prefix
						+ "objfct" + postfix);
				final PrintWriter equilWeightWriter = new PrintWriter(prefix
						+ "equilweight" + postfix);
				final PrintWriter unifWeightWriter = new PrintWriter(prefix
						+ "unifweight" + postfix);
				final PrintWriter offsetWriter = new PrintWriter(prefix
						+ "offset" + postfix);

				for (int replication = 0; replication < replications; replication++) {

					final double deltaCostAtWhichSwitchingIsCertain = 5.0;
					final List<TwoRoutesReplanner> replanners = new ArrayList<TwoRoutesReplanner>(
							linkCnt);
					final boolean simulate = true;
					for (int i = 0; i < linkCnt; i++) {
						final TwoRoutesReplanner replanner;
						if (simulate) {
							replanner = new IndividualDecisionsRouteReplanner(
									MathHelpers.round(demand), true, rnd);
							((IndividualDecisionsRouteReplanner) replanner)
									.setFixedReplanningProbability(replanningProbability);
						} else {
							replanner = new ContinuousShiftToBestRouteReplanner(
									demand, deltaCostAtWhichSwitchingIsCertain);
						}
						replanners.add(replanner);
					}

					final NTimesTwoRoutesSimulator system = new NTimesTwoRoutesSimulator(
							replanners, capacity);

					// final DecisionVariableRandomizer randomization = new
					// MultiLevelTollRandomizer(
					// system, linkCnt, 0.1, 11, 1.0 / linkCnt,
					// maxDeltaBin, rnd);
					// final DecisionVariableRandomizer randomization = new
					// ContinuousTollRandomizer(
					// system, linkCnt, 0.1, 1.0, rnd);
					final DecisionVariableRandomizer randomization = new ContinuousDiscreteTollRandomizer(
							system, linkCnt, linkCnt, 0.1, 1.0, rnd);

					// final TrajectorySamplingSelfTuner selfTuner = new
					// TrajectorySamplingSelfTuner();
					final TrajectorySamplingSelfTuner selfTuner = new TrajectorySamplingSelfTuner(
							0.0, 0.0, 0.0, 0.95, 1.0);

					// final ObjectiveFunction objectiveFunction = new
					// NTimesTwoRoutesObjectiveFunction_exact(
					// externalities, capacity);
					final VectorBasedObjectiveFunction objectiveFunction = new NTimesTwoRoutesObjectiveFunction(
							externalities);

					final ConvergenceCriterion convergenceCriterion = new ObjectiveFunctionChangeConvergenceCriterion(
							1e-4, 1e-4, simulate ? 10 : 10);

					final int maxMemoryLength = Integer.MAX_VALUE; // TODO NEW
					final RandomSearch search = new RandomSearch(system,
							randomization, convergenceCriterion, selfTuner,
							maxIterations, maxTransitions, populationSize, rnd,
							interpolate, keepBestSolution, objectiveFunction,
							maxMemoryLength);

					search.run();

					// CREATE PLOTTABLE DATA

					final List<Integer> transitions = search
							.getTransitionEvalautionsView();
					final List<Double> cumulativeTransitions = new ArrayList<Double>(
							transitions.size());
					double sum = 0;
					for (Integer transition : transitions) {
						sum += transition;
						cumulativeTransitions.add(sum);
					}

					// >>>>> PS TRICKS DATA >>>>>

					if (interpolate) {
						transitionList.add(new ArrayList<Double>(
								cumulativeTransitions));
						objectiveFunctionValueList.add(new ArrayList<Double>(
								search.getBestObjectiveFunctionValuesView()));
						equilibriumWeightList.add(new ArrayList<Double>(search
								.getEquilibriumGapWeightsView()));
						uniformityWeightList.add(new ArrayList<Double>(search
								.getUniformityWeightsView()));
					} else {
						naiveTransitionList.add(new ArrayList<Double>(
								cumulativeTransitions));
						naiveObjectiveFunctionValueList
								.add(new ArrayList<Double>(search
										.getBestObjectiveFunctionValuesView()));
					}

					// <<<<< PS TRICKS DATA <<<<<

					write2file(cumulativeTransitions,
							search.getBestObjectiveFunctionValuesView(),
							transitionBinSize, transitionBinCnt, objFctWriter);

					if (interpolate) {
						write2file(cumulativeTransitions,
								search.getEquilibriumGapWeightsView(),
								transitionBinSize, transitionBinCnt,
								equilWeightWriter);
						write2file(cumulativeTransitions,
								search.getUniformityWeightsView(),
								transitionBinSize, transitionBinCnt,
								unifWeightWriter);
						write2file(cumulativeTransitions,
								search.getOffsetsView(), transitionBinSize,
								transitionBinCnt, offsetWriter);
					}
				}

				objFctWriter.flush();
				objFctWriter.close();

				equilWeightWriter.flush();
				equilWeightWriter.close();

				unifWeightWriter.flush();
				unifWeightWriter.close();

				offsetWriter.flush();
				offsetWriter.close();
			}

			// >>>>> DIAGRAM >>>>>

			final PSTricksDiagramWriter writer = new PSTricksDiagramWriter(8, 5);
			writer.setLabelX("transitions [$10^3$]");
			writer.setLabelY("Q [$10^3$]");
			writer.setYMin(20.0);
			writer.setYMax(50.0);
			writer.setYDelta(5.0);
			writer.setXMin(0.0);
			writer.setXMax(25.0);
			writer.setXDelta(5.0);

			for (int r = 0; r < replications; r++) {
				final String selectId = "select"
						+ "ABCDEFGHIJKLMNOPQRSTUVWXYZ".substring(r, r + 1);
				final String naiveId = "naive"
						+ "ABCDEFGHIJKLMNOPQRSTUVWXYZ".substring(r, r + 1);

				for (int i = 0; i < transitionList.get(r).size(); i++) {
					writer.add(selectId, transitionList.get(r).get(i) / 1000,
							objectiveFunctionValueList.get(r).get(i) / 1000);
				}
				for (int i = 0; i < naiveTransitionList.get(r).size(); i++) {
					writer.add(
							naiveId,
							naiveTransitionList.get(r).get(i) / 1000,
							naiveObjectiveFunctionValueList.get(r).get(i) / 1000);
				}
				writer.setPlotAttrs(selectId, "linecolor=red");
				writer.setPlotAttrs(naiveId, "linecolor=blue");
			}
			writer.addCommand("\\rput[rt](8,5){\\textcolor{blue}{naive}, \\textcolor{red}{select}}");

			writer.printAll(System.out);

			// <<<<< DIAGRAM <<<<<

		}

		// System.out.println("... DONE");
	}
}