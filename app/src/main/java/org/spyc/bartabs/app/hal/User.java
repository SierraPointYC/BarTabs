package org.spyc.bartabs.app.hal;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * User: mtali
 * Date: 10/09/14
 * Time: 20:30
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements Parcelable {

    private String name;

    private String pin;

    private String tag;

    private Links _links;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public User() {

    }

    private User(Parcel in) {
        name = in.readString();
        pin = in.readString();
        tag = in.readString();
        _links = new Links();
        _links.self = new Link();
        _links.self.href = in.readString();
    }

    public Links get_links() {
        return _links;
    }

    public void set_links(Links _links) {
        this._links = _links;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int i) {
        out.writeString(name);
        out.writeString(pin);
        out.writeString(tag);
        out.writeString(_links.self.href);
    }

    public static final Creator<User> CREATOR
            = new Creator<User>() {
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };
}
