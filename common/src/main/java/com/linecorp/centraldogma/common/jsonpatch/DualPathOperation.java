/*
 * Copyright 2017 LINE Corporation
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
/*
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of this file and of both licenses is available at the root of this
 * project or, if you have the jar distribution, in directory META-INF/, under
 * the names LGPL-3.0.txt and ASL-2.0.txt respectively.
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: https://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.linecorp.centraldogma.common.jsonpatch;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * Base class for JSON Patch operations taking two JSON Pointers as arguments.
 */
abstract class DualPathOperation extends JsonPatchOperation {

    @JsonSerialize(using = ToStringSerializer.class)
    private final JsonPointer from;

    /**
     * Creates a new instance.
     *
     * @param op operation name
     * @param from source path
     * @param path destination path
     */
    DualPathOperation(final String op, final JsonPointer from, final JsonPointer path) {
        super(op, path);
        this.from = requireNonNull(from, "from");
    }

    /**
     * Returns the source path.
     */
    public JsonPointer from() {
        return from;
    }

    @Override
    public final void serialize(final JsonGenerator jgen,
                                final SerializerProvider provider) throws IOException {
        requireNonNull(jgen, "jgen");
        jgen.writeStartObject();
        jgen.writeStringField("op", op());
        jgen.writeStringField("path", path().toString());
        jgen.writeStringField("from", from.toString());
        jgen.writeEndObject();
    }

    @Override
    public final void serializeWithType(final JsonGenerator jgen,
                                        final SerializerProvider provider, final TypeSerializer typeSer)
            throws IOException {
        serialize(jgen, provider);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DualPathOperation)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final DualPathOperation that = (DualPathOperation) o;
        return from.equals(that.from);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), from);
    }

    @Override
    public final String toString() {
        return "op: " + op() + "; from: \"" + from + "\"; path: \"" + path() + '"';
    }
}
