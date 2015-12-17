package smtchahal.regextester;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.StringRes;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

public class AnchoredToast extends Toast {

    /**
     * Construct an empty Toast object.  You must call {@link #setView} before you
     * can call {@link #show}.
     *
     * @param context The context to use.  Usually your {@link Application}
     *                or {@link Activity} object.
     */
    public AnchoredToast(Context context) {
        super(context);
    }

    /**
     * Construct an anchored toast.
     * @param v View to which the toast will be anchored
     * @param context Context in which the toast should appear
     * @param resId ID to the string resource to display
     * @param duration Duration for toast, either of {@link #LENGTH_LONG}
     *                 or {@link #LENGTH_SHORT}
     * @return The resultant toast
     */
    public static Toast makeText(View v, Context context, @StringRes int resId, int duration) {
        int x = v.getLeft();
        int y = v.getTop();
        Toast toast = Toast.makeText(context, resId, duration);
        toast.setGravity(Gravity.TOP | Gravity.START, x, y);
        return toast;
    }

    /**
     * Construct an anchored toast.
     * @param v View to which the toast will be anchored
     * @param context Context in which the toast should appear
     * @param text String to display
     * @param duration Duration for toast, either of {@link #LENGTH_LONG}
     *                 or {@link #LENGTH_SHORT}
     * @return The resultant toast
     */
    public static Toast makeText(View v, Context context, CharSequence text, int duration) {
        int x = v.getLeft();
        int y = v.getTop();
        Toast toast = Toast.makeText(context, text, duration);
        toast.setGravity(Gravity.TOP | Gravity.START, x, y);
        return toast;
    }
}
