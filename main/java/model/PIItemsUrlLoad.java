package model;

import com.google.gson.Gson;

import gson.PIItems;

public class PIItemsUrlLoad {
    public String url;
    public String requestBody;

    public PIItemsUrlLoad(PIItems item) {
        url = item.metaData.uri;
        requestBody = new Gson().toJson(item, PIItems.class);
    }
}
