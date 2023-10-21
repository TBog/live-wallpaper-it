package rocks.tbog.livewallpaperit;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class LWIViewModel extends AndroidViewModel {
    private final MutableLiveData<String> mRedditAuth = new MutableLiveData<>();
    private final MutableLiveData<RedditAuthState> mRedditAuthVerified = new MutableLiveData<>(RedditAuthState.AUTH_NOT_DONE);

    enum RedditAuthState {
        AUTH_NOT_DONE,
        AUTH_IN_PROGRESS,
        AUTH_FAILED,
        AUTH_VALID
    }
    public LWIViewModel(@NonNull Application application) {
        super(application);
    }

    public void setRedditAuth(String clientId) {
        mRedditAuth.postValue(clientId);
    }

    public LiveData<String> getRedditAuth() {
        return mRedditAuth;
    }

    public void setRedditAuthVerified(RedditAuthState state) {
        mRedditAuthVerified.postValue(state);
    }

    public LiveData<RedditAuthState> getRedditAuthVerified() {
        return mRedditAuthVerified;
    }
}
