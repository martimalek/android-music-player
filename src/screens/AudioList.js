import React, { useEffect, useState } from 'react';
import { DeviceEventEmitter, Dimensions, FlatList, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AudioControls } from '../components/AudioControls';
import { AudioItem } from '../components/AudioItem';

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
        DeviceEventEmitter.addListener(AudioManager.ON_AUDIO_RESUMED, handleAudioResumed);
        DeviceEventEmitter.addListener(AudioManager.ON_CHILDREN_UPDATED, handleChildrenUpdate);
        DeviceEventEmitter.addListener(AudioManager.ON_POSITION_CHANGED, handlePositionChanged);

        return () => {
            DeviceEventEmitter.removeAllListeners(AudioManager.ON_AUDIO_ENDED);
            DeviceEventEmitter.removeAllListeners(AudioManager.ON_AUDIO_PAUSED);
            DeviceEventEmitter.removeAllListeners(AudioManager.ON_AUDIO_RESUMED);
            DeviceEventEmitter.removeAllListeners(AudioManager.ON_CHILDREN_UPDATED);
            DeviceEventEmitter.removeAllListeners(AudioManager.ON_POSITION_CHANGED);
        }
    }, []);

    const handleSongToggle = AudioManager.toggle;
    const handleNext = AudioManager.playNext;
    const handlePrev = AudioManager.playPrevious;
    const onItemSwipeRight = AudioManager.addSongToSelectedQueueByPosition;

    useEffect(() => {
        if (hasAudioEnded) handleNext()
    }, [hasAudioEnded]);

    const handleAudioStopped = () => setIsPlaying(false);
    const handleAudioResumed = () => setIsPlaying(true);
    const handleChildrenUpdate = setSongs;
    const handlePositionChanged = setSelectedSong;

    const handleAudioEnd = () => {
        setIsPlaying(false);
        setHasAudioEnded(true);
    };

    const playSong = async (index) => {
        if (hasAudioEnded) setHasAudioEnded(false);
        try {
            await AudioManager.playFromQueuePosition(index);
        } catch (err) {
            // Handle error
        }
    };

    const init = async () => AudioManager.init();

    const renderItem = ({ item: { title }, index }) => (
        <AudioItem
            style={{ ...(index === selectedSong ? styles.selected : {}) }}
            onPress={() => playSong(index)}
            title={title}
            onSwipeRight={() => onItemSwipeRight(index)}
        />
    );

    return (
        <SafeAreaView style={styles.container}>
            <Text style={styles.title}>MUSIC</Text>
            <FlatList
                style={styles.list}
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
    list: {
        marginBottom: 110,
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
