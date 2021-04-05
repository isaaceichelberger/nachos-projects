package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    private static int adultsOnOahu;
    private static int childrenOnOahu;
    private static int adultsOnMolokai;
    private static int childrenonMolokai;
    private static int boatLoc;// 0  Oahu, 1 Molokai
    private static int totalChildren;
    private static int totalAdults;
    private static Condition2 adultWaiter_Oahu;
    private static Condition2 childWaiter_Oahu;
    private static Condition2 adultWaiter_Molokai;
    private static Condition2 childWaiter_Molokai;
    private static Condition2 boatFiller;
    private static Lock boat;
    private static boolean done;
    private static int childrenOnBoat;


    public static void selfTest()
    {
        BoatGrader b = new BoatGrader();

        System.out.println("\n ***Testing Boats with only 2 children***");
        begin(0, 2, b);

        System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
        begin(1, 2, b);

        System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
        begin(3, 3, b);

        System.out.println("\n ***Testing Boats with 3 children, 4 adults***");
        begin(4, 3, b);

    }

    public static void begin( int adults, int children, BoatGrader b ) {
        // Store the externally generated autograder in a class
        // variable to be accessible by children.
        bg = b;
        // Instantiate global variables here
        adultsOnOahu = adults;
        childrenOnOahu = children;
        totalAdults = adults;
        totalChildren = children;
        boat = new Lock();
        adultWaiter_Molokai = new Condition2(boat);
        adultWaiter_Oahu = new Condition2(boat);
        childWaiter_Molokai = new Condition2(boat);
        childWaiter_Oahu = new Condition2(boat);
        boatFiller = new Condition2(boat);

        boatLoc = 0; // 0 = Oahu, 1 = Molokai
        adultsOnMolokai = 0;
        childrenonMolokai = 0;
        childrenOnBoat = 0;
        done = false;

        // Create threads here. See section 3.4 of the Nachos for Java
        // Walkthrough linked from the projects page.

        for (int i = 0; i < adults; i++) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    AdultItinerary();
                }
            };
            KThread t = new KThread(r);
            t.setName("Adult" + i);
            t.fork();
        }

        for (int j = 0; j < children; j++) {
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ChildItinerary();
                }
            };
            KThread c = new KThread(r2);
            c.setName("Child" + j);
            c.fork();
        }
    }


    static void AdultItinerary()
    {
        boat.acquire();
        while (!done) {
            if (boatLoc == 0) {
                // Adult on oahu has to wait if there is a child already on the boat, or if there aren't enough children on Molokai
                while (boatLoc == 1 || childrenOnBoat > 1 || childrenOnOahu > 1) {
                    adultWaiter_Oahu.sleep();
                }
                // if we are past this point, then we are at Oahu and there are children on molokai
                bg.AdultRowToMolokai();
                adultsOnOahu--;
                adultsOnMolokai++;
                boatLoc = 1;
                if (adultsOnOahu == 0 && childrenOnOahu == 0){
                    done = true;
                }
                // wake one thread on molokai that needs to return to Oahu
                childWaiter_Molokai.wakeAll();
                // sleep the thread that is now on Molokai
                adultWaiter_Molokai.sleep();

            } else if (boatLoc == 1) {
                adultWaiter_Molokai.sleep(); // we dont wanna be here
            }
        }
        boat.release();

    }

    static void ChildItinerary() {
        boat.acquire();
        while (!done) {
            if (boatLoc == 0) {
                while (boatLoc != 0 || childrenOnBoat == 2 || (adultsOnOahu > 0 && childrenOnOahu == 1)) {
                    childWaiter_Oahu.sleep();
                }
                childWaiter_Oahu.wakeAll();
                if (childrenOnOahu == 1 && adultsOnOahu == 0) {
                    childrenOnOahu--;
                    bg.ChildRowToMolokai();
                    boatLoc = 1;
                    childrenonMolokai++;
                    childrenOnBoat = 0;
                    adultWaiter_Molokai.wakeAll();
                    childWaiter_Molokai.wakeAll();
                    childWaiter_Molokai.sleep();
                    done = true;
                    // should be finished here
                } else if (childrenOnOahu > 1) {
                    // boat cannot go until 2 children
                    childrenOnBoat++;
                    if (childrenOnBoat == 1) {
                        boatFiller.sleep();
                        childrenOnOahu--;
                        bg.ChildRowToMolokai();
                        boatLoc = 1;
                        childrenonMolokai++;
                        boatFiller.wake();
                        // make the child wait
                        childWaiter_Molokai.sleep();
                    } else if (childrenOnBoat == 2) {
                        boatFiller.wake();
                        boatFiller.sleep();
                        childrenOnOahu--;
                        bg.ChildRideToMolokai();
                        childrenOnBoat = 0;
                        boatLoc = 1;
                        childrenonMolokai++;
                        if (adultsOnOahu == 0 && childrenOnOahu == 0){
                            done = true;
                        }
                        childWaiter_Molokai.wakeAll(); // wake up a child to go back
                        childWaiter_Molokai.sleep(); // sleep the current child
                    }
                }
            }
            if (boatLoc == 1) {
                while (boatLoc != 1) { // error catching if we wake up in the wrong spot
                    childWaiter_Molokai.sleep();
                    adultWaiter_Molokai.sleep();
                }
                // need the second half to finish
                if ((childrenonMolokai >= 1 && adultsOnOahu != 0) || (adultsOnMolokai == totalAdults && childrenonMolokai != totalChildren)) {
                    childrenonMolokai--;
                    bg.ChildRowToOahu();
                    childrenOnOahu++;
                    boatLoc = 0;
                    // wake the adults and children, if there are more children they take priority
                    childWaiter_Oahu.wakeAll();
                    adultWaiter_Oahu.wakeAll();
                    // sleep current child
                    childWaiter_Oahu.sleep();
                } else {
                    if (childrenOnOahu == 0) {
                        done = true;
                    }
                }
            }
        }
        boat.release();
    }
    static void SampleItinerary()
    {
        // Please note that this isn't a valid solution (you can't fit
        // all of them on the boat). Please also note that you may not
        // have a single thread calculate a solution and then just play
        // it back at the autograder -- you will be caught.
        System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
        bg.AdultRowToMolokai();
        bg.ChildRideToMolokai();
        bg.AdultRideToMolokai();
        bg.ChildRideToMolokai();
    }
}