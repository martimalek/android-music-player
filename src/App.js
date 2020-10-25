import 'react-native-gesture-handler';
import React from 'react';
import { StatusBar } from 'react-native';

import { MainNavigator } from './navigation/MainNavigator';

const App = () => {

    return (
        <>
            <StatusBar barStyle="dark-content" />
            <MainNavigator />
        </>
    );
};

export default App;
