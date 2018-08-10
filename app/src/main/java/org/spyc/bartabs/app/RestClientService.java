package org.spyc.bartabs.app;


import android.app.AlertDialog;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.spyc.bartabs.app.hal.GetResponse;
import org.spyc.bartabs.app.hal.Payment;
import org.spyc.bartabs.app.hal.Transaction;
import org.spyc.bartabs.app.hal.User;

import java.util.Collections;
import java.util.Date;

public class RestClientService extends IntentService {

    private static final String TAG = RestClientService.class.getSimpleName();

    public static final int LOAD_USERS_REQUEST_CODE = 1;
    public static final int LOAD_ITEMS_REQUEST_CODE = 2;
    public static final int LOAD_TRANSACTIONS_REQUEST_CODE = 3;
    public static final int SUBMIT_TRANSACTION_REQUEST_CODE = 4;
    public static final int CANCEL_TRANSACTION_REQUEST_CODE = 5;
    public static final int SUBMIT_PAYMENT_REQUEST_CODE = 6;

    // Incoming extra parameters
    public static final String PENDING_RESULT_EXTRA = "pending_result";
    public static final String REQUEST_CODE_EXTRA = "request_code";
    public static final String USER_EXTRA = "user";
    public static final String TRANSACTION_EXTRA = "transaction";
    public static final String PAYMENT_EXTRA = "transaction";

    // Outgoing extra parameters
    public static final String USERS_RESULT_EXTRA = "user_data";
    public static final String ITEMS_RESULT_EXTRA = "item_data";
    public static final String TRANSACTIONS_RESULT_EXTRA = "transaction_data";

    // Result codes
    public static final int RESULT_OK_CODE = 0;
    public static final int INVALID_URL_CODE = 1;
    public static final int ERROR_CODE = 2;

    public static final String kDefaultServer = "http://10.0.2.2:8080/";
    public static final String kApiBasePath = "api/";
    public static String mServer = kDefaultServer + kApiBasePath;

    private RestTemplate mRestTemplate;

    public RestClientService() {
        super(TAG);

    }


    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mServer = sharedPref.getString("bar_tabs_server", "kDefaultServer") + kApiBasePath;

        // Create a new RestTemplate instance
        mRestTemplate = new RestTemplate();

        // Add the Jackson and String message converters
        mRestTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        mRestTemplate.getMessageConverters().add(new StringHttpMessageConverter());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        PendingIntent reply = intent.getParcelableExtra(PENDING_RESULT_EXTRA);
        try {
            try {

                int requestCode = intent.getIntExtra(REQUEST_CODE_EXTRA, 0);
                Intent result = new Intent();

                // Set the Accept header
                HttpHeaders requestHeaders = new HttpHeaders();
                requestHeaders.setAccept(Collections.singletonList(new MediaType("application","json")));
                boolean success = false;
                switch(requestCode) {
                    case LOAD_USERS_REQUEST_CODE:
                        success = loadUsers(mRestTemplate, requestHeaders, result);
                        break;
                    case LOAD_ITEMS_REQUEST_CODE:
                        success = loadItems(mRestTemplate, requestHeaders, result);
                        break;
                    case LOAD_TRANSACTIONS_REQUEST_CODE:
                        User user = intent.getParcelableExtra(USER_EXTRA);
                        success = loadTransactions(user, mRestTemplate, requestHeaders, result);
                        break;
                    case SUBMIT_TRANSACTION_REQUEST_CODE:
                        Transaction transaction = intent.getParcelableExtra(TRANSACTION_EXTRA);
                        success = submitTransaction(transaction, mRestTemplate, requestHeaders, result);
                        break;
                    case CANCEL_TRANSACTION_REQUEST_CODE:
                        Transaction transaction2 = intent.getParcelableExtra(TRANSACTION_EXTRA);
                        success = cancelTransaction(transaction2, mRestTemplate, requestHeaders, result);
                        break;
                    case SUBMIT_PAYMENT_REQUEST_CODE:
                        Payment payment = intent.getParcelableExtra(TRANSACTION_EXTRA);
                        success = submitPayment(payment, mRestTemplate, requestHeaders, result);
                        break;
                    default:
                        Log.i(TAG, "Invalid request code " + requestCode);
                        reply.send(ERROR_CODE);
                        break;
                }
                if (success) {
                    reply.send(this, RESULT_OK_CODE, result);
                }
                else {
                    Log.i(TAG, "HTTP request failed");
                    //result.putExtra(USERS_RESULT_EXTRA, response.get_embedded().user);
                    reply.send(ERROR_CODE);
                }
           }
            catch (HttpStatusCodeException e) {
                HttpStatus statusCode = e.getStatusCode();
                String responseString = e.getResponseBodyAsString();
                Log.i(TAG, "HTTP request failed "  + statusCode + " " + responseString);
            }
           catch (Exception exc) {
                // could do better by treating the different sax/xml exceptions individually
                Log.i(TAG, "request failed", exc);
                reply.send(ERROR_CODE);
            }
        } catch (PendingIntent.CanceledException exc) {
            Log.i(TAG, "reply cancelled", exc);
        }

    }

    private boolean loadUsers(RestTemplate restTemplate, HttpHeaders requestHeaders, Intent result) {
        // Make the HTTP GET request, marshaling the response from JSON to an array of Events
        HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
        ResponseEntity<GetResponse> responseEntity = restTemplate.exchange(mServer + "user", HttpMethod.GET, requestEntity, GetResponse.class);
        GetResponse response = responseEntity.getBody();

        result.putExtra(USERS_RESULT_EXTRA, response.get_embedded().user);
        return responseEntity.getStatusCode()== HttpStatus.OK;
    }

    private boolean loadItems(RestTemplate restTemplate, HttpHeaders requestHeaders, Intent result) {
        // Make the HTTP GET request, marshaling the response from JSON to an array of Events
        HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
        ResponseEntity<GetResponse> responseEntity = restTemplate.exchange(mServer + "item", HttpMethod.GET, requestEntity, GetResponse.class);
        GetResponse response = responseEntity.getBody();

        result.putExtra(ITEMS_RESULT_EXTRA, response.get_embedded().item);
        return responseEntity.getStatusCode()== HttpStatus.OK;

    }

    private boolean loadTransactions(User user, RestTemplate restTemplate, HttpHeaders requestHeaders, Intent result) {
        // Make the HTTP GET request, marshaling the response from JSON to an array of Events
        HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
        String url = "transaction/search/findByStatusAndUserName?status=" + Transaction.Status.UNPAID +"&name=" + user.getName();
        ResponseEntity<GetResponse> responseEntity = restTemplate.exchange(mServer + url, HttpMethod.GET, requestEntity, GetResponse.class);
        GetResponse response = responseEntity.getBody();

        result.putExtra(TRANSACTIONS_RESULT_EXTRA, response.get_embedded().transaction);

        return responseEntity.getStatusCode()== HttpStatus.OK;
    }


    private boolean submitTransaction(Transaction transaction, RestTemplate restTemplate, HttpHeaders requestHeaders, Intent result) {
        // Make the HTTP POST request
        HttpEntity<Transaction> requestEntity = new HttpEntity<Transaction>(transaction, requestHeaders);
        ResponseEntity<Transaction> responseEntity = restTemplate.exchange(mServer + "transaction", HttpMethod.POST, requestEntity, Transaction.class);
        return responseEntity.getStatusCode()== HttpStatus.CREATED;
    }

    private boolean submitPayment(Payment payment, RestTemplate restTemplate, HttpHeaders requestHeaders, Intent result) {
        // Make the HTTP POST request
        HttpEntity<Payment> requestEntity = new HttpEntity<Payment>(payment, requestHeaders);
        ResponseEntity<Payment> responseEntity = restTemplate.exchange(mServer + "payment", HttpMethod.POST, requestEntity, Payment.class);
        return responseEntity.getStatusCode()== HttpStatus.CREATED;
    }

    private boolean cancelTransaction(Transaction transaction, RestTemplate restTemplate, HttpHeaders requestHeaders, Intent result) {
        // Make the HTTP PATCH request
        transaction.setStatus(Transaction.Status.CANCELLED);
        transaction.setCloseDate(new Date());
        HttpEntity<Transaction> requestEntity = new HttpEntity<Transaction>(transaction, requestHeaders);
        ResponseEntity<Transaction> responseEntity = restTemplate.exchange(transaction.get_links().self.href, HttpMethod.PATCH, requestEntity, Transaction.class);
        return responseEntity.getStatusCode()== HttpStatus.OK;
    }
    public static void showAlertDialog(Context ctx, String error_msg) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);

        // set title
        alertDialogBuilder.setTitle("Server error!");

        // set dialog message
        alertDialogBuilder
                .setMessage(error_msg)
                .setCancelable(false)
                .setNeutralButton("Ok",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

}
