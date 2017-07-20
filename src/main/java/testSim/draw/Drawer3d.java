package testSim.draw;

import edu.illinois.mitra.cyphyhouse.interfaces.LogicThread;

import java.awt.*;

/**
 * Created by kek on 7/19/17.
 */
public abstract class Drawer3d {

    /**
     * Draw the (logic-thread specific) data onto the simulator image
     * @param lt the instance we need to draw
     * @param g the graphics to draw with
     */
    public abstract void draw(LogicThread lt, Graphics2D g);
}
