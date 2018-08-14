package org.spyc.bartabs.app;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Activity;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.spyc.bartabs.app.hal.Transaction;

public class HistoryActivity extends Activity {

    private Transaction[] mTransactions;
    TransactionAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        Parcelable[] array = getIntent().getParcelableArrayExtra(BarTabActivity.TRANSACTIONS_EXTRA);
        mTransactions = new Transaction[array.length];
        for (int i = 0; i < array.length; i++) {
            if (array[i] instanceof Transaction) {
                mTransactions[i] = (Transaction) array[i];
            }
        }
        RecyclerView recyclerView = findViewById(R.id.historyListView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        //mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        mAdapter = new TransactionAdapter(mTransactions, recyclerView);
        recyclerView.setAdapter(mAdapter);

        Button backButton = findViewById(R.id.historyBackButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Button revertButton = findViewById(R.id.historyRevertButton);
        revertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                revertTransaction();
            }
        });


    }

    private void revertTransaction() {
        int position = mAdapter.getSelectedPosition();
        if (position >= 0) {
            Transaction transaction =  mTransactions[position];
            Toast.makeText(this, "Revert  row: " + transaction, Toast.LENGTH_LONG).show();
            PendingIntent pendingResult = createPendingResult(
                    RestClientService.CANCEL_TRANSACTION_REQUEST_CODE, new Intent(), 0);
            Intent intent = new Intent(getApplicationContext(), RestClientService.class);
            intent.putExtra(RestClientService.REQUEST_CODE_EXTRA, RestClientService.CANCEL_TRANSACTION_REQUEST_CODE);
            intent.putExtra(RestClientService.TRANSACTION_EXTRA, transaction);
            intent.putExtra(RestClientService.PENDING_RESULT_EXTRA, pendingResult);
            startService(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RestClientService.CANCEL_TRANSACTION_REQUEST_CODE) {
            switch (resultCode) {
                case RestClientService.INVALID_URL_CODE:
                    RestClientService.showAlertDialog(this, "Invalid service URL for reverting transaction.");
                    break;
                case RestClientService.ERROR_CODE:
                    RestClientService.showAlertDialog(this, "Server error reverting transaction.");
                    break;
                case RestClientService.RESULT_OK_CODE:
                    finish();
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
        private RecyclerView mRecyclerView;
        private Transaction[] mDataset;
        private int mSelectedPosition = -1;
        private View mSelectedView;
        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        static class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            private LinearLayout mLayout;
            private TextView mView;
            ViewHolder(LinearLayout v) {
                super(v);
                mLayout = v;
                mView = mLayout.findViewById(R.id.history_line_text);
                mLayout.setClickable(true);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        TransactionAdapter(Transaction[] myDataset, RecyclerView recyclerView) {
            mDataset = myDataset;
            mRecyclerView = recyclerView;
        }

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public TransactionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                                int viewType) {
            // create a new view
            LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.layout_history_line, parent, false);
            ViewHolder vh = new ViewHolder(v);
            // Set a listener for this entire view
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = mRecyclerView.getChildAdapterPosition(v);
                    if (mSelectedPosition != position) {
                        v.setBackgroundColor(Color.GRAY);
                        if (mSelectedView != null) {
                            mSelectedView.setBackgroundColor(Color.WHITE);
                        }
                        mSelectedPosition = position;
                        mSelectedView = v;
                    }

                }
            });
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            holder.mView.setText(mDataset[position].toString());
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.length;
        }

        private int getSelectedPosition() {
            return mSelectedPosition;
        }
    }
}
