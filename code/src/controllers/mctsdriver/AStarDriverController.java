package controllers.mctsdriver;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import framework.core.Controller;
import framework.core.Game;
import framework.core.PTSPConstants;
import framework.core.Ship;
import framework.utils.Vector2d;

public class AStarDriverController extends DriverController
{
	public AStarDriverController(Game a_game, long a_timeDue)
	{
		this(a_game, a_timeDue, null);
	}
	
	public AStarDriverController(Game a_game, long a_timeDue, Parameters parameters)
	{
		super(a_game, a_timeDue, parameters);
		//m_par.WAIT_FOR_ROUTEPLANNER = true;
	}

	double getCostSoFar(Game state)
	{
		return m_par.ASTAR_TIME_WEIGHT   * state.getTotalTime() 
			+  m_par.ASTAR_DAMAGE_WEIGHT * state.getShip().getDamage()
			+  m_par.ASTAR_FUEL_WEIGHT   * (PTSPConstants.INITIAL_FUEL - state.getShip().getRemainingFuel())
				;
	}
	
	double getHeuristicEstimate(Game state, ExtraPlayoutInfo extraInfo)
	{
		if (state.isEnded())
		{
			if (state.getWaypointsLeft() == 0)
				return 0;
			else
				return Double.POSITIVE_INFINITY;
		}
		
		List<Integer> route = m_routePlanner.getRoute();
		
		int nextIndex = m_routePlanner.countCollectedWaypointsOnRoute(state);

		int waypointsLeft = state.getWaypointsLeft();
		
		double distanceLeft = m_routePlanner.getDistanceToWaypoint(state.getShip().s, route.get(nextIndex));
		if (distanceLeft == DistanceMapFloodFiller.c_wall)
			return Double.POSITIVE_INFINITY;
		
		if (m_routePlanner.pointIsWaypoint(route.get(nextIndex)))
			waypointsLeft--;
		
		int lastIndex = nextIndex;
		
		for(int i=nextIndex+1; i<route.size() && waypointsLeft > 0; i++)
		{
			if (!m_routePlanner.IsRoutePointCollected(state, route.get(i)))
			{
				distanceLeft += m_routePlanner.getDistanceToWaypoint(m_routePlanner.getPoint(route.get(lastIndex)), route.get(i));
				lastIndex = i;

				if (m_routePlanner.pointIsWaypoint(route.get(i)))
					waypointsLeft--;
			}
		}
		
		//double averageSpeed = (m_distanceTravelledSoFar + extraInfo.m_distanceTravelled) / getCostSoFar(state) * 0.8;
		//double averageSpeed = m_distanceTravelledSoFar / getCostSoFar(m_rootGameState);
		//if (m_par.CONSOLE_OUTPUT) System.out.printf("Average speed %.4f\n", averageSpeed);
		double averageSpeed = m_par.ASTAR_AVERAGE_SPEED_ESTIMATE;
		return distanceLeft / averageSpeed;
	}
	
	class Node
	{
		public Node(Game state, ExtraPlayoutInfo extraInfo, MetaAction incomingAction, Node parent)
		{
			m_incomingAction = incomingAction;
			m_parent = parent;
			m_g = getCostSoFar(state);
			m_h = getHeuristicEstimate(state, extraInfo);
			m_f = m_g + m_h;
			m_fBestLeaf = m_f;
			
			m_children = new ArrayList<Node>();
			
			m_shipPos = state.getShip().s.copy();
			
			if (m_par.ASTAR_USE_HASH_MAP)
				m_hashMapKey = new HashMapKey(state);
		}
		
		public Node m_parent;
		public MetaAction m_incomingAction;
		public double m_f, m_g, m_h, m_fBestLeaf;
		public Vector2d m_shipPos;
		public HashMapKey m_hashMapKey;
		
		public List<Node> m_children;
		
		private Node m_bestChild = null;
		public Node getBestChild()
		{
			return m_bestChild;
		}
		
		public void backpropagate()
		{
			m_bestChild = null;
			
			for (Node child : m_children)
			{
				if (m_bestChild == null || child.m_fBestLeaf < m_bestChild.m_fBestLeaf)
					m_bestChild = child;
			}
			
			if (m_fBestLeaf != m_bestChild.m_fBestLeaf)
			{
				m_fBestLeaf = m_bestChild.m_fBestLeaf;

				if (m_parent != null)
				{
					assert(m_parent.m_children.contains(this));
					m_parent.backpropagate();
				}
			}
		}
		
		public void prune()
		{
			if (m_par.ASTAR_USE_HASH_MAP && m_nodeHashMap.get(m_hashMapKey) == this)
				m_nodeHashMap.remove(m_hashMapKey);
			
			if (m_parent != null)
			{
				boolean wasBestChild = (this == m_parent.getBestChild());
				m_parent.m_children.remove(this);
				
				if (m_parent.m_children.isEmpty())
				{
					m_parent.prune();
				}
				else if (wasBestChild)
				{
					m_parent.m_fBestLeaf = m_parent.getBestChild().m_fBestLeaf;
					m_parent.backpropagate();
				}
				
				m_parent = null;
			}
		}
		
		public Node findRoot()
		{
			if (m_parent == null)
				return this;
			else
				return m_parent.findRoot();
		}
	}
	
	Node m_openTreeRoot;
	
	class HashMapKey
	{
		private int m_sx, m_sy, m_vx, m_vy, m_heading, m_pointsCollected;
		
		public HashMapKey(Game state)
		{
			Ship ship = state.getShip();
			m_sx = (int)(ship.s.x / m_par.ASTAR_HASH_MAP_POSITION_RESOLUTION);
			m_sy = (int)(ship.s.y / m_par.ASTAR_HASH_MAP_POSITION_RESOLUTION);
			m_vx = (int)(ship.v.x / m_par.ASTAR_HASH_MAP_VELOCITY_RESOLUTION);
			m_vy = (int)(ship.v.y / m_par.ASTAR_HASH_MAP_VELOCITY_RESOLUTION);
			m_heading = (int)(Math.atan2(ship.d.y, ship.d.x) / (Ship.steerStep * m_par.ROTATION_SUBDIVISIONS));
			
			m_pointsCollected = state.getWaypointsVisited() + state.getFuelTanksCollected();
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof HashMapKey)
			{
				HashMapKey other = (HashMapKey)obj;
				
				return this.m_sx == other.m_sx
					&& this.m_sy == other.m_sy
					&& this.m_vx == other.m_vx
					&& this.m_vy == other.m_vy
					&& this.m_heading == other.m_heading
					&& this.m_pointsCollected == other.m_pointsCollected;
			}
			else
			{
				return false;
			}
		}

		@Override
		public int hashCode()
		{
			return m_sx
				 + m_sy << 9
				 + m_vx << 18
				 + m_vy << 22
				 + m_heading << 26
				 + m_pointsCollected << 29;
		}
	}
	
	HashMap<HashMapKey, Node> m_nodeHashMap;
	
	List<MetaAction> m_bestWinningLine = null;
	double m_bestWinningLineScore = 0;
	
	double m_distanceTravelledSoFar = 0;

	void initialiseTree(Game newRootState)
	{
		m_openTreeRoot = new Node(newRootState.getCopy(), new ExtraPlayoutInfo(), null, null);
		
		if (m_nodeHashMap == null)
			m_nodeHashMap = new HashMap<HashMapKey, Node>();
		
		if (m_par.ASTAR_USE_HASH_MAP)
		{
			m_nodeHashMap.clear();
			m_nodeHashMap.put(m_openTreeRoot.m_hashMapKey, m_openTreeRoot);
		}
		
		m_bestWinningLine = null;		
	}
	
	@Override
	protected void resetSearch(Game newRootState)
	{
		super.resetSearch(newRootState);
		
		if (m_routePlanner != null)
		{
			if (m_openTreeRoot != null)
			{
				Node bestChild = m_openTreeRoot.getBestChild();
				if (bestChild != null && bestChild.m_shipPos.equals(newRootState.getShip().s))
				{
					//if (m_par.CONSOLE_OUTPUT) System.out.println("Descending tree");
					
					ExtraPlayoutInfo info = new ExtraPlayoutInfo();
					info.m_distanceTravelled = 0;
					ApplyMetaAction(m_rootGameState.getCopy(), getBestMetaAction(), info);
					m_distanceTravelledSoFar += info.m_distanceTravelled;
					/*if (m_par.CONSOLE_OUTPUT)
						System.out.printf("Distance travelled so far: %.4f // Cost so far: %.4f // Ratio: %.4f\n",
							m_distanceTravelledSoFar,
							getCostSoFar(newRootState),
							m_distanceTravelledSoFar / getCostSoFar(newRootState)
							);*/

					for (Node child : m_openTreeRoot.m_children)
						child.m_parent = null;
					
					m_openTreeRoot = bestChild;
					
					if (m_bestWinningLine != null)
						m_bestWinningLine.remove(0);
				}
				else
				{
					if (m_par.CONSOLE_OUTPUT) System.out.println("Something went wrong, resetting tree");
					initialiseTree(newRootState);
				}
			}
			else
			{
				if (m_par.CONSOLE_OUTPUT) System.out.println("Initialising tree");
				initialiseTree(newRootState);
			}
		}
	}

	@Override
	protected void doSearch(long timeDue)
	{
		if (m_openTreeRoot == null)
			resetSearch(m_rootGameState);
		
		long maxIterationMillis = 0;

		while (System.currentTimeMillis() + maxIterationMillis < timeDue)
		{
			long iterationStartMillis = System.currentTimeMillis();
			
			Node current = m_openTreeRoot;
			Game currentState = m_rootGameState.getCopy();
			ExtraPlayoutInfo currentExtraInfo = new ExtraPlayoutInfo();
			currentExtraInfo.m_distanceTravelled = 0;
			
			while (!current.m_children.isEmpty())
			{
				double epsilon = m_par.ASTAR_OPEN_LIST_EPSILON;
				if (m_bestWinningLine != null)
					epsilon = m_par.ASTAR_OPEN_LIST_EPSILON_WITH_WINNING_LINE;
				
				if (rng.nextDouble() < epsilon)
					current = current.m_children.get(rng.nextInt(current.m_children.size()));
				else
					current = current.getBestChild();
				
				ApplyMetaAction(currentState, current.m_incomingAction, currentExtraInfo);
				
				if (System.currentTimeMillis() >= timeDue)
				{
					if (m_par.CONSOLE_OUTPUT) System.out.println("Iteration timed out");
					return;
				}
			}
			
			if (currentState.isEnded())
			{
				if (currentState.getWaypointsLeft() == 0)
				{
					return; // Success
				}
				else // Failure
				{
					if (current == m_openTreeRoot)
					{
						// Certain failure :(
						return;
					}
					
					current.prune();
				}
			}
			else
			{
				for (MetaAction action : m_actionList)
				{
					Game neighbourState = currentState.getCopy();
					ExtraPlayoutInfo neighbourExtraInfo = new ExtraPlayoutInfo();
					ApplyMetaAction(neighbourState, action, neighbourExtraInfo);
					
					if (neighbourState.getWaypointsLeft() == 0)
					{
						double score = getCostSoFar(neighbourState);
						if (m_bestWinningLine == null || score < m_bestWinningLineScore)
						{
							if (m_par.CONSOLE_OUTPUT) System.out.printf("Found a new winning line: score %.4f\n", score);
							
							m_bestWinningLineScore = score;
							
							m_bestWinningLine = new LinkedList<MetaAction>();
							m_bestWinningLine.add(0, action);
							
							for (Node node = current; node.m_parent != null; node = node.m_parent)
							{
								m_bestWinningLine.add(0, node.m_incomingAction);
							}
						}
					}
					else if (neighbourState.hasStarted()
							&& !(m_par.PRUNE_STALLING_MOVES && neighbourExtraInfo.m_lastMoveWasStalling)
							&& !(neighbourState.isEnded() && neighbourState.getWaypointsLeft() > 0)
						)
					{
						Node neighbourNode = new Node(neighbourState, neighbourExtraInfo, action, current);
						
						Node oldNeighbourNode = null;
						if (m_par.ASTAR_USE_HASH_MAP)
							oldNeighbourNode = m_nodeHashMap.get(neighbourNode.m_hashMapKey);
						
						if (oldNeighbourNode != null && oldNeighbourNode.findRoot() != m_openTreeRoot)
							oldNeighbourNode = null;
						
						if (oldNeighbourNode == null || oldNeighbourNode.m_g > neighbourNode.m_g)
						{
							current.m_children.add(neighbourNode);
							
							if (oldNeighbourNode != null)
								oldNeighbourNode.prune();
							
							if (m_par.ASTAR_USE_HASH_MAP)
								m_nodeHashMap.put(neighbourNode.m_hashMapKey, neighbourNode);
						}
					}
				}
				
				if (current.m_children.isEmpty())
					current.prune();
				else
					current.backpropagate();
			}
			
			long timeTaken = System.currentTimeMillis() - iterationStartMillis;
			if (timeTaken > maxIterationMillis)
				maxIterationMillis = timeTaken;
		}
	}

	@Override
	protected boolean isSearchTreeEmpty()
	{
		return m_openTreeRoot == null || m_openTreeRoot.m_children.isEmpty();
	}

	@Override
	protected MetaAction getBestMetaAction()
	{
		if (m_bestWinningLine != null && !m_bestWinningLine.isEmpty())
			return m_bestWinningLine.get(0);
		
		if (m_openTreeRoot.getBestChild() == null)
			return new MetaAction(m_par.ROTATION_SUBDIVISIONS, 0, false);
		else
			return m_openTreeRoot.getBestChild().m_incomingAction;
	}
	
	int paintTree(Graphics2D g, Node node)
	{
		int numLeafs = 0;
		
		g.setColor(new Color(0.0f, 0.5f, 0.5f, 0.1f));
		for (Node child : node.m_children)
		{
			g.drawLine((int)node.m_shipPos.x, (int)node.m_shipPos.y, (int)child.m_shipPos.x, (int)child.m_shipPos.y);
			numLeafs += paintTree(g, child);
		}
		
		if (node.m_children.isEmpty())
			numLeafs++;
		
		return numLeafs;
	}
	
	@Override
	public void paint(Graphics2D g)
	{
		try
		{
			super.paint(g);
			
			if (m_rootGameState.getMap() == null)
				return;
			
			int numNodes = paintTree(g, m_openTreeRoot);
			
			g.setColor(Color.white);
	        g.setFont(new Font("Arial", Font.BOLD, 16));
			g.drawString(String.format("Open leaf nodes: %d", numNodes), 200,20);
			g.drawString(String.format("Hash map items: %d", m_nodeHashMap.size()), 200,36);
	        g.setFont(new Font("Arial", Font.BOLD, 10));
			g.drawString(String.format("Velocity: (%.4f, %.4f)", m_rootGameState.getShip().v.x, m_rootGameState.getShip().v.y), 200,46);
			
			g.setColor(Color.green);
	
			double posx = m_openTreeRoot.m_shipPos.x;
			double posy = m_openTreeRoot.m_shipPos.y;
	
			for (Node node = m_openTreeRoot.getBestChild(); node != null; node = node.getBestChild())
			{
				double newposx = node.m_shipPos.x;
				double newposy = node.m_shipPos.y;
				
				g.drawLine((int)posx, (int)posy, (int)newposx, (int)newposy);
				
				if (node.m_children.isEmpty())
				{
					g.setFont(new Font("Arial", Font.PLAIN, 10));
					g.drawString(String.format("  %.4f + %.4f = %.4f", node.m_g, node.m_h, node.m_f), (int)newposx, (int)newposy);
				}
				
				posx = newposx; posy = newposy;
			}
			
			if (m_bestWinningLine != null)
			{
				g.setColor(Color.yellow);
				
				Game state = m_rootGameState.getCopy();
				
				posx = state.getShip().s.x;
				posy = state.getShip().s.y;
	
				for (MetaAction action : m_bestWinningLine)
				{
					ApplyMetaAction(state, action, null);
					double newposx = state.getShip().s.x;
					double newposy = state.getShip().s.y;
					
					g.drawLine((int)posx, (int)posy, (int)newposx, (int)newposy);
					
					posx = newposx; posy = newposy;
				}
				
				g.setFont(new Font("Arial", Font.PLAIN, 10));
				g.drawString(String.format("  %.4f", m_bestWinningLineScore), (int)posx, (int)posy + 10);
			}
			
			/*for (HashMapKey key : new ArrayList<HashMapKey>(m_nodeHashMap.keySet()))
			{
				if (m_nodeHashMap.get(key).findRoot() == m_openTreeRoot)
					g.setColor(new Color(1.0f, 0.5f, 0.0f, 0.2f));
				else
					g.setColor(new Color(1.0f, 0.0f, 0.0f, 0.2f));
					
				g.drawLine(key.m_sx, key.m_sy, key.m_sx, key.m_sy);
			}*/
		}
		catch (ConcurrentModificationException err)
		{
			// Do nothing (this is just to get rid of the annoying red text in the console)
		}
	}
}
