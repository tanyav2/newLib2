package edu.illinois.mitra.cyphyhouse.interfaces;

import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;
import edu.illinois.mitra.cyphyhouse.objects.Point3d;
import edu.illinois.mitra.cyphyhouse.objects.PositionList;

public interface TrackedRobot {
	void initialize();
	Point3d predict(double[] noises, double timeSinceUpdate);
	void collision(Point3d collision_point);
	void updatePos(boolean followPredict);
	boolean inMotion();
	void updateSensor(ObstacleList obspoint_positions, PositionList<ItemPosition> sensepoint_positions);
}
