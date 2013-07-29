package controllers.utils;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 13/02/13
 * Time: 14:35
 * To change this template use File | Settings | File Templates.
 */
public class OrderedSolutionList
{
    public ArrayList<Solution> m_members;

    public OrderedSolutionList()
    {
        m_members = new ArrayList<Solution>();
    }

    public void clear() {m_members.clear();}
    public int size() {return m_members.size();}
    public Solution get(int index) {return m_members.get(index);}
    public void remove(int index) {m_members.remove(index);}

    public void add(Solution a_element)
    {
        //Additions are (asc) ordered by the value of the first element of the array.
        boolean here = false;
        int i = 0;
        while(!here && i < m_members.size())
        {
            Solution m = m_members.get(i);

            if(a_element.m_data[0] < m.m_data[0])
            {
                here = true;
                m_members.add(i,a_element);
            }
            ++i;
        }

        if(!here)
        {   //add at the end:
            m_members.add(a_element);
        }

    }
    
    
}
