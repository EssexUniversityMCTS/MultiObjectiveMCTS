package controllers.mctsdriver;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

import javax.imageio.ImageIO;

//import wpm.mjpeg.MJPEGGenerator;

import framework.core.Map;
import framework.core.Ship;
import framework.utils.Vector2d;

public class DistanceMapFloodFiller
{
	private double[][] m_distanceMap;
	int m_width, m_height;
	int m_originX, m_originY;
	Map m_map;
	Parameters m_par;
	
	static final double c_wall = Double.NEGATIVE_INFINITY;
	static final double c_empty = Double.POSITIVE_INFINITY;
	
	private void init(Parameters param, Map map, int originX, int originY)
	{
		m_par = param;

		m_originX = originX;
		m_originY = originY;
		m_width = map.getMapWidth() / m_par.DISTANCE_MAP_RESOLUTION;
		m_height = map.getMapHeight() / m_par.DISTANCE_MAP_RESOLUTION;
		
		m_map = map;
		m_distanceMap = new double[m_width][m_height];
		
		for (int x=0; x<m_width; x++)
		{
			for (int y=0; y<m_height; y++)
			{
				m_distanceMap[x][y]= c_empty;
				
				for (int dx=0; dx<m_par.DISTANCE_MAP_RESOLUTION; dx++)
					for (int dy=0; dy<m_par.DISTANCE_MAP_RESOLUTION; dy++)
						if (m_map.isObstacle(x*m_par.DISTANCE_MAP_RESOLUTION+dx, y*m_par.DISTANCE_MAP_RESOLUTION+dy))
							m_distanceMap[x][y] = c_wall;
			}
		}
		
		makeWallsFatter();

		floodFill(originX/m_par.DISTANCE_MAP_RESOLUTION, originY/m_par.DISTANCE_MAP_RESOLUTION);
	}
	
	public DistanceMapFloodFiller(Parameters param, Map map, int originX, int originY)
	{
		init(param, map, originX, originY);
	}
	
	public DistanceMapFloodFiller(Parameters param, Map map, Vector2d origin)
	{
		init(param, map, (int)origin.x, (int)origin.y);
	}
	
	void makeWallsFatter()
	{
		int wallThickness;
		
		switch (m_par.DISTANCE_MAP_RESOLUTION)
		{
		case 1: wallThickness = 3; break;
		case 2: wallThickness = 2; break;
		case 3: wallThickness = 1; break;
		default: return; // no need to make the walls fatter at this scale
		}
		
		int wallsToAdd1 = 0;
		int wallsToAdd2 = 0; 
		
		for (int x=0; x<m_width; x++)
		{
			for (int y=0; y<m_height; y++)
			{
				if (m_distanceMap[x][y] == c_wall)
				{
					wallsToAdd1 = wallThickness;
				}
				if (m_distanceMap[x][m_height-y-1] == c_wall)
				{
					wallsToAdd2 = wallThickness;
				}
				
				if (wallsToAdd1 > 0)
				{
					m_distanceMap[x][y] = c_wall;
					wallsToAdd1--;
				}
				
				if (wallsToAdd2 > 0)
				{
					m_distanceMap[x][m_height-y-1] = c_wall;
					wallsToAdd2--;
				}
				
				
			}
		}
		wallsToAdd1 = 0;
		wallsToAdd2 = 0; 
		
		for (int y=0; y<m_height; y++)
		{
			for (int x=0; x<m_width; x++)
			{
				if (m_distanceMap[x][y] == c_wall)
				{
					wallsToAdd1 = wallThickness;
				}
				if (m_distanceMap[m_width-x-1][y] == c_wall)
				{
					wallsToAdd2 = wallThickness;
				}
				if (wallsToAdd1 > 0)
				{
					m_distanceMap[x][y] = c_wall;
					wallsToAdd1--;
				}
				
				if (wallsToAdd2 > 0)
				{
					m_distanceMap[m_width-x-1][y] = c_wall;
					wallsToAdd2--;
				}
			}
		}
	}
	
	public double getDistance(int x, int y)
	{
		// We know the origin has distance 0
		if (x == m_originX && y == m_originY)
			return 0;
		
		// The four interpolation points are named for the diagonal compass directions
		// If x is exactly on a distance map pixel coordinate we set west = east, likewise for y and north = south. 
		
		int xWest = x / m_par.DISTANCE_MAP_RESOLUTION;
		int xEast = (x % m_par.DISTANCE_MAP_RESOLUTION == 0) ? xWest : xWest+1;

		int yNorth = y / m_par.DISTANCE_MAP_RESOLUTION;
		int ySouth = (y % m_par.DISTANCE_MAP_RESOLUTION == 0) ? yNorth : yNorth+1;

		// Check that all coordinates are in bounds
		if (xWest < 0 || yNorth < 0 || xEast >= m_width || ySouth >= m_height)
			return c_wall;
		
		// Look up the four values
		double northWest = m_distanceMap[xWest][yNorth];
		double northEast = m_distanceMap[xEast][yNorth];
		double southWest = m_distanceMap[xWest][ySouth];
		double southEast = m_distanceMap[xEast][ySouth];
		
		// If any point is a wall, call this a wall too
		if (northWest == c_wall || northEast == c_wall || southWest == c_wall || southEast == c_wall)
			return c_wall;
		
		// Coordinates (in [0,1]) of the point within the distance map pixel
		double fracx = (double)(x - m_par.DISTANCE_MAP_RESOLUTION * xWest)  / m_par.DISTANCE_MAP_RESOLUTION;
		double fracy = (double)(y - m_par.DISTANCE_MAP_RESOLUTION * yNorth) / m_par.DISTANCE_MAP_RESOLUTION;
		
		// Bilinear interpolation 
		double dist = northWest * (1-fracx)*(1-fracy)
					+ northEast * (  fracx)*(1-fracy) 
					+ southWest * (1-fracx)*(  fracy) 
					+ southEast * (  fracx)*(  fracy);
		
		assert(dist >= 0);
		
		// Scale the distance up (so we don't have to worry about m_par.DISTANCE_MAP_RESOLUTION's effect on the driver evaluation function)
		dist *= m_par.DISTANCE_MAP_RESOLUTION;
		
		// Ensure only the origin point has a distance of 0
		if (dist == 0) dist = 0.1 * (Math.abs(x-m_originX) + Math.abs(y-m_originY));
		
		return dist;
	}
	
	public double getDistance(Vector2d p)
	{
		return getDistance((int)p.x, (int)p.y);
	}
	
	class QueueItem implements Comparable<QueueItem>
	{
		public int x,y;
		private  double d;
		public QueueItem(int xx, int yy, double dd) { x=xx; y=yy; d=dd; }
		
		@Override
		public int compareTo(QueueItem o)
		{
			return -Double.compare(this.d, o.d);
		}
	}
	
	Queue<QueueItem> q;
	
	// Leave this as 1 for now -- values > 1 will cause it to miss 1-pixel obstacles! 
	static final int RADIUS = 1;
	
	static double[][] s_radiusDistances = null;
	
	double getDist(int x1, int y1, int x2, int y2)
	{
		if (s_radiusDistances == null)
		{
			s_radiusDistances = new double[RADIUS*2+1][RADIUS*2+1];
			
			for (int rx=-RADIUS; rx<=RADIUS; rx++) for (int ry=-RADIUS; ry<=RADIUS; ry++)
			{
				s_radiusDistances[rx+RADIUS][ry+RADIUS] = Math.sqrt(rx*rx + ry*ry);
			}
		}
		
		if (x1==x2 && y1==y2)
			return 0;
		
		double cost = 1.0;
		if (m_map.isLava(x1*m_par.DISTANCE_MAP_RESOLUTION, y1*m_par.DISTANCE_MAP_RESOLUTION))
			cost += m_par.FLOODFILL_LAVA_WEIGHT / 2;
		
		if (m_map.isLava(x2*m_par.DISTANCE_MAP_RESOLUTION, y2*m_par.DISTANCE_MAP_RESOLUTION))
			cost += m_par.FLOODFILL_LAVA_WEIGHT / 2;
		
		return s_radiusDistances[x2-x1+RADIUS][y2-y1+RADIUS] * cost;
	}
	
	double getMinDist(int x, int y)
	{
		double result = m_distanceMap[x][y];
		
		for (int rx=-RADIUS; rx<=RADIUS; rx++) for (int ry=-RADIUS; ry<=RADIUS; ry++)
		{
			if (rx==0 && ry==0) continue;
			if (x+rx < 0 || x+rx >= m_width || y+ry < 0 || y+ry >= m_height) continue;
			
			double d = m_distanceMap[x+rx][y+ry];
			if (d == c_wall) continue;

			d += getDist(x, y, x+rx, y+ry);
			
			if (d < result) result = d;
		}
		
		return result;
	}
	
	void scan(int x, int y, int dx, boolean queuedAbove, boolean queuedBelow)
	{
		if (x < 0 || x >= m_width) return;
		
		x += dx;
		double d = getMinDist(x, y);
		
		int step = 0;
		while (x >= 0 && x < m_width && m_distanceMap[x][y] > d)
		{
			m_distanceMap[x][y] = d;
			
			if (y > 0)
			{
				//double d2 = d + (m_map.isLava(x*m_par.DISTANCE_MAP_RESOLUTION, (y-1)*m_par.DISTANCE_MAP_RESOLUTION) ? m_par.FLOODFILL_LAVA_WEIGHT : 1);
				double d2 = d + getDist(x,y, x,y-1);
				
				if (m_distanceMap[x][y-1] > d2)
				{
					if (!queuedAbove)
					{
						q.add(new QueueItem(x, y-1, d2));
						queuedAbove = true;
					}
				}
				else
					queuedAbove = false;
			}
			
			if (y < m_height-1)
			{
				//double d2 = d + (m_map.isLava(x*m_par.DISTANCE_MAP_RESOLUTION, (y+1)*m_par.DISTANCE_MAP_RESOLUTION) ? m_par.FLOODFILL_LAVA_WEIGHT : 1);
				double d2 = d + getDist(x,y, x,y+1);

				if (m_distanceMap[x][y+1] > d2)
				{
					if (!queuedBelow)
					{
						q.add(new QueueItem(x, y+1, d2));
						queuedBelow = true;
					}
				}
				else
					queuedBelow = false;
			}
	
			x += dx;
			step++;
			
			d = getMinDist(x, y);
		}
	}
	
	//static final boolean WRITE_DEBUG_MOVIE = true;
	static final boolean WRITE_DEBUG_MOVIE = false;
	static final int DEBUG_MOVIE_FRAME_LIMIT = 10000;
	
	static boolean WRITE_DEBUG_IMAGES = false;	
	
	void floodFill(int x, int y)
	{
		q = new LinkedList<QueueItem>();
		q.add(new QueueItem(x, y, 0));
		
		int step = 0;
		
		/*MJPEGGenerator debugMovie = null;
		if (WRITE_DEBUG_MOVIE)
			try {
				debugMovie = new MJPEGGenerator(new File("floodFill_debug.avi"), m_width, m_height, 10, DEBUG_MOVIE_FRAME_LIMIT);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		
		for (QueueItem n = q.poll(); n != null; n = q.poll())
		{
			if (WRITE_DEBUG_IMAGES)
				dump(String.format("C:\\Users\\Ed\\Desktop\\flood\\step%06d.png", step++), n, false);
			
			if (m_distanceMap[n.x][n.y] > n.d)
			{
				/*if (debugMovie != null)
				{
					System.out.print("Writing frame "); System.out.println(step);
					
					try {
						BufferedImage bi = dumpImage();
						bi.setRGB(n.x, n.y, 0xFFFFFF);
						debugMovie.addImage(bi);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					step++;
					if (step > DEBUG_MOVIE_FRAME_LIMIT)
					{
						System.out.println("Finished debug movie");
						
						try {
							debugMovie.finishAVI();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						debugMovie = null;
					}
				}*/
				
				m_distanceMap[n.x][n.y] = n.d;
				
				boolean queuedAbove = false;
				boolean queuedBelow = false;
				
				if (n.y > 0)
				{
					double d2 = getMinDist(n.x, n.y-1);
					queuedAbove = (m_distanceMap[n.x][n.y-1] > d2);
					if (queuedAbove)
						q.add(new QueueItem(n.x, n.y-1, d2));
				}
				
				if (n.y < m_height-1)
				{
					double d2 = getMinDist(n.x, n.y+1);
					queuedBelow = (m_distanceMap[n.x][n.y+1] > d2);
					if (queuedBelow)
						q.add(new QueueItem(n.x, n.y+1, d2));
				}
				
				scan(n.x, n.y, -1, queuedAbove, queuedBelow);
				scan(n.x, n.y, +1, queuedAbove, queuedBelow);
			}
		}
		
		/*if (debugMovie != null)
		{
			System.out.println("Finished debug movie");
			
			try {
				debugMovie.finishAVI();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			debugMovie = null;
		}*/
	}
	
	public BufferedImage dumpImage(boolean greyscale)
	{
		BufferedImage bi = new BufferedImage(m_width, m_height, BufferedImage.TYPE_INT_RGB);
		
		for (int y=0; y<m_height; y++)
		{
			for (int x=0; x<m_width; x++)
			{
				Color color;
				
				if (greyscale)
				{
					if (m_distanceMap[x][y] == c_wall)
					{
						color = Color.darkGray;
					}
					else
					{
						if (m_map.isLava(x*m_par.DISTANCE_MAP_RESOLUTION, y*m_par.DISTANCE_MAP_RESOLUTION))
							color = Color.lightGray;
						else
							color = Color.white;
							
						double d = m_distanceMap[x][y];
						d = d - (int)(d/25)*25;
						
						if (d < 3)
						{
							color = new Color((float)d/3, (float)d/3, (float)d/3);
						}
					}
				}
				else
				{
					if (m_distanceMap[x][y] == c_wall)
						color = Color.gray;
					else if (m_distanceMap[x][y] == c_empty)
					{
						if (m_map.isLava(x*m_par.DISTANCE_MAP_RESOLUTION, y*m_par.DISTANCE_MAP_RESOLUTION))
							color = Color.orange.darker().darker();
						else
							color = Color.black;
					}
					else
						color = new Color(Color.HSBtoRGB((float)m_distanceMap[x][y] / 200.0f, 1.0f, 0.5f));
					
					if (m_map.isLava(x*m_par.DISTANCE_MAP_RESOLUTION, y*m_par.DISTANCE_MAP_RESOLUTION))
						color = color.brighter();
				}
				
				bi.setRGB(x, y, color.getRGB());
				
			}
		}
		
		for (QueueItem n : q)
		{
			bi.setRGB(n.x, n.y, Color.white.getRGB());
		}
		
		return bi;
	}
	
	public BufferedImage dumpInterpolatedImage()
	{
		BufferedImage bi = new BufferedImage(m_map.getMapWidth(), m_map.getMapHeight(), BufferedImage.TYPE_INT_RGB);
		
		for (int y=0; y<m_map.getMapHeight(); y++)
		{
			for (int x=0; x<m_map.getMapWidth(); x++)
			{
				Color color;
				double dist = getDistance(x, y);
				if (dist == c_wall)
					color = Color.gray;
				else if (dist == c_empty)
				{
					if (m_map.isLava(x, y))
						color = Color.orange.darker().darker();
					else
						color = Color.black;
				}
				else
					color = new Color(Color.HSBtoRGB((float)dist / 200.0f, 1.0f, 0.5f));
				
				if (m_map.isLava(x, y))
					color = color.brighter();
					
				bi.setRGB(x, y, color.getRGB());
				
			}
		}
		
		return bi;
	}
	
	void dump(String filename, QueueItem currentItem, boolean greyscale)
	{
		try
		{
			BufferedImage image = dumpImage(greyscale);
			if (currentItem != null)
				image.setRGB(currentItem.x, currentItem.y, Color.white.getRGB());
			ImageIO.write(image, filename.substring(filename.length() - 3), new File(filename));
			//ImageIO.write(dumpGradientImage(), "png", new File("gradient_" + filename));
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void dumpInterpolated(String filename)
	{
		try
		{
			BufferedImage image = dumpInterpolatedImage();
			ImageIO.write(image, filename.substring(filename.length() - 3), new File(filename));
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
