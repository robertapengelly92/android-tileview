# android-tileview

Clone of https://github.com/moagrius/TileView with added support for pre-honeycomb devices.

Not all features has been tested so any unexpected behaviour check the original and then let me know so I can fix the bug.

Usage:

    Step 1. Add the JitPack repository to your build file
    
    Add it in your root build.gradle at the end of repositories:
    
    allprojects {
		  repositories {
			  ...
			  maven { url 'https://jitpack.io' }
		  }
	}
    
    Step 2. Add the dependency
    
    dependencies {
	        compile 'com.github.robertapengelly:android-tileview:1.0.1'
	}

Basic Implementation:

    Add the import to the top of the file
    
    import robertapengelly.support.tileview.TileView;
    
    Within you your class add
    
        public static final double NORTH_WEST_LATITUDE = 39.9639998777094;
        public static final double NORTH_WEST_LONGITUDE = -75.17261900652977;
        public static final double SOUTH_EAST_LATITUDE = 39.93699709962642;
        public static final double SOUTH_EAST_LONGITUDE = -75.12462846235614;
    
    In your onCreate method
    
        TileView tileview = new TileView(this);
        
        // add the detail levels, optional but recommended
        tileview.addDetailLevel(1f, "tiles/high_season_map/1000/%d_%d.jpg");
        tileview.addDetailLevel(0.5f, "tiles/high_season_map/500/%d_%d.jpg");
        tileview.addDetailLevel(0.25f, "tiles/high_season_map/250/%d_%d.jpg");
        tileview.addDetailLevel(0.125f, "tiles/high_season_map/125/%d_%d.jpg");
        
        // define the bounds of our TileView
        tileview.defineBounds(NORTH_WEST_LONGITUDE, NORTH_WEST_LATITUDE, SOUTH_EAST_LONGITUDE, SOUTH_EAST_LATITUDE);
        
        // markers should align to the coordinate along the horizontal center and vertical bottom
        tileview.setMarkerAnchorPoints(-0.5f, -1.0f);
        
        // start small and allow zoom
        tileview.setScale(0.5f);
        
        // set the limits to scale minimum, maximum
        tileview.setScaleLimits(0, (2 * getResources().getDisplayMetrics().density));
        
        // render the image while we pan
        tileview.setShouldRenderWhilePanning(true);
        
        // set the size to the original untiled image size
        tileview.setSize(1801, 1273);
        
        // add the original image as the first child of the TileView
        // this is optional but if not added then while the tiles render there won't be any images
        ImageView downsample = new ImageView(this);
        downsample.setImageResource(R.drawable.downsample);
        tileview.addView(downsample, 0);
        
        // add markers at specific latitude, longitude
        ImageView marker = new ImageView(this);
        marker.setImageResource(R.drawable.map_marker_normal);
        tileview.addMarker(marker, -75.1489070, 39.9484760, null, null);
        
        setContentView(tileview);
