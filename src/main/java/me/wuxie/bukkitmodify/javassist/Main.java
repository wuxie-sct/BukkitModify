package me.wuxie.bukkitmodify.javassist;

import javassist.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class Main {
    private final static ClassPool pool = ClassPool.getDefault();
    public static String bukkitVersion = null;
    public static void premain(final String agentArgs,final Instrumentation inst) {
        bukkitVersion = agentArgs;
        System.out.println(bukkitVersion+" ---> bukkitVersion");
        inst.addTransformer(new Transformer());
    }
    
    private static final class Transformer implements ClassFileTransformer {
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
            return null;
        }
    }
}