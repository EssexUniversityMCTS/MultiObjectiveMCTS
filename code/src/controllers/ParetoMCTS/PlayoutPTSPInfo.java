package controllers.ParetoMCTS;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 02/08/13
 * Time: 13:06
 * To change this template use File | Settings | File Templates.
 */
public class PlayoutPTSPInfo implements PlayoutInfo
{

    public int m_thurstCount;

    public PlayoutPTSPInfo()
    {
        m_thurstCount = 0;
    }

    public void reset()
    {
        m_thurstCount = 0;
    }

}
