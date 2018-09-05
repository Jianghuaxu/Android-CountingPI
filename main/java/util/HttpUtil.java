package util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.journaldev.searchview.LogonActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;

import gson.PIHeaders;
import model.WarehouseOrderCount;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpUtil {

    private static String server_client_name = "?sap-client=210";
    private static String wareHouseNumber;
    private static OkHttpClient client;
    private static String hostName;
    private static String username;
    private static String credential;

    //token is retrieved in the beginning during logon process, and be saved waiting for using for put action, will be updated when response error
    private static String token;

    public static void setClient(OkHttpClient okHttpClient) {
        client = okHttpClient;
    }

    public static void setToken(String newToken) {
        token = newToken;
    }

    public static void setCredential(String userName, String password) {
        username = userName;
        credential = Credentials.basic(userName, password);
    }

    public static void setWareHouseNumber(String whn) {
        wareHouseNumber = whn;
    }

    public static String getUserName() {
        return username;
    }

    public static void sendOkHttpRequestLogon(okhttp3.Callback callback) {
        Request.Builder builder =  new Request.Builder();
        builder = addCredential(builder);
        builder.header("Content-Type", "application/atom+xml");
        builder.header("x-csrf-token", "fetch");
        Request request = builder.url(hostName).build();
        client.newCall(request).enqueue(callback);
    }

    public static void sendOkHttpRequest(String url, okhttp3.Callback callback) {
        Request.Builder builder =  new Request.Builder();
        builder = addCredential(builder);
        Request request = builder.url(url).build();
        client.newCall(request).enqueue(callback);
    }

    public static void saveOkHttpPostRequest(String postUrl, String postLoad, okhttp3.Callback callback) {
        Request.Builder builder =  new Request.Builder();
        builder = addCredential(builder);
        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        builder.header("x-csrf-token", token);
        builder.header("X-Requested-With", "XMLHttpRequest");
        builder.header("Content-Type","application/json");
        builder.method("PUT", RequestBody.create(JSON, postLoad));
        Request request = builder.url(postUrl).build();
        client.newCall(request).enqueue(callback);
    }

    private static Request.Builder addCredential(Request.Builder builder) {
        builder.addHeader("Authorization", credential);
        return builder;
    }

    public static void setHostName(String server_name) {
        hostName = server_name;
    }

    public static String getPIListUrl(String woNumber, String storageType, String aisle) {
        String piListUrl;
        String filter_storageType = "";
        String filter_aisle = "";
        String filter_woNumber = "";
        String entityProperties = "PIHeaderSet?sap-client=210";
        if(!storageType.equals("")) {
            filter_storageType = "%20and%20(StorageType%20eq%20%27" + storageType + "%27)";
        }
        if(!aisle.equals("")) {
            filter_aisle = "%20and%20(Aisle%20eq%20%27" + aisle + "%27)";
        }
        if(!woNumber.equals("")) {
            filter_woNumber = "%20and%20(WarehouseOrder%20eq%20%27" + woNumber + "%27)";
        }
        String filtersMadotary = "&$filter=(Warehouse%20eq%20%27" + wareHouseNumber + "%27%20and%20(PhysicalInventoryDocumentYear%20eq%20%272018%27)" + filter_aisle + filter_storageType + filter_woNumber + "%20and%20ActivationStatus%20" +
                "eq%20%271%27%20and%20CountStatus%20eq%20%273%27)";
        String select = "&$select=PhysicalInventoryDocumentNumber%2cPhysicalInventoryDocumentGUID";
        piListUrl = hostName + entityProperties + filtersMadotary + select  + "&$format=json";
        return piListUrl;
    }

    public static String getPIItemURL(String piDocUuid, String woNumber) {
        String entityProperties = "&$skip=0&$top=1000&$select=StorageBin%2cStorageBinEmpty%2cHandlingUnit%2cHandlingUnitComplete%2cHandlingUnitEmpty%2cHandlingUnitMissing%2cProduct%2cProductDescription%2cBatch%2cProductQuantity%2cProductQuantityUoM%2cPhysicalInventoryItemStatusDescription%2cParentNodeGUID%2cItemNodeGUID%2cSubHandlingUnitComplete%2cPhysicalInventoryItemNumber%2cSubHandlingUnitEmpty%2cPhysicalInventoryItemStatus%2cSubHandlingUnitMissing%2cPhysicalInventoryItemCategory%2cSubHandlingUnit%2cBookQuantity%2cBookQuantityUoM%2cStockType%2cStockTypeDescription%2cOwner%2cOwnerDescription%2cPartyEntitledToDispose%2cPartyEntitledToDisposeDescription%2cUsage%2cUsageDescription%2cSpecialStockType%2cSpecialStockTypeDescription%2cSpecialStockExternalNumber%2cSpecialStockItemNumber%2cShelfLifeExpirationDate%2cGoodsReceiptDate%2cCountryOfOrigin%2cCountry";
        String navigation_property = "/PIWOSet(PhysicalInventoryDocumentGUID=guid'" + piDocUuid + "',WarehouseOrder='" + woNumber + "',Warehouse='" + wareHouseNumber + "')/WhoToPIItemList";
        return hostName + navigation_property + server_client_name + entityProperties + "&$format=json";
    }

    public static String getWOListUrl(String PIDocNumber){
        String expandDynamicFields = "&$expand=WhoToPIItemDynamicFields";
        String entitiySet = "PIWOSet";
        String filterProperties = "&$filter=Warehouse%20eq%20%27" + wareHouseNumber + "%27%20and%20PhysicalInventoryDocumentNumber%20eq%20%27" + PIDocNumber + "%27";
        return hostName + entitiySet + server_client_name + expandDynamicFields + filterProperties;
    }

    public static void afterRequestFailed(Context context){
        Toast.makeText(context, "Logon failed", Toast.LENGTH_SHORT).show();
    }

}
