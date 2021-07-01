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
import com.android.billingclient.api.BillingClient.BillingResponseCode;
import com.android.billingclient.api.BillingClient.FeatureType;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchasesResult;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.shalzz.attendance.data.DataManager;
import com.shalzz.attendance.utils.RxUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

public class BillingManager implements PurchasesUpdatedListener {

    // Default value of mBillingClientResponseCode until BillingManager was not yet initialized
    public static final int BILLING_MANAGER_NOT_INITIALIZED  = -1;

    private enum SkuState {
        SKU_STATE_UNPURCHASED, SKU_STATE_PENDING, SKU_STATE_PURCHASED, SKU_STATE_PURCHASED_AND_ACKNOWLEDGED
    }

    private BillingClient mBillingClient;

    private final BillingUpdatesListener mBillingUpdatesListener;
    private BillingResult mBillingClientResult =
            BillingResult.newBuilder().setResponseCode(BILLING_MANAGER_NOT_INITIALIZED).build();

    private Set<ConsumeParams> mTokensToBeConsumed;
    private Map<String, SkuState> skuStateMap = new HashMap<>();
    private Map<String, SkuDetails> skuDetailsMap = new HashMap<>();

    private AppCompatActivity mActivity;
    private DataManager mDataManager;
    private CompositeDisposable mConnectionDisposable = new CompositeDisposable();
    private PublishSubject<List<Purchase>> publishSubject = PublishSubject.create();

    /**
     * Listener to the updates that happen when purchases list was updated or consumption of the
     * item was finished
     */
    public interface BillingUpdatesListener {
        void onBillingClientSetupFinished(BillingResult result);
        void onConsumeFinished(String token, @BillingResponseCode int result);
        void onPurchasesUpdated(List<Purchase> purchases);
    }

    public BillingManager(AppCompatActivity activity,
                          DataManager dataManager,
                          final BillingUpdatesListener updatesListener) {
        mActivity = activity;
        mDataManager = dataManager;
        mBillingUpdatesListener = updatesListener;
        mBillingClient = BillingClient.newBuilder(mActivity).setListener(this).build();

        mConnectionDisposable.add(
                observePurchasesUpdates().subscribe(mBillingUpdatesListener::onPurchasesUpdated,
                        Timber::e)
        );

        // Setup all listeners before establishing a connection.
        mConnectionDisposable.add(
                connect()
                        .doOnSubscribe(disposable -> querySkuDetailsAsync())
                        .subscribe(result -> {
                            Timber.d("First Connection. Response code: %d", result.getResponseCode());
                            if (result.getResponseCode() == BillingResponseCode.OK) {
                                // IAB is fully setup. Now get an inventory of stuff the user owns.
                                queryPurchases();
                            }
                        }));
    }

    private Observable<BillingResult> connect() {
        // Notify the listener that the billing client is ready.
        return Observable.create((ObservableOnSubscribe<BillingResult>) source -> {
            if (source.isDisposed()) return;
            if (mBillingClient.isReady()) {
                Timber.d("Client already connected. Response code: %d",
                        mBillingClientResult.getResponseCode());
                source.onNext(mBillingClientResult);
                source.onComplete();
            } else {
                mBillingClient.startConnection(new BillingClientStateListener() {
                    @Override
                    public void onBillingServiceDisconnected() {
                        source.tryOnError(new Throwable("Billing Client disconnected!"));
                        Timber.w("onBillingServiceDisconnected()");
                    }

                    @Override
                    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                        Timber.d("Setup finished. Response code: %d", billingResult.getResponseCode());
                        mBillingClientResult = billingResult;
                        source.onNext(billingResult);
                        source.onComplete();
                    }
                });
            }
        })
                .retry()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                // Notify the listener that the billing client is ready.
                .doOnNext(mBillingUpdatesListener::onBillingClientSetupFinished);
    }

    /**
     * Handle a callback that purchases were updated from the Billing library
     */
    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingResponseCode.OK) {
            publishSubject.onNext(purchases);
        } else if (billingResult.getResponseCode() == BillingResponseCode.USER_CANCELED) {
            Timber.i("onPurchasesUpdated() - user cancelled the purchase flow - skipping");
        } else {
            Timber.w("onPurchasesUpdated() got unknown resultCode: %d", billingResult.getResponseCode());
        }
    }

    public Observable<List<Purchase>> observePurchasesUpdates() {
        return publishSubject.concatMap(this::handlePurchases);
    }

    /**
     * Handles the purchase
     * <p>Note: Notice that for each purchase, we check if signature is valid.
     * </p>
     * @param purchases Purchases to be handled
     */
    private Observable<List<Purchase>> handlePurchases(List<Purchase> purchases) {
        return Observable.fromIterable(purchases)
                .flatMap(purchase ->
                        mDataManager.verifyValidSignature(purchase)
                                .filter(aBoolean -> aBoolean)
                                .map(aBoolean -> purchase)
                )
                .doOnNext(purchase -> Timber.d("Got a verified purchase: %s", purchase))
                .toList()
                .toObservable();
    }

    /**
     * Start a purchase or subscription replace flow
     */
    public void initiatePurchaseFlow(final String skuId) {
        SkuDetails sku = skuDetailsMap.get(skuId);
        Disposable disposable = connect()
                .subscribe(result -> {
                    if (result.getResponseCode() == BillingResponseCode.OK && sku != null) {
                        BillingFlowParams purchaseParams =
                                BillingFlowParams.newBuilder().setSkuDetails(sku).build();
                        mBillingClient.launchBillingFlow(mActivity, purchaseParams);
                    }
                });
        mConnectionDisposable.add(disposable);
    }

    /**
     * Returns the value Billing client response code or BILLING_MANAGER_NOT_INITIALIZED if the
     * client connection response was not received yet.
     */
    public int getBillingClientResponseCode() {
        return mBillingClientResult.getResponseCode();
    }

    /**
     * Checks if subscriptions are supported for current client
     * <p>Note: This method does not automatically retry for RESULT_SERVICE_DISCONNECTED.
     * It is only used in unit tests and after queryPurchases execution, which already has
     * a retry-mechanism implemented.
     * </p>
     */
    private boolean areSubscriptionsSupported() {
        int responseCode = mBillingClient.isFeatureSupported(FeatureType.SUBSCRIPTIONS).getResponseCode();
        if (responseCode != BillingResponseCode.OK) {
            Timber.w("areSubscriptionsSupported() got an error response: %d", responseCode);
        }
        return responseCode == BillingResponseCode.OK;
    }

    /**
     * Handle a result from querying of purchases and report an updated list to the listener
     */
    private void onQueryPurchasesFinished(PurchasesResult result) {
        // Have we been disposed of in the meantime? If so, or bad result code, then quit
        if (mBillingClient == null || result.getResponseCode() != BillingResponseCode.OK) {
            Timber.w("Billing client was null or result code (%d) was bad - quitting",
                    result.getResponseCode());
            return;
        }

        Timber.d("Query inventory was successful.");
        publishSubject.onNext(result.getPurchasesList());
    }

    /**
     * Receives the result from [.querySkuDetailsAsync]}.
     *
     * Store the SkuDetails and post them in the [.skuDetailsMap]. This allows other
     * parts of the app to use the [SkuDetails] to show SKU information and make purchases.
     */
    private void onSkuDetailsResponse(BillingResult billingResult,
                                      List<SkuDetails> skuDetailsList) {
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        switch (responseCode) {
            case BillingClient.BillingResponseCode.OK:
                Timber.i("onSkuDetailsResponse: %d %s",responseCode, debugMessage);
                if (skuDetailsList == null || skuDetailsList.isEmpty()) {
                    Timber.e(
                            "onSkuDetailsResponse: " +
                                    "Found null or empty SkuDetails. " +
                                    "Check to see if the SKUs you requested are correctly published " +
                                    "in the Google Play Console."
                    );
                } else {
                    for (SkuDetails skuDetails: skuDetailsList) {
                        String sku = skuDetails.getSku();
                        skuDetailsMap.put(sku, skuDetails);
                    }
                }
                break;
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
            case BillingClient.BillingResponseCode.ERROR:
                Timber.e("onSkuDetailsResponse: %d %s",responseCode, debugMessage);
                break;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                Timber.i("onSkuDetailsResponse: %d %s",responseCode, debugMessage);
                break;
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
                Timber.wtf("onSkuDetailsResponse: %d %s",responseCode, debugMessage);
                break;
            default:
                Timber.wtf("onSkuDetailsResponse: %d %s",responseCode, debugMessage);
        }
//        if (responseCode == BillingClient.BillingResponseCode.OK) {
//            skuDetailsResponseTime = SystemClock.elapsedRealtime();
//        } else {
//            skuDetailsResponseTime = -SKU_DETAILS_REQUERY_TIME
//        }
    }

    /**
     * Query purchases across various use cases and deliver the result in a formalized way through
     * a listener
     */
    public void queryPurchases() {
        Disposable disposable = connect()
                .subscribe(result -> {
                    if (result.getResponseCode() == BillingResponseCode.OK) {
                        long time = System.currentTimeMillis();
                        PurchasesResult purchasesResult = mBillingClient.queryPurchases(SkuType.INAPP);
                        Timber.i("Querying purchases elapsed time: %s ms",
                                (System.currentTimeMillis() - time));
                        // If there are subscriptions supported, we add subscription rows as well
                        if (areSubscriptionsSupported()) {
                            PurchasesResult subscriptionResult
                                    = mBillingClient.queryPurchases(SkuType.SUBS);

                            if (subscriptionResult.getPurchasesList() != null && subscriptionResult.getResponseCode()
                                    == BillingResponseCode.OK) {
                                purchasesResult.getPurchasesList().addAll(
                                        subscriptionResult.getPurchasesList());

                                Timber.i("Querying purchases and subscriptions elapsed time: %s ms",
                                        (System.currentTimeMillis() - time));
                                Timber.i( "Querying subscriptions result code: %d res: %d"
                                        , subscriptionResult.getResponseCode()
                                        ,  subscriptionResult.getPurchasesList().size());
                            } else {
                                Timber.e( "Got an error response trying to query subscription purchases");
                            }
                        } else if (purchasesResult.getResponseCode() == BillingResponseCode.OK) {
                            Timber.i("Skipped subscription purchases query since they are not supported");
                        } else {
                            Timber.w("queryPurchases() got an error response code: %s"
                                    , purchasesResult.getResponseCode());
                        }
                        onQueryPurchasesFinished(purchasesResult);
                    }
                });
        mConnectionDisposable.add(disposable);
    }

    public void querySkuDetailsAsync() {
        Disposable disposable = connect()
                .subscribe(result -> {
                    if (!BillingConstants.getSkuList(SkuType.INAPP).isEmpty()) {
                        SkuDetailsParams params =
                                SkuDetailsParams.newBuilder()
                                        .setSkusList(BillingConstants.getSkuList(SkuType.INAPP))
                                        .setType(SkuType.INAPP)
                                        .build();
                        mBillingClient.querySkuDetailsAsync(params, this::onSkuDetailsResponse);
                    }
                    if (!BillingConstants.getSkuList(SkuType.SUBS).isEmpty()) {
                        SkuDetailsParams params =
                                SkuDetailsParams.newBuilder()
                                        .setSkusList(BillingConstants.getSkuList(SkuType.SUBS))
                                        .setType(SkuType.SUBS)
                                        .build();
                        mBillingClient.querySkuDetailsAsync(params, this::onSkuDetailsResponse);
                    }
                });
        mConnectionDisposable.add(disposable);
    }

    public void consumeAsync(final ConsumeParams purchaseToken) {
        // If we've already scheduled to consume this regId - no action is needed (this could happen
        // if you received the regId when querying purchases inside onReceive() and later from
        // onActivityResult()
        if (mTokensToBeConsumed == null) {
            mTokensToBeConsumed = new HashSet<>();
        } else if (mTokensToBeConsumed.contains(purchaseToken)) {
            Timber.i("Token was already scheduled to be consumed - skipping...");
            return;
        }
        mTokensToBeConsumed.add(purchaseToken);

        final ConsumeResponseListener onConsumeListener =
                (responseCode, purchaseToken1) ->
                        mBillingUpdatesListener.onConsumeFinished(purchaseToken1, responseCode.getResponseCode());

        Disposable disposable = connect()
                .subscribe(result -> {
                    if (result.getResponseCode() == BillingResponseCode.OK) {
                        mBillingClient.consumeAsync(purchaseToken, onConsumeListener);
                    }
                });
        mConnectionDisposable.add(disposable);
    }

    /**
     * Clear the resources
     */
    public void destroy() {
        Timber.d( "Destroying the manager.");

        RxUtil.dispose(mConnectionDisposable);
        if (mBillingClient != null && mBillingClient.isReady()) {
            mBillingClient.endConnection();
            mBillingClient = null;
        }
    }
}
