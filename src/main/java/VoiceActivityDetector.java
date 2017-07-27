import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.util.fft.FloatFFT;

/**
 * Created by KennyChoo on 11/5/17.
 */
public class VoiceActivityDetector {

    // Properties
    private float min_e = Float.MAX_VALUE, e_gain_diff = 6; // 6 dB ~twice the SNR above the min dBSPL
    private float f_primthresh = 185, min_f = Float.MAX_VALUE;
    private float sf_primthresh = 5, min_sf = Float.MAX_VALUE;
    private float freqbin;

    // Initial Observation Window
    private int obsWindow = 100; // ~ 600ms window, we might make this longer

    // Objects
    FloatFFT fft;
    private long silence_ctr = obsWindow;

    // frame label
    private boolean isVoiced = false;

    public VoiceActivityDetector(int SAMPLE_RATE, int BUFFER_SIZE) {
        freqbin = ((float) SAMPLE_RATE) / BUFFER_SIZE;
        fft = new FloatFFT(BUFFER_SIZE);
    }

    public boolean isVoiced(AudioEvent audioEvent) {
        int count = 0;
        isVoiced = false;
        float[] samples = audioEvent.getFloatBuffer();

        float[] freqSamp = getFFT(samples);

        // Get the values of the three features
        float e = (float) AudioUtils.computedBSPL(samples);
        double[] sqpow = AudioUtils.squaredPower(freqSamp);
        float f = getMaxMagFreq(sqpow);
        float sf = getSpectralFlatness(sqpow);

        // initial observation window, Observe
        if (obsWindow-- > 0) {
            if (e < min_e) {
                min_e = e;
            }
            if (f < min_f) {
                min_f = f;
            }
            if (sf < min_sf) {
                min_sf = sf;
            }
            isVoiced = false;
        }
        // post observation for minimums
        else {
            if (f - min_f >= f_primthresh) {
                count++;
            }
            if (sf - min_sf >= sf_primthresh) {
                count++;
            }

            // is voiced
            if (e >= min_e + e_gain_diff && count >= 1) {
                isVoiced = true;
            }
            // update global min_e for silence
            else {
                silence_ctr++;
                min_e = ((silence_ctr * min_e) + e) / (silence_ctr + 1);
            }

//            DecimalFormat df = new DecimalFormat("#.00");
//            System.out.println(df.format(audioEvent.getTimeStamp()) + ", energy: " + df.format(e) + ", min_e: " +
//                    df.format(min_e) + ", thresh_e:" + df.format(thresh_e) + ", f: " + df.format(f) + "," +
//                    " min_f: " + df.format(min_f) + ", sf: " + df.format(sf )+ ", min_sf: " + df.format(min_sf) +
//                    ", voiced: " + (isVoiced ? "TRUE" : "FALSE"));
        }
        return isVoiced;
    }

    private float getSpectralFlatness(double[] sqpow) {
        double gm = 1, am = 0;

        for (double val : sqpow) {
            gm *= val;
            am += val;
        }

        gm = Math.pow(gm, 1.0 / sqpow.length);
        am = am / sqpow.length;

        if (gm == 0)
            return 0;
        double output = 10 * Math.log10(gm / am);
        return (float) (output);
    }

    private float getMaxMagFreq(double[] sqpow) {
        // get max
        int maxidx = 0;
        for (int i = 1, il = sqpow.length; i < il; i++) {
            if (sqpow[i] > sqpow[maxidx]) {
                maxidx = i;
            }
        }
        return maxidx * freqbin;
    }

    private float[] getFFT(float[] samples) {
        float[] dest = new float[samples.length];
        float[] output = new float[samples.length/2];
        System.arraycopy(samples, 0, dest, 0, samples.length);
        fft.realForward(dest);
        System.arraycopy(dest, 0, output, 0, output.length);
        return output;  // only output the front half
    }
}
