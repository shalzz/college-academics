package com.shalzz.attendance.billing;

import android.app.Activity;

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
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.shalzz.attendance.utils.RxUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

public class BillingManager implements PurchasesUpdatedListener {

    // Default value of mBillingClientResponseCode until BillingManager was not yet initialized
    public static final int BILLING_MANAGER_NOT_INITIALIZED  = -1;

    private BillingClient mBillingClient;

    private final BillingUpdatesListener mBillingUpdatesListener;
    private int mBillingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED;

    private final List<Purchase> mPurchases = new ArrayList<>();
    private Set<String> mTokensToBeConsumed;

    private Activity mActivity;
    private Disposable mConnectionDisposable;

    /* BASE_64_ENCODED_PUBLIC_KEY should be YOUR APPLICATION'S PUBLIC KEY
     * (that you got from the Google Play developer console). This is not your
     * developer public key, it's the *app-specific* public key.
     *
     * Instead of just storing the entire literal string here embedded in the
     * program,  construct the key at runtime from pieces or
     * use bit manipulation (for example, XOR with some other string) to hide
     * the actual key.  The key itself is not secret information, but we don't
     * want to make it easy for an attacker to replace the public key with one
     * of their own and then fake messages from the server.
     */
    private static final String BASE_64_ENCODED_PUBLIC_KEY = "CONSTRUCT_YOUR_KEY_AND_PLACE_IT_HERE";

    /**
     * Listener to the updates that happen when purchases list was updated or consumption of the
     * item was finished
     */
    public interface BillingUpdatesListener {
        void onBillingClientSetupFinished();
        void onConsumeFinished(String token, @BillingResponse int result);
        void onPurchasesUpdated(List<Purchase> purchases);
    }

    public BillingManager(Activity activity,
                          final BillingUpdatesListener updatesListener) {
        mActivity = activity;
        mBillingUpdatesListener = updatesListener;
        mBillingClient = BillingClient.newBuilder(mActivity).setListener(this).build();

        // Start the setup asynchronously.
        // The specified listener is called once setup completes.
        // New purchases are reported through the onPurchasesUpdated() callback
        // of the class specified using the setListener() method above.
        executeServiceRequest(() -> {
            // Notify the listener that the billing client is ready.
            mBillingUpdatesListener.onBillingClientSetupFinished();
            // IAB is fully setup. Now get an inventory of stuff the user owns.
            queryPurchases();
        });
    }

    private void executeServiceRequest(final Runnable executeOnSuccess) {
        RxUtil.dispose(mConnectionDisposable);
        mConnectionDisposable = Observable.create((ObservableOnSubscribe<Integer>) source -> {
            Timber.d("Called");
            if (source.isDisposed()) return;
            if (mBillingClient.isReady()) {
                if (executeOnSuccess != null) {
                    executeOnSuccess.run();
                    return;
                }
            }
            mBillingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@BillingResponse int billingResponseCode) {
                    Timber.d("Setup finished. Response code: %d", billingResponseCode);
                    mBillingClientResponseCode = billingResponseCode;
                    source.onNext(mBillingClientResponseCode);
                }

                @Override
                public void onBillingServiceDisconnected() {
                    source.tryOnError(new Throwable("Billing Client disconnected!"));
                    Timber.w("onBillingServiceDisconnected1()");
                }
            });
        })
        .retry()
        .subscribe(code -> {
            if (code == BillingResponse.OK) {
                if (executeOnSuccess != null) {
                    executeOnSuccess.run();
                }
            }
        }, throwable -> {
            Timber.w("onBillingServiceDisconnected()");
        });
    }

    /**
     * Handle a callback that purchases were updated from the Billing library
     */
    @Override
    public void onPurchasesUpdated(int resultCode, List<Purchase> purchases) {
        if (resultCode == BillingResponse.OK) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
            mBillingUpdatesListener.onPurchasesUpdated(mPurchases);
        } else if (resultCode == BillingResponse.USER_CANCELED) {
            Timber.i("onPurchasesUpdated() - user cancelled the purchase flow - skipping");
        } else {
            Timber.w("onPurchasesUpdated() got unknown resultCode: %d", resultCode);
        }
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
        Runnable purchaseFlowRequest = () -> {
            Timber.d("Launching in-app purchase flow. Replace old SKU? %s" ,(oldSkus !=
                    null));
            BillingFlowParams purchaseParams = BillingFlowParams.newBuilder()
                    .setSku(skuId).setType(billingType).setOldSkus(oldSkus).build();
            mBillingClient.launchBillingFlow(mActivity, purchaseParams);
        };

        executeServiceRequest(purchaseFlowRequest);
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

    public void querySkuDetailsAsync(@SkuType final String itemType, final List<String> skuList,
                                     final SkuDetailsResponseListener listener) {
        // Creating a runnable from the request to use it inside our connection retry policy below
        Runnable queryRequest = new Runnable() {
            @Override
            public void run() {
                // Query the purchase async
                SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                params.setSkusList(skuList).setType(itemType);
                mBillingClient.querySkuDetailsAsync(params.build(),
                        new SkuDetailsResponseListener() {
                            @Override
                            public void onSkuDetailsResponse(int responseCode,
                                                             List<SkuDetails> skuDetailsList) {
                                listener.onSkuDetailsResponse(responseCode, skuDetailsList);
                            }
                        });
            }
        };

        executeServiceRequest(queryRequest);
    }

    public void consumeAsync(final String purchaseToken) {
        // If we've already scheduled to consume this token - no action is needed (this could happen
        // if you received the token when querying purchases inside onReceive() and later from
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

        Runnable consumeRequest = () -> mBillingClient.consumeAsync(purchaseToken, onConsumeListener);

        executeServiceRequest(consumeRequest);
    }

    /**
     * Returns the value Billing client response code or BILLING_MANAGER_NOT_INITIALIZED if the
     * clien connection response was not received yet.
     */
    public int getBillingClientResponseCode() {
        return mBillingClientResponseCode;
    }

    /**
     * Handles the purchase
     * <p>Note: Notice that for each purchase, we check if signature is valid on the client.
     * It's recommended to move this check into your backend.
     * See {@link Security#verifyPurchase(String, String, String)}
     * </p>
     * @param purchase Purchase to be handled
     */
    private void handlePurchase(Purchase purchase) {
        if (!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
            Timber.i( "Got a purchase: %s ; but signature is bad. Skipping...", purchase);
            return;
        }

        Timber.d("Got a verified purchase: %s", purchase);

        mPurchases.add(purchase);
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

        // Update the UI and purchases inventory with new list of purchases
        mPurchases.clear();
        onPurchasesUpdated(BillingResponse.OK, result.getPurchasesList());
    }

    /**
     * Checks if subscriptions are supported for current client
     * <p>Note: This method does not automatically retry for RESULT_SERVICE_DISCONNECTED.
     * It is only used in unit tests and after queryPurchases execution, which already has
     * a retry-mechanism implemented.
     * </p>
     */
    public boolean areSubscriptionsSupported() {
        int responseCode = mBillingClient.isFeatureSupported(FeatureType.SUBSCRIPTIONS);
        if (responseCode != BillingResponse.OK) {
            Timber.w("areSubscriptionsSupported() got an error response: %d", responseCode);
        }
        return responseCode == BillingResponse.OK;
    }

    /**
     * Query purchases across various use cases and deliver the result in a formalized way through
     * a listener
     */
    public void queryPurchases() {
        Runnable queryToExecute = new Runnable() {
            @Override
            public void run() {
                long time = System.currentTimeMillis();
                PurchasesResult purchasesResult = mBillingClient.queryPurchases(SkuType.INAPP);
                Timber.i("Querying purchases elapsed time: " + (System.currentTimeMillis() - time)
                        + "ms");
                // If there are subscriptions supported, we add subscription rows as well
                if (areSubscriptionsSupported()) {
                    PurchasesResult subscriptionResult
                            = mBillingClient.queryPurchases(SkuType.SUBS);
                    Timber.i("Querying purchases and subscriptions elapsed time: "
                            + (System.currentTimeMillis() - time) + "ms");
                    Timber.i( "Querying subscriptions result code: "
                            + subscriptionResult.getResponseCode()
                            + " res: " + subscriptionResult.getPurchasesList().size());

                    if (subscriptionResult.getResponseCode() == BillingResponse.OK) {
                        purchasesResult.getPurchasesList().addAll(
                                subscriptionResult.getPurchasesList());
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
        };

        executeServiceRequest(queryToExecute);
    }

    /**
     * Verifies that the purchase was signed correctly for this developer's public key.
     * <p>Note: It's strongly recommended to perform such check on your backend since hackers can
     * replace this method with "constant true" if they decompile/rebuild your app.
     * </p>
     */
    private boolean verifyValidSignature(String signedData, String signature) {
        try {
            return Security.verifyPurchase(BASE_64_ENCODED_PUBLIC_KEY, signedData, signature);
        } catch (IOException e) {
            Timber.e(e, "Got an exception trying to validate a purchase");
            return false;
        }
    }
}
