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

    static play() {
        NativeModules.AudioManager.play();
    }

    static pause() {
        NativeModules.AudioManager.pause();
    }

    static ON_AUDIO_ENDED = NativeModules.AudioManager.ON_AUDIO_ENDED;
    static ON_AUDIO_PAUSED = NativeModules.AudioManager.ON_AUDIO_PAUSED;
}

console.log(NativeModules.AudioManager)

export default AudioManager;

