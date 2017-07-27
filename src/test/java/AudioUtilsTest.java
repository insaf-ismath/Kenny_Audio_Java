import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by KennyChoo on 31/5/17.
 */
public class AudioUtilsTest {
    @Test
    public void computeDeltaMFCC() throws Exception {
        ArrayList<AudioProcessingResult> processingResults = new ArrayList<>();

        AudioProcessingResult a1 = new AudioProcessingResult();
        AudioProcessingResult a2 = new AudioProcessingResult();
        AudioProcessingResult a3 = new AudioProcessingResult();
        AudioProcessingResult a4 = new AudioProcessingResult();
        AudioProcessingResult a5 = new AudioProcessingResult();
        AudioProcessingResult a6 = new AudioProcessingResult();
        AudioProcessingResult a7 = new AudioProcessingResult();
        AudioProcessingResult a8 = new AudioProcessingResult();
        AudioProcessingResult a9 = new AudioProcessingResult();
        float[] m1 = {100, 1,  2};
        float[] m2 = {100, 7,  8};
        float[] m3 = {100, 10, 5};
        float[] m4 = {100, 4,  5};
        float[] m5 = {100, 13, 7};
        float[] m6 = {100, 10, 11};
        float[] m7 = {100, 4,  5};
        float[] m8 = {100, 13, 14};
        float[] m9 = {100, 13, 14};
        a1.setMFCC(m1);
        a2.setMFCC(m2);
        a3.setMFCC(m3);
        a4.setMFCC(m4);
        a5.setMFCC(m5);
        a6.setMFCC(m6);
        a7.setMFCC(m7);
        a8.setMFCC(m8);
        a9.setMFCC(m9);
        processingResults.add(a1);
        processingResults.add(a2);
        processingResults.add(a3);
        processingResults.add(a4);
        processingResults.add(a5);
        processingResults.add(a6);
        processingResults.add(a7);
        processingResults.add(a8);
        processingResults.add(a9);

        // process
        AudioUtils.computeDeltaMFCC(processingResults);

        // generate expected
        float[] iamnull = null;
        float[] f2 = {2.1f, 0.7f};
        float[] f3 = {0.9f, 0.8f};
        float[] f4 = {-0.6f, 0.6f};
        float[] f5 = {0.9f, 1.6f};
        float[] f6 = {0.3f, 1.7f};
        ArrayList<float[]> e = new ArrayList<>();
        e.add(iamnull);
        e.add(iamnull);
        e.add(f2);
        e.add(f3);
        e.add(f4);
        e.add(f5);
        e.add(f6);
        e.add(iamnull);
        e.add(iamnull);

        float[] ddmfccExpected = {-0.36f, 0.28f};

        for (int i = 0; i < 9; i++) {
            assertArrayEquals(e.get(i), processingResults.get(i).getDMFCC(), 0.01f);
        }
        assertArrayEquals(ddmfccExpected, processingResults.get(4).getDDMFCC(), 0.01f);
    }

    @Test
    public void computeRegDelta() throws Exception {
        float[] input = {1, 2, 3, 4, 5};
        float actual, expected = 1;

        actual = AudioUtils.computeRegDelta(input);

        assertEquals(expected, actual, 0);
    }

}