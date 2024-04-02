package com.vishnu.sjce_map.miscellaneous;

public interface SearchQueryListener {
    void onSearchQuerySubmitted(String query);

    void onSearchQueryUpdated(String query);
}
