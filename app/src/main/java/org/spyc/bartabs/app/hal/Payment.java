package org.spyc.bartabs.app.hal;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * User: mtali
 * Date: 10/09/14
 * Time: 20:30
 */
public class Payment implements Parcelable {

    public class PaymentLinks extends Links {
        Link user;
    }

    private String user;

    private Department department;

    private Integer amount;

    private PaymentMethod method;

    @Override
    public String toString() {
        return super.toString();
    }

    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd@HH:mm:ss.SSSZ")
    private Date date;

    private PaymentLinks _links;

    public Payment() {

    }

    public Payment(Department department, int amount, PaymentMethod method, User user, Date date) {
        this.department = department;
        this.amount = amount;
        this.method = method;
        this.date = date;
        this.user = user.get_links().self.href;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    private Payment(Parcel in) {
        amount = in.readInt();
        method = PaymentMethod.valueOf(in.readString());
        department = Department.valueOf(in.readString());
        date = new Date(in.readLong());
        String selfLink = in.readString();
        String userLink = in.readString();
        if (selfLink != null || userLink != null) {
            _links = new PaymentLinks();
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

    public PaymentLinks get_links() {
        return _links;
    }

    public void set_links(PaymentLinks _links) {
        this._links = _links;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int i) {
        out.writeInt(amount);
        out.writeString(method.toString());
        out.writeString(department.toString());
        out.writeLong(date.getTime());

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

    public static final Creator<Payment> CREATOR
            = new Creator<Payment>() {
        public Payment createFromParcel(Parcel in) {
            return new Payment(in);
        }

        public Payment[] newArray(int size) {
            return new Payment[size];
        }
    };
}
