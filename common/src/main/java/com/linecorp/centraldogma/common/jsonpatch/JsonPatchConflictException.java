/*
 * Copyright 2025 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.centraldogma.common.jsonpatch;

import com.linecorp.centraldogma.common.CentralDogmaException;
import com.linecorp.centraldogma.common.ChangeConflictException;

/**
 * A {@link CentralDogmaException} raised when a JSON Patch operation fails.
 */
public final class JsonPatchConflictException extends ChangeConflictException {

    private static final long serialVersionUID = 4746173383862473527L;

    /**
     * Creates a new instance.
     */
    public JsonPatchConflictException(String message) {
        super(message);
    }

    /**
     * Creates a new instance with the specified {@code cause}.
     */
    public JsonPatchConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
