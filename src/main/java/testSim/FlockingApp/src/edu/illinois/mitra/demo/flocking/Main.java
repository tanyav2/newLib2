package testSim.FlockingApp.src.edu.illinois.mitra.demo.flocking;

import testSim.main.SimSettings;
import testSim.main.Simulation;

import static java.lang.Integer.parseInt;

/**
 * Created by kek on 7/11/17.
 */
public class Main {

    public static void main(String[] args) {

        SimSettings.Builder settings = new SimSettings.Builder();
//        settings.N_IROBOTS(0);
//        settings.N_CARS(2); // pick N reasonably large (> ~10) for rotations along arcs instead of going across middle always

        settings.N_IROBOTS(parseInt(args[0]));
        settings.N_CARS(parseInt(args[1]));
        settings.N_QUADCOPTERS(parseInt(args[2]));

        settings.TIC_TIME_RATE(5);
        settings.WAYPOINT_FILE("four.wpt");
        //settings.WAYPOINT_FILE(System.getProperty("user.dir")+"\\trunk\\android\\RaceApp\\waypoints\\four1.wpt");
        settings.DRAW_WAYPOINTS(false);
        settings.DRAW_WAYPOINT_NAMES(false);
        settings.DRAWER(new FlockingDrawer());

        Simulation sim = new Simulation(FlockingApp.class, settings.build());
        sim.start();
    }
}
