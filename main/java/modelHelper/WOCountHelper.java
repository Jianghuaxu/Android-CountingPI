package modelHelper;

import java.util.ArrayList;

import model.WarehouseOrderCount;

public class WOCountHelper {
    private static ArrayList<WarehouseOrderCount> woList;

    public static void setWOList(ArrayList<WarehouseOrderCount> listData) {
        woList = listData;
    }
}
