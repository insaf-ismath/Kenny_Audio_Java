import be.tarsos.dsp.pitch.PitchDetectionResult;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by KennyChoo on 3/5/17.
 */
public class AudioProcessingResult {
    private static final String TAG = "AudioProcessingResult";
    private boolean isVoiced = false;
    private float startTime, endTime;
    private float dbSPL;    // sound pressure level / intensity
    private float pitch;
    private float pitchProbability;
    private float[] mfcc;
    private float[] dmfcc;
    private float[] ddmfcc;
    private ArrayList<Onset> onsets = new ArrayList<>();
    private User user = User.NONE;
    private int[] eventBuffer = new int[5];
    public Instances instances = generateInstancesHeader();
    public static final float frameLength = ((float) WAVProcessor.BUFFER_SIZE) /
            ((float) WAVProcessor.SAMPLE_RATE);

    public AudioProcessingResult() {}

    public AudioProcessingResult(String arffString) {
        String[] t = arffString.split("\\s*,\\s*");
        this.startTime = Float.parseFloat(t[0]);
        this.endTime = startTime + frameLength;
        this.dbSPL = Float.parseFloat(t[1]);
        this.pitch = Float.parseFloat(t[2]);
        this.pitchProbability = pitchProbability;   // feature is stored, but not in use
        this.mfcc = new float[13];
        this.mfcc[0] = Float.parseFloat(t[3]);
        this.mfcc[1] = Float.parseFloat(t[4]);
        this.mfcc[2] = Float.parseFloat(t[5]);
        this.mfcc[3] = Float.parseFloat(t[6]);
        this.mfcc[4] = Float.parseFloat(t[7]);
        this.mfcc[5] = Float.parseFloat(t[8]);
        this.mfcc[6] = Float.parseFloat(t[9]);
        this.mfcc[7] = Float.parseFloat(t[10]);
        this.mfcc[8] = Float.parseFloat(t[11]);
        this.mfcc[9] = Float.parseFloat(t[12]);
        this.mfcc[10] = Float.parseFloat(t[13]);
        this.mfcc[11] = Float.parseFloat(t[14]);
        this.mfcc[12] = Float.parseFloat(t[15]);
        switch (t[16]) {
            case "ADULT":
                this.user = User.ADULT;
                break;
            case "CHILD":
                this.user = User.CHILD;
                break;
            default:
                this.user = User.NONE;
        }
    }

    public AudioProcessingResult(double startTime, double dbSPL, double pitch,
                                 double mfcc01, double mfcc02, double mfcc03, double mfcc04,
                                 double mfcc05, double mfcc06, double mfcc07, double mfcc08,
                                 double mfcc09, double mfcc10, double mfcc11, double mfcc12,
                                 double mfcc13, User user) {
        this(startTime, dbSPL, pitch, -1,
                mfcc01, mfcc02, mfcc03, mfcc04, mfcc05,
                mfcc06, mfcc07, mfcc08, mfcc09, mfcc10,
                mfcc11, mfcc12, mfcc13, user);
    }

    public AudioProcessingResult(double startTime, double dbSPL, double pitch, double pitchProbability,
                                 double mfcc01, double mfcc02, double mfcc03, double mfcc04,
                                 double mfcc05, double mfcc06, double mfcc07, double mfcc08,
                                 double mfcc09, double mfcc10, double mfcc11, double mfcc12,
                                 double mfcc13, User user) {
        this((float) startTime, (float) dbSPL, (float) pitch, (float) pitchProbability,
                (float) mfcc01, (float) mfcc02, (float) mfcc03, (float) mfcc04, (float) mfcc05,
                (float) mfcc06, (float) mfcc07, (float) mfcc08, (float) mfcc09, (float) mfcc10,
                (float) mfcc11, (float) mfcc12, (float) mfcc13, user);
    }

    public AudioProcessingResult(float startTime, float dbSPL, float pitch,
                                 float mfcc01, float mfcc02, float mfcc03, float mfcc04,
                                 float mfcc05, float mfcc06, float mfcc07, float mfcc08,
                                 float mfcc09, float mfcc10, float mfcc11, float mfcc12,
                                 float mfcc13, User user) {
        this(startTime, dbSPL, pitch, -1,
                mfcc01, mfcc02, mfcc03, mfcc04, mfcc05,
                mfcc06, mfcc07, mfcc08, mfcc09, mfcc10,
                mfcc11, mfcc12, mfcc13, user);
    }

    public AudioProcessingResult(float startTime, float dbSPL, float pitch, float pitchProbability,
                                 float mfcc01, float mfcc02, float mfcc03, float mfcc04,
                                 float mfcc05, float mfcc06, float mfcc07, float mfcc08,
                                 float mfcc09, float mfcc10, float mfcc11, float mfcc12,
                                 float mfcc13, User user) {
        this.startTime = startTime;
        this.endTime = startTime + frameLength;
        this.dbSPL = dbSPL;
        this.pitch = pitch;
        this.pitchProbability = pitchProbability;   // feature is stored, but not in use
        this.mfcc = new float[13];
        this.mfcc[0] = mfcc01;
        this.mfcc[1] = mfcc02;
        this.mfcc[2] = mfcc03;
        this.mfcc[3] = mfcc04;
        this.mfcc[4] = mfcc05;
        this.mfcc[5] = mfcc06;
        this.mfcc[6] = mfcc07;
        this.mfcc[7] = mfcc08;
        this.mfcc[8] = mfcc09;
        this.mfcc[9] = mfcc10;
        this.mfcc[10] = mfcc11;
        this.mfcc[11] = mfcc12;
        this.mfcc[12] = mfcc13;
        this.user = user;
    }

    public float getStartTime() {
        return startTime;
    }

    public void setStartTime(float startTime) {
        this.startTime = startTime;
    }

    public float getEndTime() {
        return startTime + frameLength;
    }

    public void setEndTime(float endTime) {
        this.endTime = endTime;
    }

    public float getDbSPL() {
        return dbSPL;
    }

    public void setDbSPL(float dbSPL) {
        this.dbSPL = dbSPL;
    }

    public void setPitchDetectionResult(PitchDetectionResult pitchDetectionResult) {
        if (isVoiced) {
            this.pitch = pitchDetectionResult.getPitch();
            this.pitchProbability = pitchDetectionResult.getProbability();
        }
        else {
            this.pitch = 0;
            this.pitchProbability = 0;
        }
    }

    public float getPitch() {
        if (isVoiced)
            return pitch;
        else return 0;
    }

    public float getPitchProbability() { return pitchProbability; }

    public float[] getMFCC() {
        return mfcc;
    }

    public void setMFCC(float[] mfcc) {
        this.mfcc = mfcc;
    }

    public float[] getDMFCC() {
        return this.dmfcc;
    }

    public void setDMFCC(float[] dmfcc) {
        this.dmfcc = dmfcc;
    }

    public float[] getDDMFCC() {
        return this.ddmfcc;
    }

    public void setDDMFCC(float[] ddmfcc) {
        this.ddmfcc = ddmfcc;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    private static Instances generateInstancesHeader() {
        ArrayList<Attribute> attributeList = new ArrayList<>();

        // construct the class values
        ArrayList<String> classVal = new ArrayList<>();
        classVal.add("NONE");
        classVal.add("ADULT");
        classVal.add("CHILD");

        attributeList.add(new Attribute("startTime"));
        attributeList.add(new Attribute("dbspl"));
        attributeList.add(new Attribute("pitch"));
        attributeList.add(new Attribute("mfcc01"));
        attributeList.add(new Attribute("mfcc02"));
        attributeList.add(new Attribute("mfcc03"));
        attributeList.add(new Attribute("mfcc04"));
        attributeList.add(new Attribute("mfcc05"));
        attributeList.add(new Attribute("mfcc06"));
        attributeList.add(new Attribute("mfcc07"));
        attributeList.add(new Attribute("mfcc08"));
        attributeList.add(new Attribute("mfcc09"));
        attributeList.add(new Attribute("mfcc10"));
        attributeList.add(new Attribute("mfcc11"));
        attributeList.add(new Attribute("mfcc12"));
        attributeList.add(new Attribute("mfcc13"));
        attributeList.add(new Attribute("class", classVal));

        Instances data = new Instances("Test", attributeList, 0);

        data.setClassIndex(attributeList.size() - 1);

        return data;
    }

    public Instance getInstance() {
        Instance instance = new DenseInstance(17);

        Attribute time = new Attribute("startTime", 0);
        Attribute dbspl = new Attribute("dbspl", 1);
        Attribute pitch = new Attribute("pitch", 2);
        Attribute mfcc01 = new Attribute("mfcc01", 3);
        Attribute mfcc02 = new Attribute("mfcc02", 4);
        Attribute mfcc03 = new Attribute("mfcc03", 5);
        Attribute mfcc04 = new Attribute("mfcc04", 6);
        Attribute mfcc05 = new Attribute("mfcc05", 7);
        Attribute mfcc06 = new Attribute("mfcc06", 8);
        Attribute mfcc07 = new Attribute("mfcc07", 9);
        Attribute mfcc08 = new Attribute("mfcc08", 10);
        Attribute mfcc09 = new Attribute("mfcc09", 11);
        Attribute mfcc10 = new Attribute("mfcc10", 12);
        Attribute mfcc11 = new Attribute("mfcc11", 13);
        Attribute mfcc12 = new Attribute("mfcc12", 14);
        Attribute mfcc13 = new Attribute("mfcc13", 15);
//        List<String> nominal_values = new ArrayList(3);
//        nominal_values.add("NONE");
//        nominal_values.add("ADULT");
//        nominal_values.add("CHILD");
//        Attribute person = new Attribute("class", nominal_values, 16);
        instance.setValue(time, this.startTime);
        instance.setValue(dbspl, this.dbSPL);
        instance.setValue(pitch, this.pitch);
        instance.setValue(mfcc01, this.mfcc[0]);
        instance.setValue(mfcc02, this.mfcc[1]);
        instance.setValue(mfcc03, this.mfcc[2]);
        instance.setValue(mfcc04, this.mfcc[3]);
        instance.setValue(mfcc05, this.mfcc[4]);
        instance.setValue(mfcc06, this.mfcc[5]);
        instance.setValue(mfcc07, this.mfcc[6]);
        instance.setValue(mfcc08, this.mfcc[7]);
        instance.setValue(mfcc09, this.mfcc[8]);
        instance.setValue(mfcc10, this.mfcc[9]);
        instance.setValue(mfcc11, this.mfcc[10]);
        instance.setValue(mfcc12, this.mfcc[11]);
        instance.setValue(mfcc13, this.mfcc[12]);
//        instance.setValue(person, this.user.toString());

        instance.setDataset(instances);

        return instance;
    }

    public boolean isVoiced() {
        return isVoiced;
    }

    public void setVoiced(boolean voiced) {
        isVoiced = voiced;
    }

    public int[] getEventBuffer() {
        return eventBuffer;
    }

    public void setEventBuffer(int[] eventBuffer) {
        this.eventBuffer = eventBuffer;
    }

    public void addOnset(Onset onset) {
        onsets.add(onset);
    }

    public ArrayList<Onset> getOnsets() {
        return onsets;
    }


    /**
     * Classifies the feature vector to identify which user it is.
     *
     * This method also sets the User for this AudioProcessingResult.
     *
     * @param classifier The classifier used to classify which user it is.
     * @return The classification result, the User based on this set of features.
     */
    public User runClassifier(Classifier classifier) {
        double result = 0.0;    // default: User.NONE
        try {
            result = classifier.classifyInstance(getInstance());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        setUser(User.is(result));
        return user;
    }

    /**
     * This is the vector that gets written into the ARFF
     *
     * @return
     */
    public String getFeatureVector() {
        return
//                startTime + "," +
                    mfcc[1] + "," + mfcc[2]  + "," + mfcc[3]  + "," + mfcc[4]  + "," +
                    mfcc[5] + "," + mfcc[6]  + "," + mfcc[7]  + "," + mfcc[8]  + "," +
                    mfcc[9] + "," + mfcc[10] + "," + mfcc[11] + "," + mfcc[12] + "," +
                    dmfcc[0] + "," + dmfcc[1] + "," + dmfcc[2]  + "," + dmfcc[3]  + "," +
                    dmfcc[4] + "," + dmfcc[5] + "," + dmfcc[6]  + "," + dmfcc[7]  + "," +
                    dmfcc[8] + "," + dmfcc[9] + "," + dmfcc[10] + "," + dmfcc[11] + "," +
                    ddmfcc[0] + "," + ddmfcc[1] + "," + ddmfcc[2]  + "," + ddmfcc[3]  + "," +
                    ddmfcc[4] + "," + ddmfcc[5] + "," + ddmfcc[6]  + "," + ddmfcc[7]  + "," +
                    ddmfcc[8] + "," + ddmfcc[9] + "," + ddmfcc[10] + "," + ddmfcc[11] + "," + user;
    }

    public static String getLogHeaderString() {
        return "startTime, dbSPL, pitch, " +
                "mfcc01, mfcc02, mfcc03, mfcc04, mfcc05, mfcc06, mfcc07, " +
                "mfcc08, mfcc09, mfcc10, mfcc11, mfcc12, mfcc13, " +
                "user, overlap, continuedTurns, noResponse, longTurn, fastSpeech";
    }

    public String getLogString() {
        return startTime + ", " + dbSPL + ", " + getPitch() + ", "
                + Arrays.toString(mfcc).replace("[","").replace("]", "") + ", "
                + user + ", " + eventBuffer[0] + ", "
                + eventBuffer[1] + ", " + eventBuffer[2] + ", " + eventBuffer[3] + ", " + eventBuffer[4];
    }

    public String toString() {
        return "StartTime: " + startTime + ", dbSPL: " + dbSPL + ", Pitch: " + getPitch() + ", " +
                "mfcc: "
                + Arrays.toString(mfcc).replace("[","{").replace("]", "}") + ", User: "
                + user;
    }


}
