package controllers.ParetoMCTS;

import controllers.utils.ParetoArchive;
import controllers.utils.Solution;
import controllers.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Created by Diego Perez, University of Essex.
 * Date: 30/07/13
 */
public class ParetoView extends JComponent
{
    private ParetoArchive m_pa;
    private Dimension m_size;
    private Font m_font;
    private boolean m_firstDraw;
    private BufferedImage m_mapImage;

    private Color background = Color.WHITE;
    private Color pointsA = Color.blue;
    private Color pointsB = Color.red;
    private Color axis = Color.black;

    private int MARGIN = 10;
    private int MARGIN_EXTRA = 50;
    private int POINT_SIZE = 5;
    private double SCALE = 10000.0;

    public ParetoView(ParetoArchive pa, Dimension size)
    {
        m_pa = pa;
        m_size = size;
        m_font = new Font("Courier", Font.PLAIN, 14);
        m_firstDraw = true;
        m_mapImage = null;
    }

    public void setParetoArchive(ParetoArchive pa) {m_pa = pa;}


    public void paintComponent(Graphics gx)
    {
        Graphics2D g = (Graphics2D) gx;

        //For a better graphics, enable this: (be aware this could bring performance issues depending on your HW & OS).
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(background);
        g.fillRect(0, m_size.height, m_size.width, m_size.height);

        if(m_firstDraw)
        {
            m_mapImage = new BufferedImage(m_size.width, m_size.height,BufferedImage.TYPE_INT_RGB);
            Graphics2D gImage = m_mapImage.createGraphics();

            gImage.setColor(background);
            gImage.fillRect(0, 0, m_size.width, m_size.height);

            gImage.setColor(axis);
            gImage.drawLine(0, m_size.height - MARGIN, m_size.width, m_size.height - MARGIN);
            gImage.drawLine(MARGIN, m_size.height, MARGIN, 0);

            m_firstDraw = false;

        } else {
            //Just paint the buffer from the 2nd time on.
            g.drawImage(m_mapImage,0,0,null);
        }

        if(m_pa.m_members.size() > 0)
        {
            int card = m_pa.m_members.m_members.get(0).m_card;
            if (card==2)
                paint2(g);
            else if(card==3)
                paint3(g);
        }

    }

    public void paint2(Graphics2D g)
    {
        int previousP1=-1;
        int previousP2=-1;
        g.setColor(pointsA);
        //System.out.println("###############################");

        double xRange[] = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};
        double yRange[] = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};

        for(Solution s : m_pa.m_members.m_members)
        {
            double[] point = s.m_data;
            if(point[0] < xRange[0])
                xRange[0] = point[0];
            if(point[0] > xRange[1])
                xRange[1] = point[0];

            if(point[1] < yRange[0])
                yRange[0] = point[1];
            if(point[1] > yRange[1])
                yRange[1] = point[1];
        }

        //System.out.printf("R:(%.3f,%.3f)\n", xRange[0], yRange[1]);
        for(Solution s : m_pa.m_members.m_members)
        {
            double[] point = s.m_data;

            double xVal0 = point[0] - xRange[0];
            double yVal0 = point[1] - yRange[0];

            double xVal1 = point[1] - xRange[0];
            double yVal1 = point[1] - yRange[0];


            int p1 = MARGIN_EXTRA + (int) (xVal0*SCALE);
            int p2 = (m_size.height-MARGIN_EXTRA) - (int) (yVal0*SCALE);

            g.fillOval(p1,p2,POINT_SIZE,POINT_SIZE);
            //System.out.printf("(%.3f,%.3f):(%d,%d):%d\n", point[0], point[1], p1, p2, s.m_through);
            //System.out.printf("(%.3f,%.3f):(%.3f,%.3f):(%d,%d):%d\n", point[0], point[1], xVal0,yVal0, p1, p2, s.m_through);

            g.drawString(s.m_through+"",p1+5,p2-5);

            if(previousP1 != -1)
            {
                g.drawLine(previousP1, previousP2, p1, p2);
            }

            previousP1 = p1;
            previousP2 = p2;
        }
        //System.out.println("###############################");
    }

    public void paint3(Graphics2D g)
    {
        int previousP1=-1;
        int previousP2=-1;
        int previousP3=-1;
        //System.out.println("###############################");
        for(Solution s : m_pa.m_members.m_members)
        {
            double[] point = s.m_data;

            int p1 = MARGIN + (int) (point[0]*SCALE);
            int p2 = (m_size.height-MARGIN) - (int) (point[1]*SCALE);
            int p3 = (m_size.height-MARGIN) - (int) (point[2]*SCALE);
            //System.out.printf("(%.3f,%.3f,%.3f):(%d,%d,%d)\n", point[0], point[1], point[2], p1, p2, p3);


            g.setColor(pointsA);
            g.fillOval(p1,p2,POINT_SIZE,POINT_SIZE);

            g.setColor(pointsB);
            g.fillOval(p1,p3,POINT_SIZE,POINT_SIZE);

            if(previousP1 != -1)
            {
                g.drawLine(previousP1, previousP2, p1, p2);
                g.drawLine(previousP1, previousP3, p1, p3);
            }

            previousP1 = p1;
            previousP2 = p2;
            previousP3 = p3;
        }
        //System.out.println("###############################");
    }


    public Dimension getPreferredSize() {
        return m_size;
    }
}
