package org.spyc.bartabs.app.hal;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * User: mtali
 * Date: 10/09/14
 * Time: 20:30
 */
public class Item implements Parcelable {

    private ItemType type;

    private Integer cost;

    private Department department;

    private Links _links;

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Links get_links() {
        return _links;
    }

    public void set_links(Links _links) {
        this._links = _links;
    }

    public Item() {

    }

    private Item(Parcel in) {
        type = ItemType.valueOf(in.readString());
        cost = in.readInt();
        department = Department.valueOf(in.readString());
        _links = new Links();
        _links.self = new Link();
        _links.self.href = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int i) {
        out.writeString(type.toString());
        out.writeInt(cost);
        out.writeString(department.toString());
        out.writeString(_links.self.href);
    }

    public static final Creator<Item> CREATOR
            = new Creator<Item>() {
        public Item createFromParcel(Parcel in) {
            return new Item(in);
        }

        public Item[] newArray(int size) {
            return new Item[size];
        }
    };
}
