package testSim.RaceApp.src.edu.illinois.mitra.demo.race;

import edu.illinois.mitra.cyphyhouse.interfaces.LogicThread;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;
import edu.illinois.mitra.cyphyhouse.objects.Obstacles;
import edu.illinois.mitra.cyphyhouse.objects.Point3d;
import testSim.draw.Drawer;

import java.awt.*;

/**
 * Created by kek on 7/11/17.
 */
public class RaceDrawer extends Drawer {
    private Stroke stroke = new BasicStroke(8);
    private Color selectColor = new Color(0,0,255,100);

    @Override
    public void draw(LogicThread lt, Graphics2D g) {
        RaceApp app = (RaceApp) lt;

        g.setColor(Color.RED);

        int i = 0;
        for(ItemPosition dest : app.destinationsHistory) {
            i++;
            g.fillRect(dest.getX() - 13, dest.getY() - 13, 26, 26);
            g.drawString(dest.name, dest.getX() + 30, dest.getY() - 20);
        }

        g.setColor(Color.GRAY);
        ObstacleList list = app.obs;
        for(int k = 0; k < list.ObList.size(); k++)
        {
            Obstacles currobs = list.ObList.get(k);
            if(currobs.hidden)
                g.setColor(Color.LIGHT_GRAY);
            else
                g.setColor(Color.GRAY);

            Point3d nextpoint = currobs.obstacle.firstElement();
            Point3d curpoint = currobs.obstacle.firstElement();
            int[] xs = new int[currobs.obstacle.size()];
            int[] ys = new int[currobs.obstacle.size()]; ;

            for(int j = 0; j < currobs.obstacle.size() -1 ; j++){
                curpoint = currobs.obstacle.get(j);
                nextpoint = currobs.obstacle.get(j+1);
                g.drawLine(curpoint.x, curpoint.y, nextpoint.x, nextpoint.y);
                xs[j] = curpoint.x;
                ys[j] = curpoint.y;
            }
            xs[currobs.obstacle.size()-1] = nextpoint.x;
            ys[currobs.obstacle.size()-1] = nextpoint.y;

            g.drawLine(nextpoint.x, nextpoint.y, currobs.obstacle.firstElement().x, currobs.obstacle.firstElement().y);
            g.fillPolygon(xs,ys,currobs.obstacle.size());
        }

        g.setColor(selectColor);
        g.setStroke(stroke);
        if(app.currentDestination != null)
            g.drawOval(app.currentDestination.getX() - 20, app.currentDestination.getY() - 20, 40, 40);
        g.setColor(Color.BLACK);
    }
}
