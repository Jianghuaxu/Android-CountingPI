package util;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;

public class Util {
    public static void showProgressDialog(ProgressDialog dialog, boolean flag) {
        if(flag) {
            if(!dialog.isShowing()) {
                dialog.show();
                return;
            }
        }else {
            if(dialog.isShowing()) {
                dialog.hide();
            }
        }

    }

}
