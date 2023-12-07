package rocks.tbog.livewallpaperit;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class LWIViewModel extends AndroidViewModel {
    private final MutableLiveData<RedditAuth> mRedditAuth = new MutableLiveData<>(new RedditAuth());
    private final MutableLiveData<RedditAuthState> mRedditAuthState =
            new MutableLiveData<>(RedditAuthState.AUTH_NOT_DONE);

    enum RedditAuthState {
        AUTH_NOT_DONE,
        AUTH_NOT_NEEDED,
        AUTH_IN_PROGRESS,
        AUTH_FAILED,
        AUTH_VALID
    }

    public LWIViewModel(@NonNull Application application) {
        super(application);
    }

    public void setRedditAuthNow(@NonNull String clientId, boolean isVerified) {
        mRedditAuth.setValue(new RedditAuth(clientId, isVerified));
    }

    public void setRedditAuth(@NonNull String clientId, boolean isVerified) {
        mRedditAuth.postValue(new RedditAuth(clientId, isVerified));
    }

    public LiveData<RedditAuth> getRedditAuth() {
        return mRedditAuth;
    }

    public void setRedditAuthState(RedditAuthState state) {
        mRedditAuthState.postValue(state);
    }

    public LiveData<RedditAuthState> getRedditAuthState() {
        return mRedditAuthState;
    }

    public static class RedditAuth {
        @NonNull
        public final String mClientId;

        public final boolean mIsVerified;

        public RedditAuth(@NonNull String clientId, boolean isVerified) {
            mClientId = clientId;
            mIsVerified = isVerified;
        }

        public RedditAuth() {
            this("", false);
        }
    }
}
