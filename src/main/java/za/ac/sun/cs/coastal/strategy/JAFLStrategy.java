package za.ac.sun.cs.coastal.strategy;

import java.io.PrintWriter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import za.ac.sun.cs.coastal.Configuration;
import za.ac.sun.cs.coastal.listener.ConfigurationListener;
import za.ac.sun.cs.coastal.symbolic.SegmentedPC;
import za.ac.sun.cs.coastal.symbolic.SegmentedPCIf;
import za.ac.sun.cs.coastal.symbolic.SymbolicState;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;

public class JAFLStrategy implements Strategy, ConfigurationListener {

	private Logger log;

	private Green green;

	private final Set<String> visitedModels = new HashSet<>();

	private int infeasibleCount = 0;

	private RandomPathTree pathTree;

	private long randomSeed = 987654321;

	private long pathLimit = 0;

	private long totalTime = 0, solverTime = 0, pathTreeTime = 0, modelExtractionTime = 0;

	private static ArrayList<Byte[]> coastalInputs = new ArrayList<Byte[]>();

	public JAFLStrategy() {
		// We expect configurationLoaded(...) to be called shortly.
		// This will initialize this instance.
		coastalInputs = new ArrayList<Byte[]>();
	}

	@Override
	public void configurationLoaded(Configuration configuration) {
		log = configuration.getLog();
		configuration.getReporterManager().register(this);
		pathTree = new RandomPathTree(configuration);
		randomSeed = configuration.getLongProperty("coastal.randomStrategy.seed", randomSeed);
		pathTree.setSeed(randomSeed);
		pathLimit = configuration.getLimitPaths();
		if (pathLimit == 0) {
			pathLimit = Long.MIN_VALUE;
		}
		// Set up green
		green = new Green("COASTAL", LogManager.getLogger("GREEN"));
		Properties greenProperties = configuration.getOriginalProperties();
		greenProperties.setProperty("green.log.level", "ALL");
		greenProperties.setProperty("green.services", "model");
		greenProperties.setProperty("green.service.model", "(bounder modeller)");
		greenProperties.setProperty("green.service.model.bounder", "za.ac.sun.cs.green.service.bounder.BounderService");
		greenProperties.setProperty("green.service.model.modeller", "za.ac.sun.cs.green.service.z3.ModelZ3Service");
		new za.ac.sun.cs.green.util.Configuration(green, greenProperties).configure();
	}

	@Override
	public void collectProperties(Properties properties) {
		properties.setProperty("coastal.randomStrategy.seed", Long.toString(randomSeed));
	}

	@Override
	public Map<String, Constant> refine(SymbolicState symbolicState) {
		long t0 = System.currentTimeMillis();
		System.out.println("BEGIN REFINEMENT.....");
		List<Map<String, Constant>> refinement = refine0(symbolicState);
		totalTime += System.currentTimeMillis() - t0;
		// return refinement;
		// System.out.println("Printing input options: " + refinement.size());
		for (Map<String, Constant> entry : refinement) {
			TreeMap<String, Constant> sortedMap = new TreeMap<String, Constant>((k1, k2) -> {
				final int k1_id = Integer.parseInt(k1.substring(k1.lastIndexOf('_') + 1));
				final int k2_id = Integer.parseInt(k2.substring(k2.lastIndexOf('_') + 1));
				return k1_id - k2_id;
			});
			sortedMap.putAll(entry);
			// System.out.println("Input -> ");
			ArrayList<Byte> baos = new ArrayList<Byte>();
			for (Map.Entry<String, Constant> e : sortedMap.entrySet()) {
				// System.out.println(e.getKey() + " -> " + e.getValue());
				baos.add((byte) Integer.parseInt(e.getValue().toString()));
			}
			coastalInputs.add(baos.toArray(new Byte[baos.size()]));
		}
		return null;
	}

	public static ArrayList<Byte[]> getCoastalInputs() {
		return coastalInputs;
	}

	private List<Map<String, Constant>> refine10(SymbolicState symbolicState) {
		List<Map<String, Constant>> list = new LinkedList<Map<String, Constant>>();
		long t;
		SegmentedPC spc = symbolicState.getSegmentedPathCondition();
		log.info("explored <{}> {}", spc.getSignature(), spc.getPathCondition().toString());
		// return null;
		boolean infeasible = false;
		while (true) {
			if (--pathLimit < 0) {
				log.warn("path limit reached");
				return list;
			}
			t = System.currentTimeMillis();
			spc = pathTree.insertPath(spc, infeasible);
			pathTreeTime += System.currentTimeMillis() - t;
			if (spc == null) {
				log.info("no further paths");
				log.trace("Tree shape: {}", pathTree.getShape());
				return list;
			}
			infeasible = false;
			Expression pc = spc.getPathCondition();
			String sig = spc.getSignature();
			log.info("trying   <{}> {}", sig, pc.toString());
			Instance instance = new Instance(green, null, pc);
			t = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Map<IntVariable, IntConstant> model = (Map<IntVariable, IntConstant>) instance.request("model");
			solverTime += System.currentTimeMillis() - t;
			if (model == null) {
				log.info("no model");
				log.trace("(The spc is {})", spc.getPathCondition().toString());
				infeasible = true;
				infeasibleCount++;
			} else {
				t = System.currentTimeMillis();
				Map<String, Constant> newModel = new HashMap<>();
				for (IntVariable variable : model.keySet()) {
					String name = variable.getName();
					if (name.startsWith(SymbolicState.NEW_VAR_PREFIX)) {
						continue;
					}
					Constant value = model.get(variable);
					newModel.put(name, value);
				}
				String modelString = newModel.toString();
				modelExtractionTime += System.currentTimeMillis() - t;
				log.info("new model: {}", modelString);

				if (visitedModels.add(modelString)) {
					// return newModel;
					list.add(newModel);
				} else {
					log.info("model {} has been visited before, retrying", modelString);
				}

			}
		}
	}

	private List<Map<String, Constant>> refine0(SymbolicState symbolicState) {
		List<Map<String, Constant>> list = new LinkedList<Map<String, Constant>>();
		long t;
		SegmentedPC spc = symbolicState.getSegmentedPathCondition();
		log.info("explored <{}> {}", spc.getSignature(), spc.getPathCondition().toString());
		// generate new segmented pcs, all with one conjunct negated:
		Set<SegmentedPC> altSpcs = new HashSet<>();
		for (SegmentedPC pointer = spc; pointer != null; pointer = pointer.getParent()) {
			altSpcs.add(generateAltSpc(spc, pointer));
		}
		// return null;
		boolean infeasible = false;
		for (SegmentedPC thePC : altSpcs) {
			Expression pc = thePC.getPathCondition();
			String sig = spc.getSignature();
			log.info("trying   <{}> {}", sig, pc.toString());
			Instance instance = new Instance(green, null, pc);
			Map<IntVariable, IntConstant> model = (Map<IntVariable, IntConstant>) instance.request("model");
			if (model == null) {
				log.info("no model");
				log.trace("(The spc is {})", spc.getPathCondition().toString());
			} else {
				Map<String, Constant> newModel = new HashMap<>();
				for (IntVariable variable : model.keySet()) {
					String name = variable.getName();
					if (name.startsWith(SymbolicState.NEW_VAR_PREFIX)) {
						continue;
					}
					Constant value = model.get(variable);
					newModel.put(name, value);
				}
				String modelString = newModel.toString();
				log.info("new model: {}", modelString);
				if (visitedModels.add(modelString)) {
					list.add(newModel);
				} else {
					log.info("model {} has been visited before, retrying", modelString);
				}
			}
		}
		return list;
	}

	private SegmentedPC generateAltSpc(SegmentedPC spc, SegmentedPC pointer) {
		SegmentedPC parent = null;
		boolean value = ((SegmentedPCIf) spc).getValue();
		if (spc == pointer) {
			parent = spc.getParent();
			value = !value;
		} else {
			parent = generateAltSpc(spc.getParent(), pointer);
		}
		return new SegmentedPCIf(parent, spc.getExpression(), spc.getPassiveConjunct(), value);
	}

	// ======================================================================
	//
	// RandomPathTree
	//
	// ======================================================================

	private static class RandomPathTree extends PathTree {

		private final Random rng = new Random();

		RandomPathTree(Configuration configuration) {
			super(configuration);
		}

		public void setSeed(long seed) {
			rng.setSeed(seed);
		}

		@Override
		public SegmentedPC findNewPath() {
			SegmentedPC pc = null;
			PathTreeNode cur = getRoot();
			outer: while (true) {
				int n = cur.getChildCount();
				int i = rng.nextInt(n);
				for (int j = 0; j < n; j++, i = (i + 1) % n) {
					PathTreeNode ch = cur.getChild(i);
					if ((ch != null) && !ch.isComplete()) {
						pc = cur.getPcForChild(i, pc);
						cur = ch;
						continue outer;
					}
				}
				for (int j = 0; j < n; j++, i = (i + 1) % n) {
					PathTreeNode ch = cur.getChild(i);
					if (ch == null) {
						return cur.getPcForChild(i, pc);
					}
				}
				return null;
			}
		}

	}

	// ======================================================================
	//
	// REPORTING
	//
	// ======================================================================

	@Override
	public String getName() {
		return "JAFLStrategy";
	}

	@Override
	public void report(PrintWriter info, PrintWriter trace) {
		info.println("  Inserted paths: " + pathTree.getPathCount());
		info.println("  Revisited paths: " + pathTree.getRevisitCount());
		info.println("  Infeasible paths: " + infeasibleCount);
		info.println("  Solver time: " + solverTime);
		info.println("  Path tree time: " + pathTreeTime);
		info.println("  Model extraction time: " + modelExtractionTime);
		info.println("  Overall strategy time: " + totalTime);
	}

}
