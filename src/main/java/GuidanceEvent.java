/**
 * Created by KennyChoo on 12/5/17.
 */
public class GuidanceEvent {
    public static final int numGuidanceEventTypes = 5;
    public static final int OVERLAP = 0;
    public static final int CONTINUED_TURNS = 1;
    public static final int NO_RESPONSE = 2;
    public static final int LONG_TURN = 3;
    public static final int FAST_SPEECH = 4;


    private float startTime;  // startTime of the GuidanceEvent
    private float endTime;
    public int id;  // GuidanceEvent Types

    public GuidanceEvent(float startTime, int id) {
        this.startTime = startTime;
        this.endTime = startTime + AudioProcessingResult.frameLength;
        this.id = id;
    }

    public String toString() {
        return startTime + "\t" + endTime + "\t" + id;
    }
}