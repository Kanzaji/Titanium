/*
 * This file is part of Titanium
 * Copyright (C) 2019, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.material;

import com.google.common.collect.HashMultimap;
import com.hrznstudio.titanium.annotation.MaterialReference;
import com.hrznstudio.titanium.api.material.IResourceType;
import com.hrznstudio.titanium.util.AnnotationUtil;
import net.minecraftforge.registries.ForgeRegistryEntry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;

public class ResourceRegistry {

    private static HashMap<String, ResourceMaterial> MATERIALS = new HashMap<>();
    private static HashMap<String, HashMultimap<String, Field>> ANNOTATED_FIELDS = new HashMap<>();

    public static void scanForReferences() {
        for (Field annotatedField : AnnotationUtil.getAnnotatedFields(MaterialReference.class)) {
            if (Modifier.isStatic(annotatedField.getModifiers())) {
                MaterialReference reference = annotatedField.getAnnotation(MaterialReference.class);
                if (!annotatedField.isAccessible()) {
                    annotatedField.setAccessible(true);
                }
                if (Modifier.isFinal(annotatedField.getModifiers())) {
                    try {
                        Field modifiersField = Field.class.getDeclaredField("modifiers");
                        modifiersField.setAccessible(true);
                        modifiersField.setInt(annotatedField, annotatedField.getModifiers() & ~Modifier.FINAL);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                ANNOTATED_FIELDS.computeIfAbsent(reference.material(), s -> HashMultimap.create()).put(reference.type(), annotatedField);
            }
        }
    }

    public static ResourceMaterial getOrCreate(String type) {
        return MATERIALS.computeIfAbsent(type, s -> new ResourceMaterial(type));
    }

    public static Collection<ResourceMaterial> getMaterials() {
        return MATERIALS.values();
    }

    public static HashMap<String, HashMultimap<String, Field>> getAnnotatedFields() {
        return ANNOTATED_FIELDS;
    }

    public static void injectField(ResourceMaterial material, IResourceType type, ForgeRegistryEntry entry) {
        if (ANNOTATED_FIELDS.containsKey(material.getMaterialType())) {
            HashMultimap<String, Field> multimap = ANNOTATED_FIELDS.get(material.getMaterialType());
            if (multimap.containsKey(type.getName())) {
                multimap.get(type.getName()).stream().filter(field -> entry.getRegistryType().isAssignableFrom(field.getType())).forEach(field -> {
                    try {
                        field.set(null, entry);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }
}
