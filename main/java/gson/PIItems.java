package gson;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.UUID;

public class PIItems {
    public String PhysicalInventoryItemStatusDescription;
    public String StockTypeDescription;
    public String OwnerDescription;
    public String PartyEntitledToDisposeDescription;
    public String Country;
    public String SpecialStockType;
    public String UsageDescription;
    public String SpecialStockExternalNumber;
    public String ShelfLifeExpirationDate;
    public String SpecialStockTypeDescription;
    public String SubHandlingUnit;
    public boolean HandlingUnitComplete;
    public String SpecialStockItemNumber;
    public String Usage;
    public boolean SubHandlingUnitComplete;
    public boolean SubHandlingUnitEmpty;
    public boolean SubHandlingUnitMissing;
    public boolean HandlingUnitEmpty;
    public boolean HandlingUnitMissing;
    public String PhysicalInventoryItemNumber;
    public String PhysicalInventoryItemStatus;
    public UUID ItemNodeGUID;
    public String PhysicalInventoryItemCategory;
    public UUID ParentNodeGUID;
    public String StorageBin;
    public boolean StorageBinEmpty;
    public String HandlingUnit;
    public String Product;
    public String ProductDescription;
    public String ProductQuantity;
    public String ProductQuantityUoM;
    public String BookQuantity;
    public String BookQuantityUoM;
    public String StockType;
    public String Batch;
    public String Owner;
    public String PartyEntitledToDispose;
    public String GoodsReceiptDate;
    public String CountryOfOrigin;

    @SerializedName("__metadata")
    public MetaData metaData;

    //for save
    public String CountDate;
    public String CountUser;
}
