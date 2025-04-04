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

package com.linecorp.centraldogma.common.jsonpatch;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Equivalence;

import com.linecorp.centraldogma.internal.jsonpatch.JsonNumEquals;

/**
 * JSON Patch {@code safeReplace} operation.
 *
 * <p>This operation is similar to {@link ReplaceOperation}, but it throws an error if the path does not have
 * the expected value.</p>
 */
public final class SafeReplaceOperation extends JsonPatchOperation {

    private static final Equivalence<JsonNode> EQUIVALENCE = JsonNumEquals.getInstance();

    @JsonSerialize
    private final JsonNode oldValue;
    @JsonSerialize
    private final JsonNode newValue;

    /**
     * Creates a new instance.
     */
    @JsonCreator
    SafeReplaceOperation(@JsonProperty("path") final JsonPointer path,
                         @JsonProperty("oldValue") JsonNode oldValue,
                         @JsonProperty("value") JsonNode newValue) {
        super("safeReplace", path);
        requireNonNull(oldValue, "oldValue");
        requireNonNull(newValue, "newValue");
        this.oldValue = oldValue.deepCopy();
        this.newValue = newValue.deepCopy();
    }

    /**
     * Returns the old value to be replaced.
     */
    public JsonNode oldValue() {
        return oldValue;
    }

    /**
     * Returns the new value to replace the old value.
     */
    public JsonNode newValue() {
        return newValue;
    }

    @Override
    public JsonNode apply(JsonNode node) {
        requireNonNull(node, "node");
        final JsonNode actual = ensureExistence(node);

        final JsonPointer path = path();
        if (!EQUIVALENCE.equivalent(actual, oldValue)) {
            throw new JsonPatchConflictException("mismatching value at '" + path + "': " +
                                                 actual + " (expected: " + oldValue + ')');
        }
        final JsonNode replacement = newValue.deepCopy();
        if (path.toString().isEmpty()) {
            return replacement;
        }
        final JsonNode parent = node.at(path.head());
        final String rawToken = path.last().getMatchingProperty();
        if (parent.isObject()) {
            ((ObjectNode) parent).set(rawToken, replacement);
        } else {
            ((ArrayNode) parent).set(Integer.parseInt(rawToken), replacement);
        }
        return node;
    }

    @Override
    public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("op", op());
        gen.writeStringField("path", path().toString());
        gen.writeFieldName("oldValue");
        gen.writeTree(oldValue);
        gen.writeFieldName("value");
        gen.writeTree(newValue);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer)
            throws IOException {
        serialize(gen, serializers);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SafeReplaceOperation)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final SafeReplaceOperation that = (SafeReplaceOperation) o;
        return oldValue.equals(that.oldValue) && newValue.equals(that.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), oldValue, newValue);
    }

    @Override
    public String toString() {
        return "op: " + op() + "; path: \"" + path() + "\"; oldValue: " + oldValue + "; value: " + newValue;
    }
}
