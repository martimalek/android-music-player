import React from 'react';
import PropTypes from 'prop-types';
import { Image, StyleSheet, Text, TouchableOpacity, View } from 'react-native';

import { PauseIcon, PlayIcon } from '../assets/icons';
import { NextSong } from '../assets/images';
import { formatDuration } from '../utils';

export const AudioControls = ({ isPlaying, onToggle, onNext, onPrev, duration }) => {
    const { minutes, seconds } = formatDuration(duration);

    return (
        <View style={styles.container}>
            <TouchableOpacity activeOpacity={0.8} style={styles.smallFab} onPress={onPrev}>
                <Image source={NextSong} style={{ ...styles.icon, ...styles.reverse }} tintColor='white' />
            </TouchableOpacity>
            <TouchableOpacity activeOpacity={0.8} style={styles.fab} onPress={onToggle}>
                {isPlaying ? (
                    <PauseIcon fill="white" />
                ) : (
                        <PlayIcon fill="white" />
                    )}
            </TouchableOpacity>
            <TouchableOpacity activeOpacity={0.8} style={styles.smallFab} onPress={onNext}>
                <Image source={NextSong} style={styles.icon} tintColor='white' />
            </TouchableOpacity>
            <View>
                <Text style={{ color: 'white' }}>{minutes}:{seconds}</Text>
            </View>
        </View>
    );
};

AudioControls.propTypes = {
    isPlaying: PropTypes.bool.isRequired,
    onToggle: PropTypes.func.isRequired,
    onNext: PropTypes.func.isRequired,
    onPrev: PropTypes.func.isRequired,
};

const styles = StyleSheet.create({
    container: {
        position: 'absolute',
        flexDirection: 'row',
        bottom: 0,
        elevation: 3,
        backgroundColor: '#7092ff',
        justifyContent: 'center',
        alignItems: 'center',
        width: '100%',
        paddingVertical: 10,
    },
    fab: {
        alignItems: 'center',
        justifyContent: 'center',
        width: 80,
        height: 80,
        borderRadius: 50,
        backgroundColor: '#7092ff',
    },
    smallFab: {
        alignItems: 'center',
        justifyContent: 'center',
        width: 40,
        height: 40,
        borderRadius: 50,
        backgroundColor: '#7092ff',
        marginHorizontal: 20,
    },
    icon: {
        width: 20,
        resizeMode: 'contain',
    },
    reverse: {
        transform: [{ rotate: '180deg' }],
    },
});
