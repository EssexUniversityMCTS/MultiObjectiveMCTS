package framework.utils;

/**
 * Created by Diego Perez, University of Essex.
 * Date: 15/07/13
 */
public class Cube
{
    public double[] m_top;
    public double[] m_bot;
    public double[] m_sides;
    public double[] m_center;
    public double m_volume;

    public Cube(double a_xt, double a_yt, double a_zt, double a_xb, double a_yb, double a_zb)
    {
        m_top = new double[]{a_xt, a_yt, a_zt};
        m_bot = new double[]{a_xb, a_yb, a_zb};
        m_sides= new double[]{m_top[0]-m_bot[0], m_top[1]-m_bot[1], m_top[2]-m_bot[2]};
        m_volume = m_sides[0]*m_sides[1]*m_sides[2];
        m_center= new double[]{a_xb + m_sides[0]/2.0, a_yb + m_sides[1]/2.0, a_zb + m_sides[2]/2.0};
    }
}
