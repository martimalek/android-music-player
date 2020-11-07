import React, { useEffect, useState } from 'react';
import { DeviceEventEmitter, Dimensions, FlatList, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AudioControls } from '../components/AudioControls';

import AudioManager from '../services/AudioManager';

export const AudioList = () => {
    const [songs, setSongs] = useState([]);
    const [isPlaying, setIsPlaying] = useState(false);
    const [hasAudioEnded, setHasAudioEnded] = useState(false);
    const [selectedSong, setSelectedSong] = useState(null);

    useEffect(() => {
        init();
        DeviceEventEmitter.addListener(AudioManager.ON_AUDIO_ENDED, handleAudioEnd);
        DeviceEventEmitter.addListener(AudioManager.ON_AUDIO_PAUSED, handleAudioStopped);

        return () => {
            DeviceEventEmitter.removeAllListeners(AudioManager.ON_AUDIO_ENDED);
            DeviceEventEmitter.removeAllListeners(AudioManager.ON_AUDIO_PAUSED);
        }
    }, []);

    useEffect(() => {
        if (hasAudioEnded) handleNext()
    }, [hasAudioEnded]);

    const handleAudioStopped = () => setIsPlaying(false);

    const handleAudioEnd = () => {
        setIsPlaying(false);
        setHasAudioEnded(true);
    };

    const playSong = (index) => {
        if (hasAudioEnded) setHasAudioEnded(false);
        setSelectedSong(index);
        setIsPlaying(true);
        AudioManager.playSpecific(songs[index].data);
    };

    const init = async () => {
        await AudioManager.init();
        // if (isInitialized) getSongs();
    };

    const getSongs = async () => setSongs(await AudioManager.getAudios());

    const renderItem = ({ item: { title }, index }) => (
        <TouchableOpacity style={{ ...styles.item, ...(index === selectedSong ? styles.selected : {}) }} onPress={() => playSong(index)}>
            <Text style={styles.itemText}>{title}</Text>
        </TouchableOpacity>
    );

    const handleSongToggle = () => {
        AudioManager.toggle();
        // if (isPlaying) {
        //     AudioManager.pause();
        //     setIsPlaying(false);
        // } else {
        //     if (selectedSong) AudioManager.play();
        //     else AudioManager.playSpecific(songs[0].data);
        //     setIsPlaying(true);
        // }
    };

    const handleNext = () => {
        if (selectedSong !== songs.length - 1) playSong(selectedSong + 1);
        else playSong(0);
    };

    const handlePrev = () => {
        if (selectedSong === 0) playSong(songs.length - 1);
        else playSong(selectedSong - 1);
    };

    return (
        <SafeAreaView style={styles.container}>
            <Text style={styles.title}>MUSIC</Text>
            <FlatList
                data={songs}
                renderItem={renderItem}
                keyExtractor={({ id }) => `song-${id}`}
            />
            <AudioControls
                isPlaying={isPlaying}
                duration={140000}
                onToggle={handleSongToggle}
                onNext={handleNext}
                onPrev={handlePrev}
            />
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    item: {
        backgroundColor: '#597FFB',
        paddingVertical: 20,
        marginVertical: 8,
        marginHorizontal: 16,
        borderRadius: 10,
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    itemText: {
        color: 'white',
        paddingHorizontal: 20,
    },
    fab: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        position: 'absolute',
        width: 80,
        height: 80,
        right: (Dimensions.get('window').width / 2) - 40,
        bottom: 30,
        borderRadius: 50,
        elevation: 3,
        backgroundColor: '#7092ff',
    },
    selected: {
        backgroundColor: '#94adff',
    },
    title: {
        fontSize: 20,
        padding: 10,
        color: 'white',
    },
    container: {
        flex: 1,
        justifyContent: 'flex-start',
        alignItems: 'center',
        backgroundColor: '#100B2E',
    },
});
