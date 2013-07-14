package controllers.utils;


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

    public ParetoArchive()
    {
        m_members = new OrderedArrayList();
    }

    public void reset()
    {
        m_members.clear();
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
            return true;
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
            System.out.format("%.2f %.2f\n", member[0], member[1]);
        }
        System.out.println("############################");
        
    }

    public boolean contains(double[] a_point)
    {
        int nMembers = m_members.size();
        for(int i = 0; i < nMembers; ++i)
        {
            double[] member = m_members.get(i);
            if(member[0] == a_point[0] && member[1] == a_point[1])
                return true;
        }
        return false;
    }

    //Only valid for 2 dimensions.
    public double computeHV2()
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
        return acum;
    }

    public double computeHV2(double[][] bounds)
    {
        double dim1 = 0;
        double acum = 0;

        for(int i = 0; i < m_members.size(); ++i)
        {
            double[] member = m_members.get(i);
            double base = member[0] - dim1;
            double height = member[1];
            double val1 = Utils.normalise(base, bounds[0][0], bounds[0][1]);
            double val2 = Utils.normalise(height, bounds[1][0], bounds[1][1]);
            acum += (val1*val2);

            dim1 = member[0];
        }
        return acum;
    }


}
