package controllers.mctsdriver;

public class ExtraPlayoutInfo
{
	public double m_collisionDamage = 0;
	public boolean m_lastMoveWasStalling = false;
	
	public double m_distanceTravelled = Double.NaN; // Set this to something other than NaN if you want it to be updated
	
	public ExtraPlayoutInfo getCopy()
	{
		ExtraPlayoutInfo copy = new ExtraPlayoutInfo();
		copy.m_collisionDamage = this.m_collisionDamage;
		copy.m_lastMoveWasStalling = this.m_lastMoveWasStalling;
		copy.m_distanceTravelled = this.m_distanceTravelled; 
		return copy;
	}
}
