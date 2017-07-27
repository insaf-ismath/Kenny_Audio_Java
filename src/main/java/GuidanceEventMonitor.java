/**
 * Created by KennyChoo on 12/5/17.
 */

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The GuidanceEventMonitor examines each AudioProcessingResult (an audio buffer read) to see if
 * a GuidanceEvent should be reported.
 *
 * @author KennyChoo <kennytwchoo@gmail.com> created on 29/4/17.
 */

public class GuidanceEventMonitor {
    private static final String TAG = "GuidanceEventMonitor";

    private Turns turns = new Turns();
    AudioProcessingResult result;
    private int[] eventsBuffer = new int[GuidanceEvent.numGuidanceEventTypes]; // assuming 4 types of events
    private ArrayList<GuidanceEvent> guidanceEvents = new ArrayList<>();

    // --- Parameters ---
    // Overlap
    private static final float overlapSpeechFrameLengthThreshold = 5;    // in seconds
    private static final float overlapSpeechPauseThreshold = 0;  // in seconds
    // Continued Turns
    private static final int contTurnRepsThreshold = 5; // no. of continued turns
    private static final float contTurnsSepThreshold = 1;   // in seconds
    // No Response
    private static final int noResponseRepsThreshold = 5;   // no. of child turns with no response
    // LongTurn
    private static final float longTurnThreshold = 1;   // in seconds
    // Fast Speech
    private static final float fastSpeechFrameLengthThreshold = 5;    // in seconds
    private static final float fastSpeechRateThreshold = 1.6f;// syllables(onsets) per second


    /**
     * Checks the result to append to turn list, check whether we should check for guidance events.
     *
     * @param result a result from one audio buffer read and compute
     * @return true if GuidanceEvents were checked for.
     */
    public boolean checkResult(AudioProcessingResult result) {
        this.result = result;
        // process result for turns
        if (turns.processResult(result)) {
            checkGuidanceEvents();
            return true;
        }
        else {
            return false;
        }
    }

    private void checkGuidanceEvents() {
        eventsBuffer[GuidanceEvent.OVERLAP] = checkOverlap() ? 1 : 0;
        eventsBuffer[GuidanceEvent.CONTINUED_TURNS] = checkContinuedTurns() ? 1 : 0;
        eventsBuffer[GuidanceEvent.NO_RESPONSE] = checkNoResponse() ? 1 : 0;
        eventsBuffer[GuidanceEvent.LONG_TURN] = checkLongTurn() ? 1 : 0;
        eventsBuffer[GuidanceEvent.FAST_SPEECH] = checkFastSpeech() ? 1 : 0;
//        Log.d(TAG, "GuidanceEvents: " + guidanceEvents);
    }

    private boolean checkOverlap() {
        ArrayList<Turn> turnsInFrame = new ArrayList<>();
        float startOfCheckFrame = result.getStartTime()
                - Turns.timeToleranceSilenceAfterFrames
                - overlapSpeechFrameLengthThreshold;

        int lastTurnIndex = turns.size() - 1;

        // Grab all turns that are in the time frame
        for (int i = lastTurnIndex; i >= 0; i--) {
            Turn turn = turns.get(i);

            if (turn.getEndTime() >= startOfCheckFrame || turn.getStartTime() >= startOfCheckFrame) {
                turnsInFrame.add(turn);
            }
        }

        // Check that they meet each other
        for (int i = 1, il = turnsInFrame.size(); i < il; i++) {
            Turn prev = turns.get(i - 1);
            Turn curr = turns.get(i);

            if (curr.getStartTime() - prev.getEndTime() < overlapSpeechPauseThreshold) {
                return true;
            }
        }
        return false;
    }

    private boolean checkContinuedTurns() {
        // there should be at least N turns before we sublist
        if (turns.size() < contTurnRepsThreshold) {
            return false;
        }

        // Get the last N turns
        List<Turn> lastNTurns = turns.subList(turns.size() - contTurnRepsThreshold, turns.size());

        // All turns should be from an adult
        for (Turn t : lastNTurns) {
            if (t.getUser() != User.ADULT) {
                return false;
            }
        }

        // Turns should be separated by x seconds
        for (int i = 1, il = lastNTurns.size(); i < il; i++) {
            if (lastNTurns.get(i).getStartTime() - lastNTurns.get(i - 1).getEndTime() >
                    contTurnsSepThreshold) {
                return false;
            }
        }

        guidanceEvents.add(new GuidanceEvent(result.getEndTime(), GuidanceEvent.CONTINUED_TURNS));
        return true;
    }

    private boolean checkNoResponse() {
        // there should be at least N turns before we sublist
        if (turns.size() < noResponseRepsThreshold) {
            return false;
        }

        // Get the last N turns
        List<Turn> lastNTurns = turns.subList(turns.size() - noResponseRepsThreshold, turns.size());

        // All turns should be from a child
        for (Turn t : lastNTurns) {
            if (t.getUser() != User.CHILD) {
                return false;
            }
        }
        guidanceEvents.add(new GuidanceEvent(result.getEndTime(), GuidanceEvent.NO_RESPONSE));
        return true;
    }

    private boolean checkLongTurn() {
        Turn turn = turns.getLatestTurn();

        if (turn.getUser() == User.ADULT && turn.getTurnLength() >=  longTurnThreshold) {
            guidanceEvents.add(new GuidanceEvent(result.getEndTime(), GuidanceEvent.LONG_TURN));
            return true;
        }

        return false;
    }

    private boolean checkFastSpeech() {
        ArrayList<Onset> onsetsInFrame = new ArrayList<>();
        float startOfCheckFrame = result.getStartTime()
                - Turns.timeToleranceSilenceAfterFrames
                - fastSpeechFrameLengthThreshold;

        int lastTurnIndex = turns.size() - 1;

        // check for Onsets that are within the check frame and collate them
        for (int i = lastTurnIndex; i >= 0; i--) {
            Turn turn = turns.get(i);
            // Check for Adult fast speech
            if (turn.getUser() == User.ADULT) {
                ArrayList<Onset> currentOnsets = turn.getOnsets();
                for (Onset onset : currentOnsets) {
                    if (onset.time >= startOfCheckFrame) {
                        onsetsInFrame.add(onset);
                    }
                }
            }
        }

        // compute the speech rate and compare against the threshold
        float syllablesPerSecond = onsetsInFrame.size() / fastSpeechFrameLengthThreshold;
//        System.out.println("syllablesPerSecond: " + syllablesPerSecond);
        if (syllablesPerSecond >= fastSpeechRateThreshold) {
            return true;
        }
        return false;
    }

    public Turns getTurns() {
        return turns;
    }

    public AudioProcessingResult getResult() {
        return result;
    }

    /**
     * Gets the buffer to output UI events, then clears the eventsBuffer
     *
     * @return buffer of the different GuidanceEvents detected. Returns null if there were no
     * events.
     */
    public int[] getEventsBuffer() {
        boolean hasEvent = false;
        // check if there were any events detected
        for (int event : eventsBuffer) {
            hasEvent = hasEvent || (event == 1 ? true : false);
        }
        if (hasEvent == false)
            return null;

        // make a copy to output
        int[] output = Arrays.copyOf(eventsBuffer, eventsBuffer.length);

        // clear the buffer
        eventsBuffer = new int[eventsBuffer.length];

        return output;
    }

    public void writeGuidanceEventsToFile(String filename) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename, false));

            for (GuidanceEvent g : guidanceEvents) {
                bw.write(g.toString() + "\n");
            }

            bw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeTurnsToFile(String filename) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename, false));

            for (Turn t : turns) {
                bw.write(t.toString() + "\n");
            }

            bw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
