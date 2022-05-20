package me.wuxie.bukkitmodify.javassist;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import lombok.SneakyThrows;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static me.wuxie.bukkitmodify.javassist.Main.stringToListString;

public class ItemMetaModify {
    static String bukkitVersion = Main.bukkitVersion;
    static boolean initNMS = false;
    static Class<?> CraftMetaItem;
    static Class<?> CraftNBTTagConfigSerializer;
    static Class<?> NBTBase;
    static Class<?> NBTTagString;
    static Class<?> NBTTagCompound;
    static Class<?> NBTCompressedStreamTools;
    static Class<?> CraftPersistentDataContainer;
    static Method hasCustomModelData;
    static Method getCustomModelData;
    static Method hasBlockData;
    static Method hasRepairCost;
    static Method hasDamage;
    static Method serializeInternal;
    static Method serialize;
    static Method set;
    static Method asString;
    static Method a;
    static Method serialize1;
    static Field blockData;
    static Field attributeModifiers;
    static Field repairCost;
    static Field damage;
    static Field unhandledTags;
    static Field persistentDataContainer;
    @SneakyThrows
    static void initNMS(){
        initNMS = true;
        CraftMetaItem = Class.forName("org.bukkit.craftbukkit."+bukkitVersion+".inventory.CraftMetaItem");
        CraftNBTTagConfigSerializer = Class.forName("org.bukkit.craftbukkit."+bukkitVersion+".util.CraftNBTTagConfigSerializer");
        NBTTagCompound = Class.forName("net.minecraft.server."+bukkitVersion+".NBTTagCompound");
        NBTBase = Class.forName("net.minecraft.server."+bukkitVersion+".NBTBase");
        NBTCompressedStreamTools = Class.forName("net.minecraft.server."+bukkitVersion+".NBTCompressedStreamTools");
        CraftPersistentDataContainer = Class.forName("org.bukkit.craftbukkit."+bukkitVersion+".persistence.CraftPersistentDataContainer");
        NBTTagString = Class.forName("net.minecraft.server."+bukkitVersion+".NBTTagString");

        hasCustomModelData = CraftMetaItem.getMethod("hasCustomModelData");
        getCustomModelData = CraftMetaItem.getMethod("getCustomModelData");
        hasBlockData = CraftMetaItem.getMethod("hasBlockData");
        hasRepairCost = CraftMetaItem.getMethod("hasRepairCost");
        hasDamage = CraftMetaItem.getMethod("hasDamage");
        serializeInternal = CraftMetaItem.getDeclaredMethod("serializeInternal", Map.class);
        serializeInternal.setAccessible(true);

        serialize = CraftNBTTagConfigSerializer.getMethod("serialize",NBTBase);
        set = NBTTagCompound.getMethod("set",String.class,NBTBase);
        asString = NBTTagString.getMethod("asString");
        a = NBTCompressedStreamTools.getMethod("a",NBTTagCompound, OutputStream.class);
        serialize1 = CraftPersistentDataContainer.getMethod("serialize");

        blockData = CraftMetaItem.getDeclaredField("blockData");
        attributeModifiers = CraftMetaItem.getDeclaredField("attributeModifiers");
        repairCost = CraftMetaItem.getDeclaredField("repairCost");
        damage = CraftMetaItem.getDeclaredField("damage");
        unhandledTags = CraftMetaItem.getDeclaredField("unhandledTags");
        persistentDataContainer = CraftMetaItem.getDeclaredField("persistentDataContainer");
        blockData.setAccessible(true);
        attributeModifiers.setAccessible(true);
        repairCost.setAccessible(true);
        damage.setAccessible(true);
        unhandledTags.setAccessible(true);
        persistentDataContainer.setAccessible(true);
    }

    @SneakyThrows
    public static ImmutableMap.Builder<String, Object> serialize(ImmutableMap.Builder<String, Object> builder, ItemMeta meta) {
        if(!initNMS){
            initNMS();
        }
        Map internalTags = new HashMap((Map) unhandledTags.get(meta));

        if (meta.hasDisplayName()) {
            builder.put("display-name", meta.getDisplayName());
        }
        if (meta.hasLocalizedName()) {
            builder.put("loc-name", meta.getLocalizedName());
        }
        boolean put = false;
        if(internalTags.containsKey("lore12")){
            if(internalTags.containsKey("sLore")){
                put = true;
                builder.put("ilore", stringToListString((String) asString.invoke(internalTags.get("sLore"))));
            }
        }else if (meta.hasLore()) {
            builder.put("lore", ImmutableList.copyOf(meta.getLore()));
        }
        if ((boolean)hasCustomModelData.invoke(meta)) {
            builder.put("custom-model-data", getCustomModelData.invoke(meta));
        }
        if ((boolean)hasBlockData.invoke(meta)) {
            builder.put("BlockStateTag", serialize.invoke(CraftNBTTagConfigSerializer,blockData.get(meta)));
        }
        serializeEnchantments(meta.getEnchants(), builder);
        serializeModifiers((Multimap<Attribute, AttributeModifier>) attributeModifiers.get(meta), builder);
        if ((boolean)hasRepairCost.invoke(meta)) {
            builder.put("repair-cost", repairCost.get(meta));
        }
        List<String> hideFlags = new ArrayList();
        Iterator var4 = meta.getItemFlags().iterator();
        while(var4.hasNext()) {
            ItemFlag hideFlagEnum = (ItemFlag)var4.next();
            hideFlags.add(hideFlagEnum.name());
        }
        if (!hideFlags.isEmpty()) {
            builder.put("ItemFlags", hideFlags);
        }
        if (meta.isUnbreakable()) {
            builder.put("Unbreakable", meta.isUnbreakable());
        }
        if ((boolean)hasDamage.invoke(meta)) {
            builder.put("Damage", damage.get(meta));
        }
        if (internalTags.containsKey("lore12")) {
            builder.put("i12Lore", true);
            internalTags.remove("lore12");
        }
        if (internalTags.containsKey("ilore")&&!put) {
            List<String> l = new ArrayList<>();
            List list = (List) internalTags.get("ilore");
            for (Object o:list){
                l.add((String) asString.invoke(o));
            }
            builder.put("ilore", l);
            internalTags.remove("ilore");
        }
        if (internalTags.containsKey("sLore")) {
            internalTags.remove("sLore");
        }
        serializeInternal.invoke(meta,internalTags);

        if (!internalTags.isEmpty()) {
            Object internal = NBTTagCompound.newInstance();
            for (Map.Entry e : (Set<Map.Entry>)internalTags.entrySet()) {
                //if(!e.getKey().equals("i12Lore")){
                set.invoke(internal, e.getKey(), e.getValue());
                //}
            }
            try {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                a.invoke(NBTCompressedStreamTools,internal,buf);
                builder.put("internal", org.bukkit.craftbukkit.libs.org.apache.commons.codec.binary.Base64.encodeBase64String(buf.toByteArray()));
            } catch (Exception var7) {
                Logger.getLogger(meta.getClass().getName()).log(Level.SEVERE, null, var7);
            }
        }
        if (!((PersistentDataContainer)persistentDataContainer.get(meta)).isEmpty()) {
            builder.put("PublicBukkitValues", serialize1.invoke(persistentDataContainer.get(meta)));
        }
        return builder;
    }
    static void serializeEnchantments(Map<Enchantment, Integer> enchantments, ImmutableMap.Builder<String, Object> builder) {
        if (enchantments != null && !enchantments.isEmpty()) {
            ImmutableMap.Builder<String, Integer> enchants = ImmutableMap.builder();
            Iterator var5 = enchantments.entrySet().iterator();

            while(var5.hasNext()) {
                Map.Entry<? extends Enchantment, Integer> enchant = (Map.Entry)var5.next();
                enchants.put(((Enchantment)enchant.getKey()).getName(), (Integer)enchant.getValue());
            }

            builder.put("enchants", enchants.build());
        }
    }
    static void serializeModifiers(Multimap<Attribute, AttributeModifier> modifiers, ImmutableMap.Builder<String, Object> builder) {
        if (modifiers != null && !modifiers.isEmpty()) {
            Map<String, List<Object>> mods = new LinkedHashMap();
            Iterator var5 = modifiers.entries().iterator();

            while(var5.hasNext()) {
                Map.Entry<Attribute, AttributeModifier> entry = (Map.Entry)var5.next();
                if (entry.getKey() != null) {
                    Collection<AttributeModifier> modCollection = modifiers.get(entry.getKey());
                    if (modCollection != null && !modCollection.isEmpty()) {
                        mods.put((entry.getKey()).name(), new ArrayList(modCollection));
                    }
                }
            }

            builder.put("attribute-modifiers", mods);
        }
    }
}
