package testSim.RaceApp.src.edu.illinois.mitra.demo.race;

import testSim.main.SimSettings;
import testSim.main.Simulation;

import static java.lang.Integer.parseInt;

/**
 * Created by kek on 7/11/17.
 */
public class Main {
    public static void main(String[] args) {
        SimSettings.Builder settings = new SimSettings.Builder();
        settings.OBSPOINT_FILE("Obstacles.wpt");
        settings.N_IROBOTS(parseInt(args[0]));
        settings.N_CARS(parseInt(args[1]));
        settings.N_QUADCOPTERS(parseInt(args[2]));
        settings.GPS_POSITION_NOISE(4);
        settings.TIC_TIME_RATE(0.5);
        settings.WAYPOINT_FILE("four1.wpt");
        settings.INITIAL_POSITIONS_FILE("start.wpt");
        settings.DRAW_TRACE_LENGTH(-1);
        settings.DRAW_WAYPOINTS(false);
        settings.DRAW_WAYPOINT_NAMES(false);
        settings.DRAWER(new RaceDrawer());
        settings.DRAW_TRACE(true);
        settings.DRAW__ROBOT_TYPE(true);

        Simulation sim = new Simulation(RaceApp.class, settings.build());
        sim.start();
    }
}
