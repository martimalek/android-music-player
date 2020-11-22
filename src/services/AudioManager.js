import { NativeModules } from 'react-native';

class AudioManager {
    static async init() {
        return NativeModules.AudioManager.init();
    }

    static async getAudios() {
        return NativeModules.AudioManager.getAudios();
    }

    static playSpecific(path) {
        NativeModules.AudioManager.playAudio(path);
    }

    static toggle() {
        NativeModules.AudioManager.toggle();
    }

    static playNext() {
        NativeModules.AudioManager.playNext();
    }

    static playPrevious() {
        NativeModules.AudioManager.playPrevious();
    }

    static ON_AUDIO_ENDED = NativeModules.AudioManager.ON_AUDIO_ENDED;
    static ON_AUDIO_PAUSED = NativeModules.AudioManager.ON_AUDIO_PAUSED;
    static ON_AUDIO_RESUMED = NativeModules.AudioManager.ON_AUDIO_RESUMED;
    static ON_CHILDREN_UPDATED = NativeModules.AudioManager.ON_CHILDREN_UPDATED;
}

export default AudioManager;

