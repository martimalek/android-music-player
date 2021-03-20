import BarsColors from '@martimalek/react-native-bars-colors';

/**
 * Change the navbar color programmatically in android.
 * @param {String} navBarcolor In hex format (#aabbcc).
 * @param {String} statusBarColor In hex format (#aabbcc).
 * @param {Boolean} isLightTheme Whether the theme of the navbar is light or dark
 */
export const changeNavBarColor = async (navBarcolor, statusBarColor, isLightTheme = false) => BarsColors.changeNavBarColor(navBarcolor, statusBarColor, isLightTheme);
