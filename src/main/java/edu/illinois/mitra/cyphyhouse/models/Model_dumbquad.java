package edu.illinois.mitra.cyphyhouse.models;

import edu.illinois.mitra.cyphyhouse.interfaces.TrackedRobot;
import edu.illinois.mitra.cyphyhouse.objects.*;

import javax.sound.midi.Track;

/**
 * Created by kek on 7/19/17.
 */
public class Model_dumbquad extends ItemPosition implements TrackedRobot{

    private Point curr;
    private Point init;
    private Point dest;

    private double velocity;

    private double logInterval;

    public Model_dumbquad(Point init, Point dest, double velocity) {
        this.init = init;
        this.curr = init;
        this.dest = dest;
        this.velocity = velocity;
        this.logInterval = 10;
    }

    @Override
    public void initialize() {

    }

    @Override
    public Point3d predict(double[] noises, double timeSinceUpdate) {
        return null;
    }

    @Override
    public void collision(Point3d collision_point) {

    }

    @Override
    public void updatePos(boolean followPredict) {

    }

    @Override
    public boolean inMotion() {
        return false;
    }

    @Override
    public void updateSensor(ObstacleList obspoint_positions, PositionList<ItemPosition> sensepoint_positions) {

    }


}
