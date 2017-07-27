import java.util.ArrayList;

/**
 * Created by KennyChoo on 12/5/17.
 */
public class Turn {
    private static final String TAG = "Turn";

    private float startTime = 0;
    private float endTime = 0;
    private User user = User.NONE;
    private String content = "";    // transcript of speech in that turn
    private ArrayList<Onset> onsets = null;

    public Turn(float startTime, float endTime, User user, ArrayList<Onset> onsets) {
        this(startTime, endTime, user, "", onsets);
    }

    public Turn(float startTime, float endTime, User user, String content, ArrayList<Onset>
            onsets) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.user = user;
        this.content = content;
        this.onsets = onsets;
    }

    public Turn(AudioProcessingResult result) {
        this(result.getStartTime(), result.getStartTime() + AudioProcessingResult.frameLength,
                result.getUser(), result.getOnsets());
    }

    public float getStartTime() {
        return startTime;
    }

    public void setStartTime(float startTime) {
        this.startTime = startTime;
    }

    public float getEndTime() {
        return endTime;
    }

    public void setEndTime(float endTime) {
        this.endTime = endTime;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ArrayList<Onset> getOnsets() {
        return onsets;
    }

    public void addOnsets(ArrayList<Onset> onsets) {
        this.onsets.addAll(onsets);
    }

    public void setOnsets(ArrayList<Onset> onsets) {
        this.onsets = onsets;
    }

    public float getTurnLength() {
        return endTime - startTime;
    }

    public static long timeFloatToLongMilliseconds(float timeFloatInSeconds) {
        return (long) (timeFloatInSeconds * 1000);
    }

    public static String timeConvertToMMSSsss(float timeFloatInSeconds) {
        long time = timeFloatToLongMilliseconds(timeFloatInSeconds);
        long millis = time % 1000;
        long timeLongInSeconds = time / 1000;
        long seconds = timeLongInSeconds % 60;
        long minutes = timeLongInSeconds / 60;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }

    public static float timeConvertMMSSsssToFloatInSeconds(String timeString) {
        long result = 0;
        String[] tokens = timeString.trim().split(":|\\.");
        result += Long.parseLong(tokens[0]) * 60 * 1000;
        result += Long.parseLong(tokens[1]) * 1000;
        result += Long.parseLong(tokens[2]);
        return ((float) result) / 1000;
    }

    public String toLongFormString() {
        return timeConvertToMMSSsss(startTime) + "\t" + timeConvertToMMSSsss(endTime) + "\t" +
                user + "\t" + onsets + "\t" + content;
    }

    public String toString() {
        return startTime + "\t" + endTime + "\t" + user + "\t" + onsets + "\t" + content;
    }
}
