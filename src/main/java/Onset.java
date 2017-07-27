/**
 * Created by KennyChoo on 3/5/17.
 */
public class Onset {
    public final double time;
    public final double salience;

    public Onset(double time, double salience) {
        this.time = time;
        this.salience = salience;
    }

    public String toString() {
        return "(" + time + ", " + salience + ")";
    }
}

