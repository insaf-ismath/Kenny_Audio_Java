/**
 * Created by KennyChoo on 3/5/17.
 */
public enum User {
    NONE,
    ADULT,
    CHILD;

    // to cache the result
    private static final User[] v = values();

    public static User is(double ordinalValue) {
        return v[(int) ordinalValue];
    }
}
