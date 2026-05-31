package com.baghdad.edulife.core.session;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Process-wide signal for "the Firebase token is no longer valid — sign the learner out".
 *
 * Network-layer components ({@code FirebaseTokenAuthenticator}) post here from an OkHttp worker
 * thread. The host activity is the single point that reacts: it tears down the session and
 * navigates to the login screen. Spreading sign-out across the network layer historically
 * logged the learner out on transient blips because each layer made its own call.
 */
public final class SessionEventBus {

    private static final MutableLiveData<Boolean> SESSION_EXPIRED = new MutableLiveData<>();

    private SessionEventBus() {}

    public static LiveData<Boolean> sessionExpired() {
        return SESSION_EXPIRED;
    }

    public static void postSessionExpired() {
        SESSION_EXPIRED.postValue(true);
    }

    /**
     * Consumed by the host activity after it has signed the learner out so a re-attached
     * observer cannot re-fire sign-out + nav.
     */
    public static void clear() {
        SESSION_EXPIRED.postValue(null);
    }
}
