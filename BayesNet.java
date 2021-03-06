package bayes;

import java.util.Arrays;
import java.util.Random;

/**
 * Simple class for approximate inference based on the Poker-game network.
 * James Luxton 1190809
 */
public class BayesNet {

	/**
	 * Inner class for representing a node in the network.
	 */
	public class Node {

		// The name of the node
		private String name;

		// The parent nodes
		private Node[] parents;

		// The probabilities for the CPT
		private double[] probs;

		// The current value of the node
		public boolean value;

		/**
		 * Initializes the node.
		 */
		private Node(String n, Node[] pa, double[] pr) {
			name = n;
			parents = pa;
			probs = pr;
		}


		private double assignedProbability() {

			int index = 0;

			if (parents.length == 0) {
				if (value == true) return probs[0];
				else return probs[1];
			}

			for (int i = 0; i < parents.length; i++) {
				if (parents[i].value == false) {
					index += Math.pow(2, parents.length - i - 1);
				}
			}

			double j = 1;

			if (this.value == false) return (j - probs[index]);
			else return probs[index];
		}

		/**
		 // Returns conditional probability of value "true" for the current node
		 // based on the values of the parent nodes.
		 // 
		 // @return The conditional probability of this node, given its parents.
		 **/
		private double conditionalProbability() {

			int index = 0;		

			for (int i = 0; i < parents.length; i++) {
				if (parents[i].value == false) {
					index += Math.pow(2, parents.length - i - 1);
				}
			}	

			return probs[index];
		}
	}

	// The list of nodes in the Bayes net
	private Node[] nodes;

	// A collection of examples describing whether Bot B is { cocky, bluffing
	// }
	public static final boolean[][] BBLUFF_EXAMPLES = { { true, true },
		{ true, true }, { true, true }, { true, false }, { true, true },
		{ false, false }, { false, false }, { false, true },
		{ false, false }, { false, false }, { false, false },
		{ false, false }, { false, true } };

	/**
	 * Constructor that sets up the Poker-game network.
	 */
	public BayesNet() {

		nodes = new Node[7];

		nodes[0] = new Node("B.Cocky", new Node[] {}, new double[] { 0.05 });
		nodes[1] = new Node("B.Bluff", new Node[] { nodes[0] },
				calculateBBluffProbabilities(BBLUFF_EXAMPLES));
		nodes[2] = new Node("A.Deals", new Node[] {},
				new double[] { 0.5 });
		nodes[3] = new Node("A.GoodHand", new Node[] { nodes[2] },
				new double[] { 0.75, 0.5 });
		nodes[4] = new Node("B.GoodHand", new Node[] { nodes[2] },
				new double[] { 0.4, 0.5 });
		nodes[5] = new Node("B.Bets", new Node[] { nodes[1], nodes[4] },
				new double[] { 0.95, 0.7, 0.9, 0.01 });
		nodes[6] = new Node("A.Wins", new Node[] { nodes[3], nodes[4] },
				new double[] { 0.45, 0.75, 0.25, 0.55 });
	}

	/**
	 * Prints the current state of the network to standard out.
	 */
	public void printState() {

		for (int i = 0; i < nodes.length; i++) {
			if (i > 0) {
				System.out.print(", ");
			}
			System.out.print(nodes[i].name + " = " + nodes[i].value);
		}
		System.out.println();
	}

	/**
	 * Calculates the probability that Bot B will bluff based on whether it is
	 * cocky or not.
	 * 
	 * @param bluffInstances
	 *            A set of training examples in the form { cocky, bluff } from
	 *            which to compute the probabilities.
	 * @return The probability that Bot B will bluff when it is { cocky, !cocky
	 *         }.
	 */
	public double[] calculateBBluffProbabilities(boolean[][] bluffInstances) {

		double[] probabilities = new double[2];

		// number of training data instances
		double n = bluffInstances.length;

		// Number of Cocky Bluffs
		double cockyB = 0;
		// Number of not Cocky Bluffs
		double notCockyB = 0;
		// Number of Instances where bot is cocky
		double cockyI = 0;
		// number of instances where bot is not cocky
		double nCockyI = 0;

		// iterate over given array and test first if bot is cocky or not then if it is bluffing
		// increment variables accordingly so we can find probabilities easily later
		for (int i = 0; i < n; i++) {
			if (bluffInstances[i][0] == true) { 
				cockyI++;
				if (bluffInstances[i][1] == true) cockyB++;
			}
			if (bluffInstances[i][0] == false) {
				nCockyI++;
				if (bluffInstances[i][1] == true)
					notCockyB++; 
			}
		}

		probabilities[0] = cockyB / cockyI;
		probabilities[1] = notCockyB / nCockyI;

		return probabilities;
	}

	/**
	 * This method calculates the exact probability of a given event occurring,
	 * where all variables are assigned a given evidence value.
	 *
	 * @param evidenceValues
	 *            The values of all nodes.
	 * @return -1 if the evidence does not cover every node in the network.
	 *         Otherwise a probability between 0 and 1.
	 */
	public double calculateExactEventProbability(boolean[] evidenceValues) {
		// Only performs exact calculation for all evidence known.
		if (evidenceValues.length != nodes.length)
			return -1;

		for (int i = 0; i < evidenceValues.length; i++) {
			nodes[i].value = evidenceValues[i];			
		}

		double res = 1.0;

		for (Node n : nodes) {
			// System.err.println(n.name + " " + n.value + " " + n.assignedProbability());
			res *= n.assignedProbability();
		}		
		return res;
	}

	/**
	 * This method assigns new values to the nodes in the network by sampling
	 * from the joint distribution (based on PRIOR-SAMPLE method from text
	 * book/slides).
	 */
	public void priorSample() {
		// use this to generate a double between 0.0 and 1.0
		Random ran = new Random();

		for (Node n : nodes) {
			n.value = false;
			if (ran.nextDouble() < n.conditionalProbability()) n.value = true;
		}
	}

	/**
	 * Rejection sampling. Returns probability of query variable being true
	 * given the values of the evidence variables, estimated based on the given
	 * total number of samples (see REJECTION-SAMPLING method from text
	 * book/slides).
	 * 
	 * The nodes/variables are specified by their indices in the nodes array.
	 * The array evidenceValues has one value for each index in
	 * indicesOfEvidenceNodes. See also examples in main().
	 * 
	 * @param queryNode
	 *            The variable for which rejection sampling is calculating.
	 * @param indicesOfEvidenceNodes
	 *            The indices of the evidence nodes.
	 * @param evidenceValues
	 *            The values of the indexed evidence nodes.
	 * @param N
	 *            The number of iterations to perform rejection sampling.
	 * @return The probability that the query variable is true given the
	 *         evidence.
	 */
	public double rejectionSampling(int queryNode,
			int[] indicesOfEvidenceNodes, boolean[] evidenceValues, int N) {

		double True = 0;
		double False = 0;
		boolean Rejected = true;

		for (int j = 1; j <= N; j++){
			priorSample();
			Rejected = false;

			for (int i = 0; i < indicesOfEvidenceNodes.length; i++){
				if (!nodes[indicesOfEvidenceNodes[i]].value == evidenceValues[i]) Rejected = true;
			}

			if (!Rejected) {
				if (nodes[queryNode].value == true) True++;
				else False++;
			}			
		}

		double res = True / (True + False);
		// System.err.println("True: " + True + " False: " + False + " P(T): " + True / (True + False));

		return res; 
	}

	/**
	 * This method assigns new values to the non-evidence nodes in the network
	 * and computes a weight based on the evidence nodes (based on
	 * WEIGHTED-SAMPLE method from text book/slides).
	 * 
	 * The evidence is specified as in the case of rejectionSampling().
	 * 
	 * @param indicesOfEvidenceNodes
	 *            The indices of the evidence nodes.
	 * @param evidenceValues
	 *            The values of the indexed evidence nodes.
	 * @return The weight of the event occurring.
	 * 
	 */
	public double weightedSample(int[] indicesOfEvidenceNodes,
			boolean[] evidenceValues) {

		double w = 1.0;
		int i = 0;
		Random r = new Random();	

		for(int j = 0; j < nodes.length; j++) {
			// Find a value at position index inside the evidence node list.
			i = Arrays.binarySearch(indicesOfEvidenceNodes, j);
			if(i >= 0) {
				nodes[j].value = evidenceValues[i];
				w *= nodes[j].assignedProbability();
			}
			else {
				nodes[j].value = false;
				if (r.nextDouble() < nodes[j].conditionalProbability()) nodes[j].value = true;
			}
		}
		return w;
	}

	/**
	 * Likelihood weighting. Returns probability of query variable being true
	 * given the values of the evidence variables, estimated based on the given
	 * total number of samples (see LIKELIHOOD-WEIGHTING method from text
	 * book/slides).
	 * 
	 * The parameters are the same as in the case of rejectionSampling().
	 * 
	 * @param queryNode
	 *            The variable for which rejection sampling is calculating.
	 * @param indicesOfEvidenceNodes
	 *            The indices of the evidence nodes.
	 * @param evidenceValues
	 *            The values of the indexed evidence nodes.
	 * @param N
	 *            The number of iterations to perform rejection sampling.
	 * @return The probability that the query variable is true given the
	 *         evidence.
	 */
	public double likelihoodWeighting(int queryNode,
			int[] indicesOfEvidenceNodes, boolean[] evidenceValues, int N) {

		double True = 0;
		double False = 0;
		double Weight = 1.0;

		for (int i = 1; i <= N; i++) {
			Weight = weightedSample(indicesOfEvidenceNodes, evidenceValues);
			if (nodes[queryNode].value == true) { True += Weight; }
			else  { False += Weight; }
		}

		return (True / (True + False));
	}

	/**
	 * MCMC inference. Returns probability of query variable being true given
	 * the values of the evidence variables, estimated based on the given total
	 * number of samples (see MCMC-ASK method from text book/slides).
	 * 
	 * The parameters are the same as in the case of rejectionSampling().
	 * 
	 * @param queryNode
	 *            The variable for which rejection sampling is calculating.
	 * @param indicesOfEvidenceNodes
	 *            The indices of the evidence nodes.
	 * @param evidenceValues
	 *            The values of the indexed evidence nodes.
	 * @param N
	 *            The number of iterations to perform rejection sampling.
	 * @return The probability that the query variable is true given the
	 *         evidence.
	 */
	public double MCMCask(int queryNode, int[] indicesOfEvidenceNodes,
			boolean[] evidenceValues, int N) {
		
		double True = 0;
		double False = 0;
		int index = 0;
		
		// initialize nodes with random values for the variables in !EvidenceNodes
		priorSample();
		for (int i : indicesOfEvidenceNodes) {
			nodes[i].value = evidenceValues[index];
			index++;
		}		
		
		for (int j = 1; j <= N; j++) {		
			
			MCMCsample(indicesOfEvidenceNodes, evidenceValues);
			
			if (nodes[queryNode].value == true) { True++; }
			else { False++; }
		}
		
		return True / (True + False);
	}
	
	public void MCMCsample(int[] indicesOfEvidenceNodes, boolean[] evidenceValues){
		
		Random ran = new Random();
		int j = 0;
		
		for (int i = 0; i < nodes.length; i++) {
			j = Arrays.binarySearch(indicesOfEvidenceNodes, i);
			if(j >= 0) {
				nodes[i].value = evidenceValues[j];
			}
			else {
				nodes[i].value = false;
				if (ran.nextDouble() < MCMCblanket(nodes[i])) nodes[i].value = true;
			} 
		}
	}
	
	public double MCMCblanket(Node n) {
		
		double True = n.conditionalProbability();
		double False =  1 - n.conditionalProbability();
		
		for (int i = 0; i < nodes.length; i++) {
			for (Node p : nodes[i].parents){
				if (p.name.equals(n.name)) {
					n.value = true;
					if (nodes[i].value) True *= nodes[i].conditionalProbability();
					else True *= (1 - nodes[i].conditionalProbability());
					
					n.value = false;
					if (nodes[i].value) False *= nodes[i].conditionalProbability();
					else False *= (1 - nodes[i].conditionalProbability());
				}
			}
		}		
		return True / (True + False);
	}

	/**
	 * The main method, with some example method calls.
	 */
	public static void main(String[] ops) {

		// Create network.
		BayesNet b = new BayesNet();

		double[] bluffProbabilities = b
				.calculateBBluffProbabilities(BBLUFF_EXAMPLES);
		System.out.println("When Bot B is cocky, it bluffs "
				+ (bluffProbabilities[0] * 100) + "% of the time.");
		System.out.println("When Bot B is not cocky, it bluffs "
				+ (bluffProbabilities[1] * 100) + "% of the time.");

		System.out.println();
		double bluffWinProb = b.calculateExactEventProbability(new boolean[] {
				true, true, true, false, false, true, false });
		System.out
		.println("The probability of Bot B winning on a cocky bluff "
				+ "(with bet) and both bots have bad hands (A dealt) is: "
				+ bluffWinProb);

		System.out.println();
		// Sample five states from joint distribution and print them
		int GeneratedStates = 1; // change this to display more prior samples
		for (int i = 0; i < GeneratedStates; i++) {
			System.out.println("Prior Samples");
			b.priorSample();
			b.printState();
		}

		// Print out results of some example queries based on rejection
		// sampling.
		// Same should be possible with likelihood weighting and MCMC inference.
		System.out.println('\n' + "Running all tests 1000000 times with the same arguments in respect to index's of results" + '\n');
		System.out.println("Rejection Sampling");
		// Probability of B.GoodHand given bet and A not win.
		System.out.println(b.rejectionSampling(4, new int[] { 5, 6 },
				new boolean[] { true, false }, 1000000));
		// Probability of betting given a cocky
		System.out.println(b.rejectionSampling(1, new int[] { 0 },
				new boolean[] { true }, 1000000));
		// Probability of B.Goodhand given B.Bluff and A.Deal
		System.out.println(b.rejectionSampling(4, new int[] { 1, 2 },
				new boolean[] { true, true }, 1000000));

		System.out.println();
		System.out.println("Likelihood Weighting");
		// Probability of B.GoodHand given bet and A not win.
		System.out.println(b.likelihoodWeighting(4, new int[] { 5, 6 },
				new boolean[] { true, false }, 1000000));
		// Probability of betting given a cocky
		System.out.println(b.likelihoodWeighting(1, new int[] { 0 },
				new boolean[] { true }, 1000000));
		// Probability of B.Goodhand given B.Bluff and A.Deal
		System.out.println(b.likelihoodWeighting(4, new int[] { 1, 2 },
				new boolean[] { true, true }, 1000000));

		System.out.println();
		System.out.println("Markov Chain Monte Carlo");
		// Probability of B.GoodHand given bet and A not win.
		System.out.println(b.MCMCask(4, new int[] { 5, 6 },
				new boolean[] { true, false }, 1000000));
		// Probability of betting given a cocky
		System.out.println(b.MCMCask(1, new int[] { 0 },
				new boolean[] { true }, 1000000));
		// Probability of B.Goodhand given B.Bluff and A.Deal
		System.out.println(b.MCMCask(4, new int[] { 1, 2 },
				new boolean[] { true, true }, 1000000));
	}
}