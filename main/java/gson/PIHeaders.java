package gson;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.UUID;

public class PIHeaders {
    public UUID PhysicalInventoryDocumentGUID;
    public String PhysicalInventoryDocumentNumber;
//    public String NumberOfWarehouseOrder;
//    public String NumberOfCountedItems;
//    public String NumberOfTotalItems;
//    public String PrintStatus;
//    public String ActivationStatus;
//    public Date CreationDate;
//    public String Warehouse;
//    public String OutputControlMode;
    @SerializedName("__metadata")
    public MetaData metaData;
}
