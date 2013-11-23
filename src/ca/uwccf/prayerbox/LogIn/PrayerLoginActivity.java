package ca.uwccf.prayerbox.LogIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import ca.uwccf.prayerbox.R;
import ca.uwccf.prayerbox.Data.PrayerInternetInfo;
import ca.uwccf.prayerbox.Data.PrayerParser;
import ca.uwccf.prayerbox.MainScreen.MainTabbedFragmentActivity;
import ca.uwccf.prayerbox.R.id;
import ca.uwccf.prayerbox.R.layout;
import ca.uwccf.prayerbox.R.menu;
import ca.uwccf.prayerbox.R.string;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class PrayerLoginActivity extends Activity {
	public static DefaultHttpClient client;
	public static PrayerInternetInfo intInfo;
	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private UserLoginTask mAuthTask = null;

	// Values for email and password at the time of the login attempt.
	private String mUser;
	private String mPassword;

	// UI references.
	private EditText mUserView;
	private EditText mPasswordView;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences sharedPref = this.getSharedPreferences(
				ACCOUNT_SERVICE, MODE_PRIVATE);
		client = new DefaultHttpClient();
		intInfo = new PrayerInternetInfo();
		if(!sharedPref.getBoolean("validated", false)){
			Intent intent = new Intent(getApplicationContext(),
					PrayerValidationActivity.class);
			startActivity(intent);
			finish();
		}
		if (sharedPref.contains("session_id")) {
			Intent intent = new Intent(getApplicationContext(),
					MainTabbedFragmentActivity.class);
			startActivity(intent);
			finish();
		}

		setContentView(R.layout.activity_prayer_login);

		getActionBar().hide();

		mUserView = (EditText) findViewById(R.id.user);
		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id,
							KeyEvent keyEvent) {
						if (id == R.id.login || id == EditorInfo.IME_NULL) {
							attemptLogin();
							return true;
						}
						return false;
					}
				});

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.sign_in_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptLogin();
					}
				});
		
		findViewById(R.id.sign_up_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						Intent i = new Intent(getApplicationContext(), PrayerSignUpActivity.class);
						startActivity(i);
					}
					
				});
		findViewById(R.id.forgot_pass_button).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getApplicationContext(), PrayerResetPasswordActivity.class);
				startActivity(i);				
			}
		});

		findViewById(R.id.prayerbox_logo).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent i = new Intent(Intent.ACTION_VIEW, Uri
								.parse("http://www.uwccf.ca/prayerbox"));
						startActivity(i);
					}
				});

		findViewById(R.id.uwccf_logo_button).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent i = new Intent(Intent.ACTION_VIEW, Uri
								.parse("http://www.uwccf.ca"));
						startActivity(i);
					}
				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.prayer_login, menu);
		return true;
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mUserView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mUser = mUserView.getText().toString();
		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		} else if (mPassword.length() < 4) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(mUser)) {
			mUserView.setError(getString(R.string.error_field_required));
			focusView = mUserView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mPasswordView.getWindowToken(), 0);
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true);
			if(intInfo.isNetworkAvailable(getApplicationContext())){
				mAuthTask = new UserLoginTask(PrayerLoginActivity.this);
				mAuthTask.execute((Void) null);
			}else{
				Toast.makeText(getApplicationContext(), R.string.no_internet, Toast.LENGTH_LONG).show();
				showProgress(false);
			}
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginTask extends AsyncTask<Void, Void, String> {
		private String result;
		private Context mContext;

		public UserLoginTask(Context context) {
			mContext = context;
		}

		@Override
		protected String doInBackground(Void... params) {
			// TODO: attempt authentication against a network service.
			try {
				HttpPost httpMethod = new HttpPost(
						"http://www.uwccf.ca/prayerbox/api/loginproxy.php");
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						2);
				nameValuePairs.add(new BasicNameValuePair("user", mUser));
				nameValuePairs
						.add(new BasicNameValuePair("password", mPassword));
				httpMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				HttpResponse response = client.execute(httpMethod);
				HttpEntity entity = response.getEntity();
				result = EntityUtils.toString(entity);
				return result;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

		}

		@Override
		protected void onPostExecute(final String success) {
			mAuthTask = null;
			showProgress(false);
			PrayerParser pray_parser = new PrayerParser(result);
			HashMap<String, String> accountInfo = pray_parser.parseLogin();
			if (!accountInfo.get("error").isEmpty()) {
				mPasswordView
						.setError(getString(R.string.error_incorrect_password));
				mPasswordView.requestFocus();
			} else {
				SharedPreferences sharedPref = mContext.getSharedPreferences(
						ACCOUNT_SERVICE, MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putString("user", accountInfo.get("user"));
				editor.putString("session_id", accountInfo.get("session_id"));
				editor.commit();
				Intent intent = new Intent(getApplicationContext(),
						MainTabbedFragmentActivity.class);
				startActivity(intent);
				finish();
			}
		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
			showProgress(false);
		}
	}
}
