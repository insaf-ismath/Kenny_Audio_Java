import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;
import weka.filters.unsupervised.attribute.AddID;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by KennyChoo on 13/5/17.
 */
public class PredictionProcessor {
    public static final String filepath = "./bin/";

    public static void main(String[] args) {
        Instances data = readARFF(filepath, "P01raw");

        System.out.println("Num of original data instances: " + data.size());

        AddID addIDfilter = new AddID();

        Instances filteredInstances = null;
        try {
            addIDfilter.setInputFormat(data);
            filteredInstances = Filter.useFilter(data, addIDfilter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(filteredInstances);

//        Resample filter = new Resample();
//        Instances filteredIns = null;
//        filter.setBiasToUniformClass(1.0);
//        try {
//            filter.setInputFormat(data);
//            filter.setNoReplacement(false);
//            filter.setSampleSizePercent(100);
//            filteredIns = Filter.useFilter(data, filter);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }

    public static Instances readARFF(String filepath, String filename) {
        Instances data = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(filepath + filename + ".arff"));

            data = new Instances(br);
            data.setClassIndex(data.numAttributes() - 1);

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static Instances generateInstancesHeader() {
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
}
