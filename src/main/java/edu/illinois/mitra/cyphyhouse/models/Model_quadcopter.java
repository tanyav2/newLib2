package edu.illinois.mitra.cyphyhouse.models;

import java.util.Random;

import edu.illinois.mitra.cyphyhouse.exceptions.ItemFormattingException;
import edu.illinois.mitra.cyphyhouse.interfaces.TrackedRobot;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;
import edu.illinois.mitra.cyphyhouse.objects.Point3d;
import edu.illinois.mitra.cyphyhouse.objects.PositionList;
/**
 * This class represents a simple model of the quadcopter
 * @author Yixiao Lin
 * @version 1.0
 */

public class Model_quadcopter extends ItemPosition implements TrackedRobot{

	// for default values, see initial_helper()
	public static double yaw;
	public static double pitch;
	public static double roll;

	//vertical speed
	public double gaz;
	public int radius;

	// mass in kilograms
	public double mass;
	public int height;

	public double v_x;
	public double v_y;
	public double v_z;

	public double v_yaw;

	public Random rand;

	public double v_yawR = 0;
	public double pitchR = 0;
	public double rollR = 0;
	public double gazR = 0;

	// the p represents predictions
    // after adding in noise
	private int x_p = 0;
	private int y_p = 0;
	private int z_p = 0;

	private double yaw_p = 0.0;
	private double pitch_p = 0.0;
	private double roll_p = 0.0;

	private double v_yaw_p = 0.0;

	private double v_x_p = 0.0;
	private double v_y_p = 0.0;
	private double v_z_p = 0.0;

	// platform specific control parameters: see page 78 of http://www.msh-tools.com/ardrone/ARDrone_Developer_Guide.pdf 
	public double max_gaz = 1000; // millimeter/s 200 to 2000 
	public double max_pitch_roll = 20;  // in degrees  
	public double max_yaw_speed = 200;  // degrees/s
	public double windx = 0;   // millimeter/s
	public double windy = 0;
	public double windt = 0;
	public double windxNoise;
	public double windyNoise;

    public boolean leftbump;
    public boolean rightbump;

	/**
	 * Construct an Model_quadcopter from a received GPS broadcast message
	 * 
	 * @param received GPS broadcast received 
	 * @throws ItemFormattingException
	 */

	public Model_quadcopter(String received) throws ItemFormattingException {
		initial_helper();
		String[] parts = received.replace(",", "").split("\\|");
		if(parts.length == 9) {
			this.name = parts[1];
			this.x = Integer.parseInt(parts[2]);
			this.y = Integer.parseInt(parts[3]);
			this.z = Integer.parseInt(parts[4]);
			this.yaw = Integer.parseInt(parts[5]);
			this.pitch = Integer.parseInt(parts[6]);
			this.roll = Integer.parseInt(parts[7]);
			System.out.println("wyoming is a good place: received: " + received);
		} else {
			throw new ItemFormattingException("Should be length 9, is length " + parts.length);
		}
	}

	public Model_quadcopter(String name, int x, int y) {
		super(name, x, y, 0);
        System.out.println("wyoming is a good place 2");
		initial_helper();
	}

	public Model_quadcopter(String name, int x, int y, int z) {
		super(name, x, y, z);
        System.out.println("wyoming is a good place 3");
		initial_helper();
	}

	public Model_quadcopter(String name, int x, int y, int z, double yaw, double pitch, double roll, int radius) {
		super(name, x, y, z);
		initial_helper();
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
		this.radius = radius;
        System.out.println("wyoming is a good place 4");
	}

	public Model_quadcopter(String name, int x, int y, int z, int yaw) {
		super(name, x, y, z);
		initial_helper();
		this.yaw = yaw;
        System.out.println("wyoming is a good place 5");
	}


	public Model_quadcopter(ItemPosition t_pos) {
		super(t_pos.name, t_pos.x, t_pos.y, t_pos.z);
		initial_helper();
        System.out.println("wyoming is a good place 6 - item pos: " + t_pos);

	}

	@Override 
	public String toString() {
		return name + ": " + x + ", " + y + ", " + z + "; yaw, pitch, roll, vertical_speed: " + yaw + ", " + pitch + ", " + roll + " ," + gaz;
	}


	private void initial_helper(){
		height = 50;
		yaw = 0.0;
		pitch = 0.0;
		roll = 0.0;
		radius = 340;
		mass = 0.5; // default mass is 500 grams
		v_x = 0.0;
		v_y = 0.0;
		v_z = 0.0;
		v_yaw = 0.0;
		gaz = 0.0;
	}

	@Override
	public Point3d predict(double[] noises, double timeSinceUpdate) {

		if(noises.length != 3){
			System.out.println("Incorrect number of noises parameters passed in, please pass in x, y, z, yaw, pitch, roll noises");
			return new Point3d(x,y,z);
		}

		v_yaw += (v_yawR - v_yaw) * timeSinceUpdate;
		pitch += (pitchR - pitch) * timeSinceUpdate;
		roll += (rollR-roll) * timeSinceUpdate;
		gaz += (gazR- gaz) * timeSinceUpdate;

		double xNoise = (rand.nextDouble() * 2 * noises[0]) - noises[0];
		double yNoise = (rand.nextDouble() * 2 * noises[0]) - noises[0];
		double zNoise = (rand.nextDouble() * 2 * noises[0]) - noises[0];
		double yawNoise = (rand.nextDouble() * 2 * noises[1]) - noises[1];

		windt += timeSinceUpdate;
		windxNoise = xNoise + windx * Math.sin(windt);
		windyNoise = yNoise + windy * Math.sin(windt);

		//TODO: correct the model

		// speed is in millimeter/second
		// mass in kilograms
		// each pixel is 1 millimeter
		// timeSinceUpdate is in second
		int dX = (int)(xNoise + v_x*timeSinceUpdate + windxNoise);
		int dY = (int)(yNoise + v_y*timeSinceUpdate + windyNoise);
		int dZ = (int)(zNoise + gaz*timeSinceUpdate);

		x_p = x + dX;
		y_p = y + dY;
		z_p = z + dZ;

		double thrust;
		if((mass * Math.cos(Math.toRadians(roll)) * Math.cos(Math.toRadians(pitch))) != 0){
			thrust = ((gaz + 1000) / (mass * Math.cos(Math.toRadians(roll))) / (Math.cos(Math.toRadians(pitch))));
		} else{
			thrust = 1000;
		}

		double dv_x = - (thrust * Math.sin(Math.toRadians(roll) * Math.sin(Math.toRadians(yaw)) + Math.cos(Math.toRadians(roll)) * Math.sin(Math.toRadians(pitch)) * Math.cos(Math.toRadians(yaw))))/ (mass) ;
		double dv_y = (thrust * (Math.sin(Math.toRadians(roll)) * Math.cos(Math.toRadians(yaw)) - Math.cos(Math.toRadians(roll)) * Math.sin(Math.toRadians(pitch)) * Math.sin(Math.toRadians(yaw))))/ (mass) ;

		v_x_p = v_x + dv_x * timeSinceUpdate;
		v_y_p = v_y + dv_y * timeSinceUpdate;
		v_z_p = gaz;

		double dYaw = (v_yaw * timeSinceUpdate);
		yaw_p = (yaw + dYaw) % 360;

		return new Point3d(x_p, y_p, z_p);
	}

	@Override
	public void collision(Point3d collision_point) {

		// No collision point, set both sensor to false
		if(collision_point == null){
			return;
		} else{
		    System.out.println("someone hurt me but i can deal with it" + collision_point);
			gaz = -30;
		}
	}

	@Override
	public void updatePos(boolean followPredict) {
		if(followPredict){
			x = x_p;
			y = y_p;
			z = z_p;
			yaw = yaw_p;
			v_yaw = v_yaw_p;
			v_x = v_x_p;
			v_y = v_y_p;
			v_z = v_z_p;
		}
		else{
			z = z_p;
			v_z = v_z_p;
			if(z < 20){
				roll = 0;
				pitch = 0;
			}
		}
		if(z < 0){
			z = 0;
			v_z = 0;
		}
	}

	@Override
	public boolean inMotion() {
		return (v_x != 0 || v_y != 0 || v_z != 0 || v_yaw != 0);
	}

	@Override
	public void initialize() {
		rand = new Random(); //initialize random variable for TrackedRobot
	}

	@Override
	public void updateSensor(ObstacleList obspoint_positions,
			PositionList<ItemPosition> sensepoint_positions) {
		return;
		// no sensor model yet
	}
}
