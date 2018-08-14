package org.spyc.bartabs.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;

import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.spyc.bartabs.app.hal.User;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    public static final String USER_TAG_EXTRA = "user_tag";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mMemberNameView;
    private EditText mMemberPinView;
    private View mProgressView;
    private View mLoginFormView;
    private Map<String, User> mUserMap = null;
    private String mTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTag = getIntent().getStringExtra(USER_TAG_EXTRA);

        populateAutoComplete();

        setContentView(R.layout.activity_login);
        // Set up the login form.
        mMemberNameView = findViewById(R.id.member_name);

        mMemberPinView = findViewById(R.id.member_pin);
        mMemberPinView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    private void populateAutoComplete() {
        PendingIntent pendingResult = createPendingResult(
                RestClientService.LOAD_USERS_REQUEST_CODE, new Intent(), 0);
        Intent intent = new Intent(getApplicationContext(), RestClientService.class);
        intent.putExtra(RestClientService.REQUEST_CODE_EXTRA, RestClientService.LOAD_USERS_REQUEST_CODE);
        intent.putExtra(RestClientService.PENDING_RESULT_EXTRA, pendingResult);
        startService(intent);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RestClientService.LOAD_USERS_REQUEST_CODE) {
            switch (resultCode) {
                case RestClientService.INVALID_URL_CODE:
                    RestClientService.showAlertDialog(this, "Invalid service URL.");
                    break;
                case RestClientService.ERROR_CODE:
                    RestClientService.showAlertDialog(this, "Server error.");
                    break;
                case RestClientService.RESULT_OK_CODE:
                    Parcelable[] users = data.getParcelableArrayExtra(RestClientService.USERS_RESULT_EXTRA);
                    Map<String, User> userMap = new HashMap<>();
                    boolean tagInvalid = true;
                    for (Parcelable p : users) {
                        User user = (User) p;
                        if (mTag != null && mTag.equals(user.getTag())) {
                            tagInvalid = false;
                            startBarTabActivity(user);
                        }
                        userMap.put(user.getName(), user);
                    }
                    if (tagInvalid && mTag != null) {
                        TextView v = findViewById(R.id.unknown_tag_view);
                        v.setText("Unknown Tag, UUID=" + mTag);
                        v.setVisibility(View.VISIBLE);
                    }
                    mUserMap = userMap;
                    addNamesToAutoComplete(userMap.keySet());
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startBarTabActivity(User user) {
        finish();
        Intent in=new Intent(LoginActivity.this, BarTabActivity.class);
        in.putExtra(BarTabActivity.USER_EXTRA, user);
        startActivity(in);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mMemberNameView.setError(null);
        mMemberPinView.setError(null);

        // Store values at the time of the login attempt.
        String name = mMemberNameView.getText().toString();
        String password = mMemberPinView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mMemberPinView.setError(getString(R.string.error_invalid_password));
            focusView = mMemberPinView;
            cancel = true;
        }
        if (isAdminLogin(name, password)) {
            // This administrator login, show settings
            finish();
            Intent in = new Intent(LoginActivity.this, SettingsActivity.class);
            startActivity(in);
        }
        // Check for a valid name.
        if (TextUtils.isEmpty(name)) {
            mMemberNameView.setError(getString(R.string.error_field_required));
            focusView = mMemberNameView;
            cancel = true;
        } else if (!isNameValid(name)) {
            mMemberNameView.setError(getString(R.string.error_invalid_name));
            focusView = mMemberNameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(name, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isAdminLogin(String name, String password) {
        return "Admin".equals(name) && "513774".equals(password);
    }

    private boolean isNameValid(String name) {
        return mUserMap != null && mUserMap.containsKey(name);
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void addNamesToAutoComplete(Set<String> userNames) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        //android.R.layout.simple_dropdown_item_1line,
                        android.R.layout.simple_selectable_list_item,
                        userNames.toArray(new String[]{}));

        mMemberNameView.setAdapter(adapter);
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mName;
        private final String mPassword;

        UserLoginTask(String name, String password) {
            mName = name;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return false;
            }

            if (mUserMap == null) {
                return false;
            }

            String pin = mUserMap.get(mName).getPin();

            return mPassword.equals(pin);
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                startBarTabActivity(mUserMap.get(mName));

            } else {
                mMemberPinView.setError(getString(R.string.error_incorrect_password));
                mMemberPinView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

