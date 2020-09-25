/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.springcloud.exception;

public class NoResourcesAvailableException extends SpringConfigurationException {

    public NoResourcesAvailableException(String message) {
        super(message);
    }

    private static final long serialVersionUID = -8631337506110108893L;

}
