package org.spyc.bartabs.app;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.GridLayout;

import org.spyc.bartabs.app.hal.Item;
import org.spyc.bartabs.app.hal.ItemType;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;

public class SelectItemDialog extends Dialog {
    private BarTabActivity activity;


    SelectItemDialog(Activity a) {
        super(a);
        this.activity = (BarTabActivity)a;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_select_item);
        GridLayout layout = findViewById(R.id.select_item_layout);
        Button backButton = findViewById(R.id.select_item_back_button);
        Map<ItemType, Item> items = activity.getItems();
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        float buttonTextSize = backButton.getTextSize();
        backButton.setHeight((int)buttonTextSize * 3);
        Context context = backButton.getContext();
        ArrayList<Button> buttons = new ArrayList<>(ItemType.values().length);
        int maxWidth = 0;
        for (ItemType t: ItemType.values()) {
            final Item item = items.get(t);
            if (item == null) {
                continue;
            }
            Button button = new Button(context);
            button.setText(MessageFormat.format("{0} (${1})", t.toString(), item.getCost()));
            button.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonTextSize);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.startBuyActivity(item);
                    dismiss();
                }
            });
            buttons.add(button);
            int width = button.getText().length();
            maxWidth = (width > maxWidth) ? width: maxWidth;
        }
        for (Button button: buttons) {
            button.setWidth((int)(maxWidth * buttonTextSize * 0.6));
            button.setHeight((int)buttonTextSize * 3);
            layout.addView(button);
        }
    }
}
