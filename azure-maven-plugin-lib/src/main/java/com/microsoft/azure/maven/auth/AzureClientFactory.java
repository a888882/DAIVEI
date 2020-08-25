/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.auth;

import com.google.common.base.Preconditions;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.auth.AzureTokenWrapper;
import com.microsoft.azure.auth.exception.AzureLoginFailureException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.management.resources.Subscription;

import com.microsoft.azure.maven.telemetry.BenchmarkUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Optional;

import static com.microsoft.azure.maven.telemetry.BenchmarkUtils.getDateTimeNowString;
import static com.microsoft.azure.maven.telemetry.BenchmarkUtils.recordEvents;

public class AzureClientFactory {

    private static final String SUBSCRIPTION_TEMPLATE = "Subscription : %s(%s)";
    public static final String SUBSCRIPTION_NOT_FOUND = "Subscription %s was not found in current account.";
    public static final String NO_AVAILABLE_SUBSCRIPTION = "No available subscription found in current account.";
    public static final String SUBSCRIPTION_NOT_SPECIFIED = "Subscription ID was not specified, using the first subscription in current account," +
            " please refer https://github.com/microsoft/azure-maven-plugins/wiki/Authentication#subscription for more information.";

    public static Azure getAzureClient(AzureTokenWrapper azureTokenCredentials, String subscriptionId,
                                       String userAgent) throws IOException, AzureLoginFailureException {
        Preconditions.checkNotNull(azureTokenCredentials, "The parameter 'azureTokenCredentials' cannot be null.");
        Log.info(azureTokenCredentials.getCredentialDescription());
        final Authenticated authenticated = Azure.configure().withInterceptor(BenchmarkUtils.getInterceptor()).withUserAgent(userAgent).authenticate(azureTokenCredentials);
        // For cloud shell, use subscription in profile as the default subscription.
        if (StringUtils.isEmpty(subscriptionId) && AzureAuthHelperLegacy.isInCloudShell()) {
            subscriptionId = AzureAuthHelperLegacy.getSubscriptionOfCloudShell();
        }
        subscriptionId = StringUtils.isEmpty(subscriptionId) ? azureTokenCredentials.defaultSubscriptionId() : subscriptionId;
        final Azure azureClient = StringUtils.isEmpty(subscriptionId) ? authenticated.withDefaultSubscription() :
                authenticated.withSubscription(subscriptionId);
        checkSubscription(azureClient, subscriptionId);
        final Subscription subscription = azureClient.getCurrentSubscription();
        Log.info(String.format(SUBSCRIPTION_TEMPLATE, subscription.displayName(), subscription.subscriptionId()) + getDateTimeNowString());
        recordEvents(String.format(SUBSCRIPTION_TEMPLATE, subscription.displayName(), subscription.subscriptionId()));
        return azureClient;
    }

    private static void checkSubscription(Azure azure, String targetSubscription) throws AzureLoginFailureException {
        final PagedList<Subscription> subscriptions = azure.subscriptions().list();
        subscriptions.loadAll();
        if (subscriptions.size() == 0) {
            throw new AzureLoginFailureException(NO_AVAILABLE_SUBSCRIPTION);
        }
        if (StringUtils.isEmpty(targetSubscription)) {
            Log.warn(SUBSCRIPTION_NOT_SPECIFIED);
            return;
        }
        final Optional<Subscription> optionalSubscription = subscriptions.stream()
                .filter(subscription -> StringUtils.equals(subscription.subscriptionId(), targetSubscription))
                .findAny();
        if (!optionalSubscription.isPresent()) {
            throw new AzureLoginFailureException(String.format(SUBSCRIPTION_NOT_FOUND, targetSubscription));
        }
    }
}
