package me.wuxie.bukkitmodify.javassist;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import javassist.*;
import lombok.SneakyThrows;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private final static ClassPool pool = ClassPool.getDefault();
    public static String bukkitVersion = null;
    public static int version;
    public static void premain(final String agentArgs,final Instrumentation inst) {
        bukkitVersion = agentArgs;
        System.out.println("bukkit修改器已启动 ---> ");
        System.out.println("已绑定bukkit版本 ---> "+bukkitVersion);
        version = Integer.parseInt(bukkitVersion.split("_")[1]);
        inst.addTransformer(new Transformer());
    }
    
    private static final class Transformer implements ClassFileTransformer {
        @SneakyThrows
        @Override
        public byte[] transform(final ClassLoader loader,String className,final Class<?> classBeingRedefined,final ProtectionDomain protectionDomain,final byte[] classfileBuffer) {
            className=className.replace('/','.');
            if (className.equals("org.bukkit.craftbukkit."+bukkitVersion+".inventory.CraftInventory")) {
                try {
                    pool.insertClassPath(new LoaderClassPath(loader));
                    CtClass clazz=pool.get(className);
                    CtMethod addItem = clazz.getDeclaredMethod("addItem");
                    addItem.insertBefore("{me.wuxie.bukkitmodify.event.InventoryAddItemPreEvent event = me.wuxie.bukkitmodify.event.InventoryAddItemPreEvent.callEvent($0,$1);if(event.isCancelled()){return new java.util.HashMap();}else{$1=event.getItemStacks();}}");
                    addItem.insertAfter("{me.wuxie.bukkitmodify.event.InventoryAddItemPostEvent.callEvent($0,$1,$_);return $_;}");
                    CtMethod setItem = clazz.getDeclaredMethod("setItem");
                    setItem.insertBefore("{me.wuxie.bukkitmodify.event.InventorySetItemEvent event = me.wuxie.bukkitmodify.event.InventorySetItemEvent.callEvent($0,$1,$2);$1=event.getSlot();$2=event.getItemStack();}");
                    return clazz.toBytecode();
                }
                catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            if (className.equals("org.bukkit.configuration.file.YamlConfiguration")) {
                pool.insertClassPath(new LoaderClassPath(loader));
                CtClass clazz=pool.get(className);
                CtMethod saveToString = clazz.getDeclaredMethod("saveToString");
                //saveToString.insertAfter("{me.wuxie.bukkitmodify.javassist.Main.printStackTree();}");
                return clazz.toBytecode();
            }
            if (className.equals("org.bukkit.inventory.ItemStack")) {
                pool.insertClassPath(new LoaderClassPath(loader));
                CtClass clazz=pool.get(className);
                CtMethod serialize = clazz.getDeclaredMethod("serialize");
                //serialize.insertAfter("{me.wuxie.bukkitmodify.javassist.Main.printStackTree();return $_;}");
                return clazz.toBytecode();
            }
            if(version>12){
                if(className.equals("org.bukkit.craftbukkit."+bukkitVersion+".inventory.CraftMetaItem")){
                    try {
                        pool.insertClassPath(new LoaderClassPath(loader));
                        CtClass clazz = pool.get(className);
                        clazz.setModifiers(Modifier.PUBLIC);
                        CtField ilore = CtField.make("public java.util.List ilore = null;", clazz);
                        clazz.addField(ilore);
                        CtField i12Lore = CtField.make("public boolean i12Lore = false;", clazz);
                        clazz.addField(i12Lore);
                        CtMethod setLore = clazz.getDeclaredMethod("setLore");
                        System.out.println();
                        setLore.insertAfter(
                                "{" +
                                        "if($0.unhandledTags.containsKey(\"lore12\"))" +
                                        "{" +
                                        "$0.i12Lore=true;" +
                                        "$0.unhandledTags.put(\"sLore\",net.minecraft.server."+bukkitVersion+".NBTTagString.a(me.wuxie.bukkitmodify.javassist.Main.listStringToString($1)));"+
                                        "}"+
                                        "if($0.i12Lore){" +
                                        "$0.ilore=new java.util.ArrayList($1);" +
                                        "}" +
                                        "}"
                        );
                        CtMethod getLore = clazz.getDeclaredMethod("getLore");
                        getLore.setBody(
                                "{" +
                                        "if(!$0.i12Lore&&$0.unhandledTags.containsKey(\"lore12\"))" +
                                        "{" +
                                        "$0.i12Lore=true;"+
                                        "if($0.unhandledTags.containsKey(\"sLore\"))" +
                                        "{" +
                                        "$0.lore=new java.util.ArrayList();" +
                                        "$0.ilore=me.wuxie.bukkitmodify.javassist.Main.stringToListString(((net.minecraft.server."+bukkitVersion+".NBTTagString)$0.unhandledTags.get(\"sLore\")).asString());"+
                                        "org.bukkit.craftbukkit."+bukkitVersion+".inventory.CraftMetaItem.safelyAdd($0.ilore,$0.lore,true);"+
                                        "}" +
                                        "}" +
                                        //"if($0.i12Lore!=null){return new java.util.ArrayList($0.i12Lore);} else {return new java.util.ArrayList($0.lore);}" +
                                        "java.util.List lore = $0.ilore!=null?$0.ilore:$0.lore;"+
                                        "return lore ==null?null:new java.util.ArrayList(lore);"+
                                        "}"
                        );
                        CtMethod hasLore = clazz.getDeclaredMethod("hasLore");
                        hasLore.setBody(
                                "{" +
                                "return ($0.lore != null && !$0.lore.isEmpty())" +
                                "||($0.ilore != null && !$0.ilore.isEmpty())" +
                                "||($0.unhandledTags.containsKey(\"lore12\")&&$0.unhandledTags.containsKey(\"sLore\"));" +
                                "}");
                        CtConstructor ctConstructor = clazz.getDeclaredConstructor(new CtClass[]{ClassPool.getDefault().get("java.util.Map")});
                        ctConstructor.insertAfter(
                                "{" +
                                "if($1.containsKey(\"i12Lore\")&&((java.lang.Boolean)$1.get(\"i12Lore\")).booleanValue())" +
                                        "{" +
                                        //"java.lang.System.out.println($1.get(\"i12Lore\").getClass().getName());"+
                                        "$0.i12Lore=true;" +
                                        "$0.unhandledTags.put(\"lore12\",net.minecraft.server."+bukkitVersion+".NBTTagByte.c);"+
                                        "java.util.List lore = $1.containsKey(\"ilore\")?(java.util.List)$1.get(\"ilore\"):me.wuxie.bukkitmodify.javassist.Main.stringToListString($0.unhandledTags.containsKey(\"ilore\")?((net.minecraft.server."+bukkitVersion+".NBTTagString)$0.unhandledTags.get(\"sLore\")).asString():\"\");" +
                                        "$0.ilore=lore;" +
                                        "java.util.Iterator var4 = lore.iterator();" +
                                        "java.util.List l = new java.util.ArrayList();" +
                                        "while (var4.hasNext()){java.lang.Object object = var4.next();if(object instanceof java.lang.String){l.add((java.lang.String)object);}}" +
                                        "java.util.List ll = new java.util.ArrayList();"+
                                        "org.bukkit.craftbukkit."+bukkitVersion+".inventory.CraftMetaItem.safelyAdd(lore,ll,true);" +
                                        "$0.lore=ll;" +
                                        "$0.unhandledTags.put(\"sLore\",net.minecraft.server."+bukkitVersion+".NBTTagString.a(me.wuxie.bukkitmodify.javassist.Main.listStringToString(l)));"+
                                        ""+
                                "}" +
                                "}");
                        CtMethod serialize1 = clazz.getDeclaredMethod("serialize",new CtClass[0]);
                        serialize1.setBody(
                                "{" +
                                "com.google.common.collect.ImmutableMap$Builder map = com.google.common.collect.ImmutableMap.builder();" +
                                "map.put(\"meta-type\", org.bukkit.craftbukkit."+bukkitVersion+".inventory.CraftMetaItem.SerializableMeta.classMap.get($0.getClass()));" +
                                "me.wuxie.bukkitmodify.javassist.ItemMetaModify.serialize(map,$0);" +
                                "return map.build();" +
                                "}");
                        return clazz.toBytecode();
                    }
                    catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }
    public static void system(ImmutableMap.Builder builder){
        ImmutableMap map = builder.build();
        for (Map.Entry e:(ImmutableSet<Map.Entry>)map.entrySet()){
            System.out.println(e.getKey()+" - "+e.getValue());
        }
    }
    public static ImmutableMap.Builder getBuilder(ImmutableMap.Builder builder){
        //printStackTree();
        ImmutableMap.Builder builder1 = ImmutableMap.builder();
        ImmutableMap map = builder.build();
        for (Map.Entry e:(ImmutableSet<Map.Entry>)map.entrySet()){
            if(!e.getKey().equals("lore")){
                builder1.put(e.getKey(),e.getValue());
            }
        }
        return builder1;
    }

    public static void printStackTree() {
        System.out.println("---------------- StackTree ----------------");
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (StackTraceElement e:elements){
            System.out.println(e.toString());
        }
        System.out.println("-------------------------------------------");
    }

    public static String listStringToString(List<String> lore){
        StringBuilder s = new StringBuilder();
        int a =0;
        for (String l:lore){
            s.append(l);
            if(a<lore.size()){
                s.append("-sws-");
            }
            a+=1;
        }
        return s.toString();
    }
    public static List<String> stringToListString(String s){
        if(s.isEmpty())return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(s.split("-sws-")));
    }
}
/*    "if($1.containsKey(\"sLore\"))" +
                                        "{" +
                                        "java.util.List ll = new java.util.ArrayList();" +
                                        "$0.ilore=me.wuxie.bukkitmodify.javassist.Main.stringToListString($1.get(\"sLore\").toString());" +
                                        "org.bukkit.craftbukkit."+bukkitVersion+".inventory.CraftMetaItem.safelyAdd($0.ilore,ll,true);" +
                                        //"$0.lore=me.wuxie.bukkitmodify.javassist.Main.stringToListString(((net.minecraft.server."+bukkitVersion+".NBTTagString)$1.get(\"sLore\")).asString());" +
                                        "$0.lore=ll;" +
                                        "}" +*/
/*CtMethod serialize = clazz.getDeclaredMethod("serialize",
                                new CtClass[]{ClassPool.getDefault().get("com.google.common.collect.ImmutableMap$Builder")});
                        serialize.insertAfter(
                                "{" +
                                "if($0.i12Lore!=null)" +
                                "{" +
                                "$1 = me.wuxie.bukkitmodify.javassist.Main.getBuilder($1);"+
                                "$1.put(\"lore\", com.google.common.collect.ImmutableList.copyOf($0.i12Lore));" +
                                "}" +
                                "else if($0.unhandledTags.containsKey(\"sLore\"))" +
                                "{" +
                                "$1 = me.wuxie.bukkitmodify.javassist.Main.getBuilder($1);"+
                                "$1.put(\"lore\", me.wuxie.bukkitmodify.javassist.Main.stringToListString(((net.minecraft.server."+bukkitVersion+".NBTTagString)$0.unhandledTags.get(\"sLore\")).asString()));" +
                                "}" +
                                "return $1;" +
                                "}");*/