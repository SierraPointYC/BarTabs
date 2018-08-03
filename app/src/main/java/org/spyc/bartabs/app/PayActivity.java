package org.spyc.bartabs.app;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.spyc.bartabs.app.hal.Department;
import org.spyc.bartabs.app.hal.Payment;
import org.spyc.bartabs.app.hal.PaymentMethod;
import org.spyc.bartabs.app.hal.Transaction;
import org.spyc.bartabs.app.hal.User;

import java.util.Date;

public class PayActivity extends AppCompatActivity {

    User mUser;
    Button mSubmitButton;
    TextView mPayTextView;

    int mBarAmount = 0;
    int mFoodAmount = 0;
    int mDonationAmount = 0;
    int mOtherAmount = 0;
    int mTotalAmount;
    PaymentMethod mPaymentMethod;
    Department[] mDepartments = Department.values();
    int mCurrentDepartmentIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUser = getIntent().getParcelableExtra(BarTabActivity.USER_EXTRA);
        mBarAmount = getIntent().getIntExtra(BarTabActivity.BAR_AMOUNT_EXTRA, 0);
        mFoodAmount = getIntent().getIntExtra(BarTabActivity.FOOD_AMOUNT_EXTRA, 0);
        mDonationAmount = getIntent().getIntExtra(BarTabActivity.DONATION_AMOUNT_EXTRA, 0);
        mOtherAmount = getIntent().getIntExtra(BarTabActivity.OTHER_AMOUNT_EXTRA, 0);
        mTotalAmount = mBarAmount + mFoodAmount + mDonationAmount + mOtherAmount;
        setContentView(R.layout.activity_pay);

        mPayTextView = (TextView) findViewById(R.id.pay_text);
        mPayTextView.setText("I paid $" + mTotalAmount  + " to:");
        mSubmitButton = (Button) findViewById(R.id.submit_button);
        mSubmitButton.setEnabled(false);
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!processPayments()) {
                    finish();
                }
            }
        });
        RadioGroup paymentMethod = (RadioGroup)findViewById(R.id.payment_method);
        paymentMethod.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                switch (id) {
                    case R.id.payment_cash_box:
                        mPaymentMethod = PaymentMethod.CASH_BOX;
                        break;
                    case R.id.payment_cash_register:
                        mPaymentMethod = PaymentMethod.CASH_REGISTER;
                        break;
                    case R.id.payment_square:
                        mPaymentMethod = PaymentMethod.SQUARE;
                        break;
                    case R.id.payment_club_express:
                        mPaymentMethod = PaymentMethod.CLUBEXPRESS;
                        break;
                }
                mSubmitButton.setEnabled(true);
            }
        });
        Button cancelButton = (Button) findViewById(R.id.payment_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private int getDepartmentAmount(Department department) {
        switch (department) {
            case BAR:
                return mBarAmount;
            case FOOD:
                return mFoodAmount;
            case DONATION:
                return mDonationAmount;
            case OTHER:
                return mOtherAmount;
        }
        return 0;
    }

    private Department getNextDepartment() {
        mCurrentDepartmentIndex++;
        while(mCurrentDepartmentIndex < mDepartments.length) {
            Department d = mDepartments[mCurrentDepartmentIndex];
            int amount = getDepartmentAmount(d);
            if (amount > 0) {
                return d;
            }
            mCurrentDepartmentIndex++;
        }
        return null;
    }

    private boolean processPayments() {
        Department d = getNextDepartment();
        if (d == null) {
            return false;
        }
        int amount = getDepartmentAmount(d);
        submitPayment(d, amount);
        return true;
    }

    private void submitPayment(Department department, int amount) {
        Payment payment = new Payment(department, amount, mPaymentMethod, mUser, new Date());
        PendingIntent pendingResult = createPendingResult(
                RestClientService.SUBMIT_PAYMENT_REQUEST_CODE, new Intent(), 0);
        Intent intent = new Intent(getApplicationContext(), RestClientService.class);
        intent.putExtra(RestClientService.REQUEST_CODE_EXTRA, RestClientService.SUBMIT_PAYMENT_REQUEST_CODE);
        intent.putExtra(RestClientService.PAYMENT_EXTRA, payment);
        intent.putExtra(RestClientService.PENDING_RESULT_EXTRA, pendingResult);
        startService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RestClientService.SUBMIT_PAYMENT_REQUEST_CODE) {
            switch (resultCode) {
                case RestClientService.INVALID_URL_CODE:
                    RestClientService.showAlertDialog(this, "Invalid service URL submitting payment.");
                    break;
                case RestClientService.ERROR_CODE:
                    RestClientService.showAlertDialog(this, "Server error submitting payment.");
                    break;
               case RestClientService.RESULT_OK_CODE:
                    if (!processPayments()) {
                        showAlertDialog();
                        //finish();
                    }
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showAlertDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle("Thanks!");

        // set dialog message
        alertDialogBuilder
                .setMessage("Payment processed successfully.")
                .setCancelable(false)
                .setNeutralButton("Ok",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        finish();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }
}
