package smtchahal.regextester;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class JSONAdapter extends BaseAdapter {

    private static final String LOG_TAG = "JSONAdapter";
    private JSONArray mJsonArray;
    private final Context mContext;
    private final LayoutInflater mInflater;

    public JSONAdapter(Context context, LayoutInflater inflater, JSONArray jsonArray) {
        mContext = context;
        mInflater = inflater;
        mJsonArray = jsonArray;
    }

    @Override
    public int getCount() {
        return mJsonArray.length();
    }

    @Override
    public JSONObject getItem(int position) {
        return mJsonArray.optJSONObject(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        // check if the view already exists
        // if so, no need to inflate and findViewById again!
        if (convertView == null) {

            // Inflate the custom row layout from XML
            convertView = mInflater.inflate(R.layout.row_regex, null);

            // create a new "Holder" with subviews
            holder = new ViewHolder();
            holder.regexKeyTextView = (TextView) convertView.findViewById(R.id.regex_key_textview);
            holder.regexValueTextView = (TextView) convertView.findViewById(R.id.regex_value_textview);

            // hang onto the holder for future recyclage
            convertView.setTag(holder);
        } else {

            // skip all the expensive inflation/findViewById
            // and just get the holder you already made
            holder = (ViewHolder) convertView.getTag();
        }
        JSONObject jsonObject = getItem(position);

        Iterator<String>keys = jsonObject.keys();
        String key = "";
        String value = "";
        try {
            while (keys.hasNext()) {
                key = keys.next();
                value = jsonObject.getString(key);
            }
            holder.regexKeyTextView.setText(key);
            holder.regexValueTextView.setText(value);
        } catch (JSONException e) {
            Toast.makeText(mContext, R.string.json_exception_toast_message, Toast.LENGTH_LONG).show();
            Log.d(LOG_TAG, "JSONException, this shouldn't have happened...");
            Log.d(LOG_TAG, "e.getMessage() = " + e.getMessage());
        }

        return convertView;
    }

    public void updateData(JSONArray jsonArray) {
        // update the adapter's dataset
        mJsonArray = jsonArray;
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        public TextView regexKeyTextView;
        public TextView regexValueTextView;
    }
}
