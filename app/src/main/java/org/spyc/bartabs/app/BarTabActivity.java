package org.spyc.bartabs.app;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Parcelable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.spyc.bartabs.app.hal.Item;
import org.spyc.bartabs.app.hal.ItemType;
import org.spyc.bartabs.app.hal.Transaction;
import org.spyc.bartabs.app.hal.User;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    private CountDownTimer mIdleTimer;

    private int mTableRows = 0;
    private float mButtonTextSize = 24.0F;
    private float mTextSize = 34.0F;
    private float buttonHeightFactor = 2.5F;
    private boolean needToLoadTransactions = false;

    private static class ViewHolder {
        Button button;
        TextView text;
        TextView subTotal;
    }

    private ViewHolder[] mViewHolders =  new ViewHolder[ItemType.values().length];
    private List<ItemType> defaultItems = Arrays.asList(ItemType.SODA, ItemType.JUICE, ItemType.BEER);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUser = getIntent().getParcelableExtra(USER_EXTRA);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        setContentView(R.layout.activity_bar_tab);

        View content = findViewById(R.id.bar_tab_content);
        content.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);


        loadItems();
        TextView nameText = findViewById(R.id.nameText);
        nameText.setText(MessageFormat.format("Bar Tab of {0}", mUser.getName()));

        Button payButton = findViewById(R.id.payButton);
        payButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPayActivity();
            }
        });

        Button historyButton = findViewById(R.id.historyButton);
        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startHistoryActivity();
            }
        });

        Button doneButton = findViewById(R.id.doneButton);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavUtils.navigateUpFromSameTask(BarTabActivity.this);
            }
        });

        Button moreButton = findViewById(R.id.moreButton);
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SelectItemDialog moreDialog =new SelectItemDialog(BarTabActivity.this);
                Bundle bundle = new Bundle();
                bundle.putParcelableArray(RestClientService.ITEMS_RESULT_EXTRA, mItems.values().toArray(new Parcelable[]{}));
                moreDialog.show();
            }
        });

        mButtonTextSize = moreButton.getTextSize();
        moreButton.setHeight((int)(mButtonTextSize * buttonHeightFactor));
        doneButton.setHeight((int)(mButtonTextSize * buttonHeightFactor));
        payButton.setHeight((int)(mButtonTextSize * buttonHeightFactor));
        historyButton.setHeight((int)(mButtonTextSize * buttonHeightFactor));
        mTextSize = nameText.getTextSize();
        needToLoadTransactions = true;
        mIdleTimer = new CountDownTimer(30000, 1000) {

            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                finish();
            }
        }.start();
    }

    @Override
    public void onUserInteraction() {
        mIdleTimer.cancel();
        mIdleTimer.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        if (needToLoadTransactions) {
            loadTransactions();
        }
        super.onResume();
    }

    private ViewHolder createViewHolder(final Item item, int count) {
        ViewHolder vh = new ViewHolder();
        TableLayout tl = findViewById(R.id.tableLayout);
        TableRow row = new TableRow(this);
        row.setLayoutParams(new TableLayout.LayoutParams( TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
        Button button = new Button(this);
        button.setText(item.getType().toString());
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, mButtonTextSize);
        button.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBuyActivity(item);
            }
        });
        button.setHeight((int)(mButtonTextSize * buttonHeightFactor));
        vh.button = button;
        row.addView(button);
        TextView text = new TextView(this);
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
        text.setText(MessageFormat.format("{0} x ${1}", count, item.getCost()));
        text.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        text.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2));
        vh.text = text;
        row.addView(text);
        TextView subTotal = new TextView(this);
        subTotal.setText(MessageFormat.format("${0}", item.getCost() * count));
        subTotal.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
        subTotal.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
        subTotal.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        vh.subTotal = subTotal;
        row.addView(subTotal);
        updateViewHolder(vh, item, count);
        tl.addView(row, mTableRows++);
        return vh;
    }

    private void updateViewHolder(ViewHolder holder, Item item, int count) {
        holder.text.setText(MessageFormat.format("{0} x ${1}", count, item.getCost()));
        holder.subTotal.setText(MessageFormat.format("${0}", item.getCost() * count));
    }
    private static class BarTabLine {
        int count;
        int total;
    }

    private Map<ItemType, BarTabLine> calculateBarTab() {
        Map<ItemType, BarTabLine> tab = new HashMap<>();
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

    private int updateBarTabLine(ItemType type, Map<ItemType, BarTabLine> tab) {
        BarTabLine line = tab.get(type);
        if (line != null || defaultItems.contains(type)) {
            if (line == null) {
                line = new BarTabLine();
                line.count = 0;
                line.total = 0;
            }
            Item item = mItems.get(type);
            int index = type.ordinal();
            ViewHolder holder = mViewHolders[index];
            if (holder == null) {
                holder = createViewHolder(item, line.count);
                mViewHolders[index] = holder;
            }
            else {
                updateViewHolder(holder, item, line.count);
            }
            return line.total;
        }
        else {
            int index = type.ordinal();
            ViewHolder holder = mViewHolders[index];
            if (holder != null) {
                line = new BarTabLine();
                line.count = 0;
                line.total = 0;
                Item item = mItems.get(type);
                updateViewHolder(holder, item, line.count);
            }
        }
        return 0;
    }

    private void updateBarTab() {
        tab = calculateBarTab();
        int total = 0;
        for (ItemType type: ItemType.values()) {
            total += updateBarTabLine(type, tab);
        }
        TextView grandTotal = findViewById(R.id.textTotal);
        grandTotal.setText(MessageFormat.format("${0}", total));
    }

    public void startBuyActivity(Item item) {
        Intent in = new Intent(BarTabActivity.this, BuyActivity.class);
        in.putExtra(ITEM_EXTRA, item);
        in.putExtra(USER_EXTRA, mUser);
        startActivity(in);
        needToLoadTransactions = true;
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
        needToLoadTransactions = true;
    }

    private void startHistoryActivity() {
        Intent in = new Intent(BarTabActivity.this, HistoryActivity.class);
        in.putExtra(USER_EXTRA, mUser);
        in.putExtra(TRANSACTIONS_EXTRA, mTransactions.toArray(new Transaction[mTransactions.size()]));
        startActivity(in);
        needToLoadTransactions = true;
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

    public Map<ItemType, Item> makeItemMap(Parcelable[] items) {
        Map<ItemType, Item> itemMap = new HashMap<>();
        for (Parcelable p : items) {
            Item item = (Item) p;
            itemMap.put(item.getType(), item);
        }
        return itemMap;
    }

    public Map<ItemType, Item> getItems() {
        return mItems;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RestClientService.LOAD_ITEMS_REQUEST_CODE) switch (resultCode) {
            case RestClientService.INVALID_URL_CODE:
                RestClientService.showAlertDialog(this, "Invalid service URL loading items.");
                break;
            case RestClientService.ERROR_CODE:
                RestClientService.showAlertDialog(this, "Server error loading items.");
                break;
            case RestClientService.RESULT_OK_CODE:
                Parcelable[] items = data.getParcelableArrayExtra(RestClientService.ITEMS_RESULT_EXTRA);
                mItems = makeItemMap(items);
                break;
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
                    List<Transaction> transactionList = new ArrayList<>();
                    for (Parcelable p : transactions) {
                        Transaction transaction = (Transaction) p;
                        transaction.setUser(mUser.get_links().self.href);
                        transactionList.add(transaction);
                    }
                    mTransactions = transactionList;
                    updateBarTab();
                    needToLoadTransactions = false;
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
