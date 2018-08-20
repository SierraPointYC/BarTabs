package org.spyc.bartabs.app.hal;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

/**
 * User: mtali
 * Date: 10/09/14
 * Time: 20:30
 */
public class Transaction implements Parcelable {

    DateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm a");

    @Override
    public String toString() {
        return format.format(openDate) +
                " " + items +
                " " + item +
                " $" + amount;
    }

    public class TransactionLinks extends Links {
        public Link user;
    }

    public enum Status {
        UNPAID, PAID, CANCELLED
    }

    private Integer items;

    private String user;

    private Integer amount;

    private ItemType item;

    private Department department;

    private Status status;

    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd@HH:mm:ss.SSSZ")
    private Date openDate;

    private Date closeDate;

    private TransactionLinks _links;

    public Transaction() {

    }

    public Transaction (Item item, int count, User user, Date openDate) {
        this.item = item.getType();
        this.department = item.getDepartment();
        this.items = count;
        this.amount = count * item.getCost();
        this.openDate = openDate;
        this.status = Status.UNPAID;
        this.user = user.get_links().self.href;
    }

    public Integer getItems() {
        return items;
    }

    public void setItems(Integer items) {
        this.items = items;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public ItemType getItem() {
        return item;
    }

    public void setItem(ItemType item) {
        this.item = item;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Date getOpenDate() {
        return openDate;
    }

    public void setOpenDate(Date openDate) {
        this.openDate = openDate;
    }

    public Date getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(Date closeDate) {
        this.closeDate = closeDate;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    private Transaction(Parcel in) {
        items = in.readInt();
        amount = in.readInt();
        item = ItemType.valueOf(in.readString());
        department = Department.valueOf(in.readString());
        status = Status.valueOf(in.readString());
        openDate = new Date(in.readLong());
        long closeTime = in.readLong();
        if (closeTime > 0){
            closeDate = new Date(closeTime);
        }
        String selfLink = in.readString();
        String userLink = in.readString();
        if (selfLink != null || userLink != null) {
            _links = new TransactionLinks();
            if (selfLink != null) {
                _links.self = new Link();
                _links.self.href = selfLink;
            }
            if (userLink != null) {
                _links.user = new Link();
                _links.user.href = userLink;
            }
        }
        user = in.readString();
    }

    public TransactionLinks get_links() {
        return _links;
    }

    public void set_links(TransactionLinks _links) {
        this._links = _links;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int i) {
        out.writeInt(items);
        out.writeInt(amount);
        out.writeString(item.toString());
        out.writeString(department.toString());
        out.writeString(status.toString());
        out.writeLong(openDate.getTime());
        if (closeDate != null){
            out.writeLong(closeDate.getTime());
        }
        else {
            out.writeLong(0);
        }
        if (_links != null) {
            if (_links.self != null) {
                out.writeString(_links.self.href);
            }
            else {
                out.writeString(null);
            }
            if (_links.user != null) {
                out.writeString(_links.user.href);
            }
            else {
                out.writeString(null);
            }
        }
        else {
            out.writeString(null);
            out.writeString(null);
        }
        out.writeString(user);
    }

    public static final Creator<Transaction> CREATOR
            = new Creator<Transaction>() {
        public Transaction createFromParcel(Parcel in) {
            return new Transaction(in);
        }

        public Transaction[] newArray(int size) {
            return new Transaction[size];
        }
    };
}
