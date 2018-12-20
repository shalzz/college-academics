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
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClient.FeatureType;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchasesResult;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.shalzz.attendance.data.DataManager;
import com.shalzz.attendance.utils.RxUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private BillingClient mBillingClient;

    private final BillingUpdatesListener mBillingUpdatesListener;
    private int mBillingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED;

    private Set<String> mTokensToBeConsumed;

    private AppCompatActivity mActivity;
    private DataManager mDataManager;
    private CompositeDisposable mConnectionDisposable = new CompositeDisposable();
    private PublishSubject<List<Purchase>> publishSubject = PublishSubject.create();

    /**
     * Listener to the updates that happen when purchases list was updated or consumption of the
     * item was finished
     */
    public interface BillingUpdatesListener {
        void onBillingClientSetupFinished(int code);
        void onConsumeFinished(String token, @BillingResponse int result);
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
                        .subscribe(code -> {
                            Timber.d("First Connection. Response code: %d", code);
                            if (code == BillingResponse.OK) {
                                // IAB is fully setup. Now get an inventory of stuff the user owns.
                                queryPurchases();
                            }
                        }));
    }

    private Observable<Integer> connect() {
        // Notify the listener that the billing client is ready.
        return Observable.create((ObservableOnSubscribe<Integer>) source -> {
            if (source.isDisposed()) return;
            if (mBillingClient.isReady()) {
                Timber.d("Client already connected. Response code: %d",
                        mBillingClientResponseCode);
                source.onNext(mBillingClientResponseCode);
                source.onComplete();
            } else {
                mBillingClient.startConnection(new BillingClientStateListener() {
                    @Override
                    public void onBillingSetupFinished(@BillingResponse int billingResponseCode) {
                        Timber.d("Setup finished. Response code: %d", billingResponseCode);
                        mBillingClientResponseCode = billingResponseCode;
                        source.onNext(mBillingClientResponseCode);
                        source.onComplete();
                    }

                    @Override
                    public void onBillingServiceDisconnected() {
                        source.tryOnError(new Throwable("Billing Client disconnected!"));
                        Timber.w("onBillingServiceDisconnected()");
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
    public void onPurchasesUpdated(int resultCode, List<Purchase> purchases) {
        if (resultCode == BillingResponse.OK) {
            publishSubject.onNext(purchases);
        } else if (resultCode == BillingResponse.USER_CANCELED) {
            Timber.i("onPurchasesUpdated() - user cancelled the purchase flow - skipping");
        } else {
            Timber.w("onPurchasesUpdated() got unknown resultCode: %d", resultCode);
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
     * Start a purchase flow
     */
    public void initiatePurchaseFlow(final String skuId, final @SkuType String billingType) {
        initiatePurchaseFlow(skuId, null, billingType);
    }

    /**
     * Start a purchase or subscription replace flow
     */
    public void initiatePurchaseFlow(final String skuId, final ArrayList<String> oldSkus,
                                     final @SkuType String billingType) {

        Disposable disposable = connect()
                .subscribe(code -> {
                    if (code == BillingResponse.OK) {
                        Timber.d("Launching in-app purchase flow. Replace old SKU? %s",
                                (oldSkus != null));
                        BillingFlowParams purchaseParams = BillingFlowParams.newBuilder()
                                .setSku(skuId).setType(billingType).setOldSkus(oldSkus).build();
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
        return mBillingClientResponseCode;
    }

    /**
     * Checks if subscriptions are supported for current client
     * <p>Note: This method does not automatically retry for RESULT_SERVICE_DISCONNECTED.
     * It is only used in unit tests and after queryPurchases execution, which already has
     * a retry-mechanism implemented.
     * </p>
     */
    private boolean areSubscriptionsSupported() {
        int responseCode = mBillingClient.isFeatureSupported(FeatureType.SUBSCRIPTIONS);
        if (responseCode != BillingResponse.OK) {
            Timber.w("areSubscriptionsSupported() got an error response: %d", responseCode);
        }
        return responseCode == BillingResponse.OK;
    }

    /**
     * Handle a result from querying of purchases and report an updated list to the listener
     */
    private void onQueryPurchasesFinished(PurchasesResult result) {
        // Have we been disposed of in the meantime? If so, or bad result code, then quit
        if (mBillingClient == null || result.getResponseCode() != BillingResponse.OK) {
            Timber.w("Billing client was null or result code (%d) was bad - quitting",
                    result.getResponseCode());
            return;
        }

        Timber.d("Query inventory was successful.");
        publishSubject.onNext(result.getPurchasesList());
    }

    /**
     * Query purchases across various use cases and deliver the result in a formalized way through
     * a listener
     */
    public void queryPurchases() {
        Disposable disposable = connect()
                .subscribe(code -> {
                    if (code == BillingResponse.OK) {
                        long time = System.currentTimeMillis();
                        PurchasesResult purchasesResult = mBillingClient.queryPurchases(SkuType.INAPP);
                        Timber.i("Querying purchases elapsed time: %s ms",
                                (System.currentTimeMillis() - time));
                        // If there are subscriptions supported, we add subscription rows as well
                        if (areSubscriptionsSupported()) {
                            PurchasesResult subscriptionResult
                                    = mBillingClient.queryPurchases(SkuType.SUBS);

                            if (subscriptionResult!= null && subscriptionResult.getResponseCode()
                                    == BillingResponse.OK) {
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
                        } else if (purchasesResult.getResponseCode() == BillingResponse.OK) {
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

    public void querySkuDetailsAsync(@SkuType final String itemType, final List<String> skuList,
                                     final SkuDetailsResponseListener listener) {

        Disposable disposable = connect()
                .subscribe(code -> {
                    if (code == BillingResponse.OK) {
                        // Query the purchase async
                        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                        params.setSkusList(skuList).setType(itemType);
                        mBillingClient.querySkuDetailsAsync(params.build(), listener);
                    }
                });
        mConnectionDisposable.add(disposable);
    }

    public void consumeAsync(final String purchaseToken) {
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
                        mBillingUpdatesListener.onConsumeFinished(purchaseToken1, responseCode);

        Disposable disposable = connect()
                .subscribe(code -> {
                    if (code == BillingResponse.OK) {
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
