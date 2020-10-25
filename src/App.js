import 'react-native-gesture-handler';
import React from 'react';
import PropTypes from 'prop-types';
import { StatusBar } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { MainNavigator } from './navigation/MainNavigator';

const App = () => {

    return (
        <MainProvider>
            <StatusBar barStyle="dark-content" />
            <MainNavigator />
        </MainProvider>
    );
};

export default App;

const MainProvider = ({ children }) => (
    <SafeAreaProvider>
        {children}
    </SafeAreaProvider>
);

MainProvider.propTypes = {
    children: PropTypes.node.isRequired,
};
