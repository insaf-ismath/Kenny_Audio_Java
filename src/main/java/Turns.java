import java.util.ArrayList;
import java.util.List;

/**
 * Created by KennyChoo on 12/5/17.
 */
public class Turns extends ArrayList<Turn> {
    private static final String TAG = "Turns";

    // parameters
    public static final float timeTolerancePauseBetweenFrames = 0.5f;  // default should be 0.5s
    public static final float timeToleranceSilenceAfterFrames = 0f;    // 0 so we know

    /**
     * Processes the latest AudioProcessingResult to see if
     * 1. A new turn should be added.
     * 2. The latest turn should be appended with this result.
     * 3. If the current result should trigger GuidanceEvent detection.
     *
     * @param result
     * @return true if we should trigger a GuidanceEvent detection.
     */
    public boolean processResult(AudioProcessingResult result) {
        Turn turn = getLatestTurn();

        // Check to see if the result frame is part of the latest turn or a new one
        if (result.getUser() != User.NONE) {
            boolean isPartOfTurn = false;
            if (turn != null) {     // has latest turn
                if (turn.getUser() == result.getUser()) {   // Same User
                    if (result.getStartTime() - turn.getEndTime() <= timeTolerancePauseBetweenFrames) {
//                        Log.d(TAG, "TurnEndTime: " + turn.getEndTime() + ", ResultStartTime: " +
//                                result.getStartTime() + ", Tolerance: " + timeTolerancePauseBetweenFrames);

                        // extend the latest turn's length
                        turn.setEndTime(turn.getEndTime() + AudioProcessingResult.frameLength);

                        // add in the onsets
                        turn.addOnsets(result.getOnsets());

                        isPartOfTurn = true;    // it was part of the latest turn

//                        Log.d(TAG, "Part of latest turn!");
                    }
                }
            }
            // was not part of turn, add new turn
            if (!isPartOfTurn) {
                add(new Turn(result));
            }
        }
        // Event Detection happens when nobody speaks for a period after speaking
        else if (turn != null) {
            float timeDiff = result.getStartTime() - turn.getEndTime();
            if (timeDiff >= timeToleranceSilenceAfterFrames && timeDiff <
                    timeToleranceSilenceAfterFrames + AudioProcessingResult.frameLength) {
                return true;    // trigger event detection
            }
        }

        return false;
    }

    public List<Turn> getTurnsFor(User user) {
        List<Turn> userTurns = new ArrayList<>();
        for (Turn turn : this) {
            if (turn.getUser() == user) {
                userTurns.add(turn);
            }
        }
        return userTurns;
    }

    public Turn getLatestTurn() {
        return isEmpty() ? null : get(size() - 1);
    }

}
