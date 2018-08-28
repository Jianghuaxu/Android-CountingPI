package model;

import java.util.ArrayList;

import gson.PIItems;

public class StorageBin {
    public boolean binCounted;
    public ArrayList<PIItems> piItemsInBin;
    public String storageBin;
    public boolean binEmpty;
    public ArrayList<String> HUMissing = new ArrayList<>();
    public ArrayList<String> HUEmpty = new ArrayList<>();
    public ArrayList<String> SubHUMissing = new ArrayList<>();
    public ArrayList<String> SubHUEmpty = new ArrayList<>();

}
