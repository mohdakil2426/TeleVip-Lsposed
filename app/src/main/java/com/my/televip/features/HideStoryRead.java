package com.my.televip.features;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.obfuscate.AutomationResolver;

public class HideStoryRead {

    public static boolean isReadStoriesRequest(Object object) {
        String className = object.getClass().getName();
        Class<?> objectClass = object.getClass();
        if (className.contains("TL_stories_readStories") ||
                className.contains("TL_stories_incrementStoryViews")) return true;

        return objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_STORIES_READ_STORIES))) ||
                objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_STORIES_INCREMENT_STORY_VIEWS)));
    }
}
