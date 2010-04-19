
package novoda.wallpaper.flickr;

public class Constants {

    /*
     * Options for choosing to span your wallpaper across the dashboards or to
     * have the downloaded images displayed in a nice frame.
     */
    public class Prefs {
        public static final String NAME = "flickrSettings";

        public static final String DISPLAY = "flickr_scale";

        public static final String TAP_TYPE = "flickr_action";

        public static final String DISPLAY_FRAME = "middle";

        public static final String TAP_TYPE_REFRESH = "refeshOnClick";

        public static final String TAP_TYPE_VISIT = "vistOnClick";
    }

    /*
     * When displaying downloaded image within a frame the frame, the frame
     * graphic dictates certain specifications.
     * 
     */
    public class Frame {
        public static final int LANDSCAPE_MARGIN_LEFT = 24;

        public static final int LANDSCAPE_MARGIN_TOP = 110;

        public static final int LANDSCAPE_IMG_MARGIN_LEFT = 69;

        public static final int LANDSCAPE_IMG_MARGIN_TOP = 154;

        public static final int PORTRAIT_IMG_MARGIN_TOP = 118;

        public static final int PORTRAIT_IMG_MARGIN_LEFT = 97;

        public static final int PORTRAIT_MARGIN_TOP = 70;

        public static final int PORTRAIT_MARGIN_LEFT = 47;
    }

}
