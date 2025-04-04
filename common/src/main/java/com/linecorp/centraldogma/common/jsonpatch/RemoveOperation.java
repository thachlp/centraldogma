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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * JSON Path {@code remove} operation.
 *
 * <p>This operation only takes one pointer ({@code path}) as an argument. It
 * is an error condition if no JSON value exists at that pointer.</p>
 */
public final class RemoveOperation extends JsonPatchOperation {

    /**
     * Creates a new instance.
     */
    @JsonCreator
    RemoveOperation(@JsonProperty("path") final JsonPointer path) {
        super("remove", path);
    }

    @Override
    public JsonNode apply(final JsonNode node) {
        requireNonNull(node, "node");
        final JsonPointer path = path();
        if (path.toString().isEmpty()) {
            return MissingNode.getInstance();
        }
        ensureExistence(node);

        final JsonNode parentNode = node.at(path.head());
        final String raw = path.last().getMatchingProperty();
        if (parentNode.isObject()) {
            ((ObjectNode) parentNode).remove(raw);
        } else {
            ((ArrayNode) parentNode).remove(Integer.parseInt(raw));
        }
        return node;
    }

    @Override
    public void serialize(final JsonGenerator jgen,
                          final SerializerProvider provider) throws IOException {
        requireNonNull(jgen, "jgen");
        jgen.writeStartObject();
        jgen.writeStringField("op", "remove");
        jgen.writeStringField("path", path().toString());
        jgen.writeEndObject();
    }

    @Override
    public void serializeWithType(final JsonGenerator jgen,
                                  final SerializerProvider provider, final TypeSerializer typeSer)
            throws IOException {
        serialize(jgen, provider);
    }

    @Override
    public String toString() {
        return "op: " + op() + "; path: \"" + path() + '"';
    }
}
