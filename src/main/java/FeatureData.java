/**
 * Created by KennyChoo on 4/5/17.
 */
public class FeatureData {
    private String featureVector;

    public FeatureData(String featureVector) {
        this.featureVector = featureVector;
    }

    public User getUser() {
        String[] tokens = featureVector.split(" ");
        String userString = tokens[tokens.length - 1];

        switch (userString) {
            case "ADULT":
                return User.ADULT;
            case "CHILD":
                return User.CHILD;
            default:
                return User.NONE;
        }
    }

    public String toString() {
        return featureVector;
    }
}
