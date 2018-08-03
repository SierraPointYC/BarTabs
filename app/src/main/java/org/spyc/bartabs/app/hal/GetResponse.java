package org.spyc.bartabs.app.hal;



public class GetResponse {

    public static class ResponseLinks extends Links {
        public Link profile;
        public Link search;
    }
    public static class Embedded {
        public Name[] name;
        public User[] user;
        public Item[] item;
        public Transaction[] transaction;
    }

    public static class Page {
        public int size;
        public int totalElements;
        public int totalPages;
        public int number;
    }

    public Embedded get_embedded() {
        return _embedded;
    }

    public void set_embedded(Embedded _embedded) {
        this._embedded = _embedded;
    }

    public Links get_links() {
        return _links;
    }

    public void set_links(Links _links) {
        this._links = _links;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    private Embedded _embedded;

    private Links _links;

    private Page page;


}
