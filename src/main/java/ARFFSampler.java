import java.io.*;
import java.util.ArrayList;

/**
 * Created by KennyChoo on 4/5/17.
 */
public class ARFFSampler {

    public static final String filepath = "./bin/";
    public static final float trainPercentage = 0.2f;

    public static void main(String[] args) {
        split(trainPercentage,"P01-A");
        split(trainPercentage,"P03-A");
        split(trainPercentage,"P04-A");
        split(trainPercentage,"P06-A");
        split(trainPercentage,"P07-A");
        split(trainPercentage,"P01-C");
        split(trainPercentage,"P03-C");
        split(trainPercentage,"P04-C");
        split(trainPercentage,"P06-C");
        split(trainPercentage,"P07-C");
    }

    public static void split(float trainPercentage, String filename) {
        String file = filepath + filename;

        // Read the file
        try {
            BufferedReader br = new BufferedReader(new FileReader(file + ".arff"));

            // Skip over the header and the @Relation bits to get to the @FeatureData
            int linesToSkip = 27;
            while (linesToSkip-- > 0) {
                br.readLine();
            }

            ArrayList<FeatureData> noneList = new ArrayList<>();
            ArrayList<FeatureData> adultList = new ArrayList<>();
            ArrayList<FeatureData> childList = new ArrayList<>();

            String str;
            while ((str = br.readLine()) != null) {
                FeatureData featureData = new FeatureData(str);

                switch (featureData.getUser()) {
                    case ADULT:
                        adultList.add(featureData);
                        break;
                    case CHILD:
                        childList.add(featureData);
                        break;
                    default:
                        noneList.add(featureData);
                }
            }
            br.close();

            // split the files
            int adultSplit = (int) (adultList.size() * trainPercentage);
            int childSplit = (int) (childList.size() * trainPercentage);
            int noneSplit = (int) (noneList.size() * trainPercentage);

            // Write out the file
            BufferedWriter bwTrain = new BufferedWriter(new FileWriter(file + " train.arff", false));
            BufferedWriter bwTest = new BufferedWriter(new FileWriter(file + " test.arff", false));
            bwTrain.write(AudioUtils.getARFFHeader(AudioUtils.getFilenameFromPathfilename(file + "train")));
            bwTest.write(AudioUtils.getARFFHeader(AudioUtils.getFilenameFromPathfilename(file + "test")));

            for (int i = 0, il = adultList.size(); i < il; i++) {
                FeatureData featureData = adultList.get(i);
                if (i < adultSplit) {
                    bwTrain.write(featureData.toString());
                    bwTrain.newLine();
                }
                else {
                    bwTest.write(featureData.toString());
                    bwTest.newLine();
                }
            }
            for (int i = 0, il = childList.size(); i < il; i++) {
                FeatureData featureData = childList.get(i);
                if (i < childSplit) {
                    bwTrain.write(featureData.toString());
                    bwTrain.newLine();
                }
                else {
                    bwTest.write(featureData.toString());
                    bwTest.newLine();
                }
            }
            for (int i = 0, il = noneList.size(); i < il; i++) {
                FeatureData featureData = noneList.get(i);
                if (i < noneSplit) {
                    bwTrain.write(featureData.toString());
                    bwTrain.newLine();
                }
                else {
                    bwTest.write(featureData.toString());
                    bwTest.newLine();
                }
            }
            bwTrain.close();
            bwTest.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}
