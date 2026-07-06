package com.my.televip.virtuals.ui.Components;

import com.my.televip.obfuscate.AutomationResolver;

import de.robv.android.xposed.XposedHelpers;

public class UItem {

    Object uItem;

    public UItem(Object uItem){
        this.uItem = uItem;
    }

    public int getID(){
        return XposedHelpers.getIntField(uItem, AutomationResolver.resolve("UItem", "id", AutomationResolver.ResolverType.Field));
    }

    public String getText(){
        return (String) XposedHelpers.getObjectField(uItem, AutomationResolver.resolve("UItem", "text", AutomationResolver.ResolverType.Field));
    }

    public String getSubtext(){
        return (String) XposedHelpers.getObjectField(uItem, AutomationResolver.resolve("UItem", "subtext", AutomationResolver.ResolverType.Field));
    }

    public Object getUItem(){
        return uItem;
    }
}
