/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.plugin.login;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureCredential;
import com.microsoft.azure.common.utils.TextUtils;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.management.resources.Subscription;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.util.Scanner;

/**
 * Goal to switch among multiple azure subscriptions.
 */
@Mojo(name = "select-subscription", aggregator = true)
public class SelectSubscriptionMojo extends AbstractAzureMojo {

    /**
     * The maven cli argument for set the active subscription by id or name
     */
    @Parameter(property = "subscription")
    private String subscription;

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        final Log log = getLog();
        final AzureTokenCredentials tokenCredentials;

        if (!AzureAuthHelper.existsAzureSecretFile()) {
            throw new MojoFailureException("You are not logged in.");
        }

        try {
            tokenCredentials = AzureAuthHelper.getMavenAzureLoginCredentials();
        } catch (IOException e) {
            throw new MojoFailureException("Cannot read azure credentials due to error: " + e.getMessage());
        }

        final Authenticated azure = Azure.configure().authenticate(tokenCredentials);
        Subscription selectSubscription = null;

        final PagedList<Subscription> subscriptions = azure.subscriptions().list();
        for (final Subscription newSubscription : subscriptions) {
            if (StringUtils.equalsIgnoreCase(subscription, newSubscription.subscriptionId()) ||
                    StringUtils.equalsIgnoreCase(subscription, newSubscription.displayName())) {
                selectSubscription = newSubscription;
                break;
            }
        }

        final String oldSelectedSubscription = tokenCredentials.defaultSubscriptionId();

        if (selectSubscription == null) {
            if (StringUtils.isNotBlank(subscription)) {
                throw new MojoFailureException(String.format("The subscription of '%s' doesn't exist.", subscription));
            }
            if (subscriptions.size() == 0) {
                throw new MojoExecutionException("Cannot find any subscriptions.");
            } else if (subscriptions.size() == 1) {
                selectSubscription = subscriptions.get(0);
            } else {
                // promote user for select subscription
                // TODO: wrap it in an utility method
                System.out.println("Please choose from the following subscriptions:");
                int index = 1;
                for (final Subscription subs : subscriptions) {
                    final boolean current = StringUtils.equalsIgnoreCase(oldSelectedSubscription, subs.subscriptionId());
                    final String subscriptionLine = String.format("%2d. %s (%s)%s", index++, subs.displayName(), subs.subscriptionId(),
                            current ? " [CURRENT]" : "");

                    if (current) {
                        System.out.println(TextUtils.green(subscriptionLine));
                    } else {
                        System.out.println(subscriptionLine);
                    }

                }

                final Scanner scanner = getScanner();
                int userSelected;
                while (true) {
                    System.out.print("Enter index value for subscription id: ");
                    System.out.flush();
                    final String input = scanner.nextLine();
                    try {
                        final int selectIndex = Integer.parseInt(input);
                        if (selectIndex >= 1 && selectIndex <= subscriptions.size()) {
                            userSelected = selectIndex;
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                    // Reaching here means invalid input
                    System.err.println("Wong value selected: " + input);
                }
                selectSubscription = subscriptions.get(userSelected - 1);

            }

        }

        if (selectSubscription != null && !StringUtils.equalsIgnoreCase(selectSubscription.subscriptionId(), oldSelectedSubscription)) {
            try {
                final AzureCredential azureCredential = AzureAuthHelper.readAzureCredentials();
                azureCredential.setDefaultSubscription(selectSubscription.subscriptionId());
                AzureAuthHelper.writeAzureCredentials(azureCredential, AzureAuthHelper.getAzureSecretFile());
                log.info(String.format("You have set default subscription to '%s'.", TextUtils.green(selectSubscription.displayName())));
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot update subscription id due to error: " + e.getMessage(), e);
            }
        }

    }

    protected Scanner getScanner() {
        return new Scanner(System.in, "UTF-8");
    }
}
