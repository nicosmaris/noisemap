package xifopen.noisemap.client.computer.data;

import com.softsynth.jsyn.*;
import com.softsynth.math.AudioMath;
import java.math.BigDecimal;
import java.util.Calendar;
import xifopen.noisemap.client.computer.UI.LocatorAndNoiseMeterThread;

/**
 * Alternatives to Jsyn that didn't work: JavaSound(random bug), ASIO, JMF, Jass, jRtAudio, FMJ
 * Jsyn recommended at http://stackoverflow.com/questions/4403804/sound-related-libraries
 * and at book: Physical computing: sensing and controlling the physical world with computers
 * TODO: check why it sometimes gives 0.0 dB
 * @author nickmeet
 */
public class VUmeter {
    public void test() {
        VUmeter meter = new VUmeter();
        for (int i = 0; i < 3; i++){
            System.out.println(meter.getDB(null));
        }
    }
    private int n = 0;                          // number of elements
    private double m_n = 0.0;                   // last avg
    private void movingAVG(double x){
        n++;
        m_n += (x-m_n)/n;
    }
    public double getDB(LocatorAndNoiseMeterThread t){       // The typical VU scale is from −20 to +3
        Synth.startEngine(Synth.FLAG_ENABLE_INPUT); // flag necessary for lineIn
        PeakFollower pd = new PeakFollower();
        PeakFollower follower = new PeakFollower();
        pd.start();
        LineIn lineIn = new LineIn();
        lineIn.start();
        lineIn.output.connect(0, pd.input, 0);  // connects the signal you want to analyze to the right detector
        try {   // we find the peak within 100msec and keep the avg over 2sec
            for (int i = 0; i < 20; i++) {      // polls the detector 10 times a second for 2 seconds
                double peak = pd.output.get();
                if(peak!=0)                     // it may happen in the beginning when there is not enough output
                    movingAVG(AudioMath.amplitudeToDecibels(peak));
                if(t!=null) t.progressed(50/20);
                Thread.sleep(100);
            }
            Synth.stopEngine();
        } catch (SynthException e) {
            throw new Issue("Jsyn engine cannot be stopped.");
        } catch (InterruptedException e) {
            throw new Issue("Interrupted while waiting for "+
                    "next noise intensity measurement. Multiple measurements take place for convenience.");
        }
        BigDecimal bd = new BigDecimal(this.m_n);
        BigDecimal rounded = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.n = 0;
        this.m_n = 0.0;
        return rounded.doubleValue();
    }
    /**
     * Instead of polluting the java code with methods that throw exceptions,
     * a class can throw runtime exceptions as an inner class
     * Writing description of erroneous cases can describe indirectly the
     * functionality of the code in normal execution
     */
    public class Issue extends RuntimeException{
        private String mistake;
        public Issue(String say_what_happened){
            super(say_what_happened);
            String precondition = "Please check that your microphone is not mute "+
                    "and that you have not plugged in your headphones.\n";
            mistake = precondition + say_what_happened;
        }
        public String get(){
            return mistake;
        }
    }
}
