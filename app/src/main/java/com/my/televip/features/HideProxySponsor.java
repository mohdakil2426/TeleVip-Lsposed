package com.my.televip.features;

import com.my.televip.virtuals.messenger.MessagesController;

public class HideProxySponsor {

    public static boolean isPromoDataRequest(Object object){
        return object.getClass().getName().contains("TL_help_getPromoData");
    }

    public static void removePromoDialog() {
        if (MessagesController.getGlobalMainSettings() == null) return;
        MessagesController.getGlobalMainSettings().edit().remove("proxy_dialog").remove("proxyDialogAddress").remove("nextPromoInfoCheckTime").apply();
    }

}
