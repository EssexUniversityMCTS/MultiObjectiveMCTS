package controllers.mctsdriver;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import framework.core.Game;
import framework.utils.Vector2d;

public class MctsDriverController extends DriverController
{
	private class TreeNode
	{
		private TreeNode m_parent;
		private List<TreeNode> m_children;
		private MetaAction m_incomingAction;
		
		private int m_visits;
		private double m_cumulativeReward;
		
		public TreeNode(TreeNode parent, MetaAction incomingAction)
		{
			m_parent = parent;
			m_children = null;
			m_incomingAction = incomingAction;
			
			m_visits = 0;
			m_cumulativeReward = 0;
		}
		
		public TreeNode GetParent()
		{
			return m_parent;
		}
		
		public void SetChildren(List<MetaAction> childActions)
		{
			
			m_children = new ArrayList<TreeNode>();
			for (MetaAction a : childActions)
				m_children.add(new TreeNode(this,a));
		}
		
		public List<TreeNode> GetChildren()
		{
			return m_children;
		}
		
		public MetaAction GetIncomingMetaAction()
		{
			return m_incomingAction;
		}
		
		public void UpdateNode(double reward)
		{
			m_visits++;
			m_cumulativeReward += reward;
		}
		
		public void UpdateNode(double reward, double weight)
		{
			m_visits += weight;
			m_cumulativeReward += weight*reward;
		}
		
		public int GetVisits()
		{
			return m_visits;
		}
		
		public double GetAverageReward()
		{
			return m_cumulativeReward / m_visits;
		}
		
		public double GetExplorationUrgency()
		{
			return m_par.MCTS_EXPLORATION * Math.sqrt(Math.log(m_parent.GetVisits())/this.GetVisits());
		}
		
		public boolean IsPruned()
		{
			return m_cumulativeReward == Double.NEGATIVE_INFINITY;
		}

		public void prune()
		{
			m_cumulativeReward = Double.NEGATIVE_INFINITY;
			m_visits++;
			
			if (m_parent != null)
			{
				boolean allSiblingsArePruned = true;
				for (TreeNode sibling : m_parent.m_children)
				{
					if (!sibling.IsPruned())
					{
						allSiblingsArePruned = false;
						break;
					}
				}
				
				if (allSiblingsArePruned)
					m_parent.prune();
			}
		}
	}
	
	private TreeNode m_rootNode;
	
	/** Whether to write a score.csv file with evaluations for the visited states.
	 *  DON'T FORGET TO SET THIS TO FALSE BEFORE UPLOADING THE PLAYER!!! */
	private final boolean WRITE_SCORE_CSV_FILE = false;

	private BufferedWriter m_scoreWriter;
	
	private int m_decisionsInPanicMode = 0;
	private int m_totalDecisions = 0;

	public MctsDriverController(Game a_game, long a_timeDue)
	{
		super(a_game, a_timeDue, null);

		if (WRITE_SCORE_CSV_FILE)
		{
			try
			{
				FileWriter fstream = new FileWriter("score.csv");
				m_scoreWriter = new BufferedWriter(fstream);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public MctsDriverController(Game a_game, long a_timeDue, Parameters parameters)
	{
		super(a_game, a_timeDue, parameters);
	}

	@Override
	protected void resetSearch(Game newRootState) 
	{
		super.resetSearch(newRootState);
		m_rootNode = new TreeNode(null,null);
		
		if (m_par == m_panicPar)
			leavePanicMode();
		
		if (newRootState.isEnded() || newRootState.getWaypointsLeft() == 0)
			System.out.printf("Decisions in panic mode: %d/%d\n", m_decisionsInPanicMode, m_totalDecisions);
	}
	
	@Override
	protected boolean isSearchTreeEmpty()
	{
		return m_rootNode.GetChildren() == null;
	}
	
	int m_trials = 0;
	int m_bestTrials = 0;
	
	@Override
	protected MetaAction getBestMetaAction()
	{
		if (m_par == m_panicPar)
			m_decisionsInPanicMode++;
		
		m_totalDecisions++;
		
		TreeNode bestChild = null;
		int mostVisits = Integer.MIN_VALUE;
		
		for (TreeNode child : m_rootNode.GetChildren())
		{
			if (m_par.CONSOLE_OUTPUT) System.out.printf("%s: %d/%d visits, reward %f\n",
					child.m_incomingAction.toString(), child.m_visits, m_rootNode.m_visits, child.m_cumulativeReward / child.m_visits);
			
			if (child.GetVisits() > mostVisits)
			{
				bestChild = child;
				mostVisits = child.GetVisits();
			}
		}
		
		if (m_par.CONSOLE_OUTPUT) System.out.println();
		
		m_bestTrials = bestChild.GetVisits();
		m_trials = m_rootNode.GetVisits();
		
		if (m_scoreWriter != null)
		{
			try {
				Parameters oldParams = m_evaluator.m_par;
				m_evaluator.m_par = m_normalPar;
				double score = m_evaluator.evaluate(m_rootGameState, null, m_rootGameState, m_routePlanner);
				m_evaluator.m_par = oldParams;
				
				m_scoreWriter.write(String.format("%d,%f",
						m_rootGameState.getWaypointsVisited(),
						score
				));
				
				m_scoreWriter.write(String.format(",%f", bestChild.m_cumulativeReward / bestChild.m_visits));
				
				if (m_par == m_panicPar)
					m_scoreWriter.write(String.format(",%f", m_evaluator.evaluate(m_rootGameState, null, m_rootGameState, m_routePlanner)));
				
				/*if (m_rootNode != null && m_rootNode.GetChildren() != null)
					for (TreeNode node : m_rootNode.GetChildren())
					{
						m_scoreWriter.write(String.format(",%f", node.m_cumulativeReward / node.m_visits));
					}*/
				m_scoreWriter.write("\n");
				
				m_scoreWriter.flush();
			} catch (IOException e) { e.printStackTrace(); }
		}
		
		return bestChild.GetIncomingMetaAction();
	}

	@Override
	protected void doSearch(long timeDue)
	{
		if (m_par != m_panicPar && getPositionInCurrentMetaAction() > m_par.PANIC_MODE_DECISION_TIME_PORTION)
		{
			if (GetSimulationResult(m_rootGameState, null) > m_rootNode.m_cumulativeReward / m_rootNode.m_visits + m_par.PANIC_MODE_LOCAL_MAXIMUM_THRESHOLD)
			{
				enterPanicMode();
				m_rootNode = new TreeNode(null,null);
			}
		}
		
		long averageIterationMillis = 0;
		int iterations = 0;
		
		while (System.currentTimeMillis() + averageIterationMillis < timeDue)
		{
			long iterationStartMillis = System.currentTimeMillis();
			
			Game currentState = m_rootGameState.getCopy();
			ExtraPlayoutInfo extraInfo = new ExtraPlayoutInfo();
			TreeNode currentNode = m_rootNode;
			int currentDepth = 0;
			
			boolean prune = false;
			
			while (!prune && currentNode.GetChildren() != null && currentDepth < m_par.MCTS_PLAYOUT_LIMIT && currentDepth < m_par.MCTS_EXPANSION_LIMIT)
			{
				currentNode = DoSelection(currentNode);
				assert(!currentNode.IsPruned());
				ApplyMetaAction(currentState, currentNode.GetIncomingMetaAction(), extraInfo);
				currentDepth += 1;
				
				if (m_par.PRUNE_STALLING_MOVES && extraInfo.m_lastMoveWasStalling)
					prune = true;
			}
			
			if (!prune && !currentState.isEnded() && currentDepth < m_par.MCTS_PLAYOUT_LIMIT && currentDepth < m_par.MCTS_EXPANSION_LIMIT)
			{
				TreeNode newNode = DoExpansion(currentNode);
				if (newNode != null)
				{
					currentNode = newNode;
					ApplyMetaAction(currentState, currentNode.GetIncomingMetaAction(), extraInfo);
					currentDepth += 1;
				}
				
				if (m_par.PRUNE_STALLING_MOVES && extraInfo.m_lastMoveWasStalling)
					prune = true;
			}
			
			if (prune)
			{
				currentNode.prune();
				// Don't simulate or backpropagate
				
				if (m_rootNode.IsPruned())
				{
					if (m_par.CONSOLE_OUTPUT) System.out.println("Uh oh, all moves were pruned");
					m_par.PRUNE_STALLING_MOVES = false;
					resetSearch(m_rootGameState);
				}
			}
			else
			{
				double simulationResult = DoSimulation(currentState, currentDepth, extraInfo);
				
				//double weight = (currentDepth + 1 / (double)MCTS_SIMULATION_LIMIT);
				DoBackPropagation(currentNode, simulationResult);
			}
			
			long timeTaken = System.currentTimeMillis() - iterationStartMillis;
			if (timeTaken > averageIterationMillis)
				averageIterationMillis = timeTaken;
			//averageIterationMillis += ((1.0 / (iterations + 1)) * (System.currentTimeMillis() - iterationStartMillis)) + ((iterations/(iterations + 1)* averageIterationMillis));
			iterations++;
		}
	}
	
	private TreeNode DoSelection(TreeNode node)
	{
		ArrayList<TreeNode> bestNodes = new ArrayList<TreeNode>();
		double bestScore = Double.MAX_VALUE * -1;
		
		for (TreeNode child : node.GetChildren())
		{
			double childScore = Double.MAX_VALUE * -1;
			if (child.GetVisits() == 0)
				childScore = Double.MAX_VALUE - rng.nextDouble();
			else
				childScore = child.GetAverageReward() + child.GetExplorationUrgency();
			
			if (childScore > bestScore)
			{
				bestNodes.clear();
				bestScore = childScore;
				bestNodes.add(child);
			}
			else if (childScore == bestScore)
				bestNodes.add(child);
		}
		
		return bestNodes.get(rng.nextInt(bestNodes.size()));
	}
	
	private TreeNode DoExpansion(TreeNode leaf)
	{
		leaf.SetChildren(m_actionList);
		
		return leaf.GetChildren().get(rng.nextInt(m_actionList.size()));
	}
	
	private void DoBackPropagation(TreeNode leaf, double simulationResult)
	{
		for (TreeNode n = leaf; n != null; n=n.GetParent())
		{
			n.UpdateNode(simulationResult);
			//simulationResult *= MCTS_DISCOUNT_FACTOR;
		}
	}
	
	private void DoBackPropagation(TreeNode leaf, double simulationResult, double weight)
	{
		for (TreeNode n = leaf; n != null; n=n.GetParent())
		{
			n.UpdateNode(simulationResult,weight);
			//simulationResult *= MCTS_DISCOUNT_FACTOR;
		}
	}

	@Override
	public void paint(Graphics2D g)
	{
		super.paint(g);
		
		if (m_rootGameState.getMap() == null)
			return;
		
		if (m_par == m_normalPar)
			g.setColor(Color.green);
		else if (m_par == m_panicPar)
			g.setColor(Color.pink);
		
		Game copy = m_rootGameState.getCopy();
		if (m_rootNode != null)
		{
			TreeNode currentNode = m_rootNode;
			
			double posx = copy.getShip().ps.x;
			double posy = copy.getShip().ps.y;
			
			while(currentNode.GetChildren() != null)
			{
				TreeNode bestNode = null;
				int mostVisits = -1;
				for (TreeNode c : currentNode.GetChildren())
				{
					if (c.GetVisits() > mostVisits)
					{
						mostVisits = c.GetVisits();
						bestNode = c;
					}
				}
				
				ApplyMetaAction(copy, bestNode.GetIncomingMetaAction(), null);
				double newposx = copy.getShip().ps.x;
				double newposy = copy.getShip().ps.y;
				
				g.drawLine((int)posx, (int)posy, (int)newposx, (int)newposy);
				
				//g.setFont(new Font("Arial", Font.PLAIN, 10));
				//g.drawString(String.format("  %.4f", currentNode.m_cumulativeReward / currentNode.m_visits), (int)newposx, (int)newposy);
				
				posx = newposx;
				posy = newposy;
				
				currentNode = bestNode;
			}
			
			g.setColor(Color.CYAN);
	        g.setFont(new Font("Courier", Font.PLAIN, 16));
			g.drawString(String.format("Trials: %d/%d", m_bestTrials, m_trials),200,20);
			g.setColor(Color.YELLOW);
		}		
	}
	
	
}
