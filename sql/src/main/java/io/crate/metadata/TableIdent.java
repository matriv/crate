/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.metadata;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import io.crate.exceptions.InvalidSchemaNameException;
import io.crate.exceptions.InvalidTableNameException;
import io.crate.sql.Identifiers;
import io.crate.sql.tree.QualifiedName;
import io.crate.sql.tree.Table;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class TableIdent implements Writeable {

    private static final Set<String> INVALID_TABLE_NAME_CHARACTERS = ImmutableSet.of(".");

    private final String schema;
    private final String name;

    public static TableIdent of(Table tableNode, String defaultSchema) {
        return of(tableNode.getName(), defaultSchema);
    }

    public static TableIdent of(QualifiedName name, String defaultSchema) {
        List<String> parts = name.getParts();
        Preconditions.checkArgument(parts.size() < 3,
            "Table with more then 2 QualifiedName parts is not supported. only <schema>.<tableName> works.");
        if (parts.size() == 2) {
            return new TableIdent(parts.get(0), parts.get(1));
        }
        return new TableIdent(defaultSchema, parts.get(0));
    }

    public static TableIdent fromIndexName(String indexName) {
        IndexParts indexParts = new IndexParts(indexName);
        return indexParts.toTableIdent();
    }

    public static String fqnFromIndexName(String indexName) {
        return new IndexParts(indexName).toFullyQualifiedName();
    }

    public TableIdent(StreamInput in) throws IOException {
        schema = in.readString();
        name = in.readString();
    }

    public TableIdent(String schema, String name) {
        assert schema != null : "schema name must not be null";
        assert name != null : "table name must not be null";
        this.schema = schema;
        this.name = name;
    }

    public String schema() {
        return schema;
    }

    public String name() {
        return name;
    }

    public String fqn() {
        return schema + "." + name;
    }

    public String sqlFqn() {
        return Identifiers.quoteIfNeeded(schema) + "." + Identifiers.quoteIfNeeded(name);
    }

    public String indexName() {
        if (schema.equalsIgnoreCase(Schemas.DOC_SCHEMA_NAME)) {
            return name;
        }
        return fqn();
    }

    public void validate() throws InvalidSchemaNameException, InvalidTableNameException {
        if (!isValidTableOrSchemaName(schema)) {
            throw new InvalidSchemaNameException(schema);
        }
        if (!isValidTableOrSchemaName(name)) {
            throw new InvalidTableNameException(this);
        }
    }

    private static boolean isValidTableOrSchemaName(String name) {
        for (String illegalCharacter : INVALID_TABLE_NAME_CHARACTERS) {
            if (name.contains(illegalCharacter) || name.length() == 0) {
                return false;
            }
        }
        if (name.startsWith("_")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        TableIdent o = (TableIdent) obj;
        return Objects.equal(schema, o.schema) &&
               Objects.equal(name, o.name);
    }

    @Override
    public int hashCode() {
        int result = schema.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return fqn();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(schema);
        out.writeString(name);
    }
}
