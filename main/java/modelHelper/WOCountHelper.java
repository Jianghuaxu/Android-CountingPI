package modelHelper;

import java.util.ArrayList;

import model.WarehouseOrderCount;

public class WOCountHelper {
    private static ArrayList<WarehouseOrderCount> warehouseOrderCountArrayList;

    private static boolean isGuidedMode;

    public static void setIsGuidedMode(boolean isGuidedMode) {
        WOCountHelper.isGuidedMode = isGuidedMode;
    }

    public static boolean getIsGuidedMode() {
        return isGuidedMode;
    }

    public static void setWarehouseOrderForCounting(ArrayList<WarehouseOrderCount> listData) {
        warehouseOrderCountArrayList = listData;
    }

    public static WarehouseOrderCount getNextWarehouseOrder() {
        WarehouseOrderCount woCount = null;
        if(warehouseOrderCountArrayList.size() > 0) {
            woCount = warehouseOrderCountArrayList.get(0);
            warehouseOrderCountArrayList.remove(0);
        }
        return woCount;
    }
}
