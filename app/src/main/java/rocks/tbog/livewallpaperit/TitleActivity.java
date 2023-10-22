package rocks.tbog.livewallpaperit;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class TitleActivity extends AppCompatActivity {

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (color != 0 && !(title instanceof Spannable)) {
                SpannableString ss = new SpannableString(title);
                ss.setSpan(new ForegroundColorSpan(color), 0, title.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                actionBar.setTitle(ss);
            } else {
                actionBar.setTitle(title);
            }
        }
    }
}
