import React, { useEffect, useState } from 'react';
import { DeviceEventEmitter, Dimensions, FlatList, Pressable, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { PauseIcon, PlayIcon } from '../assets/icons';

import AudioManager from '../services/AudioManager';

export const AudioList = () => {
    const [songs, setSongs] = useState([]);
    const [selectedSong, setSelectedSong] = useState(null);
    const [isPlaying, setIsPlaying] = useState(false);

    useEffect(() => {
        init();
        DeviceEventEmitter.addListener(AudioManager.ON_AUDIO_END, handleAudioEnd);

        return () => {
            DeviceEventEmitter.removeAllListeners(AudioManager.ON_AUDIO_END);
        }
    }, []);

    const handleAudioEnd = () => {
        if (selectedSong !== songs.length - 1) AudioManager.playSpecific(songs[selectedSong + 1].data);
        else AudioManager.playSpecific(songs[0].data);
    };

    const init = async () => {
        const isInitialized = await AudioManager.init();
        if (isInitialized) getSongs();
    };

    const getSongs = async () => setSongs(await AudioManager.getAudios());

    const handlePress = (index) => {
        setIsPlaying(true);
        setSelectedSong(index);
        AudioManager.playSpecific(songs[index].data);
    };

    const renderItem = ({ item: { title }, index }) => (
        <TouchableOpacity style={styles.item} onPress={() => handlePress(index)}>
            <Text style={styles.itemText}>{title}</Text>
            {selectedSong === index && (
                <View style={styles.selected} />
            )}
        </TouchableOpacity>
    );

    const handleSongToggle = () => {
        if (isPlaying) {
            AudioManager.pause();
            setIsPlaying(false);
        } else {
            if (selectedSong) AudioManager.play();
            else AudioManager.playSpecific(songs[0].data);
            setIsPlaying(true);
        }
    };

    return (
        <SafeAreaView style={styles.container}>
            <Text style={styles.title}>MUSIC</Text>
            <FlatList
                data={songs}
                renderItem={renderItem}
                keyExtractor={({ id }) => `song-${id}`}
            />
            <TouchableOpacity activeOpacity={0.8} style={styles.fab} onPress={handleSongToggle}>
                {isPlaying ? (
                    <PauseIcon fill="white" />
                ) : (
                        <PlayIcon fill="white" />
                    )}
            </TouchableOpacity>
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
        height: 2,
        width: '96%',
        backgroundColor: 'white',
        borderRadius: 20,
        transform: [{ translateY: 19 }],
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
