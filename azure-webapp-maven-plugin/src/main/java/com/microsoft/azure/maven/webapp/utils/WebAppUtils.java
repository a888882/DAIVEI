/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.common.utils.AppServiceUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.ExistingLinuxPlanWithGroup;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.ExistingWindowsPlanWithGroup;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithDockerContainerImage;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;

import static com.microsoft.azure.maven.telemetry.BenchmarkUtils.getDateTimeNowString;
import static com.microsoft.azure.maven.telemetry.BenchmarkUtils.recordEvents;


public class WebAppUtils {
    public static final String SERVICE_PLAN_NOT_APPLICABLE = "The App Service Plan '%s' is not a %s Plan";
    public static final String CREATE_SERVICE_PLAN = "Creating App Service Plan '%s'...";
    public static final String SERVICE_PLAN_CREATED = "Successfully created App Service Plan.";
    public static final String CONFIGURATION_NOT_APPLICABLE =
            "The configuration is not applicable for the target Web App (%s). Please correct it in pom.xml.";

    public static void assureLinuxWebApp(final WebApp app) throws AzureExecutionException {
        if (!isLinuxWebApp(app)) {
            throw new AzureExecutionException(String.format(CONFIGURATION_NOT_APPLICABLE, "Windows"));
        }
    }

    public static void assureWindowsWebApp(final WebApp app) throws AzureExecutionException {
        if (isLinuxWebApp(app)) {
            throw new AzureExecutionException(String.format(CONFIGURATION_NOT_APPLICABLE, "Linux"));
        }
    }

    public static WithDockerContainerImage defineLinuxApp(final String resourceGroup,
                                                          final String appName,
                                                          final Azure azureClient,
                                                          final AppServicePlan plan) throws AzureExecutionException {
        assureLinuxPlan(plan);

        final ExistingLinuxPlanWithGroup existingLinuxPlanWithGroup = azureClient.webApps()
                .define(appName).withExistingLinuxPlan(plan);
        return azureClient.resourceGroups().contain(resourceGroup) ?
                existingLinuxPlanWithGroup.withExistingResourceGroup(resourceGroup) :
                existingLinuxPlanWithGroup.withNewResourceGroup(resourceGroup);
    }

    public static WithCreate defineWindowsApp(final String resourceGroup,
                                              final String appName,
                                              final Azure azureClient, final AppServicePlan plan) throws AzureExecutionException {
        assureWindowsPlan(plan);

        final ExistingWindowsPlanWithGroup existingWindowsPlanWithGroup = azureClient.webApps()
                .define(appName).withExistingWindowsPlan(plan);
        return azureClient.resourceGroups().contain(resourceGroup) ?
                existingWindowsPlanWithGroup.withExistingResourceGroup(resourceGroup) :
                existingWindowsPlanWithGroup.withNewResourceGroup(resourceGroup);
    }

    public static AppServicePlan createOrGetAppServicePlan(String servicePlanName,
                                                           final String resourceGroup,
                                                           final Azure azure,
                                                           final String servicePlanResourceGroup,
                                                           final Region region,
                                                           final PricingTier pricingTier,
                                                           final OperatingSystem os) throws AzureExecutionException {
        final AppServicePlan plan = AppServiceUtils.getAppServicePlan(servicePlanName, azure,
                resourceGroup, servicePlanResourceGroup);

        return plan != null ? plan : createAppServicePlan(servicePlanName, resourceGroup, azure,
                servicePlanResourceGroup, region, pricingTier, os);
    }

    public static AppServicePlan createAppServicePlan(String servicePlanName,
                                                           final String resourceGroup,
                                                           final Azure azure,
                                                           final String servicePlanResourceGroup,
                                                           final Region region,
                                                           final PricingTier pricingTier,
                                                           final OperatingSystem os) throws AzureExecutionException {
        if (region == null) {
            throw new AzureExecutionException("Please config the <region> in pom.xml, " +
                    "it is required to create a new Azure App Service Plan.");
        }
        servicePlanName = AppServiceUtils.getAppServicePlanName(servicePlanName);
        final String servicePlanResGrp = AppServiceUtils.getAppServicePlanResourceGroup(
                resourceGroup, servicePlanResourceGroup);
        recordEvents(CREATE_SERVICE_PLAN + getDateTimeNowString());
        Log.info(String.format(CREATE_SERVICE_PLAN, servicePlanName));

        final AppServicePlan.DefinitionStages.WithGroup withGroup = azure.appServices().appServicePlans()
                .define(servicePlanName).withRegion(region);

        final AppServicePlan.DefinitionStages.WithPricingTier withPricingTier
                = azure.resourceGroups().contain(servicePlanResGrp) ?
                withGroup.withExistingResourceGroup(servicePlanResGrp) :
                withGroup.withNewResourceGroup(servicePlanResGrp);

        final AppServicePlan result = withPricingTier.withPricingTier(pricingTier).withOperatingSystem(os).create();
        recordEvents(SERVICE_PLAN_CREATED);
        Log.info(SERVICE_PLAN_CREATED + getDateTimeNowString());
        return result;
    }

    private static void assureWindowsPlan(final AppServicePlan plan) throws AzureExecutionException {
        if (!plan.operatingSystem().equals(OperatingSystem.WINDOWS)) {
            throw new AzureExecutionException(String.format(SERVICE_PLAN_NOT_APPLICABLE,
                    plan.name(), OperatingSystem.WINDOWS.name()));
        }
    }

    private static void assureLinuxPlan(final AppServicePlan plan) throws AzureExecutionException {
        if (!plan.operatingSystem().equals(OperatingSystem.LINUX)) {
            throw new AzureExecutionException(String.format(SERVICE_PLAN_NOT_APPLICABLE,
                    plan.name(), OperatingSystem.LINUX.name()));
        }
    }

    private static boolean isLinuxWebApp(final WebApp app) {
        return app.inner().kind().contains("linux");
    }
}
