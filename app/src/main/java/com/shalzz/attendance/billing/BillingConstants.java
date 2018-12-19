/*
 * Copyright (c) 2013-2018 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of College Academics.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.shalzz.attendance.billing;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.SkuType;

import java.util.Arrays;
import java.util.List;

/**
 * Static fields and methods useful for billing
 */
public final class BillingConstants {
    // SKUs for our products: the premium upgrade (non-consumable) and gas (consumable)
    public static final String SKU_PRO_KEY = "pro_key";
    public static final String SKU_DONATION_200 = "donation_200";
    public static final String SKU_DONATION_250 = "donation_250";

    // SKU for our subscription

    private static final String[] IN_APP_SKUS = {SKU_PRO_KEY, SKU_DONATION_200, SKU_DONATION_250};
    private static final String[] SUBSCRIPTIONS_SKUS = {};

    private BillingConstants(){}

    /**
     * Returns the list of all SKUs for the billing type specified
     */
    public static final List<String> getSkuList(@BillingClient.SkuType String billingType) {
        return (billingType == SkuType.INAPP) ? Arrays.asList(IN_APP_SKUS)
                : Arrays.asList(SUBSCRIPTIONS_SKUS);
    }
}

