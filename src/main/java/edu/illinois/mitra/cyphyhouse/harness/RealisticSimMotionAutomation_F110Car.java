package edu.illinois.mitra.cyphyhouse.harness;

import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouse.motion.MotionAutomation_F110Car;
import edu.illinois.mitra.cyphyhouse.objects.Common;

/**
 * Created by kek on 6/26/17.
 */
public class RealisticSimMotionAutomation_F110Car extends MotionAutomation_F110Car{

        private SimGpsProvider gpsp;
        private String name;

        public RealisticSimMotionAutomation_F110Car(GlobalVarHolder gvh, SimGpsProvider gpsp) {
            super(gvh);
            name = gvh.id.getName();
            this.gpsp = gpsp;
        }

        @Override
        public void motion_stop() {
            gpsp.setVelocityForCar(name, 0, 0);
            super.running = false;
            super.stage = MotionAutomation_F110Car.STAGE.INIT;
            super.destination = null;
            super.inMotion = false;
        }

        @Override
        protected void curve(int velocity, int radius) {
            if(running) {
                sendMotionEvent(Common.MOT_ARCING, velocity, radius);
                gpsp.setVelocityForCar(name, velocity, (int) Math.round((velocity*360.0)/(2*Math.PI*radius)));
            }
        }

        @Override
        protected void straight(int velocity) {
            gvh.log.i(TAG, "Straight at velocity " + velocity);
            if(running) {
                if(velocity != 0) {
                    sendMotionEvent(Common.MOT_STRAIGHT, velocity);
                } else {
                    sendMotionEvent(Common.MOT_STOPPED, 0);
                }
                gpsp.setVelocityForCar(name, velocity, 0);
            }
        }

        @Override
        protected void turn(int velocity, int angle) {
            if(running) {
                sendMotionEvent(Common.MOT_TURNING, velocity, angle);
                gpsp.setVelocityForCar(name, 0, (int)Math.copySign(velocity, -angle));
            }
        }

        @Override
        public void cancel() {
            super.running = false;
        }
}
