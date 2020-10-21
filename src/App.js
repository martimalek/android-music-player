import React, { useEffect } from 'react';
import {
    SafeAreaView,
    StyleSheet,
    ScrollView,
    StatusBar,
    Text,
    View,
} from 'react-native';

import {
    Header,
    Colors,
} from 'react-native/Libraries/NewAppScreen';

const App = () => {

    useEffect(() => {
        getSongs();
    }, []);

    const getSongs = async () => {

    };

    return (
        <>
            <StatusBar barStyle="dark-content" />
            <SafeAreaView>
                <ScrollView
                    contentInsetAdjustmentBehavior="automatic"
                    style={styles.scrollView}>
                    <Header />
                </ScrollView>
                <View style={styles.divider} />
                <Text>Hello Martí! Maybe you feel that this continuing this project is pointless, but please don't give up on it yet!</Text>
                <View style={styles.divider} />
                <Text>Think about using your own app to listen to music... the degree of freedom to change it however you want, the experience you will win while developing it and of course...</Text>
                <View style={styles.divider} />
                <Text>Showing off huhuhu</Text>
                <View style={styles.divider} />
                <View style={styles.divider} />
                <Text style={styles.importantText}>Now stop reading and go fot it!! ;)</Text>
            </SafeAreaView>
        </>
    );
};

const styles = StyleSheet.create({
    scrollView: {
        backgroundColor: Colors.lighter,
    },
    divider: {
        marginBottom: 20,
    },
    importantText: {
        fontSize: 20,
    },
});

export default App;
