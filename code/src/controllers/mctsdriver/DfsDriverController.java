package controllers.mctsdriver;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Stack;

import framework.core.Game;

public class DfsDriverController extends DriverController
{

	public DfsDriverController(Game a_game, long a_timeDue)
	{
		super(a_game, a_timeDue, null);
	}

	public DfsDriverController(Game a_game, long a_timeDue, Parameters parameters)
	{
		super(a_game, a_timeDue, parameters);
	}
	
	private class SearchFrame
	{
		public Game m_state;
		public int m_incomingAction;
		public int m_nextAction;
		public SearchFrame m_bestChild;
		public double m_bestScore;
		
		public SearchFrame(Game state, int incomingAction)
		{
			m_state = state.getCopy();
			m_incomingAction = incomingAction;
			m_bestChild = null;
			m_bestScore = Double.NEGATIVE_INFINITY;
			m_nextAction = 0;
		}
	}
	
	private Stack<SearchFrame> m_searchStack;
	private int m_searchDepth = m_par.DFS_SEARCH_DEPTH;

	@Override
	protected void resetSearch(Game newRootState)
	{
		super.resetSearch(newRootState);
		
		m_searchStack = new Stack<SearchFrame>();
		m_searchStack.push(new SearchFrame(newRootState, 0));
	}
	
	@Override
	protected void doSearch(long timeDue)
	{
		while (System.currentTimeMillis() < timeDue)
		{
			SearchFrame currentFrame = m_searchStack.peek();
				
			if (currentFrame.m_nextAction < m_actionList.size())
			{
				Game nextState = currentFrame.m_state.getCopy();
				ApplyMetaAction(nextState, m_actionList.get(currentFrame.m_nextAction), null);
				SearchFrame nextFrame = new SearchFrame(nextState, currentFrame.m_nextAction);
				if (m_searchStack.size() >= m_searchDepth)
				{
					nextFrame.m_nextAction = m_actionList.size();
					nextFrame.m_bestScore = GetSimulationResult(nextState, null);
				}
				m_searchStack.push(nextFrame);
			}
			else
			{
				if (m_searchStack.size() == 1)
					return;
				
				m_searchStack.pop();
				SearchFrame previousFrame = m_searchStack.peek();
				
				if (currentFrame.m_bestScore > previousFrame.m_bestScore)
				{
					previousFrame.m_bestScore = currentFrame.m_bestScore;
					previousFrame.m_bestChild = currentFrame;
				}
				
				previousFrame.m_nextAction ++;
			}
		}
	}
	
	@Override
	protected boolean isSearchTreeEmpty()
	{
		return m_searchStack.get(0).m_bestChild == null;
	}
	
	boolean m_searchFinished = false;
	
	@Override
	protected MetaAction getBestMetaAction()
	{
		m_searchFinished = (m_searchStack.get(0).m_nextAction >= m_actionList.size());
		return m_actionList.get(m_searchStack.get(0).m_bestChild.m_incomingAction);
	}

	@Override
	public void paint(Graphics2D g)
	{
		super.paint(g);
		
		g.setColor(Color.white);

		double posx = m_rootGameState.getShip().ps.x;
		double posy = m_rootGameState.getShip().ps.y;

		for (SearchFrame frame = m_searchStack.get(0); frame != null; frame = frame.m_bestChild)
		{
			double newposx = frame.m_state.getShip().ps.x;
			double newposy = frame.m_state.getShip().ps.y;
			
			g.drawLine((int)posx, (int)posy, (int)newposx, (int)newposy);
			
			if (frame.m_bestChild == null)
			{
				g.setFont(new Font("Arial", Font.PLAIN, 10));
				g.drawString(String.format("  %.4f", frame.m_bestScore), (int)newposx, (int)newposy);
			}
			
			posx = newposx; posy = newposy;
		}
		
		g.setColor(Color.CYAN);
        g.setFont(new Font("Courier", Font.PLAIN, 16));
        if (m_searchFinished)
        	g.drawString("Finished",200,20);
        else
        	g.drawString("Not finished",200,20);
	}
	
	
}
