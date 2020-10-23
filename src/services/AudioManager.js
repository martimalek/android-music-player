import { NativeModules } from 'react-native';

class AudioManager {
    static async getAudios() {
        return NativeModules.AudioManager.getAudios();
    }

    static playSpecific(path) {
        NativeModules.AudioManager.playAudio(path);
    }

    static play() {
        NativeModules.AudioManager.play();
    }

    static stop() {
        NativeModules.AudioManager.stop();
    }
}

export default AudioManager;

