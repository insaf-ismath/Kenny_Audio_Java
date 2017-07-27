import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.mfcc.MFCC;
import weka.classifiers.Classifier;
import weka.core.SerializationHelper;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by KennyChoo on 3/5/17.
 */
public class WAVProcessor {
    public static final int SAMPLE_RATE = 8000;
    public static final int BUFFER_SIZE = 160;  // default: 640
    public static final int BUFFER_OVERLAP = 80; // default: 0
    public static final float GAIN = 1.0f;
    public static final int numCepstralCoeffs = 13;
    public static final int numMelFilters = 40;
    public static final float lowerFilterFreq = 64; // Hz
    public static final float upperFilterFreq = (float) 4000; // Hz - human frequencies
    public static final float trainPercentage = 0.9f;

    public static final String filepath = "./bin/";
    public static final String fileUser = "P04-1";


    public static void main(String[] args) {
        int SAMPLE_RATE = 8000, BUFFER_SIZE = 160, BUFFER_OVERLAP = 80;
        int numCepstralCoeffs = 13, numMelFilters = 40; float lowerFilterFreq = 64, upperFilterFreq = 4000;
//
//        String[] pids = {"P01"};
//        processParticipants(filepath, pids, "_" + BUFFER_SIZE + "_" + BUFFER_OVERLAP, SAMPLE_RATE, BUFFER_SIZE,
//                BUFFER_OVERLAP, numCepstralCoeffs, numMelFilters, lowerFilterFreq, upperFilterFreq);

//        processWAVFileForFeatures(filepath + "voice", "test", SAMPLE_RATE, BUFFER_SIZE, BUFFER_OVERLAP,
//                numCepstralCoeffs, numMelFilters, lowerFilterFreq, upperFilterFreq, User.ADULT);

        getExperimentARFFs();
    }

    public static void getExperimentARFFs() {
        int SAMPLE_RATE = 8000, BUFFER_SIZE = 160, BUFFER_OVERLAP = 80;
        int numCepstralCoeffs = 13, numMelFilters = 40; float lowerFilterFreq = 64, upperFilterFreq = 4000;
        String[] pids = {"P01", "P03", "P04", "P06", "P07"};
        processParticipants(filepath, pids, "_" + BUFFER_SIZE + "_" + BUFFER_OVERLAP, SAMPLE_RATE, BUFFER_SIZE,
                BUFFER_OVERLAP, numCepstralCoeffs, numMelFilters, lowerFilterFreq, upperFilterFreq);

//        SAMPLE_RATE = 8000; BUFFER_SIZE = 240; BUFFER_OVERLAP = 80;
//        numCepstralCoeffs = 13; numMelFilters = 40; lowerFilterFreq = 64; upperFilterFreq = 4000;
//        processParticipants(filepath, pids, "_" + BUFFER_SIZE + "_" + BUFFER_OVERLAP, SAMPLE_RATE, BUFFER_SIZE,
//                BUFFER_OVERLAP, numCepstralCoeffs, numMelFilters, lowerFilterFreq, upperFilterFreq);

        // --- Experiments show that 20-30ms frames are the best ---
//        SAMPLE_RATE = 8000; BUFFER_SIZE = 320; BUFFER_OVERLAP = 80;
//        numCepstralCoeffs = 13; numMelFilters = 40; lowerFilterFreq = 64; upperFilterFreq = 4000;
//        processParticipants(filepath, pids, "_" + BUFFER_SIZE + "_" + BUFFER_OVERLAP, SAMPLE_RATE, BUFFER_SIZE,
//                BUFFER_OVERLAP, numCepstralCoeffs, numMelFilters, lowerFilterFreq, upperFilterFreq);
//
//        SAMPLE_RATE = 8000; BUFFER_SIZE = 320; BUFFER_OVERLAP = 160;
//        numCepstralCoeffs = 13; numMelFilters = 40; lowerFilterFreq = 64; upperFilterFreq = 4000;
//        processParticipants(filepath, pids, "_" + BUFFER_SIZE + "_" + BUFFER_OVERLAP, SAMPLE_RATE, BUFFER_SIZE,
//                BUFFER_OVERLAP, numCepstralCoeffs, numMelFilters, lowerFilterFreq, upperFilterFreq);
    }

    public static void processParticipants(String filepath, String[] pids, String append,
        int SAMPLE_RATE, int BUFFER_SIZE, int BUFFER_OVERLAP,
        int numCepstralCoeffs, int numMelFilters, float lowerFilterFreq, float upperFilterFreq) {

        for (String pid : pids) {
            processParticipant(filepath, pid, "_" + BUFFER_SIZE + "_" + BUFFER_OVERLAP, SAMPLE_RATE, BUFFER_SIZE,
                    BUFFER_OVERLAP, numCepstralCoeffs, numMelFilters, lowerFilterFreq, upperFilterFreq);
        }
    }

    public static void processParticipant(String filepath, String pid, String append,
        int SAMPLE_RATE, int BUFFER_SIZE, int BUFFER_OVERLAP,
        int numCepstralCoeffs, int numMelFilters, float lowerFilterFreq, float upperFilterFreq) {
        ArrayList<Thread> threads = new ArrayList<>();

        String fChild = filepath + pid + "-C";
        String fAdult = filepath + pid + "-A";
        String fRaw = filepath + pid + "raw";

        // Process the triad of files
        threads.add(processWAVFileForFeatures(fChild, append, SAMPLE_RATE, BUFFER_SIZE, BUFFER_OVERLAP,
                numCepstralCoeffs, numMelFilters, lowerFilterFreq, upperFilterFreq, User.CHILD));
        threads.add(processWAVFileForFeatures(fAdult, append, SAMPLE_RATE, BUFFER_SIZE, BUFFER_OVERLAP,
                numCepstralCoeffs, numMelFilters, lowerFilterFreq, upperFilterFreq, User.ADULT));
        threads.add(processWAVFileForFeatures(fRaw, append, SAMPLE_RATE, BUFFER_SIZE, BUFFER_OVERLAP,
                numCepstralCoeffs, numMelFilters, lowerFilterFreq, upperFilterFreq));

        // Block threads till finish - ensures we can then combine the files
        AudioUtils.blockTillThreadsFinish(threads);

        // overwrite the "raw" file with the values from "child" and "adult"
        AudioUtils.writeUsersToRaw(fChild + append, fAdult + append, fRaw + append);
    }

    /**
     * Processes a WAV file for features and saves it into an ARFF file
     *
     * @param inputFilename Full path file name without extension
     * @param SAMPLE_RATE Sampling rate of the audio. Default is 8000 Hz.
     * @param BUFFER_SIZE Size of each read of the audio buffering. Default should be 20 ms (for 8000 Hz, this is 160
     *                   samples)
     * @param BUFFER_OVERLAP Overlap in the sliding window of audio buffer reads. Default should be half that of
     *                       BUFFER_SIZE
     * @param numCepstralCoeffs Number of ceptral coefficients to output from the MFCC computation. Default is 13,
     *                          and we discard the 1st value which reflects the audio power.
     * @param numMelFilters Number of mel filters to construct. Default is 40 (as used in CMUSphinx).
     * @param lowerFilterFreq Lower filter frequency
     * @param upperFilterFreq Upper filter frequency
     * @return the Thread object that has been started for processing. This enables blocking functions.
     */
    public static Thread processWAVFileForFeatures(String inputFilename,
        int SAMPLE_RATE, int BUFFER_SIZE, int BUFFER_OVERLAP,
        int numCepstralCoeffs, int numMelFilters, float lowerFilterFreq, float upperFilterFreq) {
        return processWAVFileForFeatures(inputFilename, "", SAMPLE_RATE, BUFFER_SIZE, BUFFER_OVERLAP,
                numCepstralCoeffs, numMelFilters, lowerFilterFreq, upperFilterFreq, User.NONE);
    }

    /**
     * Processes a WAV file for features and saves it into an ARFF file
     *
     * @param inputFilename Full path file name without extension
     * @param SAMPLE_RATE Sampling rate of the audio. Default is 8000 Hz.
     * @param BUFFER_SIZE Size of each read of the audio buffering. Default should be 20 ms (for 8000 Hz, this is 160
     *                   samples)
     * @param BUFFER_OVERLAP Overlap in the sliding window of audio buffer reads. Default should be half that of
     *                       BUFFER_SIZE
     * @param numCepstralCoeffs Number of ceptral coefficients to output from the MFCC computation. Default is 13,
     *                          and we discard the 1st value which reflects the audio power.
     * @param numMelFilters Number of mel filters to construct. Default is 40 (as used in CMUSphinx).
     * @param lowerFilterFreq Lower filter frequency
     * @param upperFilterFreq Upper filter frequency
     * @param user User to label voiced segments
     * @return the Thread object that has been started for processing. This enables blocking functions.
     */
    public static Thread processWAVFileForFeatures(String inputFilename,
        int SAMPLE_RATE, int BUFFER_SIZE, int BUFFER_OVERLAP,
        int numCepstralCoeffs, int numMelFilters, float lowerFilterFreq, float upperFilterFreq,
        User user) {
        return processWAVFileForFeatures(inputFilename, "", SAMPLE_RATE, BUFFER_SIZE, BUFFER_OVERLAP,
                numCepstralCoeffs, numMelFilters, lowerFilterFreq, upperFilterFreq, user);
    }

    /**
     * Processes a WAV file for features and saves it into an ARFF file
     *
     * @param inputFilename Full path file name without extension
     * @param append String to append to filename to indicate different runs for experiments
     * @param SAMPLE_RATE Sampling rate of the audio. Default is 8000 Hz.
     * @param BUFFER_SIZE Size of each read of the audio buffering. Default should be 20 ms (for 8000 Hz, this is 160
     *                   samples)
     * @param BUFFER_OVERLAP Overlap in the sliding window of audio buffer reads. Default should be half that of
     *                       BUFFER_SIZE
     * @param numCepstralCoeffs Number of ceptral coefficients to output from the MFCC computation. Default is 13,
     *                          and we discard the 1st value which reflects the audio power.
     * @param numMelFilters Number of mel filters to construct. Default is 40 (as used in CMUSphinx).
     * @param lowerFilterFreq Lower filter frequency
     * @param upperFilterFreq Upper filter frequency
     * @return the Thread object that has been started for processing. This enables blocking functions.
     */
    public static Thread processWAVFileForFeatures(String inputFilename, String append,
        int SAMPLE_RATE, int BUFFER_SIZE, int BUFFER_OVERLAP,
        int numCepstralCoeffs, int numMelFilters, float lowerFilterFreq, float upperFilterFreq) {
        return processWAVFileForFeatures(inputFilename, append, SAMPLE_RATE, BUFFER_SIZE, BUFFER_OVERLAP,
                numCepstralCoeffs, numMelFilters, lowerFilterFreq, upperFilterFreq, User.NONE);
    }



    /**
     * Processes a WAV file for features and saves it into an ARFF file
     *
     * @param inputFilename Full path file name without extension
     * @param append String to append to filename to indicate different runs for experiments
     * @param SAMPLE_RATE Sampling rate of the audio. Default is 8000 Hz.
     * @param BUFFER_SIZE Size of each read of the audio buffering. Default should be 20 ms (for 8000 Hz, this is 160
     *                   samples)
     * @param BUFFER_OVERLAP Overlap in the sliding window of audio buffer reads. Default should be half that of
     *                       BUFFER_SIZE
     * @param numCepstralCoeffs Number of ceptral coefficients to output from the MFCC computation. Default is 13,
     *                          and we discard the 1st value which reflects the audio power.
     * @param numMelFilters Number of mel filters to construct. Default is 40 (as used in CMUSphinx).
     * @param lowerFilterFreq Lower filter frequency
     * @param upperFilterFreq Upper filter frequency
     * @param user User to label voiced segments
     * @return the Thread object that has been started for processing. This enables blocking functions.
     */
    public static Thread processWAVFileForFeatures(String inputFilename, String append,
            int SAMPLE_RATE, int BUFFER_SIZE, int BUFFER_OVERLAP,
            int numCepstralCoeffs, int numMelFilters, float lowerFilterFreq, float upperFilterFreq, User user) {
        BufferedWriter bw = null;
        Thread thread;

        // default: 4. 4 previous and 4 look ahead buffers - this caters for N = 2, for delta, delta-delta MFCCs
        int N = 2;
        int fBuff = N * 2;
        int fBuffMax = fBuff * 2 + 1;

        try {
            bw = new BufferedWriter(new FileWriter(inputFilename + append + ".arff", false));
            bw.write(AudioUtils.getARFFHeader(inputFilename)); // Write the ARFF header
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // Create the audio dispatcher
        AudioDispatcher dispatcher = AudioUtils.audioDispatcherFromFile(new File(inputFilename + ".wav"), SAMPLE_RATE,
                BUFFER_SIZE, BUFFER_OVERLAP);

        // Initialise the audio processing result object buffer
//        final AudioProcessingResult[] apr = new AudioProcessingResult[fBuff * 2 + 1];

        final ArrayList<AudioProcessingResult> processingResults = new ArrayList<>();

        // Initialise Voice Activity Detector
        VoiceActivityDetector vad = new VoiceActivityDetector(SAMPLE_RATE, BUFFER_SIZE);

        // Init audio processor objects
        BufferedWriter finalBw = bw;    // file writer
        final MFCC mfcc = new MFCC(BUFFER_SIZE, (float) SAMPLE_RATE, numCepstralCoeffs, numMelFilters,
                lowerFilterFreq, upperFilterFreq);// MFCC

        //--- Start adding audio processors ---
        dispatcher.addAudioProcessor(mfcc);
        dispatcher.addAudioProcessor(new AudioProcessor() {
            int ctr = 0;
            @Override
            public boolean process(AudioEvent audioEvent) {
                AudioProcessingResult apr = new AudioProcessingResult();

                // Check if voiced
                apr.setVoiced(vad.isVoiced(audioEvent));

                // Set the features
                apr.setStartTime((float) audioEvent.getTimeStamp());
                apr.setMFCC(apr.isVoiced() ? mfcc.getMFCC() : new float[13]);
                apr.setUser(apr.isVoiced() ? user : User.NONE);

                // Deal with START case - buffer the "previous" with copies of the same
                if (processingResults.isEmpty()) {
                    for (int i = 0, il = fBuff; i <= il; i++) {
                        processingResults.add(apr);
                    }
                }
                // Deal with NORMAL cases
                else {
                    processingResults.add(apr);

                    // Buffer overflow, remove first buffer
                    if (processingResults.size() > fBuffMax) {
                        processingResults.remove(0);    // slide the window one step to the right
                    }

                    // Buffer is full, process for delta, delta-delta MFCCs
                    if (processingResults.size() == fBuffMax) {
                        AudioUtils.computeDeltaMFCC(processingResults);
                        AudioProcessingResult middleFrame = processingResults.get(fBuff);

                        // Write out the buffer
                        try {
                            // Only start writing when the buffer is full - grab the middle array
                            finalBw.write(middleFrame.getFeatureVector());
                            finalBw.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return true;
            }

            @Override
            public void processingFinished() {
                // TODO to be tested
                AudioProcessingResult lastRes = processingResults.get(processingResults.size() - 1);

                for (int i = 0; i < fBuff; i++) {
                    processingResults.add(lastRes);
                    processingResults.remove(0);

                    AudioUtils.computeDeltaMFCC(processingResults);

                    AudioProcessingResult middleFrame = processingResults.get(fBuff);

                    // Write out the buffer
                    try {
                        // Only start writing when the buffer is full - grab the middle array
                        finalBw.write(middleFrame.getFeatureVector());
                        finalBw.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


                // Close the file writer
                try {
                    finalBw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // Initiate the thread and return it for blocking
        thread = new Thread(dispatcher);
        thread.start();
        return thread;
    }

    /**
     * Computes the features, runs the classifier to identify users and output reminder events
     *
     * @param filepath
     * @param fileName
     * @return
     */
    public static Thread processWAVRAWFile(String filepath, String fileName) {
        String filepathname = filepath + fileName;
        // User classifier
        try {
            Classifier classifier = (Classifier) SerializationHelper.read(filepathname + ".model");
            AudioDispatcher dispatcher = AudioUtils.processRAWFileForLog(filepathname, classifier, SAMPLE_RATE, BUFFER_SIZE,
                    BUFFER_OVERLAP);
            Thread thread = new Thread(dispatcher);
            thread.start();
            return thread;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



}
