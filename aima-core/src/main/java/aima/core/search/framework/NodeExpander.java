package aima.core.search.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import aima.core.agent.Action;
import aima.core.search.framework.problem.ActionsFunction;
import aima.core.search.framework.problem.Problem;
import aima.core.search.framework.problem.ResultFunction;
import aima.core.search.framework.problem.StepCostFunction;

/**
 * Instances of this class are responsible for node creation and expansion. They
 * compute path costs, support progress tracing, and count the number of
 * {@link #expand(Node, Problem)} calls.
 * 
 * @author Ruediger Lunde
 *
 */
public class NodeExpander {

	protected boolean useParentLinks = true;

	/**
	 * Modifies {@link #useParentLinks} and returns this node expander. When
	 * using local search to search for states, parent links are not needed and
	 * lead to unnecessary memory consumption.
	 * 
	 * @return
	 */
	public NodeExpander useParentLinks(boolean state) {
		useParentLinks = state;
		return this;
	}

	///////////////////////////////////////////////////////////////////////
	// expanding nodes

	/**
	 * Factory method, which creates a root node for the specified state.
	 */
	public Node createRootNode(Object state) {
		return new Node(state);
	}

	/**
	 * Computes the path cost for getting from the root node state via the
	 * parent node state to the specified state, creates a new node for the
	 * specified state, adds it as child of the provided parent (if
	 * {@link #useParentLinks} is true), and returns it.
	 */
	public Node createNode(Object state, Node parent, Action action, double stepCost) {
		Node p = useParentLinks ? parent : null;
		return new Node(state, p, action, parent.getPathCost() + stepCost);
	}

	/**
	 * Returns the children obtained from expanding the specified node in the
	 * specified problem.
	 * 
	 * @param node
	 *            the node to expand
	 * @param problem
	 *            the problem the specified node is within.
	 * 
	 * @return the children obtained from expanding the specified node in the
	 *         specified problem.
	 */
	public List<Node> expand(Node node, Problem problem) {
		List<Node> successors = new ArrayList<>();

		ActionsFunction actionsFunction = problem.getActionsFunction();
		ResultFunction resultFunction = problem.getResultFunction();
		StepCostFunction stepCostFunction = problem.getStepCostFunction();

		for (Action action : actionsFunction.actions(node.getState())) {
			Object successorState = resultFunction.result(node.getState(), action);

			double stepCost = stepCostFunction.c(node.getState(), action, successorState);
			successors.add(createNode(successorState, node, action, stepCost));
		}
		notifyNodeListeners(node);
		return successors;
	}

	///////////////////////////////////////////////////////////////////////
	// progress tracing

	/**
	 * All node listeners added to this list get informed whenever a node is
	 * expanded.
	 */
	private List<Consumer<Node>> nodeListeners = new ArrayList<>();

	/**
	 * Adds a listener to the list of node listeners. It is informed whenever a
	 * node is expanded during search.
	 */
	public void addNodeListener(Consumer<Node> listener) {
		nodeListeners.add(listener);
	}

	/**
	 * Removes a listener from the list of node listeners.
	 */
	public boolean removeNodeListener(Consumer<Node> listener) {
		return nodeListeners.remove(listener);
	}

	protected void notifyNodeListeners(Node node) {
		for (Consumer<Node> listener : nodeListeners)
			listener.accept(node);
	}
}
