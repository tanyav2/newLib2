package edu.illinois.mitra.cyphyhouse.harness;

import java.util.Observer;
import java.util.Vector;

import edu.illinois.mitra.cyphyhouse.interfaces.TrackedRobot;
import edu.illinois.mitra.cyphyhouse.models.ModelF110Car;
import edu.illinois.mitra.cyphyhouse.models.Model_iRobot;
import edu.illinois.mitra.cyphyhouse.models.Model_quadcopter;
import edu.illinois.mitra.cyphyhouse.objects.*;
import javafx.geometry.Pos;

public interface SimGpsProvider {

	public abstract void registerReceiver(String name,
			SimGpsReceiver simGpsReceiver);

	public abstract void addRobot(TrackedRobot bot);

	// Implemented only by ideal gps provider
	public abstract void setDestination(String name, ItemPosition dest, int vel);
	
	public abstract void setControlInput(String name, double v_yaw, double pitch, double roll, double gaz);

	// Implemented only be realistic gps provider
	public abstract void setVelocity(String name, int fwd, int rad);

	public abstract void setVelocityForCar(String name, int fwd, int rad);

	public abstract void halt(String name);

	public abstract PositionList<Model_iRobot> getiRobotPositions();

	public abstract PositionList<ModelF110Car> getF110CarPositions();
	
	public abstract PositionList<Model_quadcopter> getQuadcopterPositions();
	
	public abstract PositionList<ItemPosition> getAllPositions();

	public abstract void setWaypoints(PositionList<ItemPosition> loadedWaypoints);
	
	public abstract void setSensepoints(PositionList<ItemPosition> loadedSensepoints);
	
	public abstract void setObspoints(ObstacleList loadedObspoints);

	public abstract void setViews(ObstacleList environment, int nBots);

	public abstract PositionList<ItemPosition> getWaypointPositions();
	
	public abstract PositionList<ItemPosition> getSensePositions();

	public abstract ObstacleList getObspointPositions();
	
	public abstract Vector<ObstacleList> getViews();
	
	public abstract void start();
	
	public abstract void addObserver(Observer o);

}