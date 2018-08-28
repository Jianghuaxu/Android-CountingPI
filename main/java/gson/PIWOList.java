package gson;

import java.util.Date;
import java.util.UUID;

public class PIWOList {
    public boolean CompleteHandlingUnitActiveIndicator;
    public String IsProductBased;//should be boolean?
    public String EndDate;// null value
    public String StartDate;
    public boolean LaborManagementActiveIndicator;
    public boolean ProposeQuantityIndicator;
    public UUID PhysicalInventoryDocumentGUID;
    public String Warehouse;
    public String PhysicalInventoryDocumentNumber;
    public String WarehouseOrder;
    public Number NumberOfItems;
    public String CountUser;
    public String CountDate;
    public String WarehouseOrderStatus;
    public boolean ChangeCountIndicator;
    public WhoToPItemDynamicFields WhoToPIItemDynamicFields;

}
