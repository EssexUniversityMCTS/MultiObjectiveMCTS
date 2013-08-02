package controllers.utils;

/**
 * Created by Diego Perez, University of Essex.
 * Date: 23/07/13
 */
public class Solution{

    public int m_card;
    public double[] m_data;
    private String m_pattern = "[%.3f,%.3f,%.3f]:%d";
    public int m_through = -1;

    public Solution(double[] a_data)
    {
        m_card = a_data.length;
        m_data = a_data;
    }

    public String toString()
    {
        //if(m_card == 3)
        return String.format(m_pattern, m_data[0] , m_data[1] , m_data[2], m_through);
        //return ""
    }

    public Solution copy()
    {
        double []newArray = new double[m_card];
        System.arraycopy(m_data,0,newArray,0,m_card);
        Solution s = new Solution(newArray);
        s.m_through = this.m_through;
        return s;
    }

}