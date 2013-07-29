package controllers.ParetoMCTS;

import controllers.utils.ParetoArchive;
import controllers.utils.Solution;

/**
 * Created by Diego Perez, University of Essex.
 * Date: 15/07/13
 */
public class Test
{
    public static void main(String args[])
    {
        ParetoArchive pa = new ParetoArchive();

       // pa.add(new double[]{3,5,3});
      //  pa.add(new double[]{2,7,4});
       // pa.add(new double[]{1,6,8});
        pa.add(new Solution(new double[]{3,3,3}));
        pa.add(new Solution(new double[]{2,4,2}));

        pa.computeHV();

    }

}
