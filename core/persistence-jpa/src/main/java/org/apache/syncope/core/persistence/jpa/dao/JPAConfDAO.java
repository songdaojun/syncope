/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.Collections;
import java.util.List;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.Conf;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPAConf;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAConfDAO extends AbstractDAO<Conf> implements ConfDAO {

    private static final String KEY = "cd64d66f-6fff-4008-b966-a06b1cc1436d";

    @Override
    public Conf get() {
        Conf instance = entityManager().find(JPAConf.class, KEY);
        if (instance == null) {
            instance = new JPAConf();
            instance.setKey(KEY);

            instance = entityManager().merge(instance);
        }

        return instance;
    }

    @Transactional(readOnly = true)
    @Override
    public CPlainAttr find(final String key) {
        return get().getPlainAttr(key);
    }

    @Transactional(readOnly = true)
    @Override
    public List<String> getValuesAsStrings(final String key) {
        CPlainAttr attr = find(key);
        return attr == null ? Collections.<String>emptyList() : attr.getValuesAsStrings();
    }

    @Transactional(readOnly = true)
    @Override
    public <T> T find(final String key, final T defaultValue) {
        CPlainAttr result = find(key);
        if (result == null) {
            return defaultValue;
        }

        return result.getUniqueValue() == null
                ? result.getValues().isEmpty()
                ? null
                : result.getValues().get(0).<T>getValue()
                : result.getUniqueValue().<T>getValue();
    }

    @Override
    public Conf save(final CPlainAttr attr) {
        Conf instance = get();

        CPlainAttr old = instance.getPlainAttr(attr.getSchema().getKey());
        if (old != null && (!attr.getSchema().isUniqueConstraint()
                || (!attr.getUniqueValue().getStringValue().equals(old.getUniqueValue().getStringValue())))) {

            old.setOwner(null);
            instance.remove(old);
        }

        instance.add(attr);
        attr.setOwner(instance);

        return entityManager().merge(instance);
    }

    @Override
    public Conf delete(final String key) {
        Conf instance = get();
        CPlainAttr attr = instance.getPlainAttr(key);
        if (attr != null) {
            attr.setOwner(null);
            instance.remove(attr);

            instance = entityManager().merge(instance);
        }

        return instance;
    }
}
