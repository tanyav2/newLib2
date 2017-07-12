# newLib
Migrating from old StarLib to a library with only Java dependencies. In order
to support new CyphyHouse project on various of platforms.

Original repo can be found here: [`StarLib`](https://github.com/detree/StarL1.5/tree/master/trunk/android/StarLib)
in [StarL1.5 - yixiao.](https://github.com/lin187/StarL1.5)

## to compile and run:
  * Please be sure to install `maven` and `java`. Currently building under `java1.8`
  * In the folder after clone:
  * Type in terminal: `./runapps.sh [APP_NAME] [NUM_OF_IROBOTS] [NUM_OF_CARS] [NUM_OF_QUADCOPTERS]`
    * Currently the apps supported are `followapp`, `raceapp` and `flockingapp`

## programs under `tools` folder
  * `send.py` sends UDP packages to a designated address. Currently used for fake position info.
  * `simul_recv.c` receives UDP packages under port 5556. Currently used for fake ARDrone instance.
