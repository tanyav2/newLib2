package testSim.FollowApp;

import java.awt.*;
import java.awt.geom.GeneralPath;

import edu.illinois.mitra.cyphyhouse.interfaces.LogicThread;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import testSim.draw.Drawer;

/**
 * This class draws the waypoints and creates a halo around
 * the waypoints that are currently the destination of any robots
 * Can represent points in 3d
 * Points can be either obstacles, or waypoints, or sensepoints
 * They are represented following the representation followed by vectors
 *
 */
public class FollowDrawer extends Drawer {

	// Color and stroke of the color surrounding each waypoint
	private Stroke stroke = new BasicStroke(20);
	private Color selectColor = new Color(0,200,255,100);
	
	@Override
	public void draw(LogicThread logicThread, Graphics2D g) {
		FollowApp app = (FollowApp) logicThread;

		// Sets the color of the waypoint

		g.setColor(Color.WHITE);
		for(ItemPosition dest : app.destinations.values()) {
		    if(dest.getZ() > 0) {
		        g.setColor(Color.CYAN);
                g.fillOval(dest.getX() - 13, dest.getY() - 13, 40, 40);
            } else if(dest.getZ() == 0) {
		        g.setColor(selectColor);
		        g.setStroke(stroke);
                g.drawOval(dest.getX() - 25, dest.getY() - 25, 70, 70);
            } else if(dest.getZ() < 0) {
		        g.setColor(Color.BLACK);
		        g.setStroke(new BasicStroke(10));
		        g.draw(createDiagonalCross(dest.getX(), dest.getY()));
            }
		}

		// If this waypoint is the destination of a robot, then the halo will be drawn
        // Else if no one is going towards it, only the solid waypoint will be drawn
        g.setStroke(stroke);
		if(app.currentDestination != null)
			g.drawOval(app.currentDestination.getX() - 25, app.currentDestination.getY() - 25, 70, 70);
	}


    /**
     * Creates a diagonal cross to represent the points going inside the page
     * @param x dest.getX()
     * @param y dest.getY()
     * @return a shape that is a diagonal cross
     */
    public Shape createDiagonalCross(final float x, final float y) {
        final GeneralPath p0 = new GeneralPath();
        p0.moveTo(x, y);
        p0.lineTo(x+25, y+25);
        p0.moveTo(x,y);
        p0.lineTo(x-25, y-25);
        p0.moveTo(x, y);
        p0.lineTo(x+25, y-25);
        p0.moveTo(x, y);
        p0.lineTo(x-25, y+25);
        p0.moveTo(x,y);
        return p0;
    }

}
