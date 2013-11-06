package controllers.utils;


//import controllers.ParetoMCTS.ParetoMCTSController;
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
    public static double EPSILON = 0.0; //0.01
    public OrderedSolutionList m_members;
    public double m_hv;
    public boolean m_hvClean;

    public ParetoArchive()
    {
        m_hvClean = false;
        m_hv = -1;
        m_members = new OrderedSolutionList();
    }

    public void reset()
    {
        m_members.clear();
        m_hvClean = false;
        m_hv = -1;
    }

    public ParetoArchive copy()
    {
        ParetoArchive newPA = new ParetoArchive();

        for(Solution s : this.m_members.m_members)
        {
            newPA.m_members.add(s.copy());
        }

        return newPA;
    }

    public void addMembers(OrderedSolutionList a_list)
    {
        int nMembers = a_list.size();
        for(int i = 0; i < nMembers; ++i)
        {
            Solution member = a_list.get(i);
            this.add(member);
        }
    }

    public boolean add(Solution a_sol)
    {
        //Check if the new entry is dominated by any in the set:

        boolean dominated = false;
        boolean dominatesAny = false;
        boolean crowded = false;
        int i = 0;
        while(!dominated && i < m_members.size())
        {
            Solution member = m_members.get(i);
            int dom = Utils.dominates(member.m_data, a_sol.m_data);

            //if(dom == -1)
            if(dom == -1)
            {
                //if(crowded)
                  //  System.out.println("OUT ["+m_members.size()+"]");

                dominated = true;
            }else if(dom == 1)
            {
                //This one is dominated. It must be out.
                m_members.remove(i);
                m_hvClean = false;
                dominatesAny = true;
                //And keep the index in place:
                --i;
            }else if(dom == 2)    //Use this line to allow any distance between points in pareto.
            //}else if(dom == 2 || crowded) //Use this line to avoid crowded fronts (min. distance: EPSILON).
            {
                //There is another identical member in the set. Do NOT include:
                return false;
            }
            else if(dom == 0)
            {
                crowded |= Utils.crowded(member.m_data, a_sol.m_data, EPSILON);
            }
            ++i;
        }

        if(dominatesAny)
            crowded = false;

        if(!dominated && !crowded)
        {
            /*double[] newOne = new double[a_candidate.length];
            System.arraycopy(a_candidate, 0, newOne, 0, a_candidate.length);
            Solution newSol = new Solution(newOne);    */
            m_members.add(a_sol);
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
            Solution member = m_members.get(i);
            int dom = Utils.dominates(member.m_data, a_point);
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
            Solution member = m_members.get(i);
            for(int j = 0; j < member.m_card; ++j)
            {
                System.out.format("%.5f ", member.m_data[j]);
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
            Solution member = m_members.get(i);
            int nTargets = member.m_card;
            boolean distinct = false;

            for(int j = 0; !distinct && j < nTargets; ++j)
            {
                if(member.m_data[j] != a_point[j])
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
            double card = m_members.get(0).m_card;

            if(card == 1)
                return lebesgue1();
            if(card == 2)
                return lebesgue2();
            else if(card == 3)
                return lebesgue3();
        }

        return -1;
    }

    private double lebesgue1()    //Assumes maximization.
    {
        return m_members.get(0).m_data[0];
    }

    private double lebesgue2()    //Assumes maximization.
    {
        double dim1 = 0;
        double acum = 0;

        for(int i = 0; i < m_members.size(); ++i)
        {
            double[] member = m_members.get(i).m_data;
            double base = member[0] - dim1;
            double height = member[1];
            acum += (base*height);

            dim1 = member[0];
        }
        m_hvClean = true; //We are calculating it.
        m_hv = acum;
        return acum;
    }

    private double lebesgue3()         //Assumes maximization.
    {
        //long now = System.currentTimeMillis();
        //ParetoMCTSController.HV_COUNTS++;

        OrderedList pointsInX = new OrderedList();
        OrderedList pointsInY = new OrderedList();
        OrderedList pointsInZ = new OrderedList();

        //We decompose the studied region in a 3-dimensional grid using all values recorded for (x,y,z).
        for(int i = 0; i < m_members.size(); ++i)
        {
            double[] member = m_members.get(i).m_data;
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
        m_hv = acum;

        //System.out.println("lebesgue3() with " + m_members.size() + " points: " + (System.currentTimeMillis()-now));

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
