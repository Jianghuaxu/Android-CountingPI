package com.journaldev.searchview;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

public class PIDoc extends BaseObservable{
    private String piDocNumber;
    private int imageId;

    public PIDoc(String piDocNumber, int imageId){
        this.piDocNumber = piDocNumber;
        this.imageId  = imageId;
    }

    @Bindable
    public String getPIDocNumber(){
        return piDocNumber;
    }

    @Bindable
    public int getImageId(){
        return imageId;
    }

    public int getImageResourceById() {
        switch (imageId) {
            case 1:
                return R.drawable.pi_doc;
                default:
                    return R.drawable.pi_doc;
        }
    }

}