package nachos.threads;

import nachos.machine.*;
import nachos.machine.Timer;
import java.util.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    // data members
    private static Map<Long, KThread> threadMap = new HashMap<>();
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        // called every 500 clock ticks
        if (!threadMap.isEmpty()) {
                Iterator it = threadMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    // get the time
                    long currentTime = Machine.timer().getTime();
                    if ((long) pair.getKey() <= currentTime) { //  the time has at least past
                        // wake the thread back up
                        boolean intStatus = Machine.interrupt().disable();
                        KThread thread = (KThread) pair.getValue();
                        threadMap.remove(pair.getKey());
                        thread.ready();
                        Machine.interrupt().restore(intStatus);
                }
            }
        }
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	Timer#getTime()
     */
    public void waitUntil(long x) {
        long wakeTime = Machine.timer().getTime() + x;
        threadMap.put(wakeTime, KThread.currentThread());
        boolean intStatus = Machine.interrupt().disable();
        (KThread.currentThread()).sleep();
        Machine.interrupt().restore(intStatus);
    }
}
