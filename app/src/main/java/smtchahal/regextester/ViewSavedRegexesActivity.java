package smtchahal.regextester;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ViewSavedRegexesActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final String LOG_TAG = "ViewSavedRegexes";
    private ListView regexesListView;
    private JSONAdapter mJSONAdapter;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;
    private CoordinatorLayout mainClayout;
    private String initialJsonArrayString = "[]";
    private static final String PACKAGE_NAME = "sumit.regextester";
    private static final String PREF_NAME = PACKAGE_NAME;
    private static final String PREFS_STRING_JSON_SAVED_REGEXES = PACKAGE_NAME + ".JsonSavedRegexes";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_saved_regexes);

        prefs = this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefsEditor = prefs.edit();
        regexesListView = (ListView) findViewById(R.id.regexes_listview);
        mainClayout = (CoordinatorLayout) findViewById(R.id.activity_view_regexes_clayout);

        try {
            InputStream is = getAssets().open("regex_values_preset.json");
            StringBuilder buf = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String str;

            while ((str = in.readLine()) != null) {
                str += "\n";
                buf.append(str);
            }
            in.close();

            initialJsonArrayString = buf.toString();
            Log.d(LOG_TAG, "initialJsonArrayString = " + initialJsonArrayString);

            JSONArray jsonArray = new JSONArray(prefs.getString(PREFS_STRING_JSON_SAVED_REGEXES, initialJsonArrayString));
            mJSONAdapter = new JSONAdapter(this, getLayoutInflater(), jsonArray);
            regexesListView.setAdapter(mJSONAdapter);
            regexesListView.setOnItemClickListener(this);
            regexesListView.setOnItemLongClickListener(this);

            TextView emptyTextView = (TextView) findViewById(R.id.regexes_listview_empty);
            regexesListView.setEmptyView(emptyTextView);
        } catch (JSONException | IOException e) {
            Toast.makeText(this, R.string.json_exception_toast_message, Toast.LENGTH_LONG).show();
            Log.d(LOG_TAG, e.getClass().toString() + ", this shouldn't have happened...");
            Log.d(LOG_TAG, "e.getMessage() = " + e.getMessage());
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        useSavedRegex(view);
    }

    private void useSavedRegex(View view) {
        String patternText = ((TextView) view.findViewById(R.id.regex_value_textview)).getText().toString();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(PACKAGE_NAME + ".SelectedPatternText", patternText);
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        TextView keyTextView = (TextView) view.findViewById(R.id.regex_key_textview);
        builder.setTitle(keyTextView.getText().toString());
        builder.setItems(R.array.saved_regex_actions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // 1st item = "Use"
                        useSavedRegex(view);
                        break;

                    case 1: // 2nd item = "Share"
                        shareSavedRegex(view);
                        break;

                    case 2: // 3rd item = "Modify"
                        modifySavedRegex(view);
                        break;

                    case 3: // 4th item = "Delete"
                        deleteSavedRegex(view);
                        break;
                }
            }
        });
        builder.show();
        return true;
    }

    private void deleteSavedRegex(View view) {
        TextView keyTextView = (TextView) view.findViewById(R.id.regex_key_textview);

        final String key = keyTextView.getText().toString();

        // Create an AlertDialog, asking for confirmation
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get literal string resources
        String alertTitleLiteral = getResources().getString(R.string.delete_regex_title);
        String alertMessageLiteral = getResources().getString(R.string.delete_regex_message);

        // Format the strings
        String alertTitle = String.format(alertTitleLiteral, key);
        String alertMessage = String.format(alertMessageLiteral, key);

        // Set title and message
        builder.setTitle(alertTitle);
        builder.setMessage(alertMessage);

        // Create an "OK" button
        builder.setPositiveButton(R.string.delete_regex_button_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    // Retrieve jsonArray encoded settings
                    MyJSONArray jsonArray = new MyJSONArray(prefs.getString(PREFS_STRING_JSON_SAVED_REGEXES,
                            initialJsonArrayString));
                    Log.d(LOG_TAG, "Initially, jsonArray = " + jsonArray.toString());

                    // Delete the item
                    MyJSONArray jsonArrayNew = jsonArray.remove(jsonArray.getIndexOfKey(key));

                    // Save settings
                    prefsEditor.putString(PREFS_STRING_JSON_SAVED_REGEXES, jsonArrayNew.toString());
                    prefsEditor.commit();

                    // Update the ListView
                    mJSONAdapter.updateData(jsonArrayNew);

                    Snackbar.make(mainClayout, R.string.regex_deleted_success, Snackbar.LENGTH_SHORT)
                            .setAction(R.string.snackbar_button_dismiss, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) { }
                            })
                            .show();
                } catch (JSONException e) {
                    Toast.makeText(ViewSavedRegexesActivity.this, R.string.json_exception_toast_message, Toast.LENGTH_LONG).show();
                    Log.d(LOG_TAG, "JSONException, this shouldn't have happened...");
                    Log.d(LOG_TAG, "e.getMessage() = " + e.getMessage());
                }
            }
        });
        builder.setNegativeButton(R.string.delete_regex_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        // ALWAYS remember to call .show()
        builder.show();
    }

    private void modifySavedRegex(View view) {
        TextView keyTextView = (TextView) view.findViewById(R.id.regex_key_textview);
        TextView valueTextView = (TextView) view.findViewById(R.id.regex_value_textview);
        final String key = keyTextView.getText().toString();
        final String value = valueTextView.getText().toString();

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get literal string from resources
        String alertTitleLiteral = getResources().getString(R.string.modify_regex_title);

        // Format it with "key"
        String alertTitle = String.format(alertTitleLiteral, key);

        // Set alert title
        builder.setTitle(alertTitle);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.modify_confirm_dialog, null);

        // Get first EditText (containing key or name of regex)
        final AutoCompleteTextView keyEditText = (AutoCompleteTextView) dialogView.findViewById(R.id.modify_regex_key_edittext);

        try {
            // Get the JSON-encoded preferences
            MyJSONArray jsonArray = new MyJSONArray(prefs.getString(PREFS_STRING_JSON_SAVED_REGEXES,
                    initialJsonArrayString));
            String[] keys = jsonArray.getKeys();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                    keys);
            keyEditText.setAdapter(adapter);
        } catch (JSONException e) {
            Log.d(LOG_TAG, "JSONException, this shouldn't have happened...");
            Toast.makeText(this, R.string.json_exception_toast_message, Toast.LENGTH_LONG);
        }

        // Get second EditText (containing value or the regex itself)
        final EditText valueEditText = (EditText) dialogView.findViewById(R.id.modify_regex_value_edittext);

        // Set the EditText's to the values we got from
        // the two TextView's of the selected View from the ListView
        keyEditText.setText(key);
        valueEditText.setText(value);

        // Highlight the key EditText
        keyEditText.selectAll();
        keyEditText.requestFocus();

        // Set view for the builder
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.generic_button_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    // Get new key and value pairs
                    final String newKey = keyEditText.getText().toString();
                    final String newValue = valueEditText.getText().toString();

                    // If the key entered was empty,
                    // display a toast with error message
                    // and return
                    if (newKey.length() == 0) {
                        Snackbar.make(mainClayout, R.string.key_cannot_be_empty, Snackbar.LENGTH_LONG)
                                .setAction(R.string.snackbar_button_dismiss, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {}
                                })
                                .show();
                        return;
                    }

                    // Get jsonArray encoded settings
                    final MyJSONArray jsonArray = new MyJSONArray(prefs.getString(PREFS_STRING_JSON_SAVED_REGEXES,
                            initialJsonArrayString));

                    // Get the position of the key to modify
                    final int keyIndex = jsonArray.getIndexOfKey(key);
                    final int newKeyIndex = jsonArray.getIndexOfKey(newKey);

                    // Key already exists
                    if (newKeyIndex >= 0) {
                        // Create a new AlertDialog.Builder
                        AlertDialog.Builder builder2 = new AlertDialog.Builder(ViewSavedRegexesActivity.this);

                        // Get resource literals
                        String titleLiteral = getResources().getString(R.string.modify_regex_replace_confirm_title);
                        String messageLiteral = getResources().getString(R.string.modify_regex_replace_confirm_message);

                        String oldValue = jsonArray.getJSONObject(newKeyIndex).getString(newKey);

                        // Format these literals
                        String title = String.format(titleLiteral, newKey);
                        SpannableStringBuilder message = new SpannableStringBuilder(String.format(messageLiteral,
                                newKey, oldValue));

                        int start = message.toString().length() - oldValue.length();
                        int end = message.toString().length();
                        message.setSpan(new TypefaceSpan("monospace"),
                                start,
                                end,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        builder2.setTitle(title);
                        builder2.setMessage(message);
                        builder2.setPositiveButton(R.string.modify_regex_replace_confirm_button_ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Remove the JSONObject at the positions
                                        MyJSONArray jsonArrayNew = jsonArray.remove(keyIndex);

                                        jsonArrayNew = jsonArrayNew.remove(newKeyIndex);

                                        // Put new key-value pairs
                                        JSONObject jsonObject = new JSONObject();
                                        try {
                                            jsonObject.put(newKey, newValue);
                                        } catch (JSONException e) {
                                            Log.d(LOG_TAG, "JSONException, this shouldn't have happened...");
                                            Toast.makeText(ViewSavedRegexesActivity.this,
                                                    R.string.json_exception_toast_message,
                                                    Toast.LENGTH_LONG)
                                                    .show();
                                        }
                                        jsonArrayNew.put(jsonObject);

                                        // Save settings
                                        prefsEditor.putString(PREFS_STRING_JSON_SAVED_REGEXES, jsonArrayNew.toString());
                                        prefsEditor.commit();

                                        // Update the ListView
                                        mJSONAdapter.updateData(jsonArrayNew);

                                        // Display a snackbar
                                        Snackbar.make(mainClayout, R.string.regex_saved_success_message, Snackbar.LENGTH_SHORT)
                                                .setAction(R.string.snackbar_button_dismiss, new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                    }
                                                })
                                                .show();
                                    }
                                });
                        builder2.setNegativeButton(R.string.modify_regex_replace_confirm_button_cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {}
                                });
                        builder2.show();
                    } else {

                        // Remove the JSONObject at the positions
                        MyJSONArray jsonArrayNew = jsonArray.remove(keyIndex);

                        // Second removal
                        jsonArrayNew = jsonArrayNew.remove(newKeyIndex);

                        // Put new key-value pairs
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(newKey, newValue);
                        jsonArrayNew.put(jsonObject);

                        // Save settings
                        Log.d(LOG_TAG, "Saving preference, jsonArrayNew = " + jsonArrayNew.toString());
                        prefsEditor.putString(PREFS_STRING_JSON_SAVED_REGEXES, jsonArrayNew.toString());
                        prefsEditor.commit();

                        // Update the ListView
                        mJSONAdapter.updateData(jsonArrayNew);

                        // Display a snackbar
                        Snackbar.make(mainClayout, R.string.regex_saved_success_message, Snackbar.LENGTH_SHORT)
                                .setAction(R.string.snackbar_button_dismiss, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                    }
                                })
                                .show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(ViewSavedRegexesActivity.this, R.string.json_exception_toast_message, Toast.LENGTH_LONG).show();
                    Log.d(LOG_TAG, "JSONException, this shouldn't have happened...");
                    Log.d(LOG_TAG, "e.getMessage() = " + e.getMessage());
                }

            }
        });
        builder.setNegativeButton(R.string.generic_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        builder.show();
    }

    private void shareSavedRegex(View view) {
        // Get the key-value TextView's
        TextView keyTextView = (TextView) view.findViewById(R.id.regex_key_textview);
        TextView valueTextView = (TextView) view.findViewById(R.id.regex_value_textview);

        // Create new shareIntent
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);

        // Get literal string resource
        String subjectLiteral = getResources().getString(R.string.share_extra_subject);

        // Format it and use it
        String subject = String.format(subjectLiteral, keyTextView.getText().toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        shareIntent.putExtra(Intent.EXTRA_TEXT, valueTextView.getText().toString());
        shareIntent.setType("text/plain");

        // Enclose intent in .createChooser so that all the sharing
        // apps are shown every time the user selects this,
        // even if the user asked to always use a particular app
        startActivity(Intent.createChooser(shareIntent, "Share regex"));
    }
}
