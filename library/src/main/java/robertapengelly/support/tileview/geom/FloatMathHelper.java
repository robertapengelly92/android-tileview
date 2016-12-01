package robertapengelly.support.tileview.geom;

public class FloatMathHelper {

    public static int scale(int base, float multiplier) {
        return (int) ((base * multiplier) + 0.5f);
    }
    
    public static int unscale(int base, float multiplier) {
        return (int) ((base / multiplier) + 0.5f);
    }

}