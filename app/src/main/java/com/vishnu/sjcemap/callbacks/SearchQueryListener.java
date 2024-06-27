package com.vishnu.sjcemap.callbacks;

public interface SearchQueryListener {
    void onSearchQuerySubmitted(String query);

    void onSearchQueryUpdated(String query);
}
