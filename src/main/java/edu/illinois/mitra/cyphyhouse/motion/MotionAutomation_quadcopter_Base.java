package edu.illinois.mitra.cyphyhouse.motion;

import java.util.*;

import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.interfaces.RobotEventListener.Event;
import edu.illinois.mitra.cyphyhouse.models.Model_quadcopter;
import edu.illinois.mitra.cyphyhouse.objects.*;

/**
 * This motion controller is for quadcopter models only
 * 
 * Motion controller which extends the RobotMotion abstract class. Capable of
 * going to destination or passing through a destination without stopping.
 * Includes optional collision avoidance which is controlled
 * by the motion parameters setting.
 *
 * 2017-2-9 Move to general Java env
 *  
 * @author Yixiao Lin, Shuchen
 * @version 1.1
 */
public class MotionAutomation_quadcopter_Base extends RobotMotion {
	protected static final String TAG = "MotionAutomaton";
	protected static final String ERR = "Critical Error";

    public static double currGaz;

	final int safeHeight = 500;

	protected GlobalVarHolder gvh;

	// Motion tracking
	protected ItemPosition destination;
	private Model_quadcopter mypos;
    private ItemPosition blocker;
    private ObstacleList obsList;


	// Added STAGEs - VERTICAL_ASC and DESC, YAW_LEFT, YAW_RIGHT
    // yaw left means move nose left and vice versa
    // pitch forward means move nose downward and vice versa
    // roll left (increase rpm on left motors which leads to a rise on the left and dip on right)
    // roll right (increase rpm on right motors etc)
	protected enum STAGE {
		INIT, MOVE, HOVER, TAKEOFF, LAND, GOAL, STOP,
        VERTICAL_ASCENT, VERTICAL_DESCENT, YAW_LEFT, YAW_RIGHT,
        PITCH_FORWARD, PITCH_BACKWARD,
        ROLL_LEFT, ROLL_RIGHT
	}

	private STAGE next = null;
	protected STAGE stage = STAGE.INIT;
	private STAGE prev = null;
	protected boolean running = false;
	boolean colliding = false;

	private enum OPMODE {
		GO_TO
	}

	private OPMODE mode = OPMODE.GO_TO;

	private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
	private volatile MotionParameters param = DEFAULT_PARAMETERS;
	//need to pass some more parameteres into this param
	//	MotionParameters.Builder settings = new MotionParameters.Builder();
	//	private volatile MotionParameters param = settings.build();


    // Collision avoidance
    private enum COLSTAGE {
        TURN, STRAIGHT
    }
    private COLSTAGE colprev = null;
    private COLSTAGE colstage = COLSTAGE.TURN;
    private COLSTAGE colnext = null;

    private enum COLSTAGE0 {
        BACK, STRAIGHT
    }
    private COLSTAGE0 colprev0 = null;
    private COLSTAGE0 colstage0 = COLSTAGE0.STRAIGHT;
    private COLSTAGE0 colnext0 = null;

    private enum COLSTAGE1 {
        BACK, STRAIGHT, TURN, SMALLARC
    }
    private COLSTAGE1 colprev1 = null;
    private COLSTAGE1 colstage1 = COLSTAGE1.STRAIGHT;
    private COLSTAGE1 colnext1 = null;


    private enum COLSTAGE2 {
        BACK, RANDOM, STRAIGHT
    }
    private COLSTAGE2 colprev2 = null;
    private COLSTAGE2 colstage2 = COLSTAGE2.BACK;
    private COLSTAGE2 colnext2 = null;

    private int col_straightime = 0;
    private int col_backtime = 0;
    private int col_turntime = 0;
    private int RanAngle = 0;



	public MotionAutomation_quadcopter_Base(GlobalVarHolder gvh) {
		super(gvh.id.getName());
		this.gvh = gvh;
	}

	public void goTo(ItemPosition dest, ObstacleList obsList) {
        if((inMotion && !this.destination.equals(dest)) || !inMotion) {
            done = false;
            this.destination = new ItemPosition(dest.name,dest.x,dest.y,dest.z);
//            Log.d(TAG, "Going to X: " + Integer.toString(dest.x) + " Y: " + Integer.toString(dest.y));
            //this.destination = dest;
            this.mode = OPMODE.GO_TO;
            this.obsList = obsList;
            startMotion();
        }
        // goTo(dest);
	}

	public void goTo(ItemPosition dest) {
//		if((inMotion && !this.destination.equals(dest)) || !inMotion) {
//			done = false;
//			this.destination = new ItemPosition(dest.name,dest.x,dest.y,dest.z);
//			this.mode = OPMODE.GO_TO;
//			startMotion();
//		}

        System.out.println("areyoumydest"+":"+dest);
        Scanner in = new Scanner(((Model_quadcopter)gvh.gps.getMyPosition()).name).useDelimiter("[^0-9]+");
        int index = in.nextInt();
        Vector<ObstacleList> temp = gvh.gps.getViews();
        ObstacleList obsList;
        if(!temp.isEmpty()) {
            obsList = temp.elementAt(index);
        }
        else {
            obsList = new ObstacleList();
        }
        //obsList = new ObstacleList();
        // work in progress here
        goTo(dest, obsList);
	}


	@Override
	public synchronized void start() {
		super.start();
		gvh.log.d(TAG, "STARTED!");
	}

	@Override
	public void run() {
		super.run();
		gvh.threadCreated(this);
		// some control parameters
		double kpx,kpy,kpz, kdx,kdy,kdz;

		// what are these magic numbers ??
		kpx = kpy = kpz = 0.00033;
		kdx = kdy = kdz = 0.0006;

		while(true) {

			if(running) {

			    // TODO Why is gvh.gps.getPos not used here - it's used in the car
				mypos = (Model_quadcopter) gvh.plat.getModel();
				System.out.println("mypos " + mypos.toString());

				// Distance between curr position and destination involving 3d coordinates
				int distance = (int) Math.sqrt(Math.pow((mypos.x - destination.x),2) + Math.pow((mypos.y - destination.y), 2));

				if(mypos.gaz < -50){
			        System.out.println("going down because mypos.gaz < -50");
				}

				// If the quadcopter is not trying to land but it's
                // about to hit the ground, set colliding to true

				// There are 4 ways in which colliding could be set to true

                // First - collision with ground ie if the quadcopter is not
                // trying to land but it's about to hit the ground, set colliding to true

                colliding = (stage != STAGE.LAND && mypos.gaz < -50);

//                switch(param.COLAVOID_MODE) {
//                    case USE_COLAVOID:
//                        colliding = collision_mem_less();
//                        break;
//                    default:
//                        colliding = false;
//                        break;
//                }

				// Second - collision with car/irobot/other ground obstacle

                // Third - collision with obstacles for z > 0

                // Fourth - collision with another quadcopter

				if(!colliding && stage != null) {
					switch(stage) {
					case INIT:
					    System.out.print("im in init-");
						if(mode == OPMODE.GO_TO) {
						    System.out.print("im in goto-");
							if(mypos.z < safeHeight){
								// just a safe distance from ground
                                System.out.println("im in takeoff");
								takeOff();
								next = STAGE.TAKEOFF;
							}
							else{
								if(distance <= param.GOAL_RADIUS) {
								    System.out.println("next stage is goal");
									next = STAGE.GOAL;
								}
								else{
								    System.out.println("next stage is move");
									next = STAGE.MOVE;
								}
							}
						}
						System.out.println("ifimtoocloseitsaproblem");
						break;
					case MOVE:
						if(mypos.z < safeHeight){
							// just a safe distance from ground
                            System.out.println("Im in takeoff zone of MOVE");
							takeOff();
							next = STAGE.TAKEOFF;
							break;
						}
						if(distance <= param.GOAL_RADIUS) {
                            System.out.println("Im in goal zone of MOVE");
							next = STAGE.GOAL;
						}
						else{
						    System.out.print("Im in math zone of MOVE--distance--"+distance+"--param.GOAL_RADIUS--"+param.GOAL_RADIUS);
							double Ax_d, Ay_d = 0.0;
							double Ryaw, Rroll, Rpitch, Rvs, Ryawsp = 0.0;
							Ax_d = (kpx * (destination.x - mypos.x) - kdx * mypos.v_x) ;
							Ay_d = (kpy * (destination.y - mypos.y) - kdy * mypos.v_y) ;
							Ryaw = Math.atan2(destination.y - mypos.y, destination.x - mypos.x);

							Ryawsp = kpz * ((Ryaw - Math.toRadians(mypos.yaw)));
							Rroll = Math.asin((Ay_d * Math.cos(Math.toRadians(mypos.yaw)) - Ax_d * Math.sin(Math.toRadians(mypos.yaw))) %1);
							Rpitch = Math.asin( (-Ay_d * Math.sin(Math.toRadians(mypos.yaw)) - Ax_d * Math.cos(Math.toRadians(mypos.yaw))) / (Math.cos(Rroll)) %1);
							Rvs = (kpz * (destination.z - mypos.z) - kdz * mypos.v_z);

							setControlInputRescale(Math.toDegrees(Ryawsp),Math.toDegrees(Rpitch)%360,Math.toDegrees(Rroll)%360,Rvs);
						}
						break;
					case HOVER:
						setControlInput(0,0,0, 0);
						System.out.println("Imma hovering now");
						// do nothing
						break;
					case TAKEOFF:
					    System.out.println("Im in case takeoff");
						switch(mypos.z/(safeHeight/2)){
						case 0:// 0 - 1/2 safeHeight
							setControlInput(0,0,0,1);
							break;
						case 1: // 1/2- 1 safeHeight
							setControlInput(0,0,0, 0.5);
							break;
						default: // above safeHeight:
							hover();
							if(prev != null){
								next = prev;
							}
							else{
								next = STAGE.HOVER;
							}
							break;
						}
						break;
                    case VERTICAL_ASCENT:
                        setControlInput(Model_quadcopter.yaw, Model_quadcopter.pitch, Model_quadcopter.roll, currGaz + 8);
                        currGaz += 8;
                        mypos.z += 8;
                        break;
                    case VERTICAL_DESCENT:
                        // check if it is safe to descend
                        if(mypos.gaz < -50){
                            //System.out.println("going down");
                        } else {
                            setControlInput(Model_quadcopter.yaw, Model_quadcopter.pitch, Model_quadcopter.roll, currGaz -8);
                            currGaz -= 8;
                            mypos.z -= 8;
                        }
                        break;
                    case YAW_LEFT:
                        setControlInput(-8 + Model_quadcopter.yaw, Model_quadcopter.pitch, Model_quadcopter.roll, currGaz);
                        Model_quadcopter.yaw -= 8;
                        break;
                    case YAW_RIGHT:
                        setControlInput(8 + Model_quadcopter.yaw, Model_quadcopter.pitch, Model_quadcopter.roll, currGaz);
                        Model_quadcopter.yaw += 8;
                         break;
                    case PITCH_FORWARD:
                        setControlInput(Model_quadcopter.yaw, 8 + Model_quadcopter.pitch, Model_quadcopter.roll, currGaz);
                        Model_quadcopter.pitch += 8;
                        break;
                    case PITCH_BACKWARD:
                        setControlInput(Model_quadcopter.yaw, -8 + Model_quadcopter.pitch, Model_quadcopter.roll, currGaz);
                        Model_quadcopter.pitch -= 8;
                        break;
                    case ROLL_LEFT:
                        setControlInput(Model_quadcopter.yaw, Model_quadcopter.pitch, -8 + Model_quadcopter.roll, currGaz);
                        Model_quadcopter.roll -= 8;
                        break;
                    case ROLL_RIGHT:
                        setControlInput(Model_quadcopter.yaw, Model_quadcopter.pitch, 8 + Model_quadcopter.roll, currGaz);
                        Model_quadcopter.roll += 8;
                        break;

					case LAND:
					    System.out.println("Im in landing zone");
						switch(mypos.z/(safeHeight/2)){
						case 0:// 0 - 1/2 safeHeight
							setControlInput(0,0,0,0);
							next = STAGE.STOP;
							break;
						case 1: // 1/2- 1 safeHeight
							setControlInput(0,0,0, -0.05);
							break;
						default:   // above safeHeight
							setControlInput(0,0,0,-0.5);
							break;
						}
						break;
					case GOAL:
					    System.out.println("Im in goal zone");
						done = true;
						gvh.log.i(TAG, "At goal!");
						gvh.log.i("DoneFlag", "write");
						if(param.STOP_AT_DESTINATION){
							hover();
							next = STAGE.HOVER;
						}
						running = false;
						inMotion = false;
						break;
					case STOP:
						gvh.log.i("FailFlag", "write");
						System.out.println("STOP");
						motion_stop();
						//do nothing
					}
					if(next != null) {
						prev = stage;
						stage = next;
						gvh.log.i(TAG, "Stage transition to " + stage.toString());
						gvh.trace.traceEvent(TAG, "Stage transition", stage.toString(), gvh.time());
					}
					next = null;
				} 

				if((colliding || stage == null) ) {
					gvh.log.i("FailFlag", "write");
					done = false;
					motion_stop();
				}
			}
			gvh.sleep(param.AUTOMATON_PERIOD);
		}
	}

	public void cancel() {
		running = false;
	}

	@Override
	public void motion_stop() {
		//land();
		//stage = STAGE.LAND;
		this.destination = null;
		running = false;
		inMotion = false;
	}

	@Override
	public void motion_resume() {
		running = true;
	}

	private void startMotion() {
		System.out.println("I'm in startMotion");
		running = true;
		stage = STAGE.INIT;
		inMotion = true;
	}

	protected void sendMotionEvent(int motiontype, int... argument) {
		// TODO: This might not be necessary
		gvh.trace.traceEvent(TAG, "Motion", Arrays.toString(argument), gvh.time());
		gvh.sendRobotEvent(Event.MOTION, motiontype);
	}

	private void setControlInputRescale(double yaw_v, double pitch, double roll, double gaz){
		setControlInput(rescale(yaw_v, mypos.max_yaw_speed), rescale(pitch, mypos.max_pitch_roll), rescale(roll, mypos.max_pitch_roll), rescale(gaz, mypos.max_gaz));
	}

	private double rescale(double value, double max_value){
		if(Math.abs(value) > max_value){
			return (Math.signum(value));
		}
		else{
			return value/max_value;
		}
	}

    //TODO: change this func to integrate the hardware control.
    /**
     * I would've assumed that the rpm stuff would come here cos rn
     * all it does is throw exceptions, ideally i should have an rpm 
     * class, also what does it mean to integrate the hardware control
     * @param yaw_v
     * @param pitch
     * @param roll
     * @param gaz
     */
	protected void setControlInput(double yaw_v, double pitch, double roll, double gaz){
		if(yaw_v > 1 || yaw_v < -1){
			throw new IllegalArgumentException("yaw speed must be between -1 to 1");
		}
		if(pitch > 1 || pitch < -1){
			throw new IllegalArgumentException("pitch must be between -1 to 1");
		}
		if(roll > 1 || roll < -1){
			throw new IllegalArgumentException("roll speed must be between -1 to 1");
		}
		if(gaz > 1 || gaz < -1){
			throw new IllegalArgumentException("gaz, vertical speed must be between -1 to 1");
		}
        Model_quadcopter.yaw = yaw_v;
        Model_quadcopter.roll = roll;
        Model_quadcopter.pitch = pitch;
	}

	/**
	 *  take off from ground
	 */
	protected void takeOff(){
		//Bluetooth command to control the drone
		gvh.log.i(TAG, "Drone taking off");
	}

	/**
	 * land on the ground
	 */
	protected void land(){
		//Bluetooth command to control the drone
		gvh.log.i(TAG, "Drone landing");
	}

	/**
	 * hover at current position
	 */
	protected void hover(){
		//Bluetooth command to control the drone
		gvh.log.i(TAG, "Drone hovering");
	}

	@Override
	public void turnTo(ItemPosition dest) {
		throw new IllegalArgumentException("quadcopter does not have a corresponding turn to");
	}

	@Override
	public void setParameters(MotionParameters param) {
		// TODO Auto-generated method stub		
	}

    private boolean collision_mem_less(){
        if(mypos.leftbump || mypos.rightbump){
            double ColPoint_x, ColPoint_y, ColPoint_z;
            if(mypos.leftbump){
                ColPoint_x = -mypos.radius*0.9 + mypos.x;
                ColPoint_y = -mypos.radius*0.9 + mypos.y;
                ColPoint_z = mypos.z;
                blocker = new ItemPosition("detected", (int) ColPoint_x, (int) ColPoint_y, (int) ColPoint_z);
                return true;
            }
            else if(mypos.rightbump){
                ColPoint_x = mypos.radius*0.9 + mypos.x;
                ColPoint_y = mypos.radius*0.9 + mypos.y;
                ColPoint_z = mypos.z;
                blocker = new ItemPosition("detected", (int) ColPoint_x, (int) ColPoint_y, (int) ColPoint_z);
                return true;
            }
            else
                return false; // add top and down bump too
        }
        else
            return false;
    }

}
