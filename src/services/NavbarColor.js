import { NativeModules, Platform } from 'react-native';

/**
 * Change the navbar color programmatically in android.
 * @param {String} color In hex format (#aabbcc).
 * @param {Boolean} isLightTheme Whether the theme of the navbar is light or dark
 * @param {Number} delay Delay (ms) before the animation
 */
export const changeNavBarColor = async (color, isLightTheme = false, delay = 0) => {
    setTimeout(() => {
        if (Platform.OS !== 'android') return;

        NativeModules.NavbarColor.changeNavBarColor(color, isLightTheme);
    }, delay);
};