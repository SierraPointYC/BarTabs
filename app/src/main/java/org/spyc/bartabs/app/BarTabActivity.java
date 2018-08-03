package org.spyc.bartabs.app;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.spyc.bartabs.app.hal.Item;
import org.spyc.bartabs.app.hal.ItemType;
import org.spyc.bartabs.app.hal.Transaction;
import org.spyc.bartabs.app.hal.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BarTabActivity extends AppCompatActivity {


    public static final String ITEM_EXTRA = "item";
    public static final String USER_EXTRA = "user";
    public static final String BAR_AMOUNT_EXTRA = "bar_amount";
    public static final String FOOD_AMOUNT_EXTRA = "food_amount";
    public static final String DONATION_AMOUNT_EXTRA = "donation_amount";
    public static final String OTHER_AMOUNT_EXTRA = "other_amount";
    public static final String TRANSACTIONS_EXTRA = "transaction";
    private User mUser;
    private Map<ItemType, Item> mItems;
    private List<Transaction> mTransactions;
    private Map<ItemType, BarTabLine> tab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUser = getIntent().getParcelableExtra(USER_EXTRA);
        setContentView(R.layout.activity_bar_tab);
        loadItems();
        TextView nameText = (TextView) findViewById(R.id.nameText);
        nameText.setText(mUser.getName() + " Bar Tab");
        Button beerButton = (Button) findViewById(R.id.beerButton);
        beerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBuyActivity(mItems.get(ItemType.BEER));
            }
        });
        Button sodaButton = (Button) findViewById(R.id.sodaButton);
        sodaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBuyActivity(mItems.get(ItemType.SODA));
            }
        });
        Button juiceButton = (Button) findViewById(R.id.juiceButton);
        juiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBuyActivity(mItems.get(ItemType.JUICE));
            }
        });
        Button corkageButton = (Button) findViewById(R.id.corkageButton);
        corkageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBuyActivity(mItems.get(ItemType.CORKAGE));
            }
        });
        Button mealButton = (Button) findViewById(R.id.mealButton);
        mealButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBuyActivity(mItems.get(ItemType.MEAL));
            }
        });
        Button payButton = (Button) findViewById(R.id.payButton);
        payButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPayActivity();
            }
        });

        Button historyButton = (Button) findViewById(R.id.historyButton);
        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startHistoryActivity();
            }
        });

        Button doneButton = (Button) findViewById(R.id.doneButton);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavUtils.navigateUpFromSameTask(BarTabActivity.this);
            }
        });


    }

    @Override
    protected void onStart() {
        loadTransactions();
        super.onStart();
    }

    @Override
    protected void onResume() {
        //loadTransactions();
        super.onResume();
    }

    private static class BarTabLine {
        int count;
        int total;
    }

    private Map<ItemType, BarTabLine> calculateBarTab() {
        Map<ItemType, BarTabLine> tab = new HashMap<ItemType, BarTabLine>();
        for (Transaction t : mTransactions) {
            if (t.getStatus() != Transaction.Status.UNPAID) {
                continue;
            }
            ItemType item = t.getItem();
            BarTabLine line = tab.get(item);
            if (line == null) {
                line = new BarTabLine();
                line.count=t.getItems();
                line.total=t.getAmount();
                tab.put(item, line);
                continue;
            }
            line.count += t.getItems();
            line.total += t.getAmount();
        }
        return tab;
    }

    private int updateBarTabLine(Item item, Map<ItemType, BarTabLine> tab, TextView text, TextView total) {
        BarTabLine line = tab.get(item.getType());
        int unitCost = item.getCost();
        int totalCost = 0;
        String textString = "0 x $" + unitCost;
        String totalString="$0";
        if (line != null) {
            textString = line.count + " x $" + unitCost;
            totalString = "$" + line.total;
            totalCost = line.total;
        }
        text.setText(textString);
        total.setText(totalString);
        return totalCost;
    }

    private void updateBarTab() {
        tab = calculateBarTab();
        int total = 0;
        TextView countText = (TextView) findViewById(R.id.beerText);
        TextView totalText = (TextView) findViewById(R.id.beerTotal);
        total += updateBarTabLine(mItems.get(ItemType.BEER), tab, countText, totalText);
        countText = (TextView) findViewById(R.id.sodaText);
        totalText = (TextView) findViewById(R.id.sodaTotal);
        total += updateBarTabLine(mItems.get(ItemType.SODA), tab, countText, totalText);
        countText = (TextView) findViewById(R.id.juiceText);
        totalText = (TextView) findViewById(R.id.juiceTotal);
        total += updateBarTabLine(mItems.get(ItemType.JUICE), tab, countText, totalText);
        countText = (TextView) findViewById(R.id.corkageText);
        totalText = (TextView) findViewById(R.id.corkageTotal);
        total += updateBarTabLine(mItems.get(ItemType.CORKAGE), tab, countText, totalText);
        countText = (TextView) findViewById(R.id.mealText);
        totalText = (TextView) findViewById(R.id.mealTotal);
        total += updateBarTabLine(mItems.get(ItemType.MEAL), tab, countText, totalText);
        TextView grandTotal = (TextView) findViewById(R.id.textTotal);
        grandTotal.setText("$" + total);
    }

    private void startBuyActivity(Item item) {
        Intent in = new Intent(BarTabActivity.this, BuyActivity.class);
        in.putExtra(ITEM_EXTRA, item);
        in.putExtra(USER_EXTRA, mUser);
        startActivity(in);
    }

    private void startPayActivity() {
        Intent in = new Intent(BarTabActivity.this, PayActivity.class);
        int barAmount = 0;
        int foodAmount = 0;
        int donationsAmount = 0;
        int otherAmount = 0;
        for (ItemType it : tab.keySet()) {
            BarTabLine line = tab.get(it);
            Item item = mItems.get(it);
            switch (item.getDepartment()) {
                case BAR:
                    barAmount += line.total;
                    break;
                case FOOD:
                    foodAmount += line.total;
                    break;
                case DONATION:
                    donationsAmount += line.total;
                    break;
                case OTHER:
                    otherAmount += line.total;
                    break;
            }
        }
        in.putExtra(USER_EXTRA, mUser);
        in.putExtra(BAR_AMOUNT_EXTRA, barAmount);
        in.putExtra(FOOD_AMOUNT_EXTRA, foodAmount);
        in.putExtra(DONATION_AMOUNT_EXTRA, donationsAmount);
        in.putExtra(OTHER_AMOUNT_EXTRA, otherAmount);
        startActivity(in);
    }

    private void startHistoryActivity() {
        Intent in = new Intent(BarTabActivity.this, HistoryActivity.class);
        in.putExtra(USER_EXTRA, mUser);
        in.putExtra(TRANSACTIONS_EXTRA, mTransactions.toArray(new Transaction[mTransactions.size()]));
        startActivity(in);
    }

    private void loadItems() {
        PendingIntent pendingResult = createPendingResult(
                RestClientService.LOAD_ITEMS_REQUEST_CODE, new Intent(), 0);
        Intent intent = new Intent(getApplicationContext(), RestClientService.class);
        intent.putExtra(RestClientService.REQUEST_CODE_EXTRA, RestClientService.LOAD_ITEMS_REQUEST_CODE);
        intent.putExtra(RestClientService.PENDING_RESULT_EXTRA, pendingResult);
        startService(intent);
    }

    private void loadTransactions() {
        PendingIntent pendingResult = createPendingResult(
                RestClientService.LOAD_TRANSACTIONS_REQUEST_CODE, new Intent(), 0);
        Intent intent = new Intent(getApplicationContext(), RestClientService.class);
        intent.putExtra(RestClientService.REQUEST_CODE_EXTRA, RestClientService.LOAD_TRANSACTIONS_REQUEST_CODE);
        intent.putExtra(RestClientService.USER_EXTRA, mUser);
        intent.putExtra(RestClientService.PENDING_RESULT_EXTRA, pendingResult);
        startService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RestClientService.LOAD_ITEMS_REQUEST_CODE) {
            switch (resultCode) {
                case RestClientService.INVALID_URL_CODE:
                    RestClientService.showAlertDialog(this, "Invalid service URL loading items.");
                    break;
                case RestClientService.ERROR_CODE:
                    RestClientService.showAlertDialog(this, "Server error loading items.");
                    break;
                case RestClientService.RESULT_OK_CODE:
                    Parcelable[] items = data.getParcelableArrayExtra(RestClientService.ITEMS_RESULT_EXTRA);
                    Map<ItemType, Item> itemMap = new HashMap<ItemType,Item>();
                    for (Parcelable p : items) {
                        Item item = (Item) p;
                        itemMap.put(item.getType(),item);
                    }
                    mItems = itemMap;
                    break;
            }
        }
        else if (requestCode == RestClientService.LOAD_TRANSACTIONS_REQUEST_CODE) {
            switch (resultCode) {
                case RestClientService.INVALID_URL_CODE:
                    //RestClientService.showAlertDialog(this, "Invalid service URL loading transactions.");
                    break;
                case RestClientService.ERROR_CODE:
                    //RestClientService.showAlertDialog(this, "Server error loading transactions.");
                    break;
                case RestClientService.RESULT_OK_CODE:
                    Parcelable[] transactions = data.getParcelableArrayExtra(RestClientService.TRANSACTIONS_RESULT_EXTRA);
                    List<Transaction> transactionList = new ArrayList<Transaction>();
                    for (Parcelable p : transactions) {
                        Transaction transaction = (Transaction) p;
                        transactionList.add(transaction);
                    }
                    mTransactions = transactionList;
                    updateBarTab();
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
