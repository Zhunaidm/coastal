package za.ac.sun.cs.coastal.strategy;

import org.apache.logging.log4j.Logger;

import za.ac.sun.cs.coastal.Configuration;
import za.ac.sun.cs.coastal.symbolic.SegmentedPC;

public abstract class PathTree {

	protected final Logger log;

	private final boolean drawPaths; 

	private PathTreeNode root = null;

	private int pathCount = 0;

	private int revisitCount = 0;

	public PathTree(Configuration configuration) {
		log = configuration.getLog();
		drawPaths = configuration.getDrawPaths();
	}

	public PathTreeNode getRoot() {
		return root;
	}

	public int getPathCount() {
		return pathCount;
	}

	public int getRevisitCount() {
		return revisitCount;
	}

	public SegmentedPC insertPath(SegmentedPC spc, boolean isInfeasible) {
		pathCount++;
		/*
		 * Step 1: Deconstruct the path condition.
		 */
		final int depth = spc.getDepth();
		SegmentedPC[] path = new SegmentedPC[depth];
		int idx = depth - 1;
		for (SegmentedPC s = spc; s != null; s = s.getParent()) {
			path[idx--] = s;
		}
		assert idx == -1;
		log.trace("::: depth:{}", depth);
		/*
		 * Step 2: Add the new path (spc) to the path tree
		 */
		root = insert(root, path, 0, depth, isInfeasible);
		/*
		 * Step 3: Dump the tree if required
		 */
		if (drawPaths && (root != null)) {
			log.trace(":::");
			for (String ll : stringRepr()) {
				log.trace("::: {}", ll);
			}
		}
		/*
		 * Step 4: Return a new path through the tree
		 */
		return findNewPath();
	}

	private String getId(PathTreeNode node) {
		if (node == null) {
			return "NUL";
		} else {
			return "#" + node.getId();
		}
	}

	private PathTreeNode insert(PathTreeNode node, SegmentedPC[] path, int cur, int depth, boolean isInfeasible) {
		log.trace("::: insert(node:{}, conjunct:{}, cur/depth:{}/{})", getId(node), path[cur].getActiveConjunct(), cur, depth);
		/*
		 * Step 1: create node if necessary
		 */
		PathTreeNode nd = node;
		if (node == null) {
			node = PathTreeNode.createNode(path[cur], path[cur].getNrOfOutcomes());
			nd = node;
			log.trace("::: create missing node {}", getId(nd));
		} else {
			if (!nd.getPc().getExpression().equals(path[cur].getExpression())) {
				log.trace("::: {} conjunct disagreement", getId(nd));
			}
		}
		/*
		 * Step 2: find the child and check that it checks out
		 */
		int i = path[cur].getOutcomeIndex();
		/*
		 * Step 3: insert the rest of the path
		 */
		if (cur + 1 < depth) {
			nd.setChild(i, insert(nd.getChild(i), path, cur + 1, depth, isInfeasible));
		} else if (isInfeasible) {
			log.trace("::: create infeasible node");
			nd.setChild(i, PathTreeNode.createInfeasible());
		} else {
			log.trace("::: create leaf");
			nd.setChild(i, PathTreeNode.createLeaf());
		}
		/*
		 * Step 4: check if this node is now fully explored
		 */
		if (!node.isFullyExplored()) {
			int n = node.getChildCount();
			boolean full = true;
			for (i = 0; i < n; i++) {
				PathTreeNode x = node.getChild(i);
				if ((x == null) || !x.isComplete()) {
					full = false;
					break;
				}
			}
			if (full) {
				log.trace("::: setting {} as fully explored", getId(node));
				node.setFullyExplored();
			}
		}
		/*
		 * Step 5: return this node
		 */
		return node;
	}

	public abstract SegmentedPC findNewPath();

	public String[] stringRepr() {
		int h = root.height() * 4 - 2;
		int w = root.width();
		/*
		 * Step 1: create and clear the character array
		 */
		char[][] lines = new char[h][w];
		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				lines[i][j] = ' ';
			}
		}
		/*
		 * Step 2: draw the path tree
		 */
		root.stringFill(lines, 0, 0);
		/*
		 * Step 3: convert the character array to strings
		 */
		String[] finalLines = new String[h];
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < h; i++) {
			b.setLength(0);
			int k = w - 1;
			while ((k >= 0) && (lines[i][k] == ' ')) {
				k--;
			}
			for (int j = 0; j <= k; j++) {
				b.append(lines[i][j]);
			}
			finalLines[i] = b.toString();
		}
		return finalLines;
	}

	public String getShape() {
		return (root == null) ? "0" : root.getShape();
	}

}
