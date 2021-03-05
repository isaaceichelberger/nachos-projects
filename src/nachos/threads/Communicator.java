package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    private Lock canCommunicate = new Lock(); // Lock to base all of the conditions on
    private Condition2 canSpeak = new Condition2(canCommunicate); // A condition that tells us if the speaker can speak
    private Condition2 canListen = new Condition2(canCommunicate); // A condition that tells us if the receiver can listen
    private Condition2 returnBlocker = new Condition2(canCommunicate); // A condition that tells us if listen can return
    private int comBox;
    private boolean isBoxFull;
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        boolean intStatus = Machine.interrupt().disable();
        canCommunicate.acquire();
        while (isBoxFull){ // if the mailbox is full, we can't speak
            canSpeak.sleep();
        }
        // The mailbox must not be full here
        comBox = word;
        isBoxFull = true;
        // Now that we've put we the word in the box, we need to wake the listener
        canListen.wake();
        returnBlocker.sleep(); // need another condition to keep speak from finishing before listen runs
        canCommunicate.release();
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
        boolean intStatus = Machine.interrupt().disable();
        canCommunicate.acquire();
        while (!isBoxFull){ // if the mailbox isn't full, we can't listen
            canListen.sleep();
        }
        // Word received
        isBoxFull = false;
        int response = comBox; // need to do this just in case scheduler changes comBox's value
        canSpeak.wake(); // box is no longer full, so we can speak
        returnBlocker.wake(); // we can now return because a word has been received
        canCommunicate.release();
        Machine.interrupt().restore(intStatus);
        return response;
    }
}
