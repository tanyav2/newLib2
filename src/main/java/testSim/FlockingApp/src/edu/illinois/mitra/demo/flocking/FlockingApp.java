package testSim.FlockingApp.src.edu.illinois.mitra.demo.flocking;

import edu.illinois.mitra.cyphyhouse.comms.MessageContents;
import edu.illinois.mitra.cyphyhouse.comms.RobotMessage;
import edu.illinois.mitra.cyphyhouse.functions.BarrierSynchronizer;
import edu.illinois.mitra.cyphyhouse.functions.RandomLeaderElection;
import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.gvh.RobotGroup;
import edu.illinois.mitra.cyphyhouse.interfaces.LeaderElection;
import edu.illinois.mitra.cyphyhouse.interfaces.LogicThread;
import edu.illinois.mitra.cyphyhouse.interfaces.Synchronizer;
import edu.illinois.mitra.cyphyhouse.motion.MotionParameters;
import edu.illinois.mitra.cyphyhouse.motion.RobotMotion;
import edu.illinois.mitra.cyphyhouse.objects.Common;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.PositionList;

import java.util.*;

/**
 * Created by kek on 7/11/17.
 */
public class FlockingApp extends LogicThread {

    private enum STAGE { START, SYNC, ELECT, MOVE, DONE }
    private STAGE stage = STAGE.START;

    private RobotMotion moat;
    // Following array represents the neighbours for each bot and rf distance between them. Index represents the bot num. As following:
    // bots_neighbour [bot_number][0= beforeBot_name], bots_neighbour [bot_number][1= afterBot_name], bots_neighbour [bot_number][2= rf]
    private static String[][] botsNeighbor;
    private int n_waypoints;

    private static final boolean RANDOM_DESTINATION = false;


    final Map<String, ItemPosition> destinations = new HashMap<>();
    ItemPosition currentDestination;

    private static final MotionParameters DEFAULT_PARAMETERS = MotionParameters.defaultParameters();
    private volatile MotionParameters param = DEFAULT_PARAMETERS;
    private int cur_waypoint = 0;
    PositionList pl = new PositionList();
    String wpn = "wp";

    private LeaderElection le;
    private Synchronizer sn;
    boolean initializeVee;
    private static final int SEND_NONES = 21;
    private static final int SEND_NOT_NONES = 22;

    public FlockingApp(GlobalVarHolder gvh) {
        super(gvh);
        gvh.comms.addMsgListener(this, SEND_NONES);
        gvh.comms.addMsgListener(this, SEND_NOT_NONES);
        initializeVee = true;
        botsNeighbor = new String[gvh.id.getParticipants().size()][3];

        le = new RandomLeaderElection(gvh);

        le.elect();
        gvh.BotGroup = new RobotGroup(gvh.id.getName(), Common.numOFgroups);

        sn = new BarrierSynchronizer(gvh);

        moat = gvh.plat.moat;
        MotionParameters.Builder settings = new MotionParameters.Builder();
        settings = settings.ENABLE_ARCING(true);
        settings = settings.STOP_AT_DESTINATION(true);

        settings = settings.COLAVOID_MODE(MotionParameters.COLAVOID_MODE_TYPE.USE_COLAVOID); // buggy, just goes back, deadlocks...

        param = settings.build();
        moat.setParameters(param);
        //n_waypoints = gvh.gps.getWaypointPositions().getNumPositions();
        n_waypoints = Integer.MAX_VALUE;
        String n = wpn + gvh.id.getName() + cur_waypoint;
        pl.update(new ItemPosition(n, 2000, 2000, 0));

    }

    @Override
    public List<Object> callStarL() {

        String robotName = gvh.id.getName();
        Integer robotNum = getNumFromName(robotName);
        Integer count = 0;
        Integer leaderNum = 1;
        boolean test = true;

        while(true) {
            switch (stage) {
                case START: {

                    sn.barrierSync("round" + count.toString());
                    stage = STAGE.SYNC;

                    System.out.printf("robot %3d, round " + count.toString() + "\n", robotNum);


                    break;
                }
                case SYNC: {
                    if (sn.barrierProceed("round" + count.toString())) {
                        stage = STAGE.ELECT;
                        // le.elect();
                    }
                    break;
                }
                case ELECT: {
                    if(le.getLeader() != null) {
                        System.out.printf("robot %3d, leader is: " + le.getLeader() + "\n", robotNum);
                        //leaderNum = Integer.parseInt(le.getLeader().substring(3)); // assumes: botYYY
                        leaderNum = getNumFromName(le.getLeader());
                        stage = STAGE.MOVE;
                        // All below code in Elect state is to assign order-rank- for each robot in its group
                        if (robotNum!=leaderNum) {
                            if (gvh.BotGroup.setAfterBefore) {
                                ItemPosition myPosition = gvh.gps.getMyPosition();
                                int mySummation = myPosition.getX() + myPosition.getY();
                                int ranking = 1;
                                PositionList plAll = gvh.gps.get_robot_Positions();
                                ArrayList<ItemPosition> allBots = plAll.getList();
                                for (ItemPosition rp :allBots ) {
                                    //Integer rpNum = Integer.valueOf(rp.getName().substring(3));
                                    Integer rpNum = getNumFromName(rp.getName());
                                    if (rpNum != leaderNum) {
                                        if (rpNum != robotNum) {
                                            // Integer rpGroup = Integer.valueOf(rp.getName().substring(3)) % Common.numOFgroups;
                                            Integer rpGroup = getNumFromName(rp.getName()) % Common.numOFgroups;
                                            if (gvh.BotGroup.getGroupNum() == rpGroup) {
                                                int otherSummation =  rp.getX() + rp.getY();
                                                // if (mySummation == otherSummation){

                                                //}
                                                if (mySummation == otherSummation)
                                                    System.out.println("############************** There is potential same locations ***************########### "+ robotName+" and "+ rp.getName());

                                                if (mySummation >= otherSummation) {
                                                    if (gvh.BotGroup.BeforeBot == null) {
                                                        if (mySummation == otherSummation) {
                                                            if (robotNum > getNumFromName(rp.getName())) // changed this
                                                                gvh.BotGroup.BeforeBot = rp.getName();
                                                        }else
                                                            gvh.BotGroup.BeforeBot = rp.getName();
                                                    }
                                                    else {
                                                        int xSub = 0;
                                                        int ySub = 0;
                                                        // int angleSub = 0;
                                                        PositionList plAllSub = gvh.gps.get_robot_Positions();
                                                        ArrayList<ItemPosition> allRobots = plAllSub.getList();
                                                        for (ItemPosition rpSub : allRobots) {
                                                            //if (Integer.valueOf(rpSub.getName().substring(3)) == Integer.valueOf(gvh.BotGroup.BeforeBot.substring(3))) {
                                                            if (getNumFromName(rpSub.getName()) == getNumFromName(gvh.BotGroup.BeforeBot)) {
                                                                xSub = rpSub.getX();
                                                                ySub = rpSub.getY();
                                                                //angleSub = rpSub.angle;
                                                            }

                                                        }
                                                        int beforeBotSummation = xSub + ySub ;
                                                        if (otherSummation > beforeBotSummation)
                                                            gvh.BotGroup.BeforeBot = rp.getName();
                                                    }
                                                    if (mySummation == otherSummation){

                                                        System.out.println("############************** There is potential same locations ***************########### "+ robotName+" and "+ rp.getName());
                                                        if (robotNum < getNumFromName(rp.getName())){
                                                            gvh.BotGroup.AfterBot = rp.getName();
                                                        }else{
                                                            gvh.BotGroup.BeforeBot = rp.getName();
                                                            ranking++;
                                                        }
                                                    }else
                                                        ranking++;
                                                } else if (mySummation < otherSummation)
                                                    if (gvh.BotGroup.AfterBot == null)
                                                        gvh.BotGroup.AfterBot = rp.getName();
                                                    else {
                                                        int xSub = 0;
                                                        int ySub = 0;
                                                        PositionList plAllSub = gvh.gps.get_robot_Positions();
                                                        ArrayList<ItemPosition> allRobots = plAllSub.getList();
                                                        for (ItemPosition rpSub :allRobots) {
                                                            if (getNumFromName(rpSub.getName()) == getNumFromName(gvh.BotGroup.AfterBot)) {
                                                                xSub = rpSub.getX();
                                                                ySub = rpSub.getY();
                                                            }

                                                        }
                                                        int afterBotSummation = xSub + ySub ;

                                                        if (otherSummation == afterBotSummation)
                                                            if (robotNum < getNumFromName(rp.getName()) )
                                                                gvh.BotGroup.AfterBot = rp.getName();

                                                        if (otherSummation < afterBotSummation)
                                                            gvh.BotGroup.AfterBot = rp.getName();
                                                    }
                                                else if (getNumFromName(gvh.id.getName()) > getNumFromName(rp.getName())){
                                                    gvh.BotGroup.BeforeBot = rp.getName();
                                                    ranking++;
                                                } else gvh.BotGroup.AfterBot = rp.getName();
                                            }


                                        }
                                    }
                                }
                                gvh.BotGroup.rank = ranking;
                                gvh.BotGroup.setAfterBefore = false;
                                if (gvh.BotGroup.BeforeBot == null) {
                                    plAll = gvh.gps.get_robot_Positions();
                                    ArrayList<ItemPosition> allRobots = plAll.getList();
                                    for (ItemPosition rp :  allRobots)
                                        if (getNumFromName(rp.getName()) == leaderNum)
                                            gvh.BotGroup.BeforeBot = rp.getName();
                                }
                                if (gvh.BotGroup.AfterBot == null)
                                    gvh.BotGroup.isLast = true;

                                botsNeighbor[robotNum][0] = gvh.BotGroup.BeforeBot;
                                if (!gvh.BotGroup.isLast) botsNeighbor[robotNum][1] = gvh.BotGroup.AfterBot;
                                else botsNeighbor[robotNum][1] = "none";
                                botsNeighbor[robotNum][2] = String.valueOf(gvh.BotGroup.rf);
                                gvh.BotGroup.setAfterBefore = false;
                                // send message to all with Common vals and bot number
                                String[] temp = new String[4];
                                temp[0] = botsNeighbor[robotNum][0];
                                temp[1] = botsNeighbor[robotNum][1];
                                temp[2] = botsNeighbor[robotNum][2];
                                temp[3] = Integer.toString(robotNum);
                                MessageContents content = new MessageContents(temp);
                                RobotMessage assign_msg = new RobotMessage("ALL", name, SEND_NOT_NONES, content);
                                gvh.comms.addOutgoingMessage(assign_msg);
                            }
                        } else {
                            botsNeighbor[robotNum][0] = "none";
                            botsNeighbor[robotNum][1] = "none";
                            botsNeighbor[robotNum][2] = "none";
                            // send message to all with instruction to set to none
                            String contents = Integer.toString(robotNum);
                            MessageContents content = new MessageContents(contents);
                            RobotMessage m = new RobotMessage("ALL", name, SEND_NONES, content);
                            gvh.comms.addOutgoingMessage(m);
                        }

                        /* For Testing purpose
                        for (int i=0; i<Common.numOFbots; i++){
                            System.out.println("bot"+i+" and his before bot is "+botsNeighbor[i][0]+" and his after bot is "+botsNeighbor[i][1]+" and group distance is "+botsNeighbor[i][2]);
                        } */

                    }
                    break;
                }
                case MOVE: {
                    if(!moat.inMotion) {
                        String n = wpn + gvh.id.getName() + cur_waypoint;
                        ItemPosition dest;
                        if (initializeVee){
                            // Let the leader in the center
                            if (gvh.id.getName().equals(le.getLeader())){
                                dest = new ItemPosition(n, 0, 0, 0);
                                moat.goTo(dest);
                            } else {
                                // All other bots move to their place according to their order in the group
                                int oldX = gvh.BotGroup.rank*500;
                                int oldY = 0;

                                double newXX = oldX*Math.cos(Math.toRadians(gvh.BotGroup.theta))-oldY*Math.sin(Math.toRadians(gvh.BotGroup.theta));
                                double newYY = oldY*Math.cos(Math.toRadians(gvh.BotGroup.theta))+oldX*Math.sin(Math.toRadians(gvh.BotGroup.theta));

                                int newX= (int) newXX;
                                int newY= (int) newYY;


                                dest = new ItemPosition(n, newX, newY, gvh.BotGroup.theta.intValue());

                                moat.goTo(dest);
                            }
                            initializeVee = false;
                        } else {
                            //*********************** START: Rotation **********************
                            int newX=0;
                            int newY=0;
                            int beforeX=0;
                            int beforeY=0;
                            int afterX=0;
                            int afterY=0;
                            ItemPosition BeforeBot = new ItemPosition("BeforeBot", 0, 0, 0);
                            ItemPosition AfterBot = new ItemPosition("AfterBot", 0, 0, 0);

                            if (!gvh.id.getName().equals(le.getLeader())) {
                                PositionList plAll = gvh.gps.get_robot_Positions();
                                ArrayList<ItemPosition> allBots = plAll.getList();
                                for (ItemPosition rp : allBots) {
                                    if (rp.getName().equals(gvh.BotGroup.BeforeBot))
                                        BeforeBot = rp;
                                    if (!gvh.BotGroup.isLast) {
                                        if (rp.getName().equals(gvh.BotGroup.AfterBot))
                                            AfterBot = rp;
                                    }
                                }
                            }

                            //  ****************** Rotation for the robot********************
                            double newXX = gvh.gps.getMyPosition().getX() * Math.cos(Math.toRadians(-gvh.BotGroup.theta)) - gvh.gps.getMyPosition().getY() * Math.sin(Math.toRadians(-gvh.BotGroup.theta));
                            double newYY = gvh.gps.getMyPosition().getY() * Math.cos(Math.toRadians(-gvh.BotGroup.theta)) + gvh.gps.getMyPosition().getX() * Math.sin(Math.toRadians(-gvh.BotGroup.theta));

                            newX = (int) newXX;
                            newY = (int) newYY;

                            //******************** Rotation for robot before the Robot (Left Robot)**************
                            double beforeXX = BeforeBot.getX() * Math.cos(Math.toRadians(-gvh.BotGroup.theta)) - BeforeBot.getY() * Math.sin(Math.toRadians(-gvh.BotGroup.theta));
                            double beforeYY = BeforeBot.getY() * Math.cos(Math.toRadians(-gvh.BotGroup.theta)) + BeforeBot.getX() * Math.sin(Math.toRadians(-gvh.BotGroup.theta));

                            beforeX = (int) beforeXX;
                            beforeY = (int) beforeYY;

                            //******************** Rotation for robot after the Robot (right Robot)**************
                            double afterXX = AfterBot.getX() * Math.cos(Math.toRadians(-gvh.BotGroup.theta)) - AfterBot.getY() * Math.sin(Math.toRadians(-gvh.BotGroup.theta));
                            double afterYY = AfterBot.getY() * Math.cos(Math.toRadians(-gvh.BotGroup.theta)) + AfterBot.getX() * Math.sin(Math.toRadians(-gvh.BotGroup.theta));

                            afterX = (int) afterXX;
                            afterY = (int) afterYY;

                            //*********************** END: Rotation   **********************

                            //*********************** START: Forming the flock **********************
                            //*********************** Leader doesn't need any change
                            if (!gvh.id.getName().equals(le.getLeader())) {

                                //*********************** If Robot is the Rightmost (Last robot in the group)
                                if (gvh.BotGroup.isLast) {
                                    newX = (beforeX + newX + gvh.BotGroup.rf) / 2;
                                    newY = (beforeY + newY) / 2;
                                } else {
                                    // ******************** If it is interior
                                    newX = (beforeX + afterX) / 2;
                                    newY = (beforeY + afterY) / 2;
                                }
                            }
                            //*********************** END: Forming the flock   **********************

                            //*********************** START: Rotation Back**********************

                            //*********************** Leader doesn't need any change
                            // if (!gvh.id.getName().equals(le.getLeader())) {

                            if (is_Flocking()) {
                                gvh.BotGroup.theta = gvh.BotGroup.theta + 20;
                            }

                            newXX = newX * Math.cos(Math.toRadians(gvh.BotGroup.theta)) - newY * Math.sin(Math.toRadians(gvh.BotGroup.theta));
                            newYY = newY * Math.cos(Math.toRadians(gvh.BotGroup.theta)) + newX * Math.sin(Math.toRadians(gvh.BotGroup.theta));

                            newX = (int) newXX;
                            newY = (int) newYY;

                            //*********************** END: Rotation   **********************

                            System.out.println(robotName+" has old coordination X "+gvh.gps.getMyPosition().getX()+" and Y "+gvh.gps.getMyPosition().getY()+" New X is "+newX+" and New Y is "+newY);
                            dest = new ItemPosition(n, newX, newY, gvh.BotGroup.theta.intValue());

                            moat.goTo(dest);
                        }
                        count ++;
                    }

                    // wait here while robot is in motion
                    while (moat.inMotion) {
                        gvh.sleep(100);
                    }

                    stage = STAGE.START; // repeat
                    break;
                }

                case DONE: {
                    gvh.trace.traceEnd();
                    return Arrays.asList(results);
                }
            }
            sleep(100);
        }
    }


    @Override
    protected void receive(RobotMessage m) {
        if(m.getMID() == SEND_NONES){
            int index = Integer.parseInt(m.getContents(0));
            botsNeighbor[index][0] = "none";
            botsNeighbor[index][1] = "none";
            botsNeighbor[index][2] = "none";
        }

        if(m.getMID() == SEND_NOT_NONES) {
            int index = Integer.parseInt(m.getContents(3));
            botsNeighbor[index][0] = m.getContents(0);
            botsNeighbor[index][1] = m.getContents(1);
            botsNeighbor[index][2] = m.getContents(2);
        }

    }

    private static final Random rand = new Random();

    @SuppressWarnings("unchecked")
    private <X, T> T getRandomElement(Map<X, T> map) {
        if(RANDOM_DESTINATION)
            return (T) map.values().toArray()[rand.nextInt(map.size())];
        else
            return (T) map.values().toArray()[0];
    }
    public boolean is_Flocking ()
    {
        boolean isFlocking = true;

        ItemPosition BeforeBot = new ItemPosition("BeforeBot", 0, 0, 0);
        ItemPosition AfterBot = new ItemPosition("AfterBot", 0, 0, 0);
        ItemPosition Bot = new ItemPosition("Bot", 0, 0, 0);

        int groupDis = 0;

        Integer leadNum = getNumFromName(le.getLeader());

        boolean once = true;
        for  (int i=0; i<Common.numOFbots; i++) {
            if (!botsNeighbor[i][2].equals("none")) {
                groupDis = Integer.parseInt(botsNeighbor[i][2]);

                if (once ) {
                    System.out.println("Reference distance between each group is " +groupDis);
                    once =false;
                }

            }
            if (i !=leadNum) {
                PositionList plAll = gvh.gps.get_robot_Positions();
                ArrayList<ItemPosition> allBots = plAll.getList();
                for (ItemPosition rp : allBots) {
                    if (rp.getName().equals("bot"+i))
                        Bot = rp;
                    if (rp.getName().equals(botsNeighbor[i][0]))
                        BeforeBot = rp;
                    if (!botsNeighbor[i][1].equals("none")) {
                        if (rp.getName().equals(botsNeighbor[i][1]))
                            AfterBot = rp;
                    }

                }

                // Distance between the bot and his before (left) neighbour
                double botDistance = Math.sqrt((Bot.getX()-BeforeBot.getX())*(Bot.getX()-BeforeBot.getX())+(Bot.getY()-BeforeBot.getY())*(Bot.getY()-BeforeBot.getY()));
                if (botDistance < (groupDis-groupDis*0.5)  || botDistance > (groupDis+groupDis*0.5) ) {
                    System.out.println("It is false because before bot is out of the range, their distance between each other is "+ String.valueOf(botDistance));
                    return false;
                }

                // Distance between the bot and his after (right) neighbour
                if (!botsNeighbor[i][1].equals("none")) {
                    double botDistanceAfter = Math.sqrt((Bot.getX()-AfterBot.getX())*(Bot.getX()-AfterBot.getX())+(Bot.getY()-AfterBot.getY())*(Bot.getY()-AfterBot.getY()));

                    if (botDistanceAfter < (groupDis-groupDis*0.5) || botDistanceAfter > (groupDis+groupDis*0.5)) {
                        System.out.println("It is false because after bot is out of the range, their distance between each other is "+ String.valueOf(botDistance));
                        return false;
                    }
                }
            }
        }

        return isFlocking;
    }

    private int getNumFromName(String name) {
        // this will work for bots with sequential numbers in their names, not irobot0 quadrotor0
        String s = name.replaceAll("[^0-9]", "");
        return Integer.parseInt(s);
    }
}
