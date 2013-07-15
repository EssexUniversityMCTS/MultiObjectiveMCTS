package controllers.utils;


import framework.utils.Cube;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 13/02/13
 * Time: 13:39
 * To change this template use File | Settings | File Templates.
 */
public class ParetoArchive
{
    public OrderedArrayList m_members;
    public double m_hv;
    public boolean m_hvClean;

    public ParetoArchive()
    {
        m_hvClean = false;
        m_hv = -1;
        m_members = new OrderedArrayList();
    }

    public void reset()
    {
        m_members.clear();
        m_hvClean = false;
        m_hv = -1;
    }

    public void addMembers(OrderedArrayList a_list)
    {
        int nMembers = a_list.size();
        for(int i = 0; i < nMembers; ++i)
        {
            double[] member = a_list.get(i);
            this.add(member);
        }
    }

    public boolean add(double[] a_candidate)
    {
        //Check if the new entry is dominated by any in the set:
        boolean dominated = false;
        int i = 0;
        while(!dominated && i < m_members.size())
        {
            double[] member = m_members.get(i);
            int dom = Utils.dominates(member, a_candidate);
            if(dom == -1)
            {
                dominated = true;
            }else if(dom == 1)
            {
                //This one is dominated. It must be out.
                m_members.remove(i);
                m_hvClean = false;
                //And keep the index in place:
                --i;
            }else if(dom == 2)
            {
                //There is another identical member in the set. Do NOT include:
                return false;
            }
            ++i;
        }

        if(!dominated)
        {
            double[] newOne = new double[a_candidate.length];
            System.arraycopy(a_candidate, 0, newOne, 0, a_candidate.length);
            m_members.add(newOne);
            m_hvClean = false;
            return true;
        }

        return false;
    }

    public boolean isDominated(double[] a_point)
    {
        int i = 0;
        while(i < m_members.size())
        {
            double[] member = m_members.get(i);
            int dom = Utils.dominates(member, a_point);
            if(dom == -1)
                return true;
            ++i;
        }
        return false;
    }
    
    public void printArchive()
    {
        System.out.println("########### PA: ############");
        int nMembers = m_members.size();
        for(int i = 0; i < nMembers; ++i)
        {
            double[] member = m_members.get(i);
            for(int j = 0; j < member.length; ++j)
            {
                System.out.format("%.2f ", member[j]);
            }
            System.out.println();
        }
        System.out.println("############################");
        
    }

    public boolean contains(double[] a_point)
    {
        int nMembers = m_members.size();
        for(int i = 0; i < nMembers; ++i)
        {
            double[] member = m_members.get(i);
            int nTargets = member.length;
            boolean distinct = false;

            for(int j = 0; !distinct && j < nTargets; ++j)
            {
                if(member[j] != a_point[j])
                    distinct = true;
            }

            if(!distinct)
                return true;
        }
        return false;
    }

    //Computes HV
    public double computeHV()
    {
        if(m_hvClean)
            return m_hv; //No changes made, no need to recalculate HV.

        double dim1 = 0;
        double hvAcum = 0;

        if(m_members.size() > 0)
        {
            double first[] = m_members.get(0);
            if(first.length == 2)
                return lebesgue2();
            else if(first.length == 3)
                return lebesgue3();
        }

        return -1;
    }

    private double lebesgue2()    //Assumes maximization.
    {
        double dim1 = 0;
        double acum = 0;

        for(int i = 0; i < m_members.size(); ++i)
        {
            double[] member = m_members.get(i);
            double base = member[0] - dim1;
            double height = member[1];
            acum += (base*height);

            dim1 = member[0];
        }
        m_hvClean = true; //We are calculating it.
        return acum;
    }

    private double lebesgue3()         //Assumes maximization.
    {
        OrderedList pointsInX = new OrderedList();
        OrderedList pointsInY = new OrderedList();
        OrderedList pointsInZ = new OrderedList();

        //We decompose the studied region in a 3-dimensional grid using all values recorded for (x,y,z).
        for(int i = 0; i < m_members.size(); ++i)
        {
            double[] member = m_members.get(i);
            pointsInX.add(member[0]);
            pointsInY.add(member[1]);
            pointsInZ.add(member[2]);
        }

        double xPrev = 0;
        double yPrev = 0;
        double zPrev = 0;
        double acum = 0;

        //Go through the (sorted) grid, defining the sub-cubes contained in it.
        for(double x : pointsInX.m_members)
        {
            for(double y : pointsInY.m_members)
            {
                for(double z : pointsInZ.m_members)
                {
                    Cube c = new Cube(x,y,z,xPrev,yPrev,zPrev);
                    //Check if the cube (by its center) is below the pareto front:
                    if(isDominated(c.m_center))
                    {
                        //Add the volumes of all dominated regions (cubes).
                        acum += c.m_volume;
                    }
                    zPrev = z;
                }
                yPrev = y;
                zPrev = 0;
            }
            xPrev = x;
            yPrev = 0;
            zPrev = 0;
        }

        m_hvClean = true;
        return acum;
    }

    public double computeHV(double[][] bounds)
    {
        throw new RuntimeException("Uups! Not implemented!");
        /*
        if(m_hvClean)
            return m_hv; //No changes made, no need to recalculate HV.

        double dim1 = 0;
        double hvAcum = 0;

        if(m_members.size() > 0)
        {
            double first[] = m_members.get(0);
            if(first.length == 2)
                return lebesgue2(bounds);
            else if(first.length == 3)
                return lebesgue3(bounds);
        }

        return -1; */
    }

    private class DoubleComparator implements Comparator<Double>
    {
        public int compare(Double x, Double y)
        {
            if(x < y) return -1;
            else if(x > y) return 1;
            return 0;
        }
    }

}
