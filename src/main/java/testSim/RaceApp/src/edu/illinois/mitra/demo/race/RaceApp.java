package testSim.RaceApp.src.edu.illinois.mitra.demo.race;

import edu.illinois.mitra.cyphyhouse.comms.RobotMessage;
import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.interfaces.LogicThread;
import edu.illinois.mitra.cyphyhouse.models.Model_quadcopter;
import edu.illinois.mitra.cyphyhouse.motion.MotionParameters;
import edu.illinois.mitra.cyphyhouse.motion.RRTNode;
import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;
import edu.illinois.mitra.cyphyhouse.objects.ObstacleList;
import edu.illinois.mitra.cyphyhouse.objects.PositionList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by kek on 7/11/17.
 */
public class RaceApp extends LogicThread {
    private static final boolean RANDOM_DESTINATION = false;
    public static final int ARRIVED_MSG = 22;

    final Map<String, ItemPosition> destinations = new HashMap<>();
    PositionList<ItemPosition> destinationsHistory = new PositionList<>();
    ItemPosition currentDestination;
    PositionList<ItemPosition> doReachavoidCalls = new PositionList<>();
    ObstacleList obs;
    public RRTNode kdTree;

    private enum Stage {
        PICK, GO, DONE, FAIL
    };

    private Stage stage = Stage.PICK;

    public RaceApp(GlobalVarHolder gvh) {
        super(gvh);
        MotionParameters.Builder settings = new MotionParameters.Builder();

        settings.COLAVOID_MODE(MotionParameters.COLAVOID_MODE_TYPE.USE_COLAVOID);

        MotionParameters param = settings.build();

        gvh.plat.moat.setParameters(param);
        for(ItemPosition i : gvh.gps.getWaypointPositions()){
            destinations.put(i.getName(), i);
            destinationsHistory.update(i);
        }
        gvh.comms.addMsgListener(this, ARRIVED_MSG);
        obs = gvh.gps.getObspointPositions();
    }

    @Override
    public List<Object> callStarL() {
        while(true) {

            if(gvh.plat.model instanceof Model_quadcopter){
                gvh.log.i("WIND", ((Model_quadcopter)gvh.plat.model).windxNoise + " " +  ((Model_quadcopter)gvh.plat.model).windyNoise);
            }
            switch(stage) {
                case PICK:
                    //	System.out.println("picking");
                    if(destinations.isEmpty()) {
                        stage = Stage.DONE;
                    } else {
                        currentDestination = getRandomElement(destinations);
                        gvh.plat.reachAvoid.doReachAvoid(gvh.gps.getMyPosition(), currentDestination, obs);
                        kdTree = gvh.plat.reachAvoid.kdTree;
                        gvh.log.i("DoReachAvoid", currentDestination.x + " " +currentDestination.y);
                        doReachavoidCalls.update(new ItemPosition(name + "'s " + "doReachAvoid Call to destination: " + currentDestination.name, gvh.gps.getMyPosition().x,gvh.gps.getMyPosition().y));
                        stage = Stage.GO;
                    }
                    break;
                case GO:
                    if(gvh.plat.reachAvoid.doneFlag) {
                        gvh.log.i("DoneFlag", "read");
                        if(currentDestination != null)
                            destinations.remove(currentDestination.getName());
                        RobotMessage inform = new RobotMessage("ALL", name, ARRIVED_MSG, currentDestination.getName());
                        gvh.comms.addOutgoingMessage(inform);
                        stage = Stage.PICK;
                    }
                    else if(gvh.plat.reachAvoid.failFlag){
                        //ra.doReachAvoid(gvh.gps.getMyPosition(), currentDestination, obs);
                        gvh.log.i("FailFlag", "read");
                        stage = Stage.FAIL;
                    }
                    break;
                case FAIL:
                    System.out.println(gvh.log.getLog());
                    break;
                case DONE:
                    System.out.println(gvh.log.getLog());
                    return null;
            }
            sleep(100);
        }
    }

    @Override
    protected void receive(RobotMessage m) {
        String posName = m.getContents(0);
        if(destinations.containsKey(posName))
            destinations.remove(posName);

        if(currentDestination.getName().equals(posName)) {
            gvh.plat.reachAvoid.cancel();
            stage = Stage.PICK;
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
}
