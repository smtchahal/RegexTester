package smtchahal.regextester;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView appVersionTextView = (TextView) findViewById(R.id.about_app_version_textview);
        TextView appDescriptionTextView = (TextView) findViewById(R.id.about_app_description_textview);
        appVersionTextView.setText(BuildConfig.VERSION_NAME);

        // Make <a href="..."></a> links clickable
        appDescriptionTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
