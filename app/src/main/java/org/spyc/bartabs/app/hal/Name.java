package org.spyc.bartabs.app.hal;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * User: mtali
 * Date: 10/09/14
 * Time: 20:30
 */
public class Name implements Parcelable {

    private Long id;

    private String value;

    private Links _links;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    private Name(Parcel in) {
        //id = in.readLong();
        value = in.readString();
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
        //out.writeLong(id);
        out.writeString(value);
    }

    public static final Parcelable.Creator<Name> CREATOR
            = new Parcelable.Creator<Name>() {
        public Name createFromParcel(Parcel in) {
            return new Name(in);
        }

        public Name[] newArray(int size) {
            return new Name[size];
        }
    };
}
