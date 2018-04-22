package com.shalzz.attendance.event;

import com.android.billingclient.api.Purchase;

import java.util.List;

public class PurchaseEvent {
    private List<Purchase> mPurchaseList;

    public PurchaseEvent(List<Purchase> purchaseList) {
        mPurchaseList = purchaseList;
    }

    public List<Purchase> getPurchases() {
        return mPurchaseList;
    }
}
