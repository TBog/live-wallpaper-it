package rocks.tbog.livewallpaperit;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class LWIActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView clientId = findViewById(R.id.client_id);
        clientId.setText(Utils.loadRedditAuth(getApplicationContext()));
    }
}