package org.spyc.bartabs.app;

import android.app.PendingIntent;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.spyc.bartabs.app.hal.Item;
import org.spyc.bartabs.app.hal.Transaction;
import org.spyc.bartabs.app.hal.User;

import java.text.MessageFormat;
import java.util.Date;

public class BuyActivity extends AppCompatActivity {

    private User mUser;
    private Item mItem;
    private int mCount = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy);
        mUser = getIntent().getParcelableExtra(BarTabActivity.USER_EXTRA);
        mItem = getIntent().getParcelableExtra(BarTabActivity.ITEM_EXTRA);

        updateBuyText();

        ImageButton upButton = findViewById(R.id.upArrowButton);
        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCount++;
                updateBuyText();
            }
        });

        ImageButton downButton = findViewById(R.id.downArrowButton);
        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCount--;
                updateBuyText();
            }
    });
        Button buyButton = findViewById(R.id.buyButton);
        buyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performTransaction();
            }
        });

        Button doneButton = findViewById(R.id.cancelBuyButton);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void updateBuyText() {
        TextView nameText = findViewById(R.id.buyText);
        String itemString = mItem.getType().toString().toLowerCase();
        if (mCount > 1) {
            itemString = itemString + "s";
        }
        nameText.setText(MessageFormat.format("Add {0} {1} for ${2}", mCount, itemString, mCount * mItem.getCost()));

    }

    private void performTransaction() {
        Transaction transaction = new Transaction(mItem, mCount, mUser, new Date());
        PendingIntent pendingResult = createPendingResult(
                RestClientService.SUBMIT_TRANSACTION_REQUEST_CODE, new Intent(), 0);
        Intent intent = new Intent(getApplicationContext(), RestClientService.class);
        intent.putExtra(RestClientService.REQUEST_CODE_EXTRA, RestClientService.SUBMIT_TRANSACTION_REQUEST_CODE);
        intent.putExtra(RestClientService.TRANSACTION_EXTRA, transaction);
        intent.putExtra(RestClientService.PENDING_RESULT_EXTRA, pendingResult);
        startService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RestClientService.SUBMIT_TRANSACTION_REQUEST_CODE) {
            switch (resultCode) {
                case RestClientService.INVALID_URL_CODE:
                    RestClientService.showAlertDialog(this, "Invalid service URL submitting a transaction.");
                    break;
                case RestClientService.ERROR_CODE:
                    RestClientService.showAlertDialog(this, "Server error submitting transaction.");
                    break;
                case RestClientService.RESULT_OK_CODE:
                    finish();
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
