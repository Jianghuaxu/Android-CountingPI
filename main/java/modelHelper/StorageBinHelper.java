package modelHelper;

import android.view.View;
import android.widget.ArrayAdapter;

import com.google.gson.Gson;
import com.journaldev.searchview.R;

import java.util.ArrayList;

import gson.PIItems;
import model.PIItemsUrlLoad;
import model.StorageBin;
import util.HttpUtil;

public class StorageBinHelper {
    //Suppose: only one WO exist.

    public static ArrayList<StorageBin> binArrayList = new ArrayList<>();

    private static int countedStorageBins = 0;

    public static void setBinArrayList(ArrayList<StorageBin> binList) {
        binArrayList = binList;
        countedStorageBins = 0;
    }

    public static boolean isOwnerSame (ArrayList<PIItems> items) {
        boolean isOwnerSameFlag = true;
        PIItems firstItem = items.get(0);
        for(PIItems item: items) {
            if(!item.Owner.equals(firstItem.Owner)) {
                isOwnerSameFlag = false;
                break;
            }
        }
        return isOwnerSameFlag;

    }

    public static void resetCountedStorageBins(){
        countedStorageBins = 0;
    }

    public static ArrayList<StorageBin> getBinArrayList() {
        return binArrayList;
    }

    public static StorageBin getDefaultStorageBin() {
        StorageBin defaultBin = null;
        for(StorageBin bin: binArrayList) {
            if(!bin.binCounted) {
                defaultBin = bin;
                break;
            }
        }
        if(defaultBin == null) {
            defaultBin = binArrayList.get(0);
        }
        return defaultBin;
    }

    public static StorageBin getNextStorageBin(StorageBin currentStorageBin) {
        // this method is called when previous Storage bin is counted already
        StorageBin nextBin = null;
        //update currentStorageBin and find next
        for(StorageBin bin: binArrayList) {
            if(bin.storageBin.equals(currentStorageBin.storageBin)) {
                StorageBin newBin = currentStorageBin;
                bin.binCounted = true;
                bin.piItemsInBin = newBin.piItemsInBin;
                if(nextBin != null) {
                    break;
                }
            } else {
                if(!bin.binCounted) {
                    //rule for getting next storage bin: next bin which is not counted yet, not considering the WO number
                    nextBin = bin;
                    break;
                }
            }
        }
        return nextBin;
    }

    public static StorageBin getNextStorageBinOfNextWO() {
        return null;
    }

    public static Boolean checkStorageBinsComplete() {
        for(StorageBin bin : binArrayList) {
            if(bin.binCounted) {
                return false;
            }
        }
        return true;
    }

    public static int getItemPosition(StorageBin bin) {
        //suppose that only one WO is existing in the arrayList binArrayList
        int itemPosition = 0;
        for(int i = 0; i < binArrayList.size(); i ++) {
            if(bin.storageBin.equals(binArrayList.get(i).storageBin)) {
                itemPosition = i + 1;
                break;
            }
        }
        return itemPosition;
    }

    public static int getNumberOfItems() {
        return binArrayList.size();
    }

    public static int getProgress(int max) {
        int newProgress;
        newProgress = max * countedStorageBins / binArrayList.size();
        return newProgress;
    }

    public static void updateProgress() {
        countedStorageBins = countedStorageBins + 1;
    }

    public static ArrayList<PIItemsUrlLoad> preparePutRequestLoad(String CountDate) {
        ArrayList<PIItemsUrlLoad> piItemsUrlLoadList = new ArrayList<>();
        //HUs and SubHUS would save all HUs & SubHUs of current Storage Bin that have status empty or missing
        ArrayList<String> HUs = new ArrayList<>();
        ArrayList<String> SubHUs = new ArrayList<>();
        /***
         * currently we only support two scenarios: bin empty case + HU Empty case + HU Missing case + input quanitty case
         */
        for(StorageBin bin: binArrayList) {
            if(bin.binEmpty) {
                PIItems item = bin.piItemsInBin.get(0);
                item.StorageBinEmpty = true;
                item.ProductQuantity = "0.000";
                PIItemsUrlLoad urlLoad = new PIItemsUrlLoad(item);
                piItemsUrlLoadList.add(urlLoad);
                //only first line
                continue;
            }
            //for not bin empty case, we check every entries
            for(PIItems item: bin.piItemsInBin) {
                if(item.Product.equals("")) {
                    item.ProductQuantity = "0.000";
                }
                //Handling Unit is missing/empty
                if(bin.HUEmpty.contains(item.HandlingUnit) || bin.HUMissing.contains(item.HandlingUnit)) {
                    if(HUs.contains(item.HandlingUnit)) {
                        continue;
                    }
                    HUs.add(item.HandlingUnit);
                    PIItemsUrlLoad urlLoad = new PIItemsUrlLoad(item);
                    piItemsUrlLoadList.add(urlLoad);
                    continue;
                }

                //SubHandling Unit is missing/empty
                if(bin.SubHUEmpty.contains(item.SubHandlingUnit) || bin.SubHUMissing.contains(item.SubHandlingUnit)) {
                    if(HUs.contains(item.SubHandlingUnit)) {
                        continue;
                    }
                    SubHUs.add(item.SubHandlingUnit);
                    PIItemsUrlLoad urlLoad = new PIItemsUrlLoad(item);
                    piItemsUrlLoadList.add(urlLoad);
                    continue;
                }

                //product quantity handling
                PIItemsUrlLoad urlLoad = new PIItemsUrlLoad(item);
                piItemsUrlLoadList.add(urlLoad);
            }
        }
        //add count user and count date in the end
        PIItemsUrlLoad piItemsUrlLoadFirst = piItemsUrlLoadList.get(0);
        PIItems itemFirst = new Gson().fromJson(piItemsUrlLoadFirst.requestBody, PIItems.class);
        itemFirst.CountUser = HttpUtil.getUserName().toUpperCase();
        itemFirst.CountDate = CountDate;
        piItemsUrlLoadFirst.requestBody = new Gson().toJson(itemFirst, PIItems.class);
        piItemsUrlLoadList.set(0, piItemsUrlLoadFirst);
        return piItemsUrlLoadList;
    }

//    public static StorageBin onHUStatusChanged(StorageBin bin, PIItems item, View v) {
//        String HU = item.HandlingUnit;
//        ArrayList<PIItems> itemList = bin.piItemsInBin;
//        String property = null;
//        switch(v.getId()) {
//            case R.id.hu_empty:
//                property = "HandlingUnitEmpty";
//                //if selected => add it to array bin.HUEmpty, if not remove it from array
//                if(bin.HUEmpty != null){
//                    if(bin.HUEmpty.contains(HU)) {
//                        break;
//                    }
//                }
//                bin.HUEmpty.add(HU);
//                if(bin.HUMissing != null){
//                    if(bin.HUMissing.contains(HU)) {
//                        bin.HUMissing.remove(HU);
//                    }
//                }
//                break;
//            case R.id.hu_missing:
//                property = "HandlingUnitMissing";
//                if(bin.HUMissing != null){
//                    if(bin.HUMissing.contains(HU)) {
//                        break;
//                    }
//                }
//                bin.HUMissing.add(HU);
//                if(bin.HUEmpty != null){
//                    if(bin.HUEmpty.contains(HU)) {
//                        bin.HUEmpty.remove(HU);
//                    }
//                }
//                break;
//        }
//        bin.piItemsInBin = setProperty(itemList, property, v.isSelected());
//        return bin;
//    }

    public static StorageBin onQuantityChange(StorageBin bin, int position, String quantity) {
        PIItems item = bin.piItemsInBin.get(position);
        item.ProductQuantity = quantity;
        bin.piItemsInBin.set(position, item);
        return bin;
    }

    public static StorageBin onAddQuantity (StorageBin bin, View v) {
        int position = (int) v.getTag();
        PIItems item = bin.piItemsInBin.get(position);
        if(item.ProductQuantity.equals("")) {
            item.ProductQuantity = "1";
        } else {
            item.ProductQuantity = String.valueOf(Integer.valueOf(item.ProductQuantity) + 1);
        }
        bin.piItemsInBin.set(position, item);
        return bin;
    }

    public static StorageBin onReduceQuantity (StorageBin bin, View v) throws Exception{
        int position = (int) v.getTag();
        PIItems item = bin.piItemsInBin.get(position);
        if(item.ProductQuantity.equals("") || item.ProductQuantity.equals("0")) {
            throw new Exception("Quantity should not be negative");
        } else {
            item.ProductQuantity = String.valueOf(Integer.valueOf(item.ProductQuantity) - 1);
        }
        bin.piItemsInBin.set(position, item);
        return bin;
    }

    private static ArrayList<PIItems> setProperty(ArrayList<PIItems> list, String property, boolean value) {
        if(property.equals("HandlingUnitEmpty")) {
            for(PIItems item: list) {
                item.HandlingUnitEmpty = value;
                item.HandlingUnitMissing = !value;
            }
        }
        if(property.equals("HandlingUnitMissing")) {
            for(PIItems item: list) {
                item.HandlingUnitMissing = value;
                item.HandlingUnitEmpty = !value;
            }
        }
        return list;
    }

}
