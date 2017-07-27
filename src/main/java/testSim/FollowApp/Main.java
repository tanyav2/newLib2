package testSim.FollowApp;

import testSim.main.SimSettings;
import testSim.main.Simulation;

import static java.lang.Integer.parseInt;

/**
 * Created by kek on 6/21/17.
 */
public class Main {
    public static void main(String[] args) {

        // Initializes the settings for running the simulation
        SimSettings.Builder settings = new SimSettings.Builder();

        // Pick numbers of robots
        // Note that N_IROBOTS defaults to 4
        settings.N_IROBOTS(parseInt(args[0]));
        settings.N_CARS(parseInt(args[1]));
        settings.N_QUADCOPTERS(parseInt(args[2]));

        // Pick how fast you want the simulation to run
        settings.TIC_TIME_RATE(2);

        // Pick waypoints
        settings.WAYPOINT_FILE("3dobstaclepoints.wpt");
        settings.OBSPOINT_FILE("square.wpt");

        // No idea what it does
        // NOTE : if set to true or commented out, simulation hangs and no movement happens
        // if set to false, simulation works as expected
        settings.DRAW_WAYPOINTS(false);

        // Gives each waypoint a name if set to true
        settings.DRAW_WAYPOINT_NAMES(false);

        // Apparently if you remove this line or set argument to null, the waypoint circles
        // arent drawn
        // Else if (new FollowDrawer()) is passed in, things work as expected
        settings.DRAWER(new FollowDrawer());

        // Instantiate and start the simulation thread
        Simulation sim = new Simulation(FollowApp.class, settings.build());
        sim.start();
    }
}
