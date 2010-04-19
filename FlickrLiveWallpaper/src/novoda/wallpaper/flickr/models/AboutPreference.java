
package novoda.wallpaper.flickr.models;

import novoda.wallpaper.flickr.R;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.DialogPreference;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AboutPreference extends DialogPreference {

    @Override
    protected View onCreateDialogView() {
        final LinearLayout layout = new LinearLayout(getContext());
        layout.setPadding(15, 5, 10, 5);
        layout.setOrientation(LinearLayout.VERTICAL);
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.LEFT;

        ImageView img = new ImageView(getContext());
        img.setImageResource(R.drawable.ic_novoda);
        img.setClickable(true);
        
        TextView txt = new TextView(getContext());
        txt.setLayoutParams(params);
        txt.setText(R.string.flickr_settings_about_txt);
        txt.setTextColor(Color.WHITE);
        txt.setTextSize(12);
        txt.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        
        TextView linkTxt = new TextView(getContext());
        linkTxt.setLayoutParams(params);
        linkTxt.setAutoLinkMask(Linkify.ALL);
        linkTxt.setLinksClickable(true);
        linkTxt.setClickable(true);
        linkTxt.setTextSize(14);
        linkTxt.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        linkTxt.setText(R.string.flickr_settings_about_code_link);
        
        TextView picRef = new TextView(getContext());
        picRef.setLayoutParams(params);
        picRef.setText(R.string.flickr_settings_photographer_credit);
        picRef.setTextColor(Color.WHITE);
        picRef.setTextSize(12);
        picRef.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        
        layout.addView(img);
        layout.addView(txt);
        layout.addView(linkTxt);
        layout.addView(picRef);
        
        return layout;        
    }

    public AboutPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AboutPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

}
