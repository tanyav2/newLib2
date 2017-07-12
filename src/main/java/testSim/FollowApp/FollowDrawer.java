package testSim.FollowApp;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import edu.illinois.mitra.cyphyhouse.interfaces.LogicThread;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import testSim.draw.Drawer;

/**
 * This class draws the waypoints and creates a halo around
 * the waypoints that are currently the destination of any robots
 */
public class FollowDrawer extends Drawer {

	// Color and stroke of the color surrounding each waypoint
	private Stroke stroke = new BasicStroke(20);
	private Color selectColor = new Color(0,200,255,100);
	
	@Override
	public void draw(LogicThread logicThread, Graphics2D g) {
		FollowApp app = (FollowApp) logicThread;

		// Sets the color of the waypoints
		g.setColor(Color.RED);
		for(ItemPosition dest : app.destinations.values()) {
			g.fillOval(dest.getX() - 13, dest.getY() - 13, 40, 40);
		}

		// Sets the color and stroke of the halo surrrounding the waypoint
		g.setColor(selectColor);
		g.setStroke(stroke);

		// If this waypoint is the destination of a robot, then the halo will be drawn
        // Else if no one is going towards it, only the solid waypoint will be drawn
		if(app.currentDestination != null)
			g.drawOval(app.currentDestination.getX() - 25, app.currentDestination.getY() - 25, 70, 70);
	}

}
