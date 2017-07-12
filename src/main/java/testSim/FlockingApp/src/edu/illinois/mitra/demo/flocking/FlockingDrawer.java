package testSim.FlockingApp.src.edu.illinois.mitra.demo.flocking;

import edu.illinois.mitra.cyphyhouse.interfaces.LogicThread;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import testSim.draw.Drawer;

import java.awt.*;

/**
 * Created by kek on 7/11/17.
 */
public class FlockingDrawer extends Drawer {

    private Stroke stroke = new BasicStroke(8);
    private Color selectColor = new Color(0,0,255,100);

    @Override
    public void draw(LogicThread lt, Graphics2D g) {
        FlockingApp app = (FlockingApp) lt;

        g.setColor(Color.RED);
        for(ItemPosition dest : app.destinations.values()) {
            g.fillRect(dest.getX() - 13, dest.getY() - 13, 26, 26);
        }

        g.setColor(selectColor);
        g.setStroke(stroke);
        if(app.currentDestination != null)
            g.drawOval(app.currentDestination.getX() - 20, app.currentDestination.getY() - 20, 40, 40);
    }

}
