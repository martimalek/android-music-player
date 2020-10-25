import React from 'react';
import { SafeAreaView, StatusBar, StyleSheet, Text } from 'react-native';

import { AudioList } from './screens/AudioList';

const App = () => {

    return (
        <>
            <StatusBar barStyle="dark-content" />
            <SafeAreaView style={styles.container}>
                <Text style={styles.title}>MUSIC</Text>
                <AudioList />
            </SafeAreaView>
        </>
    );
};

const styles = StyleSheet.create({
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

export default App;
