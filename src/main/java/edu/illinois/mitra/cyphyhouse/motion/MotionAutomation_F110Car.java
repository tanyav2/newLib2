package edu.illinois.mitra.cyphyhouse.motion;

import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.interfaces.RobotEventListener;
import edu.illinois.mitra.cyphyhouse.models.ModelF110Car;
import edu.illinois.mitra.cyphyhouse.objects.Common;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;

import java.util.Arrays;
import java.util.Scanner;
import java.util.Vector;

/**
 * Created by kek on 6/26/17.
 */
public class MotionAutomation_F110Car extends RobotMotion {

    protected static final String TAG = "MotionAutomatonF110Car";
    protected static final String ERR = "Critical Error";

    protected GlobalVarHolder gvh;

    // Motion tracking
    protected ItemPosition destination;
    private ModelF110Car mypos;
    private ItemPosition blocker;
    private ObstacleList obsList;

    // No necessity for obstacles rn

    protected enum STAGE {
        INIT, ARCING, STRAIGHT, TURN, SMALLTURN, GOAL, UNABLE
    }

    private STAGE next = null;
    protected STAGE stage = STAGE.INIT;
    private STAGE prev = null;
    protected boolean running = false;
    boolean colliding = false;

    private enum OPMODE {
        GO_TO, TURN_TO
    }

    private OPMODE mode = OPMODE.GO_TO;

    private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
    private volatile MotionParameters param = DEFAULT_PARAMETERS;

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
    private double linspeed;
    private double turnspeed;

    public MotionAutomation_F110Car(GlobalVarHolder gvh) {
        super(gvh.id.getName());
        this.gvh = gvh;
        this.linspeed = (param.LINSPEED_MAX - param.LINSPEED_MIN) / (double) (param.SLOWFWD_RADIUS - param.GOAL_RADIUS);
        this.turnspeed = (param.TURNSPEED_MAX - param.TURNSPEED_MIN) / (param.SLOWTURN_ANGLE - param.SMALLTURN_ANGLE);
    }

    @Override
    public void cancel() {

    }

    /**
     * Go to a destination using the default motion parameters
     *
     * @param dest    the robot's destination
     * @param obsList
     */
    @Override
    public void goTo(ItemPosition dest, ObstacleList obsList) {
        if((inMotion && !this.destination.equals(dest)) || !inMotion) {
            this.destination = new ItemPosition(dest.name,dest.x,dest.y,0);
//            Log.d(TAG, "Going to X: " + Integer.toString(dest.x) + " Y: " + Integer.toString(dest.y));
            //this.destination = dest;
            this.mode = OPMODE.GO_TO;
            this.obsList = obsList;
            startMotion();
        }
    }

    @Override
    public void goTo(ItemPosition dest) {
        System.out.println("areyoumydest"+":"+dest);
        Scanner in = new Scanner(((ModelF110Car)gvh.gps.getMyPosition()).name).useDelimiter("[^0-9]+");
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

    /**
     * Turn to face a destination using the default motion parameters
     *
     * @param dest the destination to face
     */
    @Override
    public void turnTo(ItemPosition dest) {
        if((inMotion && !this.destination.equals(dest)) || !inMotion) {
            this.destination = dest;
            this.mode = OPMODE.TURN_TO;
            startMotion();
        }
    }

    /**
     * Enable robot motion
     */
    @Override
    public void motion_resume() {
        running = true;
    }

    /**
     * Stop the robot and disable motion until motion_resume is called. This cancels the current motion.
     */
    @Override
    public void motion_stop() {

    }



    /**
     * Set the default motion parameters to use
     *
     * @param param the parameters to use by default
     */
    @Override
    public void setParameters(MotionParameters param) {

        this.param = param;
        this.linspeed = (double) (param.LINSPEED_MAX - param.LINSPEED_MIN) / Math.abs((param.SLOWFWD_RADIUS - param.GOAL_RADIUS));
        this.turnspeed = (param.TURNSPEED_MAX - param.TURNSPEED_MIN) / (param.SLOWTURN_ANGLE - param.SMALLTURN_ANGLE);

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

        while(true) {
            if(running) {
                mypos = (ModelF110Car) gvh.gps.getMyPosition();
                int distance = mypos.distanceTo(destination);
                int angle = mypos.angleTo(destination);
                int absangle = Math.abs(angle);
                switch(param.COLAVOID_MODE) {
                    case BUMPERCARS:
                        colliding = false;
                        break;
                    case USE_COLAVOID:
                        colliding = collision_mem_less();
                        break;
                    case USE_COLBACK:
                        colliding = collision();
                        break;
                    case STOP_ON_COLLISION:
                        colliding = collision();
                        break;
                    default:
                        colliding = false;
                        break;
                }

                if(!colliding && stage != null) {
                    if(stage != prev)
                        gvh.log.e(TAG, "Stage is: " + stage.toString());
                    if(distance <= param.GOAL_RADIUS) {
                        next = STAGE.GOAL;
                    }
                    switch(stage) {
                        case INIT:
                            done = false;
                            if(mode == OPMODE.GO_TO) {
                                if(param.ENABLE_ARCING && distance <= param.ARC_RADIUS && absangle <= param.ARCANGLE_MAX) {
                                    next = STAGE.ARCING;
                                } else {
                                    next = STAGE.TURN;
                                }
                            } else {
                                next = STAGE.TURN;
                            }
                            break;
                        case ARCING:
                            // If this is the first run of ARCING, begin the arc
                            if(stage != prev) {
                                int radius = curveRadius();
                                curve(param.ARCSPEED_MAX, radius);
                            } else {
                                // Otherwise, check exit conditions
                                if(absangle > param.ARC_EXIT_ANGLE)
                                    next = STAGE.TURN;
                                if(absangle < param.STRAIGHT_ANGLE)
                                    next = STAGE.STRAIGHT;
                            }
                            break;
                        case STRAIGHT:
                            if(stage != prev) {
                                straight(LinSpeed(distance));
                            } else {
                                if(Common.inRange(distance, param.GOAL_RADIUS, param.SLOWFWD_RADIUS))
                                    straight(LinSpeed(distance));
                                if(Common.inRange(absangle, param.SMALLTURN_ANGLE, param.ARCANGLE_MAX))
                                    next = STAGE.SMALLTURN;
                                if(absangle > param.ARCANGLE_MAX) {
                                    next = STAGE.TURN;
                                }

                            }
                            break;
                        case TURN:
                            if(stage != prev) {
                                turn(TurnSpeed(absangle), angle);
                            } else {
                                if(absangle <= param.SMALLTURN_ANGLE) {
                                    gvh.log.i(TAG, "Turn stage: within angle bounds!");
                                    next = (mode == OPMODE.GO_TO) ? STAGE.STRAIGHT : STAGE.GOAL;
                                } else if(absangle <= param.SLOWTURN_ANGLE) {
                                    // Resend a reduced-speed turn command if we're
                                    // within the slow-turn window
                                    turn(TurnSpeed(absangle), angle);
                                }
                            }
                            break;
                        case SMALLTURN:
                            if(stage != prev) {
                                int radius = curveRadius() / 2;
                                curve(LinSpeed(distance), radius);
                            } else {
                                if(absangle <= param.SMALLTURN_ANGLE)
                                    next = STAGE.STRAIGHT;
                                if(absangle > param.ARCANGLE_MAX)
                                    next = STAGE.TURN;
                            }
                            break;
                        case GOAL:
                            done = true;
                            gvh.log.i(TAG, "At goal, done flag set!");
                            if(param.STOP_AT_DESTINATION)
                                straight(0);
                            running = false;
                            inMotion = false;
                            break;
                        case UNABLE:
                            System.out.println("motionautomation inactive");
                            gvh.log.i(TAG, "motion could not reach dest");
                            straight(0);
                            done = false;
                            running = false;
                            inMotion = false;
                            break;
                    }

                    prev = stage;
                    if(next != null) {
                        stage = next;
                        gvh.log.i(TAG, "Stage transition to " + stage.toString());
                        gvh.trace.traceEvent(TAG, "Stage transition", stage.toString(), gvh.time());
                    }
                    next = null;
                }
                if((colliding || stage == null) ) {
                    switch(param.COLAVOID_MODE) {
                        case USE_COLAVOID:
                            use_colavoid();
                            break;
                        case USE_COLBACK:
                            use_colback();
                            break;
                        case STOP_ON_COLLISION:
                            if(stage != null) {
                                gvh.log.d(TAG, "Imminent collision detected!");
                                straight(0);
                                stage = STAGE.UNABLE;
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
            gvh.sleep(param.AUTOMATON_PERIOD);
        }
    }

    private void use_colavoid() {
        if(stage != null) {
            gvh.log.d(TAG, "Imminent collision detected!");
            stage = null;
            straight(0);
            colnext = null;
            colprev = null;
            colstage = COLSTAGE.TURN;
        }
        switch(colstage) {
            case TURN:
                if(colstage != colprev) {
                    gvh.log.d(TAG, "Colliding: sending turn command");
                    turn(param.TURNSPEED_MAX, -1 * mypos.angleTo(blocker));

                }

                if(!colliding) {

                    colnext = COLSTAGE.STRAIGHT;

                } else {
                    gvh.log.d(TAG, "colliding with " + blocker.name + " - " + mypos.isFacing(blocker) + " - " + mypos.distanceTo(blocker));
                }
                break;
            case STRAIGHT:
                if(colstage != colprev) {
                    gvh.log.d(TAG, "Colliding: sending straight command");
                    straight(param.LINSPEED_MAX);
                    col_straightime = 0;
                } else {
                    col_straightime += param.AUTOMATON_PERIOD;
                    // If a collision is imminent (again), return to the
                    // turn stage
                    if(colliding) {
                        gvh.log.d(TAG, "Collision imminent! Cancelling straight stage");
                        straight(0);
                        colnext = COLSTAGE.TURN;
                    }
                    // If we're collision free and have been for enough
                    // time, restart normal motion
                    if(!colliding && col_straightime >= param.COLLISION_AVOID_STRAIGHTTIME) {
                        gvh.log.d(TAG, "Free! Returning to normal execution");
                        colprev = null;
                        colnext = null;
                        colstage = null;
                        stage = STAGE.INIT;
                    }
                }
                break;
        }
        colprev = colstage;
        if(colnext != null) {
            colstage = colnext;
            gvh.log.i(TAG, "Advancing stage to " + colnext);
        }
        colnext = null;
        return;
    }

    private void use_colback() {
        goalbot();
    }

    private void goalbot() {
        if(stage != null) {
            gvh.log.d(TAG, "Imminent collision detected!");
            stage = null;
            straight(0);
            colnext0 = null;
            colprev0 = null;
            colstage0 = COLSTAGE0.BACK;
        }

        switch(colstage0) {
            case BACK:
                col_backtime += param.AUTOMATON_PERIOD;
                straight(- param.LINSPEED_MAX/2);
                if(col_backtime > param.COLLISION_AVOID_BACKTIME){
                    col_backtime = 0;
                    straight(0);
                    colprev0 = null;
                    colnext0 = null;
                    colstage0 = null;
                    stage = STAGE.UNABLE;
                }
                break;
            case STRAIGHT:
                straight(param.LINSPEED_MAX);
                if(colliding) {
                    gvh.log.d(TAG, "Collision imminent! Cancelling straight stage");
                    straight(0);
                    colnext0 = COLSTAGE0.BACK;
                }
                break;
        }
        colprev0 = colstage0;
        if(colnext0 != null) {
            colstage0 = colnext0;
            gvh.log.i(TAG, "Advancing stage to " + colnext);
        }
        colnext0 = null;

    }


    // Calculates the radius of curvature to meet a target
    private int curveRadius() {
        int x0 = mypos.x;
        int y0 = mypos.y;
        int x1 = destination.x;
        int y1 = destination.y;
        int theta = (int)mypos.angle;
        double alpha = -180 + Math.toDegrees(Math.atan2((y1 - y0), (x1 - x0)));
        double rad = -(Math.sqrt(Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2)) / (2 * Math.sin(Math.toRadians(alpha - theta))));
        return (int) rad;
    }

    private void startMotion() {
        running = true;
        stage = STAGE.INIT;
        inMotion = true;
    }

    protected void sendMotionEvent(int motiontype, int... argument) {
        gvh.trace.traceEvent(TAG, "Motion", Arrays.toString(argument), gvh.time());
        gvh.sendRobotEvent(RobotEventListener.Event.MOTION, motiontype);
    }

    protected void curve(int velocity, int radius) {}

    protected void straight(int velocity) {}

    protected void turn(int velocity, int angle) {}

    // Ramp linearly from min at param.SMALLTURN_ANGLE to max at param.SLOWTURN_ANGLE
    public int TurnSpeed(int angle) {
        if(angle > param.SLOWTURN_ANGLE) {
            return param.TURNSPEED_MAX;
        } else if(angle > param.SMALLTURN_ANGLE && angle <= param.SLOWTURN_ANGLE) {
            return param.TURNSPEED_MIN + (int) ((angle - param.SMALLTURN_ANGLE) * turnspeed);
        } else {
            return param.TURNSPEED_MIN;
        }
    }

    /**
     * Slow down linearly upon coming within R_slowfwd of the goal
     *
     * @param distance
     * @return
     */
    private int LinSpeed(int distance) {
        if(distance > param.SLOWFWD_RADIUS)
            return param.LINSPEED_MAX;
        if(distance > param.GOAL_RADIUS && distance <= param.SLOWFWD_RADIUS) {
            return param.LINSPEED_MIN + (int) ((distance - param.GOAL_RADIUS) * linspeed);
        }
        return param.LINSPEED_MIN;
    }

    private boolean collision_mem_less(){
        if(mypos.leftbump || mypos.rightbump){
            double ColPoint_x, ColPoint_y;
            if(mypos.leftbump&&mypos.rightbump){
                ColPoint_x = mypos.radius*(Math.cos(Math.toRadians(mypos.angle))) + mypos.x;
                ColPoint_y = mypos.radius*(Math.sin(Math.toRadians(mypos.angle))) + mypos.y;
                blocker = new ItemPosition("detected", (int) ColPoint_x, (int) ColPoint_y, 0);
            }
            else if(mypos.leftbump){
                ColPoint_x = mypos.radius*(Math.cos(Math.toRadians(mypos.angle+45))) + mypos.x;
                ColPoint_y = mypos.radius*(Math.sin(Math.toRadians(mypos.angle+45))) + mypos.y;
                blocker = new ItemPosition("detected", (int) ColPoint_x, (int) ColPoint_y, 0);
            }
            else{
                ColPoint_x = mypos.radius*(Math.cos(Math.toRadians(mypos.angle-45))) + mypos.x;
                ColPoint_y = mypos.radius*(Math.sin(Math.toRadians(mypos.angle-45))) + mypos.y;
                blocker = new ItemPosition("detected", (int) ColPoint_x, (int) ColPoint_y, 0);
            }

            return true;
        }
        else
            return false;
    }

    private boolean collision() {
        boolean toreturn = collision_mem_less();
        if(toreturn)
            obsList.detected(blocker);
        return toreturn;
    }

}
