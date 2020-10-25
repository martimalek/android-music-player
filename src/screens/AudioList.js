import React, { useEffect, useState } from 'react';
import { Dimensions, FlatList, Pressable, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';

import AudioManager from '../services/AudioManager';

export const AudioList = () => {
    const [songs, setSongs] = useState([]);
    const [selectedSong, setSelectedSong] = useState(null);
    const [isPlaying, setIsPlaying] = useState(false);

    useEffect(() => {
        init();
    }, []);

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
        <>
            <FlatList
                data={songs}
                renderItem={renderItem}
                keyExtractor={({ id }) => `song-${id}`}
            />
            <Pressable style={styles.fab} onPress={handleSongToggle}>
                <Text style={styles.itemText}>{isPlaying ? 'STOP' : 'PLAY'}</Text>
            </Pressable>
        </>
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
        position: 'absolute',
        width: 80,
        height: 80,
        alignItems: 'center',
        justifyContent: 'center',
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
});
