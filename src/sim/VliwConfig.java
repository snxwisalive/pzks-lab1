package sim;

import java.util.Map;

public class VliwConfig {
    public final int processors;
    public final int banks;
    public final Map<Character, Integer> duration;

    public VliwConfig(int processors, int banks, Map<Character, Integer> duration) {
        this.processors = processors;
        this.banks = banks;
        this.duration = duration;
    }

    // parameters obtained from Eucalyptus
    public static VliwConfig defaults() {
        return new VliwConfig(6, 2, Map.of('+', 2, '-', 3, '*', 4, '/', 8));
    }
}