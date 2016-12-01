package robertapengelly.support.tileview.graphics;

import  android.content.Context;
import  android.content.res.AssetManager;
import  android.graphics.Bitmap;
import  android.graphics.BitmapFactory;

import  java.io.InputStream;
import  java.util.Locale;

import  robertapengelly.support.tileview.tile.Tile;

/**
 * This is a very simple implementation of BitmapProvider, using a formatted string to find
 * an asset by filename, and built-in methods to decode the bitmap data.
 *
 * Feel free to use your own implementation here, where you might implement a favorite library like
 * Picasso, or add your own disk-caching scheme, etc.
 */
public class BitmapProviderAssets implements BitmapProvider {

    private static final BitmapFactory.Options OPTIONS = new BitmapFactory.Options();
    
    static { OPTIONS.inPreferredConfig = Bitmap.Config.RGB_565; }
    
    @Override
    public Bitmap getBitmap(Tile tile, Context context) {
    
        Object data = tile.getData();
        
        if (!(data instanceof String))
            return null;
        
        String unformattedFileName = (String) data;
        String formattedFileName = String.format(Locale.US, unformattedFileName, tile.getColumn(), tile.getRow());
        
        AssetManager manager = context.getAssets();
        
        try {
        
            InputStream input = manager.open(formattedFileName);
            
            if (input != null)
                return BitmapFactory.decodeStream(input, null, OPTIONS);
        
        } catch (Exception ex) {
            // this is probably an IOException, meaning the file can't be found
        }
        
        return null;
    
    }

}