package smtchahal.regextester;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private static final String PACKAGE_NAME = "sumit.regextester";
    private static final String LOG_TAG = "MainActivity";
    private static final String PREF_NAME = PACKAGE_NAME;
    private static final String PREFS_STRING_SUBJECT = PACKAGE_NAME + ".Subject";
    private static final String PREFS_STRING_PATTERN = PACKAGE_NAME + ".Pattern";
    private static final String PREFS_STRING_JSON_SAVED_REGEXES = PACKAGE_NAME + ".JsonSavedRegexes";
    private static final String PREFS_BOOL_CASE_INSENSITIVE = PACKAGE_NAME + ".CaseInsensitive";
    private static final String PREFS_BOOL_UNIX = PACKAGE_NAME + ".Unix";
    private static final String PREFS_BOOL_COMMENTS = PACKAGE_NAME + ".Comments";
    private static final String PREFS_BOOL_DOT_ALL = PACKAGE_NAME + ".DotAll";
    private static final String PREFS_BOOL_LITERAL = PACKAGE_NAME + ".Literal";
    private static final String PREFS_BOOL_MULTILINE = PACKAGE_NAME + ".Multline";
    private static final String PREFS_INT_MATCH = PACKAGE_NAME + ".Match";
    private static final String PREFS_BOOL_PERM_DENIED = PACKAGE_NAME + ".PermissionDenied";
    private String initialJsonArrayString = "[]";
    private FloatingActionButton findButton;
    private FloatingActionButton saveButton;
    private EditText patternEditText;
    private EditText subjectEditText;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;
    private CoordinatorLayout mainClayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        subjectEditText = (EditText) findViewById(R.id.subject_edittext);
        patternEditText = (EditText) findViewById(R.id.pattern_edittext);
        findButton = (FloatingActionButton) findViewById(R.id.find_button);
        saveButton = (FloatingActionButton) findViewById(R.id.save_button);
        mainClayout = (CoordinatorLayout) findViewById(R.id.activity_main_clayout);
        prefs = this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefsEditor = prefs.edit();

        findButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);
        findButton.setOnLongClickListener(this);
        saveButton.setOnLongClickListener(this);

        // Reset this value each time onCreate() is called
        prefsEditor.putInt(PREFS_INT_MATCH, -1);
        prefsEditor.commit();

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
        } catch (IOException e) {
            Log.d(LOG_TAG, "IOException, e.getMessage() = " + e.getMessage());
            String messageLiteral = getResources().getString(R.string.io_exception_toast_message);
            String message = String.format(messageLiteral, e.getMessage());
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        restoreValues();

        // After restoring saved values, check if intent was received
        // containing required selectedPatternText from ViewSavedRegexesActivity
        Intent intent = getIntent(); //notnull
        Bundle bundle = intent.getExtras();

        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                if (value != null)
                    Log.d(LOG_TAG, key + " = " + value.toString() + "; " + value.getClass().getName());
            }
        }

        // Handle explicit intent from ViewSavedRegexesActivity
        //noinspection ConstantConditions
        String selectedPatternValue;
        if ((selectedPatternValue = intent.getStringExtra(PACKAGE_NAME + ".SelectedPatternText")) != null) {
            patternEditText.setText(selectedPatternValue);
        }

        // Handle implicit intents
        handleImplicitIntents();
    }

    private void handleImplicitIntents() {
        Intent intent = getIntent();
        String action = intent.getAction();
        Bundle bundle = intent.getExtras();

        // Share action
        if (Intent.ACTION_SEND.equals(action)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);

            if (sharedText == null) {
                Uri fileUri = null;
                if (bundle != null) {
                    fileUri = (Uri) bundle.get(Intent.EXTRA_STREAM);
                }

                // If a file was "shared"
                if (fileUri != null) {
                    Log.d(LOG_TAG, "fileUri = " + fileUri);
                    String fileUriString = fileUri.toString();
                    String filePath = fileUriString.substring("file://".length(), fileUriString.length());
                    Log.d(LOG_TAG, "filePath = " + filePath);

                    // Marshmallow+: If permission not granted...
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            && !prefs.getBoolean(PREFS_BOOL_PERM_DENIED, false)
                            && ContextCompat.checkSelfPermission(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {

                        // ...request it
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                1);
                    } else if (prefs.getBoolean(PREFS_BOOL_PERM_DENIED, false)) {
                        Snackbar.make(mainClayout, R.string.permission_grant_failed, Snackbar.LENGTH_LONG)
                                .setAction(R.string.snackbar_button_dismiss, null)
                                .show();
                    } else {
                        try {
                            sharedText = getStringFromFile(filePath);
                            if (sharedText != null) {
                                subjectEditText.setText(sharedText);
                                Log.d(LOG_TAG, "Logging sharedText below...");
                                Log.d(LOG_TAG, sharedText);
                            } else {
                                Log.d(LOG_TAG, "sharedText is null");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                subjectEditText.setText(sharedText);
                Log.d(LOG_TAG, "Logging sharedText below...");
                Log.d(LOG_TAG, sharedText);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(LOG_TAG, "Permission granted");
                    prefsEditor.putBoolean(PREFS_BOOL_PERM_DENIED, false);
                    prefsEditor.commit();
                    handleImplicitIntents();
                } else {
                    Log.d(LOG_TAG, "Permission denied");
                    prefsEditor.putBoolean(PREFS_BOOL_PERM_DENIED, true);
                    prefsEditor.commit();
                }
            }
        }
    }

    public static String convertStreamToString(InputStream is)
        throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile(String filePath)
        throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        // Make sure you close all streams.
        fin.close();
        return ret;
    }

    // Restore values saved in SharedPreferences
    // Note that the BOOL values of menu checkboxes aren't included,
    // they will be handled in separate methods
    private void restoreValues() {
        String subjectPresetValue = prefs.getString(PREFS_STRING_SUBJECT, "");
        String patternPresetValue = prefs.getString(PREFS_STRING_PATTERN, "");

        // Don't restore values if both strings are empty
        if (subjectPresetValue.length() != 0 || patternPresetValue.length() != 0) {
            subjectEditText.setText(subjectPresetValue);
            patternEditText.setText(patternPresetValue);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        savePrefs();
    }

    // Save preferences
    // Note that the BOOL values of menu checkboxes aren't included,
    // they will be handled in separate methods
    private void savePrefs() {
        prefsEditor.putString(PREFS_STRING_SUBJECT, subjectEditText.getText().toString());
        prefsEditor.putString(PREFS_STRING_PATTERN, patternEditText.getText().toString());
        prefsEditor.putBoolean(PREFS_BOOL_PERM_DENIED, false);
        prefsEditor.commit();
        prefsEditor.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        restoreMenuValues(menu);
        return true;
    }

    // Menu checkboxes are restored here
    private void restoreMenuValues(Menu menu) {
        boolean caseInsensitiveChecked = prefs.getBoolean(PREFS_BOOL_CASE_INSENSITIVE, false);
        boolean unixChecked = prefs.getBoolean(PREFS_BOOL_UNIX, false);
        boolean commentsChecked = prefs.getBoolean(PREFS_BOOL_COMMENTS, false);
        boolean dotAllChecked = prefs.getBoolean(PREFS_BOOL_DOT_ALL, false);
        boolean literalChecked = prefs.getBoolean(PREFS_BOOL_LITERAL, false);
        boolean multiLineChecked = prefs.getBoolean(PREFS_BOOL_MULTILINE, false);

        menu.findItem(R.id.case_insensitive_checkbox).setChecked(caseInsensitiveChecked);
        menu.findItem(R.id.unix_lines_checkbox).setChecked(unixChecked);
        menu.findItem(R.id.comments_checkbox).setChecked(commentsChecked);
        menu.findItem(R.id.dot_all_checkbox).setChecked(dotAllChecked);
        menu.findItem(R.id.literal_checkbox).setChecked(literalChecked);
        menu.findItem(R.id.multiline_checkbox).setChecked(multiLineChecked);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // If item is checkable
        if (item.isCheckable()) {
            boolean checked = !item.isChecked();

            // Switch state (unchecked -> checked, checked -> unchecked)
            item.setChecked(checked);

            // Save these preferences
            saveMenuPrefs(item);
            return true;
        }

        switch (id) {
            case R.id.action_menu_about: // Action "About"
                startAboutActivity();
                break;

            case R.id.action_menu_view_saved_regexes: // Action "View saved regexes
                startViewSavedRegexesActivity();
                break;
        }


        return super.onOptionsItemSelected(item);
    }

    private void startViewSavedRegexesActivity() {
        Intent intent = new Intent(this, ViewSavedRegexesActivity.class);
        startActivity(intent);
    }

    private void startAboutActivity() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    private void saveMenuPrefs(MenuItem item) {
        // Don't invert the boolean this time
        // it has already been inverted
        boolean checked = item.isChecked();

        int id = item.getItemId();
        switch (id) {
            case R.id.case_insensitive_checkbox:
                prefsEditor.putBoolean(PREFS_BOOL_CASE_INSENSITIVE, checked);
                prefsEditor.commit();
                break;

            case R.id.unix_lines_checkbox:
                prefsEditor.putBoolean(PREFS_BOOL_UNIX, checked);
                prefsEditor.commit();
                break;

            case R.id.comments_checkbox:
                prefsEditor.putBoolean(PREFS_BOOL_COMMENTS, checked);
                prefsEditor.commit();
                break;

            case R.id.dot_all_checkbox:
                prefsEditor.putBoolean(PREFS_BOOL_DOT_ALL, checked);
                prefsEditor.commit();
                break;

            case R.id.literal_checkbox:
                prefsEditor.putBoolean(PREFS_BOOL_LITERAL, checked);
                prefsEditor.commit();
                break;

            case R.id.multiline_checkbox:
                prefsEditor.putBoolean(PREFS_BOOL_MULTILINE, checked);
                prefsEditor.commit();
                break;
        }
    }

    private void onFindClick() {
        // Get flags
        int flags = 0;
        if (prefs.getBoolean(PREFS_BOOL_CASE_INSENSITIVE, false))
            flags |= Pattern.CASE_INSENSITIVE;

        if (prefs.getBoolean(PREFS_BOOL_UNIX, false))
            flags |= Pattern.UNIX_LINES;

        if (prefs.getBoolean(PREFS_BOOL_COMMENTS, false))
            flags |= Pattern.COMMENTS;

        if (prefs.getBoolean(PREFS_BOOL_DOT_ALL, false))
            flags |= Pattern.DOTALL;

        if (prefs.getBoolean(PREFS_BOOL_LITERAL, false))
            flags |= Pattern.LITERAL;

        if (prefs.getBoolean(PREFS_BOOL_MULTILINE, false))
            flags |= Pattern.MULTILINE;

        String subject = subjectEditText.getText().toString();
        String patternText = patternEditText.getText().toString();

        try {
            RegexFind regexFind = new RegexFind(subject, patternText, flags);

            // If match was found
            if (regexFind.matchFound()) {
                // Get indices where the matches were found
                int[][] matchesIndices = regexFind.getIndices();

                // Number of times the user pressed find button
                int count = 0;

                // If this is the first time the user clicked
                // the find button
                if (prefs.getInt(PREFS_INT_MATCH, -1) == -1) {
                    prefsEditor.putInt(PREFS_INT_MATCH, count);
                    prefsEditor.commit();

                    // If patternEditText already has focus
                    // requestFocus() from subjectEditText
                    // to steal focus away temporarily
                    if (subjectEditText.hasFocus())
                        patternEditText.requestFocus();

                    // Now request focus, make a toast
                    // if request failed
                    if (!subjectEditText.requestFocus())
                        Toast.makeText(this, R.string.cannot_request_focus_err_msg, Toast.LENGTH_SHORT).show();

                    // Select the first match
                    subjectEditText.setSelection(matchesIndices[0][0], matchesIndices[0][1]);
                } else {
                    // Increment count by 1
                    // After the above if structure,
                    // and after the below statement,
                    // count should be equal to 0
                    // after that it keeps incrementing on each click
                    // until it equals matchesIndices.length (see below)
                    count = prefs.getInt(PREFS_INT_MATCH, -1) + 1;

                    // count must never exceed matchesIndices.length
                    if (count < matchesIndices.length) {
                        // If patternEditText already has focus
                        // requestFocus() from subjectEditText
                        // to steal focus away temporarily
                        if (subjectEditText.hasFocus())
                            patternEditText.requestFocus();

                        // Now request focus, make a toast
                        // if request failed
                        if (!subjectEditText.requestFocus())
                            Toast.makeText(this, R.string.cannot_request_focus_err_msg, Toast.LENGTH_SHORT).show();

                        // Select (count)th match
                        subjectEditText.setSelection(matchesIndices[count][0], matchesIndices[count][1]);

                        // Save the value of count into prefs
                        prefsEditor.putInt(PREFS_INT_MATCH, count);
                        prefsEditor.commit();
                    } else {
                        // if count == matchesIndices.length
                        // reset the counter...
                        prefsEditor.putInt(PREFS_INT_MATCH, -1);
                        prefsEditor.commit();

                        // ...and display a toast message
                        Toast.makeText(this, R.string.reached_bottom, Toast.LENGTH_SHORT).show();

                        // also set the selection to the first char
                        subjectEditText.setSelection(0);
                    }
                }
            } else {
                // if no matches found
                // log it...
                Log.d(LOG_TAG, "No matches found");

                // ...and display a snackbar
                Snackbar.make(mainClayout, R.string.no_matches_found, Snackbar.LENGTH_SHORT)
                        .setAction(R.string.snackbar_button_dismiss, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {}
                        })
                        .show();
            }
        } catch (PatternSyntaxException e) {
            // If pattern contains syntax errors
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.syntax_error_title);

            // The position of the bad character
            // causing syntax error
            int index = e.getIndex();

            // Description of the error in the form
            // "%1$s near index %2$d"
            String description = String.format(
                    this.getResources().getString(R.string.syntax_error_description),
                    e.getDescription(),
                    index);
            String pattern = e.getPattern();

            // The complete message, before adding
            // ForegroundColorSpan
            String str = description + pattern;

            // Add a span for the bad character causing syntax error
            SpannableStringBuilder message = new SpannableStringBuilder(str);
            int color = ContextCompat.getColor(this, R.color.red500);

            // Set the span
            message.setSpan(new ForegroundColorSpan(color),
                    description.length() + index - 1, // before the bad character
                    description.length() + index, // right after the bad character
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Change the fontStyle of the pattern to monospace
            message.setSpan(new TypefaceSpan("monospace"),
                    description.length(),
                    message.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Show dialog
            builder.setMessage(message);
            builder.setPositiveButton(R.string.syntax_error_button_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            builder.show();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.find_button:
                onFindClick();
                break;

            case R.id.save_button:
                onSaveClick();
                break;
        }
    }

    private void onSaveClick() {
        // Create a new AlertDialog.Builder
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.save_regex_title);

        // Get patternText
        String patternText = patternEditText.getText().toString();

        // Get view to set for the Builder
        final View view = LayoutInflater.from(this).inflate(R.layout.save_confirm_dialog, null);
        AutoCompleteTextView keyEditText = (AutoCompleteTextView) view.findViewById(R.id.save_regex_key_edittext);
        final TextView textView = (TextView) view.findViewById(R.id.save_regex_pre_message_textview);

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
            e.printStackTrace();
            Toast.makeText(this, R.string.json_exception_toast_message, Toast.LENGTH_LONG);
        }

        // Get literal message string from resources
        String messageLiteral = getResources().getString(R.string.save_confirm_text);

        // Create a SpannableStringBuilder
        SpannableStringBuilder message = new SpannableStringBuilder(String.format(messageLiteral,
                        patternText));

        // Start and end points for the pattern
        // to change its style to monospace
        int start = message.toString().indexOf('"');
        int end = start + patternText.length();

        // Set span
        message.setSpan(new TypefaceSpan("monospace"),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Change text of the textView
        textView.setText(message);

        // Set the Builder's view
        builder.setView(view);
        builder.setPositiveButton(R.string.save_regex_button_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                final EditText editText = (EditText) view.findViewById(R.id.save_regex_key_edittext);

                // Get key and value pairs
                final String key = editText.getText().toString();
                final String value = patternEditText.getText().toString();

                // If the key entered was empty,
                // display a toast with error message
                // and return
                if (key.length() == 0) {
                    Snackbar.make(mainClayout, R.string.key_cannot_be_empty, Snackbar.LENGTH_LONG)
                            .setAction(R.string.generic_button_ok, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {}
                            })
                            .show();
                    return;
                }

                try {
                    final MyJSONArray jsonArray = new MyJSONArray(prefs.getString(PREFS_STRING_JSON_SAVED_REGEXES,
                            initialJsonArrayString));
                    final JSONObject jsonObject = new JSONObject();
                    jsonObject.put(key, value);

                    // Get the index of the matching key in MyJSONArray
                    final int keyIndex = jsonArray.getIndexOfKey(key);

                    // If the key was found
                    if (keyIndex >= 0) {
                        JSONObject jsonOldObject = jsonArray.optJSONObject(keyIndex);
                        AlertDialog.Builder builder2 = new AlertDialog.Builder(MainActivity.this);
                        final String dialogTitle = String.format(getResources()
                                        .getString(R.string.key_exists_title),
                                key);
                        final String dialogMessageString = String.format(getResources()
                                        .getString(R.string.key_exists_message),
                                key,
                                jsonOldObject.getString(key));
                        final SpannableStringBuilder dialogMessage = new SpannableStringBuilder(dialogMessageString);
                        int start = dialogMessageString.length() - jsonOldObject.getString(key).length();
                        int end = dialogMessage.toString().length();
                        dialogMessage.setSpan(new TypefaceSpan("monospace"),
                                start,
                                end,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        builder2.setTitle(dialogTitle);
                        builder2.setMessage(dialogMessage);
                        builder2.setPositiveButton(R.string.key_exists_button_ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        MyJSONArray jsonArrayNew = jsonArray.remove(keyIndex);
                                        jsonArrayNew.put(jsonObject);

                                        prefsEditor.putString(PREFS_STRING_JSON_SAVED_REGEXES, jsonArrayNew.toString());
                                        prefsEditor.commit();

                                        // Make a snackbar, telling the user
                                        // that the regex has been saved successfully
                                        Snackbar.make(mainClayout, R.string.regex_saved_success_message, Snackbar.LENGTH_SHORT)
                                                .setAction(R.string.snackbar_button_dismiss, new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {}
                                                })
                                                .show();
                                    }
                                });
                        builder2.setNegativeButton(R.string.key_exists_button_cancel, null);
                        builder2.show();
                    } else {
                        jsonArray.put(jsonObject);
                        prefsEditor.putString(PREFS_STRING_JSON_SAVED_REGEXES, jsonArray.toString());
                        Log.d(LOG_TAG, "Saving to prefs: jsonArray = " + jsonArray.toString());
                        prefsEditor.commit();

                        // Make a snackbar, telling the user
                        // that the regex has been saved successfully
                        Snackbar.make(mainClayout, R.string.regex_saved_success_message, Snackbar.LENGTH_SHORT)
                                .setAction(R.string.snackbar_button_dismiss, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {}
                                })
                                .show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(MainActivity.this, R.string.json_exception_toast_message, Toast.LENGTH_LONG).show();
                    Log.d(LOG_TAG, "JSONException, this shouldn't have happened...");
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton(R.string.save_regex_button_cancel, null);
        builder.show();
    }

    @Override
    public boolean onLongClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.find_button:
                AnchoredToast.makeText(findButton, this, R.string.find_button_text, AnchoredToast.LENGTH_SHORT)
                        .show();
                break;

            case R.id.save_button:
                AnchoredToast.makeText(saveButton, this, R.string.save_button_text, AnchoredToast.LENGTH_SHORT)
                        .show();
                break;
        }
        return true;
    }
}
