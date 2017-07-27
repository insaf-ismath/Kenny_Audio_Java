import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.mfcc.MFCC;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.writer.WaveHeader;
import weka.classifiers.Classifier;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by KennyChoo on 3/5/17.
 */
public class AudioUtils {
    private AudioUtils() {
    }

    /**
     * Converts an array of samples from short to float.
     *
     * @param pcms Array of short PCM samples that range from {@value Short#MIN_VALUE} to {@value
     *             Short#MAX_VALUE}.
     * @return Array of converted floats
     */
    public static float[] shorts2Floats(short[] pcms) {
        float[] floaters = new float[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            floaters[i] = pcms[i];
        }
        return floaters;
    }

    /**
     * Converts an array of samples from float to short.
     *
     * @param pcms Array of float PCM samples that range from -1.0 to 1.0.
     * @return Array of converted shorts
     */
    public static short[] floats2Shorts(float[] pcms) {
        short[] shortBuffer = new short[pcms.length];
        for (int i = 0, il = pcms.length; i < il; i++) {
            shortBuffer[i] = (short) (pcms[i] * Short.MAX_VALUE);
        }
        return shortBuffer;
    }

    /**
     * Converts an array of samples from float to byte.
     *
     * @param pcms Array of float PCM samples that range from -1.0 to 1.0.
     * @return Array of converted bytes in Little Endian format.
     */
    public static byte[] floats2Bytes(float[] pcms) {
        byte[] bytes = new byte[pcms.length * 2];
        short[] shorts = floats2Shorts(pcms);

        for (int i = 0, il = shorts.length; i < il; i++) {
            ByteBuffer bb = ByteBuffer.allocate(2);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putShort(shorts[i]);
            bytes[2 * i] = bb.get(0);
            bytes[2 * i + 1] = bb.get(1);
        }

        return bytes;
    }

    /**
     * Converts an array of samples from short to byte.
     *
     * @param pcms Array of short PCM samples that range from {@value Short#MIN_VALUE} to {@value
     *             Short#MAX_VALUE}.
     * @return Array of converted bytes in Little Endian format.
     */
    public static byte[] shorts2Bytes(short[] pcms) {
        byte[] bytes = new byte[pcms.length * 2];
        for (int i = 0; i < pcms.length; i++) {
            // using little-endian format
            bytes[i * 2] = (byte) pcms[i];
            bytes[(i * 2) + 1] = (byte) (pcms[i] >> 8);
        }
        return bytes;
    }

    /**
     * Converts an array of samples from byte to short.
     *
     * @param pcms Array of byte PCM samples in Little Endian format.
     * @return Array of converted shorts that range from {@value Short#MIN_VALUE} to {@value
     * Short#MAX_VALUE}.
     */
    public static short[] bytes2Shorts(byte[] pcms) {
        short[] shorts = new short[pcms.length / 2];
        ByteBuffer.wrap(pcms).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        return shorts;
    }

    /**
     * Converts an array of samples from byte to float.
     *
     * @param pcms Array of byte PCM samples in Little Endian format.
     * @return Array of converted floats.
     */
    public static float[] bytes2Floats(byte[] pcms) {
        short[] shorts = new short[pcms.length / 2];
        float[] floats = new float[pcms.length / 2];
        ByteBuffer.wrap(pcms).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

        for (int i = 0, il = shorts.length; i < il; i++) {
            float x = shorts[i];

            // normalise since shorts range from -32768 to 32767
            if (x < 0) {
                x /= 32768;
            } else {
                x /= 32767;
            }

            floats[i] = x;
        }
        return floats;
    }

    public static double computeRMSSoundPressureLevel(List<Short> pcmValues) {
        double sum = 0;
        for (short val : pcmValues) {
            double value = (double) val * WAVProcessor.GAIN;
            sum += value * value;
        }
        sum /= Math.pow((double) Short.MAX_VALUE, 2);
        return 20 * Math.log10(Math.sqrt(sum / pcmValues.size()));
    }

    public static boolean isOverlapping(long pStart, long pEnd, long cStart, long cEnd) {
        return cStart <= pEnd && pStart <= cEnd;
    }

    public static short[] computeSamplesWithGain(short[] samples) {
        short[] output = new short[samples.length];

        for (int i = 0, il = samples.length; i < il; i++) {
            output[i] = (short) limitShort(samples[i] * WAVProcessor.GAIN);
        }
        return output;
    }

    public static float limitShort(float value) {
        return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
    }


    /**
     * Writes out the WAV file to the specified file path and name.
     *
     * @param filePathName  Full path and filepath, including extension
     * @param pcmsData      The PCM data as a little Endian byte array.
     * @param samplesFormat {@link be.tarsos.dsp.writer.WaveHeader}
     * @param numChannels   Number of channels of the audio signal.
     * @param sampleRate    Sample rate. E.g., 8000, 16000, 22050, 44100, 48000 Hz
     * @param bitsPerSample Number of bits per sample. E.g., 16 bits per sample is usaul for PCM.
     */
    public static void writeWAVFile(String filePathName, byte[] pcmsData, short
            samplesFormat, short numChannels, int sampleRate, short bitsPerSample) {
        // Set up the file to write to
        try {
            RandomAccessFile output = new RandomAccessFile(filePathName, "rws");

            // Set up the wave header writer from TarsosDSP
            WaveHeader wh = new WaveHeader(samplesFormat, numChannels, sampleRate, bitsPerSample,
                    pcmsData.length);
            ByteArrayOutputStream header = new ByteArrayOutputStream();

            // Prepare and write the header
            wh.write(header);   // generate the header
            output.seek(0); // go to the start of the file
            output.write(header.toByteArray()); // write out the header

            // write out the data
            output.write(pcmsData);

            // close the stream
            output.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes out the WAV file to the specified folder.
     *
     * @param filePathName  Full path and filepath, including extension
     * @param pcmsData      The PCM data as a little Endian float array.
     * @param samplesFormat {@link be.tarsos.dsp.writer.WaveHeader}
     * @param numChannels   Number of channels of the audio signal.
     * @param sampleRate    Sample rate. E.g., 8000, 16000, 22050, 44100, 48000 Hz
     * @param bitsPerSample Number of bits per sample. E.g., 16 bits per sample is usaul for PCM.
     */
    public static void writeWAVFile(String filePathName, float[] pcmsData, short
            samplesFormat, short numChannels, int sampleRate, short bitsPerSample) {
        writeWAVFile(filePathName, floats2Bytes(pcmsData), samplesFormat, numChannels, sampleRate,
                bitsPerSample);
    }

    /**
     * Gets a formatted date-startTime string.
     *
     * @return A date-startTime string of the format "yyyyMMdd-HHmmss".
     */
    public static String getDateTimeString() {
        Date date = new Date(System.currentTimeMillis());
        DateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss");
        return formatter.format(date);
    }

    /**
     * Creates a TarsosDSP AudioDispatcher from a file.
     * <p>
     * We assume a WAV file, 16 bits PCM, signed, little Endian format.
     *
     * @param audioFile       the data file
     * @param sampleRate
     * @param audioBufferSize
     * @param bufferOverlap
     * @return
     * @throws FileNotFoundException
     */
    public static AudioDispatcher audioDispatcherFromFile(final File audioFile, final int
            sampleRate, final int audioBufferSize, final int bufferOverlap) {

        TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, false);

        TarsosDSPAudioInputStream audioStream = null;
        try {
            audioStream = new AndroidFileInputStream(new FileInputStream
                    (audioFile), format);
            audioStream.skip(44);   // skip the WAV header
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new AudioDispatcher(audioStream, audioBufferSize, bufferOverlap);
    }

    public static double computedBSPL(float[] audioData) {
        double pRef = 0.00002;  // reference leve: threshold of human hearing
        double pRMS = 0;

        double[] sqpow = squaredPower(audioData);
        for (double d : sqpow) {
            pRMS += d;
        }
        pRMS /= audioData.length;
        pRMS = Math.pow(pRMS, 0.5);

        return 20 * Math.log10(pRMS / pRef);
    }

    public static double[] squaredPower(float[] data) {
        double[] a = new double[data.length];

        for (int i = 0, il = a.length; i < il; i++) {
            a[i] = data[i] * data[i];
        }

        return a;
    }

    /**
     * This is part of the ARFF writer for the adult voice. Uses pitch as well as dbSPL to
     * determine whether a frame of features is an adult or child or not.
     *
     * @param dbSPL
     * @param pitch
     * @param userToCheck
     * @return
     */
    public static User checkVolume(float dbSPL, float pitch, User userToCheck) {
        /**
         * Typical adult male:   85 - 155 Hz
         * Typical adult female: 165 - 255 Hz
         * Typical child:        250 - 650 Hz
         */
        // high upper bound to help with animated speaking
        float adultLowerBound = 85, adultUpperBound = 255;
        float childLowerBound = 256, childUpperBound = 650;
        float dbSPLTolerance = 40;
        User user = User.NONE;

        if (dbSPL >= dbSPLTolerance) {
            user = userToCheck;
        }

        // Original code. The problem with pitch checking is that some frames are NOT pitched!
//        if (dbSPL >= dbSPLTolerance) {
//            if (userToCheck == User.ADULT) {  // checking to see if Adult
//                if (pitch >= adultLowerBound && pitch <= adultUpperBound) {
//                    user = User.ADULT;
//                }
//            }
//            else {  // checking to see if Child
//                if (pitch >= childLowerBound && pitch <= childUpperBound) {
//                    user = User.CHILD;
//                }
//            }
//        }

        return user;
    }

    /**
     * Writes the header part of an ARFF file for CommBetter audio features.
     *
     * @return the header as a String
     */
    public static String getARFFHeader(String filename) {
        Date date = new Date(System.currentTimeMillis());
        DateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String datetime = formatter.format(date);

        return "%\n" +
                "% CommBetter Audio Features.\n" +
                "% \n" +
                "% (a) Created (yyyyMMdd-HHmmss): " + datetime + "\n" +
                "% (b) Other info: \n" +
                "%\n" +
                "@RELATION " + filename.replaceAll("\\s+", "_") + "\n\n" +
//                "@ATTRIBUTE startTime NUMERIC\n" +
//                "@ATTRIBUTE dbspl NUMERIC\n" +
//                "@ATTRIBUTE pitch NUMERIC\n" +
//                "@ATTRIBUTE mfcc01 NUMERIC\n" +
                "@ATTRIBUTE mfcc01 NUMERIC\n" +
                "@ATTRIBUTE mfcc02 NUMERIC\n" +
                "@ATTRIBUTE mfcc03 NUMERIC\n" +
                "@ATTRIBUTE mfcc04 NUMERIC\n" +
                "@ATTRIBUTE mfcc05 NUMERIC\n" +
                "@ATTRIBUTE mfcc06 NUMERIC\n" +
                "@ATTRIBUTE mfcc07 NUMERIC\n" +
                "@ATTRIBUTE mfcc08 NUMERIC\n" +
                "@ATTRIBUTE mfcc09 NUMERIC\n" +
                "@ATTRIBUTE mfcc10 NUMERIC\n" +
                "@ATTRIBUTE mfcc11 NUMERIC\n" +
                "@ATTRIBUTE mfcc12 NUMERIC\n" +
                "@ATTRIBUTE dmfcc01 NUMERIC\n" +
                "@ATTRIBUTE dmfcc02 NUMERIC\n" +
                "@ATTRIBUTE dmfcc03 NUMERIC\n" +
                "@ATTRIBUTE dmfcc04 NUMERIC\n" +
                "@ATTRIBUTE dmfcc05 NUMERIC\n" +
                "@ATTRIBUTE dmfcc06 NUMERIC\n" +
                "@ATTRIBUTE dmfcc07 NUMERIC\n" +
                "@ATTRIBUTE dmfcc08 NUMERIC\n" +
                "@ATTRIBUTE dmfcc09 NUMERIC\n" +
                "@ATTRIBUTE dmfcc10 NUMERIC\n" +
                "@ATTRIBUTE dmfcc11 NUMERIC\n" +
                "@ATTRIBUTE dmfcc12 NUMERIC\n" +
                "@ATTRIBUTE ddmfcc01 NUMERIC\n" +
                "@ATTRIBUTE ddmfcc02 NUMERIC\n" +
                "@ATTRIBUTE ddmfcc03 NUMERIC\n" +
                "@ATTRIBUTE ddmfcc04 NUMERIC\n" +
                "@ATTRIBUTE ddmfcc05 NUMERIC\n" +
                "@ATTRIBUTE ddmfcc06 NUMERIC\n" +
                "@ATTRIBUTE ddmfcc07 NUMERIC\n" +
                "@ATTRIBUTE ddmfcc08 NUMERIC\n" +
                "@ATTRIBUTE ddmfcc09 NUMERIC\n" +
                "@ATTRIBUTE ddmfcc10 NUMERIC\n" +
                "@ATTRIBUTE ddmfcc11 NUMERIC\n" +
                "@ATTRIBUTE ddmfcc12 NUMERIC\n" +
                "@ATTRIBUTE class {NONE, ADULT, CHILD}\n" +
                "\n@DATA\n";
    }

    /**
     * Merges a set of ARFF files together. This does not overwrite, but merely combines.
     *
     * @param file1  path, filename, no extensions
     * @param file2  path, filename, no extensions
     * @param mergedFile path, filename, no extensions
     */
    public static void mergeFiles(String file1, String file2, String mergedFile) {
        try {
            // check that the attributes match, else throw an exception
            boolean isSameType = true;

            BufferedWriter bw;
            BufferedReader br1 = null, br2 = null;
            br1 = new BufferedReader(new FileReader(file1 + ".arff"));
            br2 = new BufferedReader(new FileReader(file2 + ".arff"));

            // Skip over the header and the @Relation bits to get to the @Attribute
            int linesToSkip = 8;
            while (linesToSkip-- > 0) {
                br1.readLine();
                br2.readLine();
            }

            // Check the next 14 lines of attributes
            int linesToCompare = 14;
            while (linesToCompare-- > 0) {
                String s1 = br1.readLine();
                String s2 = br2.readLine();

                if (!s1.equals(s2)) {
                    isSameType = false;
                }
            }

            // merge the files
            if (isSameType) {
                // skip the last two lines to get to the data
                linesToSkip = 2;
                while (linesToSkip-- > 0) {
                    br1.readLine();
                    br2.readLine();
                }

                bw = new BufferedWriter(new FileWriter(mergedFile + ".arff"));

                // write the header
                bw.write(AudioUtils.getARFFHeader(getFilenameFromPathfilename(mergedFile)));

                // write from file1
                String str = null;
                while ((str = br1.readLine()) != null) {
                    bw.write(str);
                    bw.newLine();
                }

                // write from file2
                while ((str = br2.readLine()) != null) {
                    bw.write(str);
                    bw.newLine();
                }

            } else throw new InputMismatchException("Input ARFF files do not have the same sets of " +
                    "attributes - Cannot be merged.");

            br1.close();
            br2.close();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void rebalanceClassInARFF(String mergedFile) {

        try {
            BufferedReader br = new BufferedReader(new FileReader(mergedFile + ".arff"));

            // Skip over the header and the @Relation bits to get to the @Attribute
            int linesToSkip = 27;
            while (linesToSkip-- > 0) {
                br.readLine();
            }

            // lists to hold the data - warning, data size might be too much
            ArrayList<FeatureData> noneList = new ArrayList<>();
            ArrayList<FeatureData> adultList = new ArrayList<>();
            ArrayList<FeatureData> childList = new ArrayList<>();
            ArrayList<ArrayList<FeatureData>> lists = new ArrayList<>();
            lists.add(noneList);
            lists.add(adultList);
            lists.add(childList);

            String str = null;
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

            int sizes[] = {adultList.size(), childList.size(), noneList.size()};
            Arrays.sort(sizes);
            int middleSize = sizes[1];


            // Random subsampling of the largest array based on the size of middle array
            for (int i = 0, il = lists.size(); i < il; i++) {
                ArrayList<FeatureData> list = lists.get(i);
                if (list.size() > middleSize) {
                    Collections.shuffle(list);
                    list = new ArrayList<>(list.subList(0, middleSize));
                    lists.set(i, list);
                }
            }

            // Write out to file
            BufferedWriter bw = new BufferedWriter(new FileWriter(mergedFile + " balanced.arff", false));
            bw.write(AudioUtils.getARFFHeader(getFilenameFromPathfilename(mergedFile + " balanced")));
            for (ArrayList<FeatureData> list : lists) {
                for (FeatureData featureData : list) {
                    bw.write(featureData.toString());
                    bw.newLine();
                }
            }
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param trainPercentage
     * @param filepath        fully qualified path, no filename, no extension
     * @param file            filename, no path, no extension
     */
    public static void split(float trainPercentage, String filepath, String file) {
        // Read the file
        try {
            BufferedReader br = new BufferedReader(new FileReader(filepath + file + ".arff"));

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
            BufferedWriter bwTrain = new BufferedWriter(new FileWriter(filepath + file + " tp" + trainPercentage + " " +
                    "train.arff", false));
            BufferedWriter bwTest = new BufferedWriter(new FileWriter(filepath + file + " tp" + trainPercentage + " " +
                    "test.arff", false));
            bwTrain.write(AudioUtils.getARFFHeader(getFilenameFromPathfilename(filepath + file + " tp" +
                    trainPercentage + " train")));
            bwTest.write(AudioUtils.getARFFHeader(getFilenameFromPathfilename(filepath + file + " tp" +
                    trainPercentage + " test")));

            for (int i = 0, il = adultList.size(); i < il; i++) {
                FeatureData featureData = adultList.get(i);
                if (i < adultSplit) {
                    bwTrain.write(featureData.toString());
                    bwTrain.newLine();
                } else {
                    bwTest.write(featureData.toString());
                    bwTest.newLine();
                }
            }
            for (int i = 0, il = childList.size(); i < il; i++) {
                FeatureData featureData = childList.get(i);
                if (i < childSplit) {
                    bwTrain.write(featureData.toString());
                    bwTrain.newLine();
                } else {
                    bwTest.write(featureData.toString());
                    bwTest.newLine();
                }
            }
            for (int i = 0, il = noneList.size(); i < il; i++) {
                FeatureData featureData = noneList.get(i);
                if (i < noneSplit) {
                    bwTrain.write(featureData.toString());
                    bwTrain.newLine();
                } else {
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

    public static void skipToARFFData(BufferedReader br) throws IOException {
        if (br != null) {
//            int linesToSkip = 24;
//            while (linesToSkip-- > 0) {
            String str = "";
            while (!(str = br.readLine()) .equals("@DATA") ) {

            }
        }
    }

    public static String getFilenameFromPathfilename(String pathfilename) {
        String trimmed = pathfilename.replaceAll("\\s+", "");
        String[] tokens = trimmed.split("/");
        return tokens[tokens.length - 1];
    }

    /**
     * Takes the users from both child, adult and raw, and writes them into a new raw file.
     *
     * @param childFile child file path, filename, append, no extension
     * @param adultFile adult file path, filename, append, no extension
     * @param rawFile raw file path, filename, append, no extension
     */
    public static void writeUsersToRaw(String childFile, String adultFile, String rawFile) {
        try {
            BufferedReader brChild = new BufferedReader(new FileReader( childFile + ".arff"));
            BufferedReader brAdult = new BufferedReader(new FileReader(adultFile + ".arff"));
            BufferedReader brRaw = new BufferedReader(new FileReader(rawFile + ".arff"));
            BufferedWriter bwRawOut = new BufferedWriter(new FileWriter(rawFile + "_out.arff", false));
            bwRawOut.write(getARFFHeader(rawFile + "_out"));

            skipToARFFData(brChild);
            skipToARFFData(brAdult);
            skipToARFFData(brRaw);

            String sChild, sAdult, sRaw;
            while ((sChild = brChild.readLine()) != null
                    && (sAdult = brAdult.readLine()) != null
                    && (sRaw = brRaw.readLine()) != null) {
                String[] sc = sChild.split(",");
                String[] sa = sAdult.split(",");
                String[] sr = sRaw.split(",");

                String uc = sc[sc.length - 1];
                String ua = sa[sa.length - 1];

                String str = "NONE";
                if (ua.equals("ADULT")) {
                    str = "ADULT";
                }
                // As long as it is a child, we will overwrite
                if (uc.equals("CHILD")) {
                    str = "CHILD";
                }

                sr[sr.length - 1] = str;

                bwRawOut.write(String.join(",", sr));
                bwRawOut.newLine();
            }

            // Close all files
            brChild.close();
            brAdult.close();
            brRaw.close();
            bwRawOut.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeUsersFromTo(String filepath, String arffFileFrom, String arffFileTo) {
        // read from arffFileFrom
        ArrayList<String> arffFrom = new ArrayList<>();
        ArrayList<String> arffTo = new ArrayList<>();

        try {
            BufferedReader brFrom = new BufferedReader(new FileReader(filepath + arffFileFrom + ".arff"));
            BufferedReader brTo = new BufferedReader(new FileReader(filepath + arffFileTo + ".arff"));

            skipToARFFData(brFrom);
            skipToARFFData(brTo);

            String str1, str2;
            while ((str1 = brFrom.readLine()) != null) {
                arffFrom.add(str1);
            }
            while ((str2 = brTo.readLine()) != null) {
                arffTo.add(str2);
            }
            if (arffFrom.size() != arffTo.size()) {
                throw new IOException("Files do not have the same number of lines of data!");
            }
            brFrom.close();
            brTo.close();

            // Overwrite the old file
            BufferedWriter bw = new BufferedWriter(new FileWriter(filepath + arffFileTo + ".arff", false));
            bw.write(getARFFHeader(arffFileTo));
            for (int i = 0, il = arffFrom.size(); i < il; i++) {
                str1 = arffFrom.get(i);
                str2 = arffTo.get(i);

                String output;
                String[] str1s = str1.split(",");
                String[] str2s = str2.split(",");
                // Only overwrite if there was "NONE" before
                if (str2s[str2s.length - 1].trim().equals("NONE") && !str1s[str1s.length - 1].trim().equals("NONE")) {
                    output = str1;
                } else {
                    output = str2;
                }

                bw.write(output);
                bw.newLine();

//                String[] str1s = str1.split(",");
//                String[] str2s = str2.split(",");
//
//                // Only overwrite if there was "NONE" before
//                if (str2s[str2s.length - 1].trim().equals("NONE")) {
//                    str2s[str2s.length - 1] = str1s[str1s.length - 1];  // overwrite the last token, User
//                }
//
//                bw.write(stringArrayToCSVLine(str2s));
//                bw.newLine();
            }
            bw.close();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String stringArrayToCSVLine(String[] values) {
        String output = values[0];

        for (int i = 1, il = values.length; i < il; i++) {
            output += ", " + values[i].trim();
        }

        return output;
    }

    public static float computeEnergy(float[] samples) {
        float output = 0;
        for (float val : samples) {
            output += val * val;
        }
        return output;
    }

    public static AudioDispatcher processRAWFileForLog(String filename, final Classifier classifier, int SAMPLE_RATE,
                                                       int BUFFER_SIZE, int BUFFER_OVERLAP) {
        final int numCepstralCoeffs = 13;
        final int numMelFilters = 40;
        final float lowerFilterFreq = 64; // Hz
        final float upperFilterFreq = (float) 4000; // Hz - human frequencies

        BufferedWriter bwLog = null;
        try {
            bwLog = new BufferedWriter(new FileWriter(filename + " log.csv", false));
            bwLog.write(AudioProcessingResult.getLogHeaderString());
            bwLog.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Produce a dispatcher
        AudioDispatcher dispatcher = AudioUtils.audioDispatcherFromFile(new File(filename + ".wav"), SAMPLE_RATE,
                BUFFER_SIZE, BUFFER_OVERLAP);

        // The AudioProcessingResult reference object
        final AudioProcessingResult[] apr = new AudioProcessingResult[1];
        VoiceActivityDetector vad = new VoiceActivityDetector(SAMPLE_RATE, BUFFER_SIZE);

        // Basics - User, Time and dbSPL.
        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                apr[0] = new AudioProcessingResult();

                // voice activity detection
                apr[0].setVoiced(vad.isVoiced(audioEvent));

                apr[0].setStartTime((float) audioEvent.getTimeStamp());
                apr[0].setDbSPL(
                        apr[0].isVoiced() ? (float) AudioUtils.computedBSPL(audioEvent.getFloatBuffer()) : 0
                );
                return true;
            }

            @Override
            public void processingFinished() {

            }
        });

        // Pitch Processing
        PitchDetectionHandler handler = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
                apr[0].setPitchDetectionResult(pitchDetectionResult);
            }
        };
        dispatcher.addAudioProcessor(new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm
                .FFT_YIN, SAMPLE_RATE, BUFFER_SIZE, handler));

        // MFCC
        final MFCC mfcc = new MFCC(BUFFER_SIZE, (float) SAMPLE_RATE, numCepstralCoeffs, numMelFilters,
                lowerFilterFreq, upperFilterFreq);
        dispatcher.addAudioProcessor(mfcc);
        final BufferedWriter finalBwLog = bwLog;
        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                apr[0].setMFCC(
                        apr[0].isVoiced() ? mfcc.getMFCC() : new float[13]
                );
                apr[0].setUser(apr[0].runClassifier(classifier));
                return true;
            }

            @Override
            public void processingFinished() {

            }
        });

        // Onset Detection for Speech Rate Estimation
        OnsetHandler onsetHandler = new OnsetHandler() {
            @Override
            public void handleOnset(double time, double salience) {
                if (apr[0].isVoiced())
                    apr[0].addOnset(new Onset(time, salience));
            }
        };
        ComplexOnsetDetector onsetDetector = new ComplexOnsetDetector(BUFFER_SIZE, 0.2, 0.002, -90);
        onsetDetector.setHandler(onsetHandler);
        dispatcher.addAudioProcessor(onsetDetector);

        // Guidance Event Monitor
        final GuidanceEventMonitor guiMon = new GuidanceEventMonitor();
        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                if (guiMon.checkResult(apr[0])) {
                    int[] eventBuffer;
                    if ((eventBuffer = guiMon.getEventsBuffer()) != null) {
                        apr[0].setEventBuffer(eventBuffer);
                    }
                }
                return true;
            }

            @Override
            public void processingFinished() {

            }
        });

        // write out the files, ARFF and Log
        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                try {
//                    AudioUtils.writeAudioProcessingResultToFile(finalBw, finalBwLog, apr[0]);
                    finalBwLog.write(apr[0].getLogString());
                    finalBwLog.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public void processingFinished() {
                try {
                    finalBwLog.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        return dispatcher;
    }

    public static void blockTillThreadsFinish(ArrayList<Thread> threads) {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void mergeFiles(String filepath, String mergedFile, String... filesToMerge) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filepath + mergedFile + ".arff", false));

            bw.write(AudioUtils.getARFFHeader(mergedFile));

            // loop over each file
            for (String file : filesToMerge) {
                BufferedReader br = new BufferedReader(new FileReader(filepath + file + ".arff"));

                AudioUtils.skipToARFFData(br);

                String str;
                while ((str = br.readLine()) != null) {
                    bw.write(str);
                    bw.newLine();
                }

                br.close(); // close this BufferedReader
            }

            // finish off FileIOs
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Computes the regression based differential (applies to both dMFCC and ddMFCC) from a cross time slice of a
     * particular coefficient. E.g., mfcc02 across 5 different time periods.
     */
    public static float computeRegDelta(float[] coeffSlice) {
        int t = coeffSlice.length / 2;

        float num = 0, den = 0; // numerator and denominator
        for (int n = 1; n <= t; n++) {
            num += n * (coeffSlice[t + n] - coeffSlice[t - n]);
            den += n * n;
        }

        return num / (2 * den);
    }

    public static void computeDeltaMFCC(ArrayList<AudioProcessingResult> processingResults) {
        int numMFCC = processingResults.get(0).getMFCC().length - 1; // -1 to not count the 1st coeff
        int N = processingResults.size() / 4;
        int window = N * 2 + 1;

        // slide the window to compute (processingResults.size() - N) delta coefficients
        for (int i = 0, il = processingResults.size() - window + 1; i < il; i++) {
            int currentFrameIdx = i + N;

            // check if delta MFCC exists
            if (processingResults.get(currentFrameIdx).getDMFCC() == null) {
                // loop over the MFCCs, from 1 to 12, excluding 0.
                float[] dMFCC = new float[numMFCC];
                for (int j = 1; j <= numMFCC; j++) {
                    float[] mfccWin = new float[window];

                    // loop over each frame in the window, per MFCC
                    for (int k = 0; k < window; k++) {
                        mfccWin[k] = processingResults.get(i + k).getMFCC()[j];
                    }

                    dMFCC[j - 1] = computeRegDelta(mfccWin);
                }

                // write dMFCC to middle frame
                processingResults.get(currentFrameIdx).setDMFCC(dMFCC);
            }
        }

        // compute the delta-delta MFCC for the middle frame
        // loop over the dMFCCs, from 1 to 12
        float[] ddmfcc = new float[numMFCC];
        for (int i = 0; i < numMFCC; i++) {
            float[] dmfccWin = new float[window];

            // loop over frames to grab the corresponding dmfcc from each frame
            for (int j = N, jl = N + window; j < jl; j++) {
                dmfccWin[j - N] = processingResults.get(j).getDMFCC()[i];
            }
            ddmfcc[i] = computeRegDelta(dmfccWin);
        }

        // write ddMFCC to middle frame
        processingResults.get(window - 1).setDDMFCC(ddmfcc);
    }

}
